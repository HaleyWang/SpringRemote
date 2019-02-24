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

import java.io.IOException;
import java.net.Socket;

import com.mindbright.util.Queue;

/**
 * This class takes care of all incoming CHANNEL_OPEN messages. It contains a
 * thread which utilizes a queue to fetch the channel open requests. It creates
 * new channels according to type requested. Only a single instance of
 * <code>SSH2Connector</code> exists for each instance of the ssh2 stack.
 *
 * @see SSH2Connection
 */
public final class SSH2Connector implements Runnable {

    private Queue  openQueue;
    private Thread myThread;

    private SSH2Connection connection;

    private volatile boolean keepRunning;

    public SSH2Connector(SSH2Connection connection) {
        this.connection  = connection;
        this.openQueue   = new Queue();
        this.keepRunning = true;

        myThread = new Thread(this, "SSH2Connector");
        myThread.setDaemon(true);
        myThread.start();
    }

    public void run() {
        while (keepRunning) {
            SSH2TransportPDU pdu     = null;
            SSH2Channel      channel = null;

            String channelType = "<unknown>";
            int    peerChanId  = -1;
            int    txInitWinSz = -1;
            int    txMaxPktSz  = -1;
            String targetAddr  = "<unknown>";
            int    targetPort  = -1;
            String originAddr  = "<unknown>";
            int    originPort  = -1;

            try {
                pdu = (SSH2TransportPDU)openQueue.getFirst();

                if(pdu == null) {
                    keepRunning = false;
                    continue;
                }

                channelType = pdu.readJavaString();
                peerChanId  = pdu.readInt();
                txInitWinSz = pdu.readInt();
                txMaxPktSz  = pdu.readInt();

                if(channelType.equals(SSH2Connection.CHAN_FORWARDED_TCPIP)) {
                    String remoteAddr = pdu.readJavaString();
                    int    remotePort = pdu.readInt();
                    originAddr = pdu.readJavaString();
                    originPort = pdu.readInt();

                    connection.getLog().debug("SSH2Connection", "run",
                                              "remote connect on " +
                                              remoteAddr + ":" + remotePort +
                                              " (orig. " +
                                              originAddr + ":" + originPort +
                                              ")");

                    String[] localTarget =
                        connection.getForwardTarget(remoteAddr, remotePort);

                    SSH2StreamFilterFactory filterFactory =
                        connection.getForwardFilterFactory(remoteAddr,
                                                           remotePort);

                    if(localTarget == null) {
                        throw new IOException("Unsolicited forward attempted");
                    }

                    targetAddr = localTarget[0];
                    targetPort = Integer.valueOf(localTarget[1]).intValue();

                    connection.getLog().notice("SSH2Connector",
                                               "connect: " +
                                               localTarget[0] + ":" +
                                               localTarget[1] + " (peerid: " +
                                               peerChanId + ")");

                    channel = connect(channelType, peerChanId,
                                      txInitWinSz, txMaxPktSz,
                                      remoteAddr, remotePort,
                                      targetAddr, targetPort,
                                      originAddr, originPort,
                                      filterFactory);
                    connection.getEventHandler().remoteForwardedConnect(connection,
                            remoteAddr,
                            remotePort,
                            channel);

                } else if(channelType.equals(SSH2Connection.CHAN_DIRECT_TCPIP)) {
                    targetAddr = pdu.readJavaString();
                    targetPort = pdu.readInt();
                    originAddr = pdu.readJavaString();
                    originPort = pdu.readInt();
                    channel = connect(channelType, peerChanId,
                                      txInitWinSz, txMaxPktSz,
                                      targetAddr, targetPort,
                                      originAddr, originPort,
                                      null);
                    connection.getEventHandler().remoteDirectConnect(connection,
                            channel);

                } else if(channelType.equals(SSH2Connection.CHAN_X11)) {
                    originAddr = pdu.readJavaString();
                    originPort = pdu.readInt();

                    if(!connection.hasX11Mapping()) {
                        throw new IOException("Unexpected X11 channel open");
                    }

                    channel = connectX11(peerChanId, 
                                         txInitWinSz, txMaxPktSz,
                                         originAddr, originPort);

                    connection.getEventHandler().remoteX11Connect(connection,
                            channel);

                } else if(channelType.equals(SSH2Connection.CHAN_SESSION)) {
                    throw new IOException("Unexpected session channel open");
                } else if(channelType.equals(SSH2Connection.CHAN_AUTH_AGENT)) {
                    throw new IOException("Agent forwarding not supported");
                } else {
                    throw new IOException("Unknown channel type: " + channelType);
                }

            } catch (IOException e) {
                String msg =  "open failed: " + e.getMessage();
                connection.getLog().error("SSH2Connector", "run", msg);
                sendOpenFailure(peerChanId, 2, msg);
                connection.getEventHandler().remoteChannelOpenFailure(
                    connection, channelType,
                    targetAddr, targetPort,
                    originAddr, originPort,
                    new SSH2ConnectException("Failed in SSH2Connector", e));
            } finally {
                if(pdu != null)
                    pdu.release();
            }
        }
    }

    private SSH2Channel connectX11(int peerChanId,
                                   int txInitWinSz, int txMaxPktSz,
                                   String originAddr, int originPort) 
        throws IOException {


        String display = connection.getPreferences().
            getPreference(SSH2Preferences.X11_DISPLAY);

        if (display.equals(SSH2Preferences.X11_DISPLAY_AUTO)) {
            display = (System.getenv("DISPLAY") != null) ? 
                System.getenv("DISPLAY") : "127.0.0.1:0";
        }

        if (display.startsWith("/")
            || display.startsWith(":")
            || display.startsWith("unix:")) {
            // Display refers to an UNIX domain socket
            String path;

            if (display.startsWith("/")) {
                // Apple apparently uses a path
                path = display;
            } else {
                int num = 0;
                int start = display.lastIndexOf(":");
                int end   = display.indexOf(".", start);
                if (start != -1 && end != -1) {
                    num = Integer.parseInt(display.substring(start + 1, end));
                } else if (start != -1) {
                    num = Integer.parseInt(display.substring(start + 1));
                }
                path = "/tmp/.X11-unix/X" + num;
            }

            connection.getLog().debug("SSH2Connection", "run",
                                      "connect to X-server via " + path);

            Process proc = Runtime.getRuntime().exec(
                new String[] { "socat", "-", "UNIX-CONNECT:" + path});
            
            return connect(SSH2Connection.CHAN_X11, peerChanId,
                           txInitWinSz, txMaxPktSz,
                           proc,
                           originAddr, originPort,
                           SSH2X11Filter.getFilterFactory());

        }
		String targetAddr = "<unknown>";
		int    targetPort = -1;
		int i = display.indexOf(":");
		int j = display.indexOf(".", i);
		if(i != -1) {
		    targetAddr = display.substring(0, i);
		    if (j == -1) {
		        targetPort = Integer.parseInt(display.substring(i + 1));
		    } else {
		        targetPort = Integer.parseInt(display.substring(i + 1, j));
		    }
		} else {
		    targetAddr = display;
		    targetPort = 6000;
		}
      
		if(targetPort <= 10) {
		    targetPort += 6000;
		}

		connection.getLog().debug("SSH2Connection", "run",
		                          "connect to X-server on " +
		                          targetAddr + ":" + targetPort);

		return connect(SSH2Connection.CHAN_X11, peerChanId,
		               txInitWinSz, txMaxPktSz,
		               targetAddr, targetPort,
		               originAddr, originPort,
		               SSH2X11Filter.getFilterFactory());
    }

    public void channelOpen(SSH2TransportPDU pdu) {
        openQueue.putLast(pdu);
    }

    public void setThreadPriority(int prio) {
        myThread.setPriority(prio);
    }

    public void stop() {
        keepRunning = false;
        openQueue.setBlocking(false);
    }

    private SSH2Channel connect(String channelType, int peerChanId,
                                int txInitWinSz, int txMaxPktSz,
                                String targetAddr, int targetPort,
                                String originAddr, int originPort,
                                SSH2StreamFilterFactory filterFactory)
    throws IOException {
        return connect(channelType, peerChanId, txInitWinSz, txMaxPktSz,
                       "<N/A>", 0,
                       targetAddr, targetPort, originAddr, originPort,
                       filterFactory);
    }

    private SSH2Channel connect(String channelType, int peerChanId,
                                int txInitWinSz, int txMaxPktSz,
                                String remoteAddr, int remotePort,
                                String targetAddr, int targetPort,
                                String originAddr, int originPort,
                                SSH2StreamFilterFactory filterFactory)
    throws IOException {
        Socket fwdSocket = new Socket(targetAddr, targetPort);

        int ch = 0;
        for(ch = 0; ch < SSH2Connection.channelTypes.length; ch++) {
            if(SSH2Connection.channelTypes[ch].equals(channelType))
                break;
        }
        if(ch == SSH2Connection.channelTypes.length) {
            throw new IOException("Invalid channelType: " + channelType);
        }

        SSH2TCPChannel channel = new SSH2TCPChannel(ch, connection, this,
                                                    fwdSocket,
                                                    remoteAddr, remotePort,
                                                    originAddr, originPort);

        channel.init(peerChanId, txInitWinSz, txMaxPktSz);

        connection.setSocketOptions(SSH2Preferences.SOCK_OPT_REMOTE +
                                    remoteAddr + "." + remotePort, fwdSocket);

        connection.getEventHandler().channelConnect(this, channel,
                fwdSocket);

        if(filterFactory != null) {
            channel.applyFilter(filterFactory.createFilter(connection,
                                channel));
        }

        // MUST send confirmation BEFORE starting streams
        //
        sendOpenConfirmation(channel);

        channel.startStreams();

        return channel;
    }

    private SSH2Channel connect(String channelType, int peerChanId,
                                int txInitWinSz, int txMaxPktSz,
                                Process proc,
                                String originAddr, int originPort,
                                SSH2StreamFilterFactory filterFactory)
    throws IOException {
        int ch = 0;
        for(ch = 0; ch < SSH2Connection.channelTypes.length; ch++) {
            if(SSH2Connection.channelTypes[ch].equals(channelType))
                break;
        }
        if(ch == SSH2Connection.channelTypes.length) {
            throw new IOException("Invalid channelType: " + channelType);
        }

        SSH2ProcChannel channel = new SSH2ProcChannel(
            ch, connection, this, proc, originAddr, originPort);

        channel.init(peerChanId, txInitWinSz, txMaxPktSz);

        connection.getEventHandler().channelConnect(this, channel, null);

        if(filterFactory != null) {
            channel.applyFilter(filterFactory.createFilter(connection,
                                channel));
        }

        // MUST send confirmation BEFORE starting streams
        //
        sendOpenConfirmation(channel);

        channel.startStreams();

        return channel;
    }

    private void sendOpenConfirmation(SSH2Channel channel) {
        SSH2TransportPDU pdu =
            SSH2TransportPDU.createOutgoingPacket(SSH2.MSG_CHANNEL_OPEN_CONFIRMATION);
        pdu.writeInt(channel.peerChanId);
        pdu.writeInt(channel.channelId);
        pdu.writeInt(channel.rxInitWinSz);
        pdu.writeInt(channel.rxMaxPktSz);
        connection.transmit(pdu);
    }

    private void sendOpenFailure(int peerChanId,
                                 int reason, String description) {
        SSH2TransportPDU pdu =
            SSH2TransportPDU.createOutgoingPacket(SSH2.MSG_CHANNEL_OPEN_FAILURE);
        pdu.writeInt(peerChanId);
        pdu.writeInt(reason);

        if(!connection.getTransport().incompatibleChannelOpenFail) {
            pdu.writeString(description);
            pdu.writeString(""); // !!! TODO: Language tags again...
        }

        connection.transmit(pdu);
    }

}
