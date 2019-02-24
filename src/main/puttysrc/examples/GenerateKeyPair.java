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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Properties;

import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import com.mindbright.util.Crypto;

import com.mindbright.ssh2.SSH2PublicKeyFile;
import com.mindbright.ssh2.SSH2KeyPairFile;
import com.mindbright.ssh2.SSH2Exception;

/**
 * This is a simple demo of how to generate public key pairs for use with ssh2.
 * A file containing some definitions of the key must supplied as argument.
 * <p>
 * Usage:
 * <code> java -cp examples.jar examples.GenerateKeyPair
 * <em>definition_file</em>
 * <p>
 * The definition file is a standard Java property file which should
 * contain the following properties:
 * <dl>
 *   <dt>format</dt>
 *   <dd>Format of keyfile. Valid values are <code>openssh</code> and
 *       <code>sshinc</code>. The default value is openssh.</dd>
 *
 *   <dt>algorithm</dt>
 *   <dd>Which algorithm to generate a key for. Valid valus are
 *       <code>RSA</code> and <code>DSA</code>. The default value is RSA.</dd>
 *
 *   <dt>bits</dt>
 *   <dd>Number of bits in key. The default value is 1024.</dd>
 *
 *   <dt>password</dt>
 *   <dd>Password used to encrypt the private key</dd>
 *
 *   <dt>subject</dt>
 *   <dd>String identifying the owner of the key</dd>
 *
 *   <dt>comment</dt>
 *   <dd>Key comment</dd>
 *
 *   <dt>keyfile</dt>
 *   <dd>Base-name of files to save keys in. The private file will be
 *   stored in <em>keyfile</em> and the public key in
 *   <em>keyfile</em><code>.pub</code></dd>
 */
public class GenerateKeyPair {

    /**
     * Actually generate the key pair
     *
     * @param alg algorithm to generate for (RSA or DSA)
     * @param bits key size
     * @param rand random number source
     */
    public static KeyPair generateKeyPair(String alg, int bits,
                                          SecureRandom rand)
    throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(alg);
        kpg.initialize(bits, rand);
        return kpg.generateKeyPair();
    }

    /**
     * Save the given keypair to a file.
     */
    public static void saveKeyPair(KeyPair kp,
                                   String passwd, String fileName,
                                   String subject, String comment,
                                   boolean sshComFormat,
                                   SecureRandom rand)
    throws IOException, SSH2Exception {
        String pubKeyStr = null;

        SSH2PublicKeyFile pkif = new SSH2PublicKeyFile(kp.getPublic(),
                                 subject, comment);

        // When key is unencrypted OpenSSH doesn't tolerate headers...
        //
        if(!sshComFormat && (passwd == null || passwd.length() == 0)) {
            subject = null;
            comment = null;
        }

        SSH2KeyPairFile kpf = new SSH2KeyPairFile(kp, subject, comment);

        kpf.store(fileName, rand, passwd, sshComFormat);

        pubKeyStr = pkif.store(fileName + ".pub", sshComFormat);

        System.out.println("Key saved to " + fileName + " and " + fileName +
                           ".pub");
        System.out.println("Public key:");
        System.out.println(pubKeyStr);
    }

    /**
     * Run the application
     */
    public static void main(String[] argv) {
        try {
            if(argv.length != 1) {
                System.out.println("usage: GenerateKeyPair <definition-file>");
                System.exit(1);
            }
            String     file       = argv[0];
            Properties defaults   = new Properties();
            defaults.put("format",    "openssh");
            defaults.put("algorithm", "RSA");
            defaults.put("bits",      "1024");

            /*
             * Check and extract properties
             */
            Properties definition = new Properties(defaults);
            definition.load(new FileInputStream(file));

            SecureRandom rand      = Crypto.getSecureRandom();
            String       format    = definition.getProperty("format");
            String       algorithm = definition.getProperty("algorithm");
            int          bits      = Integer.parseInt(definition.
                                     getProperty("bits"));

            String keyfile = definition.getProperty("keyfile");
            if(keyfile == null || keyfile.trim().length() == 0) {
                throw new Exception("no 'keyfile' provided");
            }

            // Generate the key pair
            System.out.println("Generating key: " + algorithm + "(" + bits +
                               ") in " + format + " format, please wait...");
            KeyPair kp = generateKeyPair(algorithm, bits, rand);

            // Save keys to file
            boolean sshComFormat = "sshinc".equals(format);
            saveKeyPair(kp, definition.getProperty("password"),	keyfile,
                        definition.getProperty("subject"),
                        definition.getProperty("comment"),
                        sshComFormat, rand);

        } catch (FileNotFoundException e) {
            System.out.println("Couldn't load file: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("An error occured: " + e.getMessage());
        }
    }

}
