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

import java.util.Vector;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.StringSelection;

public class GlobalClipboard implements TerminalClipboardHandler {

    // Singleton instance of GlobalClipboard
    //
    private static GlobalClipboard globalClipboard = null;

    private Toolkit   toolkit;
    private Vector<TerminalMenuHandler> menuHandlers;
    private Clipboard jvmClipboard;

    private GlobalClipboard() {
        this.toolkit      = Toolkit.getDefaultToolkit();
        this.menuHandlers = new Vector<TerminalMenuHandler>();
    }

    public static synchronized GlobalClipboard getClipboardHandler() {
        return getClipboardHandler(null);
    }

    public static synchronized GlobalClipboard
    getClipboardHandler(TerminalMenuHandler menuHandler) {
        if(globalClipboard == null) {
            globalClipboard = new GlobalClipboard();
        }
        globalClipboard.addMenuHandler(menuHandler);
        return globalClipboard;
    }

    public void addMenuHandler(TerminalMenuHandler menuHandler) {
        if(menuHandler != null && !menuHandlers.contains(menuHandler)) {
            this.menuHandlers.addElement(menuHandler);
        }
    }

    public void removeMenuHandler(TerminalMenuHandler menuHandler) {
        if(menuHandlers.contains(menuHandler)) {
            menuHandlers.removeElement(menuHandler);
        }
    }

    public void setSelection(String selection) {
        // We always try to set both the Clipboard and the SystemSelection
        Clipboard cb = getClipboard();
        Clipboard ss = toolkit.getSystemSelection();

        if(selection == null)
            selection = "";
        StringSelection sl = new StringSelection(selection);

        if(cb != null) {
            cb.setContents(sl, sl);
        }
        if (ss != null) {
            ss.setContents(sl, sl);
        }
        if (cb != null || ss != null) {
            selectionAvailable(true);
        }
    }

    public String getSelection() {
        String    sl = null;

        // Prefer to fetch from SystemSelection if available

        Clipboard cb = toolkit.getSystemSelection();
        if (cb == null) {
            cb = getClipboard();
        }
        if(cb == null) {
            return sl;
        }

        Transferable t = cb.getContents(this);

        if(t != null) {
            try {
                sl = (String) t.getTransferData(DataFlavor.stringFlavor);
            } catch (Exception e) {
                try {
                    toolkit.beep();
                } catch (Throwable ee) {
                    // Could not beep, we are probably an unpriviliged applet
                }
            }
        } else {
            try {
                toolkit.beep();
            } catch (Throwable e) {
                // Could not beep, we are probably an unpriviliged applet
            }
        }

        return sl;
    }

    public void clearSelection() {
        selectionAvailable(false);
    }

    private void selectionAvailable(boolean val) {
    }

    // This is the clipboard that is used on most systems. On *nix X11 it is
    // the CLIPBOARD that e.g. CDE reads.

    private synchronized Clipboard getClipboard() {

        Clipboard cb;
        if(jvmClipboard == null) {
            try {
                cb = toolkit.getSystemClipboard();
            } catch (Throwable e) {
                //
                // If we can't access the system clipboard we use our own
                // "global" one.
                //
                cb = jvmClipboard = new Clipboard("MindTerm-local-clipboard");
            }
        } else {
            cb = jvmClipboard;
        }
        return cb;
    }

}
