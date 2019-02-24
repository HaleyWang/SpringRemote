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

import java.awt.Frame;

/**
 * A terminal listener which handles the title of the window and makes
 * sure that the current size is always displayed therein (as
 * [COLSxROWS]).
 *
 * @see TerminalWindow
 */
public class TerminalFrameTitle extends TerminalInputAdapter {

    private TerminalWindow terminal;
    private Frame       frame;
    private String      titleName;

    /**
     * Creates a new instance tied to a specified Frame and with a
     * give starting title.
     *
     * @param frame the frame whose title should be updated
     * @param titleName initial title
     */
    public TerminalFrameTitle(Frame frame, String titleName) {
        this.frame     = frame;
        this.titleName = titleName;
    }

    /**
     * Attach this to the given terminal
     *
     * @param terminal terminal window to check for size changes
     */
    public void attach(TerminalWindow terminal) {
        this.terminal = terminal;
        setTitleName(titleName);
        terminal.addInputListener(this);
    }

    /**
     * Detach from the terminal window
     */
    public void detach() {
        terminal.removeInputListener(this);
        terminal = null;
    }

    /**
     * Set the title text.
     *
     * @param titleName title text
     */
    public void setTitleName(String titleName) {
        this.titleName = titleName;
        if(terminal != null) {
            signalWindowChanged(terminal.rows(), terminal.cols(),
                                terminal.vpixels(), terminal.hpixels());
        }
    }

    public void signalWindowChanged(int rows, int cols,
                                    int vpixels, int hpixels) {
        if (terminal == null || frame == null)
            return;
        String title = terminal.getTitle();
        if(title == null) {
            title = titleName;
        }
        title += " [" + cols + "x" + rows + "]";
        frame.setTitle(title);
    }

}
