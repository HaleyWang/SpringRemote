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

/**
 * Control interface for the display classes
 */
interface DisplayController {
    public static int LEFT_BUTTON    = 0;
    public static int MIDDLE_BUTTON  = 1;
    public static int RIGHT_BUTTON   = 2;
    public static int UNKNOWN_BUTTON = -1;

    // Mouse events
    public void mouseClicked(int visTop, int row, int col, int modifier,
                             int which);
    public void mousePressed(int visTop, int row, int col, int modifier,
                             int which, int x, int y);
    public void mouseReleased(int visTop, int row, int col, int modifier,
                              int which);
    public void mouseDragged(int visTop, int row, int col, int modifier,
                             int which, int delta);

    public void scrollUp();
    public void scrollDown();

    // Display event
    public void displayDragResize(int newRows, int newCols);
    public void displayResized(int newRows, int newCols,
                               int vpixels, int hpixels);
}

