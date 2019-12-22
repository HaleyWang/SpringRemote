/******************************************************************************
 *
 * Copyright (c) 1999-2011 Cryptzone AB. All Rights Reserved.
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

import java.awt.BorderLayout;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.IOException;

import java.net.Socket;

import javax.swing.JFrame;

import com.mindbright.net.telnet.TelnetTerminalAdapter;

import com.mindbright.terminal.GlobalClipboard;
import com.mindbright.terminal.LineReaderTerminal;
import com.mindbright.terminal.TerminalFrameTitle;
import com.mindbright.terminal.TerminalWin;

import com.mindbright.util.Util;

/**
 * Open a telnet window and connect with the telnet protocol. SSH is
 * not involved att all.
 * <p>
 * Usage:
 * <code> java -cp examples.jar examples.Telnet
 * [<em>server</em>[:<em>port</em>]]
 */
public class Telnet extends WindowAdapter {

    private Socket                socket;
    private String                remoteHost;
    private int                   remotePort;
    private LineReaderTerminal    lineReader;
    private TelnetTerminalAdapter telnetAdapter;

    /**
     * Constructor which will cause the window to prompt the user for
     * the server to connect to.
     */
    public Telnet() {
        this(null, -1);
    }

    /**
     * Constructor which will cause a connection to the default port (23)
     * on the given host.
     */
    public Telnet(String remoteHost) {
        setFromHostString(remoteHost);
    }

    /**
     * Constructor which will cause a connection to a specified port
     * on the given host.
     */
    public Telnet(String remoteHost, int remotePort) {
        this.remoteHost = remoteHost;
        this.remotePort = remotePort;
    }

    /**
     * Create the terminal window and connect to the remote host.
     */
    public void startMeUp() {
        JFrame      frame    = new JFrame();
        TerminalWin terminal = new TerminalWin(frame);

        /*
         * Create terminal window
         */
        terminal.setClipboard(GlobalClipboard.getClipboardHandler());

        frame.setLayout(new BorderLayout());
        frame.add(terminal.getPanelWithScrollbar(), BorderLayout.CENTER);

        TerminalFrameTitle frameTitle =
            new TerminalFrameTitle(frame, "Telnet (not connected)");
        frameTitle.attach(terminal);

        frame.addWindowListener(this);

        frame.pack();
        frame.setVisible(true);

        try {
            /*
             * Prompt for remote host if not specified
             */
            if(remoteHost == null) {
                lineReader = new LineReaderTerminal(terminal);
                String host = null;
                do {
                    host = lineReader.promptLine("\r\nremote host[:port] : ",
                                                 null, false);
                } while(host == null || host.trim().length() == 0);
                setFromHostString(host);
                lineReader.detach();
            }

            // Connect to remote host
            socket = new Socket(remoteHost, remotePort);

            // Attach the terminal emulator to the socket
            telnetAdapter = new TelnetTerminalAdapter(socket.getInputStream(),
                                                      socket.getOutputStream(),
                                                      terminal);

            // Update window title to reflect where we are connected
            frameTitle.setTitleName(
                "telnet@" + remoteHost
                + (remotePort != 23 ? (":" + remotePort) : ""));

            // Wait for connection to close
            telnetAdapter.getTelnetNVT().getThread().join();

            /*
             * Check that the terminal is still in local echo mode
             * If so, print a message that we have disconnected
             */
            if(telnetAdapter.isBuffered()) {
                lineReader = new LineReaderTerminal(terminal);
                lineReader.promptLine("\n\rTelnet session was closed, press <return> to close window", null,
                                      false);
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        } finally {
            // Close the window
            frame.dispose();
        }
    }

    /**
     * Parse remote host name. This sets the remote host to connect
     * to. It also looks if a port-number has been specified via the
     * :<em>portnr</em> mechanism.
     */
    private void setFromHostString(String remoteHost) {
        if(remoteHost == null) {
            return;
        }
        this.remotePort = Util.getPort(remoteHost, 23);
        this.remoteHost = Util.getHost(remoteHost);
    }

    /**
     * Run the application
     */
    public static void main(String[] argv) {
        if(argv.length > 1) {
            System.out.println("usage: Telnet <server[:port]>");
            System.exit(1);
        }
        Telnet telnet = new Telnet(argv.length > 0 ? argv[0] : null);
        telnet.startMeUp();
        System.exit(0);
    }

    /**
     * Handles window close events by closing the socket to the server
     * (if any).
     */
    public void windowClosing(WindowEvent e) {
        if(socket != null) {
            try {
                socket.close();
            } catch (IOException ee) { /* don't care */
            }
        }
        if(lineReader != null) {
            lineReader.breakPromptLine("");
        }
    }

}
