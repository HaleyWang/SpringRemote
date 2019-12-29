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
 * This interface is a simple abstraction of a PKI signing mechanism. An
 * implementation of this interface can use certificates or plain public keys,
 * this is something which is defined by the ssh2 specific algorithm name used
 * to identify it.
 *
 * @see SSH2AuthPublicKey
 */
public interface SSH2PKISigner {
    /**
     * Get the algorithm name.
     *
     * @return The algorithm name.
     */
    public String getAlgorithmName();

    /**
     * Get the public key blob encoded according to the ssh standard.
     *
     * @return A byte array containing the public key.
     */
    public byte[] getPublicKeyBlob() throws SSH2SignatureException;

    /**
     * Sign a blob of data.
     *
     * @param data The data to be signed.
     *
     * @return The signature, encoded according to the ssh standard.
     */
    public byte[] sign(byte[] data) throws SSH2SignatureException;

    /**
     * Set eventual incompatibility modes depending on the remote end.
     * Some older ssh implementations use slightly incompatible algorithms
     * when signing data.
     *
     * @param transport An <code>SSH2Transport</code> object which identifies the
     *        other end.
     */
    public void setIncompatibility(SSH2Transport transport);

    /**
     * Try to remove any sensitive data from memory.
     */
    public void clearSensitiveData();
}
