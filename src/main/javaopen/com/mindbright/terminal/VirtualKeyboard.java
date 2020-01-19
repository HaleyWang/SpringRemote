/******************************************************************************
 *
 * Copyright (c) 2005-2011 Cryptzone Group AB. All Rights Reserved.
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

import java.util.StringTokenizer;

import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class VirtualKeyboard extends Panel implements ActionListener {
	private static final long serialVersionUID = 1L;
	private TerminalWin terminal;

    public VirtualKeyboard(String keylist, TerminalWin terminal,
                           DisplayView display) {
        this(arrayFromListNoTrim(keylist, ","), terminal, display);
    }

    public VirtualKeyboard(String[] keys, TerminalWin terminal,
                           DisplayView display) {
        this.terminal = terminal;
        for (int i=0; i<keys.length; i++) {
            String label;
            String action;
            int index = keys[i].indexOf('=');
            if (index == -1) {
                label = action = keys[i].trim();
            } else {
                label = keys[i].substring(0, index).trim();
                action = keys[i].substring(index+1);
            }
            add(display.mkButton(label, action, this));
        }
    }

    /**
     * Convert a list expressed as a delimited string to an array.
     * This is similar to the function in SSH2ListUtil but does not
     * trim the entries.
     *
     * @param list List to split.
     * @param delim Delimiter.
     *
     * @return Resulting array.
     */
    public static String[] arrayFromListNoTrim(String list, String delim) {
        if(list == null) {
            return new String[0];
        }
        StringTokenizer st = new StringTokenizer(list, delim);
        int cnt = 0;
        String[] sa = new String[st.countTokens()];
        while(st.hasMoreTokens()) {
            sa[cnt++] = st.nextToken();
        }
        return sa;
    }

    /*
     * ActionListener interface
     */
    public void actionPerformed(ActionEvent e) {
        terminal.sendString(e.getActionCommand());
        terminal.requestFocus();
    }
}
