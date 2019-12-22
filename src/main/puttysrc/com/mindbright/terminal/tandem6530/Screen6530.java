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
import com.mindbright.terminal.SearchContext;

public class Screen6530 implements AsciiCodes {
    final static protected boolean DEBUG    = false;

    final static public int MIN_ROWS        = 2;
    final static public int MIN_COLS        = 8;
    final static public int MAX_COLS        = 512;
    final static public int MAX_ROWS        = 512;
    final static public int MAX_SAVED_LINES = 8192;

    protected int rows;
    protected int cols;

    protected int curRow;
    protected int curCol;

    protected int bufferRow;
    protected int bufferCol;

    protected int     selectTopRow;
    protected int     selectTopCol;
    protected int     selectBottomRow;
    protected int     selectBottomCol;
    protected boolean hasSelection;
    protected int     selectClickRow = -1;
    protected boolean selectClickState;

    protected boolean insertMode;
    protected boolean wrapAroundLastLine;

    protected char[][]  screen;
    protected boolean[] autowraps;
    protected VideoAttributeMap attribMap;

    protected char defaultChar;
    protected char defaultChars[];

    protected int saveLines;  // The number of lines to save
    protected int visTop;     // Index of line at the top of the screen

    protected final static char[] spacerow  = new char[MAX_COLS];
    protected final static int[]  zerorow   = new int[MAX_COLS];

    static {
        int i;
        for(i = 0; i < MAX_COLS; i++) {
            spacerow[i] = ' ';
            zerorow[i]  = 0;
        }
    }

    protected boolean[] tabStops = new boolean[MAX_COLS];
    protected DisplayView display = null;

    public Screen6530(int rows, int cols, char defaultChar,
                      boolean wrapAroundLastLine) {
        reset();
        this.defaultChar = defaultChar;
        this.wrapAroundLastLine = wrapAroundLastLine;

        attribMap = new VideoAttributeMap(rows, cols, 0);
        defaultChars = new char[MAX_COLS];
        for (int i = 0; i < MAX_COLS; i++) {
            defaultChars[i] = defaultChar;
        }

        resizeBuffers(rows, cols);
        resetTabs();
    }

    public void setDisplay(DisplayView display) {
        this.display = display;
        attribMap.setDisplay(display);
        updateCursorPosition();
    }

    protected void updateDirtyArea(int top, int left, int bottom, int right) {
        if (display != null) {
            if (bottom < top) {
                // repaint whole screen if dirty area has wrapped around
                // last line
                top = 0;
                bottom = rows;
            }
            display.updateDirtyArea(visTop + top, left, visTop + bottom, right);
        }
    }
    protected void updateCursorPosition() {
        if (display != null) {
            display.setCursorPosition(visTop + curRow, curCol);
        }
    }

    public int getSaveLines() {
        return saveLines;
    }

    public boolean setSaveLines(int n) {
        boolean visTopHasChanged = false;
        int oldSaveLines = saveLines;
        int fromRow, toRow, copyRows;
        boolean outOfMemory = false;
        n = (n < 0 ? 0 : n);
        n = (n > MAX_SAVED_LINES ? MAX_SAVED_LINES : n);

        if(saveLines != n) {
            char[][]  oldScreen     = screen;
            boolean[] oldAutowraps  = autowraps;
            saveLines               = n;
            try {
                resizeBuffers(rows, cols);
            } catch (OutOfMemoryError e) {
                saveLines = oldSaveLines;
                resizeBuffers(rows, cols);
                outOfMemory = true;
            }
            toRow       = 0;
            if(oldSaveLines < saveLines) {
                fromRow     = 0;
                copyRows    = oldSaveLines + rows;
            } else {
                if(visTop <= saveLines) {
                    fromRow     = 0;
                    copyRows    = visTop + rows;
                } else {
                    fromRow     = visTop - saveLines;
                    copyRows    = saveLines + rows;
                    visTop -= fromRow;
                    visTopHasChanged = true;
                }
            }
            System.arraycopy(oldScreen, fromRow, screen, toRow, copyRows);
            System.arraycopy(oldAutowraps, fromRow, autowraps, toRow, copyRows);
            attribMap.resize(rows + saveLines, cols);
        }
        if (visTopHasChanged) {
            if (display != null) {
                display.setVisTop(visTop);
            }
        }
        return !outOfMemory;
    }

    public void clearSaveLines() {
        char[][]  oldScreen     = screen;
        boolean[] oldAutowraps  = autowraps;

        resizeBuffers(rows, cols);
        System.arraycopy(oldScreen, visTop, screen, 0, rows);
        System.arraycopy(oldAutowraps, visTop, screen, 0, rows);
        visTop     = 0;
        if (display != null) {
            display.setVisTop(visTop);
        }
    }

    protected char[] makeCharLine() {
        char ret[] = new char[cols];
        System.arraycopy(defaultChars, 0, ret, 0, cols);
        return ret;
    }

    public void resizeBuffers(int rows, int cols) {
        if(DEBUG)
            System.out.println("resizeBuffers: " + cols + "x" + rows);
        this.rows  = rows;
        this.cols  = cols;
        int bufSize = rows + saveLines;

        screen     = new char[bufSize][];
        autowraps  = new boolean[bufSize];

        for (int i = 0; i < bufSize; i++) {
            screen[i] = makeCharLine();
        }

        attribMap.resize(rows + saveLines, cols);
    }

    public void cursorWrite(char c) {
        cursorWrite(c, insertMode, false);
    }
    public void cursorWrite(char c, boolean insert, boolean attribChar) {
        if (DEBUG)
            System.out.println("cursorWrite '"+c+"' ("+(int)c+")");

        if (insert) {
            insertChars(1);
        }
        int idxRow = visTop + curRow;
        int dirtStartRow = idxRow;
        int dirtStartCol = curCol;
        if (!attribChar && attribMap.isAttrib(idxRow, curCol)) {
            attribMap.delete(idxRow, curCol);
        }
        screen[idxRow][curCol++]   = c;

        if(curCol == cols) {
            autowraps[visTop + curRow] = true;
            doLF();
            doCR();
        }

        updateDirtyArea(dirtStartRow, dirtStartCol, curRow+1, curCol+1);
        updateCursorPosition();
    }

    public void bufferWrite(char c) {
        int idxRow = visTop + bufferRow;
        int dirtStartCol = bufferCol;
        screen[idxRow][bufferCol++]   = c;
        if(bufferCol == cols) {
            autowraps[visTop + bufferRow] = true;
            bufferCol = 0;
            bufferRow++;
            if (bufferRow == rows) {
                bufferRow = 0;
            }
        }

        updateDirtyArea(idxRow, dirtStartCol, idxRow+1, bufferCol+1);
    }

    public int getCursorRow() {
        return curRow;
    }
    public int getCursorCol() {
        return curCol;
    }
    public int getScreenTop() {
        return visTop;
    }
    public int getTotalLines() {
        return saveLines + rows;
    }

    public char[] getCharsAt(int visTop, int row) {
        row += visTop;
        if (row < 0 || row >= (rows + saveLines)) {
            return null;
        }

        return screen[row];
    }
    public int[] getAttribsAt(int visTop, int row) {
        return attribMap.getAttribsAt(visTop, row);
    }

    public void setInsertMode(boolean set
                                 ) {
        insertMode = set
                         ;
    }

    public int getVisTop() {
        return visTop;
    }
    public int getCurRow() {
        return curRow;
    }
    public int getCurCol() {
        return curCol;
    }
    public int getSelectTopRow() {
        return selectTopRow;
    }
    public int getSelectTopCol() {
        return selectTopCol;
    }
    public int getSelectBottomRow() {
        return selectBottomRow;
    }
    public int getSelectBottomCol() {
        return selectBottomCol;
    }

    public int getRows() {
        return rows;
    }
    public int getCols() {
        return cols;
    }

    public void doBS() {
        if(DEBUG)
            System.out.println("doBS");
        cursorBackward(1);
    }

    public void doBackTabs(int n) {
        if(DEBUG)
            System.out.println("doBackTabs: " + n);
        int i;
        if(curCol > 0 && n >= 0) {
            for(i = curCol - 1; i >= 0; i--) {
                if(tabStops[i]) {
                    if(--n == 0)
                        break;
                }
            }
            curCol = (i < 0 ? 0 : i);
            updateCursorPosition();
        }
    }

    public void doCR() {
        curCol = 0;
        updateCursorPosition();
        if(DEBUG)
            System.out.println("doCR");
    }

    public void doLF() {
        cursorDown(1);
        if(DEBUG)
            System.out.println("doLF");
    }

    public int getCursorV() {
        return curRow;
    }
    public int getCursorH() {
        return curCol;
    }

    public void cursorSetPos(int v, int h, boolean relative) {
        cursorSetPos(v, h);
    }
    public void cursorSetPos(int v, int h) {
        if(DEBUG)
            System.out.println("cursorSetPos: " + v + ", " + h);
        int maxV = rows - 1;
        int maxH = cols - 1;
        int minV = 0;
        int minH = 0;
        if(v < minV)
            v = minV;
        if(h < minH)
            h = minH;
        if(v > maxV)
            v = maxV;
        if(h > maxH)
            h = maxH;
        curRow = v;
        curCol = h;
        updateCursorPosition();
    }

    public void bufferSetPos(int v, int h) {
        if(DEBUG)
            System.out.println("bufferSetPos: " + v + ", " + h);
        int maxV = rows - 1;
        int maxH = cols - 1;
        int minV = 0;
        int minH = 0;
        if(v < minV)
            v = minV;
        if(h < minH)
            h = minH;
        if(v > maxV)
            v = maxV;
        if(h > maxH)
            h = maxH;
        bufferRow = v;
        bufferCol = h;
    }

    public void cursorHome() {
        cursorSetPos(0, 0);
    }

    public void cursorHomeDown() {
        cursorSetPos(rows - 1, 0);
    }

    public void cursorToLastCharOnScreen() {
        cursorToLastCharOnRow(visTop + rows - 1, 0, true);
        if (curCol == (cols - 1)) {
            cursorDown(1);
            doCR();
        }
    }

    public void cursorToLastCharOnRow() {
        cursorToLastCharOnRow(visTop + curRow, curCol, true);
    }
    protected void cursorToLastCharOnRow(int row, int col, boolean first) {
        int lastNonBlankCol = prevPrintedChar(row, cols - 1) + 1;

        if (lastNonBlankCol == cols) {
            if (row < getLastRow()-1) {
                lastNonBlankCol = 0;
                row++;
            } else {
                lastNonBlankCol = cols - 1;
            }
        }

        if (row == getLastRow()-1) {
            cursorSetPos(row, lastNonBlankCol);
            return;
        }

        if (lastNonBlankCol <= col && (col > 0 || first)) {
            cursorToLastCharOnRow(row + 1, 0, false);
            return;
        }

        cursorSetPos(row, lastNonBlankCol);
    }

    public void cursorUp(int n) {
        if(DEBUG)
            System.out.println("cursorUp: " + n);
        curRow -= n;
        if(curRow < 0) {
            if (wrapAroundLastLine) {
                curRow = rows + curRow;
            } else {
                curRow = 0;
                setVisTopDelta(-1);
            }
        }
        updateCursorPosition();
    }
    public void cursorDown(int n) {
        if(DEBUG)
            System.out.println("cursorDown: " + n);

        if (!wrapAroundLastLine && (visTop + curRow) == (getLastRow() - 1)) {
            // Last line in display memory, add a line
            scrollUp(n);
        }

        curRow += n;
        if(curRow > (rows - 1)) {
            int exceedRows = curRow - (rows - 1);
            if (wrapAroundLastLine) {
                curRow = exceedRows - 1;
            } else {
                curRow = rows - 1;
                setVisTopDelta(exceedRows);
            }
        }

        updateCursorPosition();
    }
    public void cursorForward(int n) {
        if(DEBUG)
            System.out.println("cursorFwd: " + n);
        curCol += n;
        if(curCol >= cols) {
            curCol = 0;
            cursorDown(1);
        }
        updateCursorPosition();
    }
    public void cursorBackward(int n) {
        if(DEBUG)
            System.out.println("cursorBack: " + n);
        curCol -= n;
        if(curCol < 0) {
            curCol = cols - (0 - curCol);
            cursorUp(1);
        }
        updateCursorPosition();
    }

    public void scrollUp(int n) {
        int i, j = 0;
        boolean visTopHasChanged = false;

        if(DEBUG)
            System.out.println("scrollUp: " + n);

        if(saveLines > 0) {
            // We must save outscrolled lines
            int sl; // Lines to scroll (a full screen max)
            sl = (n < rows ? n : rows);
            if ((visTop + sl) > saveLines) {
                // We are out of save lines memory, delete the oldest
                // lines
                if (hasSelection) {
                    if ((selectTopRow - n) < 0) {
                        // Selection is scrolled out of saved lines
                        resetSelection();
                    } else {
                        scrollSelection(-n);
                    }
                }
                int ll = rows - sl;
                System.arraycopy(screen, sl,
                                 screen, 0, saveLines + ll);
                System.arraycopy(autowraps, sl,
                                 autowraps, 0, saveLines + ll);
                for (i = rows - sl; i < rows; i++) {
                    screen[saveLines + i]     = makeCharLine();
                    autowraps[saveLines + i] = false;
                }
            } else {
                visTop    += sl;
                visTopHasChanged = true;
            }
        } else {
            if(n < rows) {
                int from = visTop + 0 + n;
                int to = visTop + 0;
                int len = rows - n;
                j = (rows - n) + 0;
                System.arraycopy(screen, from, screen, to, len);
                System.arraycopy(autowraps, from, autowraps, to, len);
            }
            for(i = j; i < rows; i++) {
                screen[visTop + i]     = makeCharLine();
                autowraps[visTop + i]  = false;
            }
        }

        if (visTopHasChanged) {
            if (display != null) {
                display.setVisTop(visTop);
            }
        }
        updateCursorPosition();
        updateDirtyArea(0, 0, rows, cols);
    }

    public void scrollDown(int n) {
        int i, j = rows;

        if(DEBUG)
            System.out.println("scrollDown: " + n);

        if(n < rows) {
            j = 0 + n;
            int from = visTop + 0;
            int to = visTop + 0 + n;
            int len = rows - n;
            System.arraycopy(screen, from, screen, to, len);
            System.arraycopy(autowraps, from, autowraps, to, len);
        }
        for(i = 0; i < j; i++) {
            screen[visTop + i]     = makeCharLine();
            autowraps[visTop + i]  = false;
        }
        updateCursorPosition();
        updateDirtyArea(0, 0, rows, cols);
    }

    public void clearBelow() {
        clearBelow(true);
    }
    public void clearBelow(boolean atCursor) {
        if(DEBUG)
            System.out.println("clearBelow atCursor=" + atCursor);
        clearRight(atCursor);
        int i;
        int startRow = atCursor ? curRow : bufferRow;

        for(i = startRow + visTop; i < getTotalLines(); i++) {
            screen[i]     = makeCharLine();
            autowraps[i]  = false;
            attribMap.clearLine(i);
        }

        if (isIntersectingSelect(startRow, 0, rows, cols)) {
            resetSelection();
        }
        updateDirtyArea(startRow, 0, rows, cols);
    }

    protected void clear(int row, int startCol, int endCol) {
        System.arraycopy(defaultChars, 0,
                         screen[visTop + row], startCol,
                         endCol - startCol);
        attribMap.clear(visTop + row, startCol, endCol);

        if (isIntersectingSelect(row, startCol, row + 1, endCol + 1)) {
            resetSelection();
        }
        updateDirtyArea(row, startCol, row + 1, endCol + 1);
    }

    public void clear(int startRow, int startCol, int endRow, int endCol) {
        if (startRow == endRow) {
            clear(startRow, startCol, endCol);
        } else {
            clear(startRow, startCol, cols - 1);
            for (int i = startRow + 1; i < endRow; i++) {
                clear(i, 0, cols - 1);
            }
            clear(endRow, 0, endRow);
        }
    }

    public void clearScreen() {
        if(DEBUG)
            System.out.println("clearScreen");
        int i;

        for(i = 0; i < rows; i++) {
            screen[visTop + i]     = makeCharLine();
            autowraps[visTop + i]  = false;
            attribMap.clearLine(visTop + i);
        }

        if (isIntersectingSelect(0, 0, rows, cols)) {
            resetSelection();
        }
        updateDirtyArea(0, 0, rows, cols);
    }

    public void clearRight() {
        clearRight(true);
    }

    public void clearRight(boolean atCursor) {
        if(DEBUG)
            System.out.println("clearRight atCursor="+atCursor);
        int row = atCursor ? curRow : bufferRow;
        int col = atCursor ? curCol : bufferCol;

        clear(row, col, cols);
    }

    public void insertChars(int n) {
        insertChars(n, true);
    }
    public void insertChars(int n, boolean atCursor) {
        if(DEBUG)
            System.out.println("inserChars: "+n+" atCursor="+atCursor);
        int edge = cols;
        int row = atCursor ? curRow : bufferRow;
        int col = atCursor ? curCol : bufferCol;

        if(col < 0 || col > cols)
            return;
        if((col + n) < cols) {
            edge = col +  n;
            System.arraycopy(screen[visTop + row], col,
                             screen[visTop + row], edge,
                             cols - edge);
        }
        System.arraycopy(spacerow, 0, screen[visTop + row], col, edge - col);
        attribMap.insertChars(visTop + row, col, n);
        if (isIntersectingSelect(row, col, row + 1, cols)) {
            resetSelection();
        }
        updateDirtyArea(row, col, row + 1, cols);
    }

    public void deleteChars(int n) {
        deleteChars(n, true);
    }
    public void deleteChars(int n, boolean atCursor) {
        if(DEBUG)
            System.out.println("deleteChars: " + n);

        int row = atCursor ? curRow : bufferRow;
        int col = atCursor ? curCol : bufferCol;
        int edge = col;

        if(col < 0 || col > cols)
            return;
        if((col + n) < cols) {
            edge = cols - n;
            System.arraycopy(screen[visTop + row], col + n,
                             screen[visTop + row], col,
                             edge - col);
        }
        System.arraycopy(spacerow, 0, screen[visTop + row], edge, cols - edge);
        attribMap.deleteChars(visTop + row, col, n);
        if (isIntersectingSelect(row, col, row + 1, cols)) {
            resetSelection();
        }
        updateDirtyArea(row, col, row + 1, cols);
    }

    public void insertLines(int n) {
        insertLines(n, true);
    }
    public void insertLines(int n, boolean atCursor) {
        if(DEBUG)
            System.out.println("insertLines: " + n);
        int row = atCursor ? curRow : bufferRow;
        int i, edge = rows;

        if(row < 0 || row > rows)
            return;

        if(row + n < rows) {
            edge = row + n;
            System.arraycopy(screen, visTop + row,
                             screen, visTop + edge, rows - edge);
            System.arraycopy(autowraps, visTop + row,
                             autowraps, visTop + edge, rows - edge);
        }
        for(i = row; i < edge; i++) {
            screen[visTop + i]     = makeCharLine();
            autowraps[visTop + i]  = false;
        }
        for (i = 0; i < n; i++) {
            attribMap.insertLine(visTop + row);
        }

        if (isIntersectingSelect(row, 0, rows, cols)) {
            resetSelection();
        }
        updateDirtyArea(row, 0, rows, cols);
    }

    public void deleteLines(int n) {
        deleteLines(n, true);
    }
    public void deleteLines(int n, boolean atCursor) {
        if(DEBUG)
            System.out.println("deleteLines: " + n);
        int row = atCursor ? curRow : bufferRow;
        int i, edge = row;

        if(row < 0 || row > rows)
            return;

        if(row + n < rows) {
            edge = rows - n;
            System.arraycopy(screen, visTop + row + n, screen, visTop + row,
                             edge - row);
            System.arraycopy(autowraps, visTop + row + n,
                             autowraps, visTop + row,
                             edge - row);
        }
        for(i = edge; i < rows; i++) {
            screen[visTop + i]     = makeCharLine();
            autowraps[visTop + i]  = false;
        }
        for (i = 0; i < n; i++) {
            attribMap.deleteLine(visTop + row);
        }

        if (isIntersectingSelect(row, 0, rows, cols)) {
            resetSelection();
        }
        updateDirtyArea(row, 0, rows, cols);
    }

    public void setAttribute(int attr) {
        attribMap.add(visTop + curRow, curCol, attr);
        cursorWrite(' ', insertMode, true);
    }
    public void setDefaultAttribute(int attr) {
        attribMap.setDefault(attr);
    }

    protected int nextPrintedChar(int row, int col) {
        int i;
        for(i = col; i < cols; i++)
            if(screen[row][i] != defaultChar)
                break;
        return i;
    }

    protected int prevPrintedChar(int row, int col) {
        int i;
        for(i = col; i >= 0; i--)
            if(screen[row][i] != defaultChar)
                break;
        return i;
    }

    protected String addSpaces(int start, int end) {
        int n = end - start;

        if(end == cols)
            return "";

        char[] spaces = new char[n];
        System.arraycopy(spacerow, 0, spaces, 0, n);

        return new String(spaces);
    }

    protected boolean isIntersectingSelect(int topRow, int topCol,
                                           int bottomRow, int bottomCol) {
        if (!hasSelection) {
            return false;
        }

        topRow += visTop;
        bottomRow += visTop;

        int selectLeft, selectRight;
        if (selectTopCol < selectBottomCol) {
            selectLeft = selectTopCol;
            selectRight = selectBottomCol;
        } else {
            selectLeft = selectBottomCol;
            selectRight = selectTopCol;
        }

        int left, right;
        if (topCol < bottomCol) {
            left = topCol;
            right = bottomCol;
        } else {
            left = bottomCol;
            right = topCol;
        }

        if (selectTopRow > bottomRow) {
            return false;
        }
        if (selectBottomRow < topRow) {
            return false;
        }
        if (selectLeft > right) {
            return false;
        }
        if (selectRight < left) {
            return false;
        }

        return true;
    }


    public void resetSelection() {
        hasSelection = false;
        if (display != null) {
            display.resetSelection();
        }
    }
    public void selectAll() {
        setSelection(visTop, 0, visTop + rows-1, cols-1);
    }
    protected void scrollSelection(int n) {
        if (!hasSelection) {
            return;
        }
        int newTopRow = selectTopRow + n;
        int newBottomRow = selectBottomRow + n;
        setSelection(newTopRow, selectTopCol, newBottomRow, selectBottomCol);
    }

    public void setSelection(int anchorRow, int anchorCol,
                             int endRow, int endCol) {
        if (anchorRow < endRow) {
            selectTopRow = anchorRow;
            selectTopCol = anchorCol;
            selectBottomRow = endRow;
            selectBottomCol = endCol;
        } else if (anchorRow == endRow) {
            selectTopRow = selectBottomRow = anchorRow;
            if (anchorCol < endCol) {
                selectTopCol = anchorCol;
                selectBottomCol = endCol;
            } else {
                selectTopCol = endCol;
                selectBottomCol = anchorCol;
            }
        } else {
            selectTopRow = endRow;
            selectTopCol = endCol;
            selectBottomRow = anchorRow;
            selectBottomCol = anchorCol;
        }
        hasSelection = true;
        if (display != null) {
            display.resetSelection();
            display.setSelection(selectTopRow, selectTopCol,
                                 selectBottomRow, selectBottomCol);
        }
    }

    public String getSelection(String eol) {
        if (!hasSelection) {
            return null;
        }
        if (eol == null) {
            eol = "\r";
        }
        return getContents(selectTopRow, selectTopCol,
                           selectBottomRow, selectBottomCol, eol);
    }

    protected String getContents(int startRow, int startCol,
                                 int endRow, int endCol, String eol) {
        int i, j, n;

        StringBuilder result = new StringBuilder();

        if (startRow != endRow) {
            for(i = startCol; i < cols; i++) {
                if(screen[startRow][i] == 0) {
                    n = nextPrintedChar(startRow, i);
                    result.append(addSpaces(i, n));
                    i = n - 1;
                } else
                    result.append(screen[startRow][i]);
            }
            if(i == cols && !autowraps[startRow])
                result.append(eol);
            for(i = startRow + 1; i < endRow; i++) {
                for(j = 0; j < cols; j++) {
                    if(screen[i][j] == 0) {
                        n = nextPrintedChar(i, j);
                        result.append(addSpaces(j, n));
                        j = n - 1;
                    } else
                        result.append(screen[i][j]);
                }
                if(!autowraps[i])
                    result.append(eol);
            }
            for(i = 0; i <= endCol; i++) {
                if(screen[endRow][i] == 0) {
                    n = nextPrintedChar(endRow, i);
                    result.append(addSpaces(i, n));
                    i = n - 1;
                } else
                    result.append(screen[endRow][i]);
            }
            if(i == cols && !autowraps[endRow])
                result.append(eol);
        } else {
            for(i = startCol; i <= endCol; i++) {
                if(screen[startRow][i] == 0) {
                    n = nextPrintedChar(startRow, i);
                    result.append(addSpaces(i, n));
                    i = n - 1;
                } else
                    result.append(screen[startRow][i]);
            }
            if(i == cols)
                result.append(eol);
        }

        return result.toString();
    }

    public void resetClickSelect() {
        selectClickRow   = -1;
        selectClickState = false;
    }
    public void doClickSelect(int row, int col, String selectDelims) {
        int selectLeft, selectRight;
        if(selectClickRow == row && selectClickState) {
            selectLeft = 0;
            selectRight = cols -1;
        } else {
            int i;
            if(screen[row][col] != 0) {
                for(i = col; i >= 0; i--) {
                    if((selectDelims.indexOf(screen[row][i]) != -1) ||
                            screen[row][i] == 0) {
                        break;
                    }
                }
                selectLeft = i + 1;
                for(i = col; i < cols ; i++) {
                    if((selectDelims.indexOf(screen[row][i]) != -1) ||
                            screen[row][i] == 0) {
                        break;
                    }
                }
                selectRight = i - 1;
            } else {
                selectLeft = prevPrintedChar(row, col) + 1;
                selectRight   = nextPrintedChar(row, col) - 1;
            }
            selectLeft  = (selectLeft  > col) ? col : selectLeft;
            selectRight = (selectRight < col) ? col : selectRight;
        }
        selectClickState = !selectClickState;
        selectClickRow   = row;
        setSelection(row, selectLeft, row, selectRight);
    }

    public SearchContext search(SearchContext lastContext, String key,
                                boolean reverse, boolean caseSens) {
        int     len = key.length();
        int     lastRow = rows + saveLines;
        int     startRow;
        int     startCol;
        boolean found    = false;
        int     i, j = 0;
        char    fc = (caseSens ? key.charAt(0) :
                      Character.toLowerCase(key.charAt(0)));

        if(reverse) {
            if (lastContext != null) {
                startRow = lastContext.getStartRow();
                startCol = lastContext.getStartCol() - 1;
                if (startCol < 0) {
                    startRow--;
                }
            } else {
                startRow = lastRow;
                startCol = cols - len;
            }
            startRow = startRow < 0 ? 0 : startRow;
            startRow = startRow > (lastRow - 1) ? lastRow - 1 : startRow;
            startCol = startCol < 0 ? 0 : startCol;
            startCol = startCol > (cols - 1) ? cols - 1 : startCol;

            try {
foundItRev:
                for(i = startRow; i >= 0; i--) {
                    for(j = startCol; j >= 0; j--) {
                        if(doMatch(key, fc, screen[i], j, caseSens, len)) {
                            break foundItRev;
                        }
                    }
                    startCol = cols-1;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
                return null;
            }
            if(i >= 0) {
                found = true;
            }
        } else {
            if (lastContext != null) {
                startRow = lastContext.getEndRow();
                startCol = lastContext.getEndCol() + 1;
                if (startCol >= cols) {
                    startRow++;
                }
            } else {
                startRow = 0;
                startCol = 0;
            }
            startRow = startRow < 0 ? 0 : startRow;
            startRow = startRow > (lastRow - 1) ? lastRow - 1 : startRow;
            startCol = startCol < 0 ? 0 : startCol;
            startCol = startCol > (cols - 1) ? cols - 1 : startCol;

            try {
foundIt:
                for(i = startRow; i < lastRow; i++) {
                    for(j = startCol; j < cols - len; j++) {
                        if(doMatch(key, fc, screen[i], j, caseSens, len)) {
                            break foundIt;
                        }
                    }
                    startCol = 0;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
                return null;
            }
            if(i < lastRow) {
                found = true;
            }
        }

        if(found) {
            return new SearchContext(i, j, i, j + len - 1);
        }
        return null;
    }

    protected boolean doMatch(String findStr, char firstChar, char[] chars,
                              int idx, boolean caseSens, int len) {
        String cmpStr;
        if(caseSens) {
            if(chars[idx] != firstChar) {
                return false;
            }
            cmpStr = new String(chars, idx, len);
            if(cmpStr.equals(findStr)) {
                return true;
            }
        } else {
            if(Character.toLowerCase(chars[idx]) != firstChar) {
                return false;
            }
            cmpStr = new String(chars, idx, len);
            if(cmpStr.equalsIgnoreCase(findStr)) {
                return true;
            }
        }
        return false;
    }

    public String getStatus() {
        return "size="+rows+","+cols+" "+
               "visTop="+visTop+" "+
               "curPos="+curRow+","+curCol+" ";
    }

    public int getLastRow() {
        return saveLines + rows;
    }

    public void setVisTop(int row) {
        visTop = row;
        if (visTop < 0) {
            visTop = 0;
        }
        if (visTop > saveLines) {
            visTop = saveLines;
        }
        if (display != null) {
            display.setVisTop(visTop);
        }
        updateCursorPosition();
    }

    public void setVisTopDelta(int delta) {
        setVisTop(visTop + delta);
    }

    public String spaceToNextTabStop() {
        int i;
        StringBuilder buf = new StringBuilder();
        for(i = curCol + 1; i < cols; i++) {
            buf.append(' ');
            if(tabStops[i]) {
                break;
            }
        }
        if (i == cols) { // Make sure that cursor wraps
            buf.append(' ');
        }
        return buf.toString();
    }

    public void doHTab() {
        int i;
        if(curCol < cols) {
            for(i = curCol + 1; i < cols; i++) {
                if(tabStops[i]) {
                    break;
                }
            }
            if (i < cols) {
                cursorSetPos(curRow, i);
            } else {
                doCR();
                cursorDown(1);
            }
        }
        if(DEBUG)
            System.out.println("doHTab");
    }

    public void setTab(boolean atCursor) {
        if (atCursor) {
            tabStops[curCol] = true;
        } else {
            tabStops[bufferCol] = true;
        }
    }

    public void clearTab(boolean atCursor) {
        if (atCursor) {
            tabStops[curCol] = false;
        } else {
            tabStops[bufferCol] = false;
        }
    }

    public void clearAllTabs() {
        for(int i = 0; i < MAX_COLS; i++)
            tabStops[i] = false;
    }

    public void resetTabs() {
        for(int i = 0; i < MAX_COLS; i++) {
            tabStops[i] = false;
        }
    }

    public void reset() {
        curRow = curCol = 0;
        bufferRow = bufferCol = 0;
        visTop = 0;
    }

    public Screen6530Buffer getBuffer() {
        int rowIdx;
        Screen6530Buffer buf = new Screen6530Buffer(rows, cols);
        for (int row = 0; row < rows; row++) {
            rowIdx = visTop + row;
            for (int col = 0; col < cols; col++) {
                if (attribMap.isAttrib(rowIdx, col)) {
                    buf.setAttrib(row, col, attribMap.attribAt(rowIdx, col));
                } else {
                    buf.setChar(row, col, screen[rowIdx][col]);
                }
            }
        }
        return buf;
    }

    public void setBuffer(Screen6530Buffer buf) {
        int rowIdx;
        int maxCols = buf.getCols() < cols ? buf.getCols() : cols;
        int maxRows = buf.getRows() < rows ? buf.getRows() : rows;

        for (int row = 0; row < maxRows; row++) {
            rowIdx = visTop + row;
            for (int col = 0; col < maxCols; col++) {
                if (buf.isAttrib(row, col)) {
                    attribMap.add(rowIdx, col, buf.getAttrib(row, col));
                    screen[rowIdx][col] = ' ';
                } else {
                    screen[rowIdx][col] = buf.getChar(row, col);
                }
            }
        }
    }

    public String readWhole() {
        return read(0, 0, rows - 1, cols - 1);
    }

    public String read(int startRow, int startCol, int endRow, int endCol) {
        StringBuilder buf = new StringBuilder();
        if (startRow == endRow) {
            readLine(buf, startRow, startCol, endCol);
        } else {
            readLine(buf, startRow, startCol, cols - 1);
            for (int i = startRow + 1; i < endRow; i++) {
                readLine(buf, i, 0, cols - 1);
            }
            readLine(buf, endRow, 0, endCol);
        }
        buf.append(EOT);

        return buf.toString();
    }

    protected void readLine(StringBuilder buf, int row, int startCol,
                            int endCol) {
        int lastNonBlankCol = prevPrintedChar(visTop + row, cols - 1);

        // Adjust lastNonBlankCol if there are any attribute chars
        // right of lastNonBlankCol
        for (int i = cols - 1; i > lastNonBlankCol; i--) {
            if (attribMap.isAttrib(visTop + row, i)) {
                lastNonBlankCol = i;
                break;
            }
        }

        if (startCol < lastNonBlankCol) {
            int end = lastNonBlankCol < endCol ? lastNonBlankCol : endCol;
            int attribs[] = getAttribsAt(visTop, row);

            for (int i = startCol; i <= end; i++) {
                if (attribMap.isAttrib(visTop + row, i)) {
                    buf.append(ESC);
                    buf.append('6');
                    buf.append((char) (0x20 | attribs[i]));
                } else {
                    buf.append(screen[visTop + row][i]);
                }
            }
        }
        buf.append(CR);
    }

}
