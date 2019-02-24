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

import java.security.NoSuchAlgorithmException;

import javax.crypto.NoSuchPaddingException;

public final class SSHDES3 extends SSHCipher {
    SSHDES des1;
    SSHDES des2;
    SSHDES des3;
    
    public SSHDES3() throws NoSuchAlgorithmException, NoSuchPaddingException {
	des1 = new SSHDES();
	des2 = new SSHDES();
	des3 = new SSHDES();
    }

    public synchronized void encrypt(byte[] src, int srcOff, byte[] dest, int destOff, int len) {
        des1.encrypt(src, srcOff, dest, destOff, len);
        des2.decrypt(dest, destOff, dest, destOff, len);
        des3.encrypt(dest, destOff, dest, destOff, len);
    }

    public synchronized void decrypt(byte[] src, int srcOff, byte[] dest, int destOff, int len) {
        des3.decrypt(src, srcOff, dest, destOff, len);
        des2.encrypt(dest, destOff, dest, destOff, len);
        des1.decrypt(dest, destOff, dest, destOff, len);
    }

    public void setKey(boolean encrypt, byte[] key) {
        byte[] subKey = new byte[8];
        System.arraycopy(key, 0, subKey, 0, 8);
        des1.setKey(encrypt, subKey);
        System.arraycopy(key, 8, subKey, 0, 8);
        des2.setKey(!encrypt, subKey);
        System.arraycopy(key, 16, subKey, 0, 8);
        des3.setKey(encrypt, subKey);
    }

}
