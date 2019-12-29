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

import java.io.OutputStream;
import java.math.BigInteger;

/**
 * Utility routines for creating readable dumps of binary data.
 */
public class HexDump {

    /* hexadecimal digits.
     */
    private static final char[] HEX_DIGITS = {
        '0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'
    };

    /**
     * Returns a string of 8 hexadecimal digits (most significant
     * digit first) corresponding to the given integer
     *
     * @param n an unsigned integer
     */
    public static String intToString (int n) {
        char[] buf = new char[8];
        for(int i = 7; i >= 0; i--) {
            buf[i] = HEX_DIGITS[n & 0x0F];
            n >>>= 4;
        }
        return new String(buf);
    }

    /**
     * Returns a string of hexadecimal digits from a byte array. Each
     * byte is converted to 2 hex symbols.
     *
     * @param ba array to convert
     *
     * @return the corresponding hex-number
     */
    public static String toString(byte[] ba) {
        return toString(ba, 0, ba.length);
    }

    /**
     * Returns a string of hexadecimal digits from a byte array. Each
     * byte is converted to 2 hex symbols.
     *
     * @param ba array containing data to convert
     * @param offset offset of first byte to convert
     * @param length number of bytes to convert
     *
     * @return the corresponding hex-number
     */
    public static String toString(byte[] ba, int offset, int length) {
        char[] buf = new char[length * 2];
        for(int i = offset, j = 0, k; i < offset+length; ) {
            k = ba[i++];
            buf[j++] = HEX_DIGITS[(k >>> 4) & 0x0F];
            buf[j++] = HEX_DIGITS[ k      & 0x0F];
        }
        return new String(buf);
    }

    public static String toAsciiString(byte[] ba) {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<ba.length; i++) {
            if (ba[i] >= 32) {
                sb.append((char)ba[i]);
            } else {
                sb.append('.');
            }
        }
        return sb.toString();
    }

    public static byte[] toByteArray(String s) {
        int len = s.length() / 2;
        byte[] buf = new byte[len];
        for (int i=0; i<len; i++) {
            int c1 = s.charAt(i*2);
            int c2 = s.charAt(i*2+1);
            buf[i] = 0;
            if (c1 >= 'a' && c1 <= 'f') c1 = 10 + (c1 - 'a');
            else c1 -= '0';
            if (c2 >= 'a' && c2 <= 'f') c2 = 10 + (c2 - 'a');
            else c2 -= '0';
            buf[i] = (byte)(c1*16 + c2);
        }
        return buf;
    }
    
    /**
     * Format an integer to a hex string with a minimum length
     *
     * @param i integer to convert
     * @param sz minimum number of digits in output.
     */
    public static String formatHex(int i, int sz) {
        String str = Integer.toHexString(i);
        while(str.length() < sz) {
            str = "0" + str;
        }
        return str;
    }

    public static void print(byte[] buf, boolean asjavaarr) {
        print(System.err, null, !asjavaarr, asjavaarr, buf, 0, buf.length);
    }
    

    /**
     * Print a nice looking hex-dump of the given data on System.err
     *
     * @param buf array containing data to dump
     * @param off offset of fist byte to dump
     * @param len number of bytes to dump
     */
    public static void print(byte[] buf, int off, int len) {
        print(System.err, null, true, buf, off, len);
    }

    /**
     * Print a nice looking hex-dump of the given data to the given stream.
     *
     * @param out stream to output the hex-dump on
     * @param buf array containing data to dump
     * @param off offset of fist byte to dump
     * @param len number of bytes to dump
     */
    public static void print(OutputStream out, byte[] buf, int off, int len) {
        print(out, null, true, buf, off, len);
    }


    /**
     * Print a nice looking hex-dump of the given data on System.err
     *
     * @param header header to print before the actual dump
     * @param showAddr if true the offset from the start is shown
     *                 first on every line.
     * @param buf array containing data to dump
     * @param off offset of fist byte to dump
     * @param len number of bytes to dump
     */
    public static void print(String header, boolean showAddr,
                             byte[] buf, int off, int len) {
        print(System.err, header, showAddr, buf, off, len);
    }

    /**
     * Generate and print a nice looking hex-dump of the given data.
     *
     * @param out stream to output the hex-dump on
     * @param header header to print before the actual dump
     * @param showAddr if true the offset from the start is shown
     *                 first on every line.
     * @param buf array containing data to dump
     * @param off offset of fist byte to dump
     * @param len number of bytes to dump
     */
    public static synchronized void print(OutputStream out,
                                          String header, boolean showAddr,
                                          byte[] buf, int off, int len) {
        print(out, header, showAddr, false, buf, off, len);
    }
    
    public static synchronized void print(OutputStream out,
                                          String header, boolean showAddr, boolean javaarr, 
                                          byte[] buf, int off, int len) {
        int i, j, jmax;
        int c;

        if (header != null) {
            try {
                out.write(header.getBytes());
                out.write("\r\n".getBytes());
            } catch (Exception e) { /* Do nothing */
            }
        }

        for(i = 0; i < len; i += 0x10) {
            StringBuilder line = new StringBuilder();

            if(showAddr) {
                line.append(formatHex(i + off, 8));
                line.append(": ");
            }

            jmax = len - i;
            jmax = jmax > 16 ? 16 : jmax;

            for(j = 0; j < jmax; j++) {
                c = (buf[off+i+j] + 0x100) % 0x100;
                if (javaarr) line.append("(byte)0x");
                line.append(formatHex(c, 2));
                if (javaarr) 
                    line.append(", ");
                else if ((j % 2) != 0)
                    line.append(" ");
            }

            for(; j < 16; j++) {
                line.append("  ");
                if ((j % 2) != 0)
                    line.append(" ");
            }

            if (!javaarr) {
                line.append(" ");
                for(j = 0; j < jmax; j++) {
                    c = (buf[off+i+j] + 0x100) % 0x100;
                    c = c < 32 || c >= 127 ? '.' : c;
                    line.append((char)c);
                }
            }
            
            try {
                out.write(line.toString().getBytes());
                out.write("\r\n".getBytes());
            } catch (Exception e) { /* Do nothing */
            }
        }
    }

    /**
     * Dump the given array to System.err
     *
     * @param buf array to dump, may be null.
     */
    public static void print(byte[] buf) {
        if(buf == null) {
            System.err.println("<null>");
            return;
        }
        print(buf, 0, buf.length);
    }

    /**
     * Print an hex-dump of the given big integer to System.err
     */
    public static void print(BigInteger bi) {
        byte[] raw = bi.toByteArray();
        if(raw.length == 1 && raw[0] == (byte)0x00)
            raw = new byte[0];
        print(raw);
    }


}
