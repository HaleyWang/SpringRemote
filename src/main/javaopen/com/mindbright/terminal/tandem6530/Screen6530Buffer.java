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

public class Screen6530Buffer {
    protected static final int NO_ATTRIB = -1;
    protected int rows;
    protected int cols;
    protected int attribs[][];
    protected char chars[][];

    public Screen6530Buffer(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;

        attribs = new int[rows][cols];
        chars = new char[rows][cols];

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                attribs[row][col] = NO_ATTRIB;
            }
        }
    }

    public int getRows() {
        return rows;
    }
    public int getCols() {
        return cols;
    }

    public void setAttrib(int row, int col, int attrib) {
        attribs[row][col] = attrib;
    }
    public int getAttrib(int row, int col) {
        return attribs[row][col];
    }
    public boolean isAttrib(int row, int col) {
        return attribs[row][col] != NO_ATTRIB;
    }

    public void setChar(int row, int col, char c) {
        chars[row][col] = c;
    }
    public char getChar(int row, int col) {
        return chars[row][col];
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                buf.append(attribs[row][col]).append(' ');
            }
            buf.append('\n');
        }

        buf.append('\n');

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                buf.append((int) chars[row][col]).append(' ');
            }
            buf.append('\n');
        }
        return buf.toString();
    }
}

