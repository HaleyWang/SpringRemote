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

package com.mindbright.jce.provider.keystore;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Date;


import java.security.Key;
import java.security.MessageDigest;
import java.security.KeyStoreSpi;
import java.security.PublicKey;
import java.security.KeyStoreException;
import java.security.UnrecoverableKeyException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPublicKey;

import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;

import com.mindbright.asn1.ASN1DER;
import com.mindbright.asn1.ASN1OIDRegistry;

import com.mindbright.security.x509.X509Certificate;

import com.mindbright.security.pkcs8.EncryptedPrivateKeyInfo;

public class NetscapeKeyStore extends KeyStoreSpi {

    public final static int TYPE_VERSION       = 0;
    public final static int TYPE_CERTIFICATE   = 1;
    public final static int TYPE_NICKNAME      = 2;
    public final static int TYPE_SUBJECT       = 3;
    public final static int TYPE_REVOCATION    = 4;
    public final static int TYPE_KEYREVOCATION = 5;
    public final static int TYPE_SMIMEPROFILE  = 6;
    public final static int TYPE_CONTENTVER    = 7;

    public class DBEntry {

        protected byte[] data;
        public    int    type;
        public    int    version;
        public    int    flags;
        protected int    rPos;

        protected DBEntry(byte[] data) {
            this.data    = data;
            this.rPos    = 0;
            this.type    = readByte();
            this.version = readByte();
            this.flags   = readByte();
        }

        public final int readByte() {
            return (data[rPos++]) & 0xff;
        }

        public final int readShort() {
            int b1 = readByte();
            int b2 = readByte();
            return ((b1 << 8) + (b2 << 0));
        }

        public final byte[] readRaw(int len) {
            byte[] raw = new byte[len];
            readRaw(raw, 0, len);
            return raw;
        }

        public final void readRaw(byte[] raw, int off, int len) {
            System.arraycopy(data, rPos, raw, off, len);
            rPos += len;
        }

    }

    public class CertEntry extends DBEntry {

        public int    sslFlags;
        public int    emailFlags;
        public int    oSignFlags;
        public byte[] certificate;
        public String nickName;

        public CertEntry(byte[] data) {
            super(data);
            sslFlags    = readShort();
            emailFlags  = readShort();
            oSignFlags  = readShort();
            int certLen = readShort();
            int nickLen = readShort();
            certificate = readRaw(certLen);
            nickName    = new String(readRaw(nickLen - 1));
        }

    }

    public class KeyEntry extends DBEntry {

        public byte[] salt;
        public String nickName;
        public byte[] encryptedKey;

        public KeyEntry(byte[] data) {
            super(data);
            /*
             * OUCH, the header is different but luckily it's still 3 bytes...
             */
            salt     = readRaw(version);
            nickName = new String(readRaw(flags - 1));
            rPos++; // skip string null-termination
            encryptedKey = readRaw(data.length - rPos);
        }

    }

    private final static String[] CERT_FILES = {
                "cert8.db",       // Mozilla >= 1.3
                "cert7.db",       // Mozilla < 1.3
                "Certificates8",  // MacOS X
                "Certificates7"
            };

    private final static String[] KEY_FILES = {
                "key3.db",
                "Key Database3"
            };

    private DBHash certdb;
    private DBHash keydb;

    private Hashtable<String, CertEntry> certificates;

    public NetscapeKeyStore() {
        ASN1OIDRegistry.addModule("com.mindbright.security.pkcs12");

        /*
         * Buggy oids in here it seems (do they mean pkcs#5 or 12, hmm...)
         * (we use PKCS12PbeParams here, same struct)
         */
        ASN1OIDRegistry.register("1.2.840.113549.1.12.5.1.3",
                                 "com.mindbright.security.pkcs12.PKCS12PbeParams");

        certdb       = new DBHash();
        keydb        = new DBHash();
        certificates = new Hashtable<String, CertEntry>();
    }

    public Key engineGetKey(String alias, char[] password)
    throws NoSuchAlgorithmException, UnrecoverableKeyException {
        KeyEntry keyEntry = getKeyEntry(alias);

        if(!passwordCheck(password)) {
            throw new UnrecoverableKeyException("Invalid password");
        }

        if(keyEntry != null) {
            try {
                EncryptedPrivateKeyInfo epki = new EncryptedPrivateKeyInfo();
                ASN1DER ber = new ASN1DER();
                ByteArrayInputStream ba =
                    new ByteArrayInputStream(keyEntry.encryptedKey);
                ber.decode(ba, epki);
                byte[] enc = epki.encryptedData.getRaw();
                byte[] dec = new byte[enc.length];
                do3DESCipher(Cipher.DECRYPT_MODE, password,
                             enc, 0, enc.length, dec,
                             globalSalt(), keyEntry.salt);

                ba = new ByteArrayInputStream(dec);

                return PKCS12KeyStore.extractPrivateKey(dec);

            } catch (IOException e) {
                throw new UnrecoverableKeyException(e.getMessage());
            }
        }

        return null;
    }

    public Certificate[] engineGetCertificateChain(String alias) {
        // TODO
        return null;
    }

    public synchronized Certificate engineGetCertificate(String alias) {
        CertEntry cert = certificates.get(alias);
        if(cert != null) {
            return new X509Certificate(cert.certificate);
        }
        return null;
    }

    public Date engineGetCreationDate(String alias) {
        // N/A
        return null;
    }

    public void engineSetKeyEntry(String alias, Key key,
                                  char[] password, Certificate[] chain)
    throws KeyStoreException {
        // TODO
    }

    public void engineSetKeyEntry(String alias, byte[] key, Certificate[] chain)
    throws KeyStoreException {
        // TODO
    }

    public void engineSetCertificateEntry(String alias, Certificate cert)
    throws KeyStoreException {
        // TODO
    }

    public void engineDeleteEntry(String alias) throws KeyStoreException {
        // TODO
    }

    public synchronized Enumeration<String> engineAliases() {
        return certificates.keys();
    }

    public synchronized boolean engineContainsAlias(String alias) {
        return (certificates.get(alias) != null);
    }

    public synchronized int engineSize() {
        return certificates.size();
    }

    public boolean engineIsKeyEntry(String alias) {
        return (getKeyEntry(alias) != null);
    }

    public synchronized boolean engineIsCertificateEntry(String alias) {
        return !engineIsKeyEntry(alias) && (certificates.get(alias) != null);
    }

    public String engineGetCertificateAlias(Certificate cert) {
        // TODO
        return null;
    }

    public void engineStore(OutputStream stream, char[] password)
    throws IOException, NoSuchAlgorithmException, CertificateException {
        // TODO
    }

    public synchronized void engineLoad(InputStream stream, char[] password)
    throws IOException, NoSuchAlgorithmException, CertificateException {
        certificates.clear();

        if(!(stream instanceof ByteArrayInputStream)) {
            throw new
            IOException("Parameter 'stream' must be a ByteArrayInputStream");
        }
        byte[] dirB = new byte[stream.available()];
        stream.read(dirB);
        String dirName = new String(dirB);
        String certFile = null;
        String keyFile = null;
        int i;

        for (i=0; i<CERT_FILES.length; i++) {
            if ((new File(dirName + File.separator + CERT_FILES[i])).exists()) {
                certFile = CERT_FILES[i];
                break;
            }
        }

        if (certFile == null)
            throw new IOException("No certificate database found");
        
        for (i=0; i<KEY_FILES.length; i++) {
            if ((new File(dirName + File.separator + KEY_FILES[i])).exists()) {
                keyFile = KEY_FILES[i];
                break;
            }
        }

        if (keyFile == null)
            throw new IOException("No key database found");

        certdb.loadAll(dirName + File.separator + certFile);
        keydb.loadAll(dirName + File.separator + keyFile);

        Enumeration<?> keys = certdb.keys();
        while(keys.hasMoreElements()) {
            DBHash.DBT dbt = (DBHash.DBT)keys.nextElement();
            if(dbt.key[0] == 0x01) {
                CertEntry cert = new CertEntry(dbt.data);
                certificates.put(cert.nickName, cert);
            }
        }
        if(!passwordCheck(password)) {
            throw new IOException("Invalid password");
        }
    }

    private KeyEntry getKeyEntry(String alias) {
        Certificate cert     = engineGetCertificate(alias);
        KeyEntry    keyEntry = null;
        if (cert != null) {
            PublicKey pk = cert.getPublicKey();
            byte[] keyKey = null;
            if (pk instanceof RSAPublicKey) {
                RSAPublicKey rpk = (RSAPublicKey)pk;
                keyKey  = rpk.getModulus().toByteArray();
            } else if (pk instanceof DSAPublicKey) {
                DSAPublicKey dpk = (DSAPublicKey)pk;
                keyKey  = dpk.getY().toByteArray();
            }
            byte[] keyData = keydb.get(keyKey);
            if(keyData == null && keyKey != null && keyKey[0] == 0x00) {
                // try without leading 0-byte also (NS6/Mozilla fix)
                byte[] b = new byte[keyKey.length-1];
                System.arraycopy(keyKey, 1, b, 0, b.length);
                keyData = keydb.get(b);
            }
            if(keyData != null) {
                keyEntry = new KeyEntry(keyData);
            }
        }
        return keyEntry;
    }

    private static byte[] deriveKey(char[] password, byte[] globalSalt,
                                    byte[] entrySalt)
    throws InvalidKeyException, NoSuchAlgorithmException,
        ShortBufferException {
        Mac           hmac = Mac.getInstance("HmacSHA1");
        MessageDigest sha1 = MessageDigest.getInstance("SHA1");
        byte[]        key  = new byte[40];

        byte[] pwd = new byte[password.length];
        for(int i = 0; i < pwd.length; i++) {
            pwd[i] = (byte)password[i];
        }

        sha1.update(globalSalt);
        sha1.update(pwd);
        byte[] hp = sha1.digest();

        byte[] pes = new byte[20];
        System.arraycopy(entrySalt, 0, pes, 0, entrySalt.length);

        sha1.update(hp);
        sha1.update(entrySalt);
        byte[] chp = sha1.digest();

        hmac.init(new SecretKeySpec(chp, hmac.getAlgorithm()));
        hmac.update(pes);
        hmac.update(entrySalt);
        hmac.doFinal(key, 0);

        hmac.update(pes);
        byte[] tk = hmac.doFinal();

        hmac.update(tk);
        hmac.update(entrySalt);
        hmac.doFinal(key, 20);

        return key;
    }

    private static void do3DESCipher(int mode, char[] password,
                                     byte[] input, int off, int len,
                                     byte[] output,
                                     byte[] globalSalt, byte[] entrySalt) {
        try {
            Cipher cipher      = Cipher.getInstance("DESede/CBC/PKCS5Padding");
            byte[] keymaterial = deriveKey(password, globalSalt, entrySalt);
            byte[] key         = new byte[24];
            byte[] iv          = new byte[8];

            System.arraycopy(keymaterial, 0, key, 0, 24);
            System.arraycopy(keymaterial, 32, iv, 0, 8);

            cipher.init(mode, new SecretKeySpec(key, "DESede"),
                        new IvParameterSpec(iv));
            cipher.doFinal(input, off, len, output, 0);

        } catch (Exception e) {
            throw new Error("Error in NetscapeKeyStore.do3DESCipher: " + e);
        }
    }

    private byte[] globalSalt() {
        return keydb.get("global-salt");
    }

    private boolean passwordCheck(char[] password) {
        if (password == null)
            return true;

        byte[] keyData = keydb.get("password-check");

        if (keyData == null)
            return true;

        KeyEntry pwdCheck = new KeyEntry(keyData);

        int    off = pwdCheck.encryptedKey.length - 16;
        byte[] dec = new byte[16];
        do3DESCipher(Cipher.DECRYPT_MODE, password,
                     pwdCheck.encryptedKey, off, 16, dec,
                     globalSalt(), pwdCheck.salt);

        return "password-check".equals(new String(dec, 0, 14));
    }

}
