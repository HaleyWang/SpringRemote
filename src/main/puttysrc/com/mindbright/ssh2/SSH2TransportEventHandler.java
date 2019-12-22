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
 * This interface is an event callback interface used to monitor the transport
 * layer of an ssh2 connection. It is used with the class
 * <code>SSH2Transport</code> to get info on the progress and status of the ssh2
 * connection. It is also used to authenticate the host running the ssh2 server
 * (e.g. through comparing its public key to a known key or verifying its
 * certificate).
 *
 * @see SSH2Transport
 * @see SSH2TransportEventAdapter
 */
public interface SSH2TransportEventHandler {

    /**
     * Called when an info text is received in the version negotiation stage (as
     * defined in the transport protocol spec.).
     *
     * @param tp   the transport layer
     * @param text the info text received
     */
    public void gotConnectInfoText(SSH2Transport tp, String text);

    /**
     * Called in the version negotiation stage when the peer version is received
     * (as defined in the transport protocol spec.).
     *
     * @param tp             the transport layer
     * @param versionString  the version string of peer
     * @param major          the major protocol version of peer
     * @param minor          the minor protocol version of peer
     * @param packageVersion the package version of peer
     */
    public void gotPeerVersion(SSH2Transport tp, String versionString,
                               int major, int minor, String packageVersion);

    /**
     * Called when key exchange starts. That is when a KEXINIT message
     * is sent to the peer.
     *
     * @param tp the transport layer
     */
    public void kexStart(SSH2Transport tp);

    /**
     * Called when key exchange have agreed on algorithms. That is when
     * a KEXINIT message has been received and processed).
     *
     * @param tp        the transport layer
     * @param ourPrefs  our preferences
     * @param peerPrefs peer's preferences
     */
    public void kexAgreed(SSH2Transport tp,
                          SSH2Preferences ourPrefs, SSH2Preferences peerPrefs);

    /**
     * Called to authenticate server's host key.
     *
     * @param tp            the transport layer
     * @param serverHostKey server's host key
     *
     * @return a boolean indicating if the server could be authenticated or not.
     */
    public boolean kexAuthenticateHost(SSH2Transport tp,
                                       SSH2Signature serverHostKey);

    /**
     * Called when key exchange has been successfully completed. That
     * is when new keys and algorithms are now active.
     *
     * @param tp the transport layer
     */
    public void kexComplete(SSH2Transport tp);

    /**
     * Called when a DEBUG message is received.
     *
     * @param tp            the transport layer
     * @param alwaysDisplay boolean flag indicating whether this message should
     * always be displayed or not.
     * @param message       debug message contained in the packet
     * @param languageTag   language tag
     */
    public void msgDebug(SSH2Transport tp, boolean alwaysDisplay,
                         String message, String languageTag);

    /**
     * Called when an IGNORE message is received.
     *
     * @param tp   the transport layer
     * @param data byte array of data contained in packet
     */
    public void msgIgnore(SSH2Transport tp, byte[] data);

    /**
     * Called when an UNIMPLEMENTED message is received.
     *
     * @param tp             the transport layer
     * @param rejectedSeqNum sequence number of packet which peer didn't
     * understnad
     */
    public void msgUnimplemented(SSH2Transport tp, int rejectedSeqNum);

    /**
     * Called when an unimplemented message is received, and an UNIMPLEMENTED
     * message is sent to peer.
     *
     * @param tp   the transport layer
     * @param pktType type of message which we didn't understand
     */
    public void peerSentUnknownMessage(SSH2Transport tp, int pktType);

    /**
     * Called when transport layer is disconnected gracefully by our side of
     * connection.
     *
     * @param tp          the transport layer
     * @param description textual description for reason of disconnect 
     * @param languageTag language tag
     */
    public void normalDisconnect(SSH2Transport tp, String description,
                                 String languageTag);


    /**
     * Called when transport layer is disconnected for the given fatal reason by
     * our side of the connection. See the class <code>SSH2</code> for reason
     * codes.
     *
     * @param tp          the transport layer
     * @param reason      the reason code
     * @param description textual description for reason of disconnect 
     * @param languageTag language tag
     *
     * @see SSH2
     */
    public void fatalDisconnect(SSH2Transport tp, int reason,
                                String description, String languageTag);

    /**
     * Called when peer disconnects the transport layer for some given
     * reason. See the class <code>SSH2</code> for reason codes.
     *
     * @param tp          the transport layer
     * @param reason      the reason code
     * @param description textual description for reason of disconnect 
     * @param languageTag language tag
     *
     * @see SSH2
     */
    public void peerDisconnect(SSH2Transport tp, int reason,
                               String description, String languageTag);

}
