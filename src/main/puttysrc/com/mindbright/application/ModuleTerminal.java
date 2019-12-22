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

package com.mindbright.application;

import com.mindbright.util.JarLoader;

public class ModuleTerminal implements MindTermModule {

    MindTermModule engine = null;

    private void load(MindTermApp mindterm) {
        String name = "com.mindbright.application.ModuleTerminalImpl";
        Class<?> c;

        try {
            c = Class.forName(name);
        } catch (ClassNotFoundException e) {
            try {
                JarLoader cl = new JarLoader(mindterm.getProperty("jar-path"),
                                             "lite_term.jar");
                c = cl.loadClass(name);
            } catch (Exception e2) {
                mindterm.alert("Failed to load lite_term.jar: " + e2);
                return;
            }
        }
        try {
            engine = (MindTermModule)c.newInstance();
        } catch (Exception e) {
            mindterm.alert("Failed to create instance of '" + name + "': " + e);
            return;
        }
        engine.init(mindterm);
    }

    public void init(MindTermApp mindterm) {
        // Do nothing
    }

    public void activate(MindTermApp mindterm) {
        if (null == engine)
            load(mindterm);
        engine.activate(mindterm);
    }

    public boolean isAvailable(MindTermApp mindterm) {
        return mindterm.isConnected();
    }

    public void connected(MindTermApp mindterm) {
        // Do nothing
    }

    public void disconnected(MindTermApp mindterm) {
        if (null != engine) {
            engine.disconnected(mindterm);
        }
    }

    public String description(MindTermApp mindterm) {
        return null;
    }
}


