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


import java.security.PublicKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;

/**
 * This class is an adapter for the interface
 * <code>SSH2TransportEventHandler</code>.
 *
 * @see SSH2TransportEventHandler
 */
public class SSH2HostKeyVerifier extends SSH2TransportEventAdapter {

    protected String    fingerprint;
    protected PublicKey publickey;

    /**
     * Create an instance which will verify that the hostkey matches
     * the given public key.
     *
     * @param publickey The public key to verify against.
     */
    public SSH2HostKeyVerifier(PublicKey publickey) {
        this.publickey = publickey;
    }

    /**
     * Create an instance which will verify that the hostkey matches
     * a public key with the given fingerprint.
     *
     * @param fingerprint The fingerprint which should match the public key.
     */
    public SSH2HostKeyVerifier(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    /**
     * Perform the authenticate host stage of key exchange.
     *
     * @param tp Indentifies the <code>SSH2Transport</code> object
     * handling the connection.
     * @param serverHostKey Signature object which holds the server keys.
     *
     * @return True if the keys match.
     */
    public boolean kexAuthenticateHost(SSH2Transport tp,
                                       SSH2Signature serverHostKey) {
        boolean authenticated = false;
        if(publickey != null) {
            try {
                authenticated = comparePublicKeys(publickey,
                                                  serverHostKey.getPublicKey());
            } catch (SSH2SignatureException e) {
                authenticated = false;
            }
        } else if(fingerprint != null) {
            authenticated = compareFingerprints(fingerprint, serverHostKey);
        }
        return authenticated;
    }

    /**
     * Static utility function which can be used to compare a server key
     * against a fingerprint.
     *
     * @param fingerprint The fingerprint to check.
     * @param serverHostKey Signature object which holds the server keys.
     *
     * @return True if the server key generates an identical
     *         fingerprint as the one we are comparing against.
     */
    public static boolean compareFingerprints(String fingerprint,
            SSH2Signature serverHostKey) {
        byte[] blob  = null;
        try {
            blob = serverHostKey.getPublicKeyBlob();
        } catch (SSH2SignatureException e) {
            return false;
        }
        String fpMD5Hex = SSH2KeyFingerprint.md5Hex(blob);
        String fpBubble = SSH2KeyFingerprint.bubbleBabble(blob);
        if(fpMD5Hex.equalsIgnoreCase(fingerprint) ||
                fpBubble.equalsIgnoreCase(fingerprint)) {
            return true;
        }
        return false;
    }

    /**
     * Static utility functions which can compare two public keys.
     *
     * @param p1 Public key to compare.
     * @param p2 Public key to compare.
     *
     * @return True if they are identical.
     */
    public static boolean comparePublicKeys(PublicKey p1, PublicKey p2) {
        if((p1 instanceof DSAPublicKey) &&
                (p2 instanceof DSAPublicKey)) {
            DSAPublicKey dsa1 = (DSAPublicKey)p1;
            DSAPublicKey dsa2 = (DSAPublicKey)p2;
            if(dsa1.getY().equals(dsa2.getY()) &&
                    dsa1.getParams().getG().equals(dsa2.getParams().getG())
                    &&
                    dsa1.getParams().getP().equals(dsa2.getParams().getP())
                    &&
                    dsa1.getParams().getQ().equals(dsa2.getParams().getQ()))
                return true;
        } else if((p1 instanceof RSAPublicKey) &&
                  (p2 instanceof RSAPublicKey)) {
            RSAPublicKey rsa1 = (RSAPublicKey)p1;
            RSAPublicKey rsa2 = (RSAPublicKey)p2;
            if(rsa1.getPublicExponent().equals(rsa2.getPublicExponent()) &&
                    rsa1.getModulus().equals(rsa2.getModulus()))
                return true;
        } else if ((p1 instanceof ECPublicKey) &&
                   (p2 instanceof ECPublicKey)) {
            ECPublicKey ec1 = (ECPublicKey)p1;
            ECPublicKey ec2 = (ECPublicKey)p2;
            if (ec1.getW().getAffineX().equals(ec2.getW().getAffineX()) &&
                ec1.getW().getAffineY().equals(ec2.getW().getAffineY()))
                return true;
        }
        
        return false;
    }


}
