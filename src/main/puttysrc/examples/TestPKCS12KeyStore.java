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

import java.io.FileInputStream;

import java.util.Enumeration;

import java.security.KeyStore;
import java.security.cert.Certificate;

/**
 * List all keys found in a PKCS#12 file.
 * <p>
 * Usage:
 * <code> java -cp examples.jar examples.TestPKCS12KeyStore
 * <em>filename</em> <em>password</em>
 */
public class TestPKCS12KeyStore {

    /**
     * Run the application
     */
    public static void main(String[] argv) {
        try {
            // Check arguments
            if(argv.length != 2) {
                System.out.println("usage: TestPKCS12KeyStore <dirname> "
                                   + "<password>");
                System.exit(1);
            }
            String fileName = argv[0];
            String password = argv[1];

            // Create instance of KeyStore for PKCS#12 keys
            KeyStore p12 = KeyStore.getInstance("PKCS12");

            // Load the keys in the store
            p12.load(new FileInputStream(fileName), password.toCharArray());

            // Loop through the found certificates
            Enumeration<?> e = p12.aliases();
            while(e.hasMoreElements()) {
                String alias = (String)e.nextElement();
                System.out.println(alias + " (" +
                                   (p12.isKeyEntry(alias) ?
                                    "cert" : "trusted cert") + ")");
                Certificate cert = p12.getCertificate(alias);
                System.out.println("\t" + cert);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error: " + e);
        }
    }

}
