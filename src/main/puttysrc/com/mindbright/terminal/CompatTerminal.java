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


/** This is a interface that contains the methods from the old Terminal
 * interface that a TerminalInterpreter might need.
 */
public interface CompatTerminal {
    public final static int ATTR_BOLD         = 0x0001;
    public final static int ATTR_LOWINTENSITY = 0x0002;
    public final static int ATTR_UNDERLINE    = 0x0004;
    public final static int ATTR_BLINKING     = 0x0008;
    public final static int ATTR_INVERSE      = 0x0010;
    public final static int ATTR_INVISIBLE    = 0x0020;
    public final static int ATTR_FGCOLOR      = 0x0040;
    public final static int ATTR_BGCOLOR      = 0x0080;

    final static int OPT_REV_VIDEO    = 0;
    final static int OPT_AUTO_WRAP    = 1;
    final static int OPT_REV_WRAP     = 2;
    final static int OPT_INSERTMODE   = 3;
    final static int OPT_AUTO_LF      = 4;
    final static int OPT_SCROLL_SK    = 5;
    final static int OPT_SCROLL_SI    = 6;
    final static int OPT_VIS_CURSOR   = 7;
    final static int OPT_LOCAL_ECHO   = 8;
    final static int OPT_VIS_BELL     = 9;
    final static int OPT_MAP_CTRLSP   = 10;
    final static int OPT_DECCOLM      = 11;
    final static int OPT_DEC132COLS   = 12;
    final static int OPT_PASSTHRU_PRN = 13;
    final static int OPT_LOCAL_PGKEYS = 14;
    final static int OPT_COPY_CRNL    = 15;
    final static int OPT_ASCII_LDC    = 16;
    final static int OPT_COPY_SEL     = 17;
    final static int OPT_LAST_OPT     = 18;

    public void setTitle(String title);
    public int rows();
    public int cols();

    public void fillScreen(char c);

    public void write(char c);
    public void write(char[] c, int off, int len);
    public void write(String str);
    public void writeLineDrawChar(char c);

    public void typedChar(char c);
    public void sendBytes(byte[] b);

    public void doBell();
    public void doBS();
    public void doTab();
    public void doTabs(int n);
    public void doBackTabs(int n);
    public void setTab(int col);
    public void clearTab(int col);
    public void resetTabs();
    public void clearAllTabs();
    public void doCR();
    public void doLF();

    public void resetInterpreter();
    public void resetWindow();
    public void setWindow(int top, int bottom);
    public void setWindow(int top, int right, int bottom, int left);
    public int  getWindowTop();
    public int  getWindowBottom();
    public int  getWindowLeft();
    public int  getWindowRight();

    public int getCursorV();
    public int getCursorH();

    public void cursorSetPos(int v, int h, boolean relative);
    public void cursorUp(int n);
    public void cursorDown(int n);
    public void cursorForward(int n);
    public void cursorBackward(int n);
    public void cursorIndex(int n);
    public void cursorIndexRev(int n);

    public void cursorSave();
    public void cursorRestore();

    public void screenSave();
    public void screenRestore();

    public void scrollUp(int n);
    public void scrollDown(int n);

    public void clearBelow();
    public void clearAbove();
    public void clearScreen();
    public void clearRight();
    public void clearLeft();
    public void clearLine();

    public void eraseChars(int n);
    public void insertChars(int n);
    public void insertLines(int n);
    public void deleteChars(int n);
    public void deleteLines(int n);

    public void printScreen();
    public void startPrinter();
    public void stopPrinter();

    public void    setOption(int opt, boolean val);
    public boolean getOption(int opt);

    public void    setAttribute(int attr, boolean val);
    public boolean getAttribute(int attr);
    public void    setForegroundColor(int c);
    public void    setBackgroundColor(int c);
    public void    clearAllAttributes();
}

