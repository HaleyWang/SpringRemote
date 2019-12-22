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
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.net.Socket;
import java.util.Random;

import com.mindbright.nio.NetworkConnection;

import com.mindbright.ssh2.*;

import com.mindbright.util.Crypto;
import com.mindbright.util.Util;

/**
 * Simple program to stress test communication over port
 * forwards. It creates a connection to a server (which should
 * have an ECHO service available), then starts <em>nforwards</em>
 * port forwards, and <em>nthreads</em> threads which in turn uses the
 * pool of port forwards to send (and retrieve) <em>iter</em> messages 
 * of random sizes to the ECHO service.
 * <p>
 * Usage: 
 *  <code>java -cp examples.jar examples.StressTest <em>server:port</em> <em>user</em> <em>pass</em> <em>nforwards</em> <em>nthreads</em> <em>iter</em></code>
 * <p>
 * For example try running something like:
 * <p>
 *  <code>java -cp examples.jar examples.StressTest <em>server:port</em> <em>user</em> <em>pass</em> 16 40 100</code>
 *
 */

public class StressTest {

    private static volatile int thrcount = 0;
    private static volatile long bcount = 0;

    public static void main(String[] argv) {

        if (argv.length < 6) {
            System.out.println("Usage: StressTest <server:port> <username> " + 
                               "<password> <nforwards> <nthreads> <iters>");
            System.exit(1);
        }

        try {
            String server = argv[0];
            String user   = argv[1];
            String passwd = argv[2];
            int    port;
            int nforwards = Integer.parseInt(argv[3]);
            int nthreads  = Integer.parseInt(argv[4]);
            final int iters = Integer.parseInt(argv[5]);
            int lports[] = new int[nforwards];

            port = Util.getPort(server, 22);
            server = Util.getHost(server);

            /*
             * Connect to the server and authenticate using plain password
             * authentication (if other authentication method needed check
             * other constructors for SSH2SimpleClient).
             */
            SSH2Preferences prefs = new SSH2Preferences();
            prefs.setPreference(SSH2Preferences.CIPHERS_C2S, "arcfour");
            prefs.setPreference(SSH2Preferences.CIPHERS_S2C, "arcfour");
            NetworkConnection socket = NetworkConnection.open(server, port);
            SSH2Transport transport = new SSH2Transport
                (socket, prefs, Crypto.getSecureRandomAndPad());
            SSH2SimpleClient client = new SSH2SimpleClient
                (transport, user, passwd);
            
            /* Start port forward */
            SSH2Connection conn = client.getConnection();
            for (int i=0; i<nforwards; i++) {
                SSH2Listener l = conn.newLocalForward("127.0.0.1", 0, "127.0.0.1", 7) ;
                lports[i] = l.getListenPort();
            }
            
            /*
             * Start threads that talks to remote end        
             */
            thrcount = nthreads;
            for (int i=0; i<nthreads; i++) {
                final int lp = lports[i%nforwards];
                Thread t = new Thread(new Runnable() {
                    public void run() {
                        talk(lp, iters);
                    }}, "T" + i);
                t.start();
            }

            try {
                for (;;) {
                    Thread.sleep(1000);
                    if (thrcount <= 0) break;
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }

            System.out.println("============================");
            System.out.println("Sum bytes="+bcount);
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
    
    private static void talk(int port, int iters) {
        try {
            int sum = 0;
            Random r = new Random();
            Socket sock;
            InputStream in;
            OutputStream out;
            byte [] b = new byte[256];
            byte [] c = new byte[256];
            for (int i=0; i<b.length; i++) b[i] = 'A';
            
            for (int i=0; i<iters; i++) {
                sock = new Socket("127.0.0.1", port);
                in = sock.getInputStream();
                out = sock.getOutputStream();
                int jj = 1 + r.nextInt(100);
                
                for (int j=0; j<jj; j++) {
                    int l = r.nextInt(40);
                    b[l] = '\n';
                    out.write(b, 0, l+1);
                        b[l] = 'A';
                    while (l >= 0) {
                        int rl = in.read(c, 0, l+1);
                        if (rl <= 0) throw new IOException("read short");
                        sum += rl;
                        l -= rl;
                    }
                }
                sock.close();
            }
            System.out.println(Thread.currentThread().getName() + "/" + 
                               port + ": bytes=" + sum);
            bcount += sum;
        } catch (Throwable t) {
            t.printStackTrace();
        }
        thrcount --;
    }
}
