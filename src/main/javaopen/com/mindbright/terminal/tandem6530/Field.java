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

public class Field implements AsciiCodes {
    protected Position        start;
    protected Position        end;
    protected FieldAttributes attribs;
    protected int             cols;
    protected DataType        dataTypeTable;
    protected boolean         mdt;
    protected int             len;
    protected StringBuffer    buf = new StringBuffer();
    protected FieldVideoAttributeMap map;

    public Field(Position start, Position end, Field master) {
        this(master.cols, start, end, master.attribs, master.dataTypeTable);
    }

    public Field(int cols, Position start, Position end,
                 FieldAttributes attribs, DataType dataTypeTable) {
        this.cols = cols;
        map = new FieldVideoAttributeMap(attribs.getVideoAttrib() & 0x1f);
        setStart(start, false);
        setEnd(end);
        redefine(attribs);
        this.dataTypeTable = dataTypeTable;
    }

    public void redefine(FieldAttributes attribs) {
        this.attribs = attribs;
        mdt = attribs.getMdt();
        map.setDefaultAttrib(attribs.getVideoAttrib() & 0x1f);
    }

    /**
     * Adjust the fields start position.
     *
     * @param move if true the content of the field is moved so that
     * it remains at the same position relative to the filed start. If
     * false the content is left at the same screen position.
     */
    public void setPosition(Position start, Position end, boolean move) {
        this.end = new Position(end);
        setStart(start, move);
        len = calcLen();
    }

    public void setStart(Position p, boolean move) {
        if (start == null || !start.equals(p)) {
            // Adjust buffer if needed
            if (!move
                && start != null
                && start.getRow() != p.getRow()
                && buf.length() > 0) {
                if (p.le(start)) {
                    int rows = start.getRow() - p.getRow();
                    for (int i=0; i<rows*cols; i++) {
                        buf.insert(0, ' ');
                    }
                } else {
                    buf.delete(0, (p.getRow()-start.getRow())*cols);
                }
            }
            start = new Position(p);
            if (end == null) {
                return;
            }

            len = calcLen();
        }
    }
    public Position getStart() {
        return start;
    }

    public void setEnd(Position p) {
        if (end == null || !end.equals(p)) {
            end = new Position(p);
            if (start == null) {
                return;
            }

            len = calcLen();
        }
    }
    public Position getEnd() {
        return end;
    }

    public String getContents() {
        int lastNonBlank = -1;
        for (int i = buf.length() - 1; i >= 0; i--) {
            if (!(buf.charAt(i) == '\u0000' || buf.charAt(i) == ' ') ||
                    map.isAttrib(i)) {
                lastNonBlank = i;
                break;
            }
        }
        if (lastNonBlank == -1) {
            return "";
        }

        StringBuilder tmp = new StringBuilder();
        for (int i = 1; i <= lastNonBlank; i++) {
            if (buf.charAt(i) == '\u0000') {
                if (map.isAttrib(i)) {
                    tmp.append(ESC);
                    tmp.append('6');
                    tmp.append((char) (0x20 + map.get(i)));
                } else {
                    tmp.append(' ');
                }
            } else {
                tmp.append(buf.charAt(i));
            }
        }

        return tmp.toString();
    }

    public void resetMdt() {
        mdt = false;
    }

    public boolean getMdt() {
        return mdt;
    }

    public boolean isProtected() {
        return attribs.getProtect();
    }

    public boolean hasAutoTab() {
        return attribs.getAutoTab();
    }

    protected int calcOffset(Position p) {
        return calcOffset(p, false);
    }
    protected int calcOffset(Position p, boolean writeOnFieldStart) {
        if (!writeOnFieldStart && p.equals(start)) {
            // Can't write on field-start address (is video attribute
            // in the spec).
            return -1;
        }
        int myStartOffset = start.abs(cols);
        int writeOffset = p.abs(cols);
        int offset = writeOffset - myStartOffset;
        if (offset < 0 || offset >= len) {
            return -1;
        }
        return offset;
    }

    public boolean writeCursorChar(Position p, char c) {
        return writeCursorChar(p, c, false);
    }
    public boolean writeCursorChar(Position p, char c, boolean insertMode) {
        int offset = calcOffset(p);
        if (offset < 0) {
            return false;
        }

        if (!dataTypeTable.isOK(c, attribs.getDataType())) {
            return false;
        }

        if (attribs.getUpShift()) {
            c = Character.toUpperCase(c);
        }

        if (insertMode) {
            insertChar(p);
        }
        buf.setCharAt(offset, c);
        map.clearAt(offset);
        mdt = true;
        return true;
    }

    public boolean writeBufferChar(Position p, char c) {
        int offset = calcOffset(p, true);
        if (offset < 0) {
            return false;
        }

        buf.setCharAt(offset, c);
        map.clearAt(offset);
        return true;
    }

    public void setAttrib(Position p, int attrib) {
        int offset = calcOffset(p, true);
        if (offset < 0) {
            return;
        }

        buf.setCharAt(offset, '\u0000');
        map.set(offset, attrib);
        return;
    }

    public void clearField() {
        buf.setLength(0);
        buf.setLength(len);
        map.clearFrom(0);
    }

    public void clearAt(Position p) {
        int offset = calcOffset(p);
        if (offset < 0) {
            return;
        }

        buf.setCharAt(offset, '\u0000');
        map.clearAt(offset);
        return;
    }
    public void clearToEnd(Position p) {
        int offset = calcOffset(p);
        if (offset < 0) {
            return;
        }

        for (int i = offset; i < len; i++) {
            buf.setCharAt(i, '\u0000');
        }
        map.clearFrom(offset);
        return;
    }

    public char[] getChars(int row) {
        int startIndex;
        int len;

        if (row < start.getRow() || row > end.getRow()) {
            return null;
        }

        if (row == start.getRow()) {
            startIndex = 0;
            if (row == end.getRow()) {
                len = end.getCol() - start.getCol() + 1;
            } else {
                len = cols - start.getCol();
            }
        } else {
            startIndex = (cols - start.getCol()) +
                         ((row - 1) - start.getRow()) * cols;
            if (row == end.getRow()) {
                len = end.getCol() + 1;
            } else {
                len = cols;
            }
        }

        char line[] = new char[len];
        char c;
        for (int i = 0; i < len; i++) {
            c = buf.charAt(startIndex + i);
            if (c == '\u0000') {
                c = ' ';
            }
            line[i] = c;
        }

        return line;
    }

    public int[] getAttribs(int row) {
        int startIndex;
        int len;

        if (row < start.getRow() || row > end.getRow()) {
            return null;
        }

        if (row == start.getRow()) {
            startIndex = 0;
            if (row == end.getRow()) {
                len = end.getCol() - start.getCol() + 1;
            } else {
                len = cols - start.getCol();
            }
        } else {
            startIndex = (cols - start.getCol()) +
                         ((row - 1) - start.getRow()) * cols;
            if (row == end.getRow()) {
                len = end.getCol() + 1;
            } else {
                len = cols;
            }
        }

        int line[] = new int[len];
        for (int i = 0; i < len; i++) {
            line[i] = map.get(startIndex + i);
        }

        return line;
    }

    public void insertChar(Position p) {
        int offset = calcOffset(p);
        if (offset < 0) {
            return;
        }

        buf.insert(offset, '\u0000');
        map.insertAt(offset);
        buf.setLength(len);
    }

    public void deleteChar(Position p) {
        int offset = calcOffset(p);
        if (offset < 0) {
            return;
        }

        int i;
        for (i = offset; i < len - 1; i++) {
            buf.setCharAt(i, buf.charAt(i+1));
        }
        buf.setCharAt(i, '\u0000');
        map.deleteAt(offset);
    }

    protected int calcLen() {
        int startPos = start.abs(cols);
        int endPos = end.abs(cols);
        int len = endPos - startPos + 1;
        buf.setLength(len);
        map.clearFrom(len);
        return len;
    }

    public String toString() {
        return "Field[start=" + start +",end=" + end + "]";
    }
}

