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
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.InvalidKeySpecException;

public class RSAKeyFactory extends KeyFactorySpi {

    protected PublicKey engineGeneratePublic(KeySpec keySpec)
    throws InvalidKeySpecException {
        if(!(keySpec instanceof RSAPublicKeySpec)) {
            throw new InvalidKeySpecException("KeySpec " + keySpec +
                                              ", not supported");
        }
        RSAPublicKeySpec rsaPub = (RSAPublicKeySpec)keySpec;
        return new RSAPublicKey(rsaPub.getModulus(),
                                rsaPub.getPublicExponent());
    }

    protected PrivateKey engineGeneratePrivate(KeySpec keySpec)
    throws InvalidKeySpecException {
        if(!(keySpec instanceof RSAPrivateKeySpec)) {
            throw new InvalidKeySpecException("KeySpec " + keySpec +
                                              ", not supported");
        }

        if(keySpec instanceof RSAPrivateCrtKeySpec) {
            RSAPrivateCrtKeySpec rsaPrv = (RSAPrivateCrtKeySpec)keySpec;
            return new RSAPrivateCrtKey(rsaPrv.getModulus(),
                                        rsaPrv.getPublicExponent(),
                                        rsaPrv.getPrivateExponent(),
                                        rsaPrv.getPrimeP(),
                                        rsaPrv.getPrimeQ(),
                                        rsaPrv.getPrimeExponentP(),
                                        rsaPrv.getPrimeExponentQ(),
                                        rsaPrv.getCrtCoefficient());
        }
        RSAPrivateKeySpec rsaPrv = (RSAPrivateKeySpec)keySpec;
        return new RSAPrivateKey(rsaPrv.getModulus(),
        		rsaPrv.getPrivateExponent());
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
