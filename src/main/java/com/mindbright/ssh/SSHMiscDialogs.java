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

package com.mindbright.ssh;

import java.awt.*;
import com.mindbright.gui.GUI;

public final class SSHMiscDialogs {

    public static void alert(String title, String message, Component parent) {
        GUI.showAlert(title, message, parent);
    }

    public static String password(String title, String message, Component parent) {
        return textInput(title, message, parent, '*', "", "Password:");
    }

    public static String textInput(String title, String message, Component parent) {
        return textInput(title, null, parent, (char)0, "", message);
    }

    public static String textInput(String title, String message, Component parent,
                                   String defaultValue) {
        return textInput(title, null, parent, (char)0, defaultValue, message);
    }

    public static String textInput(String title, String message, Component parent,
                                   char echo, String defaultValue, String prompt) {
        return GUI.textInput(title, message, parent, echo, defaultValue, prompt);
    }
    
    public static String setPassword(String title, String message, Component parent) {
        return GUI.setPassword(title, message, parent);
    }
    
    public static boolean confirm(String title, String message, boolean defAnswer,
                                  Component parent) {
        return GUI.showConfirm(title, message, 0, 0, "Yes", "No", 
                               defAnswer, parent, false, false);
    }

    public static boolean confirm(String title, String message,
                                  int rows, int cols,
                                  String yesLbl, String noLbl,
                                  boolean defAnswer, Component parent,
                                  boolean scrollbar) {
        return GUI.showConfirm(title, message, rows, cols, yesLbl, noLbl,
                               defAnswer, parent, false, scrollbar);
    }
}
