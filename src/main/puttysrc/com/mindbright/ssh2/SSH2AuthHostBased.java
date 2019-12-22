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
 * This class implements a module for publickey authentication as defined in the
 * userauth protocol spec. It uses the interface <code>SSH2PKISigner</code> to
 * access an abstract PKI signing mechanism (e.g. implemented with simple file
 * based public/private keys without certificates).
 *
 * @see SSH2AuthModule
 * @see SSH2PKISigner
 */
public class SSH2AuthHostBased implements SSH2AuthModule {

    private SSH2PKISigner signer;

    public final static String STANDARD_NAME = "hostbased";

    public SSH2AuthHostBased(SSH2PKISigner signer) {
        this.signer = signer;
    }

    protected SSH2PKISigner getSigner() {
        return signer;
    }

    public String getStandardName() {
        return STANDARD_NAME;
    }

    public SSH2TransportPDU processMethodMessage(SSH2UserAuth userAuth,
                                                 SSH2TransportPDU pdu)
        throws SSH2Exception {
    		String msg = "received unexpected packet of type: " + pdu.getType();
    		userAuth.getTransport().getLog().warning("SSH2AuthHostBased", msg);
    		pdu = null;
    		throw new SSH2FatalException("SSH2AuthHostBased: " + msg);
    }

    public SSH2TransportPDU startAuthentication(SSH2UserAuth userAuth)
        throws SSH2SignatureException {
        SSH2TransportPDU pdu     = userAuth.createUserAuthRequest(STANDARD_NAME);
        SSH2PKISigner    signer  = getSigner();
        byte[]           keyBlob = signer.getPublicKeyBlob();

        pdu.writeString(signer.getAlgorithmName());
        pdu.writeString(keyBlob);
        pdu.writeString(userAuth.getTransport().getLocalHostName());
        pdu.writeUTF8String(System.getProperty("user.name", ""));
        
        signPDU(userAuth, pdu, signer, keyBlob);

        return pdu;
    }

    private void signPDU(SSH2UserAuth userAuth, SSH2TransportPDU targetPDU,
                         SSH2PKISigner signer, byte[] keyBlob)
        throws SSH2SignatureException {
        SSH2TransportPDU sigPDU = targetPDU;

        byte[] sessionId = userAuth.getTransport().getSessionId();

        int    payloadLength = sigPDU.wPos - sigPDU.getPayloadOffset();
        byte[] signData      = new byte[payloadLength + sessionId.length];

        System.arraycopy(sessionId, 0, signData, 0, sessionId.length);
        System.arraycopy(sigPDU.data, sigPDU.getPayloadOffset(),
                         signData, sessionId.length, payloadLength);

        byte[] sig = signer.sign(signData);
        targetPDU.writeString(sig);
    }

    public void clearSensitiveData() {
        signer.clearSensitiveData();
    }

    public boolean retryPointless() {
        return true;
    }
}
