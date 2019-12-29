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

import java.security.GeneralSecurityException;

import javax.crypto.Mac;
import javax.crypto.ShortBufferException;

import com.mindbright.util.SecureRandomAndPad;

import com.mindbright.nio.NonBlockingInput;
import com.mindbright.nio.NonBlockingOutput;

/**
 * Handles one packet data unit. These are the actual packets which
 * are sent over en encrypted tunnel. Encryption and compression are
 * applied to these.
 * <p>
 * There is a difference bwteeen incoming and outgoing PDUs. The
 * latter have extra space allocated in them to help speed up the
 * encryption process.
 * <p>
 * This class makes an effort to avoid copying data wherever possible.
 */
public class SSH2TransportPDU extends SSH2DataBuffer {
    public final static int PACKET_DEFAULT_SIZE = 1024;
    public final static int PACKET_MIN_SIZE     = 16;
    public final static int PACKET_MAX_SIZE     = 35000;

    public static int pktDefaultSize = PACKET_DEFAULT_SIZE;

    public static SSH2TransportPDU factoryInstance = new SSH2TransportPDUPool();

    byte[] macTmpBuf;

    protected int pktSize;
    protected int padSize;
    protected int pktType;
    protected int totSizeCompressed   = 0;
    protected int totSizeUncompressed = 0;

    protected SSH2TransportPDU() {
        /* Factory instance constructor */
    }

    /**
     * Constructor which creates a prefabricated packet.
     *
     * @param pktType Type of packet to create.
     * @param bufSize How many bytes it should have room for.
     */
    protected SSH2TransportPDU(int pktType, int bufSize) {
        super(bufSize);
        this.pktType   = pktType;
        this.pktSize   = 0;
        this.padSize   = 0;
        this.macTmpBuf = new byte[128];
    }

    
    /**
     * Create a new PDU to use for incoming packets.
     *
     * @param bufSize How many bytes it should have room for.
     */
    protected SSH2TransportPDU createInPDU(int bufSize) {
        return factoryInstance.createPDU(0, bufSize);
    }

    /**
     * Create a new PDU for an outgoing packet.
     *
     * @param pktType Type of packet to create.
     * @param bufSize How many bytes it should have room for.
     */
    protected SSH2TransportPDU createOutPDU(int pktType, int bufSize) {
        return factoryInstance.createPDU(pktType, bufSize);
    }

    /**
     * Create a new PDU for a packet.
     *
     * @param pktType Type of packet to create.
     * @param bufSize How many bytes it should have room for.
     */
    protected SSH2TransportPDU createPDU(int pktType, int bufSize) {
        return new SSH2TransportPDU(pktType, bufSize);
    }

    /**
     * Register a factor which handles the creation and destruction of
     * incoming and outgoing packets.
     */
    public final static void setFactoryInstance(SSH2TransportPDU factory) {
        factoryInstance = factory;
    }

    /**
     * Have the factory create a PDU for an incoming packet.
     *
     * @param bufSize How many bytes it should have room for.
     */
    public final static SSH2TransportPDU createIncomingPacket(int bufSize) {
        return factoryInstance.createInPDU(bufSize);
    }

    /**
     * Have the factory create a PDU for an incoming packet.
     */
    public final static SSH2TransportPDU createIncomingPacket() {
        return createIncomingPacket(pktDefaultSize);
    }

    /**
     * Have the factory create a PDU for an outgoing packet.
     *
     * @param pktType Type of packet to create.
     * @param bufSize How many bytes it should have room for.
     */
    public final static SSH2TransportPDU createOutgoingPacket(int pktType,
                                                              int bufSize) {
        SSH2TransportPDU pdu = factoryInstance.createOutPDU(pktType, bufSize);
        pdu.writeInt(0);  // dummy sequence number
        pdu.writeInt(0);  // dummy length
        pdu.writeByte(0); // dummy pad-length
        pdu.writeByte(pktType);
        return pdu;
    }

    /**
     * Have the factory create a PDU for an outgoing packet.
     *
     * @param pktType Type of packet to create.
     */
    public final static SSH2TransportPDU createOutgoingPacket(int pktType) {
        return createOutgoingPacket(pktType, pktDefaultSize);
    }

    /**
     * Static function which creates a transceiver context with the
     * mentioned algorithms.
     *
     * @param cipherName Name of cipher algorithm to use.
     * @param cipherDiscard How many bytes to discard after key init.
     * @param macName Name of message authentication cipher to use.
     * @param macLength How many bytes of the mac digest to use
     * @param compName Name of compression algorithm to use.
     */
    public final static SSH2Transport.TransceiverContext
	createTransceiverContext(String cipherName, int cipherDiscard,
				String macName, int macLength,
				String compName, NonBlockingInput in)
        throws Exception {
        return factoryInstance.createTransceiverContextImpl
	    (cipherName, cipherDiscard, macName, macLength, compName, in);
    }

    /**
     * Release this PDU. This means that the PDU can be freed or
     * reused for another packet.
     */
    public void release() {}

    /**
     * Create a copy of this PDU.
     */
    public SSH2TransportPDU makeCopy() {
        SSH2TransportPDU copy = factoryInstance.createOutPDU(this.pktType,
                                this.data.length);
        System.arraycopy(this.data, 0, copy.data, 0, this.data.length);
        copy.pktSize = this.pktSize;
        copy.padSize = this.padSize;
        copy.rPos    = this.rPos;
        copy.wPos    = this.wPos;
        return copy;
    }

    public int getType() {
        return pktType;
    }

    public void setType(int pktType) {
        this.pktType = pktType;
    }

    /**
     * Get the length of the payload. The payload is the actual data
     * sent. Note that the payload may still be compressed.
     */
    public int getPayloadLength() {
        int plSz;
        if(pktSize == 0) {
            plSz = wPos - getPayloadOffset();
        } else {
            plSz = pktSize - padSize - 1;
        }
        return plSz;
    }

    /**
     * Get the offset in the data buffer where the payload starts.
     */
    public int getPayloadOffset() {
        return 4 + 4 + 1; // Skip sequence, length and padsize
    }

    private int bs;
    private int macSize;
    private int state;
    private int needbytes = 0;
    private SSH2Transport.TransceiverContext context;

    public int getCompressedSize() {
        return totSizeCompressed;
    }

    public int getUncompressedSize() {
        return totSizeUncompressed;
    }
    
    /**
     * Prepare this PDU for receiving data.
     *
     * @param seqNum Sequence number of packet.
     * @param context Transceiver context.
     *
     */
    public void initReceive(int seqNum,
                            SSH2Transport.TransceiverContext context) {
        writeInt(seqNum);  // Not received, used for MAC calculation
        rPos = 4;          // Skip it also (i.e. we don't want to read it)
        bs = 8;
        macSize = 0;
        this.context = context;
        state = STATE_READ_HEADER;

        if(context.cipher != null) {
            bs = context.cipher.getBlockSize();
            bs = (bs > 8 ? bs : 8);
        }
        prepareForRead(bs);
    }

    /**
     * Prepare the buffer for read. That is resize it if needed and
     * set the limit so that the read will read the correct number of
     * bytes.
     *
     * @param n number of additional bytes to read
     */
    private void prepareForRead(int n) {
        int odl = data.length;
        try {
            if((data.length - wPos) < n) {
                byte[] tmp   = data;
                int    newSz = data.length * 2;
                if (newSz < wPos || newSz - wPos < n) {
                    newSz = wPos + n + (wPos >>> 1);
                }
                data = new byte[newSz];
                System.arraycopy(tmp, 0, data, 0, tmp.length);
            }
        } catch (IndexOutOfBoundsException e) {
            //System.err.println("**************** prepareforread *****************");
            //System.err.println("odl="+odl+", wPos="+wPos+", ndl="+data.length);
            //System.err.println("*************************************************");
            throw new IndexOutOfBoundsException("odl="+odl+", wPos="+wPos+", ndl="+data.length);
        }
        wPos += n;
        needbytes = n;
    }

    public int getNeededBytes() {
        return needbytes;
    }

    public void fillData(byte[] buf, int bufpos, int n) {
        System.arraycopy(buf, bufpos, data, wPos - needbytes, n);
        needbytes -= n;
    }

    private final static int STATE_READ_HEADER = 0;
    private final static int STATE_READ_DATA   = 1;
    private final static int STATE_READ_MAC    = 2;
    /**
     * Read and decrypt data from the given stream. This
     * function handles decryption, mac checking and uncompression.
     *
     * When we get there we know that the data buffer holds at least
     * as many bytes as we previously prepared it for in prepareForRead
     *
     * @return True if more data is needed to create the
     *         entire packet, otherwise False
     */
    public boolean processData()
        throws SSH2Exception, ShortBufferException,
        GeneralSecurityException {
        switch(state) {
        case STATE_READ_HEADER:
            if (context.cipher != null) {
                context.cipher.update(data, 4, bs, data, 4); // Skip seqNum
            }

            bs -= 4; // The part of body pre-read above (subtract len-field)
            pktSize = readInt();

            if (context.mac != null) {
                macSize = context.getMacLength();
            }

            int totPktSz = (pktSize + 4 + macSize);
            if(totPktSz > PACKET_MAX_SIZE || totPktSz < PACKET_MIN_SIZE) {
                throw new SSH2CorruptPacketException("Invalid packet size: " +
                                                 pktSize);
            }
            state = STATE_READ_DATA;
            prepareForRead(pktSize - bs);
            return true;

        case STATE_READ_DATA:
            if (context.cipher != null) {
                context.cipher.update(data, 8 + bs,pktSize - bs, data,8 + bs);
            }

            if (context.mac != null) {
                state = STATE_READ_MAC;
                prepareForRead(macSize);
                return true;
            }
            // Fallthrough

        case STATE_READ_MAC:
            totSizeCompressed   = pktSize + 4;
            totSizeUncompressed = 4;
            if (context.mac != null) {
                checkMac(context.mac, macSize);
                totSizeCompressed   += macSize;
                totSizeUncompressed += macSize;
            }

            padSize = readByte();

            if (context.compressor != null) {
                // Update pktSize so getPayloadLength() calculates right value
                pktSize = context.compressor.uncompress(this,
                                                        pktSize - padSize - 1);
                pktSize += padSize + 1;
            }
            totSizeUncompressed += pktSize;

            pktType = readByte();
            break;
        }
        return false;
    }

    protected void checkMac(Mac mac, int macSize)
        throws SSH2MacCheckException, GeneralSecurityException {
        mac.update(data, 0, 8 + pktSize);
        try {
            mac.doFinal(macTmpBuf, 0);
        } catch (GeneralSecurityException e) {
            throw new GeneralSecurityException(e.getMessage() + " tmp " + macTmpBuf.length);
        }
        int dOff = 8 + pktSize;

        for(int i = 0; i < macSize; i++) {
            if(macTmpBuf[i] != data[dOff++]) {
                throw new SSH2MacCheckException("MAC check failed (" + mac.getAlgorithm() + ")");
            }
        }
    }

    /**
     * Ensure that the buffer has room for at least n more bytes.
     *
     * @param n Number of bytes we need room for
     */
    private void ensureSize(int n) {
        if((data.length - wPos) < n) {
            byte[] tmp = data;
            int  newSz;
            if (wPos+n < 1024 && wPos+n < data.length*2) {
                newSz = data.length * 2;
            } else {
                newSz = wPos + n + 1024;
            }
            data = new byte[newSz];
            System.arraycopy(tmp, 0, data, 0, tmp.length);
        }
    }

    public final void writeByte(int b) {
        ensureSize(1);
        data[wPos++] = (byte)b;
    }

    public final void writeString(byte[] str, int off, int len) {
        ensureSize(len + 4);
        super.writeString(str, off, len);
    }

    public final void writeRaw(byte[] raw, int off, int len) {
        ensureSize(len);
        super.writeRaw(raw, off, len);
    }

    /**
     * Encrypts and writes an outgoing packet to the stream. This
     * function handles compression, mac calculation and encryption.
     *
     * @param out     Stream to write resulting data to.
     * @param seqNum  Sequence number of packet.
     * @param context Transceiver context to use.
     * @param rand    An object from which random numbers and padding
     *                data is read.
     */
    public void writeTo(NonBlockingOutput out, int seqNum,
                        SSH2Transport.TransceiverContext context,
                        SecureRandomAndPad rand)
    throws IOException, GeneralSecurityException, SSH2CompressionException {
        int macSize = 0;
        int bs      = 8;
        int ubytes  = 0;

        if(context.compressor != null) {
            int ow = wPos;
            context.compressor.compress(this);
            ubytes = ow - wPos;
        }

        if(context.cipher != null) {
            bs = context.cipher.getBlockSize();
            bs = (bs > 8 ? bs : 8);
        }

        // Subtract dummy sequence number since it is not sent
        //
        padSize = bs - ((wPos - 4) % bs);
        if(padSize < 4)
            padSize += bs;

        // sequence + length fields not counted in packet-length
        //
        pktSize = wPos + padSize - 8;
        ensureSize(pktSize+4);
        rand.nextPadBytes(data, wPos, padSize);

        wPos = 0;
        writeInt(seqNum); // Not transmitted, used for MAC calculation
        writeInt(pktSize);
        writeByte(padSize);
        int totPktSz = pktSize + 4; // packet size including length field

        if(context.mac != null) {
            // The MAC is calculated on full packet including sequence number
            //
            int macOffset = 4 + totPktSz;
            ensureSize(macOffset + context.getMacLength());
            context.mac.update(data, 0, macOffset);
            try {
                context.mac.doFinal(data, macOffset);
            } catch (GeneralSecurityException e) {
                throw new GeneralSecurityException(e.getMessage() + " data " + data.length + " " + macOffset);
            }
            macSize = context.getMacLength();
        }

        if (context.cipher != null) {
            context.cipher.update(data, 4, totPktSz, data, 4);
        }

        out.write(data, 4, totPktSz + macSize);
        
        totSizeCompressed = totPktSz + macSize;
        totSizeUncompressed = totSizeCompressed + ubytes;
        
        release();
    }

    /**
     * Creates a string representation of this PDU.
     */
    public String toString() {
        return "pdu: buf-sz = " + data.length +
               ", rPos = " + rPos +
               ", wPos = " + wPos +
               ", pktSize = " + pktSize +
               ", padSize = " + padSize +
               ", pktType = " + pktType;
    }

    /**
     * An implementation which actually creates a transceiver context.
     *
     * @param cipherName Name of cipher algorithm to use.
     * @param cipherDiscard How many bytes to discard after key init.     
     * @param macName Name of message authentication cipher to use.
     * @param macLength How many bytes of the mac digest to use
     * @param compName Name of compression algorithm to use.
     */
    public SSH2Transport.TransceiverContext
	createTransceiverContextImpl(String cipherName, int cipherDiscard,
				    String macName, int macLength,
				    String compName, NonBlockingInput in)
    throws Exception {
        SSH2Transport.TransceiverContext ctx =
            new SSH2Transport.TransceiverContext(in);

        if(!"none".equals(cipherName)) {
            ctx.cipher = com.mindbright.util.Crypto.getCipher(cipherName);
        }
        if(!"none".equals(macName)) {
            ctx.mac = com.mindbright.util.Crypto.getMac(macName);
        }
        if(!"none".equals(compName)) {
            ctx.compressor = SSH2Compressor.getInstance(compName);
        }
	ctx.maclength = macLength;
	ctx.discard   = cipherDiscard;
        return ctx;
    }
}
