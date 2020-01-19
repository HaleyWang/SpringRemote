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

import com.mindbright.terminal.DisplayView;

class StatusLine {
    private static int LINE_LENGTH    = 80;
    private static int MESSAGE_LENGTH = 64;
    private static int STATUS_LENGTH  = 13;
    private static int ERROR_LENGTH   = 79;

    private char statusLine[] = new char[LINE_LENGTH];
    private char errorLine[] = new char[LINE_LENGTH];
    private int attribLine[] = new int[LINE_LENGTH];
    private boolean errorSet = false;
    private Terminal6530Callback termCallback;

    StatusLine(Terminal6530Callback termCallback, int defaultAttr) {
        for (int i = 0; i < LINE_LENGTH; i++) {
            attribLine[i] = defaultAttr;
        }
        clearLine(statusLine, 0, LINE_LENGTH);
        clearLine(errorLine, 0, LINE_LENGTH);
        this.termCallback = termCallback;
    }

    private void notifyDisplay() {
        termCallback.statusLineUpdated();
    }

    private void clearLine(char line[], int offset, int len) {
        for (int i = offset; i < (offset + len); i++) {
            line[i] = ' ';
        }
    }

    private void copyString(String msg, char line[], int offset, int maxLen) {
        char ca[] = msg.toCharArray();
        copyString(ca, line, offset, maxLen);
    }
    private void copyString(char msg[], char line[], int offset, int maxLen) {
        int len = msg.length < maxLen ? msg.length : maxLen;

        clearLine(line, offset, maxLen);
        System.arraycopy(msg, 0, line, offset, len);
    }

    void setDisplay(DisplayView display) {
    }

    void setMessage(String msg) {
        copyString(msg, statusLine, 1, MESSAGE_LENGTH);
        notifyDisplay();
    }

    void setMessage(char msg[], char attribs[]) {
        // XXX ignore attributes for now
        copyString(msg, statusLine, 1, MESSAGE_LENGTH);
        notifyDisplay();
    }

    void setStatus(String msg) {
        copyString(msg, statusLine, 65, STATUS_LENGTH);
        notifyDisplay();
    }

    void setError(String msg) {
        errorSet = true;
        copyString(msg, errorLine, 1, ERROR_LENGTH);
        notifyDisplay();
    }

    void resetError() {
        errorSet = false;
        notifyDisplay();
    }

    char[] getChars() {
        if (errorSet) {
            return errorLine;
        }
        return statusLine;
    }

    int[] getAttribs() {
        return attribLine;
    }
}


