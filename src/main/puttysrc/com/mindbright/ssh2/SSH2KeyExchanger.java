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

import java.util.Hashtable;

import java.security.MessageDigest;

/**
 * Base class for implementing ssh key exchange algorithms.
 */
public abstract class SSH2KeyExchanger {

    private static Hashtable<String, String> algorithms;

    static {
        algorithms = new Hashtable<String, String>();

        algorithms.put("diffie-hellman-group1-sha1",  SSH2KEXDHGroup1SHA1.class.getName());
        algorithms.put("diffie-hellman-group14-sha1", SSH2KEXDHGroup14SHA1.class.getName());
        algorithms.put("diffie-hellman-group-exchange-sha1",   SSH2KEXDHGroupXSHA1.class.getName());
        algorithms.put("diffie-hellman-group-exchange-sha256", SSH2KEXDHGroupXSHA256.class.getName());
        algorithms.put("ecdh-sha2-nistp256", SSH2KEXECDHSHA2NIST.P256.class.getName());
        algorithms.put("ecdh-sha2-nistp384", SSH2KEXECDHSHA2NIST.P384.class.getName());
        algorithms.put("ecdh-sha2-nistp521", SSH2KEXECDHSHA2NIST.P521.class.getName());                       
    }

    protected SSH2KeyExchanger() {}

    public static SSH2KeyExchanger getInstance(String algorithm)
    throws SSH2KEXFailedException {
        String           alg = algorithms.get(algorithm);
        SSH2KeyExchanger kex = null;
        if(alg != null) {
            try {
                Class<?> c = Class.forName(alg);
                kex = (SSH2KeyExchanger)c.newInstance();
            } catch (Throwable t) {
                kex = null;
            }
        }
        if(kex == null) {
            throw new SSH2KEXFailedException("Unknown kex algorithm: " +
                                             algorithm);
        }
        return kex;
    }

    public abstract void init(SSH2Transport transport) throws SSH2Exception;

    public abstract void processKEXMethodPDU(SSH2TransportPDU pdu)
    throws SSH2Exception;

    public abstract MessageDigest getExchangeHashAlgorithm();

    public abstract byte[] getSharedSecret_K();

    public abstract byte[] getExchangeHash_H();

    public String getHostKeyAlgorithms() {
        // If we implement RFC 6239, these should vary depending on our algorithm
        if (com.mindbright.util.Crypto.hasECDSASupport())
            return SSH2Preferences.DEFAULT_HOST_KEY_ALGS_EC;
        return SSH2Preferences.DEFAULT_HOST_KEY_ALGS;
    }
}
