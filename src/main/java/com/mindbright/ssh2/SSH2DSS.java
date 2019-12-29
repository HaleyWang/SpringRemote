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

import java.math.BigInteger;

import com.mindbright.asn1.ASN1Sequence;
import com.mindbright.asn1.ASN1Integer;
import com.mindbright.asn1.ASN1DER;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.spec.DSAPublicKeySpec;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.DSAParams;

/**
 * Implements "ssh-dss" signatures according to the ssh standard.
 */
public final class SSH2DSS extends SSH2SimpleSignature {
    public final static String SSH2_KEY_FORMAT = "ssh-dss";

    /**
     * Constructor.
     */
    public SSH2DSS() {
        super("SHA1withDSA", SSH2_KEY_FORMAT);
    }

    public byte[] encodePublicKey(PublicKey publicKey) throws SSH2Exception {
        SSH2DataBuffer buf = new SSH2DataBuffer(8192);

        if(!(publicKey instanceof DSAPublicKey)) {
            throw new SSH2FatalException("SSH2DSS, invalid public key type: " +
                                         publicKey);
        }

        DSAPublicKey dsaPubKey = (DSAPublicKey)publicKey;
        DSAParams    dsaParams = dsaPubKey.getParams();

        buf.writeString(SSH2_KEY_FORMAT);
        buf.writeBigInt(dsaParams.getP());
        buf.writeBigInt(dsaParams.getQ());
        buf.writeBigInt(dsaParams.getG());
        buf.writeBigInt(dsaPubKey.getY());

        return buf.readRestRaw();
    }

    public PublicKey decodePublicKey(byte[] pubKeyBlob) throws SSH2Exception {
        BigInteger p, q, g, y;
        SSH2DataBuffer buf = new SSH2DataBuffer(pubKeyBlob.length);

        buf.writeRaw(pubKeyBlob);

        String type = buf.readJavaString();
        if(!type.equals(SSH2_KEY_FORMAT)) {
            throw new SSH2FatalException("SSH2DSS, keyblob type mismatch, got '"
                                         + type + ", (expected '" +
                                         SSH2_KEY_FORMAT + "')");
        }

        p = buf.readBigInt();
        q = buf.readBigInt();
        g = buf.readBigInt();
        y = buf.readBigInt();

        try {
            KeyFactory       dsaKeyFact = com.mindbright.util.Crypto.getKeyFactory("DSA");
            DSAPublicKeySpec dsaPubSpec = new DSAPublicKeySpec(y, p, q, g);
            return dsaKeyFact.generatePublic(dsaPubSpec);
        } catch (Exception e) {
            throw new SSH2FatalException("SSH2DSS, error decoding public key blob: " + e);
        }
    }


    public byte[] sign(byte[] data) throws SSH2SignatureException {
        try {
            signature.update(data);
            byte[] sigRaw = signature.sign();

            try {
                DSASIG sign = new DSASIG();
                ASN1DER der = new ASN1DER();
                ByteArrayInputStream dec = new ByteArrayInputStream(sigRaw);
                der.decode(dec, sign);
                sigRaw = new byte[40];
                byte[] tmp = unsignedBigIntToBytes(sign.r.getValue(), 20);
                System.arraycopy(tmp, 0, sigRaw, 0, 20);
                tmp = unsignedBigIntToBytes(sign.s.getValue(), 20);
                System.arraycopy(tmp, 0, sigRaw, 20, 20);
            } catch (IOException ioe) {
                throw new SSH2SignatureException("DER decode failed: " + ioe.getMessage());
            }
            return encodeSignature(sigRaw);
        } catch (SignatureException e) {
            throw new SSH2SignatureException("Error in " + algorithm +
                                             " sign: " + e.getMessage());
        }
    }

    public boolean verify(byte[] sigBlob, byte[] data)
        throws SSH2SignatureException {

        try {
            byte[] sigRaw = decodeSignature(sigBlob);
            signature.update(data);

            int slen = sigRaw.length / 2;
            byte[] ra = new byte[slen];
            byte[] sa = new byte[slen];

            System.arraycopy(sigRaw, 0, ra, 0, slen);
            System.arraycopy(sigRaw, slen, sa, 0, slen);

            BigInteger r  = new BigInteger(1, ra);
            BigInteger s  = new BigInteger(1, sa);

            DSASIG dsasig = new DSASIG(r, s);
            ByteArrayOutputStream enc = new ByteArrayOutputStream(128);
            ASN1DER der = new ASN1DER();
            try {
                der.encode(enc, dsasig);
            } catch (IOException ioe) {
                throw new SSH2SignatureException("DER encode failed: " + ioe.getMessage());
            }
            return signature.verify(enc.toByteArray());
        } catch (SignatureException e) {
            throw new SSH2SignatureException("Error in " + algorithm +
                                             " verify: " + e.getMessage());
        }
    }
    

   private static byte[] unsignedBigIntToBytes(BigInteger bi, int size) {
        byte[] tmp  = bi.toByteArray();
        byte[] tmp2 = null;
        if(tmp.length > size) {
            tmp2 = new byte[size];
            System.arraycopy(tmp, tmp.length - size, tmp2, 0, size);
        } else if(tmp.length < size) {
            tmp2 = new byte[size];
            System.arraycopy(tmp, 0, tmp2, size - tmp.length, tmp.length);
        } else {
            tmp2 = tmp;
        }
        return tmp2;
    }
    
    public static final class DSASIG extends ASN1Sequence {
        
        public ASN1Integer r;
        public ASN1Integer s;
        
        public DSASIG() {
            r = new ASN1Integer();
            s = new ASN1Integer();
            addComponent(r);
            addComponent(s);
        }
        
        public DSASIG(BigInteger r, BigInteger s) {
            this();
            this.r.setValue(r);
            this.s.setValue(s);
        }
    }
}
