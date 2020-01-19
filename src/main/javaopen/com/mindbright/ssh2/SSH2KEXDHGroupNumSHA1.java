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

import java.security.MessageDigest;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPublicKey;

import javax.crypto.KeyAgreement;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.interfaces.DHPrivateKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.DHPublicKeySpec;

import com.mindbright.util.Crypto;

/**
 * Base class for diffie hellman key exchange using predefined groups.
 */
public abstract class SSH2KEXDHGroupNumSHA1 extends SSH2KeyExchanger {

    protected SSH2Transport transport;
    protected DHPublicKey   dhPublicKey;
    protected DHPrivateKey  dhPrivateKey;
    protected byte[]        serverHostKey;
    protected BigInteger    serverF;
    protected BigInteger    clientE;
    protected byte[]        sharedSecret_K;
    protected byte[]        exchangeHash_H;
    protected MessageDigest sha1;
    protected KeyPair       serverKey;

    public abstract BigInteger getGroupP();
    public abstract BigInteger getGroupG();
    public abstract String getName();

    public void init(SSH2Transport transport)
	throws SSH2Exception {
	init(transport, null);
    }

    public void init(SSH2Transport transport, KeyPair serverKey)
	throws SSH2Exception {
	this.transport = transport;
	this.sha1      = createHash();
	this.serverKey = serverKey;

        DHParameterSpec groupParams =
            new DHParameterSpec(getGroupP(), getGroupG());
        generateDHKeyPair(groupParams);

        if(!transport.isServer()) {
            sendDHINIT(SSH2.MSG_KEXDH_INIT);
        }
    }

    public void processKEXMethodPDU(SSH2TransportPDU pdu)
	throws SSH2Exception {
	if(pdu.getType() == SSH2.MSG_KEXDH_REPLY) {
	    if(transport.isServer()) {
		throw new SSH2KEXFailedException("Unexpected KEXDH_REPLY");
	    }

            serverHostKey      = pdu.readString();
            serverF            = pdu.readBigInt();
            byte[] serverSigH  = pdu.readString();

	    DHPublicKeySpec srvPubSpec =
		new DHPublicKeySpec(serverF, getGroupP(), getGroupG());

            computeSharedSecret_K(srvPubSpec);
            computeExchangeHash_H();

            transport.authenticateHost(serverHostKey, serverSigH,
                                       exchangeHash_H);

            transport.sendNewKeys();

	} else if(pdu.getType() == SSH2.MSG_KEXDH_INIT) {
	    if(!transport.isServer()) {
		throw new SSH2KEXFailedException("Unexpected KEXDH_INIT");
	    }

	    clientE = pdu.readBigInt();

	    DHPublicKeySpec clntPubSpec =
		new DHPublicKeySpec(clientE, getGroupP(), getGroupG());

	    computeSharedSecret_K(clntPubSpec);

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
		SSH2TransportPDU.createOutgoingPacket(SSH2.MSG_KEXDH_REPLY);

	    out.writeString(serverHostKey);
	    out.writeBigInt(dhPublicKey.getY());
	    out.writeString(signature.sign(exchangeHash_H));
	    transport.transmitInternal(out);

	    transport.sendNewKeys();
	}
    }

    public MessageDigest getExchangeHashAlgorithm() {
        sha1.reset();
        return sha1;
    }

    public byte[] getSharedSecret_K() {
        SSH2DataBuffer buf = new SSH2DataBuffer(1024);
        buf.writeString(sharedSecret_K);
        return buf.readRestRaw();
    }

    public byte[] getExchangeHash_H() {
        return exchangeHash_H;
    }

    protected void computeExchangeHash_H() {
        SSH2DataBuffer buf = new SSH2DataBuffer(64*1024);

        if(transport.isServer()) {
            serverF = dhPublicKey.getY();
        } else {
            clientE = dhPublicKey.getY();
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
        buf.writeBigInt(clientE);
        buf.writeBigInt(serverF);
        buf.writeString(sharedSecret_K);

        sha1.reset();
        sha1.update(buf.getData(), 0, buf.getWPos());
        exchangeHash_H = sha1.digest();

        transport.getLog().debug2(getName(),
                                  "computeExchangeHash_H", "E: ",
                                  clientE.toByteArray());
        transport.getLog().debug2(getName(),
                                  "computeExchangeHash_H", "F: ",
                                  serverF.toByteArray());
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

    protected void computeSharedSecret_K(DHPublicKeySpec peerPubSpec)
    throws SSH2Exception {
        try {
            KeyFactory   dhKeyFact = Crypto.getKeyFactory("DH");
            KeyAgreement dhKEX     = Crypto.getKeyAgreement("DH");

            DHPublicKey  peerPubKey =
                (DHPublicKey)dhKeyFact.generatePublic(peerPubSpec);

            dhKEX.init(dhPrivateKey);
            dhKEX.doPhase(peerPubKey, true);
            sharedSecret_K = dhKEX.generateSecret();
            
            if ((sharedSecret_K[0] & 0x80) == 0x80) {
                byte[] tmp = sharedSecret_K;
                sharedSecret_K = new byte[sharedSecret_K.length+1];
                sharedSecret_K[0] = 0x00;
                System.arraycopy(tmp, 0, sharedSecret_K, 1, tmp.length);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new SSH2FatalException("Error computing shared secret: "
                                         + e);
        }
    }

    protected void sendDHINIT(int type) throws SSH2Exception {
        SSH2TransportPDU pdu =
            SSH2TransportPDU.createOutgoingPacket(type);

        pdu.writeBigInt(dhPublicKey.getY());

        transport.transmitInternal(pdu);
    }

    protected MessageDigest createHash() throws SSH2Exception {
        try {
            return com.mindbright.util.Crypto.getMessageDigest("SHA1");
        } catch (Exception e) {
            throw new SSH2KEXFailedException("SHA1 not implemented", e);
        }
    }

    protected void generateDHKeyPair(DHParameterSpec dhParams)
	throws SSH2Exception {
	try {
	    KeyPairGenerator dhKeyPairGen = Crypto.getKeyPairGenerator("DH");
            dhKeyPairGen.initialize(dhParams, transport.getSecureRandom());

            KeyPair dhKeyPair = dhKeyPairGen.generateKeyPair();
	    dhPrivateKey = (DHPrivateKey)dhKeyPair.getPrivate();
	    dhPublicKey  = (DHPublicKey)dhKeyPair.getPublic();
	} catch (Exception e) {
            e.printStackTrace();
	    throw new
		SSH2FatalException("Error generating DiffieHellman keys: "+e);
	}
    }

}
