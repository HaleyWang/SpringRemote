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

/** This is table 3-10.
 */
public class VariableFieldAttributeTable {
    static protected FieldAttributes defaultTable[];

    static {
        try {
            FieldAttributes tmp[] = {
                                        new FieldAttributes((char) 0x20, (char) 0x40, (char) 0x44),
                                        new FieldAttributes((char) 0x20, (char) 0x41, (char) 0x44),
                                        new FieldAttributes((char) 0x20, (char) 0x48, (char) 0x44),
                                        new FieldAttributes((char) 0x20, (char) 0x49, (char) 0x44),
                                        new FieldAttributes((char) 0x20, (char) 0x60, (char) 0x44),
                                        new FieldAttributes((char) 0x20, (char) 0x61, (char) 0x44),
                                        new FieldAttributes((char) 0x20, (char) 0x68, (char) 0x44),
                                        new FieldAttributes((char) 0x20, (char) 0x69, (char) 0x44),
                                        new FieldAttributes((char) 0x20, (char) 0x40, (char) 0x44),
                                        new FieldAttributes((char) 0x20, (char) 0x41, (char) 0x44),
                                        new FieldAttributes((char) 0x20, (char) 0x48, (char) 0x44),
                                        new FieldAttributes((char) 0x20, (char) 0x50, (char) 0x44),
                                        new FieldAttributes((char) 0x20, (char) 0x50, (char) 0x44),
                                        new FieldAttributes((char) 0x20, (char) 0x50, (char) 0x44),
                                        new FieldAttributes((char) 0x20, (char) 0x50, (char) 0x44),
                                        new FieldAttributes((char) 0x20, (char) 0x50, (char) 0x44),
                                        new FieldAttributes((char) 0x20, (char) 0x50, (char) 0x44),
                                        new FieldAttributes((char) 0x20, (char) 0x50, (char) 0x44),
                                        new FieldAttributes((char) 0x20, (char) 0x50, (char) 0x44),
                                        new FieldAttributes((char) 0x20, (char) 0x50, (char) 0x44),
                                        new FieldAttributes((char) 0x20, (char) 0x50, (char) 0x44),
                                        new FieldAttributes((char) 0x20, (char) 0x50, (char) 0x44),
                                        new FieldAttributes((char) 0x20, (char) 0x50, (char) 0x44),
                                        new FieldAttributes((char) 0x20, (char) 0x50, (char) 0x44),
                                        new FieldAttributes((char) 0x20, (char) 0x50, (char) 0x44),
                                        new FieldAttributes((char) 0x20, (char) 0x50, (char) 0x44),
                                        new FieldAttributes((char) 0x20, (char) 0x50, (char) 0x44),
                                        new FieldAttributes((char) 0x20, (char) 0x50, (char) 0x44),
                                        new FieldAttributes((char) 0x20, (char) 0x50, (char) 0x44),
                                        new FieldAttributes((char) 0x20, (char) 0x50, (char) 0x44),
                                        new FieldAttributes((char) 0x20, (char) 0x50, (char) 0x44),
                                        new FieldAttributes((char) 0x20, (char) 0x50, (char) 0x44),
                                    };
            defaultTable = tmp;
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    protected FieldAttributes table[];


    public VariableFieldAttributeTable() {
        reset();
    }

    protected boolean isInsideTable(int index) {
        return index >= 0 && index < defaultTable.length;
    }

    public FieldAttributes get
        (int index) {
        if (!isInsideTable(index)) {
            return table[0];
        }
        return table[index];
    }

    public void set
        (int startIndex, FieldAttributes entries[]) {
        if (!isInsideTable(startIndex) ||
                isInsideTable(startIndex + entries.length)) {
            return;
        }
        System.arraycopy(entries, 0, table, startIndex, entries.length);
    }

    public void reset() {
        FieldAttributes newTable[] = new FieldAttributes[defaultTable.length];
        System.arraycopy(defaultTable, 0, newTable, 0, defaultTable.length);
        table = newTable;
    }

}


