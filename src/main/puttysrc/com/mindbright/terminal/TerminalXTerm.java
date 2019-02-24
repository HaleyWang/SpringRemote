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

/*
 * Author's comment: The contents of this file is heavily based upon
 * xterm from the X Consortium, original copyright notices included
 * below.
 */
/*
Copyright (c) 1988  X Consortium
 
Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:
 
The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.
 
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
X CONSORTIUM BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 
Except as contained in this notice, the name of the X Consortium shall not be
used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization from the X Consortium.
 
*/
/*
 * Copyright 1987 by Digital Equipment Corporation, Maynard, Massachusetts.
 *
 *                         All Rights Reserved
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose and without fee is hereby granted,
 * provided that the above copyright notice appear in all copies and that
 * both that copyright notice and this permission notice appear in
 * supporting documentation, and that the name of Digital Equipment
 * Corporation not be used in advertising or publicity pertaining to
 * distribution of the software without specific, written prior permission.
 *
 *
 * DIGITAL DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE, INCLUDING
 * ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS, IN NO EVENT SHALL
 * DIGITAL BE LIABLE FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR
 * ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,
 * WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION,
 * ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS
 * SOFTWARE.
 */
package com.mindbright.terminal;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.NoSuchElementException;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;


public final class TerminalXTerm extends TerminalInterpreter {

    // !!! Set to true for extensive debug
    //
    public final static boolean DEBUG        = false;
    public final static boolean DEBUGNOTIMPL = false;
    public final static boolean DEBUGSTATE   = false;
    public final static boolean DEBUGPRINT   = false;

    public final static int CASE_GROUND_STATE  = 0;
    public final static int CASE_IGNORE_STATE  = 1;
    public final static int CASE_IGNORE_ESC    = 2;
    public final static int CASE_IGNORE        = 3;
    public final static int CASE_BELL          = 4;
    public final static int CASE_BS            = 5;
    public final static int CASE_CR            = 6;
    public final static int CASE_ESC           = 7;
    public final static int CASE_VMOT          = 8;
    public final static int CASE_TAB           = 9;
    public final static int CASE_SI            = 10;
    public final static int CASE_SO            = 11;
    public final static int CASE_SCR_STATE     = 12;
    public final static int CASE_SCS0_STATE    = 13;
    public final static int CASE_SCS1_STATE    = 14;
    public final static int CASE_SCS2_STATE    = 15;
    public final static int CASE_SCS3_STATE    = 16;
    public final static int CASE_ESC_IGNORE    = 17;
    public final static int CASE_ESC_DIGIT     = 18;
    public final static int CASE_ESC_SEMI      = 19;
    public final static int CASE_DEC_STATE     = 20;
    public final static int CASE_ICH           = 21;
    public final static int CASE_CUU           = 22;
    public final static int CASE_CUD           = 23;
    public final static int CASE_CUF           = 24;
    public final static int CASE_CUB           = 25;
    public final static int CASE_CUP           = 26;
    public final static int CASE_ED            = 27;
    public final static int CASE_EL            = 28;
    public final static int CASE_IL            = 29;
    public final static int CASE_DL            = 30;
    public final static int CASE_DCH           = 31;
    public final static int CASE_DA1           = 32;
    public final static int CASE_TRACK_MOUSE   = 33;
    public final static int CASE_TBC           = 34;
    public final static int CASE_SET           = 35;
    public final static int CASE_RST           = 36;
    public final static int CASE_SGR           = 37;
    public final static int CASE_CPR           = 38;
    public final static int CASE_DECSTBM       = 39;
    public final static int CASE_DECREQTPARM   = 40;
    public final static int CASE_DECSET        = 41;
    public final static int CASE_DECRST        = 42;
    public final static int CASE_DECALN        = 43;
    public final static int CASE_GSETS         = 44;
    public final static int CASE_DECSC         = 45;
    public final static int CASE_DECRC         = 46;
    public final static int CASE_DECKPAM       = 47;
    public final static int CASE_DECKPNM       = 48;
    public final static int CASE_IND           = 49;
    public final static int CASE_NEL           = 50;
    public final static int CASE_HTS           = 51;
    public final static int CASE_RI            = 52;
    public final static int CASE_SS2           = 53;
    public final static int CASE_SS3           = 54;
    public final static int CASE_CSI_STATE     = 55;
    public final static int CASE_OSC           = 56;
    public final static int CASE_RIS           = 57;
    public final static int CASE_LS2           = 58;
    public final static int CASE_LS3           = 59;
    public final static int CASE_LS3R          = 60;
    public final static int CASE_LS2R          = 61;
    public final static int CASE_LS1R          = 62;
    public final static int CASE_PRINT         = 63;
    public final static int CASE_XTERM_SAVE    = 64;
    public final static int CASE_XTERM_RESTORE = 65;
    public final static int CASE_XTERM_TITLE   = 66;
    public final static int CASE_DECID         = 67;
    public final static int CASE_HP_MEM_LOCK   = 68;
    public final static int CASE_HP_MEM_UNLOCK = 69;
    public final static int CASE_HP_BUGGY_LL   = 70;
    public final static int CASE_SEQ_CAPTURE   = 71;
    public final static int CASE_ESC_SEMIOSC   = 72;
    public final static int CASE_XTERM_SEQ     = 73;
    public final static int CASE_ENQ           = 74;
    public final static int CASE_XTERMWIN      = 75;
    public final static int CASE_CNL           = 76;
    public final static int CASE_CPL           = 77;
    public final static int CASE_CHA           = 78;
    public final static int CASE_CHT           = 79;
    public final static int CASE_SU            = 80;
    public final static int CASE_SD            = 81;
    public final static int CASE_ECH           = 82;
    public final static int CASE_CBT           = 83;
    public final static int CASE_HPA           = 84;
    public final static int CASE_REP           = 85;
    public final static int CASE_VPA           = 86;
    public final static int CASE_ANSI_PRINTER  = 87;
    //RSC_CODE
    public final static int CASE_VMOT2	       = 88;
    public final static int CASE_CR2	       = 89;

    public final static int[] asciiLineDrawChars = {
                ' ',  // 0x5f - _
                '+',  // 0x60 - `
                ':',  // 0x61 - a
                ' ',  // 0x62 - b
                ' ',  // 0x63 - c
                ' ',  // 0x64 - d
                ' ',  // 0x65 - e
                '\\', // 0x66 - f
                '#',  // 0x67 - g
                '#',  // 0x68 - h
                '#',  // 0x69 - i
                '+',  // 0x6a - j
                '+',  // 0x6b - k
                '+',  // 0x6c - l
                '+',  // 0x6d - m
                '+',  // 0x6e - n
                '~',  // 0x6f - o
                '-',  // 0x70 - p
                '-',  // 0x71 - q
                '-',  // 0x72 - r
                '_',  // 0x73 - s
                '+',  // 0x74 - t
                '+',  // 0x75 - u
                '+',  // 0x76 - v
                '+',  // 0x77 - w
                '|',  // 0x78 - x
                '<',  // 0x79 - y
                '>',  // 0x7a - z
                '*',  // 0x7b - {
                '!',  // 0x7c - |
                'f',  // 0x7d - }
                'o',  // 0x7e - ~

                '>',  // 0x2b - +
                '<',  // 0x2c - ,
                '^',  // 0x2d - -
                'v',  // 0x2e - .
            };

    public final static int XVK_UP        = 0;
    public final static int XVK_DOWN      = 1;
    public final static int XVK_RIGHT     = 2;
    public final static int XVK_LEFT      = 3;
    public final static int XVK_PAGE_UP   = 4;
    public final static int XVK_PAGE_DOWN = 5;
    public final static int XVK_END       = 6;
    public final static int XVK_HOME      = 7;
    public final static int XVK_INSERT    = 8;
    public final static int XVK_F1        = 9;
    public final static int XVK_F2        = 10;
    public final static int XVK_F3        = 11;
    public final static int XVK_F4        = 12;
    public final static int XVK_F5        = 13;
    public final static int XVK_F6        = 14;
    public final static int XVK_F7        = 15;
    public final static int XVK_F8        = 16;
    public final static int XVK_F9        = 17;
    public final static int XVK_F10       = 18;
    public final static int XVK_F11       = 19;
    public final static int XVK_F12       = 20;
    public final static int XVK_NUMPAD0   = 21;
    public final static int XVK_NUMPAD1   = 22;
    public final static int XVK_NUMPAD2   = 23;
    public final static int XVK_NUMPAD3   = 24;
    public final static int XVK_NUMPAD4   = 25;
    public final static int XVK_NUMPAD5   = 26;
    public final static int XVK_NUMPAD6   = 27;
    public final static int XVK_NUMPAD7   = 28;
    public final static int XVK_NUMPAD8   = 29;
    public final static int XVK_NUMPAD9   = 30;
    public final static int XVK_MULTIPLY  = 31;
    public final static int XVK_ADD       = 32;
    public final static int XVK_SUBTRACT  = 33;
    public final static int XVK_DIVIDE    = 34;
    public final static int XVK_MAX       = 35;

    public final int[] vk2xvk = {
                                    KeyEvent.VK_UP,
                                    KeyEvent.VK_DOWN,
                                    KeyEvent.VK_RIGHT,
                                    KeyEvent.VK_LEFT,
                                    KeyEvent.VK_PAGE_UP,
                                    KeyEvent.VK_PAGE_DOWN,
                                    KeyEvent.VK_END,
                                    KeyEvent.VK_HOME,
                                    KeyEvent.VK_INSERT,
                                    KeyEvent.VK_F1,
                                    KeyEvent.VK_F2,
                                    KeyEvent.VK_F3,
                                    KeyEvent.VK_F4,
                                    KeyEvent.VK_F5,
                                    KeyEvent.VK_F6,
                                    KeyEvent.VK_F7,
                                    KeyEvent.VK_F8,
                                    KeyEvent.VK_F9,
                                    KeyEvent.VK_F10,
                                    KeyEvent.VK_F11,
                                    KeyEvent.VK_F12,
                                    KeyEvent.VK_NUMPAD0,
                                    KeyEvent.VK_NUMPAD1,
                                    KeyEvent.VK_NUMPAD2,
                                    KeyEvent.VK_NUMPAD3,
                                    KeyEvent.VK_NUMPAD4,
                                    KeyEvent.VK_NUMPAD5,
                                    KeyEvent.VK_NUMPAD6,
                                    KeyEvent.VK_NUMPAD7,
                                    KeyEvent.VK_NUMPAD8,
                                    KeyEvent.VK_NUMPAD9,
                                    KeyEvent.VK_MULTIPLY,
                                    KeyEvent.VK_ADD,
                                    KeyEvent.VK_SUBTRACT,
                                    KeyEvent.VK_DIVIDE,
                                };

    public final static int EMUL_XTERM    = 0;
    public final static int EMUL_LINUX    = 1;
    public final static int EMUL_SCOANSI  = 2;
    public final static int EMUL_ATT6386  = 3;
    public final static int EMUL_SUN      = 4;
    public final static int EMUL_AIX      = 5;
    public final static int EMUL_VT220    = 6;
    public final static int EMUL_VT100    = 7;
    public final static int EMUL_ANSI     = 8;
    public final static int EMUL_VT52     = 9;

    public final static int EMUL_ALTERNATENAME = 10;
    public final static int EMUL_XTERMCOL      = EMUL_ALTERNATENAME + EMUL_XTERM;
    public final static int EMUL_LINUXLAT      = EMUL_ALTERNATENAME + EMUL_LINUX;
    public final static int EMUL_AT386         = EMUL_ALTERNATENAME + EMUL_ATT6386;
    public final static int EMUL_VT102         = EMUL_ALTERNATENAME + EMUL_VT100;
    public final static int EMUL_VT320         = EMUL_ALTERNATENAME + EMUL_VT220;

    public final static boolean hasNullPadding(int personality) {
        if(personality == EMUL_VT220 ||
                personality == EMUL_VT100 ||
                personality == EMUL_ANSI ||
                personality == EMUL_VT52)
            return true;
        return false;
    }

    public final static int DEFAULT_TERM = EMUL_XTERM;

    public final static String[] terminalTypes = {
        "xterm", "linux", "scoansi",  "att6386", "sun", "aixterm",
        "vt220", "vt100", "ansi",  "vt52",
        "xterm-color", "linux-lat", "", "at386", "", "", "vt320", "vt102"
    };

    int whoAmI;
    int whoAmIReally;
    boolean dumbMode = false;

    public final static String[][] specialKeyMap = {
                //xterm   linux   scoansi att6386 sun     aixterm vt220   vt100   ansi    vt52
                { "A",    "A",    "A",    "A",    "A",    "A",    "A",    "A",    "A",    "A"  },
                { "B",    "B",    "B",    "B",    "B",    "B",    "B",    "B",    "B",    "B"  },
                { "C",    "C",    "C",    "C",    "C",    "C",    "C",    "C",    "C",    "C"  },
                { "D",    "D",    "D",    "D",    "D",    "D",    "D",    "D",    "D",    "D"  },

                { "5~",   "5~",   "I",    "V",    "216z", "150q", "5~",   "5~",   "5~",   "5~" },
                { "6~",   "6~",   "G",    "U",    "222z", "154q", "6~",   "6~",   "6~",   "6~" },
                { "F",    "4~",   "F",    "Y",    "220z", "146q", "4~",   "4~",   "4~",   "4~" },
                { "H",    "1~",   "H",    "H",    "214z", "H",    "1~",   "1~",   "1~",   "1~" },
                { "2~",   "2~",   "L",    "@",    "2~",   "139q", "2~",   "2~",   "L",    "L"  },

                { "11~",  "[A",   "M",    "P",    "224z", "001q", "P",    "P",    "P",    "P"  },
                { "12~",  "[B",   "N",    "Q",    "225z", "002q", "Q",    "Q",    "Q",    "Q"  },
                { "13~",  "[C",   "O",    "R",    "226z", "003q", "R",    "R",    "R",    "R"  },
                { "14~",  "[D",   "P",    "S",    "227z", "004q", "S",    "S",    "S",    "S"  },
                { "15~",  "[E",   "Q",    "T",    "228z", "005q", "15~",  null,   null,   null },
                { "17~",  "17~",  "R",    "U",    "229z", "006q", "17~",  null,   null,   null },
                { "18~",  "18~",  "S",    "V",    "230z", "007q", "18~",  null,   null,   null },
                { "19~",  "19~",  "T",    "W",    "231z", "008q", "19~",  null,   null,   null },
                { "20~",  "20~",  "U",    "X",    "232z", "009q", "20~",  null,   null,   null },
                { "21~",  "21~",  "V",    "Y",    "233z", "010q", "21~",  null,   null,   null },
                { "23~",  "23~",  "W",    "Z",    "234z", "011q", "23~",  null,   null,   null },
                { "24~",  "24~",  "X",    "A",    "235z", "012q", "24~",  null,   null,   null },

                // !!! NUMPAD missing, this is not trivial given java's messy so called virtual keys... :-(
            };

    public final static String[][] specialKeyMapShift = {
                //xterm   linux   scoansi att6386 sun     aixterm vt220   vt100   ansi    vt52
                { "A",    "A",    "A",    "A",    "A",    "A",    "A",    "A",    "A",    "A"  },
                { "B",    "B",    "B",    "B",    "B",    "B",    "B",    "B",    "B",    "B"  },
                { "C",    "C",    "C",    "C",    "C",    "C",    "C",    "C",    "C",    "C"  },
                { "D",    "D",    "D",    "D",    "D",    "D",    "D",    "D",    "D",    "D"  },

                { "5~",   "5~",   "I",    "V",    "216z", "150q", "5~",   "5~",   "5~",   "5~" },
                { "6~",   "6~",   "G",    "U",    "222z", "154q", "6~",   "6~",   "6~",   "6~" },
                { "4~",   "4~",   "F",    "Y",    "220z", "146q", "4~",   "4~",   "4~",   "4~" },
                { "@",    "1~",   "H",    "H",    "214z", "H",    "1~",   "1~",   "1~",   "1~" },
                { "2~",   "2~",   "L",    "@",    "2~",   "139q", "2~",   "2~",   "L",    "L"  },

                { "23~",  "23~",  "Y",    "P",    "224z", "013q", "23~",  "P",    "P",    "P"  },
                { "24~",  "24~",  "Z",    "Q",    "225z", "014q", "24~",  "Q",    "Q",    "Q"  },
                { "25~",  "25~",  "a",    "R",    "226z", "015q", "25~",  "R",    "R",    "R"  },
                { "26~",  "26~",  "b",    "S",    "227z", "016q", "26~",  "S",    "S",    "S"  },
                { "28~",  "28~",  "c",    "T",    "228z", "017q", "28~",  null,   null,   null },
                { "29~",  "29~",  "d",    "U",    "229z", "018q", "29~",  null,   null,   null },
                { "31~",  "31~",  "e",    "V",    "230z", "019q", "31~",  null,   null,   null },
                { "32~",  "32~",  "f",    "W",    "231z", "020q", "32~",  null,   null,   null },
                { "33~",  "33~",  "g",    "X",    "232z", "021q", "33~",  null,   null,   null },
                { "34~",  "34~",  "h",    "Y",    "233z", "022q", "34~",  null,   null,   null },
                { "23$",  null,   "i",    "Z",    "234z", "023q", null,   null,   null,   null },
                { "24$",  null,   "j",    "A",    "235z", "024q", null,   null,   null,   null },

                // !!! NUMPAD missing, this is not trivial given java's messy so called virtual keys... :-(
            };

    public final static String[][] specialKeyMapCtrl = {
                //xterm   linux   scoansi att6386 sun     aixterm vt220   vt100   ansi    vt52
                { "A",    "A",    "A",    "A",    "A",    "A",    "A",    "A",    "A",    "A"  },
                { "B",    "B",    "B",    "B",    "B",    "B",    "B",    "B",    "B",    "B"  },
                { "C",    "C",    "C",    "C",    "C",    "C",    "C",    "C",    "C",    "C"  },
                { "D",    "D",    "D",    "D",    "D",    "D",    "D",    "D",    "D",    "D"  },

                { "5~",   "5~",   "I",    "V",    "216z", "150q", "5~",   "5~",   "5~",   "5~" },
                { "6~",   "6~",   "G",    "U",    "222z", "154q", "6~",   "6~",   "6~",   "6~" },
                { "4~",   "4~",   "F",    "Y",    "220z", "146q", "4~",   "4~",   "4~",   "4~" },
                { "@",    "1~",   "H",    "H",    "214z", "H",    "1~",   "1~",   "1~",   "1~" },
                { "2~",   "2~",   "L",    "@",    "2~",   "139q", "2~",   "2~",   "L",    "L"  },

                { "11^",  null,   "k",    "P",    "224z", "025q", "P",    "P",    "P",    "P"  },
                { "12^",  null,   "l",    "Q",    "225z", "026q", "Q",    "Q",    "Q",    "Q"  },
                { "13^",  null,   "m",    "R",    "226z", "027q", "R",    "R",    "R",    "R"  },
                { "14^",  null,   "n",    "S",    "227z", "028q", "S",    "S",    "S",    "S"  },
                { "15^",  null,   "o",    "T",    "228z", "029q", null,   null,   null,   null },
                { "17^",  null,   "p",    "U",    "229z", "030q", null,   null,   null,   null },
                { "18^",  null,   "q",    "V",    "230z", "031q", null,   null,   null,   null },
                { "19^",  null,   "r",    "W",    "231z", "032q", null,   null,   null,   null },
                { "20^",  null,   "s",    "X",    "232z", "033q", null,   null,   null,   null },
                { "21^",  null,   "t",    "Y",    "233z", "034q", null,   null,   null,   null },
                { "23^",  null,   "u",    "Z",    "234z", "035q", null,   null,   null,   null },
                { "24^",  null,   "v",    "A",    "235z", "036q", null,   null,   null,   null },

                // !!! NUMPAD missing, this is not trivial given java's messy so called virtual keys... :-(
            };

    public final static String[][] specialKeyMapCtrlShift = {
                //xterm   linux   scoansi att6386 sun     aixterm vt220   vt100   ansi    vt52
                { "A",    "A",    "A",    "A",    "A",    "A",    "A",    "A",    "A",    "A"  },
                { "B",    "B",    "B",    "B",    "B",    "B",    "B",    "B",    "B",    "B"  },
                { "C",    "C",    "C",    "C",    "C",    "C",    "C",    "C",    "C",    "C"  },
                { "D",    "D",    "D",    "D",    "D",    "D",    "D",    "D",    "D",    "D"  },

                { "5~",   "5~",   "I",    "V",    "216z", "150q", "5~",   "5~",   "5~",   "5~" },
                { "6~",   "6~",   "G",    "U",    "222z", "154q", "6~",   "6~",   "6~",   "6~" },
                { "4~",   "4~",   "F",    "Y",    "220z", "146q", "4~",   "4~",   "4~",   "4~" },
                { "@",    "1~",   "H",    "H",    "214z", "H",    "1~",   "1~",   "1~",   "1~" },
                { "2~",   "2~",   "L",    "@",    "2~",   "139q", "2~",   "2~",   "L",    "L"  },

                { "23^",  null,   "w",    "P",    "224z", "001q", "P",    "P",    "P",    "P"  },
                { "24^",  null,   "x",    "Q",    "225z", "002q", "Q",    "Q",    "Q",    "Q"  },
                { "25^",  null,   "y",    "R",    "226z", "003q", "R",    "R",    "R",    "R"  },
                { "26^",  null,   "z",    "S",    "227z", "004q", "S",    "S",    "S",    "S"  },
                { "28^",  null,   "@",    "T",    "228z", "005q", null,   null,   null,   null },
                { "29^",  null,   "[",    "U",    "229z", "006q", null,   null,   null,   null },
                { "31^",  null,   "\\",   "V",    "230z", "007q", null,   null,   null,   null },
                { "32^",  null,   "]",    "W",    "231z", "008q", null,   null,   null,   null },
                { "33^",  null,   "^",    "X",    "232z", "009q", null,   null,   null,   null },
                { "34^",  null,   "_",    "Y",    "233z", "010q", null,   null,   null,   null },
                { "23@",  null,   "`",    "Z",    "234z", "011q", null,   null,   null,   null },
                { "24@",  null,   "{",    "A",    "235z", "012q", null,   null,   null,   null },

                // !!! NUMPAD missing, this is not trivial given java's messy so called virtual keys... :-(
            };

    public final static String[][][] theSpecialKeyMaps = {
                specialKeyMap,
                specialKeyMapShift,
                specialKeyMapCtrl,
                specialKeyMapCtrlShift
            };

    public final static int R_ESC = 0;
    public final static int R_SS2 = 1;
    public final static int R_SS3 = 2;
    public final static int R_DCS = 3;
    public final static int R_CSI = 4;
    public final static int R_OSC = 5;
    public final static int R_PM  = 6;
    public final static int R_APC = 7;

    public final static String[] replyTypes = {
                "\033",
                "\033N",
                "\033O",
                "\033P",
                "\033[",
                "\033]",
                "\033^",
                "\033_",
            };

    public final static char CHARSET_UK        = 'A';
    public final static char CHARSET_ASCII     = 'B';
    public final static char CHARSET_LINES     = '0';
    public final static char CHARSET_ASCII_ALT = '1';
    public final static char CHARSET_ASCII_ALT2= '2';

    char[] gSets = new char[4];
    int    scsType;
    int    curGL;
    int    curGR;
    //int    curSS;

    int    curGLDECSC;
    char[] gSetsDECSC = new char[4];

    String xtermSeq = null;
    String reply = null;

    public final static int PARAMNOTUSED = -1;

    int[] parseState;

    boolean windowRelative;
    //boolean keypadAppl;

    boolean cursorKeysMode;

    public final static int MOUSE_DONTSEND = 0;
    public final static int MOUSE_X10COMP  = 1;
    public final static int MOUSE_DECVT200 = 2;
    public final static int MOUSE_HLTRACK  = 3;
    int sendMousePos;

    int[] param  = new int[10];
    int   nparam ;

    public TerminalXTerm() {
        this(DEFAULT_TERM);
    }

    public TerminalXTerm(int personality) {
        try {
            setTerminalType(personality);
        } catch (NoSuchElementException e) {
            try {
                setTerminalType(DEFAULT_TERM);
            } catch (NoSuchElementException ee) {
                // !!! Can't happen...
            }
        }
    }

    public static String listAvailableTerminalTypes() {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < terminalTypes.length; i++)
            sb.append(terminalTypes[i]).append(" ");
        return sb.toString();
    }

    public static String[] getTerminalTypes() {
        int i, n = 0;
        for(i = 0; i < terminalTypes.length; i++)
            if(!terminalTypes[i].equals(""))
                n++;

        String[] types = new String[n];

        n = 0;
        for(i = 0; i < terminalTypes.length; i++)
            if(!terminalTypes[i].equals(""))
                types[n++] = terminalTypes[i];

        return types;
    }

    public String terminalType() {
        return terminalTypes[whoAmI];
    }

    public void setTerminalType(int type) throws NoSuchElementException {
        if(type < terminalTypes.length && type > -1) {
            whoAmI = type;
            whoAmIReally = type;
            if(whoAmI >= EMUL_ALTERNATENAME)
                whoAmIReally -= EMUL_ALTERNATENAME;
            vtReset();
        } else {
            throw new NoSuchElementException(type + " is not a supported terminal-emulation");
        }
    }

    public void setTerminalType(String type) throws NoSuchElementException {
        int i;
        for(i = 0; i < terminalTypes.length; i++)
            if(terminalTypes[i].equalsIgnoreCase(type))
                break;
        setTerminalType(i);
    }

    public void setDumbMode(boolean dumb) {
        dumbMode = dumb;
    }

    final int mapLineDrawToAscii(char c) {
        if(c >= (char)0x5f && c <= (char)0x7e)
            c = (char)asciiLineDrawChars[c - 0x5f];
        else if(c >= (char)0x2b && c <= (char)0x2e) {
            c = (char)asciiLineDrawChars[(c - 0x2b) + 0x20];
        } else if (c == 0x20) {
            // !! <space>
        } else {
            if(DEBUGNOTIMPL)
                notImplemented("ASCII line-draw-char: " + c + " (" + ((int)c) + ")");
        }
        return c;
    }

    final char mapLineDrawToLinux(char c) {
        switch(c) {
        case ' ':
            c = ' ';
            break;
        case '\004':
            c = '`';
            break;
        case '\261':
            c = 'a';
            break;
        case '\370':
            c = 'f';
            break;
        case '\361':
            c = 'g';
            break;
        case '\260':
            c = 'h';
            break;
        case '\331':
        case '\211':
            c = 'j';
            break;
        case '\277':
        case '\214':
            c = 'k';
            break;
        case '\332':
        case '\206':
            c = 'l';
            break;
        case '\300':
        case '\203':
            c = 'm';
            break;
        case '\305':
            c = 'n';
            break;
        case '\304':
        case '\212':
            c = 'q';
            break;
        case '\362':
            c = 'r';
            break;
        case '\303':
        case '\207':
            c = 't';
            break;
        case '\264':
        case '\215':
            c = 'u';
            break;
        case '\301':
            c = 'v';
            break;
        case '\302':
            c = 'w';
            break;
        case '\263':
        case '\205':
            c = 'x';
            break;
        case '\363':
        case '\371':
            c = 'y';
            break;
        case '\372':
            c = 'z';
            break;
        case '\343':
        case '\373':
            c = '{';
            break;
        case '\374':
        case '\330':
            c = '|';
            break;
        case '\375':
        case '\234':
            c = '}';
            break;
        case '\376':
            c = '~';
            break;
        case '\031':
            c = '.';
            break;
        case '\333':
            c = '+';
            break;
        case '\030':
            c = '-';
            break;
        default:
            if(DEBUGNOTIMPL)
                notImplemented("linux line-draw-char: " + c + " (" + ((int)c) + ")");
            break;
        }
        return c;
    }

    final char mapLineDrawToATT6386(char c) {
        switch(c) {
        case ' ':
            c = ' ';
            break;
        case '`':
            c = '`';
            break;
        case '1':
            c = 'a';
            break;
        case 'x':
            c = 'f';
            break;
        case 'q':
            c = 'g';
            break;
        case '0':
            c = 'h';
            break;
        case 'Y':
            c = 'j';
            break;
        case '?':
            c = 'k';
            break;
        case 'Z':
            c = 'l';
            break;
        case '@':
            c = 'm';
            break;
        case 'E':
            c = 'n';
            break;
        case 'o':
            c = 'o';
            break;
        case 'p':
            c = 'p';
            break;
        case 'D':
            c = 'q';
            break;
        case 'r':
            c = 'r';
            break;
        case 's':
            c = 's';
            break;
        case 'C':
            c = 't';
            break;
        case '4':
            c = 'u';
            break;
        case 'A':
            c = 'v';
            break;
        case 'B':
            c = 'w';
            break;
        case '3':
            c = 'x';
            break;
        case 'y':
            c = 'y';
            break;
        case 'z':
            c = 'z';
            break;
        case '{':
            c = '{';
            break;
        case '|':
            c = '|';
            break;
        case '}':
            c = '}';
            break;
        case '~':
            c = '~';
            break;
        default:
            if(DEBUGNOTIMPL)
                notImplemented("att6386 line-draw-char: " + c + " (" + ((int)c) + ")");
        }
        return c;
    }

    public int interpretChar(char c) {
        int val, h, v;

        // If we have encountered a national-character we should print it!
        // The character comes from ssh as 8-bit but we do a
        // locale-specific translation to a java unicode-String... it's
        // definately not in for interpretation, silly bug... :-)
        //
        if((c) > 0xff) {
            return c;
        }

        switch(parseState[c]) {
        case CASE_PRINT:
            if(DEBUGPRINT)
                debug("PRINT: " + c + "(" + ((int)c) + ") " + curGL);
            int ic = IGNORE;
            int graphic;
            if (c >= 128 && curGR != -1) {
                graphic = curGR;
                c -= 128;
            } else {
                graphic = curGL;
            }
            switch(gSets[graphic]) {
            case CHARSET_UK:
                if(c == '#') {
                    ic = 0x00a3; // unicode pound-sign
                } else {
                    ic = c;
                }
                break;
            case CHARSET_ASCII:
            case CHARSET_ASCII_ALT:
            case CHARSET_ASCII_ALT2:
                // Do nothing...
                ic = c;

                // Make ctrl-chars print nice (OK, we should really do
                // this in TerminalWin but this is simpler...)
                //
                if(ic < 32) {
                    if(ic == 0 && hasNullPadding(whoAmIReally))
                        return IGNORE;
                    if (ic == 0x11 || ic == 0x13)
                        return IGNORE;
                    term.write('^');
                    ic += 64;
                }

                break;
            case CHARSET_LINES:
                if(!term.getOption(CompatTerminal.OPT_ASCII_LDC)) {
                    if (c  < 0x5f) {
                        ic = c;
                    } else {
                        if (whoAmIReally == EMUL_LINUX) {
                            c = mapLineDrawToLinux(c);
                        } else if (whoAmIReally == EMUL_ATT6386) {
                            c = mapLineDrawToATT6386(c);
                        }
                        term.writeLineDrawChar(c);
                    }
                } else {
                    ic = mapLineDrawToAscii(c);
                }
                break;
            default:
                if(DEBUGNOTIMPL)
                    notImplemented("unknown char-set: " + gSets[curGL] + " (" + ((int)gSets[curGL]) + "," + curGL + ")");
            }
            return ic;
        case CASE_GROUND_STATE:
            if(DEBUGSTATE)
                debug("GND_STATE");
            parseState = groundTable;
            break;
        case CASE_IGNORE_STATE:
            if(DEBUGSTATE)
                debug("IGN_STATE");
            parseState = ignTable;
            break;
        case CASE_IGNORE_ESC:
            if(DEBUGSTATE)
                debug("IGN_ESC");
            parseState = iesTable;
            break;
        case CASE_IGNORE:
            if(DEBUG)
                debug("IGNORE");
            break;
        case CASE_BELL:
            if(DEBUG)
                debug("BELL");
            term.doBell();
            break;
        case CASE_BS:
            if(DEBUG)
                debug("BS");
            term.doBS();
            break;
        case CASE_CR:
            if(DEBUG)
                debug("CR");
            term.doCR();
            parseState = groundTable;
            break;
        case CASE_ESC:
            if(DEBUGSTATE)
                debug("ESC");
            parseState = escTable;
            break;
        case CASE_VMOT:
            if(DEBUG)
                debug("VMOT");
            term.doLF();
            parseState = groundTable;
            break;
        case CASE_TAB:
            if(DEBUG)
                debug("TAB");
            term.doTab();
            break;
        case CASE_SI:
            if(DEBUG)
                debug("SI(curGL = 0)");
            curGL = 0;
            break;
        case CASE_SO:
            if(DEBUG)
                debug("S0(curGL = 1)");
            curGL = 1;
            break;
        case CASE_SCR_STATE:
            if(DEBUGSTATE)
                debug("SCR_STATE");
            parseState = scrTable;
            break;
        case CASE_SCS0_STATE:
            if(DEBUG)
                debug("SCS0");
            scsType    = 0;
            parseState = scsTable;
            break;
        case CASE_SCS1_STATE:
            if(DEBUG)
                debug("SCS1");
            scsType    = 1;
            parseState = scsTable;
            break;
        case CASE_SCS2_STATE:
            if(DEBUG)
                debug("SCS2");
            scsType    = 2;
            parseState = scsTable;
            break;
        case CASE_SCS3_STATE:
            if(DEBUG)
                debug("SCS3");
            scsType    = 3;
            parseState = scsTable;
            break;
        case CASE_ESC_IGNORE:
            if(DEBUG)
                debug("ESC_IGN");
            parseState = eigTable;
            break;
        case CASE_ESC_DIGIT:
            if(DEBUGSTATE)
                debug("ESC_DIGIT (" + ((c - '0')) + ")");
            val = param[nparam - 1];
            if(val == PARAMNOTUSED)
                val = 0;
            param[nparam - 1] = 10 * val + (c - '0');
            break;
        case CASE_ESC_SEMI:
            if(DEBUGSTATE)
                debug("ESC_SEMI");
            param[nparam++] = PARAMNOTUSED;
            break;
        case CASE_ESC_SEMIOSC:
            if(DEBUGSTATE)
                debug("ESC_SEMIOSC");
            param[nparam++] = PARAMNOTUSED;
            xtermSeq   = "";
            parseState = xtermSeqTable;
            break;
        case CASE_DEC_STATE:
            if(DEBUGSTATE)
                debug("DEC_STATE");
            parseState = decTable;
            break;
        case CASE_ICH:
            if(DEBUG)
                debug("ICH");
            val = param[0];
            if(val < 1)
                val = 1;
            term.insertChars(val);
            parseState = groundTable;
            break;
        case CASE_CPL:
            term.doCR();
            // Fall through
        case CASE_CUU:
            if(DEBUG)
                debug("CUU/CPL");
            val = param[0];
            if(val < 1)
                val = 1;
            term.cursorUp(val);
            parseState = groundTable;
            break;
        case CASE_CNL:
            term.doCR();
            // Fall through
        case CASE_CUD:
            if(DEBUG)
                debug("CUD/CNL");
            val = param[0];
            if(val < 1)
                val = 1;
            term.cursorDown(val);
            parseState = groundTable;
            break;
        case CASE_CUF:
            if(DEBUG)
                debug("CUF");
            val = param[0];
            if(val < 1)
                val = 1;
            term.cursorForward(val);
            parseState = groundTable;
            break;
        case CASE_CUB:
            if(DEBUG)
                debug("CUB");
            val = param[0];
            if(val < 1)
                val = 1;
            term.cursorBackward(val);
            parseState = groundTable;
            break;
        case CASE_CUP:
            v = param[0];
            h = param[1];
            if(DEBUG)
                debug("CUP " + v + " " + h);
            if(v < 1)
                v = 1;
            if(nparam < 2 || h < 1)
                h = 1;
            term.cursorSetPos(v - 1, h - 1, windowRelative);
            parseState = groundTable;
            break;
        case CASE_ED:
            if(DEBUG)
                debug("ED: " + param[0]);
            switch(param[0]) {
            case PARAMNOTUSED:
            case 0:
                term.clearBelow();
                break;
            case 1:
                term.clearAbove();
                break;
            case 2:
                term.clearScreen();
                term.cursorSetPos(0, 0, windowRelative);
                break;
            }
            parseState = groundTable;
            break;
        case CASE_EL:
            if(DEBUG)
                debug("EL");
            switch(param[0]) {
            case PARAMNOTUSED:
            case 0:
                term.clearRight();
                break;
            case 1:
                term.clearLeft();
                break;
            case 2:
                term.clearLine();
                break;
            }
            parseState = groundTable;
            break;
        case CASE_IL:
            if(DEBUG)
                debug("IL");
            val = param[0];
            if(val < 1)
                val = 1;
            term.insertLines(val);
            parseState = groundTable;
            break;
        case CASE_DL:
            if(DEBUG)
                debug("DL");
            val = param[0];
            if(val < 1)
                val = 1;
            term.deleteLines(val);
            parseState = groundTable;
            break;
        case CASE_DCH:
            if(DEBUG)
                debug("DCH");
            val = param[0];
            if(val < 1)
                val = 1;
            term.deleteChars(val);
            parseState = groundTable;
            break;
        case CASE_DECID:
            if(DEBUG)
                debug("DECID");
            param[0] = PARAMNOTUSED;
            // Fall through
        case CASE_DA1:
            if(DEBUG)
                debug("DA1");
            reply = replyTypes[R_CSI] + "?1;2c";
            term.sendBytes(reply.getBytes());
            parseState = groundTable;
            break;
        case CASE_TRACK_MOUSE:
            if(DEBUGNOTIMPL)
                notImplemented("TRACK_MOUSE");
            break;
        case CASE_TBC:
            if(DEBUG)
                debug("TBC " + "(" + term.getCursorH() + ")");
            if(param[0] <= 0) {
                term.clearTab(term.getCursorH());
            } else if(param[0] == 3) {
                term.clearAllTabs();
            }
            parseState = groundTable;
            break;
        case CASE_SET:
            if(DEBUG)
                debug("SET");
            ansiModes(true);
            parseState = groundTable;
            break;
        case CASE_RST:
            if(DEBUG)
                debug("RST");
            ansiModes(false);
            parseState = groundTable;
            break;
        case CASE_SGR:
            if(DEBUG)
                debug("SGR");
            sgrModes();
            parseState = groundTable;
            break;
        case CASE_CPR:
            if(DEBUG)
                debug("CPR");
            reply = null;
            if(param[0] == 5)
                reply = replyTypes[R_CSI] + "0n";
            else if(param[0] == 6)
                reply = replyTypes[R_CSI] + (term.getCursorV() + 1) + ";" + (term.getCursorH() + 1) + "R";
            if(reply != null)
                term.sendBytes(reply.getBytes());
            parseState = groundTable;
            break;
        case CASE_DECSTBM:
            if(DEBUG)
                debug("DECSTBM");
            int top = param[0];
            int bot = param[1];
            if(top < 1)
                top = 1;
            if(nparam < 2 || bot == PARAMNOTUSED || bot == 0 || bot > term.rows())
                bot = term.rows();
            if(bot > top) {
                term.setWindow(top - 1, bot);
                term.cursorSetPos(0, 0, windowRelative);
            }
            parseState = groundTable;
            break;
        case CASE_DECREQTPARM:
            if(DEBUG)
                debug("DECREQTPARM");
            if(param[0] == PARAMNOTUSED || param[0] == 1 || param[0] == 0)
                reply = (replyTypes[R_CSI] + String.valueOf((term.getCursorV() + 2)) + ";1;1;112;112;1;0x");
            if(reply != null)
                term.sendBytes(reply.getBytes());
            parseState = groundTable;
            break;
        case CASE_DECSET:
            if(DEBUG)
                debug("DECSET");
            dpModes(true);
            parseState = groundTable;
            break;
        case CASE_DECRST:
            if(DEBUG)
                debug("DECRST");
            dpModes(false);
            parseState = groundTable;
            break;
        case CASE_DECALN:
            if(DEBUG)
                debug("DECALN");
            term.fillScreen('E');
            parseState = groundTable;
            break;
        case CASE_GSETS:
            if(DEBUG)
                debug("GSETS: '" + c + "'");
            gSets[scsType] = c;
            parseState = groundTable;
            break;
        case CASE_DECSC:
            if(DEBUG)
                debug("DECSC");
            term.cursorSave();
            curGLDECSC = curGL;
            System.arraycopy(gSets, 0, gSetsDECSC, 0, 4);
            parseState = groundTable;
            break;
        case CASE_DECRC:
            if(DEBUG)
                debug("DECRC");
            term.cursorRestore();
            curGL = curGLDECSC;
            System.arraycopy(gSetsDECSC, 0, gSets, 0, 4);
            parseState = groundTable;
            break;
        case CASE_DECKPAM:
            if(DEBUG)
                debug("DECKPAM");
            //keypadAppl = true;
            parseState = groundTable;
            break;
        case CASE_DECKPNM:
            if(DEBUG)
                debug("DECKPNM");
            //keypadAppl = false;
            parseState = groundTable;
            break;
        case CASE_IND:
            term.cursorIndex(1);
            parseState = groundTable;
            break;
        case CASE_NEL:
            if(DEBUG)
                debug("NEL");
            term.cursorIndex(1);
            term.doCR();
            parseState = groundTable;
            break;
        case CASE_HTS:
            if(DEBUG)
                debug("HTS (" + term.getCursorH() + ")");
            term.setTab(term.getCursorH());
            parseState = groundTable;
            break;
        case CASE_RI:
            if(DEBUG)
                debug("RI");
            term.cursorIndexRev(1);
            parseState = groundTable;
            break;
        case CASE_SS2:
            //curSS = 2;
            parseState = groundTable;
            break;
        case CASE_SS3:
            //curSS = 3;
            parseState = groundTable;
            break;
        case CASE_CSI_STATE:
            if(DEBUGSTATE)
                debug("CSI_STATE");
            param[0]   = PARAMNOTUSED;
            nparam     = 1;
            parseState = csiTable;
            break;
        case CASE_OSC:
            param[0] = PARAMNOTUSED;
            nparam   = 1;
            parseState = oscTable;
            break;
        case CASE_RIS:
            if(DEBUG)
                debug("RIS");
            vtReset();
            parseState = groundTable;
            break;
        case CASE_LS2:
            if(DEBUG)
                debug("SI(curGL = 2)");
            curGL = 2;
            parseState = groundTable;
            break;
        case CASE_LS3:
            if(DEBUG)
                debug("SI(curGL = 3)");
            curGL = 3;
            parseState = groundTable;
            break;
        case CASE_LS3R:
            if(DEBUG)
                debug("LS3R");
            parseState = groundTable;
            curGR = 3;
            break;
        case CASE_LS2R:
            if(DEBUG)
                debug("LS2R");
            parseState = groundTable;
            curGR = 2;
            break;
        case CASE_LS1R:
            if(DEBUG)
                debug("LS1R");
            curGR = 1;
            parseState = groundTable;
            break;
        case CASE_XTERM_SAVE:
            xtermSavemodes();
            parseState = groundTable;
            break;
        case CASE_XTERM_RESTORE:
            xtermRestoremodes();
            parseState = groundTable;
            break;
        case CASE_XTERM_TITLE:
            if(DEBUGNOTIMPL)
                notImplemented("XTERM_TITLE");
            break;
        case CASE_HP_MEM_LOCK:
            if(DEBUGNOTIMPL)
                notImplemented("HP_MEM_LOCK");
            parseState = groundTable;
            break;
        case CASE_HP_MEM_UNLOCK:
            if(DEBUGNOTIMPL)
                notImplemented("HP_MEM_UNLOCK");
            parseState = groundTable;
            break;
        case CASE_HP_BUGGY_LL:
            if(DEBUGNOTIMPL)
                notImplemented("HP_BUGGY_LL");
            parseState = groundTable;
            break;
        case CASE_XTERM_SEQ:
            if(DEBUG)
                debug("XTERM_SEQ");
            xtermModes();
            parseState = groundTable;
            break;
        case CASE_SEQ_CAPTURE:
            if(DEBUG)
                debug("XTERM_SEQ_CAP" + c);
            xtermSeq += c;
            break;
        case CASE_ENQ:
            term.sendBytes(terminalType().getBytes());
            break;
        case CASE_XTERMWIN:
            xtermWinCtrl();
            parseState = groundTable;
        case CASE_CHT:
            if(DEBUG)
                debug("CHT");
            val = param[0];
            if(val == PARAMNOTUSED)
                val = 1;
            term.doTabs(val);
            parseState = groundTable;
            break;
        case CASE_SU:
            if(DEBUG)
                debug("SU");
            val = param[0];
            if(val == PARAMNOTUSED)
                val = 1;
            term.scrollUp(val);
            parseState = groundTable;
            break;
        case CASE_SD:
            if(DEBUG)
                debug("SD");
            val = param[0];
            if(val == PARAMNOTUSED)
                val = 1;
            term.scrollDown(val);
            parseState = groundTable;
            break;
        case CASE_ECH:
            if(DEBUG)
                debug("ECH");
            val = param[0];
            if(val == PARAMNOTUSED)
                val = 1;
            term.eraseChars(val);
            parseState = groundTable;
            break;
        case CASE_CBT:
            if(DEBUG)
                debug("CBT");
            val = param[0];
            if(val == PARAMNOTUSED)
                val = 1;
            term.doBackTabs(val);
            parseState = groundTable;
            break;
        case CASE_CHA:
        case CASE_HPA:
            if(DEBUG)
                debug("HPA/CHA");
            h = param[0];
            if(h < 1)
                h = 1;
            term.cursorSetPos(term.getCursorV(), h - 1, false);
            parseState = groundTable;
            break;
        case CASE_REP:
            if(DEBUGNOTIMPL)
                notImplemented("REP");
            parseState = groundTable;
            break;
        case CASE_VPA:
            if(DEBUG)
                debug("VPA");
            v = param[0];
            if(v < 1)
                v = 1;
            term.cursorSetPos(v - 1, term.getCursorH(), false);
            parseState = groundTable;
            break;
        case CASE_ANSI_PRINTER:
            if(DEBUG)
                debug("ANSI_PRINTER");
            val = param[0];
            if(val == PARAMNOTUSED)
                val = 0;
            ansiPrinterCtrl(val);
            parseState = groundTable;
            break;
        case CASE_VMOT2: //VT as escape sequence.
        	if(DEBUG)
                debug("VMOT2");
            term.doLF();
            break;
        case CASE_CR2://CR as escape sequence.    
            if(DEBUG)
                debug("CR2");
            term.doCR();
        default:
            if(DEBUGNOTIMPL)
                debug("** Unknown state !!!");
            break;
        }
        return IGNORE;
    }

    protected void ansiModes(boolean set
                                ) {
        for(int i = 0; i < nparam; i++) {
            switch(param[i]) {
            case 2:
                if(DEBUGNOTIMPL)
                    notImplemented("ANSI_AM");
                break;
            case 4:
                if(DEBUG)
                    debug("IRM " + set);
                term.setOption(CompatTerminal.OPT_INSERTMODE, set);
                break;
            case 12:
                if(DEBUGNOTIMPL)
                    notImplemented("ANSI_SRM");
                break;
            case 20:
                if(DEBUG)
                    debug("LNM");
                term.setOption(CompatTerminal.OPT_AUTO_LF, set);
                break;
            default:
                if(DEBUGNOTIMPL)
                    notImplemented("ansi-mode: " + param[i] + "(" + set + ")");
            }
        }
    }

    protected void sgrModes() {
        for(int i = 0; i < nparam; i++) {
            switch(param[i]) {
            case PARAMNOTUSED:
            case 0:
                term.clearAllAttributes();
                break;
            case 1:
            case 5:
                // !!! Should be Hilite/blinking...
                term.setAttribute(CompatTerminal.ATTR_BOLD, true);
                break;
            case 4:
                term.setAttribute(CompatTerminal.ATTR_UNDERLINE, true);
                break;
            case 7:
                term.setAttribute(CompatTerminal.ATTR_INVERSE, true);
                break;
            case 8:
                if(DEBUGNOTIMPL)
                    notImplemented("SGR invisible");
                break;
            case 22:
                term.setAttribute(CompatTerminal.ATTR_BOLD, false);
                break;
            case 24:
                term.setAttribute(CompatTerminal.ATTR_UNDERLINE, false);
                break;
            case 25:
                term.setAttribute(CompatTerminal.ATTR_BOLD, false); // !!! Actually no-blink
                break;
            case 27:
                term.setAttribute(CompatTerminal.ATTR_INVERSE, false);
                break;
            case 28:
                if(DEBUGNOTIMPL)
                    notImplemented("SGR visible");
                break;
            case 10:
                if(DEBUG)
                    debug("SGR ASCII: " + scsType);
                gSets[scsType] = CHARSET_ASCII;
                break;
            case 11:
            case 12:
                if(DEBUG)
                    debug("SGR LINEDRAW: " + scsType);
                gSets[scsType] = CHARSET_LINES;
                break;
            case 30:
            case 31:
            case 32:
            case 33:
            case 34:
            case 35:
            case 36:
            case 37:
                term.setForegroundColor(param[i] - 30);
                break;
            case 90:
            case 91:
            case 92:
            case 93:
            case 94:
            case 95:
            case 96:
            case 97:
                term.setForegroundColor(param[i] - 90);
                break;
            case 40:
            case 41:
            case 42:
            case 43:
            case 44:
            case 45:
            case 46:
            case 47:
                term.setBackgroundColor(param[i] - 40);
                break;
            case 100:
            case 101:
            case 102:
            case 103:
            case 104:
            case 105:
            case 106:
            case 107:
                term.setBackgroundColor(param[i] - 100);
                break;
            case 39:
                term.setForegroundColor(-1); // Reset to default color
                break;
            case 49:
                term.setBackgroundColor(-1); // Reset to default color
                break;
            default:
                if(DEBUGNOTIMPL)
                    notImplemented("SGR: " + param[i]);
            }
        }
    }

    protected void dpModes(boolean set) {
        for(int i = 0; i < nparam; i++) {
            switch(param[i]) {
            case 1:
                if(DEBUG)
                    debug("DECCKM");
                cursorKeysMode = set;
                break;
            case 2:
                if(DEBUG)
                    debug("ANSI/VT52");
                if(set) {
                    resetGSets();
                }
                break;
            case 3:
                if(DEBUG)
                    debug("DECCOLM");
                term.setOption(CompatTerminal.OPT_DECCOLM, set);
                break;
            case 4:
                if(DEBUGNOTIMPL)
                    notImplemented("DECSCLM");
                break;
            case 5:
                if(DEBUG)
                    debug("DECSCNM");
                term.setOption(CompatTerminal.OPT_REV_VIDEO, set);
                break;
            case 6:
                if(DEBUG)
                    debug("DECOM");
                windowRelative = set;
                break;
            case 7:
                if(DEBUG)
                    debug("DECAWM");
                term.setOption(CompatTerminal.OPT_AUTO_WRAP, set);
                break;
            case 8:
                // Not much we can do about autorepeat...
                if(DEBUG)
                    debug("DECARM");
                break;
            case 9:
                if(DEBUG)
                    debug("MOUSE_X10");
                if(set)
                    sendMousePos = MOUSE_X10COMP;
                else
                    sendMousePos = MOUSE_DONTSEND;
                break;
            case 18:
                if(DEBUGNOTIMPL)
                    notImplemented("DECPFF");
                break;
            case 19:
                if(DEBUGNOTIMPL)
                    notImplemented("DECPEF");
                break;
            case 25:
                if(DEBUG)
                    debug("VT220-VISCUR");
                term.setOption(CompatTerminal.OPT_VIS_CURSOR, set);
                break;
            case 38:
                if(DEBUGNOTIMPL)
                    notImplemented("DECTEK");
                break;
            case 40:
                if(DEBUG)
                    debug("DEC132COLS");
                term.setOption(CompatTerminal.OPT_DEC132COLS, set);
                break;
            case 41:
                if(DEBUGNOTIMPL)
                    notImplemented("DECCUR-HACK");
                break;
            case 42:
                if(DEBUGNOTIMPL)
                    notImplemented("DECNRCM");
                break;
            case 44:
                if(DEBUGNOTIMPL)
                    notImplemented("DECMRGBEL");
                break;
            case 45:
                if(DEBUG)
                    debug("DECREVWR");
                term.setOption(CompatTerminal.OPT_REV_WRAP, set);
                break;
            case 46:
                if(DEBUGNOTIMPL)
                    notImplemented("DECLOG");
                break;
            case 67:
                if(DEBUGNOTIMPL)
                    notImplemented("DECBKM");
                break;
            case 1000:
                if(DEBUG)
                    debug("MOUSE_DECVT200");
                if(set)
                    sendMousePos = MOUSE_DECVT200;
                else
                    sendMousePos = MOUSE_DONTSEND;
                break;
            case 1001:
                if(DEBUGNOTIMPL)
                    notImplemented("MOUSE_HLTRACK");
                if(set)
                    sendMousePos = MOUSE_HLTRACK;
                else
                    sendMousePos = MOUSE_DONTSEND;
                break;
            case 1002:
                if(DEBUGNOTIMPL)
                    notImplemented("Cell Motion Mouse Tracking");
                break;
            case 1003:
                if(DEBUGNOTIMPL)
                    notImplemented("All Motion Mouse Tracking");
                break;
            case 47:
            case 1047:
                curGLDECSC = curGL;
                System.arraycopy(gSets, 0, gSetsDECSC, 0, 4);
                vtReset(); // XXX: not really correct, but works better
                           // than doing nothing
                if(DEBUGNOTIMPL)
                    notImplemented("Use Alternate Screen Buffer");
                break;
            case 1048:
                term.cursorSave();
                curGLDECSC = curGL;
                System.arraycopy(gSets, 0, gSetsDECSC, 0, 4);
                if(DEBUGNOTIMPL)
                    notImplemented("Save cursor as in DECSC");
                break;
            case 1049:
                term.cursorSave();
                curGLDECSC = curGL;
                System.arraycopy(gSets, 0, gSetsDECSC, 0, 4);
		if (set) {
		    term.screenSave();
		    vtReset();
		} else {
		  term.screenRestore();
		}
                if(DEBUGNOTIMPL)
                    notImplemented("Save cursor as in DECSC + " +
                                   "Use Alternate Screen Buffer");
                break;
            default:
                if(DEBUGNOTIMPL)
                    notImplemented("DEC-private: " + param[i] + "(" + set+")");
            }
        }
    }

    protected void xtermModes() {
        switch(param[0]) {
        case 0:
            if(DEBUG)
                debug("XTERM-Change icon-name/title: " + xtermSeq);
        case 1:
            if(DEBUG)
                debug("XTERM-Change icon-name: " + xtermSeq);
        case 2:
            if(DEBUG)
                debug("XTERM-Change title: " + xtermSeq);
            term.setTitle(xtermSeq);
            break;
        case 10:
        case 11:
        case 12:
        case 13:
        case 14:
        case 15:
        case 16:
        case 17:
            if(DEBUGNOTIMPL)
                notImplemented("XTERM-Change colors: " + xtermSeq);
            break;
        case 20:
            if(DEBUGNOTIMPL)
                notImplemented("XITERM-Change bg-pixmap: " + xtermSeq);
            break;
        case 39:
            if(DEBUGNOTIMPL)
                notImplemented("XITERM-Change fg-color: " + xtermSeq);
            break;
        case 49:
            if(DEBUGNOTIMPL)
                notImplemented("XITERM-Change bg-color: " + xtermSeq);
            break;
        case 46:
            if(DEBUGNOTIMPL)
                notImplemented("XTERM-new log-file: " + xtermSeq);
            break;
        case 50:
            if(DEBUGNOTIMPL)
                notImplemented("XTERM-set font: " + xtermSeq);
            break;
        default:
            if(DEBUGNOTIMPL)
                notImplemented("XTERM-unknown: " + xtermSeq);
            break;
        }
    }

    protected void xtermWinCtrl() {
        for(int i = 0; i < nparam; i++) {
            switch(param[i]) {
            case 1:
                if(DEBUGNOTIMPL)
                    notImplemented("XTERM-deiconify");
                break;
            case 2:
                if(DEBUGNOTIMPL)
                    notImplemented("XTERM-iconify");
                break;
            case 3:
                if(DEBUGNOTIMPL)
                    notImplemented("XTERM-move (x,y): " + param[++i] + ", " + param[++i]);
                break;
            case 4:
                if(DEBUGNOTIMPL)
                    notImplemented("XTERM-resize (h,w): " + param[++i] + ", " + param[++i]);
                break;
            case 5:
                if(DEBUGNOTIMPL)
                    notImplemented("XTERM-raise");
                break;
            case 6:
                if(DEBUGNOTIMPL)
                    notImplemented("XTERM-lower");
                break;
            case 7:
                if(DEBUGNOTIMPL)
                    notImplemented("XTERM-refresh");
                break;
            case 8:
                if(DEBUGNOTIMPL)
                    notImplemented("XTERM-resize-txt (h,w): " + param[++i] + ", " + param[++i]);
                break;
            case 11:
                if(DEBUGNOTIMPL)
                    notImplemented("XTERM-report state");
                break;
            case 13:
                if(DEBUGNOTIMPL)
                    notImplemented("XTERM-report pos.");
                break;
            case 14:
                if(DEBUGNOTIMPL)
                    notImplemented("XTERM-report size");
                break;
            case 18:
                if(DEBUGNOTIMPL)
                    notImplemented("XTERM-report size-txt");
                break;
            case 20:
                if(DEBUGNOTIMPL)
                    notImplemented("XTERM-report icon-label");
                break;
            case 21:
                if(DEBUGNOTIMPL)
                    notImplemented("XTERM-report title");
                break;
            case 24:
            default:
                if(DEBUGNOTIMPL)
                    notImplemented("XTERM-resize to lines: " + param[i]);
                break;
            }
        }
    }

    protected void xtermSavemodes() {
        // !!! TODO, not much...
    }

    protected void xtermRestoremodes() {
        // !!! TODO, not much...
    }

    protected void ansiPrinterCtrl(int val) {
        switch(val) {
        case 0:
            if(DEBUG)
                debug("ANSI print screen");
            term.printScreen();
            break;
        case 4:
            if(DEBUG)
                debug("ANSI stop printer");
            term.stopPrinter();
            break;
        case 5:
            if(DEBUG)
                debug("ANSI start printer");
            term.startPrinter();
            break;
        default:
            if(DEBUGNOTIMPL)
                notImplemented("<esc>[: " + val + "i not suppored");
            break;
        }
    }

    final protected void resetGSets() {
        gSets[0] = CHARSET_ASCII;
        gSets[1] = CHARSET_LINES;
        gSets[2] = CHARSET_ASCII;
        gSets[3] = CHARSET_ASCII;
        curGL = 0;
        curGR = -1;
    }

    public void vtReset() {
        resetGSets();
        //curSS          = 0;
        parseState     = groundTable;
        windowRelative = false;
        //keypadAppl     = false;
        cursorKeysMode = false;
        sendMousePos   = MOUSE_DONTSEND;
        if(term != null) {
            term.resetWindow();
            term.clearScreen();
            term.cursorSetPos(0, 0, false);
            term.resetTabs();
        }
    }

    protected void notImplemented(String cmd) {
        debug("not implemented: " + cmd);
    }

    final int mapVKToXVK(int vk) {
        int i;
        for(i = 0; i < XVK_MAX; i++) {
            if(vk2xvk[i] == vk)
                break;
        }
        return i;
    }

    public final int mapModToTab(int modifiers) {
        int table = 0;
        if((modifiers & InputEvent.SHIFT_MASK) != 0)
            table = 1;
        if((modifiers & InputEvent.CTRL_MASK) != 0)
            table += 2;
        return table;
    }

    public final String mapSpecialKeys(int virtualKey, int modifiers) {
        int xvk = mapVKToXVK(virtualKey);
        int modTable = mapModToTab(modifiers);
        String[][] specKeyMap = theSpecialKeyMaps[modTable];
        return specKeyMap[xvk][whoAmIReally];
    }

    public void keyHandler(char c, int virtualKey, int modifiers) {
        String specialKey = null;
        String prefix = "";

        switch(virtualKey) {
        case KeyEvent.VK_UP:
        case KeyEvent.VK_DOWN:
        case KeyEvent.VK_RIGHT:
        case KeyEvent.VK_LEFT:
            if(cursorKeysMode)
                prefix = replyTypes[R_SS3];
            else
                prefix = replyTypes[R_CSI];
            specialKey = mapSpecialKeys(virtualKey, modifiers);
            break;
        case KeyEvent.VK_PAGE_UP:
        case KeyEvent.VK_PAGE_DOWN:
        case KeyEvent.VK_END:
        case KeyEvent.VK_HOME:
            if(!term.getOption(CompatTerminal.OPT_LOCAL_PGKEYS)) {
                prefix = replyTypes[R_CSI];
                specialKey = mapSpecialKeys(virtualKey, modifiers);
            }
            break;
        case KeyEvent.VK_INSERT:
            prefix = replyTypes[R_CSI];
            specialKey = mapSpecialKeys(virtualKey, modifiers);
            break;
        case KeyEvent.VK_F1:
        case KeyEvent.VK_F2:
        case KeyEvent.VK_F3:
        case KeyEvent.VK_F4:
        case KeyEvent.VK_F5:
        case KeyEvent.VK_F6:
        case KeyEvent.VK_F7:
        case KeyEvent.VK_F8:
        case KeyEvent.VK_F9:
        case KeyEvent.VK_F10:
        case KeyEvent.VK_F11:
        case KeyEvent.VK_F12:
            if(whoAmIReally == EMUL_VT100 || whoAmIReally == EMUL_ANSI || whoAmIReally == EMUL_ATT6386) {
                prefix = replyTypes[R_SS3];
            } else if(whoAmIReally == EMUL_VT220) {
                if(virtualKey > KeyEvent.VK_F4)
                    prefix = replyTypes[R_CSI];
                else
                    prefix = replyTypes[R_SS3];
            } else if(whoAmIReally == EMUL_VT52) {
                prefix = replyTypes[R_ESC];
            } else {
                prefix = replyTypes[R_CSI];
            }
            specialKey = mapSpecialKeys(virtualKey, modifiers);
            break;
        case KeyEvent.VK_NUMPAD0:
        case KeyEvent.VK_NUMPAD1:
        case KeyEvent.VK_NUMPAD2:
        case KeyEvent.VK_NUMPAD3:
        case KeyEvent.VK_NUMPAD4:
        case KeyEvent.VK_NUMPAD5:
        case KeyEvent.VK_NUMPAD6:
        case KeyEvent.VK_NUMPAD7:
        case KeyEvent.VK_NUMPAD8:
        case KeyEvent.VK_NUMPAD9:
        case KeyEvent.VK_MULTIPLY:
        case KeyEvent.VK_ADD:
        case KeyEvent.VK_SUBTRACT:
        case KeyEvent.VK_DIVIDE:
            /*
              !!! I give up on these, java is pretty immature concerning the virtual keys IMHO...
              Anyway, we should only send the key when in keypadAppl mode (the
              keyReleased/keyTyped must be handled accordingly since we must suppres
              the keyTyped...). All this is pretty messy, it would be POSSIBLE to do
              a huge kludge doing keyboard input using MOSTLY the keyReleased events
              and filling it in with the keyTyped that are not giving a sane
              VK_*. However, I am not in the mood of doing this right now. The
              virtual key handling/mapping should be refined in java IMHO
             
                  if(keypadAppl) {
                    prefix = replyTypes[R_SS3];
            	specialKey = mapSpecialKeys(virtualKey);
                  }
            */
        default:
            // !!! debug("XTerm keyHandler: " + virtualKey);
            break;
        }
        if(specialKey != null && !dumbMode) {
            specialKey = prefix + specialKey;
            term.sendBytes(specialKey.getBytes());
        }
        if (c == KeyEvent.CHAR_UNDEFINED) {
            return;
        }
        
        // In dumb mode ignore all control characters other than
        // ^C (3), ^D (4), CR (13), BS (8) and DEL (127)
        if (dumbMode && Character.isISOControl(c)
            && c != 3 && c != 4 && c != 8 && c != 13 && c != 127) {
            return;
        }

        if ((modifiers & InputEvent.CTRL_MASK) == 0 &&
                (modifiers & InputEvent.ALT_MASK) !=0) {
            // To be able to do meta-<key> in emacs with <left alt>
            term.typedChar((char) 27);
        }
        if ((c == 0x0a || c == 0x0d)
            && term.getOption(CompatTerminal.OPT_AUTO_LF)) {
            term.typedChar((char)0x0d);
            c = 0x0a;
        }
        term.typedChar(c);
    }

    int xButton(int modifiers) {
        int butt = 0;
        if((modifiers & InputEvent.BUTTON1_MASK) != 0)
            butt = 0;
        else if((modifiers & InputEvent.BUTTON2_MASK) != 0)
            butt = 1;
        else if((modifiers & InputEvent.BUTTON3_MASK) != 0)
            butt = 2;
        return butt;
    }

    int xKeyState(int modifiers) {
        int key = 0;
        if((modifiers & InputEvent.SHIFT_MASK) != 0)
            key |= 1;
        if((modifiers & InputEvent.ALT_MASK) != 0)
            key |= 2;
        if((modifiers & InputEvent.CTRL_MASK) != 0)
            key |= 4;
        return (key << 2);
    }

    public void mouseHandler(int row, int col, boolean press, int modifiers) {
        switch(sendMousePos) {
        case MOUSE_DONTSEND:
            break;
        case MOUSE_X10COMP:
            if(press) {
                term.sendBytes(("\033[M" + (char)(' ' + xButton(modifiers)) +
                                (char)(' ' + col + 1) + (char)(' ' + row + 1)).getBytes());
            }
            break;
        case MOUSE_DECVT200:
            term.sendBytes(("\033[M" + (char)(' ' + (press ?
                                              (xButton(modifiers) | xKeyState(modifiers)) :
                                              3)) +
                            (char)(' ' + col + 1) + (char)(' ' + row + 1)).getBytes());
            break;
        case MOUSE_HLTRACK:
            break;
        }
    }

    public final static int[] groundTable =
        {
            /*	NUL		SOH		STX		ETX	*/
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*	EOT		ENQ		ACK		BEL	*/
            CASE_PRINT,
            CASE_ENQ,
            CASE_PRINT,
            CASE_BELL,
            /*	BS		HT		NL		VT	*/
            CASE_BS,
            CASE_TAB,
            CASE_VMOT,
            CASE_VMOT,
            /*	NP		CR		SO		SI	*/
            CASE_VMOT,
            CASE_CR,
            CASE_SO,
            CASE_SI,
            /*	DLE		DC1		DC2		DC3	*/
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*	DC4		NAK		SYN		ETB	*/
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*	CAN		EM		SUB		ESC	*/
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_ESC,
            /*	FS		GS		RS		US	*/
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*	SP		!		"		#	*/
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*	$		%		&		'	*/
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*	(		)		*		+	*/
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*	,		-		.		/	*/
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*	0		1		2		3	*/
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*	4		5		6		7	*/
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*	8		9		:		;	*/
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*	<		=		>		?	*/
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*	@		A		B		C	*/
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*	D		E		F		G	*/
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*	H		I		J		K	*/
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*	L		M		N		O	*/
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*	P		Q		R		S	*/
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*	T		U		V		W	*/
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*	X		Y		Z		[	*/
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*	\		]		^		_	*/
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*	`		a		b		c	*/
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*	d		e		f		g	*/
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*	h		i		j		k	*/
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*	l		m		n		o	*/
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*	p		q		r		s	*/
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*	t		u		v		w	*/
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*	x		y		z		{	*/
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*	|		}		~		DEL	*/
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_GROUND_STATE,
            /*      0x80            0x81            0x82            0x83    */
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*      0x84            0x85            0x86            0x87    */
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*      0x88            0x89            0x8a            0x8b    */
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*      0x8c            0x8d            0x8e            0x8f    */
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*      0x90            0x91            0x92            0x93    */
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*      0x94            0x95            0x96            0x97    */
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*      0x99            0x99            0x9a            0x9b    */
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*      0x9c            0x9d            0x9e            0x9f    */
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*      nobreakspace    exclamdown      cent            sterling        */
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*      currency        yen             brokenbar       section         */
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*      diaeresis       copyright       ordfeminine     guillemotleft   */
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*      notsign         hyphen          registered      macron          */
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*      degree          plusminus       twosuperior     threesuperior   */
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*      acute           mu              paragraph       periodcentered  */
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*      cedilla         onesuperior     masculine       guillemotright  */
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*      onequarter      onehalf         threequarters   questiondown    */
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*      Agrave          Aacute          Acircumflex     Atilde          */
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*      Adiaeresis      Aring           AE              Ccedilla        */
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*      Egrave          Eacute          Ecircumflex     Ediaeresis      */
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*      Igrave          Iacute          Icircumflex     Idiaeresis      */
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*      Eth             Ntilde          Ograve          Oacute          */
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*      Ocircumflex     Otilde          Odiaeresis      multiply        */
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*      Ooblique        Ugrave          Uacute          Ucircumflex     */
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*      Udiaeresis      Yacute          Thorn           ssharp          */
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*      agrave          aacute          acircumflex     atilde          */
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*      adiaeresis      aring           ae              ccedilla        */
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*      egrave          eacute          ecircumflex     ediaeresis      */
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*      igrave          iacute          icircumflex     idiaeresis      */
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*      eth             ntilde          ograve          oacute          */
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*      ocircumflex     otilde          odiaeresis      division        */
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*      oslash          ugrave          uacute          ucircumflex     */
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            /*      udiaeresis      yacute          thorn           ydiaeresis      */
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
            CASE_PRINT,
        };

    // ESC [
    public final static int[] csiTable=
        {
            /*	NUL		SOH		STX		ETX	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	EOT		ENQ		ACK		BEL	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_BELL,
            /*	BS		HT		NL		VT	*/
            CASE_BS,
            CASE_TAB,
            CASE_VMOT,
            CASE_VMOT2,//RSC_CODE
            /*	NP		CR		SO		SI	*/
            CASE_VMOT,
            CASE_CR2,//RSC_CODE
            CASE_SO,
            CASE_SI,
            /*	DLE		DC1		DC2		DC3	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	DC4		NAK		SYN		ETB	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	CAN		EM		SUB		ESC	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_ESC,
            /*	FS		GS		RS		US	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	SP		!		"		#	*/
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            /*	$		%		&		'	*/
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            /*	(		)		*		+	*/
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            /*	,		-		.		/	*/
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            /*	0		1		2		3	*/
            CASE_ESC_DIGIT,
            CASE_ESC_DIGIT,
            CASE_ESC_DIGIT,
            CASE_ESC_DIGIT,
            /*	4		5		6		7	*/
            CASE_ESC_DIGIT,
            CASE_ESC_DIGIT,
            CASE_ESC_DIGIT,
            CASE_ESC_DIGIT,
            /*	8		9		:		;	*/
            CASE_ESC_DIGIT,
            CASE_ESC_DIGIT,
            CASE_IGNORE,
            CASE_ESC_SEMI,
            /*	<		=		>		?	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_DEC_STATE,
            /*	@		A		B		C	*/
            CASE_ICH,
            CASE_CUU,
            CASE_CUD,
            CASE_CUF,
            /*	D		E		F		G	*/
            CASE_CUB,
            CASE_CNL,
            CASE_CPL,
            CASE_CHA,
            /*	H		I		J		K	*/
            CASE_CUP,
            CASE_CHT,
            CASE_ED,
            CASE_EL,
            /*	L		M		N		O	*/
            CASE_IL,
            CASE_DL,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	P		Q		R		S	*/
            CASE_DCH,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_SU,
            /*	T		U		V		W	*/
            CASE_SD,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	X		Y		Z		[	*/
            CASE_ECH,
            CASE_GROUND_STATE,
            CASE_CBT,
            CASE_GROUND_STATE,
            /*	\		]		^		_	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_SD,
            CASE_GROUND_STATE,
            /*	`		a		b		c	*/
            CASE_HPA,
            CASE_GROUND_STATE,
            CASE_REP,
            CASE_DA1,
            /*	d		e		f		g	*/
            CASE_VPA,
            CASE_GROUND_STATE,
            CASE_CUP,
            CASE_TBC,
            /*	h		i		j		k	*/
            CASE_SET,
            CASE_ANSI_PRINTER,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	l		m		n		o	*/
            CASE_RST,
            CASE_SGR,
            CASE_CPR,
            CASE_GROUND_STATE,
            /*	p		q		r		s	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_DECSTBM,
            CASE_DECSC,
            /*	t		u		v		w	*/
            CASE_XTERMWIN,
            CASE_DECRC,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	x		y		z		{	*/
            CASE_DECREQTPARM,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	|		}		~		DEL	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      0x80            0x81            0x82            0x83    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x84            0x85            0x86            0x87    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x88            0x89            0x8a            0x8b    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x8c            0x8d            0x8e            0x8f    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x90            0x91            0x92            0x93    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x94            0x95            0x96            0x97    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x99            0x99            0x9a            0x9b    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x9c            0x9d            0x9e            0x9f    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      nobreakspace    exclamdown      cent            sterling        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      currency        yen             brokenbar       section         */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      diaeresis       copyright       ordfeminine     guillemotleft   */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      notsign         hyphen          registered      macron          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      degree          plusminus       twosuperior     threesuperior   */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      acute           mu              paragraph       periodcentered  */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      cedilla         onesuperior     masculine       guillemotright  */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      onequarter      onehalf         threequarters   questiondown    */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Agrave          Aacute          Acircumflex     Atilde          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Adiaeresis      Aring           AE              Ccedilla        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Egrave          Eacute          Ecircumflex     Ediaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Igrave          Iacute          Icircumflex     Idiaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Eth             Ntilde          Ograve          Oacute          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Ocircumflex     Otilde          Odiaeresis      multiply        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Ooblique        Ugrave          Uacute          Ucircumflex     */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Udiaeresis      Yacute          Thorn           ssharp          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      agrave          aacute          acircumflex     atilde          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      adiaeresis      aring           ae              ccedilla        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      egrave          eacute          ecircumflex     ediaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      igrave          iacute          icircumflex     idiaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      eth             ntilde          ograve          oacute          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      ocircumflex     otilde          odiaeresis      division        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      oslash          ugrave          uacute          ucircumflex     */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      udiaeresis      yacute          thorn           ydiaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
        };

    // ESC [ ?
    public final static int[] decTable =
        {
            /*	NUL		SOH		STX		ETX	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	EOT		ENQ		ACK		BEL	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_BELL,
            /*	BS		HT		NL		VT	*/
            CASE_BS,
            CASE_TAB,
            CASE_VMOT,
            CASE_VMOT,
            /*	NP		CR		SO		SI	*/
            CASE_VMOT,
            CASE_CR,
            CASE_SO,
            CASE_SI,
            /*	DLE		DC1		DC2		DC3	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	DC4		NAK		SYN		ETB	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	CAN		EM		SUB		ESC	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_ESC,
            /*	FS		GS		RS		US	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	SP		!		"		#	*/
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            /*	$		%		&		'	*/
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            /*	(		)		*		+	*/
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            /*	,		-		.		/	*/
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            /*	0		1		2		3	*/
            CASE_ESC_DIGIT,
            CASE_ESC_DIGIT,
            CASE_ESC_DIGIT,
            CASE_ESC_DIGIT,
            /*	4		5		6		7	*/
            CASE_ESC_DIGIT,
            CASE_ESC_DIGIT,
            CASE_ESC_DIGIT,
            CASE_ESC_DIGIT,
            /*	8		9		:		;	*/
            CASE_ESC_DIGIT,
            CASE_ESC_DIGIT,
            CASE_IGNORE,
            CASE_ESC_SEMI,
            /*	<		=		>		?	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	@		A		B		C	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	D		E		F		G	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	H		I		J		K	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	L		M		N		O	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	P		Q		R		S	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	T		U		V		W	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	X		Y		Z		[	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	\		]		^		_	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	`		a		b		c	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	d		e		f		g	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	h		i		j		k	*/
            CASE_DECSET,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	l		m		n		o	*/
            CASE_DECRST,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	p		q		r		s	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_XTERM_RESTORE,
            CASE_XTERM_SAVE,
            /*	t		u		v		w	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	x		y		z		{	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	|		}		~		DEL	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      0x80            0x81            0x82            0x83    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x84            0x85            0x86            0x87    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x88            0x89            0x8a            0x8b    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x8c            0x8d            0x8e            0x8f    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x90            0x91            0x92            0x93    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x94            0x95            0x96            0x97    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x99            0x99            0x9a            0x9b    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x9c            0x9d            0x9e            0x9f    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      nobreakspace    exclamdown      cent            sterling        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      currency        yen             brokenbar       section         */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      diaeresis       copyright       ordfeminine     guillemotleft   */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      notsign         hyphen          registered      macron          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      degree          plusminus       twosuperior     threesuperior   */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      acute           mu              paragraph       periodcentered  */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      cedilla         onesuperior     masculine       guillemotright  */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      onequarter      onehalf         threequarters   questiondown    */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Agrave          Aacute          Acircumflex     Atilde          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Adiaeresis      Aring           AE              Ccedilla        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Egrave          Eacute          Ecircumflex     Ediaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Igrave          Iacute          Icircumflex     Idiaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Eth             Ntilde          Ograve          Oacute          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Ocircumflex     Otilde          Odiaeresis      multiply        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Ooblique        Ugrave          Uacute          Ucircumflex     */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Udiaeresis      Yacute          Thorn           ssharp          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      agrave          aacute          acircumflex     atilde          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      adiaeresis      aring           ae              ccedilla        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      egrave          eacute          ecircumflex     ediaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      igrave          iacute          icircumflex     idiaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      eth             ntilde          ograve          oacute          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      ocircumflex     otilde          odiaeresis      division        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      oslash          ugrave          uacute          ucircumflex     */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      udiaeresis      yacute          thorn           ydiaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
        };

    // ESC ] ?
    public final static int[] oscTable =
        {
            /*	NUL		SOH		STX		ETX	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	EOT		ENQ		ACK		BEL	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	BS		HT		NL		VT	*/
            CASE_BS,
            CASE_TAB,
            CASE_VMOT,
            CASE_VMOT,
            /*	NP		CR		SO		SI	*/
            CASE_VMOT,
            CASE_CR,
            CASE_SO,
            CASE_SI,
            /*	DLE		DC1		DC2		DC3	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	DC4		NAK		SYN		ETB	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	CAN		EM		SUB		ESC	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_ESC,
            /*	FS		GS		RS		US	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	SP		!		"		#	*/
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            /*	$		%		&		'	*/
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            /*	(		)		*		+	*/
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            /*	,		-		.		/	*/
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            /*	0		1		2		3	*/
            CASE_ESC_DIGIT,
            CASE_ESC_DIGIT,
            CASE_ESC_DIGIT,
            CASE_ESC_DIGIT,
            /*	4		5		6		7	*/
            CASE_ESC_DIGIT,
            CASE_ESC_DIGIT,
            CASE_ESC_DIGIT,
            CASE_ESC_DIGIT,
            /*	8		9		:		;	*/
            CASE_ESC_DIGIT,
            CASE_ESC_DIGIT,
            CASE_IGNORE,
            CASE_ESC_SEMIOSC,
            /*	<		=		>		?	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	@		A		B		C	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	D		E		F		G	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	H		I		J		K	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	L		M		N		O	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	P		Q		R		S	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	T		U		V		W	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	X		Y		Z		[	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	\		]		^		_	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	`		a		b		c	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	d		e		f		g	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	h		i		j		k	*/
            CASE_DECSET,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	l		m		n		o	*/
            CASE_DECRST,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	p		q		r		s	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_XTERM_RESTORE,
            CASE_XTERM_SAVE,
            /*	t		u		v		w	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	x		y		z		{	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	|		}		~		DEL	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      0x80            0x81            0x82            0x83    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x84            0x85            0x86            0x87    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x88            0x89            0x8a            0x8b    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x8c            0x8d            0x8e            0x8f    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x90            0x91            0x92            0x93    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x94            0x95            0x96            0x97    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x99            0x99            0x9a            0x9b    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x9c            0x9d            0x9e            0x9f    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      nobreakspace    exclamdown      cent            sterling        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      currency        yen             brokenbar       section         */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      diaeresis       copyright       ordfeminine     guillemotleft   */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      notsign         hyphen          registered      macron          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      degree          plusminus       twosuperior     threesuperior   */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      acute           mu              paragraph       periodcentered  */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      cedilla         onesuperior     masculine       guillemotright  */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      onequarter      onehalf         threequarters   questiondown    */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Agrave          Aacute          Acircumflex     Atilde          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Adiaeresis      Aring           AE              Ccedilla        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Egrave          Eacute          Ecircumflex     Ediaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Igrave          Iacute          Icircumflex     Idiaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Eth             Ntilde          Ograve          Oacute          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Ocircumflex     Otilde          Odiaeresis      multiply        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Ooblique        Ugrave          Uacute          Ucircumflex     */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Udiaeresis      Yacute          Thorn           ssharp          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      agrave          aacute          acircumflex     atilde          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      adiaeresis      aring           ae              ccedilla        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      egrave          eacute          ecircumflex     ediaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      igrave          iacute          icircumflex     idiaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      eth             ntilde          ograve          oacute          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      ocircumflex     otilde          odiaeresis      division        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      oslash          ugrave          uacute          ucircumflex     */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      udiaeresis      yacute          thorn           ydiaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
        };

    //
    public final static int[] xtermSeqTable =
        {
            /*	NUL		SOH		STX		ETX	*/
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*	EOT		ENQ		ACK		BEL	*/
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_XTERM_SEQ,
            /*	BS		HT		NL		VT	*/
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*	NP		CR		SO		SI	*/
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*	DLE		DC1		DC2		DC3	*/
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*	DC4		NAK		SYN		ETB	*/
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*	CAN		EM		SUB		ESC	*/
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*	FS		GS		RS		US	*/
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*	SP		!		"		#	*/
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*	$		%		&		'	*/
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*	(		)		*		+	*/
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*	,		-		.		/	*/
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*	0		1		2		3	*/
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*	4		5		6		7	*/
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*	8		9		:		;	*/
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*	<		=		>		?	*/
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*	@		A		B		C	*/
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*	D		E		F		G	*/
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*	H		I		J		K	*/
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*	L		M		N		O	*/
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*	P		Q		R		S	*/
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*	T		U		V		W	*/
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*	X		Y		Z		[	*/
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*	\		]		^		_	*/
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*	`		a		b		c	*/
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*	d		e		f		g	*/
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*	h		i		j		k	*/
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*	l		m		n		o	*/
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*	p		q		r		s	*/
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*	t		u		v		w	*/
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*	x		y		z		{	*/
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*	|		}		~		DEL	*/
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_IGNORE,
            /*      0x80            0x81            0x82            0x83    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x84            0x85            0x86            0x87    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x88            0x89            0x8a            0x8b    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x8c            0x8d            0x8e            0x8f    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x90            0x91            0x92            0x93    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x94            0x95            0x96            0x97    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x99            0x99            0x9a            0x9b    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x9c            0x9d            0x9e            0x9f    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      nobreakspace    exclamdown      cent            sterling        */
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*      currency        yen             brokenbar       section         */
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*      diaeresis       copyright       ordfeminine     guillemotleft   */
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*      notsign         hyphen          registered      macron          */
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*      degree          plusminus       twosuperior     threesuperior   */
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*      acute           mu              paragraph       periodcentered  */
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*      cedilla         onesuperior     masculine       guillemotright  */
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*      onequarter      onehalf         threequarters   questiondown    */
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*      Agrave          Aacute          Acircumflex     Atilde          */
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*      Adiaeresis      Aring           AE              Ccedilla        */
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*      Egrave          Eacute          Ecircumflex     Ediaeresis      */
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*      Igrave          Iacute          Icircumflex     Idiaeresis      */
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*      Eth             Ntilde          Ograve          Oacute          */
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*      Ocircumflex     Otilde          Odiaeresis      multiply        */
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*      Ooblique        Ugrave          Uacute          Ucircumflex     */
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*      Udiaeresis      Yacute          Thorn           ssharp          */
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*      agrave          aacute          acircumflex     atilde          */
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*      adiaeresis      aring           ae              ccedilla        */
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*      egrave          eacute          ecircumflex     ediaeresis      */
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*      igrave          iacute          icircumflex     idiaeresis      */
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*      eth             ntilde          ograve          oacute          */
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*      ocircumflex     otilde          odiaeresis      division        */
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*      oslash          ugrave          uacute          ucircumflex     */
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            /*      udiaeresis      yacute          thorn           ydiaeresis      */
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
            CASE_SEQ_CAPTURE,
        };

    // CASE_ESC_IGNORE
    public final static int[] eigTable =
        {
            /*	NUL		SOH		STX		ETX	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	EOT		ENQ		ACK		BEL	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_BELL,
            /*	BS		HT		NL		VT	*/
            CASE_BS,
            CASE_TAB,
            CASE_VMOT,
            CASE_VMOT,
            /*	NP		CR		SO		SI	*/
            CASE_VMOT,
            CASE_CR,
            CASE_SO,
            CASE_SI,
            /*	DLE		DC1		DC2		DC3	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	DC4		NAK		SYN		ETB	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	CAN		EM		SUB		ESC	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_ESC,
            /*	FS		GS		RS		US	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	SP		!		"		#	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	$		%		&		'	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	(		)		*		+	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	,
            -		.		/	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	0		1		2		3	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	4		5		6		7	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	8		9		:		;	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	<		=		>		?	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	@		A		B		C	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	D		E		F		G	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	H		I		J		K	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	L		M		N		O	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	P		Q		R		S	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	T		U		V		W	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	X		Y		Z		[	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	\		]		^		_	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	`		a		b		c	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	d		e		f		g	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	h		i		j		k	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	l		m		n		o	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	p		q		r		s	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	t		u		v		w	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	x		y		z		{	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	|		}		~		DEL	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      0x80            0x81            0x82            0x83    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x84            0x85            0x86            0x87    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x88            0x89            0x8a            0x8b    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x8c            0x8d            0x8e            0x8f    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x90            0x91            0x92            0x93    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x94            0x95            0x96            0x97    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x99            0x99            0x9a            0x9b    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x9c            0x9d            0x9e            0x9f    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      nobreakspace    exclamdown      cent            sterling        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      currency        yen             brokenbar       section         */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      diaeresis       copyright       ordfeminine     guillemotleft   */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      notsign         hyphen          registered      macron          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      degree          plusminus       twosuperior     threesuperior   */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      acute           mu              paragraph       periodcentered  */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      cedilla         onesuperior     masculine       guillemotright  */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      onequarter      onehalf         threequarters   questiondown    */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Agrave          Aacute          Acircumflex     Atilde          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Adiaeresis      Aring           AE              Ccedilla        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Egrave          Eacute          Ecircumflex     Ediaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Igrave          Iacute          Icircumflex     Idiaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Eth             Ntilde          Ograve          Oacute          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Ocircumflex     Otilde          Odiaeresis      multiply        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Ooblique        Ugrave          Uacute          Ucircumflex     */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Udiaeresis      Yacute          Thorn           ssharp          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      agrave          aacute          acircumflex     atilde          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      adiaeresis      aring           ae              ccedilla        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      egrave          eacute          ecircumflex     ediaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      igrave          iacute          icircumflex     idiaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      eth             ntilde          ograve          oacute          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      ocircumflex     otilde          odiaeresis      division        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      oslash          ugrave          uacute          ucircumflex     */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      udiaeresis      yacute          thorn           ydiaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
        };

    // ESC
    public final static int[] escTable =
        {
            /*	NUL		SOH		STX		ETX	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	EOT		ENQ		ACK		BEL	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_BELL,
            /*	BS		HT		NL		VT	*/
            CASE_BS,
            CASE_TAB,
            CASE_VMOT,
            CASE_VMOT,
            /*	NP		CR		SO		SI	*/
            CASE_VMOT,
            CASE_CR,
            CASE_SO,
            CASE_SI,
            /*	DLE		DC1		DC2		DC3	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	DC4		NAK		SYN		ETB	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	CAN		EM		SUB		ESC	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_ESC,
            /*	FS		GS		RS		US	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	SP		!		"		#	*/
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_SCR_STATE,
            /*	$		%		&		'	*/
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            /*	(		)		*		+	*/
            CASE_SCS0_STATE,
            CASE_SCS1_STATE,
            CASE_SCS2_STATE,
            CASE_SCS3_STATE,
            /*	,		-		.		/	*/
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            /*	0		1		2		3	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	4		5		6		7	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_DECSC,
            /*	8		9		:		;	*/
            CASE_DECRC,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	<		=		>		?	*/
            CASE_GROUND_STATE,
            CASE_DECKPAM,
            CASE_DECKPNM,
            CASE_GROUND_STATE,
            /*	@		A		B		C	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	D		E		F		G	*/
            CASE_IND,
            CASE_NEL,
            CASE_HP_BUGGY_LL,
            CASE_GROUND_STATE,
            /*	H		I		J		K	*/
            CASE_HTS,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	L		M		N		O	*/
            CASE_GROUND_STATE,
            CASE_RI,
            CASE_SS2,
            CASE_SS3,
            /*	P		Q		R		S	*/
            CASE_IGNORE_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	T		U		V		W	*/
            CASE_XTERM_TITLE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	X		Y		Z		[	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_DECID,
            CASE_CSI_STATE,
            /*	\		]		^		_	*/
            CASE_GROUND_STATE,
            CASE_OSC,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            /*	`		a		b		c	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_RIS,
            /*	d		e		f		g	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	h		i		j		k	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	l		m		n		o	*/
            CASE_HP_MEM_LOCK,
            CASE_HP_MEM_UNLOCK,
            CASE_LS2,
            CASE_LS3,
            /*	p		q		r		s	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	t		u		v		w	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	x		y		z		{	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	|		}		~		DEL	*/
            CASE_LS3R,
            CASE_LS2R,
            CASE_LS1R,
            CASE_GROUND_STATE,
            /*      0x80            0x81            0x82            0x83    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x84            0x85            0x86            0x87    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x88            0x89            0x8a            0x8b    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x8c            0x8d            0x8e            0x8f    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x90            0x91            0x92            0x93    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x94            0x95            0x96            0x97    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x99            0x99            0x9a            0x9b    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x9c            0x9d            0x9e            0x9f    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      nobreakspace    exclamdown      cent            sterling        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      currency        yen             brokenbar       section         */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      diaeresis       copyright       ordfeminine     guillemotleft   */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      notsign         hyphen          registered      macron          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      degree          plusminus       twosuperior     threesuperior   */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      acute           mu              paragraph       periodcentered  */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      cedilla         onesuperior     masculine       guillemotright  */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      onequarter      onehalf         threequarters   questiondown    */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Agrave          Aacute          Acircumflex     Atilde          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Adiaeresis      Aring           AE              Ccedilla        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Egrave          Eacute          Ecircumflex     Ediaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Igrave          Iacute          Icircumflex     Idiaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Eth             Ntilde          Ograve          Oacute          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Ocircumflex     Otilde          Odiaeresis      multiply        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Ooblique        Ugrave          Uacute          Ucircumflex     */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Udiaeresis      Yacute          Thorn           ssharp          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      agrave          aacute          acircumflex     atilde          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      adiaeresis      aring           ae              ccedilla        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      egrave          eacute          ecircumflex     ediaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      igrave          iacute          icircumflex     idiaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      eth             ntilde          ograve          oacute          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      ocircumflex     otilde          odiaeresis      division        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      oslash          ugrave          uacute          ucircumflex     */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      udiaeresis      yacute          thorn           ydiaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
        };

    // CASE_IGNORE_ESC
    public final static int[] iesTable =
        {
            /*	NUL		SOH		STX		ETX	*/
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            /*	EOT		ENQ		ACK		BEL	*/
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            /*	BS		HT		NL		VT	*/
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            /*	NP		CR		SO		SI	*/
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            /*	DLE		DC1		DC2		DC3	*/
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            /*	DC4		NAK		SYN		ETB	*/
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            /*	CAN		EM		SUB		ESC	*/
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            /*	FS		GS		RS		US	*/
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            /*	SP		!		"		#	*/
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            /*	$		%		&		'	*/
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            /*	(		)		*		+	*/
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            /*	,		-		.		/	*/
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            /*	0		1		2		3	*/
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            /*	4		5		6		7	*/
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            /*	8		9		:		;	*/
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            /*	<		=		>		?	*/
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            /*	@		A		B		C	*/
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            /*	D		E		F		G	*/
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            /*	H		I		J		K	*/
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            /*	L		M		N		O	*/
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            /*	P		Q		R		S	*/
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            /*	T		U		V		W	*/
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            /*	X		Y		Z		[	*/
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            /*	\		]		^		_	*/
            CASE_GROUND_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            /*	`		a		b		c	*/
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            /*	d		e		f		g	*/
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            /*	h		i		j		k	*/
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            /*	l		m		n		o	*/
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            /*	p		q		r		s	*/
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            /*	t		u		v		w	*/
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            /*	x		y		z		{	*/
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            /*	|		}		~		DEL	*/
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            CASE_IGNORE_STATE,
            /*      0x80            0x81            0x82            0x83    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x84            0x85            0x86            0x87    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x88            0x89            0x8a            0x8b    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x8c            0x8d            0x8e            0x8f    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x90            0x91            0x92            0x93    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x94            0x95            0x96            0x97    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x99            0x99            0x9a            0x9b    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x9c            0x9d            0x9e            0x9f    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      nobreakspace    exclamdown      cent            sterling        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      currency        yen             brokenbar       section         */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      diaeresis       copyright       ordfeminine     guillemotleft   */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      notsign         hyphen          registered      macron          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      degree          plusminus       twosuperior     threesuperior   */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      acute           mu              paragraph       periodcentered  */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      cedilla         onesuperior     masculine       guillemotright  */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      onequarter      onehalf         threequarters   questiondown    */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Agrave          Aacute          Acircumflex     Atilde          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Adiaeresis      Aring           AE              Ccedilla        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Egrave          Eacute          Ecircumflex     Ediaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Igrave          Iacute          Icircumflex     Idiaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Eth             Ntilde          Ograve          Oacute          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Ocircumflex     Otilde          Odiaeresis      multiply        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Ooblique        Ugrave          Uacute          Ucircumflex     */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Udiaeresis      Yacute          Thorn           ssharp          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      agrave          aacute          acircumflex     atilde          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      adiaeresis      aring           ae              ccedilla        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      egrave          eacute          ecircumflex     ediaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      igrave          iacute          icircumflex     idiaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      eth             ntilde          ograve          oacute          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      ocircumflex     otilde          odiaeresis      division        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      oslash          ugrave          uacute          ucircumflex     */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      udiaeresis      yacute          thorn           ydiaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
        };

    // CASE_IGNORE_STATE
    public final static int[] ignTable =
        {
            /*	NUL		SOH		STX		ETX	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	EOT		ENQ		ACK		BEL	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	BS		HT		NL		VT	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	NP		CR		SO		SI	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	DLE		DC1		DC2		DC3	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	DC4		NAK		SYN		ETB	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	CAN		EM		SUB		ESC	*/
            CASE_GROUND_STATE,
            CASE_IGNORE,
            CASE_GROUND_STATE,
            CASE_IGNORE_ESC,
            /*	FS		GS		RS		US	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	SP		!		"		#	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	$		%		&		'	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	(		)		*		+	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	,		-		.		/	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	0		1		2		3	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	4		5		6		7	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	8		9		:		;	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	<		=		>		?	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	@		A		B		C	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	D		E		F		G	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	H		I		J		K	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	L		M		N		O	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	P		Q		R		S	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	T		U		V		W	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	X		Y		Z		[	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	\		]		^		_	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	`		a		b		c	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	d		e		f		g	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	h		i		j		k	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	l		m		n		o	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	p		q		r		s	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	t		u		v		w	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	x		y		z		{	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	|		}		~		DEL	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x80            0x81            0x82            0x83    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x84            0x85            0x86            0x87    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x88            0x89            0x8a            0x8b    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x8c            0x8d            0x8e            0x8f    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x90            0x91            0x92            0x93    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x94            0x95            0x96            0x97    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x99            0x99            0x9a            0x9b    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x9c            0x9d            0x9e            0x9f    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      nobreakspace    exclamdown      cent            sterling        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      currency        yen             brokenbar       section         */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      diaeresis       copyright       ordfeminine     guillemotleft   */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      notsign         hyphen          registered      macron          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      degree          plusminus       twosuperior     threesuperior   */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      acute           mu              paragraph       periodcentered  */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      cedilla         onesuperior     masculine       guillemotright  */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      onequarter      onehalf         threequarters   questiondown    */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Agrave          Aacute          Acircumflex     Atilde          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Adiaeresis      Aring           AE              Ccedilla        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Egrave          Eacute          Ecircumflex     Ediaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Igrave          Iacute          Icircumflex     Idiaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Eth             Ntilde          Ograve          Oacute          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Ocircumflex     Otilde          Odiaeresis      multiply        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Ooblique        Ugrave          Uacute          Ucircumflex     */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Udiaeresis      Yacute          Thorn           ssharp          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      agrave          aacute          acircumflex     atilde          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      adiaeresis      aring           ae              ccedilla        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      egrave          eacute          ecircumflex     ediaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      igrave          iacute          icircumflex     idiaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      eth             ntilde          ograve          oacute          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      ocircumflex     otilde          odiaeresis      division        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      oslash          ugrave          uacute          ucircumflex     */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      udiaeresis      yacute          thorn           ydiaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
        };

    // ESC #
    public final static int[] scrTable =
        {
            /*	NUL		SOH		STX		ETX	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	EOT		ENQ		ACK		BEL	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_BELL,
            /*	BS		HT		NL		VT	*/
            CASE_BS,
            CASE_TAB,
            CASE_VMOT,
            CASE_VMOT,
            /*	NP		CR		SO		SI	*/
            CASE_VMOT,
            CASE_CR,
            CASE_SO,
            CASE_SI,
            /*	DLE		DC1		DC2		DC3	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	DC4		NAK		SYN		ETB	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	CAN		EM		SUB		ESC	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_ESC,
            /*	FS		GS		RS		US	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	SP		!		"		#	*/
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            /*	$		%		&		'	*/
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            /*	(		)		*		+	*/
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            /*	,		-		.		/	*/
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            /*	0		1		2		3	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	4		5		6		7	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	8		9		:		;	*/
            CASE_DECALN,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	<		=		>		?	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	@		A		B		C	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	D		E		F		G	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	H		I		J		K	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	L		M		N		O	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	P		Q		R		S	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	T		U		V		W	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	X		Y		Z		[	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	\		]		^		_	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	`		a		b		c	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	d		e		f		g	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	h		i		j		k	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	l		m		n		o	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	p		q		r		s	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	t		u		v		w	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	x		y		z		{	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	|		}		~		DEL	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      0x80            0x81            0x82            0x83    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x84            0x85            0x86            0x87    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x88            0x89            0x8a            0x8b    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x8c            0x8d            0x8e            0x8f    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x90            0x91            0x92            0x93    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x94            0x95            0x96            0x97    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x99            0x99            0x9a            0x9b    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x9c            0x9d            0x9e            0x9f    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      nobreakspace    exclamdown      cent            sterling        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      currency        yen             brokenbar       section         */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      diaeresis       copyright       ordfeminine     guillemotleft   */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      notsign         hyphen          registered      macron          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      degree          plusminus       twosuperior     threesuperior   */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      acute           mu              paragraph       periodcentered  */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      cedilla         onesuperior     masculine       guillemotright  */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      onequarter      onehalf         threequarters   questiondown    */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Agrave          Aacute          Acircumflex     Atilde          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Adiaeresis      Aring           AE              Ccedilla        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Egrave          Eacute          Ecircumflex     Ediaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Igrave          Iacute          Icircumflex     Idiaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Eth             Ntilde          Ograve          Oacute          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Ocircumflex     Otilde          Odiaeresis      multiply        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Ooblique        Ugrave          Uacute          Ucircumflex     */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Udiaeresis      Yacute          Thorn           ssharp          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      agrave          aacute          acircumflex     atilde          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      adiaeresis      aring           ae              ccedilla        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      egrave          eacute          ecircumflex     ediaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      igrave          iacute          icircumflex     idiaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      eth             ntilde          ograve          oacute          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      ocircumflex     otilde          odiaeresis      division        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      oslash          ugrave          uacute          ucircumflex     */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      udiaeresis      yacute          thorn           ydiaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
        };

    // ESC ( etc.
    public final static int[] scsTable =
        {
            /*	NUL		SOH		STX		ETX	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	EOT		ENQ		ACK		BEL	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_BELL,
            /*	BS		HT		NL		VT	*/
            CASE_BS,
            CASE_TAB,
            CASE_VMOT,
            CASE_VMOT,
            /*	NP		CR		SO		SI	*/
            CASE_VMOT,
            CASE_CR,
            CASE_SO,
            CASE_SI,
            /*	DLE		DC1		DC2		DC3	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	DC4		NAK		SYN		ETB	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	CAN		EM		SUB		ESC	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_ESC,
            /*	FS		GS		RS		US	*/
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*	SP		!		"		#	*/
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            /*	$		%		&		'	*/
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            /*	(		)		*		+	*/
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            /*	,		-		.		/	*/
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            CASE_ESC_IGNORE,
            /*	0		1		2		3	*/
            CASE_GSETS,
            CASE_GSETS,
            CASE_GSETS,
            CASE_GROUND_STATE,
            /*	4		5		6		7	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	8		9		:		;	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	<		=		>		?	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	@		A		B		C	*/
            CASE_GROUND_STATE,
            CASE_GSETS,
            CASE_GSETS,
            CASE_GROUND_STATE,
            /*	D		E		F		G	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	H		I		J		K	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	L		M		N		O	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	P		Q		R		S	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	T		U		V		W	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	X		Y		Z		[	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	\		]		^		_	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	`		a		b		c	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	d		e		f		g	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	h		i		j		k	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	l		m		n		o	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	p		q		r		s	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	t		u		v		w	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	x		y		z		{	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*	|		}		~		DEL	*/
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      0x80            0x81            0x82            0x83    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x84            0x85            0x86            0x87    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x88            0x89            0x8a            0x8b    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x8c            0x8d            0x8e            0x8f    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x90            0x91            0x92            0x93    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x94            0x95            0x96            0x97    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x99            0x99            0x9a            0x9b    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      0x9c            0x9d            0x9e            0x9f    */
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            CASE_IGNORE,
            /*      nobreakspace    exclamdown      cent            sterling        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      currency        yen             brokenbar       section         */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      diaeresis       copyright       ordfeminine     guillemotleft   */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      notsign         hyphen          registered      macron          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      degree          plusminus       twosuperior     threesuperior   */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      acute           mu              paragraph       periodcentered  */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      cedilla         onesuperior     masculine       guillemotright  */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      onequarter      onehalf         threequarters   questiondown    */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Agrave          Aacute          Acircumflex     Atilde          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Adiaeresis      Aring           AE              Ccedilla        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Egrave          Eacute          Ecircumflex     Ediaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Igrave          Iacute          Icircumflex     Idiaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Eth             Ntilde          Ograve          Oacute          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Ocircumflex     Otilde          Odiaeresis      multiply        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Ooblique        Ugrave          Uacute          Ucircumflex     */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      Udiaeresis      Yacute          Thorn           ssharp          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      agrave          aacute          acircumflex     atilde          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      adiaeresis      aring           ae              ccedilla        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      egrave          eacute          ecircumflex     ediaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      igrave          iacute          icircumflex     idiaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      eth             ntilde          ograve          oacute          */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      ocircumflex     otilde          odiaeresis      division        */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      oslash          ugrave          uacute          ucircumflex     */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            /*      udiaeresis      yacute          thorn           ydiaeresis      */
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
            CASE_GROUND_STATE,
        };


    DateFormat debugDateFormat;

    private void debug(String msg) {
        if (debugDateFormat == null) {
            debugDateFormat = new SimpleDateFormat( "HH:mm:ss.SSS" );
        }

        System.out.println(debugDateFormat.format(new Date()) + ": " + msg);
    }
}
