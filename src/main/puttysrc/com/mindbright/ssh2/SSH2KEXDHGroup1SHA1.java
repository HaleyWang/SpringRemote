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

/**
 * Implements diffie hellman key exchange using a predefined group. This
 * algorithm is known as 'diffie-hellman-group1-sha1'
 */
public class SSH2KEXDHGroup1SHA1 extends SSH2KEXDHGroupNumSHA1 {
    private final static BigInteger oakleyGroup2P =
        new BigInteger("FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1" +
                       "29024E088A67CC74020BBEA63B139B22514A08798E3404DD" +
                       "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245" +
                       "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED" +
                       "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE65381" +
                       "FFFFFFFFFFFFFFFF", 16);

    private final static BigInteger oakleyGroup2G = BigInteger.valueOf(2);

    public final static String name = "SSH2KEXDHGroup1SHA1";

    public BigInteger getGroupP() {return oakleyGroup2P;}
    public BigInteger getGroupG() {return oakleyGroup2G;}
    public String getName()       {return name;  }
}
