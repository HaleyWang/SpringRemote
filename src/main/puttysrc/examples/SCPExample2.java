/******************************************************************************
 *
 * Copyright (c) 2006-2011 Cryptzone Group AB. All Rights Reserved.
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
import java.io.IOException;

import java.net.UnknownHostException;

import java.security.interfaces.RSAPublicKey;

import java.util.Vector;

import com.mindbright.nio.NetworkConnection;

import com.mindbright.ssh.*;

import com.mindbright.ssh2.SSH2Exception;
import com.mindbright.ssh2.SSH2SCP1Client;
import com.mindbright.ssh2.SSH2SimpleClient;
import com.mindbright.ssh2.SSH2Transport;

import com.mindbright.sshcommon.SSHSCP1;

import com.mindbright.util.Crypto;
import com.mindbright.util.Util;

/**
 * Copy a file to/from an SSH1 or SSH2 server using the scp protocol. 
 * The scp protocol was introduced with ssh1 and is very similar to the rcp
 * protocol.
 * <p>
 * See the <code>main</code> method for an example on how to do a file
 * transfer, and also how to expand glob expressions.<p>
 * The <code>copyFilesWithSSH1</code> method will copy files using the SSH1 protocol.<p>
 * The <code>copyFilesWithSSH2</code> method will copy files using the SSH2 protocol.<p>
 * The <code>doGlob</code> method will expand glob expressions.<p>
 *
 * @see SSHSCP1
 * @see SFTPCopyFile
 */

public class SCPExample2 {

    ///////////////////////////////////////////////////////////////////////////
    //

    private static boolean fileExists(String file) {
        try {
            return (new File(file)).exists();
        } catch (Throwable t) {
        }
        return false;
    }


    // try to use OpenSSH's private key files if no privatekeyfile
    // was given
    private static String findPrivateKeyFile(int version) {
        String base = System.getProperty("user.home") + 
            File.separatorChar + ".ssh" + File.separatorChar;
        if (version == 1) return base + "identity";
        String privatekeyfile = base + "id_rsa";
        if (!fileExists(privatekeyfile))
            privatekeyfile = base + "id_dsa";
        return privatekeyfile;
    }

    // implement the needed stubs for SSH1
    private static class MySSHInteractor extends SSHInteractorAdapter implements SSHAuthenticator {
        private String user, keyfile, keypass;

        MySSHInteractor(String u, String kf, String kp) {
            user = u;
            keyfile = kf;
            keypass = kp;
        }

        public String getUsername(SSHClientUser origin) throws IOException {
            return user;
        }
        
        public String getPassword(SSHClientUser origin) throws IOException {
            return "";
        }
        
        public String getChallengeResponse(SSHClientUser origin, String challenge) throws IOException {
            return "";
        }
        
        public int[] getAuthTypes(SSHClientUser origin) {
            return new int[] { SSH.AUTH_PUBLICKEY };   
        }
        
        public int getCipher(SSHClientUser origin) {
            return SSH.CIPHER_DEFAULT;
        }
        
        public SSHRSAKeyFile getIdentityFile(SSHClientUser origin) throws IOException {
            return new SSHRSAKeyFile(keyfile);
        }
        
        public String getIdentityPassword(SSHClientUser origin) throws IOException {
            return keypass;
        }
        
        public boolean verifyKnownHosts(RSAPublicKey hostPub) throws IOException {
            return true;
        }
        
    }
    

    /**
     * This copies files using an SSH1 connection.
     *
     * @param server server name
     * @param port server port
     * @param user user name
     * @param privatekeyfile the private key file, if null we will try to find the OpenSSH private key file
     * @param privatekeypassword the password for the private key file, null if none needed
     * @param files an array of files that should be transferred
     * @param destination the destination file or directory
     * @param toremote true if files should be copied to the remote server, otherwise false
     */

    public static void copyFilesWithSSH1(String server,
                                         int port,
                                         String user,
                                         String privatekeyfile,
                                         String privatekeypassword,
                                         String[] files, 
                                         String destination,
                                         boolean toremote) 
        throws IOException, UnknownHostException {

        SSHConsoleClient client = null;
        
        try {
            System.out.println("ssh1");
            
            if (privatekeyfile == null)
                privatekeyfile = findPrivateKeyFile(1);
            MySSHInteractor interactor = new MySSHInteractor(user, privatekeyfile, privatekeypassword);
            client = new SSHConsoleClient(server, port, interactor, interactor);
            SSHSCP1 scp1 = new SSHSCP1(new File("."), client, false);
            doFileCopy(scp1, files, destination, toremote);
        } finally {
            if (client != null) client.close();
        }
    }
    
    /**
     * This copies files using an SSH2 connection.
     *
     * @param server server name
     * @param port server port
     * @param user user name
     * @param privatekeyfile the private key file, if null we will try to find the OpenSSH private key file
     * @param privatekeypassword the password for the private key file, null if none needed
     * @param files an array of files that should be transferred
     * @param destination the destination file or directory
     * @param toremote true if files should be copied to the remote server, otherwise false
     */

    public static void copyFilesWithSSH2(String server,
                                         int port,
                                         String user,
                                         String privatekeyfile,
                                         String privatekeypassword,
                                         String[] files, 
                                         String destination,
                                         boolean toremote) 
        throws IOException, UnknownHostException, SSH2Exception
    {
            System.out.println("ssh2");
        SSH2Transport transport = null;

        try {
            // Connect to the server and authenticate using public key
            // authentication
            NetworkConnection socket = NetworkConnection.open(server, port);
            transport = new SSH2Transport(socket, Crypto.getSecureRandomAndPad());

            if (privatekeyfile == null)
                privatekeyfile = findPrivateKeyFile(2);

            SSH2SimpleClient client = new SSH2SimpleClient
                (transport, user, privatekeyfile, privatekeypassword);
            
            // Create SSH2SCP1 client which is used for the file transfer
            SSH2SCP1Client scpClient =
                new SSH2SCP1Client(new File(System.getProperty("user.dir")),
                                   client.getConnection(), System.err, false);            
            
            doFileCopy(scpClient.scp1(), files, destination, toremote);
        } finally {

            // Disconnect the transport layer gracefully
            if (transport != null)
                transport.normalDisconnect("User disconnects");
        }        
    }

    private static void doFileCopy(SSHSCP1 scp, String[] files, String destination, boolean toremote) 
       throws IOException {

        // use '/' as separators
        destination = destination.replace(File.separatorChar, '/');
        
        // copy files
        for (int i=0; i<files.length; i++) {
            // use '/' as separators
            files[i] = files[i].replace(File.separatorChar, '/');
            // System.out.println("copying: " + files[i]);
            if (toremote) {
                scp.copyToRemote(files[i], destination, false);
            } else {
                scp.copyToLocal(files[i], destination, false);
            }    
        }
    }
    
    // do simplistic globexp expansion of filenames
    private static void doGlob(Vector<String> v, String filespec) {
        try {
            File f = new File(filespec);
            String path = f.getParent();
            if (path == null) {
                v.addElement(filespec);
                return;
            }
            
            File dir = new File(path);
            String wcard = f.getName();
            
            // translate globexps to regexps
            wcard = wcard.replaceAll("\\.", "\\\\.");
            wcard = wcard.replaceAll("\\*", ".*");
            wcard = wcard.replace('?', '.');
            
            final String regex = wcard;

            String[] files = dir.list(new java.io.FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.matches(regex);
                }
            });
            if (files != null)
                for (int i=0; i<files.length; i++)
                    v.addElement(path + File.separatorChar + files[i]);
        } catch (Throwable t) {
//            t.printStackTrace();
        }
    }

    //
    ////////////////////////////////////////////////////////////////////////////////////////

    
    /**
     * Run the application
     */
    public static void main(String[] argv) {
        if(argv.length < 6) {
            System.out.println
            ("Usage: SCPExample2 <server:port> <username> 1|2 to|from <srcfiles...> <destination>");
            System.exit(1);
        }

        try {
            String server = argv[0];
            String user   = argv[1];
            int    port;

            port = Util.getPort(server, 22);
            server = Util.getHost(server);

            Vector<String> v = new Vector<String>();
            
            for (int i=4; i<argv.length-1; i++)
                doGlob(v, argv[i]);

            if (argv[2].equals("1")) {
                copyFilesWithSSH1(server, port, user, null, null, 
                                  v.toArray(new String[v.size()]),
                                  argv[argv.length-1], argv[3].equals("to"));
            } else {
                copyFilesWithSSH2(server, port, user, null, null, 
                                  v.toArray(new String[v.size()]),
                                  argv[argv.length-1], argv[3].equals("to"));
            }            
            
            System.out.println("transfer done");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
