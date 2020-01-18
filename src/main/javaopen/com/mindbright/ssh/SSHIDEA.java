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

package com.mindbright.ssh;

import com.mindbright.util.Crypto;

import java.security.InvalidKeyException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class SSHIDEA extends SSHCipher {
    Cipher cipher;

    public SSHIDEA() throws NoSuchAlgorithmException, NoSuchPaddingException {
	cipher = Crypto.getCipher("IDEA/CFB/NoPadding");
    }

    public void setKey(boolean encrypt, byte[] key) {
        try {
	    cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE,
			new SecretKeySpec(key, 0, 16, "IDEA"),
			new IvParameterSpec(new byte[8]));
        } catch (InvalidAlgorithmParameterException iape) {
            throw new Error("Internal error, invalid key in SSHIDEA");  
        } catch (InvalidKeyException e) {
            throw new Error("Internal error, invalid key in SSHIDEA");
        }
    }

    public synchronized void encrypt(byte[] src, int srcOff, byte[] dest, int destOff, int len) {
	try {
	    cipher.update(src, srcOff, len, dest, destOff);
	} catch (ShortBufferException sbe) {
	}
    }

    public synchronized void decrypt(byte[] src, int srcOff, byte[] dest, int destOff, int len) {
	try {
	    cipher.update(src, srcOff, len, dest, destOff);
	} catch (ShortBufferException sbe) {
	}
    }
}
