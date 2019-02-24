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

import java.math.BigInteger;


/**
 * This class implements a read/write buffer with all protocol specific
 * formatting (as defined in the architecture spec.). It is mainly used in the
 * form of a protocol data unit (derived class <code>SSH2TransportPDU</code>).
 */
public class SSH2DataBuffer {
    public final static int BOOLEAN_TRUE  = 1;
    public final static int BOOLEAN_FALSE = 0;

    // UTF8-related constants
    private static int UTF8_TWO_BYTE_ENCODING        = 0xC0;
    private static int UTF8_THREE_BYTE_ENCODING      = 0xE0;
    private static int UTF8_FOUR_BYTE_ENCODING       = 0xF0;
    private static int UTF8_FIVE_BYTE_ENCODING       = 0xF8;
    private static int UTF8_SIX_BYTE_ENCODING        = 0xFC;
    private static int UTF8_EXTENDED_BYTE_ENCODING   = 0x80;



    protected byte[] data;
    protected int    rPos;
    protected int    wPos;

    protected SSH2DataBuffer() {
        this(0);
    }

    public SSH2DataBuffer(int bufSize) {
        this.data = new byte[bufSize];
        reset();
    }

    public final void resize(int newsize) {
        if (newsize <= data.length)
            return;
        
        byte[] d = new byte[newsize];
        System.arraycopy(data, 0, d, 0, data.length);
        data = d;
    }

    public final void reset() {
        this.rPos = 0;
        this.wPos = 0;
    }

    public final int getMaxSize() {
        return data.length;
    }

    public final byte[] getData() {
        return data;
    }

    public final void setData(byte[] data) {
        this.data = data;
    }

    public final void setWPos(int wPos) {
        this.wPos = wPos;
    }

    public final int getWPos() {
        return wPos;
    }

    public final void setRPos(int rPos) {
        this.rPos = rPos;
    }

    public final int getRPos() {
        return rPos;
    }

    public final int getMaxReadSize() {
        return wPos - rPos;
    }

    public final int getMaxWriteSize() {
        return data.length - wPos;
    }

    public final int readByte() {
        return (data[rPos++]) & 0xff;
    }

    public void writeByte(int b) {
        data[wPos++] = (byte)b;
    }

    public final boolean readBoolean() {
        if(readByte() != BOOLEAN_FALSE)
            return true;
        return false;
    }

    public final void writeBoolean(boolean b) {
        if(b) {
            writeByte(BOOLEAN_TRUE);
        } else {
            writeByte(BOOLEAN_FALSE);
        }
    }

    public final int readInt() {
        int b1 = readByte();
        int b2 = readByte();
        int b3 = readByte();
        int b4 = readByte();
        return ((b1 << 24) + (b2 << 16) + (b3 << 8) + (b4 << 0));
    }

    public final long readUInt() {
        long b1 = readByte();
        long b2 = readByte();
        long b3 = readByte();
        long b4 = readByte();
        return ((b1 << 24) + (b2 << 16) + (b3 << 8) + (b4 << 0));
    }

    public final void writeInt(int i) {
        writeByte((i >>> 24) & 0xFF);
        writeByte((i >>> 16) & 0xFF);
        writeByte((i >>>  8) & 0xFF);
        writeByte((i >>>  0) & 0xFF);
    }

    public final void writeInt(long i) {
        writeByte((int)((i >>> 24) & 0xFF));
        writeByte((int)((i >>> 16) & 0xFF));
        writeByte((int)((i >>>  8) & 0xFF));
        writeByte((int)((i >>>  0) & 0xFF));
    }

    public final long readLong() {
        long b1 = readByte();
        long b2 = readByte();
        long b3 = readByte();
        long b4 = readByte();
        long b5 = readByte();
        long b6 = readByte();
        long b7 = readByte();
        long b8 = readByte();
        return ((b1 << 56) + (b2 << 48) + (b3 << 40) + (b4 << 32) +
                (b5 << 24) + (b6 << 16) + (b7 << 8) + (b8 << 0));
    }

    public final void writeLong(long l) {
        writeByte((int)((l >>> 56) & 0xFF));
        writeByte((int)((l >>> 48) & 0xFF));
        writeByte((int)((l >>> 40) & 0xFF));
        writeByte((int)((l >>> 32) & 0xFF));
        writeByte((int)((l >>> 24) & 0xFF));
        writeByte((int)((l >>> 16) & 0xFF));
        writeByte((int)((l >>>  8) & 0xFF));
        writeByte((int)((l >>>  0) & 0xFF));
    }

    public final BigInteger readBigInt() {
        byte[] raw = readString();
        if(raw.length > 0)
            return new BigInteger(raw);
        return BigInteger.valueOf(0);
    }

    public final BigInteger readBigIntBits() {
        int    bits = readInt();
        byte[] raw  = new byte[(bits + 7) / 8 + 1];

        raw[0] = 0;
        readRaw(raw, 1, raw.length - 1);
        return new BigInteger(raw);
    }

    public final void writeBigInt(BigInteger bi) {
        byte[] raw = bi.toByteArray();
        if(raw.length == 1 && raw[0] == (byte)0x00)
            raw = new byte[0];
        writeString(raw);
    }

    public final void writeBigIntBits(BigInteger bi) {
        int    bytes = ((bi.bitLength() + 7) / 8);
        byte[] raw   = bi.toByteArray();
        if(raw.length == 1 && raw[0] == (byte)0x00) {
            writeInt(0);
            return;
        }
        writeInt(bi.bitLength());
        if(raw[0] == 0) {
            writeRaw(raw, 1, bytes);
        } else {
            writeRaw(raw, 0, bytes);
        }
    }


    public final String readJavaString() {
        int len = readInt();
        if(len < 0 || len > (data.length - rPos)) {
            throw new Error("Error in SSH2DataBuffer, corrupt string on read");
        }
        String ret = new String(data, rPos, len);
        rPos += len;
        return ret;
    }

    public final byte[] readString() {
        int len = readInt();
        if(len < 0 || len > (data.length - rPos)) {
            throw new Error("Error in SSH2DataBuffer, corrupt string on read");
        }
        byte[] str = new byte[len];
        System.arraycopy(data, rPos, str, 0, len);
        rPos += len;
        return str;
    }

    public final int readString(byte[] str, int off) {
        int len = readInt();
        System.arraycopy(data, rPos, str, off, len);
        rPos += len;
        return len;
    }

    public final void writeString(String str) {
        writeString(str.getBytes());
    }

    public final void writeString(byte[] str) {
        writeString(str, 0, str.length);
    }

    public void writeString(byte[] str, int off, int len) {
        writeInt(len);
        System.arraycopy(str, off, data, wPos, len);
        wPos += len;
    }

    public final void writeUTF8String(String str) {
        int len = str.length();
        int p = 0;
        byte[] t = new byte[6 * len];
        /*
         * U-00000000 U-0000007F: 0xxxxxxx
         * U-00000080 U-000007FF: 110xxxxx 10xxxxxx
         * U-00000800 U-0000FFFF: 1110xxxx 10xxxxxx 10xxxxxx
         * U-00010000 U-001FFFFF: 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
         * U-00200000 U-03FFFFFF: 111110xx 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx
         * U-04000000 U-7FFFFFFF: 1111110x 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx 10xxxxxx
         */
        for (int i = 0; i < len; i++) {
            long chVal = str.charAt(i);

            if (       (chVal >= 0x00000000) && (chVal <= 0x0000007F)) {
                t[p++] = (byte)chVal;
            } else if ((chVal >= 0x00000080) && (chVal <= 0x000007FF)) {
                t[p++] = (byte) (UTF8_TWO_BYTE_ENCODING |
                                       ((chVal >> 6)  & 0x1F));
                t[p++] = (byte) (UTF8_EXTENDED_BYTE_ENCODING |
                                        (chVal        & 0x3F));
            } else if ((chVal >= 0x00000800) && (chVal <= 0x0000FFFF)) {
                t[p++] = (byte) (UTF8_THREE_BYTE_ENCODING |
                                       ((chVal >> 12) & 0x0F));
                t[p++] = (byte) (UTF8_EXTENDED_BYTE_ENCODING |
                                       ((chVal >>  6) & 0x3F));
                t[p++] = (byte) (UTF8_EXTENDED_BYTE_ENCODING |
                                       (chVal         & 0x3F));
            } else if ((chVal >= 0x00010000) && (chVal <= 0x001FFFFF)) {
                t[p++] = (byte) (UTF8_FOUR_BYTE_ENCODING |
                                       ((chVal >> 18) & 0x07));
                t[p++] = (byte) (UTF8_EXTENDED_BYTE_ENCODING |
                                       ((chVal >> 12) & 0x3F));
                t[p++] = (byte) (UTF8_EXTENDED_BYTE_ENCODING |
                                       ((chVal >> 6)  & 0x3F));
                t[p++] = (byte) (UTF8_EXTENDED_BYTE_ENCODING |
                                       (chVal         & 0x3F));
            } else if ((chVal >= 0x00200000) && (chVal <= 0x03FFFFFF)) {
                t[p++] = (byte) (UTF8_FIVE_BYTE_ENCODING |
                                       ((chVal >> 24) & 0x03));
                t[p++] = (byte) (UTF8_EXTENDED_BYTE_ENCODING |
                                       ((chVal >> 18) & 0x3F));
                t[p++] = (byte) (UTF8_EXTENDED_BYTE_ENCODING |
                                       ((chVal >> 12) & 0x3F));
                t[p++] = (byte) (UTF8_EXTENDED_BYTE_ENCODING |
                                       ((chVal >> 6)  & 0x3F));
                t[p++] = (byte) (UTF8_EXTENDED_BYTE_ENCODING |
                                       (chVal         & 0x3F));
            } else if ((chVal >= 0x04000000) && (chVal <= 0x7FFFFFFF)) {
                t[p++] = (byte) (UTF8_SIX_BYTE_ENCODING |
                                       ((chVal >> 30) & 0x01));
                t[p++] = (byte) (UTF8_EXTENDED_BYTE_ENCODING |
                                       ((chVal >> 24) & 0x3F));
                t[p++] = (byte) (UTF8_EXTENDED_BYTE_ENCODING |
                                       ((chVal >> 18) & 0x3F));
                t[p++] = (byte) (UTF8_EXTENDED_BYTE_ENCODING |
                                       ((chVal >> 12) & 0x3F));
                t[p++] = (byte) (UTF8_EXTENDED_BYTE_ENCODING |
                                       ((chVal >> 6)  & 0x3F));
                t[p++] = (byte) (UTF8_EXTENDED_BYTE_ENCODING |
                                       (chVal         & 0x3F));
            }
        }
        writeString(t, 0, p);
    }


    public final byte[] readRestRaw() {
        return readRaw(wPos - rPos);
    }

    public final byte[] readRaw(int len) {
        byte[] raw = new byte[len];
        readRaw(raw, 0, len);
        return raw;
    }

    public final void readRaw(byte[] raw, int off, int len) {
        System.arraycopy(data, rPos, raw, off, len);
        rPos += len;
    }

    public final void writeRaw(byte[] raw) {
        writeRaw(raw, 0, raw.length);
    }

    public void writeRaw(byte[] raw, int off, int len) {
        System.arraycopy(raw, off, data, wPos, len);
        wPos += len;
    }
}
