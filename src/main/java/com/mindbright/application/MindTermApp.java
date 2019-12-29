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

import java.util.Properties;
import java.applet.AppletContext;
import java.awt.Component;

import com.mindbright.sshcommon.SSHConsoleRemote;
import com.mindbright.ssh2.SSH2Transport;
import com.mindbright.ssh2.SSH2Connection;
import com.mindbright.ssh2.SSH2Interactor;

public interface MindTermApp {
    public String     getHost();
    public int        getPort();
    public Properties getProperties();
    public String     getProperty(String name);
    public void       setProperty(String name, String value);
    public String     getUserName();

    public Component  getDialogParent();
    public String     getAppName();

    public SSH2Interactor getInteractor();
    public void alert(String msg);

    public boolean isConnected();

    public boolean isApplet();
    public AppletContext getAppletContext();

    public SSH2Transport    getTransport();
    public SSH2Connection   getConnection();
    public SSHConsoleRemote getConsoleRemote();
}
