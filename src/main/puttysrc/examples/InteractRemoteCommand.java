/******************************************************************************
 *
 * Copyright (c) 2005-2011 Cryptzone Group AB. All Rights Reserved.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintStream;

import com.mindbright.nio.NetworkConnection;
import com.mindbright.util.Crypto;
import com.mindbright.util.Util;

import com.mindbright.ssh2.SSH2Transport;
import com.mindbright.ssh2.SSH2SimpleClient;
import com.mindbright.ssh2.SSH2ConsoleRemote;

/**
 * This is a simple demo of interacting with a single command on a ssh2 server.
 * <p>
 * Usage:
 * <code> java -cp examples.jar examples.InteractRemoteCommand
 * <em>server</em>[:<em>port</em>] <em>username</em> <em>password</em>
 * <em>command_line</em>
 *
 * @see RunRemoteCommand
 * @see RunRemoteCommand2
 */
public class InteractRemoteCommand {

    /**
     * Run the application
     */
    public static void main(String[] argv) {
        if(argv.length < 4) {
            System.out.println("usage: InteractRemoteCommand <server[:port]> <username> <password> <command-line>");
            System.exit(1);
        }
        try {
            /*
             * Parse command line arguments
             */
            String server = argv[0];
            String user   = argv[1];
            String passwd = argv[2];
            int    port;

            port = Util.getPort(server, 22);
            server = Util.getHost(server);

            String cmdLine = "";
            for(int i = 3; i < argv.length; i++) {
                cmdLine += argv[i] + " ";
            }
            cmdLine = cmdLine.trim();

            /*
             * Connect to the server and authenticate using plain password
             * authentication (if other authentication method needed
             * check other constructors for SSH2SimpleClient).
             */
            NetworkConnection socket= NetworkConnection.open(server, port);
            SSH2Transport transport = new SSH2Transport(socket,
														Crypto.getSecureRandomAndPad());
            SSH2SimpleClient client = new SSH2SimpleClient(transport,
                                      user, passwd);

            /*
             * Create the remote console to use for command execution.
             */
            SSH2ConsoleRemote console =
                new SSH2ConsoleRemote(client.getConnection());

            /*
             * Run the command (returns a boolean indicating success, we ignore
             * it here).
             * The final boolean value indicates if the remote end
             * should create a real pty or not. A pty is sometimes
             * needed to catch password queries etc.
             */
            console.command(cmdLine, true);

            /*
             * Get the streams we will use to interact with the command
             */
            PrintStream out = new PrintStream(console.getStdIn());
            BufferedReader in = new BufferedReader(
                new InputStreamReader(console.getStdOut()));

            /*
             * Wait for output.
             * This is just sample code so it is hardcoded to wait for
             * a password prompt.
             *
             * It is also possible to use
             * com.mindbright.util.ExpectOutputStream to achieve
             * this. See examples.RemoteShellScript for an example.
             */
            String data;
            char c;

            do {
                data = new String("");
                while (-1 != (c = (char)in.read()) && c != ':') {
                    data += c;
                }
            } while (c != -1 && !data.endsWith("ssword"));

            /*
             * Send the output. In this case the password.
             */
            out.println("PASSWORD_GOES_HERE");

            /*
             * Send the rest of the output to our stdout
             */
            console.changeStdOut(System.out);

            /*
             * Retrieve the exit status of the command (from the remote end).
             */
            int exitStatus = console.waitForExitStatus();

            /*
             * NOTE: at this point System.out will be closed together with the
             * session channel of the console
             */

            /*
             * Disconnect the transport layer gracefully
             */
            transport.normalDisconnect("User disconnects");

            /*
             * Exit with same status as remote command did
             */
            System.exit(exitStatus);

        } catch (Exception e) {
            System.out.println("An error occured: " + e);
            System.exit(1);
        }
    }
}
