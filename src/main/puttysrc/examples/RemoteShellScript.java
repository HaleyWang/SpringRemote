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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

import com.mindbright.nio.NetworkConnection;
import com.mindbright.util.Crypto;
import com.mindbright.util.ExpectOutputStream;
import com.mindbright.util.Util;

import com.mindbright.ssh2.*;

/**
 * This is a simple demo of running a list of command-lines given as text
 * file. The commands are run sequentially and the stdout output from each
 * command is printed to the local stdout (stderr is also redirected to the
 * local stderr).
 * <p>
 * Usage:
 * <code> java -cp examples.jar examples.RemoteShellScript
 * <em>server</em>[:<em>port</em>] <em>username</em> <em>password</em>
 * <em>script_file</em>
 *
 * @see ExpectOutputStream
 */
public class RemoteShellScript implements ExpectOutputStream.Expector {
    private OutputStream       stdin;
    private ExpectOutputStream stdout;
    private String             result;

    private static String EOL = "\n";

    /**
     * Constructor which takes the remote console where the script
     * should be executed as argument.
     *
     * @param console connection to server
     */
    public RemoteShellScript(SSH2ConsoleRemote console) {
        this.stdin   = console.getStdIn();
        this.stdout  = new ExpectOutputStream(this, "___END_CMD_MARKER___");
        console.changeStdOut(stdout);
    }

    /**
     * Launch a single command on the server
     *
     * @param cmd command line to execute
     *
     * @return the output from the given command
     */
    public String run(String cmd) {
        try {
            // Send the command to the server
            stdin.write((cmd + EOL).getBytes());

            /*
             * Send end-marker (which the ExpectOutputStream is
             * waiting for). The trick with the quotes is so that the
             * ExpectOutputStream does not trigger when the echo
             * command is echoed but just when it is executed.
             */
            stdin.write(("echo \"___END\"_CMD_MARKER___" + EOL).getBytes());
            synchronized(this) {
                // Wait until the end marker has been seen
                wait();
            }
            // Result was filled in by the reached function
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Log off from server
     */
    public void exit() {
        try {
            stdin.write(("exit" + EOL).getBytes());
        } catch (Exception e) {}
    }

    /**
     * Run the application
     */
    public static void main(String[] argv) {
        if(argv.length < 4) {
            System.out.println("usage: RemoteShellScript <server[:port]> <username> <password> <script-file>");
            System.exit(1);
        }

        try {
            String server = argv[0];
            String user   = argv[1];
            String passwd = argv[2];
            String file   = argv[3];
            int    port;

            port = Util.getPort(server, 22);
            server = Util.getHost(server);

            /*
             * Connect to the server and authenticate using plain password
             * authentication (if other authentication method needed
             * check other constructors for SSH2SimpleClient).
             */
            SSH2Preferences prefs = new SSH2Preferences();


            /*
             * How to set some preferences, i.e. force usage of
             * blowfish cipher and set loglevel to 6 (debug) and log
             * output to the file ssh2out.log
             *
             * prefs.setPreference(SSH2Preferences.CIPHERS_C2S, "blowfish-cbc");
             * prefs.setPreference(SSH2Preferences.CIPHERS_S2C, "blowfish-cbc");
             * prefs.setPreference(SSH2Preferences.LOG_LEVEL, "9");
             * prefs.setPreference(SSH2Preferences.LOG_FILE, "ssh2out.log");
             */

            NetworkConnection socket= NetworkConnection.open(server, port);
            SSH2Transport transport = new SSH2Transport(socket, prefs,
														Crypto.getSecureRandomAndPad());
            SSH2SimpleClient client = new SSH2SimpleClient(transport,
                                      user, passwd);

            /*
             * Create the remote console to use for shell execution. Here we
             * redirect stderr of all sessions started with this console to our
             * own stderr (NOTE: stdout is NOT redirected here but is instead
             * changed in the RemoteShellScript constructor).
             */
            SSH2ConsoleRemote console =
                new SSH2ConsoleRemote(client.getConnection(), null,System.err);

            /*
             * Start a shell on server (note: we don't want a PTY here)
             */
            if(!console.shell(true, "dumb", 20, 80)) {
                throw new Exception("Couldn't execute shell on server!");
            }

            /*
             * Prepare for our home-brew shell interaction.
             */
            RemoteShellScript shell = new RemoteShellScript(console);
            BufferedReader script = new BufferedReader(
                new InputStreamReader(new FileInputStream(file)));

            /*
             * Run each line from the given file in the remote shell and print
             * the output to stdout which is returned by the run() method here.
             */
            String line;
            while((line = script.readLine()) != null) {
                System.out.print(shell.run(line));
            }
            script.close();

            /*
             * Exit the shell (if not already done in the script-file).
             */
            shell.exit();

            /*
             * Retrieve the exit status of the shell (from the remote end).
             */
            int exitStatus = console.waitForExitStatus();

            /*
             * Disconnect the transport layer gracefully
             */
            client.getTransport().normalDisconnect("User disconnects");

            /*
             * Exit with same status as remote shell did
             */
            System.exit(exitStatus);

        } catch (Exception e) {
            System.out.println("An error occured: " + e);
            System.exit(1);
        }
    }

    /*
     * ExpectOutputStream.Expector interface implementation
     */
    public synchronized void reached(ExpectOutputStream out,
                                     byte[] buf, int len) {
        result = new String(buf, 0, len);
        synchronized(this) {
            notify();
        }
    }

    public void closed(ExpectOutputStream out, byte[] buf, int len) {
        result = null;
        synchronized(this) {
            notify();
        }
    }

}
