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

package com.mindbright.jce.provider.publickey;


import java.security.Key;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.KeyFactorySpi;
import java.security.InvalidKeyException;
import java.security.spec.KeySpec;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.spec.DHPublicKeySpec;
import javax.crypto.spec.DHPrivateKeySpec;

public final class DHKeyFactory extends KeyFactorySpi {

    protected PublicKey engineGeneratePublic(KeySpec keySpec)
    throws InvalidKeySpecException {
        if(!(keySpec instanceof DHPublicKeySpec)) {
            throw new InvalidKeySpecException("KeySpec " + keySpec +
                                              ", not supported");
        }
        DHPublicKeySpec dhPub = (DHPublicKeySpec)keySpec;
        return new DHPublicKey(dhPub.getY(), dhPub.getP(), dhPub.getG());
    }

    protected PrivateKey engineGeneratePrivate(KeySpec keySpec)
    throws InvalidKeySpecException {
        if(!(keySpec instanceof DHPrivateKeySpec)) {
            throw new InvalidKeySpecException("KeySpec " + keySpec +
                                              ", not supported");
        }
        DHPrivateKeySpec dhPrv = (DHPrivateKeySpec)keySpec;
        return new DHPrivateKey(dhPrv.getX(), dhPrv.getP(), dhPrv.getG());
    }

    @SuppressWarnings("unchecked")
	protected KeySpec engineGetKeySpec(Key key, Class keySpec)
    	throws InvalidKeySpecException {
        // !!! TODO
        throw new Error("DHKeyFactory.engineGetKeySpec() not implemented");
    }

    protected Key engineTranslateKey(Key key) throws InvalidKeyException {
        // !!! TODO
        throw new Error("DHKeyFactory.engineTranslateKey() not implemented");
    }

}
