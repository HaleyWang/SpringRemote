/******************************************************************************
 *
 * Copyright (c) 2005-2011 Cryptzone Group AB. All Rights Reserved.
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

/**
 * Implements diffie hellman key exchange using a predefined group. This
 * algorithm is known as 'diffie-hellman-group14-sha1'
 */
public class SSH2KEXDHGroup14SHA1 extends SSH2KEXDHGroupNumSHA1 {
    private final static BigInteger oakleyGroup14P =
        new BigInteger("FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1" +
                       "29024E088A67CC74020BBEA63B139B22514A08798E3404DD" +
                       "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245" +
                       "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED" +
                       "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D" +
                       "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F" +
                       "83655D23DCA3AD961C62F356208552BB9ED529077096966D" +
                       "670C354E4ABC9804F1746C08CA18217C32905E462E36CE3B" +
                       "E39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9" +
                       "DE2BCBF6955817183995497CEA956AE515D2261898FA0510" +
                       "15728E5A8AACAA68FFFFFFFFFFFFFFFF", 16);

    private final static BigInteger oakleyGroup14G = BigInteger.valueOf(2);

    public final static String name = "SSH2KEXDHGroup14SHA1";

    public BigInteger getGroupP() {return oakleyGroup14P;}
    public BigInteger getGroupG() {return oakleyGroup14G;}
    public String getName()       {return name;  }
}
