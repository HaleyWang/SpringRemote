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
package examples;

/**
 * Benchmark the different encryption algorithms. The code as shipped
 * tests the MindTerm implementations of the algorithms. By
 * uncommenting relevant portions, and building with Java 1.4 or
 * later, it is possible to also benchmark some of the implementations
 * in the Java runtime.
 * <p>
 * Usage:
 * <code> java -cp examples.jar examples.CipherBenchmark</code>
 */
import java.security.Provider;
import java.security.Security;

public class CipherBenchmark {
    
    private static Provider[] providers;
    
    private static void init(String[] provs) {
        if (provs != null) {
            for (int i=0; i<provs.length; i++)
                try {
                    Security.addProvider(((Provider)Class.forName(provs[i]).newInstance()));
                } catch (Throwable t) {
                    t.printStackTrace();
                }
        }

        providers = Security.getProviders();
    }
    
    /**
     * Fill the given array with zeros
     */
    private static void clear(byte[] b) {
        for (int i=0; i<b.length; i++)
            b[i] = 0x00;
    }

    /**
     * How long to test each algorithm
     */
    private final static double RUNTIME = 5.0;

    /**
     * Benchmark an algorithm, both encryption and decryption
     *
     * @param name      name to print when presenting result
     * @param cipher    formal name of algorithm to test
     * @param keylen    length of keys (bytes)
     * @param blocksize length of block to encrypt (bytes)
     * @param ivlength  length of initialization vector (bytes)
     */
    public static void benchCipher(String name, String cipher, int keylen,
                                   int blocksize, int ivlength) 
    {
        boolean found = false;
        for (int i=0; i<providers.length; i++) {
            try {
                benchCipher(name, cipher, providers[i].getName(), true, keylen, blocksize, ivlength);
                benchCipher(name, cipher, providers[i].getName(), false, keylen, blocksize, ivlength);
                found = true;
            } catch (java.security.GeneralSecurityException e) {
            }
        }
        if (!found) 
            System.out.println(" ** " + name + " - " + cipher + " not supported");        
    }
    
    /**
     * Benchmark an algorithm
     *
     * @param name      name to print when presenting result
     * @param cipher    formal name of algorithm to test
     * @param provider  which encryption provider to use
     * @param encrypt   if true test encryption otherwise decryption
     * @param keylen    length of keys (bytes)
     * @param blocksize length of block to encrypt (bytes)
     * @param ivlength  length of initialization vector (bytes)
     */
    public static void benchCipher(String name, String cipher, String provider, boolean encrypt,
                                   int keylen, int blocksize, int ivlength)
        throws java.security.GeneralSecurityException {
        javax.crypto.Cipher c =
            javax.crypto.Cipher.getInstance(cipher, provider);
        byte[] in  = new byte[blocksize];
        byte[] out = new byte[blocksize];
        byte[] key = new byte[keylen];

        clear(in);
        clear(out);
        clear(key);

        String algShortName;
        if (cipher.indexOf('/') > 0) {
            algShortName = cipher.substring(0, cipher.indexOf('/'));
        } else {
            algShortName = cipher;
        }

        if (ivlength > 0) {
            byte[] iv  = new byte[ivlength];
            clear(iv);
            c.init(encrypt ? javax.crypto.Cipher.ENCRYPT_MODE : javax.crypto.Cipher.DECRYPT_MODE, 
                   new javax.crypto.spec.SecretKeySpec(key, algShortName),
                   new javax.crypto.spec.IvParameterSpec(iv));
        } else {
            c.init(encrypt ? javax.crypto.Cipher.ENCRYPT_MODE : javax.crypto.Cipher.DECRYPT_MODE,
                   new javax.crypto.spec.SecretKeySpec(key, algShortName));
        }

        long start = System.currentTimeMillis();
        for (int i=0; i<5000; i++)
            c.doFinal(in, 0, in.length, out, 0);
        long end = System.currentTimeMillis();

        int m = (int)((RUNTIME / ((end-start)/1000.0)) * 5000);

        start = System.currentTimeMillis();
        long size = 0;
        for (int i=0; i<m; i++) {
            size += in.length;
            c.doFinal(in, 0, in.length, out, 0);
        }
        end = System.currentTimeMillis();

        double t = end - start;
        double mbps = (((int)(size / t))) / 1000.0;

        String lname = name + ",                ";
        lname = lname.substring(0,16);
        System.out.println(lname+" "+(encrypt?"en":"de")+"crypt: "
                           + (float)mbps + " MBps (Provider: " + provider + ")");
    }

    /**
     * Benchmark an hash algorithm
     *
     * @param name      name to print when presenting result
     * @param algorithm formal name of algorithm to test
     * @param blocksize length of block to operate on
     */
    public static void benchHash(String name, String algorithm,
                                 String provider, int blocksize)
        throws java.security.GeneralSecurityException {
        java.security.MessageDigest digest = 
            java.security.MessageDigest.getInstance(algorithm, provider);

        byte[] in  = new byte[blocksize];
        clear(in);

        long start = System.currentTimeMillis();
        for (int i=0; i<5000; i++)
			digest.digest(in);
        long end = System.currentTimeMillis();

        int m = (int)((RUNTIME / ((end-start)/1000.0)) * 3000);

        start = System.currentTimeMillis();
        long size = 0;
        for (int i=0; i<m; i++) {
            size += in.length;
            digest.digest(in);
        }
        end = System.currentTimeMillis();

        double t = end - start;
        double mbps = (((int)(size / t))) / 1000.0;

        String spec = name + ", on " + blocksize +
            " byte blocks:                                    ";
        String num = (float)mbps + " MBps          ";
        System.out.println(spec.substring(0, 38) + num.substring(0, 14) + " (Provider: " + provider + ")");
    }

    public static void benchHash(String name, String algorithm)
    {
        boolean found = false;
        for (int i=0; i<providers.length; i++) {
            try {
//                 benchHash(name, algorithm, providers[i].getName(), 16);
//                 benchHash(name, algorithm, providers[i].getName(), 64);
//                 benchHash(name, algorithm, providers[i].getName(), 256);
//                 benchHash(name, algorithm, providers[i].getName(), 1024);
                benchHash(name, algorithm, providers[i].getName(), 8192);
                found = true;
            } catch (java.security.GeneralSecurityException e) {
            }
        }
        if (!found) 
            System.out.println(" ** " + name + " - " + algorithm + " not supported");
    }

    /**
     * Benchmark an hmac algorithm
     *
     * @param name      name to print when presenting result
     * @param algorithm formal name of algorithm to test
     * @param keylen    length of keys (bytes)
     * @param blocksize length of block to operate on
     */
    public static void benchMac(String name, String algorithm, String provider,
                                int keylen, int blocksize)
        throws java.security.GeneralSecurityException {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance(algorithm, provider);

        byte[] in  = new byte[blocksize];
        byte[] key = new byte[keylen];
        byte[] out = new byte[mac.getMacLength()];

        clear(in);
        clear(key);
        
        mac.init(new javax.crypto.spec.SecretKeySpec(key, mac.getAlgorithm()));
        
        long start = System.currentTimeMillis();
        for (int i=0; i<5000; i++) {
            mac.reset();
            mac.update(in);
            mac.doFinal(out, 0);
        }
        long end = System.currentTimeMillis();

        int m = (int)((RUNTIME / ((end-start)/1000.0)) * 3000);

        start = System.currentTimeMillis();
        long size = 0;
        for (int i=0; i<m; i++) {
            size += in.length;
            mac.reset();
            mac.update(in);
            mac.doFinal(out, 0);
        }
        end = System.currentTimeMillis();

        double t = end - start;
        double mbps = (((int)(size / t))) / 1000.0;

        String spec = name + ", on " + blocksize +
            " byte blocks:                                    ";
        String num = (float)mbps + " MBps          ";
        System.out.println(spec.substring(0, 38) + num.substring(0, 14) + " (Provider: " + provider + ")");
    }

    public static void benchMac(String name, String algorithm,
                                     int keylen)
    {
        boolean found = false;
        for (int i=0; i<providers.length; i++) {
            try {
//                 benchMac(name, algorithm, providers[i].getName(), keylen, 16);
//                 benchMac(name, algorithm, providers[i].getName(), keylen, 64);
//                 benchMac(name, algorithm, providers[i].getName(), keylen, 256);
//                 benchMac(name, algorithm, providers[i].getName(), keylen, 1024);
                benchMac(name, algorithm, providers[i].getName(), keylen, 8192);
                found = true;
            } catch (java.security.NoSuchAlgorithmException e) {
                // it's ok
            } catch (java.security.GeneralSecurityException e) {
                e.printStackTrace();
            }
        }
        if (!found) 
            System.out.println(" ** " + name + " - " + algorithm + " not supported");
    }

    /**
     * Run the benchmark
     */
    public static void main(String[] argv) {
        try {
            init(argv);

            boolean doHash   = false;
            boolean doMac    = true;
            boolean doCipher = true;
            
            if (doHash) {
                benchHash("md2", "MD2");
                benchHash("md4", "MD4");
                benchHash("md5", "MD5");
                benchHash("ripemd160", "RIPEMD160");
                benchHash("sha1", "SHA-1");
                benchHash("sha256", "SHA-256");
            }
            
            if (doMac) {
                benchMac("hmac(md5)",       "HmacMD5", 16);
                benchMac("hmac(sha1)",      "HmacSHA1", 20);
                benchMac("hmac(sha256)",    "HmacSHA256", 20);
                benchMac("hmac(sha384)",    "HmacSHA384", 20);
                benchMac("hmac(sha512)",    "HmacSHA512", 20);
                benchMac("hmac(md5-96)",    "HmacMD5-96", 16);
                benchMac("hmac(sha1-96)",   "HmacSHA1-96", 20);
                benchMac("hmac(ripemd160)", "HmacRIPEMD160", 20);
            }

            if (doCipher) {
                benchCipher("3des-cbc", "DESede/CBC/NoPadding", 24, 1024, 8);
                benchCipher("blowfish-cbc", "Blowfish/CBC/NoPadding",  16, 1024, 16);
                benchCipher("cast128-cbc", "CAST5/CBC/NoPadding",  16, 1024, 16);
                benchCipher("idea-cbc", "IDEA/CBC/NoPadding",  16, 1024, 16);
                benchCipher("arcfour", "RC4",  16, 1024, 0);
                benchCipher("aes128-cbc", "AES/CBC/NoPadding",  16, 1024, 16);
                benchCipher("aes128-ctr", "AES/CTR/NoPadding",  16, 1024, 16);
                benchCipher("aes192-cbc", "AES/CBC/NoPadding",  24, 1024, 16);
                benchCipher("aes192-ctr", "AES/CTR/NoPadding",  24, 1024, 16);
                benchCipher("aes256-cbc", "AES/CBC/NoPadding",  32, 1024, 16);
                benchCipher("aes256-ctr", "AES/CTR/NoPadding",  32, 1024, 16);
                benchCipher("twofish128-cbc", "Twofish/CBC/NoPadding",  16, 1024, 16);
                benchCipher("twofish192-cbc", "Twofish/CBC/NoPadding",  24, 1024, 16);
                benchCipher("twofish256-cbc", "Twofish/CBC/NoPadding",  32, 1024, 16);
            }

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

}
