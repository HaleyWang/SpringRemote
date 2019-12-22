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

import java.awt.event.*;

import java.io.FileInputStream;
import java.io.IOException;

import java.util.Properties;

import javax.swing.JFrame;
import javax.swing.JMenuBar;

import com.mindbright.nio.NetworkConnection;

import com.mindbright.ssh2.SSH2ConsoleRemote;
import com.mindbright.ssh2.SSH2FTPProxyFilter;
import com.mindbright.ssh2.SSH2HostKeyVerifier;
import com.mindbright.ssh2.SSH2Interactor;
import com.mindbright.ssh2.SSH2Preferences;
import com.mindbright.ssh2.SSH2SimpleClient;
import com.mindbright.ssh2.SSH2StreamFilterFactory;
import com.mindbright.ssh2.SSH2StreamSniffer;
import com.mindbright.ssh2.SSH2TerminalAdapterImpl;
import com.mindbright.ssh2.SSH2Transport;
import com.mindbright.ssh2.SSH2UserCancelException;

import com.mindbright.terminal.GlobalClipboard;
import com.mindbright.terminal.LineReaderTerminal;
import com.mindbright.terminal.TerminalFrameTitle;
import com.mindbright.terminal.TerminalMenuHandler;
import com.mindbright.terminal.TerminalMenuHandlerFull;
import com.mindbright.terminal.TerminalMenuListener;
import com.mindbright.terminal.TerminalWin;

import com.mindbright.util.Crypto;
import com.mindbright.util.Util;

/**
 * Ssh2 client which opens a terminal window and asks the user
 * where to connect to.
 * <p>
 * Usage:
 * <code> java -cp examples.jar examples.BasicClient
 * [<em>props_file_name</em>]</code>
 * <p>
 * Username and password as well as server can be stored in the
 * properties file.
 * <p>
 * It can also read portforwards from properties. Create properties
 * named 'localN' or 'remoteN' where N is an integer 0-31. The
 * contents of the properties is in the following format:
 * <pre>
 *   [/plugin/][<em>local_host</em>:]<em>local_port</em>:<em>remote_host</em>:<em>remote_port</em>
 * </pre>
 * This client understands the <code>ftp</code> and <code>sniff</code>
 * plugins.
 *
 * @see SSH2FTPProxyFilter
 * @see SSH2StreamSniffer
 */
public final class BasicClient extends WindowAdapter
    implements SSH2Interactor, TerminalMenuListener, Runnable {
    private JFrame             frame;
    private TerminalWin        terminal;
    private SSH2Transport      transport;
    private SSH2SimpleClient   client;
    private SSH2ConsoleRemote  console;
    private Properties         props;
    private LineReaderTerminal lineReader;
    private int                exitStatus;
    private String             host;
    private String             passwd;

    /**
     * Simple constructor where all required properties have good default
     * values so no properties have to be provided. However the
     * properties is the only way to change the encryption algorithms
     * etc in this client.
     *
     * @param props SSH2 protocol properties.
     */
    public BasicClient(Properties props) {
        this.props      = props;
        this.exitStatus = 1;
    }

    /**
     * Actually runs the client. This gets called from the
     * <code>main</code> function.
     */
    public void run() {
        try {
            int    port;
            String user;

            /*
             * Create and show terminal window
             */
            boolean haveMenus = Boolean.valueOf(
                props.getProperty("havemenus")).booleanValue();
	    
            frame = new JFrame();
            terminal = new TerminalWin(frame, props);

            frame.setLayout(new BorderLayout());
            frame.add(terminal.getPanelWithScrollbar(), BorderLayout.CENTER);

            TerminalFrameTitle frameTitle =
                new TerminalFrameTitle(frame, getTitle());
            frameTitle.attach(terminal);

            if(haveMenus) {
                JMenuBar mb = new JMenuBar();
                frame.setJMenuBar(mb);
                try {
                    TerminalMenuHandler tmenus = new TerminalMenuHandlerFull();
		    tmenus.setTitleName(getTitle());
		    tmenus.addBasicMenus(terminal, mb);
		    tmenus.setTerminalMenuListener(this);
                } catch (Throwable t) {
                }
            } else {
                terminal.setClipboard(GlobalClipboard.getClipboardHandler());
            }
            frame.addWindowListener(this);

            frame.pack();
            frame.setVisible(true);

            /*
             * Prompt for where to connect to
             */
            lineReader = new LineReaderTerminal(terminal);

            host = props.getProperty("server");
            port = getPort(props.getProperty("port"));
            user = props.getProperty("username");

            terminal.write("Basic SSH2 client demo\n\r");

            while (host == null) {
                host = lineReader.promptLine("\r\nssh2 server[:port]: ",
                                             null, false);
            }

            port = Util.getPort(host, port);
            host = Util.getHost(host);

            while (user == null) {
                user = lineReader.promptLine(host + " login: ", null, false);
            }

            // Create the preferences object
            SSH2Preferences prefs = new SSH2Preferences(props);

            // This shows how to force certain properties
            //prefs.setPreference(SSH2Preferences.CIPHERS_C2S, "blowfish-cbc");
            //prefs.setPreference(SSH2Preferences.CIPHERS_S2C, "blowfish-cbc");
            //prefs.setPreference(SSH2Preferences.LOG_LEVEL, "9");
            //prefs.setPreference(SSH2Preferences.LOG_FILE, "ssh2out.log");

            /*
             * Open the TCP connection to the server and create the
             * SSH2Transport object. No traffic will be sent yet.
             */
            transport = new SSH2Transport(NetworkConnection.open(host, port),
                                          prefs, Crypto.getSecureRandomAndPad());

            /*
             * Prepare for host fingerprint verification.
             * This implementation check fingerprints agains properties of
             * the form: fingerprint.HOSTNAME.PORT
             */
            String fingerprint = props.getProperty("fingerprint." +
                                                   host + "." + port);

            /*
             * If we found a fingerprint property for this host:port then
             * create a key verifier which checks that the fingerprint of the
             * actual host matches.
             */
            if(fingerprint != null) {
                transport.setEventHandler(new SSH2HostKeyVerifier(fingerprint));
            }

            client = null;

            /*
             * This simple client can only authenticate using either
             * publickey or passwords. Depending on which it uses
             * different constructors of SSH2SimpleClient.
             *
             * The actual password to use can be stored in the
             * properties. This has severe security implications
             * though.
             *
             * The construction of SSH2SimpleClient will start up the
             * session and cause the encrypted connection to be
             * establiushed and the user to be authenticated.
             */
            String auth = props.getProperty("auth-method");
            if("publickey".equals(auth)) {
                String keyFile   = props.getProperty("private-key");
                String keyPasswd = props.getProperty("passphrase");
                client = new SSH2SimpleClient(transport, user, keyFile,
                                              keyPasswd);
            } else {
                passwd = props.getProperty("password");
                while (passwd ==  null) {
                    passwd = lineReader.promptLine(
                        user + "@" + host + "'s password: ", null, true);
                }
                client = new SSH2SimpleClient(transport, user, passwd);
            }

            // This class will not interact with the user anymore.
            lineReader.detach();

            // Start any portforwards defined in the preferences.
            startForwards();

            /*
             * Create the remote console to use for command execution.
             */
            console = new SSH2ConsoleRemote(client.getConnection());

            SSH2TerminalAdapterImpl termAdapter =
                new SSH2TerminalAdapterImpl(terminal);

            if(!console.terminal(termAdapter)) {
                throw new Exception("Couldn't start terminal!");
            }

            // Wait for user to close remote shell
            exitStatus = console.waitForExitStatus();

        } catch (LineReaderTerminal.ExternalMessageException e) {
            // ignore
        } catch (Exception e) {
            System.out.println("An error occured: " + e.getMessage());
        } finally {
            if(frame != null) {
                frame.dispose();
            }
        }
    }

    /**
     * Get the exit status from the SSH2ConsoleRemote instance
     *
     * @return the exit status
     */
    public int getExitStatus() {
        return exitStatus;
    }

    /**
     * Starts any portforwards specified in the properties.
     */
    private void startForwards() {
        int i;
        for(i = 0; i < 32; i++) {
            String spec = props.getProperty("local" + i);
            if(spec == null)
                break;
            Object[] components = Util.parseForwardSpec(spec, "127.0.0.1");
            try {
                SSH2StreamFilterFactory filter = null;
                if("ftp".equals(components[0])) {
                    filter = new SSH2FTPProxyFilter((String)components[1],
                                                    host);
                } else if("sniff".equals(components[0])) {
                    filter = SSH2StreamSniffer.getFilterFactory();
                }
                client.getConnection().
                newLocalForward((String)components[1],
                                ((Integer)components[2]).intValue(),
                                (String)components[3],
                                ((Integer)components[4]).intValue(),
                                filter);
                terminal.write("started local forward: " + spec + "\n\r");
            } catch (IOException e) {
                terminal.write("failed local forward: " + spec +
                               e.getMessage() + "\n\r");
            }
        }
        for(i = 0; i < 32; i++) {
            String spec = props.getProperty("remote" + i);
            if(spec == null)
                break;
            Object[] components = Util.parseForwardSpec(spec, "127.0.0.1");
            client.getConnection().newRemoteForward((String)components[1],
                                                    ((Integer)components[2]).intValue(),
                                                    (String)components[3],
                                                    ((Integer)components[4]).intValue());
            terminal.write("started remote forward: " + spec + "\n\r");
        }
    }

    /**
     * Get the port number of the ssh server stored in the
     * string. Defaults to 22, the ssh standard port, if none is
     * specified.
     */
    private static int getPort(String port) {
        int p;
        try {
            p = Integer.parseInt(port);
        } catch (Exception e) {
            p = 22;
        }
        return p;
    }

    private String getTitle() {
        return "Basic SSH2 Client";
    }

    /**
     * Close the connection to the server (if any) in a controlled way.
     */
    public void doClose() {
        if(lineReader != null) {
            lineReader.breakPromptLine("");
        }
        if(console != null) {
            console.close();
        }
        if(transport != null) {
            transport.normalDisconnect("User disconnects");
        }
    }

    /**
     * Overide corresponding function in java.awt.event.WindowAdapter
     */
    public void windowClosing(WindowEvent e) {
        doClose();
    }

    /*
     * TerminalMenuListener interface implementation
     */
    public void close(TerminalMenuHandler origMenu) {
        doClose();
    }

    public void update() {
        // Ignore
    }

    /**
     * Run the application
     */
    public static void main(String[] argv) {

        Properties props = new Properties();
        if(argv.length > 0) {
            String propsFile = argv[0];
            try {
                props.load(new FileInputStream(propsFile));
            } catch (Exception e) {
                System.out.println("Error loading properties: " +
                                   e.getMessage());
            }
        }
        BasicClient ssh2 = new BasicClient(props);
        ssh2.run();
        System.exit(ssh2.getExitStatus());
    }

    /*
     * SSH2Interactor interface
     */
    public String promptLine(String prompt, boolean echo)
        throws SSH2UserCancelException {
        return passwd;
    }
    public String[] promptMulti(String[] prompts, boolean[] echos)
        throws SSH2UserCancelException {
        return promptMultiFull("", "", prompts, echos);
    }
    public String[] promptMultiFull(String name, String instruction,
                                    String[] prompts, boolean[] echos)
        throws SSH2UserCancelException {
        if (prompts.length == 1) {
            return new String[]{passwd};
        } else if (prompts.length == 0) {
            return new String[0];
        }
        throw new SSH2UserCancelException();
    }
    public int promptList(String name, String instruction, String[] choices)
        throws SSH2UserCancelException {
        throw new SSH2UserCancelException();
    }
}
