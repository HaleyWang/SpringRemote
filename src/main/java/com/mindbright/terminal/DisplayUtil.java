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
import java.awt.Graphics;
import java.util.Hashtable;

public class DisplayUtil {
    private static Hashtable<Color, Color> dimmmedColors = new Hashtable<Color, Color>();

    protected static Color makeDimmerColor(Color orgColor) {
        if (orgColor == null) {
            return null;
        }

        Color dimColor = dimmmedColors.get(orgColor);
        if (dimColor != null) {
            return dimColor;
        }

        // Can't use darker() method in Color, since it don't make
        // black dimmer.

        float hsbComps[] = Color.RGBtoHSB(orgColor.getRed(),
                                          orgColor.getGreen(),
                                          orgColor.getBlue(), null);
        float hue = hsbComps[0];
        float saturation = hsbComps[1];
        float brightness = hsbComps[2];

        if ((saturation*saturation + brightness*brightness) < 0.1) {
            // orgColor is very dark, increate saturation and brightness
            // to make it appear dimmer
            saturation = (float) 0.3;
            brightness = (float) 0.3;
        } else {
            brightness *= 0.50;
        }

        dimColor = Color.getHSBColor(hue, saturation, brightness);
        dimmmedColors.put(orgColor, dimColor);

        return dimColor;
    }

    private static void drawTuplet(Graphics g, int x, int y, int x2, int y2,
                                   int bi, String s1, String s2) {
        Font font = g.getFont();
        g.setFont(new Font(font.getName(), font.getStyle(), font.getSize()/2));
        g.drawString(s1, x+1, y+1 + bi/2);
        g.drawString(s2, x2, y2 + bi/2);
        g.setFont(font);
    }

    protected static void drawLineDrawChar(Graphics g, int x, int y, int bi,
                                           char c,
                                           int charWidth, int charHeight) {
        int x2 = (x + (charWidth  / 2));
        int y2 = (y + (charHeight / 2));
        int xx = (x + charWidth);
        int yy = (y + charHeight);

        switch(c) {
        case ' ': // Blank
        case '_': // Blank
            break;
        case '`': // Diamond
            int[] polyX = new int[4];
            int[] polyY = new int[4];
            polyX[0] = x2;
            polyY[0] = y;
            polyX[1] = xx;
            polyY[1] = y2;
            polyX[2] = x2;
            polyY[2] = yy;
            polyX[3] = x;
            polyY[3] = y2;
            g.fillPolygon(polyX, polyY, 4);
            break;
        case 'a': // Checker board (stipple)
            for (int i=x; i<xx; i++) {
                for (int j=y; j<yy; j++) {
                    if ( ((i+j)%2) == 0) {
                        g.fillRect(i, j, 1, 1);
                    }
                }
            }
            break;
        case 'b': // Horizontal tab
            drawTuplet(g, x, y, x2, y2, bi, "H", "T");
            break;
        case 'c': // Form Feed
            drawTuplet(g, x, y, x2, y2, bi, "F", "F");
            break;
        case 'd': // Carriage Return
            drawTuplet(g, x, y, x2, y2, bi, "C", "R");
            break;
        case 'e': // Line Feed
            drawTuplet(g, x, y, x2, y2, bi, "L", "F");
            break;
        case 'f': { // Degrees
            char[] ca = new char[1];
            ca[0] = (char)0x00b0;
            g.drawChars(ca, 0, 1, x, y + bi);
            break;
        }
        case 'g': { // Plus/Minus
            char[] ca = new char[1];
            ca[0] = (char)0x00b1;
            g.drawChars(ca, 0, 1, x, y + bi);
            break;
        }
        case 'h': // New line
            drawTuplet(g, x, y, x2, y2, bi, "N", "L");
            break;
        case 'i': // Vertical Tab
            drawTuplet(g, x, y, x2, y2, bi, "V", "T");
            break;
        case 'j': // Lower right corner
            g.drawLine(x2, y, x2, y2);
            g.drawLine(x2, y2, x, y2);
            break;
        case 'k': // Upper right corner
            g.drawLine(x, y2, x2, y2);
            g.drawLine(x2, y2, x2, yy);
            break;
        case 'l': // Upper left corner
            g.drawLine(x2, yy, x2, y2);
            g.drawLine(x2, y2, xx, y2);
            break;
        case 'm': // Lower left corner
            g.drawLine(x2, y, x2, y2);
            g.drawLine(x2, y2, xx, y2);
            break;
        case 'n': // Cross center lines
            g.drawLine(x2, y, x2, yy);
            g.drawLine(x, y2, xx, y2);
            break;
        case 'o': // Horizontal line (top)
            g.drawLine(x, y, xx, y);
            break;
        case 'p': // Horizontal line (top-half)
            g.drawLine(x, (y+y2)/2, xx, (y+y2)/2);
            break;
        case 'q': // Horizontal line (center)
            g.drawLine(x, y2, xx, y2);
            break;
        case 'r': // Horizontal line (bottom-half)
            g.drawLine(x, (yy+y2)/2, xx, (yy+y2)/2);
            break;
        case 's': // Horizontal line (bottom)
            g.drawLine(x, yy, xx, yy);
            break;
        case 't': // Left tee
            g.drawLine(x2, y, x2, yy);
            g.drawLine(x2, y2, xx, y2);
            break;
        case 'u': // Right tee
            g.drawLine(x2, y, x2, yy);
            g.drawLine(x, y2, x2, y2);
            break;
        case 'v': // Bottom tee
            g.drawLine(x, y2, xx, y2);
            g.drawLine(x2, y2, x2, y);
            break;
        case 'w': // Top tee
            g.drawLine(x, y2, xx, y2);
            g.drawLine(x2, y2, x2, yy);
            break;
        case 'x': // Vertical line
            g.drawLine(x2, y, x2, yy);
            break;
        case 'y': { // Less than or equal
            int dx = charWidth/5;
            int dy = charHeight/5;
            g.drawLine(x+dx, y2, xx-dx, y+2*dy);
            g.drawLine(x+dx, y2, xx-dx, yy-2*dy);
            g.drawLine(x+dx, y2+dy, xx-dx, yy-dy);
            break;
        }
        case 'z': { // Greater than or equal
            int dx = charWidth/5;
            int dy = charHeight/5;
            g.drawLine(xx-dx, y2, x+dx, y+2*dy);
            g.drawLine(xx-dx, y2, x+dx, yy-2*dy);
            g.drawLine(xx-dx, y2+dy, x+dx, yy-dy);
            break;
        }
        case '{': { // Pi
            char[] ca = new char[1];
            ca[0] = (char)0x03c0;
            g.drawChars(ca, 0, 1, x, y + bi);
            break;
        }
        case '|': { // Not equal
            char[] ca = new char[1];
            ca[0] = (char)0x2260;
            g.drawChars(ca, 0, 1, x, y + bi);
            break;
        }
        case '}': { // UK pound
            char[] ca = new char[1];
            ca[0] = (char)0x00a3;
            g.drawChars(ca, 0, 1, x, y + bi);
            break;
        }
        case '~': { // Center dot
            char[] ca = new char[1];
            ca[0] = (char)0x00b7;
            g.drawChars(ca, 0, 1, x, y + bi);
            break;
        }
        default:
            break;
        }
    }


    /* Use our own box drawing since certain fonts doesn't have
       these properly defined. We simplify a bit by only drawing
       'light' versions of the box glyphs.
    */

    public static boolean isBoxOrBlockChar(char c) {
        return c >= 0x2500 && c < 0x25A0;
    } 
    
    public static void drawBoxOrBlockChar(Graphics g, int x, int y, int bi,
                                          char c,
                                          int charWidth, int charHeight) {

        int x2 = (x + (charWidth  / 2));
        int y2 = (y + (charHeight / 2));
        int xx = (x + charWidth);
        int yy = (y + charHeight);
        
        // left middle       = 0x1
        // left middle fat   = 0x2
        // right middle      = 0x4
        // right middle fat  = 0x8
        // top middle        = 0x10
        // top middle fat    = 0x20
        // bottom middle     = 0x40
        // bottom middle fat = 0x80

        final int [] map = { 
            0x05, 0x0f, 0x50, 0xf0, // normal horizontal/vertical

            0x05, 0x0f, 0x50, 0xf0, // XXX: triple dash horizontal/vertical
            0x05, 0x0f, 0x50, 0xf0, // XXX: quad dash horizontal/vertical

            0x44, 0x4c, 0xc4, 0xcc, // upper left
            0x41, 0x43, 0xc1, 0xc3, // upper right

            0x14, 0x1c, 0x34, 0x3c, // lower left
            0x11, 0x13, 0x31, 0x33, // lower right

            0x54, 0x5c, 0x74, 0xd4, // left T
            0xf4, 0x7c, 0xdc, 0xfc, // left T

            0x51, 0x53, 0x71, 0xd1, // right T
            0xf1, 0x73, 0xd3, 0xf3, // right T

            0x45, 0x4b, 0x4d, 0x4f, // top T
            0xc5, 0xcb, 0xcd, 0xcf, // top T

            0x15, 0x1b, 0x1d, 0x1f, // bottom T
            0x35, 0x3b, 0x3d, 0x3f, // bottom T

            0x55, 0x57, 0x5d, 0x5f, // plus
            0x75, 0xd5, 0xf5, 0x77, // plus
            0x7d, 0xd7, 0xdd, 0x7f, // plus
            0xdf, 0xf7, 0xfd, 0xff, // plus

            0x05, 0x0f, 0x50, 0xf0, // XXX: double dash horizontal/vertical

            0x00, 0x00, 0x00, 0x00, // handled specifically below
            0x00, 0x00, 0x00, 0x00, // handled specifically below
            0x00, 0x00, 0x00, 0x00, // handled specifically below
            0x00, 0x00, 0x00, 0x00, // handled specifically below

            0x00, 0x00, 0x00, 0x00, // handled specifically below
            0x00, 0x00, 0x00, 0x00, // handled specifically below
            0x00, 0x00, 0x00, 0x00, // handled specifically below
            0x00, 0x00, 0x00, 0x00, // handled specifically below

            0x00, 0x00, 0x00, 0x00, // handled specifically below

            0x01, 0x10, 0x04, 0x40,
            0x03, 0x30, 0x0c, 0xc0,
            0x0d, 0xd0, 0x07, 0x70,
        };

        if (c <= 0x254f || (c >= 0x2574 && c <=0x257f)) {
            int b = map[c - 0x2500];
            if ((b & 0x01) != 0)
                g.drawLine(x, y2, x2, y2);
            if ((b & 0x02) != 0)
                g.drawLine(x, y2+1, x2, y2+1);
            if ((b & 0x04) != 0)
                g.drawLine(x2, y2, xx, y2);
            if ((b & 0x08) != 0)
                g.drawLine(x2, y2+1, xx, y2+1);
            if ((b & 0x10) != 0)
                g.drawLine(x2, y, x2, y2);
            if ((b & 0x20) != 0)
                g.drawLine(x2-1, y, x2-1, y2);
            if ((b & 0x40) != 0)
                g.drawLine(x2, y2, x2, yy);
            if ((b & 0x80) != 0)
                g.drawLine(x2-1, y2, x2-1, yy);
            return;
        }
        
        switch(c) {
            case 0x2550:
                g.drawLine(x, y2-1, xx, y2-1);
                g.drawLine(x, y2+1, xx, y2+1);
                break;

            case 0x2551:
                g.drawLine(x2-1, y, x2-1, yy);
                g.drawLine(x2+1, y, x2+1, yy);
                break;

            case 0x2552:
                g.drawLine(x2, y2-1, xx, y2-1);
                g.drawLine(x2, y2+1, xx, y2+1);
                g.drawLine(x2, y2-1, x2, yy);
                break;
                
            case 0x2553:
                g.drawLine(x2-1, y2, xx, y2);
                g.drawLine(x2-1, y2, x2-1, yy);
                g.drawLine(x2+1, y2, x2+1, yy);
                break;

            case 0x2554:
                g.drawLine(x2-1, y2-1, xx, y2-1);
                g.drawLine(x2+1, y2+1, xx, y2+1);
                g.drawLine(x2-1, y2-1, x2-1, yy);
                g.drawLine(x2+1, y2+1, x2+1, yy);
                break;
                
            case 0x2555:
                g.drawLine(x, y2-1, x2, y2-1);
                g.drawLine(x, y2+1, x2, y2+1);
                g.drawLine(x2, y2-1, x2, yy);
                break;

            case 0x2556:
                g.drawLine(x, y2, x2+1, y2);
                g.drawLine(x2-1, y2, x2-1, yy);
                g.drawLine(x2+1, y2, x2+1, yy);
                break;
                
            case 0x2557:
                g.drawLine(x, y2-1, x2+1, y2-1);
                g.drawLine(x, y2+1, x2-1, y2+1);
                g.drawLine(x2+1, y2-1, x2+1, yy);
                g.drawLine(x2-1, y2+1, x2-1, yy);                
                break;
                
            case 0x2558:
                g.drawLine(x2, y, x2, y2+1);
                g.drawLine(x2, y2-1, xx, y2-1);
                g.drawLine(x2, y2+1, xx, y2+1);
                break;

            case 0x2559:
                g.drawLine(x2-1, y, x2-1, y2);
                g.drawLine(x2+1, y, x2+1, y2);
                g.drawLine(x2-1, y2, xx, y2);
                break;

            case 0x255a:
                g.drawLine(x2-1, y, x2-1, y2+1);
                g.drawLine(x2+1, y, x2+1, y2-1);
                g.drawLine(x2-1, y2+1, xx, y2+1);
                g.drawLine(x2+1, y2-1, xx, y2-1);
                break;
                
            case 0x255b:
                g.drawLine(x2, y, x2, y2+1);
                g.drawLine(x, y2-1, x2, y2-1);
                g.drawLine(x, y2+1, x2, y2+1);
                break;
                
           case 0x255c:
                g.drawLine(x2-1, y, x2-1, y2);
                g.drawLine(x2+1, y, x2+1, y2);
                g.drawLine(x, y2, x2+1, y2);
                break;

            case 0x255d:
                g.drawLine(x2-1, y, x2-1, y2-1);
                g.drawLine(x2+1, y, x2+1, y2+1);
                g.drawLine(x2-1, y2-1, x, y2-1);
                g.drawLine(x2+1, y2+1, x, y2+1);
                break;

            case 0x255e:
                g.drawLine(x2, y, x2, yy);
                g.drawLine(x2, y2-1, xx, y2-1);
                g.drawLine(x2, y2+1, xx, y2+1);
                break;

            case 0x255f:
                g.drawLine(x2+1, y2, xx, y2);
                g.drawLine(x2-1, y, x2-1, yy);
                g.drawLine(x2+1, y, x2+1, yy);
                break;

            case 0x2560:
                g.drawLine(x2-1, y, x2-1, yy);
                g.drawLine(x2+1, y, x2+1, y2-1);
                g.drawLine(x2+1, y2-1, xx, y2-1);
                g.drawLine(x2+1, y2+1, x2+1, yy);
                g.drawLine(x2+1, y2+1, xx, y2+1);
                break;

            case 0x2561:
                g.drawLine(x2, y, x2, yy);
                g.drawLine(x, y2-1, x2, y2-1);
                g.drawLine(x, y2+1, x2, y2+1);
                break;

            case 0x2562:
                g.drawLine(x2+1, y, x2+1, yy);
                g.drawLine(x2-1, y, x2-1, yy);
                g.drawLine(x, y2, x2-1, y2);
                break;

            case 0x2563:
                g.drawLine(x2+1, y, x2+1, yy);
                g.drawLine(x2-1, y, x2-1, y2-1);
                g.drawLine(x, y2-1, x2-1, y2-1);
                g.drawLine(x, y2+1, x2-1, y2+1);
                g.drawLine(x2-1, y2+1, x2-1, yy);
                break;

            case 0x2564:
                g.drawLine(x, y2-1, xx, y2-1);
                g.drawLine(x, y2+1, xx, y2+1);
                g.drawLine(x2, y2+1, x2, yy);
                break;
                
            case 0x2565:
                g.drawLine(x, y2, xx, y2);
                g.drawLine(x2-1, y2, x2-1, yy);
                g.drawLine(x2+1, y2, x2+1, yy);
                break;

            case 0x2566:
                g.drawLine(x, y2-1, xx, y2-1);
                g.drawLine(x, y2+1, x2-1, y2+1);
                g.drawLine(x2+1, y2+1, xx, y2+1);
                g.drawLine(x2-1, y2+1, x2-1, yy);
                g.drawLine(x2+1, y2+1, x2+1, yy);
                break;

            case 0x2567:
                g.drawLine(x, y2+1, xx, y2+1);
                g.drawLine(x, y2-1, xx, y2-1);
                g.drawLine(x2, y, x2, y2-1);
                break;

            case 0x2568:
                g.drawLine(x, y2, xx, y2);
                g.drawLine(x2-1, y, x2-1, y2);
                g.drawLine(x2+1, y, x2+1, y2);
                break;

            case 0x2569:
                g.drawLine(x, y2+1, xx, y2+1);
                g.drawLine(x, y2-1, x2-1, y2-1);
                g.drawLine(x2+1, y2-1, xx, y2-1);
                g.drawLine(x2-1, y, x2-1, y2-1);
                g.drawLine(x2+1, y, x2+1, y2-1);
                break;

            case 0x256a:
                g.drawLine(x2, y, x2, yy);
                g.drawLine(x, y2-1, xx, y2-1);
                g.drawLine(x, y2+1, xx, y2+1);
                break;

            case 0x256b:
                g.drawLine(x, y2, xx, y2);
                g.drawLine(x2-1, y, x2-1, yy);
                g.drawLine(x2+1, y, x2+1, yy);
                break;

            case 0x256c:
                g.drawLine(x, y2-1, x2-1, y2-1);
                g.drawLine(x2-1, y2-1, x2-1, y);
                g.drawLine(x2+1, y, x2+1, y2-1);
                g.drawLine(x2+1, y2-1, xx, y2-1);
                g.drawLine(x, y2+1, x2-1, y2+1);
                g.drawLine(x2-1, y2+1, x2-1, yy);
                g.drawLine(x2+1, yy, x2+1, y2+1);
                g.drawLine(x2+1, y2+1, xx, y2+1);
                break;

            case 0x256d:
                g.drawArc(x2, y2, charWidth, charHeight, 90, 90);
                break;
            case 0x256e:
                g.drawArc(x-charWidth/2, y2, charWidth, charHeight, 0, 90);
                break;
            case 0x256f:
                g.drawArc(x-charWidth/2, y-charHeight/2, charWidth, charHeight, 0, -90);
                break;
            case 0x2570:
                g.drawArc(x2, y-charHeight/2, charWidth, charHeight, 180, 90);
                break;

            case 0x2571:
                g.drawLine(x, yy, x2, y);
                break;

            case 0x2572:
                g.drawLine(x, y, x2, yy);
                break;

            case 0x2573:
                g.drawLine(x, yy, x2, y);
                g.drawLine(x, y, x2, yy);
                break;

            case 0x2580:
                g.fillRect(x, y, charWidth, charHeight/2);
                break;

            case 0x2581:
                g.fillRect(x, y+charHeight*7/8, charWidth, charHeight-charHeight*7/8);
                break;

            case 0x2582:
                g.fillRect(x, y+charHeight*3/4, charWidth, charHeight-charHeight*3/4);
                break;

            case 0x2583:
                g.fillRect(x, y+charHeight*5/8, charWidth, charHeight-charHeight*5/8);
                break;

            case 0x2584:
                g.fillRect(x, y2, charWidth, charHeight/2);
                break;

            case 0x2585:
                g.fillRect(x, y+charHeight*3/8, charWidth, charHeight-charHeight*3/8);
                break;

            case 0x2586:
                g.fillRect(x, y+charHeight/4, charWidth, charHeight-charHeight/4);
                break;

            case 0x2587:
                g.fillRect(x, y+charHeight/8, charWidth, charHeight-charHeight/8);
                break;

            case 0x2588:
                g.fillRect(x, y, charWidth, charHeight);
                break;

            case 0x2589:
                g.fillRect(x, y, charWidth*7/8, charHeight);
                break;

            case 0x258a:
                g.fillRect(x, y, charWidth*3/4, charHeight);
                break;

            case 0x258b:
                g.fillRect(x, y, charWidth*5/8, charHeight);
                break;

            case 0x258c:
                g.fillRect(x, y, charWidth/2, charHeight);
                break;

            case 0x258d:
                g.fillRect(x, y, charWidth*3/8, charHeight);
                break;

            case 0x258e:
                g.fillRect(x, y, charWidth/4, charHeight);
                break;

            case 0x258f:
                g.fillRect(x, y, charWidth/8, charHeight);
                break;

            case 0x2590:
                g.fillRect(x2, y, charWidth-charWidth/2, charHeight);
                break;
                
            case 0x2591:
            case 0x2592:
            case 0x2593:
                drawLineDrawChar(g, x, y, bi, 'a', charWidth, charHeight);
                break;
                
            case 0x2594:
                g.fillRect(x, y, charWidth, charHeight/8);
                break;
 
            case 0x2595:
                g.fillRect(xx-charWidth/8, y, charWidth/8, charHeight);
                break;
                
            case 0x2596:
                g.fillRect(x, y2, charWidth/2, yy-y2);
                break;
                
            case 0x2597:
                g.fillRect(x2, y2, xx-x2, yy-y2);
                break;
                
            case 0x2599:
                g.fillRect(x, y2, charWidth/2, yy-y2);
            case 0x259a:
                g.fillRect(x2, y2, xx-x2, yy-y2);
            case 0x2598:
                g.fillRect(x, y, charWidth/2, charHeight/2);
                break;
                
            case 0x259b:
                g.fillRect(x, y, charWidth, y2-y);
                g.fillRect(x, y2, x2-x, yy-y2);
                break;
                
            case 0x259c:
                g.fillRect(x, y, charWidth, y2-y);
                g.fillRect(x2, y2, xx-x2, yy-y2);
                break;

            case 0x259f:
                g.fillRect(x2, y2, xx-x2, yy-y2);
            case 0x259e:
                g.fillRect(x, y2, charWidth/2, yy-y2);
            case 0x259d:
                g.fillRect(x2, y, xx-x2, y2-y);
                break;

            default:
                break;
        }
    }
}
