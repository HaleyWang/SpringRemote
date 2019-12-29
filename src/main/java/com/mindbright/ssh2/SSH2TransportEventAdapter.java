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

package com.mindbright.ssh2;


/**
 * This class is an adapter for the interface
 * <code>SSH2TransportEventHandler</code>.
 *
 * @see SSH2TransportEventHandler
 */
public class SSH2TransportEventAdapter implements SSH2TransportEventHandler {
    public void gotConnectInfoText(SSH2Transport tp, String text) {}
    public void gotPeerVersion(SSH2Transport tp, String versionString,
                               int major, int minor, String packageVersion) {}

    public void kexStart(SSH2Transport tp) {}
    public void kexAgreed(SSH2Transport tp,
                          SSH2Preferences ourPrefs, SSH2Preferences peerPrefs) {}
    public boolean kexAuthenticateHost(SSH2Transport tp,
                                       SSH2Signature serverHostKey) {
        return true;
    }
    public void kexComplete(SSH2Transport tp) {}

    public void msgDebug(SSH2Transport tp, boolean alwaysDisplay, String message,
                         String languageTag) {}
    public void msgIgnore(SSH2Transport tp, byte[] data) {}
    public void msgUnimplemented(SSH2Transport tp, int rejectedSeqNum) {}

    public void peerSentUnknownMessage(SSH2Transport tp, int pktType) {}

    public void normalDisconnect(SSH2Transport tp, String description,
                                 String languageTag) {}
    public void fatalDisconnect(SSH2Transport tp, int reason,
                                String description, String languageTag) {}
    public void peerDisconnect(SSH2Transport tp, int reason,
                               String description, String languageTag) {}

}
