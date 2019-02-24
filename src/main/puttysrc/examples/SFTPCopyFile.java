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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import com.mindbright.nio.NetworkConnection;
import com.mindbright.util.Crypto;
import com.mindbright.util.Util;

import com.mindbright.ssh2.SSH2Preferences;
import com.mindbright.ssh2.SSH2Transport;
import com.mindbright.ssh2.SSH2SimpleClient;
import com.mindbright.ssh2.SSH2SFTP;
import com.mindbright.ssh2.SSH2SFTP.FileHandle;
import com.mindbright.ssh2.SSH2SFTPClient;

/**
 * Copy a file to/from an SSH2 server using the sftp protocol. The sftp
 * protocol was introduced with ssh version 2.
 * <p>
 * Usage:
 * <code> java -cp examples.jar examples.SFTPCopyFile
 * <em>server</em>[:<em>port</em>] <em>username</em> <em>password</em>
 * to|from <em>src_file</em> <em>dst_file</em>
 *
 * @see com.mindbright.sshcommon.SSHSCP1
 * @see SCPExample
 * @see SSH2SFTPClient
 */
public class SFTPCopyFile {

    /**
     * Run the application
     */
    public static void main(String[] argv) {
        if(argv.length < 6) {
            System.out.println("Usage: SFTPCopyFile <server:port> <username> <password> to|from <src_file> <dst_file>");
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

            SSH2Preferences prefs = new SSH2Preferences();
            prefs.setPreference(SSH2Preferences.RX_INIT_WIN_SZ, "1048576");
            prefs.setPreference(SSH2Preferences.RX_MAX_PKT_SZ,  "32768");
            prefs.setPreference(SSH2Preferences.TX_MAX_PKT_SZ,  "32768");

            /*
             * Connect to the server and authenticate using plain password
             * authentication (if other authentication method needed check
             * other constructors for SSH2SimpleClient).
             */
            NetworkConnection socket= NetworkConnection.open(server, port);
            SSH2Transport transport = new SSH2Transport(socket, prefs,
							Crypto.getSecureRandomAndPad());
            SSH2SimpleClient client = new SSH2SimpleClient(transport,
                                      user, passwd);

            /*
             * Create SFTP client which is used for the file transfer.
             * This instance can be used multiple times to transfer
             * many files although this exampel only uses it once.
             * We use asynchronous mode for the SSH2SFTPClient beacuse
             * it is much faster.
             */
            SSH2SFTPClient sftpClient =
                new SSH2SFTPClient(client.getConnection(), false, 16*1024);

            /*
             * Extract more command line arguments
             */
            String direction = argv[3];
            String srcFile = argv[4];
            String dstFile = argv[5];

            /*
             * Convert the given paths to use '/' as separators
             */
            srcFile = srcFile.replace(File.separatorChar, '/');
            dstFile = dstFile.replace(File.separatorChar, '/');

            /*
             * Copy the given file
             * The code here only copies one file but any number of
             * operations can be peformed on the SSH2SFTClient
             */
            if (direction.equals("to")) {
                copyTo(sftpClient, srcFile, dstFile);
            } else if (direction.equals("from")) {
                copyFrom(sftpClient, srcFile, dstFile);
            }

            /* Close down SFTP client */
            sftpClient.terminate();

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

    /**
     * Copy a file to the remote system.
     *
     * @param sftpClient connection to copy over
     * @param srcFile local file to copy from
     * @param dstFile remote file to copy to
     */
    private static void copyTo(SSH2SFTPClient sftpClient, String srcFile,
                               String dstFile) throws Exception {
        FileHandle handle = sftpClient.open(dstFile,
                                            SSH2SFTP.SSH_FXF_WRITE |
                                            SSH2SFTP.SSH_FXF_CREAT |
                                            SSH2SFTP.SSH_FXF_TRUNC,
                                            new SSH2SFTP.FileAttributes());
        File f = new File(srcFile);
        FileInputStream fin = new FileInputStream(f);
        sftpClient.writeFully(handle, fin);
        fin.close();
    }

    /**
     * Copy a file from the remote system.
     *
     * @param sftpClient connection to copy over
     * @param srcFile remote file to copy from
     * @param dstFile local file to copy to
     */
    private static void copyFrom(SSH2SFTPClient sftpClient, String srcFile,
                                 String dstFile) throws Exception {
        FileHandle handle = sftpClient.open(srcFile,
                                            SSH2SFTP.SSH_FXF_READ,
                                            new SSH2SFTP.FileAttributes());
        File f = new File(dstFile);
        FileOutputStream fout = new FileOutputStream(f);
        sftpClient.readFully(handle, fout);
        fout.close();
    }
}
