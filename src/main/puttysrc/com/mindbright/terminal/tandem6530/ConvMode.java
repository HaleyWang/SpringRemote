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

public class ConvMode extends ModeBase {
    private Screen6530 screen;
    private Terminal6530Callback termCallback;
    private Parser parser;

    private boolean halfDuplex = false;
    private int localTransmitColumn = 1;
    private boolean localActionMode = false;
    private char enterKeyFunction[];
    private DisplayView display;


    public ConvMode(Terminal6530Callback termCallback, Parser parser) {
        this.termCallback = termCallback;
        this.parser = parser;
        screen = new Screen6530(ROWS, COLUMNS, ' ', false);
        screen.setSaveLines(400);
    }

    public void setHalfDuplex(boolean set
                                 ) {
        halfDuplex = set
                         ;
    }

    public void setLocalTransmitColumn(int col) {
        localTransmitColumn = col;
    }

    public void setEnterKeyFunction(char keys[]) {
        enterKeyFunction = keys;
    }

    public void hostChar(char c) {
        screen.cursorWrite(c);
        repaint();
    }

    public void setDisplay(DisplayView display) {
        this.display = display;
        screen.setDisplay(display);
    }

    public int getVisTop() {
        return screen.getVisTop();
    }

    public void keyHandler(char c, int virtualKey, int modifiers) {
        boolean keyProcessed = false;

        switch (virtualKey) {
        case KeyEvent.VK_TAB:
            doTab();
            keyProcessed = true;
            break;
        case KeyEvent.VK_ENTER:
            if ((modifiers & InputEvent.ALT_MASK) == InputEvent.ALT_MASK) {
                if (localActionMode) {
                    int curCol = screen.getCursorCol();
                    char row[] = screen.getCharsAt(screen.getScreenTop(),
                                                   screen.getCursorRow());
                    for (int i = localTransmitColumn; i < curCol; i++) {
                        termCallback.send(row[i]);
                    }
                    localActionMode = false;
                } else {
                    localActionMode = true;
                }
                keyProcessed = true;
            } else if ((modifiers & InputEvent.SHIFT_MASK) ==
                       InputEvent.SHIFT_MASK || modifiers == 0) {
                if (enterKeyFunction == null) {
                    keyHandler(CR, 0, 0);
                } else {
                    for (int i = 0; i < enterKeyFunction.length; i++) {
                        keyHandler(enterKeyFunction[i], 0, 0);
                    }
                }
                if (halfDuplex) {
                    doCarriageReturn();
                }
                keyProcessed = true;
            } else if ((modifiers & InputEvent.CTRL_MASK) ==
                       InputEvent.CTRL_MASK) {
                doCursorToLastCharOnRow();
                keyProcessed = true;
            }
            break;

        case KeyEvent.VK_HOME:
            if ((modifiers & InputEvent.CTRL_MASK) == InputEvent.CTRL_MASK) {
                doCursorToLastCharOnScreen();
                keyProcessed = true;
            } else if (modifiers == 0) {
                doCursorHome();
                keyProcessed = true;
            }
            break;

        case KeyEvent.VK_END:
            if (modifiers == 0) {
                doCursorHomeDown();
                keyProcessed = true;
            }
            break;

        case KeyEvent.VK_RIGHT:
            if (modifiers == 0) {
                doCursorRight();
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
            if ((modifiers & InputEvent.ALT_MASK) == InputEvent.ALT_MASK) {
                doRollDown();
                keyProcessed = true;
            } else if (modifiers == 0) {
                doCursorUp();
                keyProcessed = true;
            }
            break;

        case KeyEvent.VK_DOWN:
            if ((modifiers & InputEvent.ALT_MASK) == InputEvent.ALT_MASK) {
                doRollUp();
                keyProcessed = true;
            } else if (modifiers == 0) {
                doCursorDown();
                keyProcessed = true;
            }
            break;

        case KeyEvent.VK_PAGE_UP:
            if (modifiers == 0) {
                doPageUp();
                keyProcessed = true;
            }
            break;

        case KeyEvent.VK_PAGE_DOWN:
            if (modifiers == 0) {
                doPageDown();
                keyProcessed = true;
            }
            break;


        case KeyEvent.VK_1:
            if ((modifiers &
                    (InputEvent.ALT_MASK | InputEvent.SHIFT_MASK)) ==
                    (InputEvent.ALT_MASK | InputEvent.SHIFT_MASK)) {
                doClearTab();
                keyProcessed = true;
            } else if ((modifiers & InputEvent.ALT_MASK) ==
                       InputEvent.ALT_MASK) {
                doSetTab();
                keyProcessed = true;
            }
            break;


        case KeyEvent.VK_2:
            if ((modifiers &
                    (InputEvent.ALT_MASK | InputEvent.SHIFT_MASK)) ==
                    (InputEvent.ALT_MASK | InputEvent.SHIFT_MASK)) {
                doEraseToEndOfPageOrMemory();
                keyProcessed = true;
            } else if ((modifiers & InputEvent.ALT_MASK) ==
                       InputEvent.ALT_MASK) {
                doEraseToEndOfLineOrField();
                keyProcessed = true;
            }
            break;

        case KeyEvent.VK_3:
            if ((modifiers & InputEvent.ALT_MASK) == InputEvent.ALT_MASK) {
                doClearAllTabs();
                keyProcessed = true;
            }
            break;

        case KeyEvent.VK_4:
            if ((modifiers & InputEvent.ALT_MASK) == InputEvent.ALT_MASK) {
                System.out.println(screen.getStatus());
                keyProcessed = true;
            }
            break;

        default:
            break;
        }

        if (keyProcessed) {
            return;
        }

        if (c == KeyEvent.CHAR_UNDEFINED) {
            return;
        }

        if (halfDuplex || localActionMode) {
            // Process keys locally since they are not echoed back
            // in halfDuplex mode or in local action mode.
            char parsed = Parser.IGNORE;

            try {
                parsed = parser.parse(c);
            } catch (ParseException e) {
                termCallback.error(e.getMessage());
                return;
            }

            if (parsed != Parser.IGNORE) {
                hostChar(c);
            }
        }

        if (!localActionMode) {
            termCallback.send(c);
        }
    }

    public void switchReset() {
        screen.setVisTop(0);
        screen.cursorSetPos(0, 0);
        screen.clearBelow();
        screen.clearAllTabs();
        if (display != null) {
            display.updateScrollbarValues();
            repaint();
        }
    }

    /* 2-8, 3-18 */
    public void doBackspace() {
        screen.doBS();
        repaint();
    }
    /* 2-8, 3-18 */
    public void doHTab() {
        screen.doHTab();
        repaint();
    }
    /* 2-54 */
    public void doTab() {
        termCallback.send(screen.spaceToNextTabStop());
    }
    /* 2-8 */
    public void doLineFeed() {
        screen.cursorDown(1);
        repaint();
    }
    /* 2-8, 3-18 */
    public void doCarriageReturn() {
        screen.doCR();
        repaint();
    }
    /* 2-6, 3-14 */
    public void doSetCursorAddress(boolean displayedPage, int row, int column) {
        screen.cursorSetPos(row - 1, column - 1);
        repaint();
    }
    /* 2-9, 3-19 */
    public void doSetTab() {
        screen.setTab(true);
    }
    /* 2-9, 3-19 */
    public void doClearTab() {
        screen.clearTab(true);
    }
    /* 2-9, 3-19 */
    public void doClearAllTabs() {
        screen.clearAllTabs();
    }
    /* 2-14, 3-23 */
    public void doSetVideoAttribute(int attrib) {
        screen.setAttribute(attrib);
        repaint();
    }
    /* 2-15, 3-24 */
    public void doSetDefaultVideoAttribute(int attrib) {
        screen.setDefaultAttribute(attrib);
        repaint();
    }
    /* 2-50 */
    public void doDefineEnterKeyFunction(char str[]) {
        enterKeyFunction = str;
    }
    /* 2-55 */
    public void doCursorToLastCharOnScreen() {
        screen.cursorToLastCharOnScreen();
        repaint();
    }
    /* 2-55 */
    public void doCursorToLastCharOnRow() {
        screen.cursorToLastCharOnRow();
        repaint();
    }
    /* 2-7, 3-17 */
    public void doCursorUp() {
        screen.cursorUp(1);
        repaint();
    }
    /* 2-55 */
    public void doCursorDown() {
        screen.cursorDown(1);
        repaint();
    }
    /* 2-8, 3-17 */
    public void doCursorRight() {
        screen.cursorForward(1);
        repaint();
    }
    /* 2-8, 3-18 */
    public void doCursorHomeDown() {
        int lastRow = screen.getLastRow();
        screen.setVisTop(lastRow - ROWS);
        screen.cursorSetPos(ROWS - 1, 0);
        repaint();
    }
    /* 2-8, 3-18 */
    public void doCursorHome() {
        screen.setVisTop(0);
        screen.cursorSetPos(0, 0);
        repaint();
    }
    /* 2-9 */
    public void doRollUp() {
        setVisTopDelta(-1);
    }
    /* 2-9 */
    public void doRollDown() {
        setVisTopDelta(1);
    }
    /* 2-10 */
    public void doPageUp() {
        setVisTopDelta(-ROWS);
    }
    /* 2-10 */
    public void doPageDown() {
        setVisTopDelta(ROWS);
    }
    /* 2-10 */
    public void doClearMemoryToSpaces() {
        doCursorHome();
        doEraseToEndOfPageOrMemory();
        repaint();
    }
    /* 2-11, 3-49 */
    public void doEraseToEndOfLineOrField() {
        screen.clearRight();
        repaint();
    }
    /* 2-10, 3-49 */
    public void doEraseToEndOfPageOrMemory() {
        screen.clearBelow();
        repaint();
    }

    public char[] getChars(int visTop, int row) {
        return screen.getCharsAt(visTop, row);
    }
    public int[] getAttribs(int visTop, int row) {
        return screen.getAttribsAt(visTop, row);
    }
    public int getBufferRows() {
        return screen.getTotalLines();
    }

    public int getRow() {
        return screen.getCursorRow() + 1;
    }
    public int getCol() {
        return screen.getCursorCol() + 1;
    }
    public int getPage() {
        return 1;
    }

    public void doClickSelect(int row, int col, String selectDelims) {
        screen.doClickSelect(row, col, selectDelims);
    }
    public void resetClickSelect() {
        screen.resetClickSelect();
    }
    public void setSelection(int row1, int col1, int row2, int col2) {
        screen.setSelection(row1, col1, row2, col2);
    }
    public void selectAll() {
        screen.selectAll();
    }
    public void resetSelection() {
        screen.resetSelection();
    }
    public String getSelection(String eol) {
        return screen.getSelection(eol);
    }

    public SearchContext search(SearchContext lastContext, String key,
                                boolean reverse, boolean caseSens) {
        SearchContext result = screen.search(lastContext, key,
                                             reverse, caseSens);
        if (result != null) {
            if (display != null) {
                display.setVisTop(result.getStartRow());
            }
            screen.setVisTop(result.getStartRow());
            screen.setSelection(result.getStartRow(), result.getStartCol(),
                                result.getEndRow(), result.getEndCol());
            // Might set the selection when the terminal window is out
            // of focus, so better do a forcible repaint
            if (display != null) {
                display.repaint(true);
            }
        }
        return result;
    }


    public Screen6530Buffer getBuffer() {
        return screen.getBuffer();
    }

    private void repaint() {
        if (display != null) {
            display.repaint();
        }
    }

    private void setVisTopDelta(int delta) {
        int row = screen.getCursorRow();
        int col = screen.getCursorCol();
        screen.setVisTopDelta(delta);
        screen.cursorSetPos(row, col, true);
        repaint();
    }
}
