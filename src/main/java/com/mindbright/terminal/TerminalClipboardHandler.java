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
 * Interface describing a clipboard for the terminal emulator.
 *
 * @see TerminalWin
 */
public interface TerminalClipboardHandler {
    /**
     * Set the content of the clipboard to the specified string.
     *
     * @param selection new contents of the clipboard.
     */
    public void   setSelection(String selection);

    /**
     * Get the current contents of the clipboard
     *
     * @return the current clipboard contents.
     */
    public String getSelection();

    /**
     * Clear the clipboard contents.
     */
    public void   clearSelection();
}
