/******************************************************************************
 *
 * Copyright (c) 2009-2011 Cryptzone Group AB. All Rights Reserved.
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

package com.mindbright.util;

import java.math.BigInteger;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.NoSuchAlgorithmException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.Provider;
import java.security.Security;

import java.util.ArrayList;
import java.util.Enumeration;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;

// Crypto helper routines. Acquire all crypto objects through this class to
// make sure we use the correct JCE / implementations.

public final class Crypto {
    
    private static boolean fipsmode;
    private static ArrayList<String> preferredproviders = new ArrayList<String>();
    private static boolean useonlypreferredprovider;
    private static String prngname;
    private static SecureRandom rand;

    private static boolean unlimitedstrengthjce = false;
    private static boolean unlimiteddhsupport   = false;
    private static boolean ecdhsupport  = false;
    private static boolean ecdsasupport = false;
    private static boolean castsupport  = false;

    private static boolean DEBUG;
    
    private static String getSystemProperty (String key, String def) {
        try {
            return System.getProperty(key, def);
        } catch (Throwable t) {
            // applet...
        }
        return def;
    }

    private static void setPreferredProviders (String list) {
        preferredproviders.clear();
        try {
            String[] l = list.split(",");
            for (int i = 0; i < l.length; i++)
                if (!"".equals(l[i]))
                    preferredproviders.add(l[i]);
        } catch (Throwable t) {
        }
    }

    static {
        String s = getSystemProperty("mindterm.jce.debug", "no").toLowerCase();
        DEBUG = s.equals("yes") || s.equals("true");
        s = getSystemProperty("mindterm.jce.fipsmode", "no").toLowerCase();
        fipsmode = s.equals("yes") || s.equals("true");
        String jce = getSystemProperty("mindterm.jce.provider", null);
        setPreferredProviders (getSystemProperty("mindterm.jce.preferred", null));
        if (jce == null && preferredproviders.size() == 0 && !fipsmode) {
            jce = "org.bouncycastle.jce.provider.BouncyCastleProvider";
            preferredproviders.add("BC");
            preferredproviders.add("SunPKCS11-NSS");
        }
        try {
            if (jce != null) {
                Security.addProvider((Provider)Class.forName(jce).newInstance());
                if (DEBUG)
                    System.out.println("Added JCE: " + jce);
            }
        } catch (Throwable tt) {
        }
		prngname = getSystemProperty("mindterm.jce.prng", null);
        s = getSystemProperty("mindterm.jce.preferredonly", "no").toLowerCase();
        useonlypreferredprovider = s.equals("yes") || s.equals("true");
    }

    private static boolean supportchecked = false;
    
    private static void checkSupport() 
    {
        if (supportchecked) return;
        supportchecked = true;
        try {
            Cipher cipher = getCipher("AES/CBC/NoPadding");
            byte[] iv = new byte[16];
            javax.crypto.KeyGenerator kgen = javax.crypto.KeyGenerator.getInstance("AES");
            kgen.init(256);
            for (int i=0; i<iv.length; i++) iv[i] = (byte)i;
            cipher.init(Cipher.ENCRYPT_MODE, kgen.generateKey(), new javax.crypto.spec.IvParameterSpec(iv));
            unlimitedstrengthjce = true;
        } catch (Throwable t) {
        }

        try {
            if (getCipher("CAST5/CBC/NoPadding") != null)
                castsupport = true;
        } catch (Throwable t) {
        }

        try {
            getKeyPairGenerator("DH").initialize
                (new javax.crypto.spec.DHParameterSpec
                 (new BigInteger("FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1" +
                                 "29024E088A67CC74020BBEA63B139B22514A08798E3404DD" +
                                 "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245" +
                                 "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED" +
                                 "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D" +
                                 "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F" +
                                 "83655D23DCA3AD961C62F356208552BB9ED529077096966D" +
                                 "670C354E4ABC9804F1746C08CA237327FFFFFFFFFFFFFFFF", 16),
		  BigInteger.valueOf(2)
                     ), getSecureRandom());
            unlimiteddhsupport = true;
        } catch (Throwable t) {
        }

        try {
            if (getKeyPairGenerator("EC") != null &&
                getKeyAgreement("ECDH") != null)
                ecdhsupport = true;
        } catch (Throwable t) {
        }

        try {
            if (getSignature("SHA512/ECDSA") != null)
                ecdsasupport = true;
        } catch (Throwable t) {
        }        
    }

    public static void setProvider(String prov, 
                                   String preferredprov,
                                   boolean useonlypreferredprov,
                                   String prng, 
                                   boolean debug) 
        throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Security.addProvider((Provider)Class.forName(prov).newInstance());
        setPreferredProviders(preferredprov);
        useonlypreferredprovider = useonlypreferredprov;
        prngname = prng;
        DEBUG = debug;
    }
    
    public static void setFipsMode(boolean v) {
        fipsmode = v;
    }
    
    public static boolean isFipsMode() {
        return fipsmode;
    }

    public static boolean hasUnlimitedStrengthJCE() {
        checkSupport();
        return unlimitedstrengthjce;
    }
    
    public static boolean hasUnlimitedDHSupport() {
        checkSupport();
        return unlimiteddhsupport;
    }

    public static boolean hasECDHSupport() {
        checkSupport();
        return ecdhsupport;
    }

    public static boolean hasECDSASupport() {
        checkSupport();
        return ecdsasupport;
    }

    public static boolean hasCASTSupport() {
        checkSupport();
        return castsupport;
    }

    public static SecureRandomAndPad getSecureRandomAndPad() {
        return getSecureRandomAndPad(null);
    }

    public static SecureRandomAndPad getSecureRandomAndPad(byte[] s) {
		return new SecureRandomAndPad(getSecureRandom(s));
    }

    public static SecureRandom getSecureRandom() {
        return getSecureRandom(null);
    }

    public synchronized static SecureRandom getSecureRandom(byte[] s) {
        if (rand == null) {
            if (DEBUG)
                System.out.println("Initializing PRNG" + ((s==null) ? "" : " with seed"));
            if (prngname != null || useonlypreferredprovider) {
                for (String preferredprovider : preferredproviders) {
                    if (rand != null)
                        break;
                    try {
                        rand = SecureRandom.getInstance(prngname, preferredprovider);
                    } catch (Throwable t) {
                    }
                }
                if (rand == null && useonlypreferredprovider)
                    return null;
                try {
                    rand = SecureRandom.getInstance(prngname);
                } catch (Throwable t) {
                }
            }
            if (rand == null)
                rand = new SecureRandom();
            if (s != null)
                rand.setSeed(s);
            if (DEBUG)
                System.out.println("Using PRNG: " + rand + " provider: " + rand.getProvider());
        }
        return rand;
    }


    public static MessageDigest getMessageDigest(String alg) throws NoSuchAlgorithmException {
        MessageDigest md = null;        
        
        try { 
            for (String preferredprovider : preferredproviders) {
                if (md != null)
                    break;
                try {
                    md = MessageDigest.getInstance(alg, preferredprovider);
                } catch (Throwable t) {
                }
            }
            if (md == null && !useonlypreferredprovider)
                md = MessageDigest.getInstance(alg);
        } finally {
            if (DEBUG)
                System.out.println("getMD("+alg+"): " + 
                                   ((md == null) ? "not found" : "provider="+md.getProvider()));
        }
        return md;
    }


    public static Mac getMac(String alg) throws NoSuchAlgorithmException {
        Mac m = null;
        try { 
            for (String preferredprovider : preferredproviders) {
                if (m != null)
                    break;
                try {
                    m = Mac.getInstance(alg, preferredprovider);
                } catch (Throwable t) {
                }
            }
            if (m == null && !useonlypreferredprovider)
                m = Mac.getInstance(alg);
        } finally {
            if (DEBUG)
                System.out.println("getMac("+alg+"): " + 
                                   ((m == null) ? "not found" : "provider="+m.getProvider()));
        }
        return m;
    }

    public static Cipher getCipher(String alg) throws NoSuchAlgorithmException, NoSuchPaddingException {
        Cipher c = null;
        try { 
            for (String preferredprovider : preferredproviders) {
                if (c != null)
                    break;
                try {
                    c = Cipher.getInstance(alg, preferredprovider);
                } catch (Throwable t) {
                }
            }
            if (c == null && !useonlypreferredprovider) {
                c = Cipher.getInstance(alg); 
            }
        } finally {   
            if (DEBUG)
                System.out.println("getCipher("+alg+"): " + 
                                   ((c == null) ? "not found" : "provider="+c.getProvider()));
        }
        return c;
    }


    public static Signature getSignature(String alg) throws NoSuchAlgorithmException {
        Signature instance = null;
        try { 
            for (String preferredprovider : preferredproviders) {
                if (instance != null)
                    break;
                try {
                    instance = Signature.getInstance(alg, preferredprovider);
                } catch (Throwable t) {
                }
            }
            if (instance == null && !useonlypreferredprovider)
                instance = Signature.getInstance(alg); 
        } finally {
            if (DEBUG)
                System.out.println("getSignature("+alg+"): " + 
                                   ((instance == null) ? "not found" : "provider="+instance.getProvider()));
        }
        return instance;
    }

    public static KeyPairGenerator getKeyPairGenerator(String alg) throws NoSuchAlgorithmException {
        KeyPairGenerator instance = null;
        try { 
            for (String preferredprovider : preferredproviders) {
                if (instance != null)
                    break;
                try {
                    instance = KeyPairGenerator.getInstance(alg, preferredprovider);
                } catch (Throwable t) {
                }
            }
            if (instance == null && !useonlypreferredprovider)
                instance = KeyPairGenerator.getInstance(alg); 
        } finally {
            if (DEBUG)
                System.out.println("getKeyPairGenerator("+alg+"): " + 
                                   ((instance == null) ? "not found" : "provider="+instance.getProvider()));
        }
        return instance;
    }

    public static KeyFactory getKeyFactory(String alg) throws NoSuchAlgorithmException {
        KeyFactory instance = null;
        try { 
            for (String preferredprovider : preferredproviders) {
                if (instance != null)
                    break;
                try {
                    instance = KeyFactory.getInstance(alg, preferredprovider);
                } catch (Throwable t) {
                }
            }
            if (instance == null && !useonlypreferredprovider)
                instance = KeyFactory.getInstance(alg); 
        } finally {
            if (DEBUG)
                System.out.println("getKeyFactory("+alg+"): " + 
                                   ((instance == null) ? "not found" : "provider="+instance.getProvider()));
        }
        return instance;
    }

    public static KeyAgreement getKeyAgreement(String alg) throws NoSuchAlgorithmException {
        KeyAgreement instance = null;
        try { 
            for (String preferredprovider : preferredproviders) {
                if (instance != null)
                    break;
                try {
                    instance = KeyAgreement.getInstance(alg, preferredprovider);
                } catch (Throwable t) {
                }
            }
            if (instance == null && !useonlypreferredprovider)
                instance = KeyAgreement.getInstance(alg); 
        } finally {
            if (DEBUG)
                System.out.println("getKeyAgreement("+alg+"): " + 
                                   ((instance == null) ? "not found" : "provider="+instance.getProvider()));
        }
        return instance;
    }

    
    
    public static String getKeyName(Cipher c) {
        String s = c.getAlgorithm();
        int idx = s.indexOf('/');
        if (idx > 0)
            return s.substring(0, idx);
        return s;
    }


    public static void main(String argv[]) {
        try { 
            System.out.println("Unlimited strength JCE: " + hasUnlimitedStrengthJCE());
	    System.out.println("Unlimited DH support: " + hasUnlimitedDHSupport());
	    System.out.println("ECDH support: " + hasECDHSupport());
	    System.out.println("ECDSA support: " + hasECDHSupport());
	    System.out.println("CAST support: " + hasCASTSupport());

	    boolean hasmac = false;
	    try {
		    hasmac = getMac("HmacSHA256") != null;
	    } catch (Throwable t) {
	    }
	    System.out.println("HmacSHA256 support: " + hasmac);

	    hasmac = false;
	    try {
		    hasmac = getMac("HmacSHA512") != null;
	    } catch (Throwable t) {
	    }
	    System.out.println("HmacSHA512 support: " + hasmac);
	    
            Provider[] ps = Security.getProviders();
            for (int i=0; i<ps.length; i++) {
                Enumeration<?> e = ps[i].propertyNames();
                for ( ; e.hasMoreElements() ;) {
                    String key = (String)e.nextElement();
                    if (key.indexOf("SecureRandom") != -1)
                        System.out.println(ps[i] + " : " + key);
                }
            }
        } catch (Throwable t) {
        }
    }
    
}


