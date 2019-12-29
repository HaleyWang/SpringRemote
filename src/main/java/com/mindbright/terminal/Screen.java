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

package com.mindbright.terminal;

public class Screen {

    final static protected boolean DEBUG = false;

    final static public int GRAVITY_SOUTHWEST = 0;
    final static public int GRAVITY_NORTHWEST = 1;

    final static public int MIN_ROWS        = 2;
    final static public int MIN_COLS        = 8;
    final static public int MAX_COLS        = 512;
    final static public int MAX_ROWS        = 512;
    final static public int MAX_SAVED_LINES = 8192;

    protected int rows;
    protected int cols;

    protected int windowTop;
    protected int windowBottom;
    protected int windowLeft;
    protected int windowRight;
    protected boolean complexScroll;

    protected int curRow; // Row of cursor couted from the upper left corner
    protected int curCol; // Cursor column, counted from the left edge

    protected int     selectTopRow;
    protected int     selectTopCol;
    protected int     selectBottomRow;
    protected int     selectBottomCol;
    protected boolean hasSelection;
    protected int     selectClickRow = -1;
    protected boolean selectClickState;

    protected int curAttr;

    protected boolean autoLF;
    protected boolean autoWrap;
    protected boolean autoReverseWrap;
    protected boolean insertMode;

    protected int curRowSave;
    protected int curColSave;
    protected int curAttrSave;

    protected char[][]  screen;
    protected int[][]   attributes;
    protected boolean[] autowraps;

    protected char defaultChar;
    protected char defaultChars[];
    protected int defaultAttribs[];

    protected int saveLines;  // The number of lines to save
    protected int visTop;     // Index of line at the top of the
                              // screen, counted from top of buffer
                              // with remembered lines.
    

    protected final static char[] spacerow  = new char[MAX_COLS];
    protected final static int[]  zerorow   = new int[MAX_COLS];

    static {
        for(int i = 0; i < MAX_COLS; i++) {
            spacerow[i] = ' ';
            zerorow[i]  = 0;
        }
    }

    protected boolean[] tabStops = new boolean[MAX_COLS];
    protected DisplayView display = null;

    public Screen(int rows, int cols) {
        this(rows, cols, (char) 0x00, 0);
    }

    public Screen(int rows, int cols, char defaultChar, int defaultAttrib) {
        curAttr       = DisplayModel.ATTR_CHARDRAWN;
        curRow        = 0;
        curCol        = 0;
        visTop        = 0;
        this.defaultChar = defaultChar;

        defaultChars = new char[MAX_COLS];
        defaultAttribs = new int[MAX_COLS];
        for (int i = 0; i < MAX_COLS; i++) {
            defaultChars[i] = defaultChar;
            defaultAttribs[i] = defaultAttrib;
        }

        resizeBuffers(rows, cols);
        resetWindow();
        resetTabs();
    }

    public void setDisplay(DisplayView display) {
        this.display = display;
    }

    protected void updateDirtyArea(int top, int left, int bottom, int right) {
        if (display != null) {
            display.updateDirtyArea(visTop + top, left, visTop + bottom, right);
        }
    }
    protected void setCursorPosition(int row, int col) {
        if (display != null) {
            display.setCursorPosition(visTop + row, col);
        }
    }

    public int getSaveLines() {
        return saveLines;
    }

    protected char[][]  save_screen;
    protected int[][]   save_attributes;
    protected boolean[] save_autowraps;
    protected int save_rows, save_cols;

    public synchronized boolean screenSave() {
        boolean outOfMemory = false;

	save_screen = screen;
	save_attributes = attributes;
	save_autowraps = autowraps;
	save_rows = rows;
	save_cols = cols;

	try {
	    resizeBuffers(rows, cols);
	} catch (OutOfMemoryError e) {
	    screen = save_screen;
	    attributes = save_attributes;
	    autowraps = save_autowraps;
	    save_screen = null;
	    save_attributes = null;
	    save_autowraps = null;
	    outOfMemory = true;
	}
        return !outOfMemory;
    }

    public synchronized boolean screenRestore() {
        if (save_screen == null) {
	  return false;
	} 
	if (save_rows != rows || save_cols != cols) {
	  clearScreen();
	} else {
	  screen = save_screen;
	  attributes = save_attributes;
	  autowraps = save_autowraps;
	}
	save_screen = null;
	save_attributes = null;
	save_autowraps = null;
        updateDirtyArea(0, 0, rows, cols);
	return true;
    }

    public synchronized boolean setSaveLines(int n) {
        boolean visTopHasChanged = false;
        int oldSaveLines = saveLines;
        int fromRow, toRow, copyRows;
        boolean outOfMemory = false;
        n = (n < 0 ? 0 : n);
        n = (n > MAX_SAVED_LINES ? MAX_SAVED_LINES : n);

        if(saveLines != n) {
            char[][]  oldScreen     = screen;
            int[][]   oldAttributes = attributes;
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
            System.arraycopy(oldAttributes, fromRow,attributes,toRow,copyRows);
            System.arraycopy(oldAutowraps, fromRow, autowraps, toRow,copyRows);
        }
        if (visTopHasChanged) {
            if (display != null) {
                display.setVisTop(visTop);
            }
        }
        return !outOfMemory;
    }

    public synchronized void clearSaveLines() {
        char[][]  oldScreen     = screen;
        int[][]   oldAttributes = attributes;
        boolean[] oldAutowraps  = autowraps;

        resizeBuffers(rows, cols);
        System.arraycopy(oldScreen, visTop, screen, 0, rows);
        System.arraycopy(oldAttributes, visTop, attributes, 0, rows);
        System.arraycopy(oldAutowraps, visTop, autowraps, 0, rows);
        visTop = 0;
        if (display != null) {
            display.setVisTop(visTop);
            setCursorPosition(curRow, curCol);
        }
    }

    protected char[] makeCharLine() {
        char ret[] = new char[cols];
        System.arraycopy(defaultChars, 0, ret, 0, cols);
        return ret;
    }

    protected int[] makeAttribLine() {
        int ret[] = new int[cols];
        System.arraycopy(defaultAttribs, 0, ret, 0, cols);
        return ret;
    }

    public synchronized void resizeBuffers(int rows, int cols) {
        if(DEBUG)
            System.out.println("resizeBuffers: " + cols + "x" + rows);
        this.rows  = rows;
        this.cols  = cols;
        int bufSize = rows + saveLines;

        screen     = new char[bufSize][];
        attributes = new int[bufSize][];
        autowraps  = new boolean[bufSize];

        for (int i = 0; i < bufSize; i++) {
            screen[i] = makeCharLine();
            attributes[i] = makeAttribLine();
        }
    }

    public void writeLineDrawChar(char c) {
        writeChar(c, curAttr | DisplayModel.ATTR_LINEDRAW, false);
    }
    public void writeChar(char c) {
        writeChar(c, curAttr, insertMode);
    }
    public void writeChar(char c, int attr) {
        writeChar(c, attr, insertMode);
    }
    public synchronized void writeChar(char c, int attr, boolean insert) {
        if (DEBUG)
            System.out.println("writeChar("+curCol+","+curRow+"): " + c);
        boolean doublewidth = false;
        int delta = 1;
        if (c > 255 && display.isWide(c)) {
            doublewidth = true;
            delta = 2;
        }
        checkWrap();
        if (insert) {
            insertChars(delta);
        }
        int idxRow = visTop + curRow;
        attributes[idxRow][curCol] = attr;
        screen[idxRow][curCol++]   = c;
        if (doublewidth) {
            attributes[idxRow][curCol-1] |= DisplayModel.ATTR_DWIDTH_L;
            if (attributes[idxRow].length > curCol)
                attributes[idxRow][curCol++] = DisplayModel.ATTR_DWIDTH_R;
        }
        setCursorPosition(curRow, curCol);
        updateDirtyArea(curRow, curCol-delta, curRow+1, curCol);
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
        return visTop + rows;
    }

    public char[] getCharsAt(int row) {
        return getCharsAt(row, false);
    }
    public synchronized char[] getCharsAt(int row, boolean screenRelative) {
        if (screenRelative) {
            row += visTop;
        }
        if (row < 0 || row >= (rows + saveLines)) {
            return null;
        }

        return screen[row];
    }
    public int[] getAttribsAt(int row) {
        return getAttribsAt(row, false);
    }
    public synchronized int[] getAttribsAt(int row, boolean screenRelative) {
        if (screenRelative) {
            row += visTop;
        }
        if (row < 0 || row >= (rows + saveLines)) {
            System.out.println("Screen: row is "+row+" max="+(rows+saveLines));
            return null;
        }

        return attributes[row];
    }

    public void setAutoLF(boolean set) {
        autoLF = set;
    }
    public void setAutoWrap(boolean set) {
        autoWrap = set;
    }
    public void setAutoReverseWrap(boolean set) {
        autoReverseWrap = set;
    }
    public void setInsertMode(boolean set) {
        insertMode = set;
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
    protected void checkWrap() {
        if(curCol == cols) {
            if(autoWrap) {
                autowraps[visTop + curRow] = true;
                curRow += 1;
                curCol = 0;
                if(curRow == windowBottom) {
                    scrollUp(1);
                    curRow = windowBottom - 1;
                }
            } else
                curCol--;
        }
    }

    public void fillScreen(char c) {
        if(DEBUG)
            System.out.println("fillScreen");
        int i;
        int[] attrLine = makeAttribLine();
        char[] charLine = new char[cols];

        for(i = 0; i < cols; i++) {
            attrLine[i] = curAttr;
            charLine[i] = c;
        }
        for(i = windowTop; i < windowBottom; i++) {
            screen[visTop + i]     = new char[cols];
            attributes[visTop + i] = makeAttribLine();
            autowraps[visTop + i]  = false;
            System.arraycopy(charLine, 0, screen[visTop + i], 0, cols);
            System.arraycopy(attrLine, 0, attributes[visTop + i], 0, cols);
        }
        if (isIntersectingSelect(0, 0, rows, cols)) {
            resetSelection();
        }
        updateDirtyArea(0, 0, rows, cols);
    }

    public void doBS() {
        if(DEBUG)
            System.out.println("doBS");
        cursorBackward(1);
    }

    public void doTab() {
        int i;
        if(curCol < windowRight) {
            for(i = curCol + 1; i < windowRight; i++) {
                if(tabStops[i]) {
                    break;
                }
            }
            curCol = (i < windowRight ? i : windowRight - 1);
            setCursorPosition(curRow, curCol);
        }
        if(DEBUG)
            System.out.println("doTab");
    }

    public void doTabs(int n) {
        while(n-- > 0)
            doTab();
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
            setCursorPosition(curRow, curCol);
        }
    }

    public void setTab(int col) {
        tabStops[col] = true;
    }

    public void clearTab(int col) {
        tabStops[col] = false;
    }

    public void resetTabs() {
        for(int i = 0; i < MAX_COLS; i++)
            tabStops[i] = ((i % 8) == 0);
    }

    public void clearAllTabs() {
        for(int i = 0; i < MAX_COLS; i++)
            tabStops[i] = false;
    }

    public void doCR() {
        curCol = windowLeft;
        setCursorPosition(curRow, curCol);
        if(DEBUG)
            System.out.println("doCR");
    }

    public void doLF() {
        curRow += 1;
        if(curRow == windowBottom) {
            scrollUp(1);
            curRow = windowBottom - 1;
        }
        setCursorPosition(curRow, curCol);
        if(autoLF) {
            doCR();
        }
        if(DEBUG)
            System.out.println("doLF");
    }

    public void resetWindow() {
        windowTop     = 0;
        windowBottom  = rows;
        windowLeft    = 0;
        windowRight   = cols;
        complexScroll = false;
    }
    public void setWindow(int top, int bottom) {
        setWindow(top, 0, bottom, cols);
    }
    public void setWindow(int top, int left, int bottom, int right) {
        windowTop    = top;
        windowLeft   = left;
        windowBottom = bottom;
        windowRight  = right;
        if(DEBUG)
            System.out.println("setWindow: " + top + ", " + bottom + ", " + left + ", " + right);

        // Ensure that the selected area is totally outside the scrolling
        // region OR that the scrolling region starts at the top of the
        // screen and the selection is completely above the scrolling
        // regions bottom. This makes things alot easier and is not that much
        // of problem.
        //
        if(hasSelection) {
            int selRowAnch = selectTopRow - visTop;
            int selRowLast = selectBottomRow - visTop;
            if(top != 0 && (selRowAnch >= 0 || selRowLast >= 0)) {
                if(!(selRowAnch < top && selRowLast < top ||
                        selRowAnch >= bottom && selRowLast >= bottom)) {
                    resetSelection();
                }
            } else {
                if(!(selRowAnch < bottom && selRowLast < bottom)) {
                    resetSelection();
                }
            }
        }

        if(windowLeft != 0 || windowRight != cols)
            complexScroll = true;
        else
            complexScroll = false;

    }
    public int  getWindowTop() {
        return windowTop;
    }
    public int  getWindowBottom() {
        return windowBottom;
    }
    public int  getWindowLeft() {
        return windowLeft;
    }
    public int  getWindowRight() {
        return windowRight;
    }

    public int getCursorV() {
        return curRow;
    }
    public int getCursorH() {
        return curCol;
    }

    public void cursorSetPos(int v, int h, boolean relative) {
        if(DEBUG)
            System.out.println("cursorSetPos: " + v + ", " + h + "(" + relative + ")");
        int maxV = rows - 1;
        int maxH = cols - 1;
        int minV = 0;
        int minH = 0;
        if(relative) {
            v += windowTop;
            maxV = windowBottom - 1;
            minV = windowTop;
            h += windowLeft;
            maxH = windowRight - 1;
            minH = windowLeft;
        }
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
        setCursorPosition(curRow, curCol);
    }
    public void cursorUp(int n) {
        if(DEBUG)
            System.out.println("cursorUp: " + n);
        int min = (curRow < windowTop ? 0 : windowTop);
        curRow -= n;
        if(curRow < min)
            curRow = min;
        setCursorPosition(curRow, curCol);
    }
    public void cursorDown(int n) {
        if(DEBUG)
            System.out.println("cursorDown: " + n);
        int max = (curRow > windowBottom - 1 ? rows - 1: windowBottom - 1);
        curRow += n;
        if(curRow > max)
            curRow = max;
        setCursorPosition(curRow, curCol);
    }
    public void cursorForward(int n) {
        if(DEBUG)
            System.out.println("cursorFwd: " + n);
        curCol += n;
        if(curCol > windowRight)
            curCol = windowRight;
        setCursorPosition(curRow, curCol);
    }
    public void cursorBackward(int n) {
        if(DEBUG)
            System.out.println("cursorBack: " + n);
        do {
            curCol -= 1;
            if(curCol < windowLeft) {
                if(autoReverseWrap) {
                    curCol = windowRight - (windowLeft - curCol);
                    cursorUp(1);
                } else {
                    curCol = windowLeft;
                }
            }
        } while (--n > 0);
        setCursorPosition(curRow, curCol);
    }
    public void cursorIndex(int n) {
        if(DEBUG)
            System.out.println("cursorIndex: " + n);
        if(curRow > windowBottom || curRow + n < windowBottom)
            cursorDown(n);
        else {
            int m = windowBottom - curRow;
            cursorDown(m);
            scrollUp((n - m) + 1);
        }
    }
    public void cursorIndexRev(int n) {
        if(DEBUG)
            System.out.println("cursorIndexRev: " + n);
        if(curRow < windowTop || curRow - n >= windowTop)
            cursorUp(n);
        else {
            int m = curRow - windowTop;
            scrollDown(n - m);
            cursorUp(m);
        }
    }

    public void cursorSave() {
        curRowSave  = curRow;
        curColSave  = curCol;
        curAttrSave = curAttr;
    }
    public void cursorRestore() {
        curRow  = curRowSave;
        curCol  = curColSave;
        curAttr = curAttrSave;
        setCursorPosition(curRow, curCol);
    }

    public synchronized void scrollUp(int n) {
        int windowHeight = windowBottom - windowTop;
        int i, j = windowTop;
        boolean visTopHasChanged = false;

        if(DEBUG)
            System.out.println("scrollUp: " + n);

        if(complexScroll) {
            // !!! TODO: This is untested...
            if(n < windowHeight) {
                j = (windowHeight - n) + windowTop;
                for(i = windowTop; i < j; i++) {
                    System.arraycopy(screen[visTop + i + n], windowLeft,
                                     screen[visTop + i],
                                     windowLeft, windowRight - windowLeft);
                    System.arraycopy(attributes[visTop + i + n], windowLeft,
                                     attributes[visTop + i], windowLeft,
                                     (windowRight - windowLeft));
                }
            }
            for(i = j; i < windowBottom; i++) {
                System.arraycopy(spacerow, 0, screen[visTop + i],
                                 windowLeft, windowRight - windowLeft);
                System.arraycopy(zerorow, 0, attributes[visTop + i],
                                 windowLeft, (windowRight - windowLeft));
            }
        } else {
            if(windowTop == 0 && windowBottom == rows && saveLines > 0) {
                // We must save outscrolled lines
                int sl; // Lines to scroll (a full screen max)
                sl = (n < windowHeight ? n : windowHeight);
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
                    int ll = windowHeight - sl;
                    System.arraycopy(screen, sl, screen, 0, saveLines + ll);
                    System.arraycopy(attributes, sl,
                                     attributes, 0, saveLines + ll);
                    System.arraycopy(autowraps, sl,
                                     autowraps, 0, saveLines + ll);
                    for (i = windowHeight - sl; i < windowHeight; i++) {
                        screen[saveLines + i]     = makeCharLine();
                        attributes[saveLines + i] = makeAttribLine();
                        autowraps[saveLines + i] = false;
                    }
                } else {
                    visTop    += sl;
                    visTopHasChanged = true;
                }
            } else {
                if(n < windowHeight) {
                    int from = visTop + windowTop + n;
                    int to = visTop + windowTop;
                    int len = windowHeight - n;
                    j = (windowHeight - n) + windowTop;
                    System.arraycopy(screen, from, screen, to, len);
                    System.arraycopy(attributes, from, attributes, to, len);
                    System.arraycopy(autowraps, from, autowraps, to, len);
                }
                for(i = j; i < windowBottom; i++) {
                    screen[visTop + i]     = makeCharLine();
                    attributes[visTop + i] = makeAttribLine();
                    autowraps[visTop + i]  = false;
                }
            }
        }

        if (visTopHasChanged) {
            if (display != null) {
                display.setVisTop(visTop);
            }
        }

        setCursorPosition(curRow, curCol);
        updateDirtyArea(windowTop, windowLeft, windowBottom, windowRight);
    }

    public synchronized void scrollDown(int n) {
        int windowHeight = windowBottom - windowTop;
        int i, j = windowBottom;

        if(DEBUG)
            System.out.println("scrollDown: " + n);

        if(complexScroll) {
            // !!! TODO: This is untested...
            if(n < windowHeight) {
                j = windowTop + n;
                for(i = windowBottom - 1; i >= j; i--) {
                    System.arraycopy(screen[visTop + i - n], windowLeft,
                                     screen[visTop + i], windowLeft,
                                     windowRight - windowLeft);
                    System.arraycopy(attributes[visTop + i - n], windowLeft,
                                     attributes[visTop + i], windowLeft,
                                     (windowRight - windowLeft));
                }
            }
            for(i = windowTop; i < j; i++) {
                System.arraycopy(spacerow, 0, screen[visTop + i], windowLeft,
                                 windowRight - windowLeft);
                System.arraycopy(zerorow, 0, attributes[visTop + i],
                                 windowLeft, (windowRight - windowLeft));
            }
        } else {
            if(n < windowHeight) {
                j = windowTop + n;
                int from = visTop + windowTop;
                int to = visTop + windowTop + n;
                int len = windowHeight - n;
                System.arraycopy(screen, from, screen, to, len);
                System.arraycopy(attributes, from, attributes, to, len);
                System.arraycopy(autowraps, from, autowraps, to, len);
            }
            for(i = windowTop; i < j; i++) {
                screen[visTop + i]     = makeCharLine();
                attributes[visTop + i] = makeAttribLine();
                autowraps[visTop + i]  = false;
            }
        }
        setCursorPosition(curRow, curCol);
        updateDirtyArea(windowTop, 0, windowBottom, cols);
    }

    private static int ATTR_MASK = DisplayModel.MASK_BGCOL |
                                   DisplayModel.MASK_FGCOL |
                                   DisplayModel.ATTR_BGCOLOR |
                                   DisplayModel.ATTR_FGCOLOR;

    public synchronized void clearBelow() {
        if(DEBUG)
            System.out.println("clearBelow");
        clearRight();
        int[] attrLine = makeAttribLine();
        int i;
        for(i = 0; i < cols; i++)
            attrLine[i] = curAttr & ATTR_MASK;
        for(i = curRow + 1; i < windowBottom; i++) {
            screen[visTop + i]     = makeCharLine();
            attributes[visTop + i] = makeAttribLine();
            autowraps[visTop + i]  = false;
            System.arraycopy(attrLine, 0, attributes[visTop + i], 0, cols);
        }
        if (isIntersectingSelect(curRow, 0, windowBottom, cols)) {
            resetSelection();
        }
        updateDirtyArea(curRow, 0, windowBottom, cols);
    }

    public synchronized void clearAbove() {
        if(DEBUG)
            System.out.println("clearAbove");
        clearLeft();
        int[] attrLine = makeAttribLine();
        int i;
        for(i = 0; i < cols; i++)
            attrLine[i] = curAttr & ATTR_MASK;
        for(i = windowTop; i < curRow; i++) {
            screen[visTop + i]     = makeCharLine();
            attributes[visTop + i] = makeAttribLine();
            autowraps[visTop + i]  = false;
            System.arraycopy(attrLine, 0, attributes[visTop + i], 0, cols);
        }
        if (isIntersectingSelect(windowTop, 0, curRow, cols)) {
            resetSelection();
        }
        updateDirtyArea(windowTop, 0, curRow, cols);
    }

    public synchronized void clearScreen() {
        if(DEBUG)
            System.out.println("clearScreen");
        int i;
        int[] attrLine = makeAttribLine();

        for(i = 0; i < cols; i++)
            attrLine[i] = curAttr & ATTR_MASK;
        for(i = windowTop; i < windowBottom; i++) {
            screen[visTop + i]     = makeCharLine();
            attributes[visTop + i] = makeAttribLine();
            autowraps[visTop + i]  = false;
            System.arraycopy(attrLine, 0, attributes[visTop + i], 0, cols);
        }
        if (isIntersectingSelect(0, 0, rows, cols)) {
            resetSelection();
        }
        updateDirtyArea(0, 0, rows, cols);
    }

    public synchronized void clearRight() {
        if(DEBUG)
            System.out.println("clearRight(" + curCol + "," + curRow + ")");
        System.arraycopy(spacerow, 0, screen[visTop + curRow], curCol,
                         cols - curCol);
        for(int i = curCol; i < cols; i++)
            attributes[visTop + curRow][i] = curAttr & ATTR_MASK;
        if (isIntersectingSelect(curRow, curCol, curRow + 1, cols)) {
            resetSelection();
        }
        updateDirtyArea(curRow, curCol, curRow + 1, cols);
    }

    public synchronized void clearLeft() {
        if(DEBUG)
            System.out.println("clearLeft(" + curCol + "," + curRow + ")");
        System.arraycopy(spacerow, 0, screen[visTop + curRow], 0, curCol);
        for(int i = 0; i <= curCol; i++)
            attributes[visTop + curRow][i] = curAttr & ATTR_MASK;
        if (isIntersectingSelect(curRow, 0, curRow + 1, curCol)) {
            resetSelection();
        }
        updateDirtyArea(curRow, 0, curRow + 1, curCol);
    }
    public synchronized void clearLine() {
        if(DEBUG)
            System.out.println("clearLine");
        screen[visTop + curRow]     = makeCharLine();
        attributes[visTop + curRow] = makeAttribLine();
        autowraps[visTop + curRow]  = false;
        for(int i = 0; i < cols; i++)
            attributes[visTop + curRow][i] = curAttr & ATTR_MASK;
        if (isIntersectingSelect(curRow, 0, curRow + 1, cols)) {
            resetSelection();
        }
        updateDirtyArea(curRow, 0, curRow + 1, cols);
    }

    public synchronized void eraseChars(int n) {
        if(DEBUG)
            System.out.println("eraseChars");
        if(n > cols - curCol)
            n = cols - curCol;
        System.arraycopy(spacerow, 0, screen[visTop + curRow], curCol, n);
        for(int i = 0; i < n; i++)
            attributes[visTop + curRow][curCol + i] = curAttr & ATTR_MASK;
        if (isIntersectingSelect(curRow, curCol, curRow, curCol + n)) {
            resetSelection();
        }
        updateDirtyArea(curRow, curCol, curRow, curCol + n);
    }

    public synchronized void insertChars(int n) {
        int edge = windowRight;
        if(DEBUG)
            System.out.println("insertChars: " + n);
        if(curCol < windowLeft || curCol > windowRight)
            return;
        if((curCol + n) < windowRight) {
            edge = curCol +  n;
            System.arraycopy(screen[visTop + curRow], curCol,
                             screen[visTop + curRow], edge, windowRight -edge);
            System.arraycopy(attributes[visTop + curRow], curCol,
                             attributes[visTop + curRow], edge,
                             (windowRight - edge));
        }
        System.arraycopy(spacerow, 0, screen[visTop + curRow], curCol,
                         edge - curCol);
        for(int i = curCol; i < edge; i++)
            attributes[visTop + curRow][i] = curAttr & ATTR_MASK;
        if (isIntersectingSelect(curRow, curCol, curRow + 1, windowRight)) {
            resetSelection();
        }
        updateDirtyArea(curRow, curCol, curRow + 1, windowRight);
    }

    public synchronized void deleteChars(int n) {
        int edge = curCol;
        if(DEBUG)
            System.out.println("deleteChars: " + n);
        if(curCol < windowLeft || curCol > windowRight)
            return;
        if((curCol + n) < windowRight) {
            edge = windowRight - n;
            System.arraycopy(screen[visTop + curRow], curCol + n,
                             screen[visTop + curRow], curCol, edge - curCol);
            System.arraycopy(attributes[visTop + curRow], (curCol + n),
                             attributes[visTop + curRow],
                             curCol, (edge - curCol));
        }
        System.arraycopy(spacerow, 0, screen[visTop + curRow],
                         edge, windowRight - edge);
        for(int i = edge; i < windowRight; i++)
            attributes[visTop + curRow][i] = curAttr & ATTR_MASK;
        if (isIntersectingSelect(curRow, curCol, curRow + 1, windowRight)) {
            resetSelection();
        }
        updateDirtyArea(curRow, curCol, curRow + 1, windowRight);
    }

    public synchronized void insertLines(int n) {
        int i, edge = windowBottom;
        if(DEBUG)
            System.out.println("insertLines: " + n);

        if(curRow < windowTop || curRow > windowBottom)
            return;

        if(complexScroll) {
            // !!! TODO: This is untested...
            if(curRow + n < windowBottom) {
                edge = curRow  + n;
                for(i = windowBottom - 1; i >= edge; i--) {
                    System.arraycopy(screen[visTop + i - n], windowLeft,
                                     screen[visTop + i], windowLeft,
                                     windowRight - windowLeft);
                    System.arraycopy(attributes[visTop + i - n], windowLeft,
                                     attributes[visTop + i],
                                     windowLeft, (windowRight - windowLeft));
                }
            }
            for(i = curRow; i < edge; i++) {
                System.arraycopy(spacerow, 0, screen[visTop + i],
                                 windowLeft, windowRight - windowLeft);
                System.arraycopy(zerorow, 0, attributes[visTop + i],
                                 windowLeft, (windowRight - windowLeft));
            }
        } else {
            if(curRow + n < windowBottom) {
                edge = curRow + n;
                System.arraycopy(screen, visTop + curRow, screen,
                                 visTop + edge, windowBottom - edge);
                System.arraycopy(attributes, visTop + curRow, attributes,
                                 visTop + edge, windowBottom - edge);
                System.arraycopy(autowraps, visTop + curRow, autowraps,
                                 visTop + edge, windowBottom - edge);
            }
            int[] attrLine = makeAttribLine();
            for(i = 0; i < cols; i++)
                attrLine[i] = (curAttr & ATTR_MASK);
            for(i = curRow; i < edge; i++) {
                screen[visTop + i]     = makeCharLine();
                attributes[visTop + i] = makeAttribLine();
                autowraps[visTop + i]  = false;
                System.arraycopy(attrLine, 0, attributes[visTop + i], 0, cols);
            }
        }

        if (isIntersectingSelect(curRow, 0, windowBottom, cols)) {
            resetSelection();
        }
        updateDirtyArea(curRow, 0, windowBottom, cols);
    }

    public synchronized void deleteLines(int n) {
        int i, edge = curRow;
        if(DEBUG)
            System.out.println("deleteLines: " + n);

        if(curRow < windowTop || curRow > windowBottom)
            return;

        if(complexScroll) {
            // !!! TODO: This is untested...
            if(curRow + n < windowBottom) {
                edge = windowBottom - n - 1;
                for(i = curRow; i <= edge; i++) {
                    System.arraycopy(screen[visTop + i + n], windowLeft,
                                     screen[visTop + i], windowLeft,
                                     windowRight - windowLeft);
                    System.arraycopy(attributes[visTop + i + n], windowLeft,
                                     attributes[visTop + i],
                                     windowLeft, (windowRight - windowLeft));
                }
            }
            for(i = edge; i < windowBottom; i++) {
                System.arraycopy(spacerow, 0, screen[visTop + i], windowLeft,
                                 windowRight - windowLeft);
                System.arraycopy(zerorow, 0, attributes[visTop + i],
                                 windowLeft, (windowRight - windowLeft));
            }
        } else {
            if(curRow + n < windowBottom) {
                edge = windowBottom - n;
                System.arraycopy(screen, visTop + curRow + n, screen,
                                 visTop + curRow, edge - curRow);
                System.arraycopy(attributes, visTop + curRow + n, attributes,
                                 visTop + curRow, edge - curRow);
                System.arraycopy(autowraps, visTop + curRow + n, autowraps,
                                 visTop + curRow, edge - curRow);
            }
            int[] attrLine = makeAttribLine();
            for(i = 0; i < cols; i++)
                attrLine[i] = (curAttr & ATTR_MASK);
            for(i = edge; i < windowBottom; i++) {
                screen[visTop + i]     = makeCharLine();
                attributes[visTop + i] = makeAttribLine();
                autowraps[visTop + i]  = false;
                System.arraycopy(attrLine, 0, attributes[visTop + i], 0, cols);
            }
        }

        if (isIntersectingSelect(curRow, 0, windowBottom, cols)) {
            resetSelection();
        }
        updateDirtyArea(curRow, 0, windowBottom, cols);
    }

    public void setAttribute(int attr, boolean val) {
        if(DEBUG)
            System.out.println("setAttr " + attr + "=" + val);
        if(val)
            curAttr |= attr;
        else
            curAttr &= ~attr;
    }
    public boolean getAttribute(int attr) {
        if(DEBUG)
            System.out.println("getAttr " + attr);
        if((curAttr & attr) == attr)
            return true;
        return false;
    }
    public void setForegroundColor(int c) {
        if(DEBUG)
            System.out.println("setForegroundColor: " + c);
        if(c >= 0 && c < 8) {
            if((curAttr & DisplayModel.ATTR_BOLD) != 0)
                c += 8;
            curAttr &= ~(DisplayModel.ATTR_FGCOLOR | DisplayModel.MASK_FGCOL);
            curAttr |= (DisplayModel.ATTR_FGCOLOR | (c << DisplayModel.SHIFT_FGCOL));
        } else {
            curAttr &= ~DisplayModel.ATTR_FGCOLOR;
        }
    }

    public void setBackgroundColor(int c) {
        if(DEBUG)
            System.out.println("setBackgroundColor: " + c);
        if(c >= 0 && c < 8) {
            curAttr &= ~(DisplayModel.ATTR_BGCOLOR | DisplayModel.MASK_BGCOL);
            curAttr |= (DisplayModel.ATTR_BGCOLOR | (c << DisplayModel.SHIFT_BGCOL));
        } else {
            curAttr &= ~DisplayModel.ATTR_BGCOLOR;
        }
    }

    public void clearAllAttributes() {
        if(DEBUG)
            System.out.println("clearAllAttributes");
        curAttr = DisplayModel.ATTR_CHARDRAWN;
    }

    public synchronized void resize(int newRows, int newCols) {
        int oldCols = cols;
        int oldRows = rows;
        char[][]  oldScreen     = screen;
        int[][]   oldAttributes = attributes;
        boolean[] oldAutowraps  = autowraps;
        boolean visTopHasChanged = false;

        if(newCols != oldCols) {
            resetSelection();
        }

        // We only want to reallocate and do all this work if the component
        // REALLY has changed size, since we seem to get lot's of call-backs
        // here we better check this...
        //
        if(newRows != rows || newCols != cols) {
            resizeBuffers(newRows, newCols);
            resetWindow(); // !!! Is this right?
            clearScreen();

            oldCols = (oldCols < newCols ? oldCols : newCols);

            /*
             * Resizing is tricky. This comment explains how it should
             * work.
             *
             *  +---+  \
             *  |1  |   \ saved lines
             *  |2  |   /
             *  |3  |  /
             * ++4--++ \
             * ||5  ||  \ visible area
             * ||6  ||  /
             * ++---++ /
             *
             * How to behave when the cursor is at the bottom
             *                Grow           Shrink
             *  +---+                        +---+
             *  |1  |         +---+          |2  |
             *  |2  |         |1  |          |3  |
             *  |3  |         |2  |          |4  |
             * ++4--++       ++3--++        ++5--++
             * ||5  ||       ||4  ||        ||6  ||
             * ||6  ||       ||5  ||        ++---++
             * ++---++       ||6  ||
             *               ++---++
             *
             * How to behave when the cursor is not at the bottom
             *                Grow           Shrink
             *  +---+                        +---+
             *  |1  |         +---+          |1  |
             *  |2  |         |1  |          |2  |
             *  |3  |         |2  |          |3  |
             * ++4--++       ++3--++        ++4--++
             * ||5  ||       ||4  ||        ||5  ||
             * ||   ||       ||5  ||        ++---++
             * ++---++       ||   ||
             *               ++---++
             *
             * Or to put it in words. the distance between the cursor
             * and the bottom of the screen is preserved when the
             * window is elarged and is consumed when the window is
             * shrunk. 
             */
            int startAt = 0;
            int copyRows = visTop + curRow + 1;
            if (copyRows > saveLines+newRows) {
                startAt = copyRows - (saveLines+newRows);
                copyRows -= startAt;
            }
            for(int i = 0; i < copyRows; i++) {
                System.arraycopy(oldScreen[i+startAt], 0,
                                 screen[i], 0, oldCols);
                System.arraycopy(oldAttributes[i+startAt], 0,
                                 attributes[i], 0, oldCols);
                autowraps[i] = oldAutowraps[i+startAt];
            }
            int emptyLines = oldRows-1-curRow;
            int visTopDelta = oldRows - (newRows + startAt);
            if (oldRows > newRows && emptyLines >= -1*visTopDelta) {
                visTopDelta -= emptyLines;
            }
            if (visTop + visTopDelta < 0) {
                visTopDelta = -1*visTop;
            }
            if (visTopDelta != 0) {
                visTop += visTopDelta;
                visTopHasChanged = true;
                curRow -= visTopDelta;
            }
                

            if(curRow >= newRows)
                curRow = newRows - 1;
            if(curCol >= newCols)
                curCol = newCols - 1;
        }
        if (visTopHasChanged && display != null) {
            display.setPendingVisTopChange(visTop);
        }
        setCursorPosition(curRow, curCol);
    }

    // !!! Ouch !!!
    //
    protected synchronized int nextPrintedChar(int row, int col) {
        int i;
        for(i = col; i < cols; i++)
            if(screen[row][i] != defaultChar)
                break;
        return i;
    }

    protected synchronized int prevPrintedChar(int row, int col) {
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
        if ((attributes[selectTopRow][selectTopCol]
             & DisplayModel.ATTR_DWIDTH_R) != 0) {
            selectTopCol--;
        }
        if ((attributes[selectBottomRow][selectBottomCol]
             & DisplayModel.ATTR_DWIDTH_L) != 0) {
            selectBottomCol++;
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

    protected synchronized String getContents(
        int startRow, int startCol, int endRow, int endCol, String eol) {
        int i, j, n;

        StringBuilder result = new StringBuilder();

        if (startRow != endRow) {
            for(i = startCol; i < cols; i++) {
                if ((attributes[startRow][i]&DisplayModel.ATTR_DWIDTH_R) != 0){
                    continue;
                } else if (screen[startRow][i] == 0) {
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
                    if ((attributes[i][j] & DisplayModel.ATTR_DWIDTH_R) != 0) {
                        continue;
                    } else if (screen[i][j] == 0) {
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
                if ((attributes[endRow][i]&DisplayModel.ATTR_DWIDTH_R) != 0) {
                    continue;
                } else if (screen[endRow][i] == 0) {
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
                if ((attributes[startRow][i]&DisplayModel.ATTR_DWIDTH_R) != 0){
                    continue;
                } else if (screen[startRow][i] == 0) {
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

    public synchronized void doClickSelect(int row, int col,
                                           String selectDelims) {
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

    public synchronized  SearchContext search(
        SearchContext lastContext, String key,
        boolean reverse, boolean caseSens) {
        int     len = key.length();
        int     lastRow = visTop + rows;
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
                        if(screen[i][j] == 0) {
                            continue;
                        }
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
                        if(screen[i][j] == 0) {
                            continue;
                        }
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
            if(chars[idx] != firstChar)
                return false;
            cmpStr = new String(chars, idx, len);
            if(cmpStr.equals(findStr))
                return true;
        } else {
            if(Character.toLowerCase(chars[idx]) != firstChar)
                return false;
            cmpStr = new String(chars, idx, len);
            if(cmpStr.equalsIgnoreCase(findStr))
                return true;
        }
        return false;
    }
}
