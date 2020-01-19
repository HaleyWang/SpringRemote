/******************************************************************************
 *
 * Copyright (c) 1999-2011 Cryptzone Group AB. All Rights Reserved.
 * 
 * This file contains Original Code and/or Modifications of Original Code as
 * defined in and that are subject to the MindTerm Public Source License,
 * Version 2.0, (the 'License'). You may not use this file except in compliance
 * with the License.
 * 
 * You should have received a copy of the MindTerm Public Source License
 * along with this software; see the file LICENSE.  If not, write to
 * Cryptzone Group AB, Drakegatan 7, SE-41250 Goteborg, SWEDEN
 *
 *****************************************************************************/

package com.mindbright.ssh2;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;

import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import com.mindbright.nio.NIOCallback;
import com.mindbright.nio.NetworkConnection;
import com.mindbright.nio.NonBlockingInput;
import com.mindbright.nio.NonBlockingOutput;
import com.mindbright.util.Queue;
import com.mindbright.util.Log;

/**
 * Class implementing streams-based channels. That is channels which
 * locally are connected to a pair of Input/Output streams. It is also
 * possible to apply filters to the channels.
 */
public class SSH2StreamChannel extends SSH2Channel implements NIOCallback {
    protected InputStream  in;
    protected OutputStream out;
    protected NonBlockingInput  nbin;
    protected NonBlockingOutput nbout;

    protected Thread transmitter;
    protected Thread receiver;
    protected Queue  rxQueue;
    protected long   txCounter;
    protected long   rxCounter;

    protected Log log;

    /*
     * NOTE, if enabled can cause dead-lock when doing re-keyexchange
     * if we initiate it, hence SSH2Transport.incompatibleCantReKey is
     * set accordingly
     */
    private boolean rxChanIsQueued;

    protected SSH2StreamChannel(int channelType, SSH2Connection connection,
                                Object creator) {
        this(channelType, connection, creator, (InputStream)null, null, false);
    }

    protected SSH2StreamChannel(int channelType, SSH2Connection connection,
                                Object creator, boolean pty) {
        this(channelType, connection, creator, (InputStream)null, null, pty);
    }

    /**
     * Create a new stream channel of the given type. The channel is
     * associated with an ssh connection. Channel types are
     * defined in <code>SSH2Connection</code> and starts with
     * <code>CH_TYPE</code>.
     *
     * @param channelType Type of channel to create.
     * @param connection The ssh connection to associate the channel with.
     * @param creator The object the channel is created from.
     * @param in The input stream from which data to be sent over the
     * channel is read.
     * @param out The output stream onto which data received from the
     * channel is written.
     *
     */
    protected SSH2StreamChannel(int channelType, SSH2Connection connection,
                                Object creator,
                                InputStream in, OutputStream out) {
        super(channelType, connection, creator, false);
        init(connection, null, in, out, null, null);
    }

    protected SSH2StreamChannel(int channelType, SSH2Connection connection,
                                Object creator,
                                InputStream in, OutputStream out,
								boolean pty) {
        super(channelType, connection, creator, pty);
        init(connection, null, in, out, null, null);
    }

    protected SSH2StreamChannel(int channelType, SSH2Connection connection,
                                Object creator, Socket s) throws IOException {
        super(channelType, connection, creator, false);
        init(connection, s, s.getInputStream(), s.getOutputStream(), null, null);
    }

    protected SSH2StreamChannel(int channelType, SSH2Connection connection,
                                Object creator,
                                NonBlockingInput in, NonBlockingOutput out) {
        super(channelType, connection, creator, false);
        init(connection, null, null, null, in, out);
    }

    protected SSH2StreamChannel(int channelType, SSH2Connection connection,
                                Object creator,
                                NonBlockingInput in, NonBlockingOutput out,
								boolean pty) {
        super(channelType, connection, creator, pty);
        init(connection, null, null, null, in, out);
    }

    private void init(SSH2Connection connection, 
                      Socket s, InputStream is, OutputStream os, 
                      NonBlockingInput in, NonBlockingOutput out) {
        
        SocketChannel channel = (s != null) ? s.getChannel() : null;
        
        if (channel != null) {
            try {
                NetworkConnection nconn = new NetworkConnection(channel);
                in  = nconn.getInput();
                out = nconn.getOutput();
            } catch (IOException ioe) {
            }
        }
        this.log = connection.getLog();
        if (in != null || out != null) {
            rxChanIsQueued = false;
            this.nbin  = in;
            this.nbout = out;
        } else {
            rxChanIsQueued =
                "true".equals(connection.getPreferences().
                              getPreference(SSH2Preferences.QUEUED_RX_CHAN));            
            this.in  = is;
            this.out = os;
            createStreams();
        }
    }
    
                      
    /**
     * Apply the given filter to this channel. Filters are not yet supported
     * on non-blocking channels.
     *
     * @param filter Filter to apply.
     */
    public void applyFilter(SSH2StreamFilter filter) {
        if (this.in == null) {
            throw new IllegalArgumentException(
                "Filters are not supported for non-blocking channels");
        }
        if(filter != null) {
            in  = filter.getInputFilter(in);
            out = filter.getOutputFilter(out);
        }
    }

    private SSH2TransportPDU pdu;
    private long             maxSz;
    private int              rcvSz = 0;
    private int              startPos; 
    private boolean waiting = false;
	
    private void initPDU() {
        pdu = SSH2TransportPDU.createOutgoingPacket(SSH2.MSG_CHANNEL_DATA,
                                                    (int)txMaxPktSz + 256);
        pdu.writeInt(peerChanId);
    }

    private void sendPDU() {
        if(rcvSz == -1) {
            sendEOF();
        } else if (!eofSent && !closeSent) {
            pdu.writeInt(rcvSz);
            pdu.wPos  += rcvSz;
            txCounter += rcvSz;
            transmit(pdu);
        }
    }

    private void launchNonBlockingRead() {
        if (nbin == null || waiting) {
            return;
        }

        for(;;) {
            initPDU();
            maxSz = checkTxWindowSizeNIO(rcvSz);
            if (maxSz <= 0) {
                rcvSz = 0;
                break;
            }
            if (maxSz > Integer.MAX_VALUE)
                maxSz = Integer.MAX_VALUE;
            startPos = pdu.wPos+4;
            ByteBuffer buf = nbin.createBuffer(pdu.data, startPos, (int)maxSz);	    
            try {
                if (!nbin.read(buf, this, true, true)) {
                    waiting = true;
                    break;
                }
            } catch (IOException e) {
                readFailed(e);
                return;
            }
            handleNonBlockingData(buf);
        }
    }

    private void handleNonBlockingData(ByteBuffer buf) { 
	rcvSz = buf.position() - startPos;
        sendPDU();
    }

    private void channelTransmitLoop() {
        Thread.yield();
        try {
            boolean interrupted = false;
            while(!eofSent && !closeSent) {
                initPDU();
                maxSz = checkTxWindowSize(rcvSz);
                if (maxSz > Integer.MAX_VALUE)
                    maxSz = Integer.MAX_VALUE;
                do {
                    try {
                        rcvSz = in.read(pdu.data, pdu.wPos + 4, (int)maxSz);
                        interrupted = false;
                    } catch (InterruptedIOException e) {
                        interrupted = true;
                    }
                } while (interrupted);
                sendPDU();
            }
        } catch (IOException e) {
            if(!eofSent) {
                connection.getLog().error("SSH2StreamChannel",
                                          "channelTransmitLoop",
                                          e.toString());
            }
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (IOException e) { /* don't care */
            }
            sendClose();
        }
        connection.getLog().debug("SSH2StreamChannel",
                                  "exiting ch. #" +
                                  channelId + " (" + getType() +
                                  ") transmitter, " + txCounter +
                                  " bytes tx");
    }

    private void channelReceiveLoop() {
        connection.getLog().debug("SSH2StreamChannel",
                                  "starting ch. #" + channelId +
                                  " (" + getType() + ") receiver");
        Thread.yield();
        try {
            SSH2TransportPDU pdu;
            while((pdu = (SSH2TransportPDU)rxQueue.getFirst()) != null) {
                rxWrite(pdu);
            }
        } catch (IOException e) {
            connection.getLog().error("SSH2StreamChannel",
                                      "channelReceiveLoop",
                                      e.toString());
        } finally {
            rxClosing();
        }
        connection.getLog().debug("SSH2StreamChannel",
                                  "exiting ch. #" +
                                  channelId + " (" + getType() +
                                  ") receiver, " + rxCounter +
                                  " bytes rx");
    }

    private final void rxWrite(SSH2TransportPDU pdu) throws IOException {
        if (out == null && nbout == null) {
            pdu.release();
            return;
        }
        int len = pdu.readInt();
        int off = pdu.getRPos();
        rxCounter += len;
        if (out != null) {
            log.debug2("SSH2StreamChannel", "rxWrite", "t@" + Thread.currentThread().getId() + " out.write " + len);
            out.write(pdu.data, off, len);
        } else {
            log.debug2("SSH2StreamChannel", "rxWrite", "t@" + Thread.currentThread().getId() + " nbout.write " + len);
            nbout.write(pdu.data, off, len);
        }
        pdu.release();
        checkRxWindowSize(len);
    }

    private final void rxClosing() {
        // Signal to transmitter that this is an orderly shutdown
        //
        eofSent = true;
        try {
            if (out != null)
                out.close();
            if (nbout != null) {
                nbout.close();
                nbout = null;
            }
        } catch (IOException e) { /* don't care */
        }
        try {
            if (in != null)
                in.close();
            if (nbin != null) {
                nbin.close();
                nbin = null;
            }
        } catch (IOException e) { /* don't care */
        }
        outputClosed();

        // there is a slight chance that the transmitter is waiting for
        // window adjust in which case we must interrupt it here so it
        // doesn't hang
        //
        if(txCurrWinSz == 0) {
            txCurrWinSz = -1;
            if (in != null) {
                transmitter.interrupt();
            }
        }
    }

    private final synchronized long checkTxWindowSize(int lastSz) {
        txCurrWinSz -= lastSz;
        while(txCurrWinSz == 0) {
            // Our window is full, wait for ACK from peer
            try {
                this.wait();
            } catch (InterruptedException e) {
                if(!eofSent) {
                    connection.getLog().error("SSH2StreamChannel",
                                              "checkTxWindowSize",
                                              "window adjust wait interrupted");
                }
            }
        }
        // Try sending remaining window size or max packet size before ACK
        //
        long dataSz = (txCurrWinSz < txMaxPktSz ? txCurrWinSz : txMaxPktSz);
        return dataSz;
    }

    private final synchronized long checkTxWindowSizeNIO(int lastSz) {
        txCurrWinSz -= lastSz;
        if (txCurrWinSz == 0) {
            return 0;
        }
        // Try sending remaining window size or max packet size before ACK
        //
        return (txCurrWinSz < txMaxPktSz ? txCurrWinSz : txMaxPktSz);
    }

    protected final void checkRxWindowSize(int len) {
        rxCurrWinSz -= len;
        if(rxCurrWinSz < 0) {
            connection.fatalDisconnect(SSH2.DISCONNECT_PROTOCOL_ERROR,
                                       "Peer overflowed window");
        } else if(rxCurrWinSz <= (rxInitWinSz >>> 1) || (rxInitWinSz - rxCurrWinSz) > rxMaxPktSz * 3) {
            // ACK on >= 50% of window received or > 3 * max packet size
            SSH2TransportPDU pdu =
                SSH2TransportPDU.createOutgoingPacket(SSH2.MSG_CHANNEL_WINDOW_ADJUST);
            pdu.writeInt(peerChanId);
            pdu.writeInt(rxInitWinSz - rxCurrWinSz);
            transmit(pdu);
            rxCurrWinSz = rxInitWinSz;
        }
    }

    protected void data(SSH2TransportPDU pdu) {
        if (rxChanIsQueued) {
            log.debug2("SSH2StreamChannel", "data", "queuing data");
            rxQueue.putLast(pdu);
        } else {
            try {
                rxWrite(pdu);
            } catch (IOException e) {
                log.debug2("SSH2StreamChannel", "data", "rxWrite failed");
                connection.getLog().error("SSH2StreamChannel",
                                          "data",
                                          e.toString());
                rxClosing();
            }
        }
    }

    protected void openConfirmationImpl(SSH2TransportPDU pdu) {
        connection.getLog().debug("SSH2StreamChannel",
                                  "starting ch. #" + channelId +
                                  " (" + getType() + ") transmitter");
        if (in != null) {
            startStreams();
        } else {
            launchNonBlockingRead();
        }
    }

    protected boolean openFailureImpl(int reasonCode, String reasonText,
                                      String langTag) {
        // Just return false since we don't want to keep the channel,
        // handle in derived class if needed
        return false;
    }

    protected synchronized void windowAdjustImpl(long inc) {
        connection.getLog().debug("SSH2StreamChannel", 
				  "adjust:" + txCurrWinSz + ", " + inc);
        txCurrWinSz += inc;
        if (in != null) {
            this.notify();
        } else {
            launchNonBlockingRead();        
        }
    }

    protected void eofImpl() {
        if(rxChanIsQueued) {
            rxQueue.setBlocking(false);
        } else {
            rxClosing();
        }
    }

    protected void closeImpl() {
        eofImpl();
    }

    /**
     * Called when no more data can be written to the channel.
     */
    protected void outputClosed() {
        // Do nothing, handle in derived class if needed
    }

    protected void handleRequestImpl(String type, boolean wantReply,
                                     SSH2TransportPDU pdu) {
        // Do nothing, handle in derived class if needed
    }

    /**
     * Create the transmitter and receiver threads.
     */
    protected void createStreams() {
        if(rxChanIsQueued) {
            receiver = new Thread(new Runnable() {
                public void run() {
                    channelReceiveLoop();
                }
            }
                , "SSH2StreamRX_" + getType() + "_" + channelId);
            receiver.setDaemon(true);
            
            rxQueue =
                new Queue(connection.getPreferences().
                          getIntPreference(SSH2Preferences.QUEUE_DEPTH),
                          connection.getPreferences().
                          getIntPreference(SSH2Preferences.QUEUE_HIWATER));
        }
        transmitter = new Thread(new Runnable() {
            public void run() {
                channelTransmitLoop();
            }
        }
            , "SSH2StreamTX_" + getType() + "_" + channelId);
        transmitter.setDaemon(true);
    }
    
    /**
     * Starts the transmitter and receiver threads.
     */
    protected void startStreams() {
        transmitter.start();
        if(rxChanIsQueued) {
            receiver.start();
        }
    }

    public void waitUntilClosed(int timeout) {
        super.waitUntilClosed(timeout);
        if(rxChanIsQueued) {
            try {
                receiver.join(timeout*1000);
            } catch (InterruptedException e) { }
        }
    }

    public void waitUntilClosed() {
        waitUntilClosed(0);
    }

    /*
     * NIOCallback interface
     */
    public void completed(ByteBuffer buf) {
        waiting = false;
        handleNonBlockingData(buf);
        launchNonBlockingRead();     
    }

    public void readFailed(Exception e) {
        if(!eofSent) {
            connection.getLog().error("SSH2StreamChannel", "readFailed",
                                      e.toString());
        }
        try {
            if (nbin != null)
                nbin.close();
        } catch (IOException ioe) { /* don't care */}
        sendClose();
        connection.getLog().debug("SSH2StreamChannel",
                                  "exiting ch. #" +
                                  channelId + " (" + getType() +
                                  ") transmitter, " + txCounter +
                                  " bytes tx");
    }

    public void writeFailed() {}
    public void connected(boolean timeout) {}
    public void connectionFailed(Exception e) {}
}


