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
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.DSAPrivateKeySpec;
import java.security.spec.InvalidKeySpecException;

public class DSAKeyFactory extends KeyFactorySpi {

    protected PublicKey engineGeneratePublic(KeySpec keySpec)
    throws InvalidKeySpecException {
        if(!(keySpec instanceof DSAPublicKeySpec)) {
            throw new InvalidKeySpecException("KeySpec " + keySpec +
                                              ", not supported");
        }
        DSAPublicKeySpec dsaPub = (DSAPublicKeySpec)keySpec;
        return new DSAPublicKey(dsaPub.getY(),
                                dsaPub.getP(), dsaPub.getQ(), dsaPub.getG());
    }

    protected PrivateKey engineGeneratePrivate(KeySpec keySpec)
    throws InvalidKeySpecException {
        if(!(keySpec instanceof DSAPrivateKeySpec)) {
            throw new InvalidKeySpecException("KeySpec " + keySpec +
                                              ", not supported");
        }
        DSAPrivateKeySpec dsaPrv = (DSAPrivateKeySpec)keySpec;
        return new DSAPrivateKey(dsaPrv.getX(),
                                 dsaPrv.getP(), dsaPrv.getQ(), dsaPrv.getG());
    }

    @SuppressWarnings("unchecked")
	protected KeySpec engineGetKeySpec(Key key, Class keySpec)
    throws InvalidKeySpecException {
        // !!! TODO
        return null;
    }

    protected Key engineTranslateKey(Key key)
    throws InvalidKeyException {
        // !!! TODO
        return null;
    }

}
