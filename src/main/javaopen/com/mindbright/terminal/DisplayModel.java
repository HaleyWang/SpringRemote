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

public interface DisplayModel {
    public final static int ATTR_BOLD         = 0x0001;
    public final static int ATTR_LOWINTENSITY = 0x0002;
    public final static int ATTR_UNDERLINE    = 0x0004;
    public final static int ATTR_BLINKING     = 0x0008;
    public final static int ATTR_INVERSE      = 0x0010;
    public final static int ATTR_INVISIBLE    = 0x0020;
    public final static int ATTR_FGCOLOR      = 0x0040;
    public final static int ATTR_BGCOLOR      = 0x0080;

    public final static int ATTR_CHARNOTDRAWN = 0x0000;
    public final static int ATTR_LINEDRAW     = 0x0100;
    public final static int ATTR_DWIDTH_L     = 0x0200;
    public final static int ATTR_DWIDTH_R     = 0x0400;
    public final static int ATTR_SELECTED     = 0x1000;
    public final static int ATTR_CHARDRAWN    = 0x8000;

    public final static int MASK_ATTR   = 0x0000ffff;
    public final static int MASK_FGCOL  = 0x00ff0000;
    public final static int MASK_BGCOL  = 0xff000000;
    public final static int SHIFT_FGCOL = 16;
    public final static int SHIFT_BGCOL = 24;


    public char[] getChars(int visTop, int row);
    public int[] getAttribs(int visTop, int row);
    public int getDisplayRows();
    public int getDisplayCols();
    public int getBufferRows();
}
