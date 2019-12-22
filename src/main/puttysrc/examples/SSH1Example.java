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

import java.io.*;
import java.net.*;
import java.util.*;
import com.mindbright.nio.NetworkConnection;
import com.mindbright.ssh.*;
import com.mindbright.sshcommon.*;
import java.security.interfaces.RSAPublicKey;

/**
 * Examples on how to use the ssh1 classes.
 * <p>
 * <strong>
 *
 *    NOTE that the ssh1 protocol is a deprecated protocol with known
 *    security vulnerabilities. Use version 2 if at all possible.
 *
 * </strong>
 * Moreover the SSH1 api in MindTerm is less structured and harder to
 * use. That said this class contains code demonstrating:
 * <ul>
 *   <li>How to set up a port-forward with SSH1</li>
 *   <li>How to start a command over an SSH1 connection and retrieve
 *       its output.</li>
 *   <li>How to start a shell over an SSH1 connection and run commands
 *       in it and retrieve the output</li>
 *   <li>How to copy a file with SCP over SSH1</li>
 * </ul>
 * Usage:
 * <code> java -cp examples.jar examples.SSH1Example
 * <em>host</em> <em>port</em> <em>username</em> <em>password</em>
 */
public class SSH1Example extends SSHInteractorAdapter
    implements SSHAuthenticator, SSHClientUser {
    // Modify this to point to a webserver
    private final static String A_WEB_HOST = "localhost";

    private Properties props = new Properties();

    private MYSSHClient client;

    /**
     * This subclass of SSHClient is needed to be able to see which
     * encryption algorithms are supported.
     */
    class MYSSHClient extends SSHClient {
        MYSSHClient(SSHAuthenticator authenticator, SSHClientUser user) {
            super(authenticator, user);
        }

        /**
         * Checks if a given encryption algorithm is supported.
         *
         * @param c Cipher algorithm <code>SSH.CIPHER_</code><em>name</em>
         */
        boolean myIsCipherSupported(int c) {
            int cipherMask = (0x01 << c);
            if((cipherMask & supportedCiphers) != 0)
                return true;
            return false;
        }
    }

    /**
     * Create an SSH1Example instance and store the login details in
     * properties.
     */
    SSH1Example(String host, String port, String user, String pass) {
        // Save properties for later use in methods from
        // the SSHAuthenticator and SSHClientUser interfaces
        props.put("server", host);
        props.put("port", port);
        props.put("username", user);
        props.put("password", pass);
        props.put("auth-method", "password");

        // To use public-key auth. uncomment the following line,
        // and modify the getIdentityFile() and getIdentityPassword()
        // methods below.
        // props.put("auth-method", "publickey");

        // Prepare the client
        client = new MYSSHClient(this, this);
    }

    /**
     * Start an SSH1 connection with port forward. Then demonstrate
     * reading and writing on this portforward.
     */
    public void runWithPortFwd() throws UnknownHostException, IOException {
        /*
         * Add the port forward from localhost:4083 to "a_WebHost"
         * (contant defined above) port 80. The portforward is of type
         * "general" (=no plugin)
         */
        client.addLocalPortForward(4083, A_WEB_HOST, 80, "general");

        /*
         * Boot the ssh protocol (causes it to connect).
         */
        client.bootSSH(false, false);

        /*
         * Open a TCP connection to the local end of the portforward.
         */
        Socket s = new Socket("localhost", 4083);
        OutputStream os = s.getOutputStream();

        // Send a simple web request
        os.write("GET / HTTP/1.0\r\n\r\n".getBytes());
        os.flush();

        // Get reply
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        BufferedReader in =
            new BufferedReader(new InputStreamReader(s.getInputStream()));
        for (;;) {
            String line = in.readLine();
            if (line == null)
                break;
            System.out.println(line);
        }
        System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        s.close();

        // Close everything
        client.forcedDisconnect();
        client.clearAllForwards();
    }


    /**
     * Start a command over SSH1 connection and run a command and
     * retrieve the output
     *
     * @param command Command to execute
     */
    public void runWithCommand(String command) throws IOException {
        // Create a suitable client instance
        SSHConsoleClient client =
            new SSHConsoleClient(getSrvHost(), getSrvPort(), this, this);

        // Register the command
        client.command(command);

        // Show output
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        BufferedReader in =
            new BufferedReader(new InputStreamReader(client.getStdOut()));
        for (;;) {
            String line = in.readLine();
            if (line == null)
                break;
            System.out.println(line);
        }
        System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
    }


    /**
     * Start a shell over SSH1 connection and run a command and
     * retrieve the output
     *
     * @param command Command to execute
     */
    public void runWithCommandInShell(String command) throws IOException {
        // Create a suitable client instance
        SSHConsoleClient client =
            new SSHConsoleClient(getSrvHost(), getSrvPort(), this, this);

        // Ask the client to start a shell on the server
        client.shell();

        // Send command to stdin of shell
        OutputStream os = client.getStdIn();
        os.write((command + "; exit\n").getBytes());
        os.flush();

        // Show output
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        BufferedReader in =
            new BufferedReader(new InputStreamReader(client.getStdOut()));
        for (;;) {
            String line = in.readLine();
            if (line == null)
                break;
            System.out.println(line);
        }
        System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
    }


    /**
     * Transfer files with SCP
     *
     * @param remotefile File on remote system to transfer
     */
    public void runSCP(String remotefile) throws IOException {
        SSHConsoleClient client;
        SSHSCP1 scp1;

        System.out.println("Copying file from remote host");
        client = new SSHConsoleClient(getSrvHost(), getSrvPort(), this, this);
        scp1 = new SSHSCP1(new File("."), client, true);
        scp1.copyToLocal("dummy", remotefile, false);
        client.close();

        System.out.println("Copying file to remote host");
        client = new SSHConsoleClient(getSrvHost(), getSrvPort(), this, this);
        scp1 = new SSHSCP1(new File("."), client, true);
        scp1.copyToRemote("dummy", "dummy2", false);
        client.close();

        (new File("dummy")).delete();
    }

    /**
     * Get property value
     */
    private String getProp(String key) {
        return (String)props.get(key);
    }

    /*
     * SSHAuthenticator implementation
     */
    /**
     * Get username to log in as
     */
    public String getUsername(SSHClientUser origin) throws IOException {
        return getProp("username");
    }

    /**
     * Get password to log in with
     */
    public String getPassword(SSHClientUser origin) throws IOException {
        return getProp("password");
    }

    /**
     * Present challenge to user an return answer
     */
    public String getChallengeResponse(SSHClientUser origin, String challenge)
    throws IOException {
        return null;
    }

    /**
     * Get the authentication method the user wants to us
     */
    public int[] getAuthTypes(SSHClientUser origin) {
        return SSH.getAuthTypes(getProp("auth-method"));
    }


    /**
     * Get desired encryption algorithm
     */
    public int getCipher(SSHClientUser origin) {
        return SSH.CIPHER_ANY;
    }

    /**
     * Return name of file containing private key for pubkey authentication
     */
    public SSHRSAKeyFile getIdentityFile(SSHClientUser origin)
    throws IOException {
        // Replace with your private key file here
        String idfile = System.getProperty("user.home") + File.separatorChar + ".ssh" + 
            File.separatorChar + "identity";
        return new SSHRSAKeyFile(idfile);
    }

    /**
     * Return password protecting identify file
     */
    public String getIdentityPassword(SSHClientUser origin)
    throws IOException {
        // If you have a password set on the private key file,
        // return it here
        return null;
    }

    /**
     * Verify the fingerprint of the remote host.
     *
     * @param hostPub public key of remote host
     *
     * @return true if the public key verifies
     */
    public boolean verifyKnownHosts(RSAPublicKey hostPub) throws IOException {
        // This is insecure, and vulnerable to man in the middle
        // attacks. At least we should remember the fingerprint after
        // the first session and compare against that.
        return true;
    }


    /*
     * SSHClientUser interface
     */
    /**
     * Get host to connect to
     */
    public String getSrvHost() throws IOException {
        return getProp("server");
    }

    /**
     * Get port number to connect to
     */
    public int getSrvPort() {
        return Integer.parseInt(getProp("port"));
    }

    /**
     * Return a connection to the server. This can be used to connect
     * through proxies etc.
     */
    public NetworkConnection getProxyConnection() throws IOException {
        return null;
    }

    /**
     * Get the display for X11 forwardings
     */
    public String getDisplay()           {
        return null;
    }

    /**
     * get maximum packet size (0 = no limit)
     */
    public int getMaxPacketSz()          {
        return 0;
    }

    /**
     * Get alive interval (0 = do not send keepalive packets)
     */
    public int getAliveInterval()        {
        return 0;
    }

    /**
     * Get desired level of compression
     */
    public int getCompressionLevel()     {
        return 0;
    }

    /**
     * Return true if X11 forwarding is desired
     */
    public boolean wantX11Forward()      {
        return false;
    }

    /**
     * Return true if we need a PTY on the server
     */
    public boolean wantPTY()             {
        return false;
    }

    /**
     * Timeout of key exchange (in seconds)
     */
    public int getKexTimeout()           {
        return 180;
    }

    /**
     * Timeout when connecting to server (in seconds)
     */
    public int getConnectTimeout()       {
        return 60;
    }

    /**
     * Timeout when waiting for initial greeting from server (in seconds)
     */
    public int getHelloTimeout()         {
        return 10;
    }

    /**
     * Get interactor which should handle the authentication phase
     */
    public SSHInteractor getInteractor() {
        return this;
    }


    /**
     * Run the application
     */
    public static void main(String argv[]) {
        if (argv == null || argv.length != 4) {
            System.out.println("Usage: SSH1Example <host> <port> <user> <pass>");
            return;
        }

        try {
            SSH1Example ssh1 =
                new SSH1Example(argv[0], argv[1], argv[2], argv[3]);
            ssh1.runWithPortFwd();
            ssh1.runWithCommand("/bin/ls");
            ssh1.runWithCommandInShell("/bin/ls");
            ssh1.runSCP(".bashrc"); // Must exist - change if needed.

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
