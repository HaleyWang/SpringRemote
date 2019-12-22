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

import java.util.Hashtable;

// !!! OUCH KLUDGE

public abstract class TerminalDefProps {

    static public final int PROP_NAME  = 0;
    static public final int PROP_VALUE = 1;
    static public Hashtable<String, String> oldPropNames = new Hashtable<String, String>();
    static {
        oldPropNames.put("rv", "rev-video");
        oldPropNames.put("aw", "autowrap");
        oldPropNames.put("rw", "rev-autowrap");
        oldPropNames.put("im", "insert-mode");
        oldPropNames.put("al", "auto-linefeed");
        oldPropNames.put("sk", "repos-input");
        oldPropNames.put("si", "repos-output");
        oldPropNames.put("vi", "visible-cursor");
        oldPropNames.put("le", "local-echo");
        oldPropNames.put("vb", "visual-bell");
        oldPropNames.put("ct", "map-ctrl-space");
        oldPropNames.put("dc", "80x132-toggle");
        oldPropNames.put("da", "80x132-enable");
        oldPropNames.put("lp", "local-pgkeys");
        oldPropNames.put("sc", "copy-crnl");
        oldPropNames.put("ad", "ascii-line");
        oldPropNames.put("cs", "copy-select");
        oldPropNames.put("fn", "font-name");
        oldPropNames.put("fs", "font-size");
        oldPropNames.put("gm", "geometry");
        oldPropNames.put("te", "term-type");
        oldPropNames.put("sl", "save-lines");
        oldPropNames.put("sb", "scrollbar");
        oldPropNames.put("bg", "bg-color");
        oldPropNames.put("fg", "fg-color");
        oldPropNames.put("cc", "cursor-color");
        oldPropNames.put("bs", "backspace-send");
        oldPropNames.put("de", "delete-send");
        oldPropNames.put("sd", "select-delim");
        oldPropNames.put("pb", "paste-button");
    }

    public static String backwardCompatProp(String key) {
        String newName = oldPropNames.get(key);
        if(newName != null) {
            key = newName;
        }
        return key;
    }

    public static String[] systemFonts;
    public static String fontList() {
        if(systemFonts == null)
            systemFonts = com.mindbright.gui.GUI.getFontList();
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < systemFonts.length; i++) {
	    if (sb.length() > 0)
		sb.append(", ");
            sb.append(systemFonts[i]);
        }
        return sb.toString();
    }

    public static String defaultFont() {
        if(fontExists("DejaVu Sans Mono"))
            return "DejaVu Sans Mono";
        if(fontExists("monospaced"))
            return "Monospaced";
        if(fontExists("courier"))
            return "Courier";
        if(fontExists("dialoginput"))
            return "DialogInput";
        return systemFonts[0];
    }

    public static boolean fontExists(String font) {
        int i;
        try {
            if(systemFonts == null)
                systemFonts = com.mindbright.gui.GUI.getFontList();
            for(i = 0; i < systemFonts.length; i++) {
                if(systemFonts[i].equalsIgnoreCase(font))
                    break;
            }
            if(i == systemFonts.length)
                return false;
        } catch (Error e) {
            // There is no display so we just fudge the result
            // This gets things working when we run MindTerm in console mode
            // without a window system available.
            return true;
        }
        return true;
    }
}

