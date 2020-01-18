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

import java.security.MessageDigest;

public abstract class SSHCipher {

    public static SSHCipher getInstance(String algorithm) {
        Class<?> c;
        try {
            c = Class.forName("com.mindbright.ssh." + algorithm);
            return (SSHCipher)c.newInstance();
        } catch(Throwable t) {
            t.printStackTrace();
            return null;
        }
    }

    public abstract void encrypt(byte[] src, int srcOff,
                                 byte[] dest, int destOff, int len);
    public abstract void decrypt(byte[] src, int srcOff,
                                 byte[] dest, int destOff, int len);
    public abstract void setKey(boolean encrypt, byte[] key);

    public byte[] encrypt(byte[] src) {
        byte[] dest = new byte[src.length];
        encrypt(src, 0, dest, 0, src.length);
        return dest;
    }

    public byte[] decrypt(byte[] src) {
        byte[] dest = new byte[src.length];
        decrypt(src, 0, dest, 0, src.length);
        return dest;
    }

    public void setKey(boolean encrypt, String key) throws NoSuchMethodException {
        MessageDigest md5;
        byte[] mdKey = new byte[32];
        try {
            md5 = com.mindbright.util.Crypto.getMessageDigest("MD5");
            md5.update(key.getBytes());
            byte[] digest = md5.digest();
            System.arraycopy(digest, 0, mdKey, 0, 16);
            System.arraycopy(digest, 0, mdKey, 16, 16);
        } catch(Exception e) {
            throw new NoSuchMethodException(
                "MD5 not implemented, can't generate key out of string!");
        }
        setKey(encrypt, mdKey);
    }

}
