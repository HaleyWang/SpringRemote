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

import com.mindbright.terminal.DisplayView;

public class VideoAttributeMap {
    protected int COLS;
    protected int TOTAL_ROWS;
    protected int defaultAttrib;

    protected int numAttribs;
    protected Attrib attribs[];

    protected DisplayView display;

    protected static class Attrib {
        private int row;
        private int col;
        private int attrib;

        Attrib(int row, int col, int attrib) {
            this.row = row;
            this.col = col;
            this.attrib = attrib;
        }

        int getRow() {
            return row;
        }
        void incRow(int n) {
            row += n;
        }
        void decRow(int n) {
            row -= n;
        }
        int getCol() {
            return col;
        }
        void incCol(int n) {
            col += n;
        }
        void decCol(int n) {
            col -= n;
        }
        int getAttrib() {
            return attrib;
        }
        void setAttrib(int attrib) {
            this.attrib = attrib;
        }

        int compareTo(int row, int col) {
            if (this.row < row) {
                return -1;
            }
            if (this.row == row) {
                if (this.col < col) {
                    return -1;
                }
                if (this.col == col) {
                    return 0;
                }
            }
            return 1;
        }

        public String toString() {
            return String.valueOf(row) + ","+col+"=" + attrib;
        }
    }

    public VideoAttributeMap(int totalRows, int cols, int defaultAttrib) {
        COLS = cols;
        TOTAL_ROWS = totalRows;
        this.defaultAttrib = defaultAttrib;
        attribs = new Attrib[1];
        numAttribs = 0;
    }

    public void setDisplay(DisplayView display) {
        this.display = display;
    }

    public boolean isAttrib(int row, int col) {
        int index = getAttrib(row, col);
        if (index < 0) {
            return false;
        }
        Attrib attrib = attribs[index];

        return (attrib.getRow() == row && attrib.getCol() == col);
    }

    public int attribAt(int row, int col) {
        int index = getAttrib(row, col);
        if (index < 0) {
            return defaultAttrib;
        }

        return attribs[index].getAttrib();
    }

    public void add
        (int row, int col, int attrib) {
        if (numAttribs + 1 > attribs.length) {
            Attrib tmp[] = new Attrib[attribs.length * 2 + 1];
            System.arraycopy(attribs, 0, tmp, 0, attribs.length);
            attribs = tmp;
        }


        int idx = getAttrib(row, col);
        if (idx >= 0 && attribs[idx].compareTo(row, col) == 0) {
            // We are replacing an existing attribute
            attribs[idx].setAttrib(attrib);
        } else {
            Attrib newAttrib = new Attrib(row, col, attrib);
            if (numAttribs == 0) {
                idx = 0;
            } else {
                idx = find(row, col);
                System.arraycopy(attribs, idx, attribs, idx + 1,
                                 attribs.length - 1 - idx);
            }
            attribs[idx] = newAttrib;
            numAttribs++;
        }

        if (idx + 1 == numAttribs) {
            // Added last attribute
            makeDirty(row, col, TOTAL_ROWS, COLS);
        } else {
            Attrib a = attribs[idx + 1];
            makeDirty(row, col, a.getRow() + 1, a.getCol());
        }
        return;
    }

    public void delete(int row, int col) {
        if (!isAttrib(row, col)) {
            return;
        }
        int index = getAttrib(row, col);
        delete(index);
    }

    protected void delete(int index) {
        int dirtyTop = attribs[index].getRow();
        int dirtyLeft = attribs[index].getCol();
        int dirtyBottom;
        int dirtyRight;

        if (numAttribs == 1 || (index + 1 == numAttribs)) {
            // Removed only or last attribute
            dirtyBottom = TOTAL_ROWS;
            dirtyRight = COLS;
        } else {
            dirtyBottom = attribs[index + 1].getRow() + 1;
            dirtyRight = attribs[index + 1].getCol() + 1;
            System.arraycopy(attribs, index + 1, attribs, index,
                             numAttribs - index - 1);
        }
        numAttribs--;



        makeDirty(dirtyTop, dirtyLeft, dirtyBottom, dirtyRight);
    }

    public void setDefault(int newDefault) {
        defaultAttrib = newDefault;

        if (numAttribs == 0) {
            // All positions on screen uses the default attribute
            makeDirty(0, 0, TOTAL_ROWS, COLS);
            return;
        }

        Attrib firstAttrib = attribs[0];

        if (firstAttrib.getRow() == 0 && firstAttrib.getCol() == 0) {
            // No position on screen uses the default attribute
            return;
        }

        // The positions from origin to the first field uses the
        // default attribute
        makeDirty(0, 0, firstAttrib.getRow() + 1, firstAttrib.getCol() + 1);
    }

    public void insertChars(int row, int col, int n) {
        int i = getAttrib(row, col);
        if (i < 0) {
            return;
        }
        if (attribs[i].compareTo(row, col) < 0) {
            // The attribute for (row,col) begins before (row, col),
            // but perhaps the next attribute is affected?
            i++;
        }

        int dirtyTop = row;
        int dirtyLeft = -1;
        int dirtyBottom = row + 1;
        int dirtyRight = -1;

        while (i < numAttribs && attribs[i].compareTo(row, COLS) < 0) {
            if (dirtyLeft == -1) {
                dirtyLeft = attribs[i].getCol();
            }
            attribs[i].incCol(n);
            if (attribs[i].compareTo(row, COLS) >= 0) {
                // attribs[i] have falled of the end of the line
                delete(i);
                dirtyRight = COLS;
            } else {
                dirtyRight = attribs[i].getCol();
                i++;
            }
        }

        if (dirtyLeft != -1) {
            makeDirty(dirtyTop, dirtyLeft, dirtyBottom, dirtyRight);
        }
    }

    public void deleteChars(int row, int col, int n) {
        int i = getAttrib(row, COLS - 1);
        if (i < 0) {
            return;
        }

        int dirtyTop = row;
        int dirtyLeft = -1;
        int dirtyBottom = row + 1;
        int dirtyRight = -1;

        while (i >= 0 && attribs[i].compareTo(row, col) >= 0) {
            if (dirtyRight == -1) {
                dirtyRight = attribs[i].getCol() + 1;
            }
            attribs[i].decCol(n);
            if (attribs[i].compareTo(row, col) < 0) {
                // attribs[i] have been deleted

                // This is a bit ugly, but otherwise the delete method will
                // get the dirty area all wrong
                attribs[i].incCol(n);
                dirtyLeft = attribs[i].getCol();
                delete(i);
            } else {
                dirtyLeft = attribs[i].getCol();
            }
            i--;
        }

        if (dirtyLeft < col) {
            dirtyLeft = col;
        }

        if (dirtyRight != -1) {
            makeDirty(dirtyTop, dirtyLeft, dirtyBottom, dirtyRight);
        }
    }

    public void clear(int row, int startCol, int endCol) {
        int i = getAttrib(row, endCol);
        if (i < 0) {
            return;
        }

        while (i >= 0 && attribs[i].compareTo(row, startCol) >= 0) {
            delete(i);
            i--;
        }

    }

    public void clearLine(int row) {
        clear(row, 0, COLS - 1);
    }

    public void insertLine(int row) {
        int i = getAttrib(row, 0);
        if (i < 0) {
            if (numAttribs == 0) {
                return;
            }
			// (row,0) uses the default attribute, but there are attributes
			// futher down
			i = 0;
        }

        if (attribs[i].compareTo(row, 0) < 0) {
            // The attribute for (row,0) begins before (row, 0),
            // but the next attribute is affected
            i++;
        }

        while (i < numAttribs) {
            attribs[i].incRow(1);
            if (attribs[i].compareTo(TOTAL_ROWS, 0) >= 0) {
                // attribs[i] have falled of the end of the display memory
                delete(i);
            } else {
                i++;
            }
        }

        makeDirty(row, 0, TOTAL_ROWS, COLS);
    }

    public void deleteLine(int row) {
        int i = getAttrib(row, 0);
        if (i < 0) {
            if (numAttribs == 0) {
                return;
            }
			// (row,0) uses the default attribute, but there are attributes
			// futher down
			i = 0;
        }

        if (attribs[i].compareTo(row, 0) < 0) {
            // The attribute for (row,0) begins before (row, 0),
            // but the next attribute is affected
            i++;
        }

        while (i < numAttribs) {
            attribs[i].decRow(1);
            if (attribs[i].compareTo(row, 0) < 0) {
                // attribs[i] has been deleted

                // This is a bit ugly, but otherwise the delete method will
                // get the dirty area all wrong
                attribs[i].incRow(1);
                delete(i);
            } else {
                i++;
            }
        }

        makeDirty(row, 0, TOTAL_ROWS, COLS);
    }

    public void resize(int totalRows, int cols) {
        int i = 0;
        while (i < numAttribs) {
            if (attribs[i].getCol() >= cols) {
                delete(i);
                continue;
            }

            if (attribs[i].getRow() >= totalRows) {
                delete(i);
                continue;
            }

            i++;
        }

        COLS = cols;
        TOTAL_ROWS = totalRows;
    }

    public int[] getAttribsAt(int visTop, int row) {
        int ret[] = new int[COLS];
        int col = 0;

        int a = defaultAttrib;
        int idx = getAttrib(visTop + row, 0);

        if (idx < 0) {
            idx = -1;
        } else {
            if (attribs[idx].compareTo(visTop, 0) >= 0) {
                // Attrib started below top visable row, use it instead
                // of default attribute
                a = attribs[idx].getAttrib();
            }
        }

        idx = nextAttrib(idx, visTop + row);

        int nextAttribStart;
        if (idx < 0) {
            nextAttribStart = COLS;
        } else {
            nextAttribStart = attribs[idx].getCol();
        }

        while (col < COLS) {
            if (col < nextAttribStart) {
                ret[col] = a;
                col++;
            } else {
                a = attribs[idx].getAttrib();
                idx = nextAttrib(idx, visTop + row);
                if (idx < 0) {
                    nextAttribStart = COLS;
                } else {
                    nextAttribStart = attribs[idx].getCol();
                }
            }
        }

        return ret;
    }

    protected int nextAttrib(int idx, int row) {
        int nextAttrib = -1;
        idx++;
        if (idx < numAttribs) {
            if (row != attribs[idx].getRow()) {
                // No more attributes on this line
                nextAttrib = -1;
            } else {
                nextAttrib = idx;
            }
        } else {
            // No more attributes after this
            nextAttrib = -1;
        }
        return nextAttrib;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < numAttribs; i++) {
            if (i > 0) {
                buf.append(' ');
            }
            buf.append(attribs[i].toString());
        }
        return buf.toString();
    }


    protected int getAttrib(int row, int col) {
        if (numAttribs == 0) {
            return -1;
        }

        int ret = find(row, col);
        if (ret == numAttribs) {
            // row,col was larger than the last attributes position
            ret--;
        }
        if (attribs[ret].compareTo(row, col) > 0) {
            // The selected attributes started after (row,col),
            // select the previous
            ret--;
        }
        return ret;
    }

    protected int find(int row, int col) {
        int left = 0;
        int right = numAttribs - 1;
        int middle = 0;

        while (left <= right) {
            middle = (left + right) >>> 1;
            int cmp = attribs[middle].compareTo(row, col);
            if (cmp < 0) {
                left = middle + 1;
            } else if (cmp > 0) {
                right = middle - 1;
            } else {
                break;
            }
        }
        int ret = middle;
        if (left > middle) {
            ret = left;
        }
        return ret;
    }

    protected void makeDirty(int top, int left, int bottom, int right) {
        if (display != null) {
            display.updateDirtyArea(top, left, bottom, right);
        }
    }

}


