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
import com.mindbright.terminal.SearchContext;

public class BlockMode extends ModeBase {
    protected static final int ROWS = 24;
    protected static final int COLS = 80;

    protected Screen6530 pages[];
    protected int displayPage;
    protected int selectedPage;
    protected boolean insertMode = false;
    protected int MAX_PAGES;
    protected DisplayView display;


    public BlockMode(int maxPages) {
        MAX_PAGES = maxPages;

        pages = new Screen6530[MAX_PAGES];
        for (int i = 0; i < MAX_PAGES; i++) {
            pages[i] = new Screen6530(ROWS, COLS, ' ', true);
        }
        displayPage = 0;
        selectedPage = 0;
    }

    public void keyHandler(char c, int virtualKey, int modifiers) {
        boolean keyProcessed = false;

        switch (virtualKey) {
        case KeyEvent.VK_BACK_SPACE:
            if (modifiers == 0) {
                pages[displayPage].doBS();
                if (insertMode) {
                    pages[displayPage].deleteChars(1);
                }
                keyProcessed = true;
            }
            break;

        case KeyEvent.VK_TAB:
            if ((modifiers & InputEvent.SHIFT_MASK) ==
                    InputEvent.SHIFT_MASK) {
                pages[displayPage].doBackTabs(1);
                keyProcessed = true;
            } else {
                pages[displayPage].doHTab();
                keyProcessed = true;
            }
            break;

        case KeyEvent.VK_ENTER:
            if ((modifiers & InputEvent.SHIFT_MASK) == InputEvent.SHIFT_MASK) {
                pages[displayPage].doCR();
                keyProcessed = true;
            } else if ((modifiers & InputEvent.CTRL_MASK) ==
                       InputEvent.CTRL_MASK) {
                pages[displayPage].cursorToLastCharOnRow();
                keyProcessed = true;
            } else if (modifiers == 0) {
                pages[displayPage].cursorDown(1);
                pages[displayPage].doCR();
                keyProcessed = true;
            }
            break;

        case KeyEvent.VK_HOME:
            if ((modifiers & InputEvent.CTRL_MASK) == InputEvent.CTRL_MASK) {
                pages[displayPage].cursorHomeDown();
                pages[displayPage].cursorToLastCharOnRow();
                keyProcessed = true;
            } else if (modifiers == 0) {
                pages[displayPage].cursorHome();
                keyProcessed = true;
            }
            break;

        case KeyEvent.VK_END:
            if (modifiers == 0) {
                pages[displayPage].cursorHomeDown();
                keyProcessed = true;
            }
            break;

        case KeyEvent.VK_INSERT:
            if ((modifiers & (InputEvent.ALT_MASK)) == InputEvent.ALT_MASK) {
                insertMode = true;
                pages[displayPage].setInsertMode(true);
                keyProcessed = true;
            } else if ((modifiers & InputEvent.CTRL_MASK) ==
                       InputEvent.CTRL_MASK) {
                pages[displayPage].insertLines(1);
                keyProcessed = true;
            } else if (modifiers == 0) {
                if (insertMode) {
                    insertMode = false;
                    pages[displayPage].setInsertMode(false);
                } else {
                    pages[displayPage].insertChars(1);
                }
                keyProcessed = true;
            }
            break;

        case KeyEvent.VK_DELETE:
            if (modifiers == 0) {
                pages[displayPage].deleteChars(1);
                keyProcessed = true;
            } else if ((modifiers & InputEvent.CTRL_MASK) ==
                       InputEvent.CTRL_MASK) {
                pages[displayPage].deleteLines(1);
                keyProcessed = true;
            }
            break;

        case KeyEvent.VK_RIGHT:
            if (modifiers == 0) {
                pages[displayPage].cursorForward(1);
                keyProcessed = true;
            }
            break;

        case KeyEvent.VK_LEFT:
            if (modifiers == 0) {
                pages[displayPage].doBS();
                keyProcessed = true;
            }
            break;


        case KeyEvent.VK_UP:
            if (modifiers == 0) {
                pages[displayPage].cursorUp(1);
                keyProcessed = true;
            }
            break;

        case KeyEvent.VK_DOWN:
            if (modifiers == 0) {
                pages[displayPage].cursorDown(1);
                keyProcessed = true;
            }
            break;

        case KeyEvent.VK_1:
            if ((modifiers &
                    (InputEvent.ALT_MASK | InputEvent.SHIFT_MASK)) ==
                    (InputEvent.ALT_MASK | InputEvent.SHIFT_MASK)) {
                pages[displayPage].clearTab(true);
                keyProcessed = true;
            } else if ((modifiers & InputEvent.ALT_MASK) ==
                       InputEvent.ALT_MASK) {
                pages[displayPage].setTab(true);
                keyProcessed = true;
            }
            break;


        case KeyEvent.VK_2:
            if ((modifiers &
                    (InputEvent.ALT_MASK | InputEvent.SHIFT_MASK)) ==
                    (InputEvent.ALT_MASK | InputEvent.SHIFT_MASK)) {
                pages[displayPage].clearBelow();
                keyProcessed = true;
            } else if ((modifiers & InputEvent.ALT_MASK) ==
                       InputEvent.ALT_MASK) {
                pages[displayPage].clearRight();
                keyProcessed = true;
            }
            break;

        case KeyEvent.VK_3:
            if ((modifiers & InputEvent.ALT_MASK) == InputEvent.ALT_MASK) {
                pages[displayPage].clearAllTabs();
                keyProcessed = true;
            }
            break;


        default:
            break;
        }

        // This is kind of ugly but it catches Shift+1 :-(
        if (c == '!' && ((modifiers &
                          (InputEvent.ALT_MASK | InputEvent.SHIFT_MASK)) ==
                         (InputEvent.ALT_MASK | InputEvent.SHIFT_MASK))) {
            pages[displayPage].clearTab(true);
            keyProcessed = true;
        }

        // This is kind of ugly but it catches Shift+2 :-(
        if (c == '@' && ((modifiers &
                          (InputEvent.ALT_MASK | InputEvent.SHIFT_MASK)) ==
                         (InputEvent.ALT_MASK | InputEvent.SHIFT_MASK))) {
            pages[displayPage].clearBelow();
            keyProcessed = true;
        }

        if (keyProcessed) {
            repaint();
            return;
        }

        if (c == KeyEvent.CHAR_UNDEFINED) {
            return;
        }

        pages[displayPage].cursorWrite(c);
        repaint();
    }

    public void hostChar(char c) {
        pages[selectedPage].bufferWrite(c);
        repaint();
    }

    protected void updateDirty() {
        if (display != null) {
            display.updateDirtyArea(0, 0, ROWS - 1, COLS - 1);
        }
    }

    protected void repaint() {
        if (display != null) {
            display.repaint();
        }
    }

    public void setDisplay(DisplayView display) {
        this.display = display;
        pages[displayPage].setDisplay(display);

        if (display != null) {
            display.setVisTop(0);
            display.updateScrollbarValues();
            repaint();
        }
    }

    public void switchReset() {
        for (int i = 0; i < MAX_PAGES; i++) {
            pages[i].reset();
        }
        insertMode = false;
        doSelectPage(1);
        doDisplayPage(1);
        repaint();
    }

    /* 2-8, 3-18 */
    public void doBackspace() {
        pages[selectedPage].doBS();
        repaint();
    }
    /* 2-8, 3-18 */
    public void doHTab() {
        pages[selectedPage].doHTab();
        repaint();
    }
    /* 2-8 */
    public void doLineFeed() {
        pages[selectedPage].doLF();
        repaint();
    }
    /* 2-8, 3-18 */
    public void doCarriageReturn() {
        pages[selectedPage].doCR();
        repaint();
    }
    /* 2-9, 3-19 */
    public void doSetTab() {
        pages[selectedPage].setTab(false);
    }
    /* 2-9, 3-19 */
    public void doClearTab() {
        pages[selectedPage].clearTab(false);
    }
    /* 2-9, 3-19 */
    public void doClearAllTabs() {
        pages[selectedPage].clearAllTabs();
    }
    /* 2-14, 3-23 */
    public void doSetVideoAttribute(int attrib) {
        pages[selectedPage].setAttribute(attrib);
        repaint();
    }
    /* 2-15, 3-24 */
    public void doSetDefaultVideoAttribute(int attrib) {
        pages[selectedPage].setDefaultAttribute(attrib);
        repaint();
    }
    /* 3-15, 3-16 */
    public void doSetBufferAddress(int row, int column) {
        pages[selectedPage].bufferSetPos(row - 1, column - 1);
    }
    public void doSetCursorAddress(int row, int column) {
        doSetCursorAddress(false, row, column);
    }
    public void doSetCursorAddress(boolean displayedPage, int row, int column) {
        if (row > ROWS || column > COLS) {
            return;
        }
        int page = displayedPage ? displayPage : selectedPage;
        pages[page].cursorSetPos(row - 1, column - 1);
        repaint();
    }

    /* 3-37 */
    public void doStartField(FieldAttributes attribs) {
        // This method is eqvivalent to setVideoAttribute in this
        // mode
        doSetVideoAttribute(attribs.getVideoAttrib() & 0x1f);
    }

    /* 3-19 */
    public void doBackTab() {
        pages[selectedPage].doBackTabs(1);
        repaint();
    }
    /* 3-12 */
    public void doSetMaxPageNumber(int n) {
        // We just support MAX_PAGES
    }
    /* 2-7, 3-17 */
    public void doCursorUp() {
        pages[selectedPage].cursorUp(1);
        repaint();
    }
    /* 2-8, 3-17 */
    public void doCursorRight() {
        pages[selectedPage].cursorForward(1);
        repaint();
    }
    /* 2-8, 3-18 */
    public void doCursorHomeDown() {
        pages[selectedPage].cursorHomeDown();
        repaint();
    }
    /* 2-8, 3-18 */
    public void doCursorHome() {
        pages[selectedPage].cursorSetPos(0, 0);
        repaint();
    }

    /* 3-48, 3-49 */
    public void doClearMemoryToSpaces(int startRow, int startCol,
                                      int endRow, int endColumn) {
        pages[selectedPage].clear(startCol - 1, startCol - 1,
                                  endRow - 1, endColumn);
        repaint();
    }
    /* 2-10, 3-49 */
    public void doEraseToEndOfPageOrMemory() {
        pages[selectedPage].clearBelow(false);
        repaint();
    }
    /* 2-11, 3-49 */
    public void doEraseToEndOfLineOrField() {
        pages[selectedPage].clearRight(false);
        repaint();
    }
    /* 3-45, 3-46 */
    public String doReadWithAddress(int startRow, int startCol,
                                    int endRow, int endCol) {
        return pages[selectedPage].read(startRow - 1, startCol - 1,
                                        endRow - 1, endCol - 1);
    }
    /* 3-46, 3-47 */
    public String doReadWithAddressAll(int startRow, int startCol,
                                       int endRow, int endCol) {
        return pages[selectedPage].read(startRow - 1, startCol - 1,
                                        endRow - 1, endCol - 1);
    }
    /* 3-50 */
    public void doInsertLine() {
        pages[selectedPage].insertLines(1, false);
        repaint();
    }
    /* 3-50 */
    public void doDeleteLine() {
        pages[selectedPage].deleteLines(1, false);
        repaint();
    }
    /* 3-51 */
    public void doInsertCharacter() {
        pages[selectedPage].insertChars(1, false);
        repaint();
    }
    /* 3-51 */
    public void doDeleteCharacter() {
        pages[selectedPage].deleteChars(1, false);
        repaint();
    }

    /* 3-44 */
    public String doReadWholePageOrBuffer() {
        return pages[selectedPage].readWhole();
    }
    /* 2-10, 3-11 */
    public void doDisplayPage(int n) {
        if (n >= 1 && n <= MAX_PAGES) {
            pages[displayPage].setDisplay(null);
            displayPage = n - 1;
            pages[displayPage].setDisplay(display);
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

    public int getRow() {
        return pages[selectedPage].getCursorRow() + 1;
    }
    public int getCol() {
        return pages[selectedPage].getCursorCol() + 1;
    }
    public int getPage() {
        return selectedPage + 1;
    }

    public char[] getChars(int visTop, int row) {
        return pages[displayPage].getCharsAt(visTop, row);
    }
    public int[] getAttribs(int visTop, int row) {
        return pages[displayPage].getAttribsAt(visTop, row);
    }

    public void doClickSelect(int row, int col, String selectDelims) {
        pages[displayPage].doClickSelect(row, col, selectDelims);
    }
    public void resetClickSelect() {
        pages[displayPage].resetClickSelect();
    }
    public void setSelection(int row1, int col1, int row2, int col2) {
        pages[displayPage].setSelection(row1, col1, row2, col2);
    }
    public void selectAll() {
        pages[displayPage].selectAll();
    }
    public void resetSelection() {
        pages[displayPage].resetSelection();
    }
    public String getSelection(String eol) {
        return pages[displayPage].getSelection(eol);
    }

    public SearchContext search(SearchContext lastContext, String key,
                                boolean reverse, boolean caseSens) {
        SearchContext result = pages[displayPage].search(lastContext, key,
                               reverse, caseSens);
        if (result != null) {
            if (display != null) {
                display.setVisTop(result.getStartRow());
            }
            pages[displayPage].setVisTop(result.getStartRow());
            pages[displayPage].setSelection(result.getStartRow(),
                                            result.getStartCol(),
                                            result.getEndRow(),
                                            result.getEndCol());
            // Might set the selection when the terminal window is out
            // of focus, so better do a forcible repaint
            if (display != null) {
                display.repaint(true);
            }
        }
        return result;
    }


    public void setBuffer(Screen6530Buffer buf) {
        pages[0].setBuffer(buf);
    }


}

