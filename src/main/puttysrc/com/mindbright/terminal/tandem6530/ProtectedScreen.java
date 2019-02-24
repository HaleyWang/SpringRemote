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

public class ProtectedScreen implements AsciiCodes {

    protected ProtectBlockMode parent;
    protected FieldMap map;
    protected int ROWS;
    protected int COLS;
    protected Position cursor;
    protected Position buffer;
    protected Position HOME;
    protected Position END;
    protected FieldAttributes defaultField;

    public ProtectedScreen(int rows, int cols, DataType dataTypeTable) {
        this(null, rows, cols, dataTypeTable);
    }
    public ProtectedScreen(ProtectBlockMode parent, int rows, int cols,
                           DataType dataTypeTable) {
        this.parent = parent;
        ROWS = rows;
        COLS = cols;
        defaultField = new FieldAttributes();
        defaultField.setProtect(true);
        map = new FieldMap(rows, cols, defaultField, dataTypeTable);
        cursor = new Position(0, 0, true);
        buffer = new Position(0, 0, true);
        HOME = new Position(0, 0);
        END = new Position(ROWS - 1, COLS - 1);
    }

    public void reset() {
        map.reset(defaultField);
        cursor.set(HOME);
        buffer.set(HOME);
    }

    public void doBackspace() {
        if (!map.haveUnprotectedFields()) {
            return;
        }
        cursor.decCol(ROWS, COLS);
        if (map.getFieldAt(cursor).isProtected() ||
                map.getFieldAt(cursor).getStart().equals(cursor)) {
            back();
        }
        updateCursorPosition();
    }

    public void doHTab() {
        if (!map.haveUnprotectedFields()) {
            return;
        }
        forward();
        updateCursorPosition();
    }

    public void doLineFeed() {
        if (!map.haveUnprotectedFields()) {
            return;
        }
        cursor.incRow(ROWS, COLS);
        if (map.getFieldAt(cursor).isProtected()) {
            forward();
        }
        updateCursorPosition();
    }

    public void doCarriageReturn() {
        if (!map.haveUnprotectedFields()) {
            return;
        }
        cursor.setCol(0);
        if (map.getFieldAt(cursor).isProtected()) {
            forward();
        }
        updateCursorPosition();
    }

    public void doBackTab() {
        if (!map.haveUnprotectedFields()) {
            return;
        }
        Field f = map.getFieldAt(cursor);
        Position firstUnprotected = new Position(f.getStart()).incCol(ROWS,
                                    COLS);
        if (firstUnprotected.lt(cursor)) {
            cursor.set(firstUnprotected);
        } else {
            back();
        }
        updateCursorPosition();
    }

    public void doCursorUp() {
        if (!map.haveUnprotectedFields()) {
            return;
        }
        cursor.decRow(ROWS, COLS);
        if (map.getFieldAt(cursor).isProtected()) {
            back();
        }
        updateCursorPosition();
    }

    public void doCursorDown() {
        if (!map.haveUnprotectedFields()) {
            return;
        }
        cursor.incRow(ROWS, COLS);
        if (map.getFieldAt(cursor).isProtected()) {
            forward();
        }
        updateCursorPosition();
    }
    public void doCursorRight() {
        if (!map.haveUnprotectedFields()) {
            return;
        }
        cursor.incCol(ROWS, COLS);
        if (map.getFieldAt(cursor).isProtected()) {
            forward();
        }
        updateCursorPosition();
    }
    public void doCursorHomeDown() {
        if (!map.haveUnprotectedFields()) {
            return;
        }
        back(END);
        updateCursorPosition();
    }
    public void doCursorHome() {
        if (!map.haveUnprotectedFields()) {
            return;
        }
        forward(HOME);
        updateCursorPosition();
    }

    public void doSetVideoAttribute(int attrib) {
        buffer.incCol(ROWS, COLS);
    }

    private Position lastCharInField(Field f) {
        Position pos = new Position(f.getStart()).incCol(ROWS, COLS);
        int numNonBlank = f.getContents().length();
        for (int i = 0; i < numNonBlank; i++) {
            pos.incCol(ROWS, COLS);
        }
        if (f.getEnd().lt(pos)) { // Do not move beyond the current fields
            pos.set(f.getEnd());
        }
        return pos;
    }

    public void cursorToLastCharInField() {
        if (!map.haveUnprotectedFields()) {
            return;
        }
        Field f = map.getFieldAt(cursor);
        Position pos = lastCharInField(f);
        if (pos.le(cursor)
            && null != (f = getNextUnprotected(f.getEnd().incCol(ROWS, COLS)))){
            pos = lastCharInField(f);
        }
        cursor.set(pos);
        updateCursorPosition();
    }

    public void cursorToNextUnprotected(int numLines) {
        if (!map.haveUnprotectedFields()) {
            return;
        }
        // Move down cursor numLines and put it first in line
        Position p = new Position(cursor);
        for (int i = 0; i < numLines; i++) {
            p.incRow(ROWS, COLS);
        }
        p.setCol(0);

        Field f = getNextUnprotected(p);
        if (f == null) {
            f = getNextUnprotected(HOME);
        }

        p = new Position(f.getStart()).incCol(ROWS, COLS);
        cursor.set(p);
        updateCursorPosition();
    }

    public void setCursorAddress(int row, int col) {
        Position p = new Position(row, col);
        if (map.getFieldAt(p).isProtected()) {
            forward(p);
        } else {
            cursor.set(row, col);
        }
        updateCursorPosition();
    }
    public Position getCursorAddress() {
        return cursor;
    }

    public void setBufferAddress(int row, int col) {
        buffer.set(row, col);
    }
    public Position getBufferAddress() {
        return buffer;
    }

    public void addField(FieldAttributes attribs) {
        addField(buffer, attribs);
        buffer.incCol(ROWS, COLS);
    }
    public void addField(Position p, FieldAttributes attribs) {
        boolean oldValue = map.haveUnprotectedFields();

        map.addField(p, attribs);

        if (oldValue == false && map.haveUnprotectedFields() == true) {
            // We got our first unprotected field, move cursor to it
            forward(HOME);
        }
    }

    public void doClearMemoryToSpaces(int startRow, int startCol,
                                      int endRow, int endColumn) {
        Position start = new Position(startRow, startCol);
        Position end   = new Position(endRow, endColumn);
        start.limit(ROWS, COLS);
        end.limit(ROWS, COLS);
        map.clear(start, end);
        if (map.getFieldAt(cursor).isProtected()) {
            forward();
            updateCursorPosition();
        }
        updateDirty(start, end);
    }

    public void doEraseToEndOfPageOrMemory(boolean atCursor) {
        Position p = atCursor ? cursor : buffer;

        Field f = getNextUnprotected(p);
        while (f != null) {
            f.clearField();
            f = getNextUnprotected(f.getStart());
        }
    }

    public void doEraseToEndOfLineOrField(boolean atCursor) {
        Position p = atCursor ? cursor : buffer;

        Field f = map.getFieldAt(p);
        f.clearToEnd(p);
        updateDirty(p, f.getEnd());
    }

    public void doInsertLine() {
        map.insertRow(buffer.getRow());
        if (map.getFieldAt(cursor).isProtected()) {
            forward();
            updateCursorPosition();
        }
        updateDirty(new Position(buffer.getRow(), 0), END);
    }

    public void doDeleteLine() {
        map.deleteRow(buffer.getRow());
        if (map.getFieldAt(cursor).isProtected()) {
            forward();
            updateCursorPosition();
        }
        updateDirty(new Position(buffer.getRow(), 0), END);
    }

    public void doInsertCharacter(boolean atCursor) {
        Position p = atCursor ? cursor : buffer;

        Field f = map.getFieldAt(p);
        if (f.isProtected()) {
            return;
        }
        f.insertChar(p);
        updateDirty(p, f.getEnd());
    }

    public void doDeleteCharacter(boolean atCursor) {
        Position p = atCursor ? cursor : buffer;

        Field f = map.getFieldAt(p);
        if (f.isProtected()) {
            return;
        }
        f.deleteChar(p);
        updateDirty(p, f.getEnd());
    }

    public void doResetModifiedDataTags() {
        Field f = getNextUnprotected(HOME);
        while (f != null) {
            f.resetMdt();
            Field next = map.nextField(f.getEnd());
            if (next == f) {
                return;
            }
            f = next;
        }
    }

    public void bufferWrite(char c) {
        Field f = map.getFieldAt(buffer);
        f.writeBufferChar(buffer, c);
        updateDirty(buffer, buffer);
        buffer.incCol(ROWS, COLS);
    }

    public void setAttribute(int attr) {
        Field f = map.getFieldAt(buffer);
        f.setAttrib(buffer, attr);
    }

    public boolean cursorWrite(char c) {
        return cursorWrite(c, false);
    }
    public boolean cursorWrite(char c, boolean insertMode) {
        Field f = map.getFieldAt(cursor);
        if (f.getStart().equals(cursor)) {
            // Autotab was disabled in last field, the user must advance
            // the cursor himself to get out of this protected position
            return true;
        }

        boolean okInput = f.writeCursorChar(cursor, c, insertMode);
        if (okInput) {
            if (insertMode) {
                updateDirty(cursor, f.getEnd());
            } else {
                updateDirty(cursor, cursor);
            }

            cursor.incCol(ROWS, COLS);
            if (map.getFieldAt(cursor) != f) {
                // Cursor outside input field
                if (f.hasAutoTab()) {
                    f = getNextUnprotected(cursor);
                    if (f == null) {
                        f = getNextUnprotected(HOME);
                    }
                    cursor = new Position(f.getStart()).incCol(ROWS, COLS);
                }
            }
            updateCursorPosition();
        }

        return okInput;
    }

    private void readField(StringBuilder buf, Field field) {
        Position p = new Position(field.getStart()).incCol(ROWS, COLS);
        buf.append(DC1);
        buf.append((char) (0x1f + p.getRow() + 1));
        buf.append((char) (0x1f + p.getCol() + 1));
        buf.append(field.getContents());
    }

    protected String readWithAddress(Position start, Position end,
                                     boolean onlyUnprotected,
                                     boolean ignoreMdt) {
        StringBuilder buf = new StringBuilder();
        Field f = map.getFieldAt(start);
        Position p = new Position(f.getStart()).incCol(ROWS, COLS);

        if (p.lt(start)) {
            // start was not on first position after video attrib,
            // start on next field
            f = getNextField(start);
        }

        while (f != null && f.getStart().le(end)) {
            if (onlyUnprotected && !f.isProtected()) {
                readField(buf, f);
            } else if (ignoreMdt || f.getMdt() == true) {
                readField(buf, f);
            }
            f = getNextField(f.getEnd());
        }
        buf.append(EOT);
        return buf.toString();
    }

    public String readWholePageOrBuffer() {
        return readWithAddress(HOME, END, true, false);
    }
    public String readWithAddress(Position start, Position end) {
        return readWithAddress(start, end, false, false);
    }

    public String readWithAddressAll(Position start, Position end) {
        return readWithAddress(start, end, false, true);
    }

    public char[] getChars(int row) {
        char ret[] = new char[COLS];
        char tmp[];
        int i = 0;
        Position p = new Position(row, 0);
        Field f = map.getFieldAt(p);

        tmp = f.getChars(row);
        while (i < COLS) {
            System.arraycopy(tmp, 0, ret, i, tmp.length);
            i += tmp.length;
            if (i >= COLS) {
                break;
            }
            p.set(f.getEnd()).incCol(ROWS, COLS);
            f = map.getFieldAt(p);
            tmp = f.getChars(row);
        }
        return ret;
    }

    public int[] getAttribs(int row) {
        int ret[] = new int[COLS];
        int tmp[];
        int i = 0;
        Position p = new Position(row, 0);
        Field f = map.getFieldAt(p);

        tmp = f.getAttribs(row);
        if (tmp == null) {
            return null;
        }
        while (i < COLS) {
            System.arraycopy(tmp, 0, ret, i, tmp.length);
            i += tmp.length;
            if (i >= COLS) {
                break;
            }
            p.set(f.getEnd()).incCol(ROWS, COLS);
            f = map.getFieldAt(p);
            tmp = f.getAttribs(row);
        }
        return ret;
    }

    protected void back() {
        back(cursor);
    }
    protected void back(Position p) {
        back(p, false);
    }
    protected void back(Position p, boolean recursive) {
        if (!map.haveUnprotectedFields()) {
            return;
        }

        Field field = map.getFieldAt(p);
        if (field.isProtected()) {
            // goto previous field
            back(new Position(field.getStart(), true).decCol(ROWS, COLS),
                 true);
            return;
        }

        Position firstPos = new Position(field.getStart()).incCol(ROWS, COLS);
        if (!recursive && firstPos.equals(cursor)) {
            // Allready on first position, goto previous field.
            // Must check if this method was called recursive because we
            // will end up here repeatedly if only one unprotected field
            // is defined.
            back(new Position(field.getStart(), true).decCol(ROWS, COLS),
                 true);
            return;
        }

        // Goto the first position in field
        cursor.set(firstPos);
    }

    protected void forward() {
        forward(cursor);
    }
    protected void forward(Position p) {
        if (!map.haveUnprotectedFields()) {
            return;
        }
        Field nextField = getNextUnprotected(p);
        if (nextField == null) {
            nextField = getNextUnprotected(HOME);
        }
        cursor.set(nextField.getStart());
        cursor.incCol(ROWS, COLS);
    }

    public void updateCursorPosition() {
        if (parent != null) {
            parent.updateCursorPosition(this, cursor);
        }
    }

    protected void updateDirty(Position start, Position end) {
        if (parent != null) {
            parent.updateDirty(this, start, end);
        }
    }

    protected Field getNextField(Position p) {
        Field f = map.getFieldAt(p);
        Field next = map.nextField(p);
        if (f == next) {
            return null;
        }
        return next;
    }

    /**
     * Get the next unprotected field at position p or later
     */
    protected Field getNextUnprotected(Position p) {
        Field f = map.getFieldAt(p);
        while (f != null && f.isProtected()) {
            Field next = map.nextField(f.getEnd());
            if (next == f) {
                // f was the last field on screen
                f = null;
            } else {
                f = next;
            }
        }
        return f;
    }
}
