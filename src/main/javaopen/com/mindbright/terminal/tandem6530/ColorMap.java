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

import com.mindbright.terminal.DisplayModel;
import com.mindbright.terminal.DisplayView;

public class ColorMap {

    protected byte[] defaultSource = {
                                         (byte) 0x70, (byte) 0x61, (byte) 0x52, (byte) 0x43, (byte) 0x34,
                                         (byte) 0x25, (byte) 0x16, (byte) 0x07, (byte) 0x78, (byte) 0x69,
                                         (byte) 0x5a, (byte) 0x4b, (byte) 0x3c, (byte) 0x2d, (byte) 0x1e,
                                         (byte) 0x0f, (byte) 0x70, (byte) 0x61, (byte) 0x52, (byte) 0x43,
                                         (byte) 0x34, (byte) 0x25, (byte) 0x16, (byte) 0x07, (byte) 0x78,
                                         (byte) 0x69, (byte) 0x5a, (byte) 0x4b, (byte) 0x3c, (byte) 0x2d,
                                         (byte) 0x1e, (byte) 0x0f };
    protected byte[] source;
    protected int map[];

    public ColorMap() {
        source = new byte[defaultSource.length];
        reset();
    }

    protected int mapColor(boolean highIntensity, int color) {
        int ret;

        switch (color) {
        case 0:
            // black/dark grey
            ret = highIntensity ? DisplayView.COLOR_I_BLACK :
                  DisplayView.COLOR_BLACK;
            break;
        case 1:
            // blue/bright blue
            ret = highIntensity ? DisplayView.COLOR_I_BLUE :
                  DisplayView.COLOR_BLUE;
            break;
        case 2:
            // green/bright grenn
            ret = highIntensity ? DisplayView.COLOR_I_GREEN :
                  DisplayView.COLOR_GREEN;
            break;
        case 3:
            // cyan/bright cyan
            ret = highIntensity ? DisplayView.COLOR_I_CYAN :
                  DisplayView.COLOR_CYAN;
            break;
        case 4:
            // red/bright red
            ret = highIntensity ? DisplayView.COLOR_I_RED :
                  DisplayView.COLOR_RED;
            break;
        case 5:
            // magenta/brigth magenta
            ret = highIntensity ? DisplayView.COLOR_I_MAGENTA :
                  DisplayView.COLOR_MAGENTA;
            break;
        case 6:
            // brown/yellow
            ret = highIntensity ? DisplayView.COLOR_I_YELLOW :
                  DisplayView.COLOR_YELLOW;
            break;
        case 7:
            // gray/white
            ret = highIntensity ? DisplayView.COLOR_I_WHITE :
                  DisplayView.COLOR_WHITE;
            break;
        default:
            ret = -1;
        }

        return ret;
    }
    protected int mapByteToAttrib(byte b) {
        boolean highIntensity = (b & 0x08) == 0x08;
        int fg = mapColor(highIntensity, b & 0x07);
        int bg = mapColor(highIntensity, (b & 0x70) >> 4);

        if (fg == -1 || bg == -1) {
            return DisplayModel.ATTR_CHARDRAWN;
        }


        return DisplayModel.ATTR_CHARDRAWN |
               DisplayModel.ATTR_FGCOLOR | (fg << DisplayModel.SHIFT_FGCOL) |
               DisplayModel.ATTR_BGCOLOR | (bg << DisplayModel.SHIFT_BGCOL);
    }

    protected int[] makeMap(byte src[]) {
        int tmp[] = new int[src.length];
        for (int i = 0; i < src.length; i++) {
            tmp[i] = mapByteToAttrib(src[i]);
        }
        return tmp;
    }

    public void set
        (int startIndex, byte entries[]) {
        if (startIndex < 0) {
            return;
        }
        if (startIndex + entries.length > source.length) {
            return;
        }
        System.arraycopy(entries, 0, source, startIndex, entries.length);
        map = makeMap(source);
    }

    public void reset() {
        set
            (0, defaultSource);
    }

    public String read() {
        char toHex[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                         'a', 'b', 'c', 'd', 'e', 'f' };
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < source.length; i++) {
            buf.append(toHex[(source[i] & 0xf0) >> 4]);
            buf.append(toHex[(source[i] & 0x0f)]);
        }

        return buf.toString();
    }

    public int map(int index) {
        return map[index];
    }

}
