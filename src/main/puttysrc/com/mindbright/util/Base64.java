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

package com.mindbright.util;

/**
 * Static routines for base64-encoding/decoding
 */
public final class Base64 {

    private final static int[] fromBase64 = {
        -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, 62, -1, -1, -1, 63,
        52, 53, 54, 55, 56, 57, 58, 59,
        60, 61, -1, -1, -1, -1, -1, -1,
        -1,  0,  1,  2,  3,  4,  5,  6,
        7,  8,  9, 10, 11, 12, 13, 14,
        15, 16, 17, 18, 19, 20, 21, 22,
        23, 24, 25, -1, -1, -1, -1, -1,
        -1, 26, 27, 28, 29, 30, 31, 32,
        33, 34, 35, 36, 37, 38, 39, 40,
        41, 42, 43, 44, 45, 46, 47, 48,
        49, 50, 51, -1, -1, -1, -1, -1
    };

    private final static byte[] toBase64 = {
        // 'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P',
        65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80,
        // 'Q','R','S','T','U','V','W','X','Y','Z','a','b','c','d','e','f',
        81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 97, 98, 99,100,101,102,
        // 'g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v',
        103,104,105,106,107,108,109,110,111,112,113,114,115,116,117,118,
        // 'w','x','y','z','0','1','2','3','4','5','6','7','8','9','+','/'
        119,120,121,122, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 43, 47, (byte)'='
    };

    private final static int PAD_CHAR = 64;

    /**
     * Encode a binary blob of data
     *
     * @param data the data to encode
     * @return an array of base64-encoded data
     */
    public static byte[] encode(byte[] data) {
        return encode(data, 0, data.length);
    }

    /**
     * Encode a binary blob of data
     *
     * @param data array containing the data to encode
     * @param offset offset of start of data in array
     * @param length how many bytes of data to encode
     * @return an array of base64-encoded data
     */
    public static byte[] encode(byte[] data, int offset, int length) {
        byte[] encoded;
        int x1, x2, x3, x4;
        int i, j, r, n;

        r       = length % 3;
        n       = offset + (length - r);
        encoded = new byte[((length / 3) * 4) + (r != 0 ? 4 : 0)];

        for(i = offset, j = 0; i < n; i += 3, j += 4) {
            x1 = (data[i] & 0xfc) >> 2;
            x2 = (((data[i    ] & 0x03) << 4) | ((data[i + 1] & 0xf0) >> 4));
            x3 = (((data[i + 1] & 0x0f) << 2) | ((data[i + 2] & 0xc0) >> 6));
            x4 = (data[i + 2] & 0x3f);
            encoded[j    ] = toBase64[x1];
            encoded[j + 1] = toBase64[x2];
            encoded[j + 2] = toBase64[x3];
            encoded[j + 3] = toBase64[x4];
        }

        if(r != 0) {
            x1 = (data[i] & 0xfc) >> 2;
            x2 = (data[i] & 0x03) << 4;
            x3 = PAD_CHAR;
            x4 = PAD_CHAR;
            if(r == 2) {
                x2 |= ((data[i + 1] & 0xf0) >> 4);
                x3 = (data[i + 1] & 0x0f) << 2;
            }
            encoded[j++] = toBase64[x1];
            encoded[j++] = toBase64[x2];
            encoded[j++] = toBase64[x3];
            encoded[j++] = toBase64[x4];
        }

        return encoded;
    }

    /**
     * Decode a given base64-blob
     *
     * @param data the base64-encoded blob to decode
     * @return the decoded data
     */
    public static byte[] decode(byte[] data) {
        return decode(data, 0, data.length);
    }

    /**
     * Decode a given base64-blob
     *
     * @param encoded array containing the base64-encoded blob to
     *                decode
     * @param offset offset of data to decode in array
     * @param length length of data to decode
     * @return the decoded data
     */
    public static byte[] decode(byte[] encoded, int offset, int length) {
        byte[] data;
        int i, j, n, v = 0, c, bits = 0;

        n    = offset + length;
        data = new byte[(length * 3) / 4];

        for(i = offset, j = 0; i < n; i++) {
            c = fromBase64[encoded[i]];
            if(c < 0) {
                if(encoded[i] == toBase64[PAD_CHAR])
                    break;
                continue;
            }
            v = (v << 6) | c;
            bits += 6;
            if(bits >= 8) {
                bits -= 8;
                data[j++] = (byte) ((v >> bits) & 0xff);
            }
        }

        if(data.length > j) {
            byte[] tmp = new byte[j];
            System.arraycopy(data, 0, tmp, 0, j);
            data = tmp;
        }

        return data;
    }

}
