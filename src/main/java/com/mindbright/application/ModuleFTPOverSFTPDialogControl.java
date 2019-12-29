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

package com.mindbright.application;

import java.awt.event.ActionListener;

public interface ModuleFTPOverSFTPDialogControl {
    static String LBL_LISTEN_ADDR     = "Listen address";
    static String LBL_LISTEN_PORT     = "Listen port";
    static String LBL_BRIDGE_DISABLED = "Bridge disabled";
    static String LBL_ENABLE          = "Enable";
    static String LBL_DISABLE         = "Disable";
    static String LBL_BROWSER         = "Browser...";
    static String LBL_DISMISS         = "Dismiss";
    static String LBL_REMOTE_SYSTEM   = "Remote system type";
    static String LBL_REMOTE_IS_UNIX  = "Unix";
    static String LBL_REMOTE_IS_WINDOWS  = "Windows";
    static String DEFAULT_HOST        = "127.0.0.1";
    static String DEFAULT_PORT        = "21";

    public void initDialog(ActionListener al, boolean showBrowse);
    public void showDialog();
    public void disposeDialog();

    public void setHost(String host);
    public void setPort(String port);
    public void setRemoteSystemIsUnix(boolean yes);
    public void setStatus(String status);
    public void setMode(boolean enable);

    public String getHost();
    public String getPort();
    public boolean isRemoteSystemUnix();
}
