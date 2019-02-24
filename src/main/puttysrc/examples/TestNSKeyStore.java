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

import java.io.ByteArrayInputStream;

import java.util.Enumeration;

import java.security.KeyStore;
import java.security.Key;

/**
 * List all keys found in a Netscape keystore.
 * <p>
 * Usage:
 * <code> java -cp examples.jar examples.TestNSKeyStore
 * <em>dirname</em> <em>password</em>
 */
public class TestNSKeyStore {

    /**
     * Run the application
     */
    public static void main(String[] argv) {
        try {
            // Check arguments
            if(argv.length != 2) {
                System.out.println("usage: TestNSKeyStore <dirname> "
                                   + "<password>");
                System.exit(1);
            }
            String dirName = argv[0];
            String password = argv[1];

            // Create instance of KeyStore for Netscape keys
            KeyStore ns = KeyStore.getInstance("Netscape");

            // Create input stream which reads the directory
            ByteArrayInputStream ba =
                new ByteArrayInputStream(dirName.getBytes());

            // Loads the data stored in the given directory
            ns.load(ba, password.toCharArray());

            // Loop through the found certificates
            Enumeration<?> e = ns.aliases();
            while(e.hasMoreElements()) {
                String  alias = (String)e.nextElement();
                boolean isKey = ns.isKeyEntry(alias);
                System.out.println(alias + " (" +
                                   (isKey ? "cert" : "trusted cert") + ")");
                System.out.println("\t" + ns.getCertificate(alias));
                if(isKey) {
                    Key key = ns.getKey(alias, password.toCharArray());
                    if(key instanceof
                       java.security.interfaces.RSAPrivateKey) {
                        System.out.println("\tfound RSA key");
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: " + e);
        }
    }
}
