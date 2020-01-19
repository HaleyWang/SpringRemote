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

package com.mindbright.ssh;

import java.io.*;

import com.mindbright.util.*;

import com.mindbright.nio.NonBlockingOutput;

public final class SSHPduOutputStream extends SSHDataOutputStream
    implements SSHPdu {

    public static final class PduByteArrayOutputStream extends ByteArrayOutputStream {
        PduByteArrayOutputStream() {
            super();
        }

        PduByteArrayOutputStream(int size) {
            super(size);
        }

        PduByteArrayOutputStream(byte[] buf) {
            this.buf = buf;
        }

        public byte[] getBuf() {
            return buf;
        }

        public int getCount() {
            return count;
        }

        public void setBuf(byte[] buf) {
            this.buf = buf;
        }

        public void setCount(int count) {
            this.count = count;
        }
    }

    public static final int SSH_DEFAULT_PKT_LEN = 8192;
    public static int mtu = SSH_DEFAULT_PKT_LEN;

    public static synchronized void setMTU(int newMtu) {
        mtu = newMtu;
    }

    byte[]  readFromRawData;
    int     readFromOff;
    int     readFromSize;

    public int                type;
    public SSHCipher          cipher;
    public SSHCompressor      compressor;
    public SecureRandomAndPad rand;

//     SSHPduOutputStream(SSHCipher cipher, SSHCompressor compressor,
//                        SecureRandomAndPad rand) {
//         super(null);
//         this.cipher     = cipher;
//         this.compressor = compressor;
//         this.rand       = rand;
//     }

    SSHPduOutputStream(int type, SSHCipher cipher, SSHCompressor compressor,
                       SecureRandomAndPad rand)
    throws IOException {
        super(new PduByteArrayOutputStream(mtu));
        this.type       = type;
        this.cipher     = cipher;
        this.compressor = compressor;
        this.rand       = rand;
        if(cipher != null) {
            PduByteArrayOutputStream bytes = (PduByteArrayOutputStream)out;
            rand.nextPadBytes(bytes.getBuf(), 4, 12);
            bytes.setCount(12);
        } else {
            for(int i = 0; i < 12; i++)
                write(0);
        }
        write(type);
    }

    public SSHPdu createPdu() throws IOException {
        SSHPdu pdu;
        pdu = new SSHPduOutputStream(this.type, this.cipher, this.compressor,
                                     this.rand);
        return pdu;
    }

    public void readFrom(InputStream in) throws IOException {
        if(type != SSH.MSG_CHANNEL_DATA &&
                type != SSH.CMSG_STDIN_DATA)
            throw new IOException("Trying to read raw data into non-data PDU");

        PduByteArrayOutputStream bytes = (PduByteArrayOutputStream) out;

        readFromRawData = bytes.getBuf();
        readFromOff     = bytes.size() + 4; // Leave space for size

        readFromSize = in.read(readFromRawData, readFromOff, mtu - readFromOff);
        if(readFromSize == -1)
            throw new IOException("EOF");

        writeInt(readFromSize);
        bytes.setCount(readFromOff + readFromSize);
    }


    /*
      byte[4]   length   (type+data+crc)
      byte[1-8] padding  (8-length%8)
      byte[1]   type
      byte[*]   data
      byte[4]   crc
    */


    public void writeTo(NonBlockingOutput sshOut) throws IOException {
        PduByteArrayOutputStream bytes = (PduByteArrayOutputStream) out;
 
        if (compressor != null) {
            compressor.compress(this);
        }

        int length = bytes.size() - 12 + 4; // 12 = our preallocated hdr size (for length+padding)
        int padlen = 8 - (length%8);
        int padstart = 12 - padlen;
        int crc32 = (int)CRC32.getValue(bytes.getBuf(), padstart, padlen + length - 4);  
        writeInt(crc32);

        byte[] data = bytes.getBuf();

        if(cipher != null) {
            cipher.encrypt(data, padstart, data, padstart, padlen + length);
        }

        data[padstart-4] = (byte) ((length >>> 24) & 0xff);
        data[padstart-3] = (byte) ((length >>> 16) & 0xff);
        data[padstart-2] = (byte) ((length >>> 8) & 0xff);
        data[padstart-1] = (byte) (length & 0xff);

        sshOut.write(data, padstart - 4, length + padlen + 4);
        sshOut.flush();
    }

    public void writeTo(OutputStream sshOut) throws IOException {
        PduByteArrayOutputStream bytes = (PduByteArrayOutputStream) out;

        if (SSH.DEBUGPKG) {
            SSH.logDebug("Writing " + SSH.msgTypeString(type));
        }
        /*
          byte[4]   length   (type+data+crc)
          byte[1-8] padding  (8-length%8)
          byte[1]   type
          byte[*]   data
          byte[4]   crc
         */

        if (compressor != null) {
            compressor.compress(this);
        }

        int length = bytes.size() - 12 + 4; // 12 = our preallocated hdr size (for length+padding)
        int padlen = 8 - (length%8);
        int padstart = 12 - padlen;
        int crc32 = (int)CRC32.getValue(bytes.getBuf(), padstart, padlen + length - 4);  
        writeInt(crc32);

        byte[] data = bytes.getBuf();

        if(cipher != null) {
            cipher.encrypt(data, padstart, data, padstart, padlen + length);
        }

        data[padstart-4] = (byte) ((length >>> 24) & 0xff);
        data[padstart-3] = (byte) ((length >>> 16) & 0xff);
        data[padstart-2] = (byte) ((length >>> 8) & 0xff);
        data[padstart-1] = (byte) (length & 0xff);

        sshOut.write(data, padstart - 4, length + padlen + 4);
        sshOut.flush();
    }

    public PduByteArrayOutputStream getOut() {
        return (PduByteArrayOutputStream)out;
    }

    public byte[] rawData() {
        return readFromRawData;
    }
    public void rawSetData(byte[] raw) {}
    public int rawOffset() {
        return readFromOff;
    }
    public int rawSize() {
        // !!! return readFromSize;
        //
        byte[] bytes = readFromRawData;
        int    off   = readFromOff - 4;
        int ch1 = ((bytes[off++] + 256) & 0xff);
        int ch2 = ((bytes[off++] + 256) & 0xff);
        int ch3 = ((bytes[off++] + 256) & 0xff);
        int ch4 = ((bytes[off]   + 256) & 0xff);
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }
    public void rawAdjustSize(int size) {
        PduByteArrayOutputStream bytes = (PduByteArrayOutputStream) out;
        bytes.setCount(size);
    }

}

