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

import java.util.*;

import java.security.interfaces.RSAPublicKey;

import com.mindbright.nio.NetworkConnection;

import com.mindbright.ssh.*;

/**
 * Examples on how to connect to an old Cisco device and send a single
 * command. All using the ssh1 classes.
 * <p>
 * Usage:
 * <code> java -cp examples.jar examples.SSH1Cisco
 * <em>host</em> <em>port</em> <em>username</em> <em>password</em>
 * <em>command</em>
 */
public class SSH1Cisco extends SSHInteractorAdapter
    implements SSHAuthenticator, SSHClientUser {

    private static final String END_MARKER = "echo END_MARKER";

    private Properties props = new Properties();

    private SSHConsoleClient client;

    /**
     * Create an SSH1Example instance and store the login details in
     * properties.
     */
    SSH1Cisco(String host, String port, String user, String pass)
        throws IOException {
        // Save properties for later use in methods from
        // the SSHAuthenticator and SSHClientUser interfaces
        props.put("server", host);
        props.put("port", port);
        props.put("username", user);
        props.put("password", pass);
        props.put("auth-method", "password");

        // Create the ssh client
        client = new SSHConsoleClient(getSrvHost(), getSrvPort(), this, this);

        // Ask the client to start a shell on the server
        client.shell();
    }

    /**
     * Start a shell over SSH1 connection and run a command and
     * retrieve the output
     *
     * @param command Command to execute
     */
    public void runCmd(String command) throws IOException {
        // Send command to stdin of shell
        OutputStream os = client.getStdIn();
        os.write((command + "\n" + END_MARKER + "\n").getBytes());
        os.flush();

        // Show output
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        BufferedReader in =
            new BufferedReader(new InputStreamReader(client.getStdOut()));
        for (;;) {
            String line = in.readLine();
            if (line == null)
                break;
            if (line.indexOf(END_MARKER) != -1) break;
            System.out.println(line);
        }
        System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
    }

    /**
     * Close connection
     */
    public void close() {
        client.close();
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
     * Get desirect encryption algorithm
     */
    public int getCipher(SSHClientUser origin) {
        return SSH.CIPHER_3DES;
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
        if (argv == null || argv.length != 5) {
            System.out.println("Usage: SSH1Cisco <host> <port> <user> <pass> <cmd>");
            return;
        }

        try {
            SSH1Cisco ssh1 =
                new SSH1Cisco(argv[0], argv[1], argv[2], argv[3]);
            ssh1.runCmd(argv[4]);
            ssh1.close();

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
