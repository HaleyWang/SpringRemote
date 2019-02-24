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

import java.math.BigInteger;

import java.security.MessageDigest;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECFieldFp;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.EllipticCurve;

import javax.crypto.KeyAgreement;

import com.mindbright.util.Crypto;

/**
 * Base class for ECC DH key exchange as defined in RFC 5656
 */
public abstract class SSH2KEXECDHSHA2NIST extends SSH2KeyExchanger {

    protected SSH2Transport transport;
    protected ECPublicKey   ecPublicKey;
    protected ECPrivateKey  ecPrivateKey;
    protected byte[]        serverHostKey;
    protected ECPoint       serverF;
    protected ECPoint       clientE;
    protected byte[]        sharedSecret_K;
    protected byte[]        exchangeHash_H;
    protected MessageDigest sha2;
    protected KeyPair       serverKey;

    public abstract String getName();
    public abstract String getECName();
    public abstract String getHashName();
    
    public static class P256 extends SSH2KEXECDHSHA2NIST {
        private final String C[] = new String [] {    
            "ecdh-sha2-nistp256", "secp256r1", "SHA-256"
        };
        public String getName()     { return C[0]; } 
        public String getECName()   { return C[1]; } 
        public String getHashName() { return C[2]; } 
    }

    public static class P384 extends SSH2KEXECDHSHA2NIST {
        private final String C[] = new String [] {    
            "ecdh-sha2-nistp384", "secp384r1", "SHA-384"
        };
        public String getName()     { return C[0]; } 
        public String getECName()   { return C[1]; } 
        public String getHashName() { return C[2]; } 
    }

    public static class P521 extends SSH2KEXECDHSHA2NIST {
        private final String C[] = new String [] {    
            "ecdh-sha2-nistp521", "secp521r1", "SHA-512"
        };
        public String getName()     { return C[0]; } 
        public String getECName()   { return C[1]; } 
        public String getHashName() { return C[2]; } 
    }
    

    public void init(SSH2Transport transport)
	throws SSH2Exception {
	init(transport, null);
    }

    public void init(SSH2Transport transport, KeyPair serverKey)
	throws SSH2Exception {
	this.transport = transport;
	this.sha2      = createHash();
	this.serverKey = serverKey;

        generateECKeyPair();

        if(!transport.isServer()) {
            sendECDHINIT(SSH2.MSG_KEX_ECDH_INIT);
        }
    }

    public static byte[] tobytes(ECPoint e, EllipticCurve curve) {
        byte[] x = e.getAffineX().toByteArray();
        byte[] y = e.getAffineY().toByteArray();
        int i, xoff = 0, yoff = 0;
        for (i=0; i < x.length - 1; i++)
            if (x[i] != 0) { xoff = i; break; }
        for (i=0; i < y.length - 1; i++)
            if (y[i] != 0) { yoff = i; break; }
        int len = (curve.getField().getFieldSize() + 7) / 8;
        if ( (x.length - xoff) > len || (y.length - yoff) > len)
            return null;
        byte[] ret = new byte[len*2 + 1];
        ret[0] = 4; // no compression
        System.arraycopy(x, xoff, ret, 1 + len - (x.length - xoff), x.length - xoff);
        System.arraycopy(y, yoff, ret, ret.length - (y.length - yoff), y.length - yoff);
        return ret;        
    }

    public static ECPoint frombytes(byte[] b, EllipticCurve curve) {
        int len = (curve.getField().getFieldSize() + 7) / 8;
        if (b.length != 2*len + 1 || b[0] != 4)
            return null;
        byte[] x = new byte[len];
        byte[] y = new byte[len];
        System.arraycopy(b, 1, x, 0, len);
        System.arraycopy(b, len + 1, y, 0, len);
        return new ECPoint(new BigInteger(1, x), new BigInteger(1, y));
    }
    
    private boolean validate(ECPoint q) {
        /// Elliptic Curve public Key Partial Validation as defined in
        //  SEC 1: Elliptic Curve Cryptography, Chapter 3.2.3

        //// 1. Check that Q != infinity
        if (q == null || q.equals(ECPoint.POINT_INFINITY)) // frombytes() would have returned null
            return false;


        //// 2. Check that

        ////     1) Qx and Qy are in interval [0,p-1] , and
        BigInteger p = ((ECFieldFp)ecPrivateKey.getParams().getCurve().getField()).getP();
        BigInteger x = q.getAffineX();
        if (x.compareTo(BigInteger.ZERO) < 0 || x.compareTo(p) >= 0) 
            return false;
        
        BigInteger y = q.getAffineY();
        if (y.compareTo(BigInteger.ZERO) < 0 || y.compareTo(p) >= 0)
            return false;
    
        ////     2) Qy^2 = Qx^3+a*Qx+b (mod p)
        BigInteger y2 = y.modPow(BigInteger.valueOf(2), p);
        BigInteger a  = ecPrivateKey.getParams().getCurve().getA();
        BigInteger b  = ecPrivateKey.getParams().getCurve().getB();
        BigInteger x3 = x.modPow(BigInteger.valueOf(3), p).add(x.multiply(a)).add(b).mod(p);
        if (y2.compareTo(x3) != 0) 
            return false;

        // alright, we are good
        return true;
    }

    public void processKEXMethodPDU(SSH2TransportPDU pdu)
	throws SSH2Exception {
	if(pdu.getType() == SSH2.MSG_KEX_ECDH_REPLY) {
	    if(transport.isServer()) {
		throw new SSH2KEXFailedException("Unexpected KEX_ECDH_REPLY");
	    }

            serverHostKey      = pdu.readString();
            serverF            = frombytes(pdu.readString(), ecPrivateKey.getParams().getCurve());
            if (!validate(serverF))
                throw new SSH2KEXFailedException("Failed to verify server ephemeral public key");
            byte[] serverSigH  = pdu.readString();

            computeSharedSecret_K(serverF);
            computeExchangeHash_H();

            transport.authenticateHost(serverHostKey, serverSigH, exchangeHash_H);
            transport.sendNewKeys();
	} else if(pdu.getType() == SSH2.MSG_KEX_ECDH_INIT) {
	    if(!transport.isServer()) {
		throw new SSH2KEXFailedException("Unexpected KEX_ECDH_INIT");
	    }

	    clientE = frombytes(pdu.readString(), ecPrivateKey.getParams().getCurve());
            if (!validate(clientE))
                throw new SSH2KEXFailedException("Failed to verify client ephemeral public key");

	    computeSharedSecret_K(clientE);

	    String sigalg = null;
	    if(serverKey.getPublic() instanceof DSAPublicKey) {
		sigalg = "ssh-dss";
	    } else if(serverKey.getPublic() instanceof RSAPublicKey) {
		sigalg = "ssh-rsa";
	    }
	    SSH2Signature signature = SSH2Signature.getInstance(sigalg);
	    signature.setPublicKey(serverKey.getPublic());
	    signature.initSign(serverKey.getPrivate());

	    serverHostKey = signature.getPublicKeyBlob();

	    computeExchangeHash_H();

	    SSH2TransportPDU out =
		SSH2TransportPDU.createOutgoingPacket(SSH2.MSG_KEX_ECDH_REPLY);

	    out.writeString(serverHostKey);
	    out.writeString(tobytes(ecPublicKey.getW(), ecPrivateKey.getParams().getCurve()));
	    out.writeString(signature.sign(exchangeHash_H));
	    transport.transmitInternal(out);

	    transport.sendNewKeys();
	}
    }

    public MessageDigest getExchangeHashAlgorithm() {
        sha2.reset();
        return sha2;
    }

    public byte[] getSharedSecret_K() {
        SSH2DataBuffer buf = new SSH2DataBuffer(128);
        buf.writeString(sharedSecret_K);
        return buf.readRestRaw();
    }

    public byte[] getExchangeHash_H() {
        return exchangeHash_H;
    }

    protected void computeExchangeHash_H() {
        SSH2DataBuffer buf = new SSH2DataBuffer(64*1024);

        if(transport.isServer()) {
            serverF = ecPublicKey.getW();
        } else {
            clientE = ecPublicKey.getW();
        }

        buf.writeString(transport.getClientVersion());
        buf.writeString(transport.getServerVersion());
        buf.writeString(transport.getClientKEXINITPDU().getData(),
                        transport.getClientKEXINITPDU().getPayloadOffset(),
                        transport.getClientKEXINITPDU().getPayloadLength());
        buf.writeString(transport.getServerKEXINITPDU().getData(),
                        transport.getServerKEXINITPDU().getPayloadOffset(),
                        transport.getServerKEXINITPDU().getPayloadLength());
        buf.writeString(serverHostKey);
        buf.writeString(tobytes(clientE, ecPrivateKey.getParams().getCurve()));
        buf.writeString(tobytes(serverF, ecPrivateKey.getParams().getCurve()));
        buf.writeString(sharedSecret_K);

        sha2.reset();
        sha2.update(buf.getData(), 0, buf.getWPos());
        exchangeHash_H = sha2.digest();

        transport.getLog().debug2(getName(),
                                  "computeExchangeHash_H", "E: ",
                                  tobytes(clientE, ecPrivateKey.getParams().getCurve()));
        transport.getLog().debug2(getName(),
                                  "computeExchangeHash_H", "F: ",
                                  tobytes(serverF, ecPrivateKey.getParams().getCurve()));
        transport.getLog().debug2(getName(),
                                  "computeExchangeHash_H", "K: ",
                                  sharedSecret_K);
        transport.getLog().debug2(getName(),
                                  "computeExchangeHash_H", "Hash over: ",
                                  buf.getData(), 0, buf.getWPos());
        transport.getLog().debug2(getName(),
                                  "computeExchangeHash_H", "H: ",
                                  exchangeHash_H);
    }

    protected void computeSharedSecret_K(ECPoint point)
    throws SSH2Exception {
        try {
            KeyFactory   ecKeyFact = Crypto.getKeyFactory("EC");
            KeyAgreement ecdhKEX   = Crypto.getKeyAgreement("ECDH");

            ECPublicKey  peerPubKey =
                (ECPublicKey)ecKeyFact.generatePublic
                (new ECPublicKeySpec(point, ecPrivateKey.getParams()));

            ecdhKEX.init(ecPrivateKey);
            ecdhKEX.doPhase(peerPubKey, true);
            sharedSecret_K = ecdhKEX.generateSecret();

            if ((sharedSecret_K[0] & 0x80) == 0x80) {
                byte[] tmp = sharedSecret_K;
                sharedSecret_K = new byte[sharedSecret_K.length+1];
                sharedSecret_K[0] = 0x00;
                System.arraycopy(tmp, 0, sharedSecret_K, 1, tmp.length);
            }
           sharedSecret_K = (new BigInteger(1, sharedSecret_K)).toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            throw new SSH2FatalException("Error computing shared secret: "
                                         + e);
        }
    }

    protected void sendECDHINIT(int type) throws SSH2Exception {
        SSH2TransportPDU pdu =
            SSH2TransportPDU.createOutgoingPacket(type);
        pdu.writeString(tobytes(ecPublicKey.getW(), ecPrivateKey.getParams().getCurve()));
        transport.transmitInternal(pdu);
    }

    protected MessageDigest createHash() throws SSH2Exception {
        try {
            return Crypto.getMessageDigest(getHashName());
        } catch (Exception e) {
            throw new SSH2KEXFailedException(getHashName() + " not implemented", e);
        }
    }

    protected void generateECKeyPair()
	throws SSH2Exception {
	try {
	    KeyPairGenerator ecKeyPairGen = Crypto.getKeyPairGenerator("EC");
            ecKeyPairGen.initialize(new ECGenParameterSpec(getECName()), transport.getSecureRandom());

            KeyPair ecKeyPair = ecKeyPairGen.generateKeyPair();
	    ecPrivateKey = (ECPrivateKey)ecKeyPair.getPrivate();
	    ecPublicKey  = (ECPublicKey)ecKeyPair.getPublic();
	} catch (Exception e) {
	    throw new
		SSH2FatalException("Error generating EC keys: "+e);
	}
    }

}
