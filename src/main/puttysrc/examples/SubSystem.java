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

import com.mindbright.nio.NetworkConnection;
import com.mindbright.util.Crypto;
import com.mindbright.util.Util;

import com.mindbright.ssh2.SSH2Transport;
import com.mindbright.ssh2.SSH2SessionChannel;
import com.mindbright.ssh2.SSH2SimpleClient;

/**
 * Login and start a subsystem. Needs to be expanded to actually handle
 * the communication with the subsystem. See place marked XXX in the code.
 * <p>
 * Usage:
 * <code> java -cp examples.jar examples.SubSystem
 * <em>server</em>[:<em>port</em>] <em>username</em> <em>password</em>
 * <em>subsystem</em>
 *
 */
public class SubSystem {

    /**
     * Run the application
     */
    public static void main(String[] argv) {
        if(argv.length < 4) {
            System.out.println("Usage: SubSystem <server:port> <username> <password> <subsystem>");
            System.exit(1);
        }

        try {
            String server = argv[0];
            String user   = argv[1];
            String passwd = argv[2];
            int    port   = 22;
            String subsystem = argv[3];

            port = Util.getPort(server, port);
            server = Util.getHost(server);

            String cmdLine = "";
            for(int i = 3; i < argv.length; i++) {
                cmdLine += argv[i] + " ";
            }
            cmdLine = cmdLine.trim();

            /*
             * Connect to the server and authenticate using plain password
             * authentication (if other authentication method needed check
             * other constructors for SSH2SimpleClient).
             */
            NetworkConnection socket= NetworkConnection.open(server, port);
            SSH2Transport transport = new SSH2Transport(socket,
                                      Crypto.getSecureRandomAndPad());
            SSH2SimpleClient client = new SSH2SimpleClient(transport,
                                      user, passwd);


            SSH2SessionChannel session = client.getConnection().newSession();
            if (!session.doSubsystem(subsystem)) {
                throw new Exception("failed to start subsystem '" + subsystem + "'");
            }

            session.getStdIn();
            session.getStdOut();

            /* XXX: Do something with the subsystem */


            /*
             * Disconnect the transport layer gracefully
             */
            transport.normalDisconnect("User disconnects");
        } catch (Exception e) {
            System.err.println("An error occured: " + e);
            e.printStackTrace();
            System.exit(1);
        }
    }
}
