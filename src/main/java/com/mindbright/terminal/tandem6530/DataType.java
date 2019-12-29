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

package com.mindbright.terminal.tandem6530;

public class DataType {
    public static final int FREE                 = 0;
    public static final int ALPHA                = 1;
    public static final int NUM                  = 2;
    public static final int ALPHA_NUM            = 3;
    public static final int FULL_NUM             = 4;
    public static final int FULL_NUM_WITH_SPACE  = 5;
    public static final int ALPHA_WITH_SPACE     = 6;
    public static final int ALPHA_NUM_WITH_SPACE = 7;

    public static final byte TYPE_0 = (byte) 0x01;
    public static final byte TYPE_1 = (byte) 0x02;
    public static final byte TYPE_2 = (byte) 0x04;
    public static final byte TYPE_3 = (byte) 0x08;
    public static final byte TYPE_4 = (byte) 0x10;
    public static final byte TYPE_5 = (byte) 0x20;
    public static final byte TYPE_6 = (byte) 0x40;
    public static final byte TYPE_7 = (byte) 0x80;

    protected byte dataTypes[] = { TYPE_0, TYPE_1, TYPE_2, TYPE_3,
                                   TYPE_4, TYPE_5, TYPE_6, TYPE_7 };
    protected byte table[];
    // This is the table D-2:
    protected byte defaultTable[] = {
                                        (byte) 0xe1, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x31,
                                        (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01,
                                        (byte) 0x01, (byte) 0x31, (byte) 0x31, (byte) 0x31, (byte) 0x31,
                                        (byte) 0x01, (byte) 0xbd, (byte) 0xbd, (byte) 0xbd, (byte) 0xbd,
                                        (byte) 0xbd, (byte) 0xbd, (byte) 0xbd, (byte) 0xbd, (byte) 0xbd,
                                        (byte) 0xbd, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01,
                                        (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0xcb, (byte) 0xcb,
                                        (byte) 0xcb, (byte) 0xcb, (byte) 0xcb, (byte) 0xcb, (byte) 0xcb,
                                        (byte) 0xcb, (byte) 0xcb, (byte) 0xcb, (byte) 0xcb, (byte) 0xcb,
                                        (byte) 0xcb, (byte) 0xcb, (byte) 0xcb, (byte) 0xcb, (byte) 0xcb,
                                        (byte) 0xcb, (byte) 0xcb, (byte) 0xcb, (byte) 0xcb, (byte) 0xcb,
                                        (byte) 0xcb, (byte) 0xcb, (byte) 0xcb, (byte) 0xcb, (byte) 0x01,
                                        (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01,
                                        (byte) 0xcb, (byte) 0xcb, (byte) 0xcb, (byte) 0xcb, (byte) 0xcb,
                                        (byte) 0xcb, (byte) 0xcb, (byte) 0xcb, (byte) 0xcb, (byte) 0xcb,
                                        (byte) 0xcb, (byte) 0xcb, (byte) 0xcb, (byte) 0xcb, (byte) 0xcb,
                                        (byte) 0xcb, (byte) 0xcb, (byte) 0xcb, (byte) 0xcb, (byte) 0xcb,
                                        (byte) 0xcb, (byte) 0xcb, (byte) 0xcb, (byte) 0xcb, (byte) 0xcb,
                                        (byte) 0xcb, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01,
                                        (byte) 0x01 };

    public DataType() {
        reset();
    }

    public void reset() {
        byte newTable[] = new byte[defaultTable.length];
        System.arraycopy(defaultTable, 0, newTable, 0, defaultTable.length);
        table = newTable;
    }

    protected boolean isInsideTable(char c) {
        if (c < (char) 0x20 || c > (char) 0x7f) {
            return false;
        }
        return true;
    }
    public void set
        (int offset, byte entries[]) {
        if (entries == null || entries.length == 0) {
            return;
        }
        if (!isInsideTable((char) offset)) {
            return;
        }
        if (!isInsideTable((char) (entries.length + offset))) {
            return;
        }

        System.arraycopy(entries, 0, table, offset, entries.length);
    }

    public boolean isOK(char c, int dataType) {
        if (dataType < 0 || dataType > 7) {
            return false;
        }
        if (!isInsideTable(c)) {
            return false;
        }

        return (table[c - 0x20] & dataTypes[dataType]) != 0 ? true : false;
    }
}
