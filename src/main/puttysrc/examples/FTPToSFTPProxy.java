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
import java.io.InputStream;
import java.io.OutputStream;

import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetAddress;

import com.mindbright.nio.NetworkConnection;

import com.mindbright.ssh2.SSH2Transport;
import com.mindbright.ssh2.SSH2SimpleClient;
import com.mindbright.ssh2.SSH2FTPOverSFTP;
import com.mindbright.ssh2.SSH2Preferences;

import com.mindbright.util.Crypto;
import com.mindbright.util.Util;

/**
 * This is a demo of one usage of the FTP over SFTP bridging capability. It
 * starts a local server which acts like an ftp server except that the ftp
 * username is used to address different ssh2 servers (with sftp enabled) to
 * connect to.  The format of the ftp username is:
 * <code><em>ssh2_user</em>@<em>ssh2_host</em>[:<em>port</em>]</code>
 * <p>
 * Usage:
 * <code> java -cp examples.jar examples.FTPToSFTPProxy
 * [<em>listen_address</em>[:<em>port</em>]]
 */
public class FTPToSFTPProxy extends SSH2FTPOverSFTP {

    private final static String IDENTITY =
        "FTPToSFTPProxy use 'ssh2-user@ssh2-host[:port]' as your username.";

    private NetworkConnection  socket;
    private SSH2Transport      transport;
    private SSH2SimpleClient   client;
    /**
     * Construct an FTP to SFTP proxy which uses the given streams to
     * communicate with an FTP client.
     *
     * @param ftpInput input stream from ftp client
     * @param ftpOutput output stream to ftp client
     */
    public FTPToSFTPProxy(InputStream ftpInput, OutputStream ftpOutput) {
        super(ftpInput, ftpOutput, IDENTITY, true);
    }

    /**
     * Connect and log in to the SSH server
     *
     * @param user username to log in as
     * @param pass password to authenticate with
     */
    public boolean login(String user, String pass) {
        int i = user.indexOf('@');
        if(i == -1) {
            return false;
        }
        String ssh2Server = user.substring(i + 1);
        String ssh2User   = user.substring(0, i);
        int    port;

        port = Util.getPort(ssh2Server, 22);
        ssh2Server = Util.getHost(ssh2Server);
        try {
            /*
             * TODO: Should include code to read host key fingerprints from a
             * file and set a SSH2HostKeyVerifier here to avoid MITM attack.
             */
            SSH2Preferences prefs = new SSH2Preferences();
            prefs.setPreference(SSH2Preferences.LOG_LEVEL, "4");
            socket = NetworkConnection.open(ssh2Server, port);
            transport  = new SSH2Transport(socket, prefs,
										   Crypto.getSecureRandomAndPad());
            client     = new SSH2SimpleClient(transport, ssh2User, pass);
            initSFTP(client.getConnection());
            return super.login(user, pass);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Close connection
     */
    public void quit() {
        if(connection != null) {
            super.quit();
        }
        sftp.terminate();
    }

    /**
     * Run the application
     */
    public static void main(String[] argv) {
        if(argv.length > 1) {
            System.out.println("usage: FTPToSFTPProxy [<listen-address>[:<port>]]");
            System.exit(1);
        }
        String server = "127.0.0.1";
        int    port   = 21;
        try {
            if(argv.length > 0) {
                server = argv[0];
                int i = server.indexOf(':');
                if(i != -1) {
                    String p = server.substring(i + 1);
                    server = server.substring(0, i);
                    port   = Integer.parseInt(p);
                }
            }
            System.out.println("** FTPToSFTPProxy listening on " + server + ":"
                               + port);
            ServerSocket listen =
                new ServerSocket(port, 32, InetAddress.getByName(server));
            while(true) {
                Socket conn = listen.accept();
                new FTPToSFTPProxy(conn.getInputStream(),
				                   conn.getOutputStream());
            }
        } catch (Exception e) {
            System.out.println("An error occured: " + e.getMessage());
        }
    }

}
