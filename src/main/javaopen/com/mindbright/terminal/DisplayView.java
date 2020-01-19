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

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;

public interface DisplayView {
    public void setModel(DisplayModel model);
    public DisplayModel getModel();
    public void setController(DisplayController controller);
    public void setKeyListener(KeyListener keyListener);
    public void delKeyListener(KeyListener keyListener);

    // Methods for setting the top line of the display
    public void setVisTopChangeAllowed(boolean set);
    public void setVisTopDelta(int delta);
    public void setVisTopDelta(int delta, boolean force);
    public void setVisTop(int visTop);
    public void setVisTop(int visTop, boolean force);
    public void setPendingVisTopChange(int visTop);

    public void updateScrollbarValues();
    public void updateDirtyArea(int top, int left, int bottom, int right);
    public void repaint(boolean force);
    public void repaint();
    public void setGeometry(int row, int col);
    public void setResizable(boolean resizable);
    public void resetSelection();
    public void setSelection(int row1, int col1, int row2, int col2);
    public void setNoCursor();
    public void setCursorPosition(int row, int col);
    public void reverseColors();

    public void doBell();
    public void doBell(boolean visualBell);

    public void setLogo(Image logoImg, int x, int y, int w, int h);
    public Image getLogo();
    public boolean showLogo();
    public void hideLogo();

    public void moveScrollbar(String scrollPos);
    public void setLineSpaceDelta(int delta);
    public void setBackgroundColor(Color c);
    public void setForegroundColor(Color c);
    public void setCursorColor(Color c);
    public void setFont(String name, int size);
    public void setPosition(int xPos, int yPos);
    public Container getPanelWithScrollbar(String scrollPos);

    public void requestFocus();
    public Component getAWTComponent();    

    public void setIgnoreClose(); // XXX remove
    public void windowClosed();

    public Component mkButton(String label, String cmd, ActionListener listener);

    /**
     * Check if the give character is wider than normal for the
     * font. This is meant to catch certain asian characters which are
     * double-width, even in a monospaced font:-(
     */
    public boolean isWide(char c);

    public final static int COLOR_BLACK         = 0;
    public final static int COLOR_RED           = 1;
    public final static int COLOR_GREEN         = 2;
    public final static int COLOR_YELLOW        = 3;
    public final static int COLOR_BLUE          = 4;
    public final static int COLOR_MAGENTA       = 5;
    public final static int COLOR_CYAN          = 6;
    public final static int COLOR_WHITE         = 7;
    public final static int COLOR_I_BLACK       = 8;
    public final static int COLOR_I_RED         = 9;
    public final static int COLOR_I_GREEN       = 10;
    public final static int COLOR_I_YELLOW      = 11;
    public final static int COLOR_I_BLUE        = 12;
    public final static int COLOR_I_MAGENTA     = 13;
    public final static int COLOR_I_CYAN        = 14;
    public final static int COLOR_I_WHITE       = 15;

    public final static String[] termColorNames = {
        "black", "red", "green", "yellow", "blue", "magenta", "cyan", "white",
        "i_black", "i_red", "i_green", "i_yellow",
        "i_blue","i_magenta", "i_cyan", "i_white"
    };

    public final static Color termColors[] = {
        Color.black,
        Color.red.darker(),
        Color.green.darker(),
        Color.yellow.darker(),
        Color.blue.darker(),
        Color.magenta.darker(),
        Color.cyan.darker(),
        Color.white,
        Color.darkGray,
        Color.red,
        Color.green,
        Color.yellow,
        Color.blue,
        Color.magenta,
        Color.cyan,
        Color.white
    };
}
