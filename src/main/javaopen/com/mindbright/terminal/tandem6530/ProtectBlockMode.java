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

import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;

import com.mindbright.terminal.DisplayView;

public class ProtectBlockMode extends ModeBase {
    protected static final int ROWS = 24;
    protected static final int COLS = 80;

    protected Terminal6530Callback termCallback;
    protected ProtectedScreen pages[];
    protected int displayPage;
    protected int selectedPage;
    protected DataType dataTypeTable;
    protected VariableFieldAttributeTable varTable;
    protected FixedFieldAttributeTable fixedTable;
    protected DisplayView display;
    protected boolean insertMode = false;
    protected int MAX_PAGES;

    protected int     selectTopRow;
    protected int     selectTopCol;
    protected int     selectBottomRow;
    protected int     selectBottomCol;
    protected boolean hasSelection;
    protected int     selectClickRow = -1;
    protected boolean selectClickState;

    public ProtectBlockMode(Terminal6530Callback termCallback, int maxPages) {
        this.termCallback = termCallback;
        MAX_PAGES = maxPages;

        dataTypeTable = new DataType();
        varTable = new VariableFieldAttributeTable();
        fixedTable = new FixedFieldAttributeTable();

        pages = new ProtectedScreen[MAX_PAGES];
        for (int i = 0; i < MAX_PAGES; i++) {
            pages[i] = new ProtectedScreen(this, ROWS, COLS, dataTypeTable);
        }
        displayPage = 0;
        selectedPage = 0;
    }

    public void updateCursorPosition(ProtectedScreen screen, Position cursor) {
        if (display != null && screen == pages[displayPage]) {
            display.setCursorPosition(cursor.getRow(), cursor.getCol());
        }
    }

    public void updateDirty() {
        if (display != null) {
            display.updateDirtyArea(0, 0, ROWS - 1, COLS - 1);
        }
    }
    public void updateDirty(ProtectedScreen screen, Position start,
                            Position end) {
        if (display != null && screen == pages[displayPage]) {
            display.updateDirtyArea(start.getRow(), start.getCol(),
                                    end.getRow()+1, end.getCol()+1);
        }
    }

    public void keyHandler(char c, int virtualKey, int modifiers) {
        boolean keyProcessed = false;

        switch (virtualKey) {
        case KeyEvent.VK_BACK_SPACE:
            if (modifiers == 0) {
                pages[displayPage].doBackspace();
                if (insertMode) {
                    pages[displayPage].doDeleteCharacter(true);
                }
                keyProcessed = true;
            }
            break;

        case KeyEvent.VK_TAB:
            if ((modifiers & InputEvent.SHIFT_MASK) ==
                    InputEvent.SHIFT_MASK) {
                pages[displayPage].doBackTab();
                keyProcessed = true;
            } else {
                pages[displayPage].doHTab();
                keyProcessed = true;
            }
            break;

        case KeyEvent.VK_ENTER:
            if ((modifiers & InputEvent.SHIFT_MASK) ==
                    InputEvent.SHIFT_MASK) {
                pages[displayPage].doHTab();
                keyProcessed = true;
            } else if ((modifiers & InputEvent.CTRL_MASK) ==
                       InputEvent.CTRL_MASK) {
                pages[displayPage].cursorToLastCharInField();
                keyProcessed = true;
            } else if (modifiers == 0) {
                pages[displayPage].cursorToNextUnprotected(1);
                keyProcessed = true;
            }
            break;

        case KeyEvent.VK_HOME:
            if ((modifiers & InputEvent.CTRL_MASK) == InputEvent.CTRL_MASK) {
                pages[displayPage].doCursorHomeDown();
                pages[displayPage].cursorToLastCharInField();
                keyProcessed = true;
            } else if (modifiers == 0) {
                pages[displayPage].doCursorHome();
                keyProcessed = true;
            }
            break;

        case KeyEvent.VK_END:
            if (modifiers == 0) {
                pages[displayPage].doCursorHomeDown();
                keyProcessed = true;
            }
            break;

        case KeyEvent.VK_INSERT:
            if ((modifiers & (InputEvent.ALT_MASK)) == InputEvent.ALT_MASK) {
                insertMode = true;
                keyProcessed = true;
            } else if (modifiers == 0) {
                if (insertMode) {
                    insertMode = false;
                } else {
                    pages[displayPage].doInsertCharacter(true);
                }
                keyProcessed = true;
            }
            break;

        case KeyEvent.VK_DELETE:
            if (modifiers == 0) {
                pages[displayPage].doDeleteCharacter(true);
                keyProcessed = true;
            }
            break;

        case KeyEvent.VK_RIGHT:
            if (modifiers == 0) {

                pages[displayPage].doCursorRight();
                keyProcessed = true;
            }
            break;

        case KeyEvent.VK_LEFT:
            if (modifiers == 0) {
                doBackspace();
                keyProcessed = true;
            }
            break;


        case KeyEvent.VK_UP:
            if (modifiers == 0) {
                pages[displayPage].doCursorUp();
                keyProcessed = true;
            }
            break;

        case KeyEvent.VK_DOWN:
            if (modifiers == 0) {
                pages[displayPage].doCursorDown();
                keyProcessed = true;
            }
            break;


        case KeyEvent.VK_2:
            if ((modifiers &
                    (InputEvent.ALT_MASK | InputEvent.SHIFT_MASK)) ==
                    (InputEvent.ALT_MASK | InputEvent.SHIFT_MASK)) {
                pages[displayPage].doEraseToEndOfPageOrMemory(true);
                keyProcessed = true;
            } else if ((modifiers & InputEvent.ALT_MASK) ==
                       InputEvent.ALT_MASK) {
                pages[displayPage].doEraseToEndOfLineOrField(true);
                keyProcessed = true;
            }
            break;

        default:
            break;
        }

        if (keyProcessed) {
            repaint();
            return;
        }

        if (c == KeyEvent.CHAR_UNDEFINED) {
            return;
        }

        if (!pages[displayPage].cursorWrite(c)) {
            termCallback.error("INVALID DATA");
            return;
        }
        repaint();
    }

    public void hostChar(char c) {
        pages[selectedPage].bufferWrite(c);
        repaint();
    }

    public int getBufferRows() {
        return ROWS;
    }

    public void setDisplay(DisplayView display) {
        this.display = display;
        if (display != null) {
            display.setVisTop(0);
            display.updateScrollbarValues();
        }
        repaint();
    }

    public void switchReset() {
        for (int i = 0; i < MAX_PAGES; i++) {
            pages[i].reset();
        }
        insertMode = false;
        dataTypeTable.reset();
        doSelectPage(1);
        doDisplayPage(1);
        repaint();
    }

    /* 2-8, 3-18 */
    public void doBackspace() {
        pages[selectedPage].doBackspace();
        repaint();
    }
    /* 2-8, 3-18 */
    public void doHTab() {
        pages[selectedPage].doHTab();
        repaint();
    }
    /* 2-8 */
    public void doLineFeed() {
        pages[selectedPage].doLineFeed();
        repaint();
    }
    /* 2-8, 3-18 */
    public void doCarriageReturn() {
        pages[selectedPage].doCarriageReturn();
        repaint();
    }
    /* 3-15, 3-16 */
    public void doSetBufferAddress(int row, int column) {
        pages[selectedPage].setBufferAddress(row - 1, column - 1);
    }
    public void doSetCursorAddress(boolean displayedPage, int row, int column) {
        if (row > ROWS || column > COLS) {
            return;
        }
        int page = displayedPage ? displayPage : selectedPage;
        pages[page].setCursorAddress(row - 1, column - 1);
        repaint();
    }
    /* 3-84 */
    public void doDefineFieldAttribute(int row, int column, boolean useFixed,
                                       int tableRow) {
        FieldAttributes attrib;
        if (useFixed) {
            attrib = fixedTable.get(tableRow);
        } else {
            attrib = varTable.get(tableRow);
        }

        if (attrib == null) {
            termCallback.error("Table index " + tableRow + " was bad");
            return;
        }

        pages[selectedPage].setBufferAddress(row - 1, column - 1);
        pages[selectedPage].addField(attrib);
        pages[selectedPage].updateCursorPosition();
        repaint();
    }

    /* 3-37 */
    public void doStartField(FieldAttributes attribs) {
        pages[selectedPage].addField(attribs);
        pages[selectedPage].updateCursorPosition();
        repaint();
    }

    /* 3-38 */
    public void doStartFieldExtended(FieldAttributes attribs) {
        pages[selectedPage].addField(attribs);
        pages[selectedPage].updateCursorPosition();
        repaint();
    }

    /* 3-19 */
    public void doBackTab() {
        pages[selectedPage].doBackTab();
        repaint();
    }
    /* 3-12 */
    public void doSetMaxPageNumber(int n) {
        // We just support MAX_PAGES
    }
    /* 3-39, 3-40 */
    public void doDefineDataTypeTable(int startIndex, byte entries[]) {
        dataTypeTable.set(startIndex, entries);
    }
    /* 3-90 */
    public void doResetVariableTable() {
        varTable.reset();
    }
    /* 3-90 */
    public void doDefineVariableTable(int startIndex,
                                      FieldAttributes attribs[]) {
        varTable.set(startIndex, attribs);
    }
    /* 2-7, 3-17 */
    public void doCursorUp() {
        pages[selectedPage].doCursorUp();
        repaint();
    }
    /* 2-8, 3-17 */
    public void doCursorRight() {
        pages[selectedPage].doCursorRight();
        repaint();
    }
    /* 2-8, 3-18 */
    public void doCursorHomeDown() {
        pages[selectedPage].doCursorHomeDown();
        repaint();
    }
    /* 2-8, 3-18 */
    public void doCursorHome() {
        pages[selectedPage].doCursorHome();
        repaint();
    }

    /* 2-14, 3-23 */
    public void doSetVideoAttribute(int attrib) {
        pages[selectedPage].doSetVideoAttribute(attrib);
    }

    /* 3-48, 3-49 */
    public synchronized void doClearMemoryToSpaces(int startRow, int startCol,
                                                   int endRow, int endColumn) {
        pages[selectedPage].doClearMemoryToSpaces(startRow - 1, startCol - 1,
                                                  endRow - 1, endColumn);
        repaint();
    }
    /* 2-10, 3-49 */
    public synchronized void doEraseToEndOfPageOrMemory() {
        pages[selectedPage].doEraseToEndOfPageOrMemory(false);
        repaint();
    }
    /* 2-11, 3-49 */
    public void doEraseToEndOfLineOrField() {
        pages[selectedPage].doEraseToEndOfLineOrField(false);
        repaint();
    }
    /* 3-45, 3-46 */
    public synchronized String doReadWithAddress(int startRow, int startCol,
                                                 int endRow, int endCol) {
        Position start = new Position(startRow - 1, startCol - 1);
        Position end = new Position(endRow - 1, endCol - 1);
        return pages[selectedPage].readWithAddress(start, end);
    }
    /* 3-46, 3-47 */
    public synchronized String doReadWithAddressAll(int startRow, int startCol,
                                                    int endRow, int endCol) {
        Position start = new Position(startRow - 1, startCol - 1);
        Position end = new Position(endRow - 1, endCol - 1);
        return pages[selectedPage].readWithAddressAll(start, end);
    }
    /* 3-50 */
    public synchronized void doInsertLine() {
        pages[selectedPage].doInsertLine();
        repaint();
    }
    /* 3-50 */
    public synchronized void doDeleteLine() {
        pages[selectedPage].doDeleteLine();
        repaint();
    }
    /* 3-51 */
    public void doInsertCharacter() {
        pages[selectedPage].doInsertCharacter(false);
        repaint();
    }
    /* 3-51 */
    public void doDeleteCharacter() {
        pages[selectedPage].doDeleteCharacter(false);
        repaint();
    }
    /* 3-38 */
    public void doResetModifiedDataTags() {
        pages[selectedPage].doResetModifiedDataTags();
    }
    /* 3-44 */
    public String doReadWholePageOrBuffer() {
        return pages[selectedPage].readWholePageOrBuffer();
    }
    /* 2-10, 3-11 */
    public void doDisplayPage(int n) {
        if (n >= 1 && n <= MAX_PAGES) {
            displayPage = n - 1;
            pages[displayPage].updateCursorPosition();
            updateDirty();
            repaint();
        }

    }
    /* 3-12 */
    public void doSelectPage(int n) {
        if (n >= 1 && n <= MAX_PAGES) {
            selectedPage = n - 1;
        }
    }

    public synchronized int getRow() {
        return pages[selectedPage].getCursorAddress().getRow() + 1;
    }
    public synchronized int getCol() {
        return pages[selectedPage].getCursorAddress().getCol() + 1;
    }
    public synchronized int getPage() {
        return selectedPage + 1;
    }

    protected void repaint() {
        if (display != null) {
            display.repaint();
        }
    }

    public synchronized char[] getChars(int visTop, int row) {
        return pages[displayPage].getChars(visTop + row);
    }
    public synchronized int[] getAttribs(int visTop, int row) {
        return pages[displayPage].getAttribs(visTop + row);
    }

    public void doClickSelect(int row, int col, String selectDelims) {
        int selectLeft, selectRight;
        if(selectClickRow == row && selectClickState) {
            selectLeft = 0;
            selectRight = COLS -1;
        } else {
            int i;
            char chars[] = pages[displayPage].getChars(row);
            for(i = col; i >= 0; i--) {
                if(selectDelims.indexOf(chars[i]) != -1) {
                    break;
                }
            }
            selectLeft = i + 1;

            for(i = col; i < COLS ; i++) {
                if(selectDelims.indexOf(chars[i]) != -1) {
                    break;
                }
            }
            selectRight = i - 1;

            selectLeft  = (selectLeft  > col) ? col : selectLeft;
            selectRight = (selectRight < col) ? col : selectRight;
        }
        selectClickState = !selectClickState;
        selectClickRow   = row;
        setSelection(row, selectLeft, row, selectRight);
    }

    public void resetClickSelect() {
        selectClickRow   = -1;
        selectClickState = false;
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

    public void selectAll() {
        setSelection(0, 0, ROWS - 1, COLS - 1);
    }

    public void resetSelection() {
        hasSelection = false;
        if (display != null) {
            display.resetSelection();
        }
    }

    public String getSelection(String eol) {
        if (!hasSelection) {
            return null;
        }
        if (eol == null) {
            eol = "\r";
        }
        String ret =
            getContents(selectTopRow, selectTopCol,
                        selectBottomRow, selectBottomCol, eol);
        return ret;

    }

    private void getLine(StringBuilder buf, int row, int startCol, int endCol,
			 String eol) {
        char chars[] = pages[displayPage].getChars(row);

        for (int i = startCol; i < endCol; i++) {
            buf.append(chars[i]);
        }
        buf.append(eol);
    }

    protected String getContents(int startRow, int startCol,
                                 int endRow, int endCol, String eol) {
        int i;

        StringBuilder result = new StringBuilder();

        if (startRow == endRow) {
            getLine(result, startRow, startCol, endCol + 1, eol);
        } else {
            getLine(result, startRow, startCol, COLS, eol);
            for(i = startRow + 1; i < endRow; i++) {
                getLine(result, i, 0, COLS, eol);
            }
            getLine(result, endRow, 0, endCol, eol);
        }

        return result.toString();
    }


}


