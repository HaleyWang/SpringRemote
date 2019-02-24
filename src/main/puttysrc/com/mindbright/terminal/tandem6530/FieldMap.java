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

public class FieldMap {
    protected int rows;
    protected int cols;
    protected Field map[][];
    protected Position HOME;
    protected Position END;
    protected DataType dataTypeTable;
    protected boolean haveUnprotectedFields = false;

    public FieldMap(int rows, int cols, FieldAttributes defaultFieldAttribs,
                    DataType dataTypeTable) {
        this.rows = rows;
        this.cols = cols;
        this.dataTypeTable = dataTypeTable;

        HOME = new Position(0, 0);
        END = new Position(rows - 1, cols - 1);

        map = new Field[rows][cols];
        Field field = new Field(cols, HOME, END, defaultFieldAttribs,
                                dataTypeTable);
        setField(HOME, END, field);
    }

    public void addField(Position p, FieldAttributes attribs) {
        Field oldField = getFieldAt(p);
        if (p.equals(oldField.getStart())) {
            // Start address is same as an existing field, redefine it
            oldField.redefine(attribs);
            return;
        }

        new Position(p);
        prevField(p);
        Field next = nextField(p);

        new Position(p).decCol(rows, cols);
        Position fieldEnd;
        if (next == oldField) {
            // oldField is the last field on screen
            fieldEnd = new Position(END);
        } else {
            fieldEnd = new Position(next.getStart()).decCol(rows, cols);
        }
        Field field = new Field(cols, p, fieldEnd, attribs, dataTypeTable);

        setField(field.getStart(), field.getEnd(), field);
        adjustFields(false);
    }

    public void reset(FieldAttributes defaultFieldAttribs) {
        Field field = new Field(cols, HOME, END, defaultFieldAttribs,
                                dataTypeTable);
        setField(HOME, END, field);
        adjustFields(false);
    }

    public boolean haveUnprotectedFields() {
        return haveUnprotectedFields;
    }

    protected void setField(Position start, Position end, Field field) {
        Position i = new Position(start);
        setFieldAt(i, field);
        while (!i.equals(end)) {
            i.incCol(rows, cols);
            setFieldAt(i, field);
        }
    }

    protected void setFieldAt(Position p, Field field) {
        map[p.getRow()][p.getCol()] = field;
    }

    public Field getFieldAt(Position p) {
        return map[p.getRow()][p.getCol()];
    }

    protected Position prevFieldEnd(Position p) {
        Field f = getFieldAt(p);
        Position pos = new Position(p).decCol(rows, cols);

        while (f == getFieldAt(pos) && !pos.equals(HOME)) {
            pos.decCol(rows, cols);
        }
        return pos;
    }

    public Field prevField(Position p) {
        return getFieldAt(prevFieldEnd(p));
    }

    protected Position nextFieldStart(Position p) {
        Field f = getFieldAt(p);
        Position pos = new Position(p).incCol(rows, cols);

        while (f == getFieldAt(pos) && !pos.equals(END)) {
            pos.incCol(rows, cols);
        }
        return pos;
    }

    public Field nextField(Position p) {
        return getFieldAt(nextFieldStart(p));
    }

    /**
     * Set the fields start/end values according to the map.
     *
     * @param move if true the content of the fields is moved with the
     * fields.
     */
    protected void adjustFields(boolean move) {
        Position fieldStart = new Position(HOME);
        Position fieldEnd = null;

        haveUnprotectedFields = false;
        while (!END.equals(fieldStart)) {
            fieldEnd = nextFieldStart(fieldStart);
            if (!END.equals(fieldEnd)) {
                fieldEnd.decCol(rows, cols);
            }

            getFieldAt(fieldStart).setPosition(fieldStart, fieldEnd, move);
            if (!getFieldAt(fieldStart).isProtected()) {
                haveUnprotectedFields = true;
            }

            fieldStart = nextFieldStart(fieldStart);
        }
    }

    public void insertRow(int row) {
        Position rowPos = new Position(row, 0);
        Field prev = getFieldAt(rowPos);

        if (rowPos.equals(prev.getStart())) {
            // This field started at rowPos, must choose the
            // previous field to be the one before that.
            prev = prevField(rowPos);
        }
        Field f = new Field(rowPos, new Position(row, cols - 1), prev);

        // Move down the fields below row, and create a new field for
        // the new row.
        //
        // I don't want to leave the new row just blank, since the
        // whole point of a default field seems to be that all positions
        // on screen should belong to a field.
        System.arraycopy(map, row, map, row + 1, (rows - row - 1));
        map[row] = new Field[cols];
        setField(rowPos, new Position(row, cols - 1), f);

        adjustFields(true);
    }

    public void deleteRow(int row) {
        Field lastField = getFieldAt(END);

        // Move all rows below row up, and let the last field fill up
        // the empty row at the bottom.
        System.arraycopy(map, row + 1, map, row, (rows - row - 1));
        map[rows-1] = new Field[cols];
        setField(new Position(rows - 1, 0), END, lastField);

        adjustFields(true);
    }

    protected void clearFields(Position start, Position end) {
        Field fillField;

        if (HOME.equals(start)) {
            fillField = getFieldAt(start);
            fillField.redefine(new FieldAttributes());
        } else {
            fillField = getFieldAt(start);
            if (fillField.getStart().equals(start)) {
                fillField = prevField(start);
            }
        }

        Position p = new Position(start);
        Position fillEnd = getFieldAt(end).getEnd();
        while (p.le(end)) {
            if (getFieldAt(p).getStart().equals(p)) {
                // A field started inside the interval [start,end], replace
                // all positions until fillEnd with the fillField
                while (p.le(fillEnd)) {
                    setFieldAt(p, fillField);
                    if (p.equals(END)) {
                        break;
                    }
                    p.incCol(rows, cols);
                }
                break;
            }
            if (p.equals(END)) {
                break;
            }
            p.incCol(rows, cols);
        }

        adjustFields(false);
    }

    public void clear(Position start, Position end) {
        // Remove any fields that starts in the interval [start,end]
        clearFields(start, end);

        Position p = new Position(start);
        while (!p.equals(end)) {
            getFieldAt(p).clearAt(p);
            p.incCol(rows, cols);
        }
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        Position fieldStart = new Position(HOME);
        Field field = null;
        int i = 0;

        while (!END.equals(fieldStart)) {
            field = getFieldAt(fieldStart);
            buf.append(" Field ").append(i++).append(' ');
            buf.append(field.getStart()).append('-').append(field.getEnd());

            fieldStart = nextFieldStart(fieldStart);
        }
        return buf.toString();
    }
}
