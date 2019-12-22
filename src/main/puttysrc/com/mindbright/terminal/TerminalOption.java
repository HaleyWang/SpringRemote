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


public class TerminalOption {
    private String key;
    private String description;
    private String defaultValue;
    private String value;
    private String[] choices;

    public TerminalOption(String key, String description, String defValue) {
        this(key, description, defValue, null);
    }
    
    public TerminalOption(String key, String description, String defValue,
                          String[] choices) {
        this.key = key;
        this.description = description;
        this.defaultValue = defValue;
        this.value = defValue;
        this.choices = choices;
    }

    public TerminalOption copy() {
        TerminalOption to = new TerminalOption(key, description, defaultValue,
                                               choices);
        to.value = value;
        return to;
    }
    
    public String getKey() {
        return key;
    }

    public String getDescription() {
        return description;
    }

    public String getDefault() {
        return defaultValue;
    }

    public String[] getChoices() {
        return choices;
    }
    
    public String getValue() {
        return value;
    }

    public boolean getValueB() {
        String lc = value.toLowerCase();
        return lc.equals("true") || lc.equals("yes");
    }

    public void setValue(String v) {
        value = v;
    }
}
