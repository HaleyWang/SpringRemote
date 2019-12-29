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
 * Defines an authentication module implementing an
 * authentication method (as defined in the userauth protocol
 * spec). Since authentication methods can be very different this
 * interface is very simple and the class implementing it must handle
 * the processing and formatting of the packets specific to the
 * authentication method it implements. Each authentication module is
 * associated with an authentication method name when adding it to the
 * <code>SSH2Authenticator</code> with which it should be used.
 * <p>
 * An authentication module is started when <code>SSH2UserAuth</code>
 * calls its <code>startAuthentication</code> method which must return
 * the first packet to send to the peer to initiate this
 * authentication method. After this the module gets all incoming
 * method specific packets passed to its
 * <code>processMethodMessage</code> method. This method is expected
 * to return formatted packets which the peer end of the
 * authentication wants in response. Exceptions that occur during the
 * execution of a module is reported to the corresponding
 * <code>SSH2Authenticator</code>. The special exception
 * <code>SSH2UserCancelException</code> can be thrown to indicate that
 * the user canceled this authentication method and that
 * authentication should not continue.
 * <p>
 * Request packets can created with the convenience method
 * <code>createUserAuthRequest</code> in
 * <code>SSH2UserAuth</code>. Method specific packets are created with
 * the ordinary <code>createOutgoingPacket</code> method in
 * <code>SSH2TransportPDU</code>.
 *
 * @see SSH2UserAuth
 * @see SSH2Authenticator
 * @see SSH2TransportPDU
 */
public interface SSH2AuthModule {

    /**
     * Should give the standard name of this module as used in the userauth
     * protocol.
     *
     * @return the standard name of the module
     */
    public String getStandardName();

    /**
     * Starts the execution of this module, called from the given
     * <code>SSH2UserAuth</code>.
     *
     * @param userAuth the authentication layer responsible
     *
     * @return the packet which starts this authentication method
     * (i.e. when sent to peer)
     *
     * @exception SSH2Exception if an error occurs
     */
    public SSH2TransportPDU startAuthentication(SSH2UserAuth userAuth)
    throws SSH2Exception;

    /**
     * Processes the given method specific packet and returns a new
     * packet which will be sent to peer to continue the authentication.
     *
     * @param userAuth the authentication layer responsible
     * @param pdu      the method specific packet
     *
     * @return the new packet to send to peer, or <code>null</code> if no packet
     * should be sent.
     *
     * @exception SSH2Exception if an error occurs
     */
    public SSH2TransportPDU processMethodMessage(SSH2UserAuth userAuth,
                                                 SSH2TransportPDU pdu)
    throws SSH2Exception;

    /**
     * Clean up any sensitive data in this authentication module. This
     * should be called once the authentication is done.
     *
     */
    public void clearSensitiveData();

    /**
     * Returns true if there is not any point in trying this method again
     * after it has failed once.
     */
    public boolean retryPointless();
}
