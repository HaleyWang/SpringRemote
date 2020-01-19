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

public class Position {
    protected int row;
    protected int col;
    protected boolean wrap;

    public Position(Position p) {
        this(p.getRow(), p.getCol(), false);
    }
    public Position(Position p, boolean wrap) {
        this(p.getRow(), p.getCol(), wrap);
    }
    public Position(int row, int col) {
        this(row, col, false);
    }
    public Position(int row, int col, boolean wrap) {
        this.wrap = wrap;
        set(row, col);
    }

    public Position set(Position p) {
        return set(p.getRow(), p.getCol());
    }
    public Position set(int row, int col) {
        setRow(row);
        return setCol(col);
    }
    public int getRow() {
        return row;
    }
    public Position setRow(int row) {
        this.row = row;
        return this;
    }
    public int getCol() {
        return col;
    }
    public Position setCol(int col) {
        this.col = col;
        return this;
    }

    public Position decCol(int rows, int cols) {
        if (col - 1 < 0) {
            if (row - 1 < 0) {
                if (wrap) {
                    row = rows - 1;
                    col = cols - 1;
                } else {
                    col = row = 0;
                }
            } else {
                row--;
                col = cols - 1;
            }
        } else {
            col--;
        }
        return this;
    }

    public Position incCol(int rows, int cols) {
        if (col + 1 >= cols) {
            if (row + 1 >= rows) {
                if (wrap) {
                    col = 0;
                    row = 0;
                } else {
                    col = cols - 1;
                    row = rows - 1;
                }
            } else {
                row++;
                col = 0;
            }
        } else {
            col++;
        }
        return this;
    }

    public Position decRow(int rows, int cols) {
        row--;
        if (row < 0) {
            if (wrap) {
                row = rows - 1;
            } else {
                row = 0;
            }
        }
        return this;
    }

    public Position incRow(int rows, int cols) {
        row++;
        if (row >= rows) {
            if (wrap) {
                row = 0;
            } else {
                row = rows - 1;
            }
        }
        return this;
    }

    /** Returns true if I'm less than other
     */
    public boolean lt(Position other) {
        if (getRow() < other.getRow()) {
            return true;
        } else if (getRow() == other.getRow() && getCol() < other.getCol()) {
            return true;
        }
        return false;
    }

    /** Returns true if I'm less than or equal to other
     */
    public boolean le(Position other) {
        if (getRow() < other.getRow()) {
            return true;
        } else if (getRow() == other.getRow() && getCol() <= other.getCol()) {
            return true;
        }
        return false;
    }


    public int abs(int cols) {
        return row * cols + col;
    }

    public boolean equals(Object o) {
	if (o == null || !(o instanceof Position))
            return false;
        Position other = (Position) o;
        return getRow() == other.getRow() && getCol() == other.getCol();
    }

    public void limit(int rows, int cols) {
        if (row < 0)     row = 0;
        if (row >= rows) row = rows-1;
        if (col < 0)     col = 0;
        if (col >= cols) col = cols-1;
    }

    public int hashCode() {
        return col ^ row;
    }

    public String toString() {
        return row + "," + col;
    }

}
