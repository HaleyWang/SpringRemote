/******************************************************************************
 *
 * Copyright (c) 2010-2011 Cryptzone Group AB. All Rights Reserved.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.math.BigInteger;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPublicKeySpec;

import com.mindbright.asn1.ASN1DER;
import com.mindbright.util.Crypto;

//
// Base class for ECC Public Keys as defined in RFC 5656
//
public abstract class SSH2ECDSASHA2NIST extends SSH2SimpleSignature 
{
    private String curve;

    public SSH2ECDSASHA2NIST(String algo, String sshname, String curve) {
        super(algo, sshname);
        this.curve = curve;
    }
    
    public static class P256 extends SSH2ECDSASHA2NIST {
        public P256() {
            super("SHA256/ECDSA", "ecdsa-sha2-nistp256", "secp256r1");
        }
    }

    public static class P384 extends SSH2ECDSASHA2NIST {
        public P384() {
            super("SHA384/ECDSA", "ecdsa-sha2-nistp384", "secp384r1");
        }
    }

    public static class P521 extends SSH2ECDSASHA2NIST {
        public P521() {
            super("SHA512/ECDSA", "ecdsa-sha2-nistp521", "secp521r1");
        }
    }

    public byte[] sign(byte[] data) throws SSH2SignatureException {
        try {
            signature.update(data);
            byte[] sigRaw = signature.sign();
            try {
                SSH2DSS.DSASIG sign = new SSH2DSS.DSASIG();
                ASN1DER der = new ASN1DER();
                ByteArrayInputStream dec = new ByteArrayInputStream(sigRaw);
                der.decode(dec, sign);
                SSH2DataBuffer buf = new SSH2DataBuffer(256);
                buf.writeBigInt(sign.r.getValue());
                buf.writeBigInt(sign.s.getValue());
                sigRaw = buf.readRestRaw();
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
            signature.update(data);
            byte[] sigRaw = decodeSignature(sigBlob);
            SSH2DataBuffer buf = new SSH2DataBuffer(sigRaw.length);
            buf.writeRaw(sigRaw);
            BigInteger r = buf.readBigInt();
            BigInteger s = buf.readBigInt();
            SSH2DSS.DSASIG dsasig = new SSH2DSS.DSASIG(r, s);
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

    protected byte[] encodePublicKey(PublicKey publicKey)
        throws SSH2Exception {
        try {
            ECPublicKey pk = (ECPublicKey)publicKey;
            SSH2DataBuffer blob = new SSH2DataBuffer(256);
            blob.writeString(ssh2KeyFormat);
            blob.writeString(ssh2KeyFormat.substring(ssh2KeyFormat.lastIndexOf("-") + 1));
            blob.writeString(SSH2KEXECDHSHA2NIST.tobytes(pk.getW(), pk.getParams().getCurve()));
            return blob.readRestRaw();
        } catch (Throwable t) {
            throw new SSH2FatalException("Failed to encode public key");
        }
    }

    public static ECParameterSpec getParamsForCurve(String curve) {
        try { 
             // This is annoying. We need to generate a keypair to be able to figure out the
            // correct parameters to use when generating the public key...
            KeyPairGenerator kpg = Crypto.getKeyPairGenerator("EC");
            kpg.initialize(new ECGenParameterSpec(curve), Crypto.getSecureRandom());
            KeyPair dummykp = kpg.generateKeyPair();
            return ((ECPublicKey)dummykp.getPublic()).getParams();
        } catch (Throwable t) {
        }
        return null;
    }
    
    protected PublicKey decodePublicKey(byte[] pubKeyBlob)
        throws SSH2Exception {
        try { 
            SSH2DataBuffer buf = new SSH2DataBuffer(pubKeyBlob.length);
            buf.writeRaw(pubKeyBlob);
            String type = buf.readJavaString();
            if (!type.equals(ssh2KeyFormat)) 
                throw new SSH2FatalException("SSH2ECDSAHSA2NIST, keyblob type mismatch, got '"
                                             + type + ", (expected '" + ssh2KeyFormat + "')");
            buf.readJavaString(); // skip nistpX string
            byte[] Q = buf.readString();

            ECParameterSpec ecspec = getParamsForCurve(curve);
            ECPoint p = SSH2KEXECDHSHA2NIST.frombytes(Q, ecspec.getCurve());
            KeyFactory kf = Crypto.getKeyFactory("EC");
            return kf.generatePublic(new ECPublicKeySpec(p, ecspec));
        } catch (Throwable t) {
            throw new SSH2FatalException("Failed to decode public key blob");
        }
    }
    
}
