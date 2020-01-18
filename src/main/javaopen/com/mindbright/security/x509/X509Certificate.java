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

package com.mindbright.security.x509;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.math.BigInteger;

import java.security.PublicKey;
import java.security.KeyFactory;
import java.security.SignatureException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import java.security.cert.CertificateException;
import java.security.cert.CertificateEncodingException;

import java.security.spec.RSAPublicKeySpec;

import com.mindbright.asn1.ASN1DER;
import com.mindbright.asn1.ASN1IA5String;
import com.mindbright.asn1.ASN1OIDRegistry;
import com.mindbright.asn1.ASN1Object;

import com.mindbright.security.pkcs1.RSAPublicKey;
import com.mindbright.security.pkcs1.DSAPublicKey;
import com.mindbright.security.pkcs1.DSAParams;

import java.security.spec.DSAPublicKeySpec;

public class X509Certificate
    extends java.security.cert.Certificate {

    private static final long serialVersionUID = 1L;

    private byte[]      encoded;
    private Certificate certificate;

    public X509Certificate(byte[] encoded) {
        super("X.509");
        this.encoded     = encoded;
        this.certificate = new Certificate();

        ASN1OIDRegistry.addModule("com.mindbright.security.x509");
        ASN1OIDRegistry.addModule("com.mindbright.security.pkcs1");

        try {
            ASN1DER              der = new ASN1DER();
            ByteArrayInputStream ba  = new ByteArrayInputStream(encoded);
            der.decode(ba, certificate);
        } catch (IOException e) {
            throw new Error("Internal error decoding DER encoded X.509 cert: " +
                            e.getMessage());
        }
    }

    public byte[] getEncoded() throws CertificateEncodingException {
        return encoded;
    }

    public void verify(PublicKey key)
    throws CertificateException, NoSuchAlgorithmException,
        InvalidKeyException, NoSuchProviderException, SignatureException {}

    public void verify(PublicKey key, String sigProvider)
    throws CertificateException, NoSuchAlgorithmException,
        InvalidKeyException, NoSuchProviderException, SignatureException {}

    public String toString() {
        return "X509 Certificate: \n" +
               "  version: "    + (getVersion()+1)  + "\n" +
               "  serialNo: "   + getSerialNumber() + "\n" +
               "  pubalg: "     + getPubAlgName()   + "\n" +
               "  issuer: "     + getIssuerDN()     + "\n" +
               "  subject: "    + getSubjectDN()    + "\n\n" +
               " Extensions:\n" + getExtensions();
    }

    public String getIssuerDN() {
        return certificate.tbsCertificate.issuer.getRFC2253Value();
    }

    public String getSubjectDN() {
        return certificate.tbsCertificate.subject.getRFC2253Value();
    }

    public BigInteger getSerialNumber() {
        return certificate.tbsCertificate.serialNumber.getValue();
    }

    public String getPubAlgName() {
        return certificate.tbsCertificate.subjectPublicKeyInfo.algorithm.algorithmName();
    }

    private ASN1Object getExtensionWithOID(String oid, Class<?> c) {
        try {
            Extensions es = certificate.tbsCertificate.extensions;
            for (int i=0; i<es.getCount(); i++) {
                Extension e = (Extension)es.getComponent(i);
                if (e.extnID.getString().equals(oid)) {
                    ASN1DER der = new ASN1DER();
                    ByteArrayInputStream ba  = new ByteArrayInputStream(e.extnValue.getRaw());
                    ASN1Object obj = (ASN1Object)c.newInstance();
                    der.decode(ba, obj);
                    return obj;
                }
            }
        } catch (Throwable t) {
        }
        return null;
    }

    public SubjectKeyIdentifier getSubjectKeyIdentifier() {
        return (SubjectKeyIdentifier)getExtensionWithOID
               ("2.5.29.14", SubjectKeyIdentifier.class);
    }

    public KeyUsage getKeyUsage() {
        return (KeyUsage)getExtensionWithOID
               ("2.5.29.15", KeyUsage.class);
    }

    public BasicConstraints getBasicConstraints() {
        return (BasicConstraints)getExtensionWithOID
               ("2.5.29.19", BasicConstraints.class);
    }

    public String getExtensions() {
        StringBuilder sb = new StringBuilder();
        try {
            Extensions es = certificate.tbsCertificate.extensions;
            for (int i=0; i<es.getCount(); i++) {
                Extension e = (Extension)es.getComponent(i);
                String oid = e.extnID.getString();
                String crit = e.critical.getValue() ? "yes" : "no ";
                String val = null;
                ASN1Object o;
                
                try {
                    o = getExtensionWithOID(oid, ASN1OIDRegistry.lookupType(oid));
                    if (o != null)
                        val = o.toString();                    
                    if (val != null && val.indexOf(':') == -1) 
                        val = ASN1OIDRegistry.lookupName(oid) + ": " + val;
                } catch (Throwable tt) {
                }

                if (val == null) { 
                     try {
                         // Oh well... let's try with a generic string 
                         o = getExtensionWithOID(oid, ASN1IA5String.class);
                         if (o != null)
                             val = o.toString();
                         if (val != null && val.indexOf(':') == -1) 
                             val = ASN1OIDRegistry.lookupName(oid) + ": " + val;                    
                     } catch (Throwable tt) {
                     }
                }
                
                if (val == null) {
                    val = ASN1OIDRegistry.lookupName(oid);
                    if (val == null)
                        val = oid;
                    val += ": ...";
                }

                sb.append("  critical: ").append(crit).append(" ").append(val).append("\n");
            }
        } catch (Throwable t) {
        }

        return sb.toString();
    }

    public int getVersion() {
        int ver = 0;
        try {
            ver = certificate.tbsCertificate.version.getValue().intValue();
        } catch (Throwable t) {}
        return ver;
    }

    public PublicKey getPublicKey() {
        SubjectPublicKeyInfo spki =
            certificate.tbsCertificate.subjectPublicKeyInfo;
        String alg = spki.algorithm.algorithmName().toUpperCase();
        ASN1DER              der = new ASN1DER();
        if (alg.startsWith("RSA")) {
            RSAPublicKey         rsa = new RSAPublicKey();
            ByteArrayInputStream ba  = new ByteArrayInputStream(
                                           spki.subjectPublicKey.getBitArray());
            try {
                der.decode(ba, rsa);
            } catch (Exception e) {
                throw new Error("Internal error decoding SubjectPublicKeyInfo.subjectPublicKey: " +
                                e.getMessage());
            }

            try {
                KeyFactory       keyFact = KeyFactory.getInstance("RSA");
                RSAPublicKeySpec pubSpec =
                    new RSAPublicKeySpec(rsa.modulus.getValue(),
                                         rsa.publicExponent.getValue());
                return keyFact.generatePublic(pubSpec);

            } catch (Exception e) {
                throw new Error("Error creating RSA key: " + e.getMessage());
            }

        } else if (alg.startsWith("DSA")) {
            DSAPublicKey         dsa = new DSAPublicKey();
            ByteArrayInputStream ba  = new ByteArrayInputStream(
                                           spki.subjectPublicKey.getBitArray());
            try {
                der.decode(ba, dsa);
            } catch (Exception e) {
                throw new Error("Internal error decoding SubjectPublicKeyInfo.subjectPublicKey: " +
                                e.getMessage());
            }
            BigInteger y = dsa.getValue();

            DSAParams dsaParams =
                (DSAParams) spki.algorithm.parameters.getValue();

            BigInteger p = dsaParams.p.getValue();
            BigInteger q = dsaParams.q.getValue();
            BigInteger g = dsaParams.g.getValue();

            try {
                KeyFactory       dsaKeyFact = KeyFactory.getInstance("DSA");
                DSAPublicKeySpec dsaPubSpec = new DSAPublicKeySpec(y, p, q, g);
                return dsaKeyFact.generatePublic(dsaPubSpec);

            } catch (Exception e) {
                throw new Error("Error creating DSA key: " + e.getMessage());
            }
        } else {
            throw new Error("Internal error decoding publicKey: unknown algorithm");
        }
    }
}

