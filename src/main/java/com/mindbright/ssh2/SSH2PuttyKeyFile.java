/******************************************************************************
 *
 * Copyright (c) 2006-2011 Cryptzone Group AB. All Rights Reserved.
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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import java.util.StringTokenizer;

import java.security.MessageDigest;
import java.security.GeneralSecurityException;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.mindbright.ssh2.SSH2FatalException;

import com.mindbright.util.Base64;
import com.mindbright.util.HexDump;


/**
 * This class implements support for reading the PuTTY key file format.
 * 
 */

public class SSH2PuttyKeyFile {

    private boolean version1;
    private String format;
    private String encryption;
    private String comment;
    private String pubblob;
    private byte[] pubbytes;
    private String privblob;
    private byte[] privbytes;
    private String privmacorhash;
    private boolean isprivmac;

    public SSH2PuttyKeyFile(InputStream in) 
	throws IOException {
	super();
	load(in);
    }

    public void load(InputStream in) 
	throws IOException {

	BufferedReader r = new BufferedReader(new InputStreamReader(in));
	StringTokenizer st = new StringTokenizer(r.readLine());
	String s = st.nextToken();
	version1 = s.endsWith("-1:");
	format = st.nextToken();
	st = new StringTokenizer(r.readLine());
	st.nextToken();
	encryption = st.nextToken();
	st = new StringTokenizer(r.readLine());
	st.nextToken();
	comment = st.nextToken();
	st = new StringTokenizer(r.readLine());
	st.nextToken();
	s = st.nextToken();
	int i, no = Integer.parseInt(s);
	pubblob = "";
	for (i=0; i<no; i++)
	    pubblob += r.readLine();
        pubbytes = Base64.decode(pubblob.getBytes());
	st = new StringTokenizer(r.readLine());
	st.nextToken();
	s = st.nextToken();
	no = Integer.parseInt(s);
	privblob = "";
	for (i=0; i<no; i++)
	    privblob += r.readLine();
	st = new StringTokenizer(r.readLine());
        isprivmac = st.nextToken().equals("Private-MAC:");        
	privmacorhash = st.nextToken();
    }

    public boolean validate(String passphrase) throws SSH2FatalException {
        // decrypt private key blob and check mac
        byte[] privbytesenc = Base64.decode(privblob.getBytes());

        if (encryption.equals("none")) {
            privbytes = privbytesenc;
        } else if (encryption.equals("aes256-cbc")) {
            try {
                byte pass[];
                byte key[] = new byte[40];
                MessageDigest sha1 = com.mindbright.util.Crypto.getMessageDigest("SHA1");
                if (passphrase == null) {
                    pass = new byte[0];
                } else {
                    pass = passphrase.getBytes();
                }
                sha1.update(new byte[] {0,0,0,0});
                sha1.update(pass);
                sha1.digest(key, 0, 20);
                sha1.reset();
                sha1.update(new byte[] {0,0,0,1});
                sha1.update(pass);
                sha1.digest(key, 20, 20);
                
                Cipher cipher = com.mindbright.util.Crypto.getCipher("AES/CBC");
                cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, 0, 32, "AES"));
                privbytes = cipher.doFinal(privbytesenc);
            } catch (GeneralSecurityException e) {
                throw new SSH2FatalException("Failed to decrypt PuTTY private key");
            }
        } else {
            throw new SSH2FatalException("Failed to decrypt PuTTY private key - " + 
                                         "unsupported encryption type: " + encryption);
        }

        byte[] data;
        
        if (version1) {
            data = privbytes;
        } else {
            SSH2DataBuffer b = new SSH2DataBuffer(32768);
            b.writeString(format);
            b.writeString(encryption);
            b.writeString(comment);
            b.writeString(pubbytes);
            b.writeString(privbytes);
            data = b.readRestRaw();
        }

        byte[] hash = null;

        try {
            MessageDigest sha1 = com.mindbright.util.Crypto.getMessageDigest("SHA1");
            if (isprivmac) {
                sha1.update("putty-private-key-file-mac-key".getBytes());
                if (!encryption.equals("none") && passphrase != null)
                    sha1.update(passphrase.getBytes());
                byte[] key = sha1.digest();
                Mac mac = com.mindbright.util.Crypto.getMac("HmacSHA1");
                mac.init(new SecretKeySpec(key, 0, 20, mac.getAlgorithm()));
                hash = mac.doFinal(data);
            } else {
                hash = sha1.digest(data);
            }
        } catch (GeneralSecurityException e) {
            throw new SSH2FatalException("Failed to calculate hash for PuTTY key file");
        }
        
	return hash != null && privmacorhash.equals(HexDump.toString(hash));
    }

    public byte[] getPublicKeyBlob() {
	return pubbytes;
    }

    public byte[] getPrivateKeyBlob() {
	return privbytes;
    }

    public String getFormat() {
	return format;
    }

    public String getComment() {
	return comment;
    }

    public String toString() {
	return 
	    "version1=" + version1 + ",format=" + format + 
	    ",encryption=" + encryption + 
	    ",comment=" + comment + ",pubblob=" + pubblob +
	    ",privblob=" + privblob + ",privmacorhash=" + privmacorhash +
            ",isprivmac=" + isprivmac;
    }
}
