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
 * Static utility functions for trimming strings and converting byte
 * counts to strings.
 */
public final class StringUtil {

    static final String[] byteUnits = { "Bytes", "kB", "MB", "GB", "TB" };

    /**
     * Trims space from the start of a string.
     *
     * @param str string to trim space characters from
     *
     * @return a new string without any leading space characters
     */
    public static String trimLeft(String str) {
        char[] val = str.toCharArray();
        int st = 0;
        while ((st < val.length) && (val[st] <= ' ')) {
            st++;
        }
        return str.substring(st);
    }

    /**
     * Trims space from the end of a string.
     *
     * @param str string to trim space characters from
     *
     * @return a new string without any trailing space characters
     */
    public static String trimRight(String str) {
        char[] val = str.toCharArray();
        int    end = val.length;
        while ((end > 0) && (val[end - 1] <= ' ')) {
            end--;
        }
        return str.substring(0, end);
    }

    /**
     * Converts a byte count to a readable string with an unit . If
     * the byte count contains too many digits the unit is upgraded to
     * Kb, MB, GB or TB until the number fits withing the given number
     * of digits. For example converting 1024 with digits=4
     * gives "1024 Bytes: but with digits=3 it becomes "1 kB".
     *
     * @param nBytes the byte count to print
     * @param digits how many digits the printout may contain. Note
     *        that this count does not include the unit length
     *
     * @return a formatted string
     */
    public static String nBytesToString(long nBytes, int digits) {
        int ix = 0;
        int thresh = 10;
        while(--digits > 0) {
            thresh *= 10;
        }
        while(nBytes >= thresh) {
            ix++;
            nBytes >>>= 10;
        }
        return nBytes + " " + byteUnits[ix];
    }

}

