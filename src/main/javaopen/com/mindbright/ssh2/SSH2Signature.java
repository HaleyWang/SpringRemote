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

import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.InvalidKeyException;
import java.security.Signature;
import java.security.SignatureException;

/**
 * Abstract base class for classes implementing the ssh2 signature algorithms.
 */
public abstract class SSH2Signature implements SSH2PKISigner {

    private static Hashtable<String, String> algorithms;

    static {
        algorithms = new Hashtable<String, String>();
        algorithms.put("ssh-dss", SSH2DSS.class.getName());
        algorithms.put("ssh-rsa", SSH2RSA.class.getName());
        algorithms.put("ecdsa-sha2-nistp256", SSH2ECDSASHA2NIST.P256.class.getName());
        algorithms.put("ecdsa-sha2-nistp384", SSH2ECDSASHA2NIST.P384.class.getName());
        algorithms.put("ecdsa-sha2-nistp521", SSH2ECDSASHA2NIST.P521.class.getName());
    };

    protected String     algorithm;
    protected Signature  signature;
    protected PublicKey  publicKey;
    protected byte[]     pubKeyBlob;

    /**
     * Get a <code>SSH2Signature</code> instance suitable for encoding
     * with the given algorithm.
     *
     * @param algorithm The algorithm. Currently the valid values are
     * "ssh-dss" and "ssh-rsa".
     *
     * @return An instance of the apropriate signature class.
     */
    public static SSH2Signature getInstance(String algorithm)
    throws SSH2Exception {
        SSH2Signature impl = getEncodingInstance(algorithm);
        impl.init(algorithm);
        return impl;
    }

    /**
     * Get a <code>SSH2Signature</code> instance suitable for encoding
     * with the given algorithm.
     *
     * @param algorithm The algorithm. Currently the valid values are
     * "ssh-dss" and "ssh-rsa".
     *
     * @return An instance of the apropriate signature class.
     */
    public static SSH2Signature getEncodingInstance(String algorithm)
    throws SSH2Exception {
        SSH2Signature impl      = null;
        String        className = algorithms.get(algorithm);
        try {
            impl = (SSH2Signature)Class.forName(className).newInstance();
        } catch (Exception e) {
            // !!! TODO
            throw new SSH2FatalException("Public key algorithm '" + algorithm +
                                         "' not supported");
        }
        return impl;
    }

    private void init(String algorithm) throws SSH2Exception {
        this.algorithm = algorithm;
        String sigAlg  = getSignatureAlgorithm();
        try {
            signature = com.mindbright.util.Crypto.getSignature(sigAlg);
        } catch (Exception e) {
            // !!! TODO
            throw new SSH2FatalException("Error initializing SSH2Signature: " +
                                         algorithm + "/" + sigAlg + " - " + e);
        }
    }

    /**
     * Constructor.
     */
    protected SSH2Signature() {}

    /**
     * Get the algorithm this instance handles.
     *
     * @return The algorithm name.
     */
    public final String getAlgorithmName() {
        return algorithm;
    }

    /**
     * Get the public key associated with this
     * <code>SSH2Signature</code> object.
     *
     * @return A public key blob.
     */
    public final byte[] getPublicKeyBlob() throws SSH2SignatureException {
        if(pubKeyBlob == null) {
            try {
                pubKeyBlob = encodePublicKey(publicKey);
            } catch (SSH2Exception e) {
                throw new SSH2SignatureException(e.getMessage());
            }
        }
        return pubKeyBlob;
    }

    /**
     * Get the public key associated with this
     * <code>SSH2Signature</code> object.
     *
     * @return A public key object.
     */
    public final PublicKey getPublicKey() throws SSH2SignatureException {
        if(publicKey == null) {
            try {
                publicKey = decodePublicKey(pubKeyBlob);
            } catch (SSH2Exception e) {
                throw new SSH2SignatureException(e.getMessage());
            }
        }
        return publicKey;
    }

    /**
     * Associate a public key with this object.
     *
     * @param publicKey The key to associate.
     */
    public final void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public void setIncompatibility(SSH2Transport transport) {
        // Do nothing here, derived class might be interested...
    }

    /**
     * Prepare for signing with the given private key.
     *
     * @param privateKey Key to use for signing.
     */
    public final void initSign(PrivateKey privateKey) throws SSH2Exception {
        try {
            signature.initSign(privateKey);
        } catch (InvalidKeyException e) {
            throw new SSH2FatalException("SSH2Signature.initSign, invalid key: "
                                         + e.getMessage());
        }
    }

    /**
     * Prepare to verify a signature with the given public key.
     *
     * @param publicKey Key to use when verifying.
     */
    public final void initVerify(PublicKey publicKey) throws SSH2Exception {
        initVerify(encodePublicKey(publicKey));
    }

    /**
     * Prepare to verify a signature with the given public key.
     *
     * @param pubKeyBlob key to use when verifying, encoded as a public
     * key blob.
     */
    public final void initVerify(byte[] pubKeyBlob) throws SSH2Exception {
        this.pubKeyBlob = pubKeyBlob;
        this.publicKey  = decodePublicKey(pubKeyBlob);
        try {
            signature.initVerify(publicKey);
        } catch (InvalidKeyException e) {
            throw new SSH2FatalException("SSH2Signature.initVerify, invalid key: "
                                         + e.getMessage());
        }
    }

    /**
     * Sign the given data. The object must have been initialized for
     * signing first.
     *
     * @param data Data to sign.
     *
     * @return A signature blob encoded in the ssh format.
     */
    public byte[] sign(byte[] data) throws SSH2SignatureException {
        try {
            signature.update(data);
            byte[] sigRaw = signature.sign();
            return encodeSignature(sigRaw);
        } catch (SignatureException e) {
            throw new SSH2SignatureException("Error in " + algorithm +
                                             " sign: " + e.getMessage());
        }
    }

    /**
     * Verify that the given signature matches the given data and the
     * public key. The public key is given in the initialization call.
     *
     * @param sigBlob Signature blob encoded in the ssh format.
     * @param data Signed data.
     *
     * @return True if the signature matches.
     */
    public boolean verify(byte[] sigBlob, byte[] data)
    throws SSH2SignatureException {
        try {
            byte[] sigRaw = decodeSignature(sigBlob);
            signature.update(data);
            return signature.verify(sigRaw);
        } catch (SignatureException e) {
            throw new SSH2SignatureException("Error in " + algorithm +
                                             " verify: " + e.getMessage());
        }
    }

    /**
     * Get the signature algorithm.
     *
     * @return The algorithm name.
     */
    protected abstract String getSignatureAlgorithm();

    /**
     * Encode the given public key into a public key blob.
     *
     * @param publicKey The public key to encode. Must be an instance of
     *                  <code>DSAPublicKey</code>.
     *
     * @return A byte array containing the key suitably encoded.
     */
    protected abstract byte[] encodePublicKey(PublicKey publicKey)
    throws SSH2Exception;

    /**
     * Decode a public key blob.
     *
     * @param pubKeyBlob A byte array containing a public key blob.
     *
     * @return A <code>Publickey</code> instance.
     */
    protected abstract PublicKey decodePublicKey(byte[] pubKeyBlob)
    throws SSH2Exception;

    /**
     * Encode the given, internal form, signature into the ssh standard form.
     *
     * @param sigRaw The raw signature.
     *
     * @return A byte array containing the signature suitably encoded.
     */
    protected abstract byte[] encodeSignature(byte[] sigRaw);

    /**
     * Decode the given signature blob from the ssh standard form to
     * the internal form.
     *
     * @param sigBlob The encoded signature.
     *
     * @return A raw signature blob.
     */
    protected abstract byte[] decodeSignature(byte[] sigBlob)
    throws SSH2SignatureException;

    public void clearSensitiveData() {
        signature = null;
    }
}
