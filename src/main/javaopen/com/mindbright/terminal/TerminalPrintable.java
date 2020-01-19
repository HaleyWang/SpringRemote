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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.print.PageFormat;
import java.awt.print.Printable;

public class TerminalPrintable implements Printable {
    /* What to print */
    public static int SCREEN = 1;
    public static int BUFFER = 2;

    private static Color BG_COLOR = Color.white;
    private static Color FG_COLOR = Color.black;

    private DisplayModel model;
    private int topLine;
    private int fontSize;
    private int lineSpaceDelta;

    // Font metrics
    private boolean fontMetricsInitialized = false;
    private int charWidth;
    private int charHeight;
    private int baselineIndex;

    private Font plainFont;
    private Font boldFont;

    public TerminalPrintable(DisplayModel model, int what,
                             String fontName, int fontSize) {
        this.model = model;
        this.fontSize = fontSize;
	this.lineSpaceDelta = fontSize/10;

        // Where to start printing
        if (what == SCREEN) {
            topLine = model.getBufferRows()-model.getDisplayRows();
        } else {
            topLine = 0;
        }

        plainFont = new Font(fontName, Font.PLAIN, fontSize);
        boldFont  = new Font(fontName, Font.BOLD, fontSize);
    }

    private void initFontMetrics(Graphics g) {
        g.setFont(plainFont);
        FontMetrics fm = g.getFontMetrics();
        charWidth      = fm.charWidth('W');
        charHeight     = fm.getHeight() + lineSpaceDelta;
        baselineIndex  = fm.getMaxAscent() + fm.getLeading() - 1;
        fontMetricsInitialized = true;
    }

    /*
     */
    public int print(Graphics g, PageFormat f, int pageIndex) {
        
        if (!fontMetricsInitialized) {
            initFontMetrics(g);
        }

        // This assumes all pages have identical format
        int height = (int)(f.getImageableHeight()-f.getImageableY()*2);
        int pageLines = height/fontSize;
        int pageTop = pageLines * pageIndex + topLine;

        if (pageTop > model.getBufferRows()) {
            return NO_SUCH_PAGE;
        }

        int pageEnd = pageTop + pageLines;
        if (pageEnd > model.getBufferRows()) {
            pageEnd = model.getBufferRows();
        }

        g.setFont(plainFont);
        for (int row = pageTop; row < pageEnd; row++) {
            int y = (int)f.getImageableY() + (row-pageTop)*fontSize;
            int[] attrRow = model.getAttribs(0,  row);
            char[] charRow = model.getChars(0, row);


            for(int col = 0; col < model.getDisplayCols(); col++) {
                Color bgColor = BG_COLOR;
                Color fgColor = FG_COLOR;
                int attr       = attrRow[col];
                int attrMasked = (attr & DisplayModel.MASK_ATTR);
                boolean doDraw = false;
                int x = (int)f.getImageableX() + (charWidth * col);
                if (((attr & DisplayModel.ATTR_INVERSE) != 0)) {
                    if ((attr & DisplayModel.ATTR_FGCOLOR) != 0) {
                        bgColor = DisplayView.termColors[
                            (attr & DisplayModel.MASK_FGCOL) 
                            >>> DisplayModel.SHIFT_FGCOL];
                    } else {
                        bgColor = FG_COLOR;
                    }
                    if ((attr & DisplayModel.ATTR_BGCOLOR) != 0) {
                        fgColor = DisplayView.termColors[
                            (attr & DisplayModel.MASK_BGCOL) 
                            >>> DisplayModel.SHIFT_BGCOL];
                    } else {
                        fgColor = BG_COLOR;
                    }

                    if ((attr & DisplayModel.ATTR_LOWINTENSITY) != 0) {
                        bgColor = DisplayUtil.makeDimmerColor(bgColor);
                    }
                    doDraw = true;
                } else {
                    if((attr & DisplayModel.ATTR_BGCOLOR) != 0) {
                        bgColor = DisplayView.termColors[
                            (attr & DisplayModel.MASK_BGCOL) 
                            >>> DisplayModel.SHIFT_BGCOL];
                        doDraw = true;
                    }
                    if((attr & DisplayModel.ATTR_FGCOLOR) != 0) {
                        fgColor = DisplayView.termColors[
                            (attr & DisplayModel.MASK_FGCOL) 
                            >>> DisplayModel.SHIFT_FGCOL];
                    }

                    if ((attr & DisplayModel.ATTR_LOWINTENSITY) != 0) {
                        fgColor = DisplayUtil.makeDimmerColor(fgColor);
                    }
                }

                // Only draw if bg is different from what we cleared area with
                if (doDraw) {
                    g.setColor(bgColor);
                    g.fillRect(x, y, charWidth, charHeight);
                }
                g.setColor(fgColor);

                if ((attrMasked & DisplayModel.ATTR_CHARDRAWN) != 0) {
                    if ((attr & DisplayModel.ATTR_INVISIBLE) != 0) {
                        // Don't draw anything invisible, but the
                        // underline should be drawn anyway.
                    } else if((attr & DisplayModel.ATTR_LINEDRAW) != 0) {
                        DisplayUtil.drawLineDrawChar(g, x, y, baselineIndex,
                                                     charRow[col],
                                                     charWidth, charHeight);
                    } else if((attr & DisplayModel.ATTR_BOLD) != 0 ||
                              (attr & DisplayModel.ATTR_BLINKING) != 0) {
                        // Approximate blinking with bold font
                        g.setFont(boldFont);
                        g.drawChars(charRow, col, 1, x, y + baselineIndex);
                        g.setFont(plainFont);
                    } else if (charRow[col] != ' ') { // no need to draw spaces
                        g.drawChars(charRow, col, 1, x, y + baselineIndex);
                    }
                    if((attr & DisplayModel.ATTR_UNDERLINE) != 0)
                        g.drawLine(x, y + baselineIndex, x + charWidth,
                                   y + baselineIndex);
                }
            }
        }
        return PAGE_EXISTS;
    }
}
