package com.mindbright.application;///******************************************************************************
// *
// * Copyright (c) 1999-2011 Cryptzone AB. All Rights Reserved.
// * 
// * This file contains Original Code and/or Modifications of Original Code as
// * defined in and that are subject to the MindTerm Public Source License,
// * Version 2.0, (the 'License'). You may not use this file except in compliance
// * with the License.
// * 
// * You should have received a copy of the MindTerm Public Source License
// * along with this software; see the file LICENSE.  If not, write to
// * Cryptzone Group AB, Drakegatan 7, SE-41250 Goteborg, SWEDEN
// *
// *****************************************************************************/
//
//package com.mindbright.application;
//
//import java.awt.BorderLayout;
//
//import java.awt.event.WindowEvent;
//import java.awt.event.WindowListener;
//
//import java.io.IOException;
//
//import java.net.ConnectException;
//import java.net.Socket;
//
//import javax.swing.JFrame;
//import javax.swing.JMenuBar;
//import javax.swing.SwingUtilities;
//
//import com.mindbright.net.telnet.TelnetTerminalAdapter;
//
//import com.mindbright.nio.NetworkConnection;
//
//import com.mindbright.ssh.SSHClient;
//import com.mindbright.ssh.SSHInteractor;
//import com.mindbright.ssh.SSHPropertyHandler;
//
//import com.mindbright.terminal.GlobalClipboard;
//import com.mindbright.terminal.LineReaderTerminal;
//import com.mindbright.terminal.TerminalFrameTitle;
//import com.mindbright.terminal.TerminalMenuHandler;
//import com.mindbright.terminal.TerminalMenuHandlerFull;
//import com.mindbright.terminal.TerminalMenuListener;
//import com.mindbright.terminal.TerminalWin;
//
//import com.mindbright.util.Util;
//
///**
// * Open a telnet window and connect with the telnet protocol. SSH is
// * not involved at all.
// */
//public class MindTermTelnet extends MindTerm
//    implements Runnable, WindowListener, TerminalMenuListener,
//    SSHInteractor {
//	private static final long serialVersionUID = 1L;
//
//    private Socket                socket;
//    private String                remoteHost;
//    private int                   remotePort;
//    private LineReaderTerminal    lineReader;
//    private TerminalFrameTitle    frameTitle;
//    private String                server = null;
//    private String                exitMessage = null;
//
//    /**
//     * Create the terminal window and connect to the remote host.
//     */
//    public void telnetConnect() {
//        try {
//            if(server != null){
//                setFromHostString(server);
//            }
//
//            /*
//             * Prompt for remote host if not specified
//             */
//            if(remoteHost == null) {
//                lineReader = new LineReaderTerminal(term);
//                String host = null;
//                do {
//                    host = lineReader.promptLine("\r\nremote host[:port] : ",
//                                                 null, false);
//                } while(host == null || host.trim().length() == 0);
//                setFromHostString(host);
//                lineReader.detach();
//            }
//
//            // Connect to remote host
//            SSHPropertyHandler ph = new SSHPropertyHandler(sshProps, true);
//            ph.setInteractor(this);
//            NetworkConnection nc = ph.getProxyConnection();
//            if (nc != null) {
//                socket = nc.getSocket();
//            }
//            if (socket == null) {
//                socket = new Socket(remoteHost, remotePort);
//            }
//
//            // Attach the terminal emulator to the socket
//            TelnetTerminalAdapter telnetAdapter =
//                new TelnetTerminalAdapter(socket.getInputStream(),
//                                          socket.getOutputStream(), term);
//
//            // Update window title to reflect where we are connected
//            frameTitle.setTitleName(
//                "telnet@" + remoteHost
//                + (remotePort != 23 ? (":" + remotePort) : ""));
//
//            // Wait for connection to close
//            telnetAdapter.getTelnetNVT().getThread().join();
//
//            /*
//             * Check that the terminal is still in local echo mode
//             * If so, print a message that we have disconnected
//             */
//            if (!telnetAdapter.isBuffered()) {
//                term.resetToDefaults();
//            }
//            lineReader = new LineReaderTerminal(term);
//            String msg = exitMessage;
//            if (msg == null || msg.length() == 0) {
//                msg = "Telnet session was closed, " +
//                    "press <return> to exit MindTerm";
//            }
//            lineReader.promptLine("\n\r" + msg, null, false);
//
//        } catch (Exception e) {
//            lineReader = new LineReaderTerminal(term);
//            String msg;
//            System.out.println("Error connecting: " + e.getMessage());
//            if (e instanceof ConnectException) {
//                msg = "Remote server refused connection, " +
//                    "press <return> to exit MindTerm";
//            } else {
//                e.printStackTrace();
//                msg = "Unknown error see console output for details, " +
//                    "press <return> to exit MindTerm";
//            }
//            try {
//                lineReader.promptLine("\n\r" + msg, null, false);
//            } catch (Exception ee) {}
//
//        } finally {
//            if (frame != null) {
//                frame.dispose();
//            }
//        }
//        lineReader.detach();
//    }
//
//    /**
//     * Parse remote host name. This sets the remote host to connect
//     * to. It also looks if a port-number has been specified via the
//     * :<em>portnr</em> mechanism.
//     */
//    private void setFromHostString(String remoteHost) {
//        if(remoteHost == null) {
//            return;
//        }
//        this.remotePort = Util.getPort(remoteHost, 23);
//        this.remoteHost = Util.getHost(remoteHost);
//        sshProps.setProperty("server", this.remoteHost);
//        sshProps.setProperty("port",   Integer.toString(this.remotePort));
//    }
//
//    /**
//     * Create the GUI
//     */
//    public void initGUI() {
//        if (separateFrame) {
//            frame = new JFrame();
//            frame.addWindowListener(this);
//            container = frame;
//        } else {
//            container = getContentPane();
//        }
//
//        term = new TerminalWin(container, termProps, (sshClone == null));
//        if(mergedTermProps)
//            term.setPropsChanged(true);
//
//        if(haveMenus) {
//            TerminalMenuHandler tmenus;
//            try {
//                JMenuBar menuBar = new JMenuBar();
//                if (separateFrame) {
//                    frame.setJMenuBar(menuBar);
//                } else {
//                    setJMenuBar(menuBar);
//                }
//                tmenus = new TerminalMenuHandlerFull();
//                tmenus.addBasicMenus(term, menuBar);
//                tmenus.setTerminalMenuListener(this);
//                term.setClipboard(GlobalClipboard.getClipboardHandler(tmenus));
//            } catch (Throwable t) {
//                t.printStackTrace();
//                System.out.println("Full menus can't be enabled since classes "
//                                   + "are missing");
//                term.setMenus(null);
//                term.setClipboard(GlobalClipboard.getClipboardHandler());
//            }
//        } else {
//            term.setClipboard(GlobalClipboard.getClipboardHandler());
//        }
//
//        /*
//         * Create terminal window
//         */
//        frameTitle = new TerminalFrameTitle(frame, "Telnet (not connected)");
//        frameTitle.attach(term);
//
//        if (separateFrame) {
//            term.setIgnoreClose();
//            container.setLayout(new BorderLayout());
//            container.add(term.getPanelWithScrollbar(), BorderLayout.CENTER);
//            frame.pack();
//            frame.setVisible(true);
//        } else {
//            container.add(term.getPanelWithScrollbar());
//        }
//        SwingUtilities.invokeLater(new Runnable() {
//            public void run() {
//                term.getDisplay().repaint(true);
//            }
//        });
//    }
//
//    public void getAppletParams() {
//        super.getAppletParams();
//        server = getParameter("server");
//        exitMessage = getParameter("exitMessage");
//    }
//
//    /**
//     * Close the terminal window
//     */
//    private void doClose() {
//        if(socket != null) {
//            try {
//                socket.close();
//                socket = null;
//            } catch (IOException ee) { /* don't care */
//            }
//        }
//        if(lineReader != null) {
//            lineReader.breakPromptLine("");
//        }
//    }
//
//    /*
//     * Applet interface
//     */
//    public void init() {
//        weAreAnApplet  = true;
//        autoSaveProps  = false;
//        autoLoadProps  = false;
//        savePasswords  = false;
//
//        getAppletParams();
//        (new Thread(this, "MindTerm.init")).start();
//    }
//
//    public void run() {
//        initGUI();
//        telnetConnect();
//    }
//
//    public void destroy() {
//        doClose();
//    }
//
//    public static void main(String[] argv) {
//        MindTermTelnet mtt = new MindTermTelnet();
//        mtt.cmdLineArgs = argv;
//
//        try {
//            mtt.getApplicationParams();
//            mtt.server = mtt.sshProps.getProperty("server");
//        } catch (Exception e) {
//            System.out.println("Error parsing parameters: " + e.getMessage());
//            System.exit(1);
//        }
//
//        mtt.run();
//    }
//
//    /*
//     * WindowListener interface
//     */
//
//    /**
//     * Handles window close events by closing the socket to the server
//     * (if any).
//     */
//    public void windowClosing(WindowEvent e)     {doClose();}
//    public void windowDeiconified(WindowEvent e) {}
//    public void windowOpened(WindowEvent e)      {}
//    public void windowClosed(WindowEvent e)      {}
//    public void windowIconified(WindowEvent e)   {}
//    public void windowActivated(WindowEvent e)   {}
//    public void windowDeactivated(WindowEvent e) {}
//
//
//    /*
//     * TerminalMenuListener interface
//     */
//    public void close(TerminalMenuHandler originMenu) {
//        doClose();
//    }
//
//    public void update() {}
//
//    /*
//     * SSHInteractor interface
//     */
//    public void startNewSession(SSHClient client) {}
//    public void sessionStarted(SSHClient client) {}
//
//    public void connected(SSHClient client) {}
//    public void open(SSHClient client) {}
//    public void disconnected(SSHClient client, boolean graceful) {}
//
//    public void report(String msg) {}
//    public void alert(String msg) {}
//
//    public void propsStateChanged(SSHPropertyHandler props) {}
//    public boolean askConfirmation(String message, boolean defAnswer) {
//        return defAnswer;
//    }
//    public boolean licenseDialog(String license) {
//        return false;
//    }
//
//    public boolean quietPrompts() {
//        return true;
//    }
//    public String promptLine(String prompt, String defaultVal)
//        throws IOException {
//        try {
//            lineReader = new LineReaderTerminal(term);
//            String reply = lineReader.promptLine(prompt, defaultVal, false);
//            lineReader.detach();
//            return reply;
//        } catch (Exception e) {
//            throw new IOException(e.getMessage());
//        }
//    }
//
//    public String promptPassword(String prompt) throws IOException {
//        try {
//            lineReader = new LineReaderTerminal(term);
//            String reply = lineReader.promptLine(prompt, null, true);
//            lineReader.detach();
//            return reply;
//        } catch (Exception e) {
//            throw new IOException(e.getMessage());
//        }
//    }
//
//    public boolean isVerbose() {
//        return false;
//    }
//}
