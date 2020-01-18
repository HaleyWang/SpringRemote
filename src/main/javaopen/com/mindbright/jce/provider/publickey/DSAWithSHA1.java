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

package com.mindbright.jce.provider.publickey;

import java.math.BigInteger;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

import com.mindbright.asn1.ASN1Sequence;
import com.mindbright.asn1.ASN1Integer;
import com.mindbright.asn1.ASN1DER;

import java.security.InvalidKeyException;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.DSAPrivateKey;


public final class DSAWithSHA1 extends BaseSignature {

    public DSAWithSHA1() {
        super("SHA1");
    }

    protected void initVerify() throws InvalidKeyException {
        if(publicKey == null || !(publicKey instanceof DSAPublicKey)) {
            throw new InvalidKeyException("Wrong key for DSAWithSHA1 verify: " +
                                          publicKey);
        }
    }

    protected void initSign() throws InvalidKeyException {
        if(privateKey == null || !(privateKey instanceof DSAPrivateKey)) {
            throw new InvalidKeyException("Wrong key for DSAWithSHA1 sign: " +
                                          privateKey);
        }
    }

    /**
     * DSA_SIG ::= SEQUENCE {
     *   r        INTEGER,
     *   s        INTEGER,
     * }
     */
    public static final class DSASIG extends ASN1Sequence {

        public ASN1Integer r;
        public ASN1Integer s;

        public DSASIG() {
            r       = new ASN1Integer();
            s       = new ASN1Integer();
            addComponent(r);
            addComponent(s);
        }

        public DSASIG(BigInteger r, BigInteger s) {
            this();
            this.r.setValue(r);
            this.s.setValue(s);
        }

    }


    protected byte[] sign(byte[] data) {
        DSAPrivateKey key  = (DSAPrivateKey)privateKey;
        DSAParams     parm = key.getParams();
        BigInteger    x    = key.getX();
        BigInteger    p    = parm.getP();
        BigInteger    q    = parm.getQ();
        BigInteger    g    = parm.getG();
        BigInteger[]  sign = DSAAlgorithm.sign(x, p, q, g, data);
        
        if (sign == null || sign.length != 2) {
            return null;
        }
        BigInteger r = sign[0];
        BigInteger s = sign[1];

        //  Encode
        DSASIG dsasig= new DSASIG(r, s);
        ByteArrayOutputStream enc = new ByteArrayOutputStream(128);
        ASN1DER               der = new ASN1DER();

        try {
            der.encode(enc, dsasig);
        } catch (IOException e) {
            // This should not happen
        }

        return enc.toByteArray();
    }

    protected boolean verify(byte[] signature, byte[] data) {
        DSAPublicKey key  = (DSAPublicKey)publicKey;
        DSAParams    parm = key.getParams();
        BigInteger   y    = key.getY();
        BigInteger   p    = parm.getP();
        BigInteger   q    = parm.getQ();
        BigInteger   g    = parm.getG();

        DSASIG sign = new DSASIG();
        try {
            ASN1DER der = new ASN1DER();
            ByteArrayInputStream dec = new ByteArrayInputStream(signature);
            der.decode(dec, sign);
        } catch (IOException e) {
            // This should not happen
            System.err.println("DSAWithSHA1.verify: " + e);
            return false;
        }

        return DSAAlgorithm.verify(y, p, q, g,
                                   sign.r.getValue(), sign.s.getValue(), data);
    }

}
