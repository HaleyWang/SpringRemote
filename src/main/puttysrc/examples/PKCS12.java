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

package examples;

import java.math.BigInteger;

import java.security.MessageDigest;

import com.mindbright.asn1.ASN1Object;
import com.mindbright.asn1.ASN1Any;
import com.mindbright.asn1.ASN1OIDRegistry;
import com.mindbright.asn1.ASN1OctetString;
import com.mindbright.asn1.ASN1CharString;
import com.mindbright.asn1.ASN1DER;

import com.mindbright.security.x509.Certificate;
import com.mindbright.security.x509.Extension;
import com.mindbright.security.x509.Attribute;
import com.mindbright.security.x509.RelativeDistinguishedName;
import com.mindbright.security.x509.Name;
import com.mindbright.security.x509.AttributeTypeAndValue;
import com.mindbright.security.x509.DirectoryString;

import com.mindbright.security.pkcs7.ContentInfo;
import com.mindbright.security.pkcs7.EncryptedData;

import com.mindbright.security.pkcs8.PrivateKeyInfo;
import com.mindbright.security.pkcs8.EncryptedPrivateKeyInfo;

import com.mindbright.security.pkcs12.PFX;
import com.mindbright.security.pkcs12.AuthenticatedSafe;
import com.mindbright.security.pkcs12.SafeContents;
import com.mindbright.security.pkcs12.SafeBag;
import com.mindbright.security.pkcs12.PKCS12PbeParams;
import com.mindbright.security.pkcs12.CertBag;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Opens a PKCS#12-file and prints its content in great detail.
 * <p>
 * Usage:
 * <code> java -cp examples.jar examples.PKCS12
 * <em>filename</em> <em>password</em>
 */
public final class PKCS12 {

    private static byte[] deriveKey(String password, int keyLen, byte[] salt,
                                   int iterations, int id, String digAlg) {
        try {
            MessageDigest digest = com.mindbright.util.Crypto.getMessageDigest(digAlg);
            int           u      = digest.getDigestLength();
            byte[]        Ai     = new byte[u];
            byte[]        key    = new byte[keyLen];
            int           v      = 64; // Set this from digest (bad API!)

            char[] pc = password.toCharArray();
            byte[] pb = new byte[pc.length * 2 + 2];

            int sl   = ((salt.length + v - 1) / v) * v;
            int pl   = ((pb.length + v - 1) / v) * v;
            byte[] D = new byte[v];
            byte[] I = new byte[sl + pl];
            byte[] B = new byte[v];

            int i;
            for(i = 0; i < pc.length; i++) {
                pb[i * 2]       = (byte)(pc[i] >>> 8);
                pb[(i * 2) + 1] = (byte)(pc[i] & 0xff);
            }
            for(i = 0; i < v; i++) {
                D[i] = (byte)id;
            }
            for(i = 0; i < sl; i++) {
                I[i] = salt[i % salt.length];
            }
            for(i = 0; i < pl; i++) {
                I[sl + i] = pb[i % pb.length];
            }

            int        cnt   = 0;
            BigInteger one   = BigInteger.valueOf(1L);
            byte[]     ijRaw = new byte[v];
            while(true) {
                digest.update(D);
                digest.update(I);
                digest.digest(Ai, 0, u);
                for(i = 1; i < iterations; i++) {
                    digest.update(Ai);
                    digest.digest(Ai, 0, u);
                }

                int n = ((u > (keyLen - cnt)) ? keyLen - cnt : u);
                System.arraycopy(Ai, 0, key, cnt, n);
                cnt += n;
                if(cnt >= keyLen) {
                    break;
                }

                for(i = 0; i < v; i++) {
                    B[i] = Ai[i % u];
                }

                BigInteger Bplus1 = (new BigInteger(1, B)).add(one);
                for(i = 0; i < I.length; i += v) {
                    System.arraycopy(I, i, ijRaw, 0, v);
                    BigInteger Ij = new BigInteger(1, ijRaw);
                    Ij = Ij.add(Bplus1);
                    ijRaw = unsignedBigIntToBytes(Ij, v);
                    System.arraycopy(ijRaw, 0, I, i, v);
                }
            }

            return key;

        } catch (Exception e) {
            e.printStackTrace();
            throw new Error("Error in PKCS12.deriveKey: " + e);
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

    private static void doCipher(int mode, String password,
                                 byte[] input, int len, byte[] output,
                                 byte[] salt, int iterations,
                                 String ciperType)
    throws Exception {
        try {
            Cipher cipher = Cipher.getInstance(ciperType);
            int keyLen = (ciperType.startsWith("3DES") ? (192 / 8) : (40 / 8));
            byte[] key = deriveKey(password, keyLen, salt, iterations,
                                   1, "SHA");
            byte[] iv  = deriveKey(password, 8, salt, iterations,
                                   2, "SHA");
            cipher.init(mode, new SecretKeySpec(key, cipher.getAlgorithm()),
                        new IvParameterSpec(iv));
            cipher.doFinal(input, 0, len, output, 0);
        } catch (NoSuchAlgorithmException e) {
            throw new Exception("Invalid algorithm in " +
                                "PKCS12.doCipher: " + e);
        } catch (InvalidKeyException e) {
            throw new Exception("Invalid key derived in " +
                                "PKCS12.doCipher: " + e);
        }
    }

    /**
     * Run the application
     */
    public static void main(String[] argv) {
        try {
            ASN1OIDRegistry.addModule("com.mindbright.security.pkcs12");
            ASN1OIDRegistry.addModule("com.mindbright.security.x509");

            if(argv.length != 2) {
                System.out.println("usage: PKCS12 <filename> <password>");
                System.exit(1);
            }

            String fileName = argv[0];
            String password = argv[1];

            ASN1DER ber    = new ASN1DER();
            PFX     pkcs12 = new PFX();

            java.io.FileInputStream file =
                new java.io.FileInputStream(fileName);
            ber.decode(file, pkcs12);
            file.close();

            System.out.println("PFX authSafe type: " +
                               pkcs12.authSafe.contentType.getString());

            checkMac(pkcs12, password);

            AuthenticatedSafe  authSafe = new AuthenticatedSafe();
            ASN1OctetString data = (ASN1OctetString)pkcs12.authSafe.content.getValue();
            java.io.ByteArrayInputStream ba =
                new java.io.ByteArrayInputStream(data.getRaw());
            ber.decode(ba, authSafe);

            for(int i = 0; i < authSafe.getCount(); i++) {
                ContentInfo ci  = authSafe.getContentInfo(i);
                String      cit = ci.contentType.getString();
                System.out.println("ContentInfo type (" + i + ") = " + cit);
                if(cit.equals("1.2.840.113549.1.7.1")) {
                    data = (ASN1OctetString)ci.content.getValue();
                    ba = new java.io.ByteArrayInputStream(data.getRaw());
                    SafeContents sc = new SafeContents();
                    ber.decode(ba, sc);
                    processSafeContents(sc, password);
                } else if(cit.equals("1.2.840.113549.1.7.6")) {
                    EncryptedData ed = (EncryptedData)ci.content.getValue();
                    System.out.println("EncryptedData type = " +
                                       ed.encryptedContentInfo.contentType.getString());
                    System.out.println("EncryptedData alg = " +
                                       ed.encryptedContentInfo.contentEncryptionAlgorithm.algorithmName());

                    PKCS12PbeParams params = (PKCS12PbeParams)
                                             ed.encryptedContentInfo.contentEncryptionAlgorithm.parameters.getValue();
                    byte[] enc        = ed.encryptedContentInfo.encryptedContent.getRaw();
                    byte[] salt       = params.salt.getRaw();
                    int    iterations = params.iterations.getValue().intValue();


                    byte[] dec = new byte[(enc.length + 7)];
                    doCipher(Cipher.DECRYPT_MODE, password,
                             enc, enc.length, dec,
                             salt, iterations, "RC2/CBC/PKCS5Padding");
                    System.out.println("**************");
                    ba = new java.io.ByteArrayInputStream(dec);
                    SafeContents sc = new SafeContents();
                    ber.decode(ba, sc);

                    processSafeContents(sc, password);

                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void checkMac(PFX pfx, String password)
    throws Exception {
        if(!pfx.macData.isSet()) {
            System.out.println("No MAC present");
            return;
        }
        String digAlg     = pfx.macData.mac.
                            digestAlgorithm.algorithm.getString();
        Mac    mac        = com.mindbright.util.Crypto.getMac(digAlg);
        byte[] data       =	pfx.getDataContent().getRaw();
        byte[] storedDig  = pfx.macData.mac.digest.getRaw();
        byte[] salt       = pfx.macData.macSalt.getRaw();
        int    iterations = pfx.macData.getIterations();
        byte[] key        = deriveKey(password, 20, salt, iterations,
                                      3, digAlg);
        mac.init(new SecretKeySpec(key, mac.getAlgorithm()));
        mac.update(data);
        byte[] dataDig = mac.doFinal();
        for(int i = 0; i < storedDig.length; i++) {
            if(dataDig[i] != storedDig[i]) {
                throw new Exception("MAC check failed (" + i + "th pos.)");
            }
        }
        System.out.println("MAC Check OK");
    }

    private static void processSafeContents(SafeContents sc, String password) throws Exception {
        java.io.ByteArrayInputStream ba = null;
        ASN1DER ber = new ASN1DER();
        for(int j = 0; j < sc.getCount(); j++) {
            SafeBag sb = sc.getSafeBag(j);
            System.out.println("SafeBag type (" + j + ") = " +
                               sb.bagId.getString());
            printSafeBagAttr(sb);
            if(sb.bagId.getString().equals("1.2.840.113549.1.12.10.1.2")) {
                EncryptedPrivateKeyInfo kb =
                    (EncryptedPrivateKeyInfo)sb.bagValue.getValue();
                System.out.println("PKCS#8 shrouded key alg = " +
                                   kb.encryptionAlgorithm.algorithmName());

                PKCS12PbeParams params = (PKCS12PbeParams)
                                         kb.encryptionAlgorithm.parameters.getValue();

                byte[] enc        = kb.encryptedData.getRaw();
                byte[] salt       = params.salt.getRaw();
                int    iterations = params.iterations.getValue().intValue();

                byte[] dec = new byte[(enc.length + 7)];
                doCipher(Cipher.DECRYPT_MODE, password,
                         enc, enc.length, dec,
                         salt, iterations, "3DES/CBC/PKCS5Padding");

                //
                System.out.println("*******************************");

                ba = new java.io.ByteArrayInputStream(dec);
                PrivateKeyInfo pki = new PrivateKeyInfo();
                ber.decode(ba, pki);
                //

                com.mindbright.security.pkcs1.RSAPrivateKey
                rsa = new com.mindbright.security.pkcs1.RSAPrivateKey();
                ba = new java.io.ByteArrayInputStream(pki.privateKey.getRaw());
                ber.decode(ba, rsa);

                System.out.println("RSAPrivateKey ver = " + rsa.version.getValue().intValue());

            } else if(sb.bagId.getString().equals("1.2.840.113549.1.12.10.1.3")) {
                CertBag cb = (CertBag)sb.bagValue.getValue();
                byte[] derCert = ((ASN1OctetString)cb.certValue.getValue()).getRaw();

                System.out.println("**** Parsing X509 cert ****");
                ba = new java.io.ByteArrayInputStream(derCert);
                Certificate x509cert = new Certificate();
                ASN1DER der = new ASN1DER();
                der.decode(ba, x509cert);
                System.out.println("**** Issuer:");
                printDistinguishedName(x509cert.tbsCertificate.issuer);
                System.out.println("Subject:");
                printDistinguishedName(x509cert.tbsCertificate.subject);
                System.out.println("key algorithm: " +
                                   x509cert.tbsCertificate.subjectPublicKeyInfo.algorithm.algorithmName());
                for(int i = 0; i < x509cert.tbsCertificate.extensions.getCount();
                        i++) {
                    Extension e = (Extension)x509cert.tbsCertificate.extensions.getComponent(i);
                    System.out.println("extension: " + e.extnID.getString() +
                                       ", crit = " + e.critical.getValue());
                    ;
                }

                System.out.println("****");
            }
        }
    }

    private static void printSafeBagAttr(SafeBag sb) {
        int cnt = sb.bagAttributes.getCount();
        System.out.println("SafeBag attributes (" + cnt + ") :");
        for(int i = 0; i < cnt; i++) {
            Attribute a = (Attribute)sb.bagAttributes.getComponent(i);
            String aName = ASN1OIDRegistry.lookupName(a.type.getString());
            if(aName == null) {
                aName = a.type.getString();
            }
            String aValue = null;
            ASN1Object v = a.values.getComponent(0);
            while(true) {
                if(v instanceof ASN1CharString) {
                    aValue = ((ASN1CharString)v).getValue();
                } else if(v instanceof ASN1Any) {
                    v = ((ASN1Any)v).getValue();
                    continue;
                } else {
                    aValue = com.mindbright.util.HexDump.
                             toString(((ASN1OctetString)v).getRaw());
                }
                break;
            }
            System.out.println(aName + " = " + aValue);
        }
    }

    private static void printDistinguishedName(Name name) {
        for(int i = 0; i < name.getCount(); i++) {
            RelativeDistinguishedName rdn = name.getRDN(i);
            for(int j = 0; j < rdn.getCount(); j++) {
                AttributeTypeAndValue atv = rdn.getTypeAndValue(j);
                String     valStr = "<unknown>";
                ASN1Object value  = atv.value.getValue();
                if(value instanceof DirectoryString) {
                    valStr = ((DirectoryString)value).getString();
                } else if(value instanceof ASN1CharString) {
                    valStr = ((ASN1CharString)value).getValue();
                }
                String aName = ASN1OIDRegistry.lookupName(atv.type.getString());
                if(aName == null) {
                    aName = atv.type.getString();
                }
                System.out.println(aName  + " = " + valStr);
            }
        }
    }
}
