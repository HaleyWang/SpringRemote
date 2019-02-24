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

import com.mindbright.nio.NetworkConnection;
import com.mindbright.nio.NonBlockingOutput;
import com.mindbright.util.Crypto;
import com.mindbright.util.Util;

import com.mindbright.ssh2.SSH2Transport;
import com.mindbright.ssh2.SSH2SimpleClient;
import com.mindbright.ssh2.SSH2ConsoleRemote;

/**
 * This is a simple demo of running a single command on a ssh2 server
 * in exactly the same way an ordinary unix ssh2 client works when
 * passed a command-line to be run.
 * <p>
 * Usage:
 * <code> java -cp examples.jar examples.RunRemoteCommand
 * <em>server</em>[:<em>port</em>] <em>username</em> <em>password</em>
 * <em>command_line</em>
 *
 * @see RemoteShellScript
 * @see RunRemoteCommand2
 */
public class RunRemoteCommand {

    /**
     * Run the application
     */
    public static void main(String[] argv) {
        if(argv.length < 4) {
            System.out.println("usage: RunRemoteCommand <server[:port]> <username> <password> <command-line>");
            System.exit(1);
        }

        try {
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
             * authentication (if other authentication methods are needed
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

            int exitStatus = -1;

            /*
             * Run the command. Here we redirect stdout and stderr of the 
             * remote command execution to a file named "out" for simplicity.
             */
            NonBlockingOutput out = new NonBlockingOutput("out");
            if (console.command(cmdLine, null, out, out)) {
                exitStatus = console.waitForExitStatus();
            } else {
                System.err.println("failed to execute command: " + cmdLine);
            }

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
            System.err.println("An error occured: " + e);
            e.printStackTrace();
            System.exit(1);
        }
    }
}
