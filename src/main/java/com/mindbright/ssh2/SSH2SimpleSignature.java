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
 * Handle signatures for the ssh draft standard algorithms.
 */
public abstract class SSH2SimpleSignature extends SSH2Signature {

    protected String signatureAlgorithm;
    protected String ssh2KeyFormat;

    protected boolean draftIncompatibleSignature;

    /**
     * Constructor.
     *
     * @param signatureAlgorithm The algorithm name to use when looking in
     *                           the crypto provider for the implementation.
     *
     * @param ssh2KeyFormat The ssh2 name for this algorithm.
     */
    protected SSH2SimpleSignature(String signatureAlgorithm,
                                  String ssh2KeyFormat) {
        super();
        this.signatureAlgorithm = signatureAlgorithm;
        this.ssh2KeyFormat      = ssh2KeyFormat;
    }

    /**
     * Get an instance of <code>SSH2Signature</code> which is prepared to
     * do a verify operation which the given public key.
     *
     * @param pubKeyBlob Blob containing the public key to use.
     */
    public static SSH2Signature getVerifyInstance(byte[] pubKeyBlob)
    throws SSH2Exception {
        String keyFormat        = getKeyFormat(pubKeyBlob);
        SSH2Signature signature = SSH2Signature.getInstance(keyFormat);
        signature.initVerify(pubKeyBlob);
        return signature;
    }

    /**
     * Get the signature algorithm name. Used internally to find the
     * right implementation of the algorithm.
     *
     * @return The name of the algorithm.
     */
    public final String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    /**
     * Encode the given signature into the form specified by the ssh
     * standard.
     *
     * @param sigRaw The signature as a raw byte array.
     *
     * @return The encoded signature as a byte array.
     */
    public byte[] encodeSignature(byte[] sigRaw) {
        if(draftIncompatibleSignature) {
            return sigRaw;
        }
	SSH2DataBuffer buf = new SSH2DataBuffer(sigRaw.length + 4 +
						ssh2KeyFormat.length() + 4);
	buf.writeString(ssh2KeyFormat);
	buf.writeString(sigRaw);
	return buf.readRestRaw();
    }

    /**
     * Decode a signature encoded according to the ssh standard.
     *
     * @param sigBlob a signature blob encoded according to the ssh standard.
     *
     * @return The raw signature
     */
    public byte[] decodeSignature(byte[] sigBlob)
    throws SSH2SignatureException {
        if(draftIncompatibleSignature) {
            return sigBlob;
        }
	SSH2DataBuffer buf = new SSH2DataBuffer(sigBlob.length);
	buf.writeRaw(sigBlob);
	
	int len = buf.readInt();
	if(len <= 0 || len > sigBlob.length) {
	    // This is probably an undetected buggy implemenation
	    // !!! TODO: might want to report this...
	    return sigBlob;
	}
	
	buf.setRPos(buf.getRPos() - 4); // undo above readInt
	
	String type = buf.readJavaString();
	if(!type.equals(ssh2KeyFormat)) {
	    throw new SSH2SignatureException(ssh2KeyFormat +
					     ", signature blob type " +
					     "mismatch, got '" + type);
	}
	
	return buf.readString();
    }

    /**
     * Get the key format of the given public key.
     *
     * @param pubKeyBlob the public key blob (encoded accoring to the
     *                   ssh standard).
     *
     * @return The ssh name of the key format.
     */
    public static String getKeyFormat(byte[] pubKeyBlob) {
        SSH2DataBuffer buf = new SSH2DataBuffer(pubKeyBlob.length);
        buf.writeRaw(pubKeyBlob);
        return buf.readJavaString();
    }

    /**
     * Set the appropriate incompatibility mode which depends on the
     * peer version.
     *
     * @param transport <code>SSH2Transport</code> object which
     * identifies the peer.
     */
    public void setIncompatibility(SSH2Transport transport) {
        draftIncompatibleSignature = transport.incompatibleSignature;
    }

}

