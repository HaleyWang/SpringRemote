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


public final class SSHBlowfish extends SSHCipher {
    Cipher cipher;
    byte[]   IV;
    byte[]   enc;
    byte[]   dec;

    public SSHBlowfish() throws NoSuchAlgorithmException, NoSuchPaddingException {
        cipher = Crypto.getCipher("Blowfish/CBC/NoPadding");
        IV     = new byte[8];
        enc    = new byte[8];
        dec    = new byte[8];
    }

    public void setKey(boolean encrypt, String skey) {
        if(skey.length() != 0) {
            byte[] key = skey.getBytes();
            setKey(encrypt, key);
        }
    }

    public void setKey(boolean encrypt, byte[] key) {
        try {
	    cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE,
			new SecretKeySpec(key, 0, 32, "Blowfish"),
			new IvParameterSpec(IV));
        } catch (InvalidAlgorithmParameterException iape) {
            throw new Error("Internal error, invalid key in SSHBlowfish");            
        } catch (InvalidKeyException e) {
            throw new Error("Internal error, invalid key in SSHBlowfish");
        }
    }

    public synchronized void encrypt(byte[] src, int srcOff, byte[] dest, int destOff, int len) {
        int end = srcOff + len;
        int i, j;

        for(int si = srcOff, di = destOff; si < end; si += 8, di += 8) {
            for(i = 0; i < 4; i++) {
                j = 3 - i;
                IV[i]     = src[si + j];
                IV[i + 4] = src[si + 4 + j];
            }
            try {
                cipher.update(IV, 0, IV.length, IV);
            } catch (ShortBufferException sbe) {
            }
            for(i = 0; i < 4; i++) {
                j = 3 - i;
                dest[di + i]     = IV[j];
                dest[di + i + 4] = IV[4 + j];
            }
        }
    }

    public synchronized void decrypt(byte[] src, int srcOff, byte[] dest, int destOff, int len) {
        int    end = srcOff + len;
        int    i, j;

        for(int si = srcOff, di = destOff; si < end; si += 8, di += 8) {
            for(i = 0; i < 4; i++) {
                j = 3 - i;
                enc[i]     = src[si + j];
                enc[i + 4] = src[si + 4 + j];
            }
            try {
                cipher.update(enc, 0, enc.length, dec);
            } catch (ShortBufferException sbe) {
            }
            for(i = 0; i < 4; i++) {
                j = 3 - i;
                dest[di+i] = dec[j];
                dest[di+i+4] = dec[4+j];                
            }
        }
    }

}
