/**
 * ****************************************************************************
 *
 * Copyright (c) 1999-2011 Cryptzone AB. All Rights Reserved.
 *
 * This file contains Original Code and/or Modifications of Original Code as
 * defined in and that are subject to the MindTerm Public Source License,
 * Version 2.0, (the 'License'). You may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the MindTerm Public Source License along
 * with this software; see the file LICENSE. If not, write to Cryptzone Group
 * AB, Drakegatan 7, SE-41250 Goteborg, SWEDEN
 *
 ****************************************************************************
 */
package com.mindbright.ssh;

import java.applet.AppletContext;

import java.awt.Component;
import java.awt.Image;
import java.awt.Toolkit;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.net.UnknownHostException;

import java.nio.ByteBuffer;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;

import com.mindbright.application.MindTermApp;
import com.mindbright.application.ModuleSFTP;
import com.mindbright.application.ModuleTerminalImpl;

import com.mindbright.gui.Logo;

import com.mindbright.net.WebProxyException;

import com.mindbright.nio.NIOCallback;
import com.mindbright.nio.NonBlockingInput;
import com.mindbright.nio.Switchboard;
import com.mindbright.nio.TimerCallback;

import com.mindbright.ssh2.*;

import com.mindbright.sshcommon.SSHConsoleRemote;

import com.mindbright.terminal.*;

import com.mindbright.util.Util;

public final class SSHInteractiveClient extends SSHClient
        implements Runnable, SSHInteractor, SSH2Interactor, MindTermApp,
        TerminalInputListener, NIOCallback {

    public boolean isSSH2 = false;
    public SSH2Transport transport;
    SSH2Connection connection;
    SSH2TerminalAdapter termAdapter;
    public boolean wantHelpInfo = true;
    public String customStartMessage = null;
    Component parent;
    SSHStdIO sshStdIO;
    SSHPropertyHandler propsHandler;
    public boolean quiet;
    public boolean exitOnLogout;
    boolean initQuiet;
    boolean isFirstPasswdAuth;
    private Switchboard switchboard;
    private boolean isBusy = false;
    private boolean isSSHv1 = false;
    private boolean isSSHv2 = false;
    private boolean kexComplete = false;
    private boolean versionComplete = false;
    private Exception storedException;
    private boolean ssh2AuthNeeded = false;
    private SSH2UserAuth userAuth;
    private SSH2Authenticator authenticator;
    private SSH2SessionChannel session;
    private SSH2SessionChannel session2;
    private TerminalWin terminal;
    private boolean shell;
    private boolean startSFTP = false;

    public SSHInteractiveClient(boolean quiet, boolean exitOnLogout,
            SSHPropertyHandler propsHandler) {
        super(propsHandler, propsHandler);

        this.propsHandler = propsHandler;
        this.interactor = this; // !!! OUCH

        propsHandler.setInteractor(this);
        propsHandler.setClient(this);

        this.quiet = quiet;
        this.exitOnLogout = exitOnLogout;
        this.initQuiet = quiet;

        setConsole(new SSHStdIO());
        sshStdIO = (SSHStdIO) console;
        sshStdIO.setClient(this);
        switchboard = Switchboard.getSwitchboard();
    }

    public SSHInteractiveClient(SSHInteractiveClient clone) {
        this(true, true, new SSHPropertyHandler(clone.propsHandler));

        this.activateTunnels = false;

        this.wantHelpInfo = clone.wantHelpInfo;
        this.customStartMessage = clone.customStartMessage;
    }

    public void setDialogParent(Component parent) {
        this.parent = parent;
    }

    public SSHPropertyHandler getPropertyHandler() {
        return propsHandler;
    }

    public void updateMenus() {
    }

    public void splashScreen() {
        TerminalWin t = getTerminalWin();

        if (t != null) {
            t.clearScreen();
            t.setCursorPos(0, 0);
        }

//        console.println("MindTerm version " + Version.version + 
//                        (com.mindbright.util.Crypto.isFipsMode() ? " (FIPS mode)" : ""));
//        console.println(Version.copyright);
//        console.println(Version.licenseMessage);

//        showLogo();


        if (propsHandler.getSSHHomeDir() != null) {
            if (t != null) {
                t.setCursorPos(t.rows() - 2, 0);
                t.clearLine();
            }
            console.println("\rMindTerm home: " + propsHandler.getSSHHomeDir());
        }
        if (t != null) {
            t.setCursorPos(t.rows() - 1, 0);
            t.clearLine();
        }
    }

    public boolean installLogo() {
        boolean isPresent = false;

        TerminalWin t = getTerminalWin();

        if (t != null) {
            ByteArrayOutputStream baos = readResource("/defaults/logo.gif");
            if (baos != null) {
                byte[] img = baos.toByteArray();
                Image logo = Toolkit.getDefaultToolkit().createImage(img);
                int width = -1;
                int height = -1;
                boolean ready = false;

                while (!ready) {
                    width = logo.getWidth(null);
                    height = logo.getHeight(null);
                    if (width != -1 && height != -1) {
                        ready = true;
                    }
                    Thread.yield();
                }

                t.setLogo(logo, -1, -1, width, height);

                isPresent = true;
            }
        }

        return isPresent;
    }

    public ByteArrayOutputStream readResource(String name) {
        InputStream in = getClass().getResourceAsStream(name);
        ByteArrayOutputStream baos = null;
        if (in != null) {
            baos = new ByteArrayOutputStream();
            try {
                int c;
                while ((c = in.read()) >= 0) {
                    baos.write(c);
                }
            } catch (IOException e) {
                // !!!
                System.err.println("ERROR reading resource " + name + " : " + e);
            } finally {
                try {
                    in.close();
                } catch (IOException ioe) {
                }
            }
        }
        return baos;
    }

    public void doSingleCommand(String commandLine)
            throws Exception {
        this.commandLine = commandLine;

        installLogo();

        splashScreen();
        startSSHClient(false);
    }

    public void startSFTP(boolean startSFTP) {
        this.startSFTP = startSFTP;
    }

    public void run() {
        boolean gotExtMsg;

//        installLogo();

        boolean keepRunning = true;
        while (keepRunning) {
            gotExtMsg = false;
            try {
//                splashScreen();

                storedException = null;

                startSSHClient(true);

                waitForCompletion();

                if (sshStdIO.isConnected()) {
                    // Server died on us without sending disconnect
                    sshStdIO.serverDisconnect("\n\r\n\rServer died or connection lost");
                    disconnect(false);
                    propsHandler.clearServerSetting();
                }

                // !!! Wait for last session to close down entirely (i.e. so
                // disconnected gets a chance to be called...)
                //
                Thread.sleep(1000);

                try {
                    propsHandler.checkSave();
                } catch (IOException e) {
                    alert("Error saving settings!");
                }

            } catch (AuthFailException e) {
                alert("Authentication failed, " + e.getMessage());
                propsHandler.clearPasswords();

            } catch (WebProxyException e) {
                alert(e.getMessage());
                propsHandler.clearPasswords();

            } catch (SSHStdIO.SSHExternalMessage e) {
                gotExtMsg = true;
                String msg = e.getMessage();

                if (msg != null && msg.trim().length() > 0) {
                    alert(e.getMessage());
                }

            } catch (UnknownHostException e) {
                String host = e.getMessage();
                if (propsHandler.getProperty("proxytype").equals("none")) {
                    alert("Unknown host: " + host);
                } else {
                    alert("Unknown proxy host: " + host);
                }
                propsHandler.clearServerSetting();

            } catch (FileNotFoundException e) {
                alert("File not found: " + e.getMessage());

            } catch (Exception e) {
                String msg = e.getMessage();
                if (msg == null || msg.trim().length() == 0) {
                    msg = e.toString();
                }
                msg = "Error connecting to " + propsHandler.getProperty("server") + ", reason:\n"
                        + "-> " + msg;
                alert(msg);
                if (SSH.DEBUGMORE) {
                    e.printStackTrace();
                }

            } catch (ThreadDeath death) {
                if (controller != null) {
                    controller.killAll();
                }
                controller = null;
                return;
            }

            propsHandler.passivateProperties();
            activateTunnels = true;

            if (!gotExtMsg) {
                if (!propsHandler.savePasswords || usedOTP) {
                    propsHandler.clearPasswords();
                }
                propsHandler.currentPropsFile = null;
                if (!propsHandler.autoLoadProps) {
                    propsHandler.clearPasswords();
                    initQuiet = false;
                }
                quiet = false;
            }

            controller = null;

            TerminalWin t = getTerminalWin();
            if (t != null) {
                t.setTitle(null);
            }

            keepRunning = !exitOnLogout || gotExtMsg;
        }
    }

    public long getConnectTimeout() {
        return propsHandler.getPropertyI("connect-timeout") * 1000;
    }

    public long getHelloTimeout() {
        return propsHandler.getPropertyI("hello-timeout") * 1000;
    }

    public long getKexTimeout() {
        return propsHandler.getPropertyI("kex-timeout") * 1000;
    }

    private void startSSHClient(boolean shell) throws Exception {
        isBusy = true;
        isSSHv1 = false;
        isSSHv2 = false;
        kexComplete = false;
        versionComplete = false;
        ssh2AuthNeeded = false;

        isFirstPasswdAuth = true;

        this.shell = shell;

        // This starts a connection to the sshd and all the related stuff...
        //
        bootSSH(shell, true);
        switchboard.notifyWhenConnected(sshSocket, getConnectTimeout(), this);
    }
    byte[] versionBuf;
    int versionLength;
    private int onlyAllowVersion = 0;

    public void connected(boolean timeout) {
        try {
            if (timeout) {
                storedException = new IOException(
                        "Server closed connection before sending identification");
                synchronized (this) {
                    isBusy = false;
                    notify();
                }
                return;
            }

            String proto = propsHandler.getProperty("protocol");
            if ("auto".equals(proto)) {
                onlyAllowVersion = 0;
            } else if ("ssh1".equals(proto)) {
                onlyAllowVersion = 1;
            } else {
                onlyAllowVersion = 2;
            }

            long hellotimeout = getHelloTimeout();
            if (hellotimeout > 0) {
                timeoutcallback = new HelloTimeoutCallback(hellotimeout);
            }
            ByteBuffer b = sshSocket.getInput().createBuffer(new byte[1]);
            versionBuf = new byte[256];
            versionLength = 0;
            sshSocket.getInput().read(b, this);
        } catch (IOException e) {
            shutdown(e);
        }
    }

    public void connectionFailed(Exception e) {
        storedException = e;
        synchronized (this) {
            isBusy = false;
            notify();
        }
    }
    private HelloTimeoutCallback timeoutcallback = null;

    private class HelloTimeoutCallback implements TimerCallback {

        private volatile Object handler;
        private volatile boolean running;

        protected HelloTimeoutCallback(long interval) {
            handler = switchboard.registerTimer(interval, this);
            running = true;
        }

        public void timerTrig() {
            if (running) {
                running = false;
                (new Thread(new Runnable() {
                    public void run() {
                        switchboard.unregisterTimer(handler);
                        connectionFailed(new IOException("Timeout"));
                    }
                })).start();
            }
        }

        protected void stop() {
            if (running) {
                running = false;
                switchboard.unregisterTimer(handler);
            }
        }
    }

    public void completed(ByteBuffer buf) {

        if (super.isReading) {
            super.completed(buf);
            return;
        }

        NonBlockingInput in = sshSocket.getInput();

        try {
            do {
                if (buf.position() == 0) {
                    continue;
                }

                byte c = buf.get(0);

                // Ignore any \r
                if (c == '\r') {
                    buf.clear();
                    continue;
                }

                // If not \n then just append it to the buffer
                if (c != '\n') {
                    if (versionLength < versionBuf.length) {
                        versionBuf[versionLength++] = c;
                    }
                    buf.clear();
                    continue;
                }

                String id = new String(versionBuf, 0, versionLength);

                if (id.startsWith("SSH-")) {
                    int hyphenCnt = 0;
                    for (int i = 0; i < versionLength; i++) {
                        if (versionBuf[i] == '-') {
                            hyphenCnt++;
                        }
                    }

                    if (hyphenCnt >= 2) {
                        versionBuf[versionLength++] = '\n';
                        in.pushback(versionBuf, 0, versionLength);
                        String verStr = new String(versionBuf, 0, versionLength);
                        try {
                            int major = getMajor(verStr);
                            int minor = getMinor(verStr);
                            if (minor == 99) {
                                major = (onlyAllowVersion == 1) ? 1 : 2;
                            }
                            versionKnown(major);
                        } catch (Exception e) {
                            versionKnown(2);
                        }
                        if (timeoutcallback != null) {
                            timeoutcallback.stop();
                            timeoutcallback = null;
                        }
                        return;
                    }
                }

                versionLength = 0;
                buf.clear();
            } while (in.read(buf, this, true, false));
        } catch (IOException e) {
            shutdown(e);
        }
        if (timeoutcallback != null) {
            timeoutcallback.stop();
            timeoutcallback = null;
        }
    }

    public void readFailed(Exception e) {
        System.out.println("readFailed: " + e);
    }

    public void writeFailed() {
        System.out.println("writeFailed:");
    }

    private static int getMajor(String versionStr) throws IOException {
        try {
            int r = versionStr.indexOf('.', 4);
            return Integer.parseInt(versionStr.substring(4, r));
        } catch (NumberFormatException e) {
            throw new IOException("corrupt version string: " + versionStr);
        }
    }

    private static int getMinor(String versionStr) throws IOException {
        try {
            int l = versionStr.indexOf('.', 4) + 1;
            int r = versionStr.indexOf('-', l);
            return Integer.parseInt(versionStr.substring(l, r));
        } catch (NumberFormatException e) {
            throw new IOException("corrupt version string: " + versionStr);
        }
    }

    private void versionKnown(int major) throws IOException {
        if (onlyAllowVersion != 0 && major != onlyAllowVersion) {
            throw new IOException("Requested SSH version not supported by remote server");
        }
        if (major == 1) {
            isSSHv1 = true;
            console.println("Warning connecting using ssh1, "
                    + "consider upgrading server!");
            console.println("");
            boot(shell, sshSocket);
        } else {
            isSSHv2 = true;
            runSSH2Client();
        }
        synchronized (this) {
            versionComplete = true;
            notify();
        }
    }

    public boolean isDumb() {
        return (console.getTerminal() == null);
    }

    public TerminalWin getTerminalWin() {
        TerminalWindow term = console.getTerminal();
        if (term != null && term instanceof TerminalWin) {
            return (TerminalWin) term;
        }
        return null;
    }

    public void showLogo() {
        TerminalWin t = getTerminalWin();
        if (t != null) {
            t.showLogo();
        }
    }

    public void hideLogo() {
        TerminalWin t = getTerminalWin();
        if (t != null) {
            t.hideLogo();
        }
    }

    public Logo getLogo() {
        Logo logo = null;
        TerminalWin t = getTerminalWin();
        if (t != null) {
            Image img = t.getLogo();
            if (img != null) {
                logo = new Logo(img);
            }
        }
        return logo;
    }

    public void updateTitle() {
        sshStdIO.updateTitle();
    }

    //
    // SSH2Interactor interface
    //
    public String promptLine(String prompt, boolean echo)
            throws SSH2UserCancelException {
        try {
            String ret = null;
            while (ret == null) {
                if (echo) {
                    ret = promptLine(prompt, "");
                } else {
                    ret = promptPassword(prompt);
                }
            }
            return ret;
        } catch (IOException e) {
            throw new SSH2UserCancelException(e.getMessage());
        }
    }

    public String[] promptMulti(String[] prompts, boolean[] echos)
            throws SSH2UserCancelException {
        return promptMultiFull(null, null, prompts, echos);
    }

    public String[] promptMultiFull(String name, String instruction,
            String[] prompts, boolean[] echos)
            throws SSH2UserCancelException {
        try {
            if (name != null && name.length() > 0) {
                console.println(name);
            }
            if (instruction != null && instruction.length() > 0) {
                console.println(instruction);
            }
            String[] resp = new String[prompts.length];
            String tmp;
            for (int i = 0; i < prompts.length; i++) {
                tmp = null;
                while (tmp == null) {
                    if (echos[i]) {
                        tmp = promptLine(prompts[i], "");
                    } else {
                        tmp = promptPassword(prompts[i]);
                    }
                }
                resp[i] = tmp;
            }
            return resp;
        } catch (IOException e) {
            throw new SSH2UserCancelException(e.getMessage());
        }
    }

    public int promptList(String name, String instruction, String[] choices)
            throws SSH2UserCancelException {
        try {
            console.println(name);
            console.println(instruction);
            for (int i = 0; i < choices.length; i++) {
                console.println(i + ") " + choices[i]);
            }
            String choice = null;
            while (choice == null) {
                choice = promptLine("Choice", "0");
            }
            return Integer.parseInt(choice);
        } catch (Exception e) {
            throw new SSH2UserCancelException(e.getMessage());
        }
    }

    //
    // SSHInteractor interface
    //
    public void propsStateChanged(SSHPropertyHandler props) {
        updateMenus();
    }

    public void startNewSession(SSHClient client) {
        // Here we can have a login-dialog with proxy-info also (or
        // configurable more than one method)
    }

    public void sessionStarted(SSHClient client) {
        quiet = initQuiet;
    }

    public boolean quietPrompts() {
        return (commandLine != null || quiet);
    }

    public boolean isVerbose() {
        return wantHelpInfo;
    }

    public String promptLine(String prompt, String defaultVal) throws IOException {
        return sshStdIO.promptLine(prompt, defaultVal, false);
    }

    public String promptPassword(String prompt) throws IOException {
        return sshStdIO.promptLine(prompt, "", true);
    }

    public boolean askConfirmation(String message, boolean defAnswer) {
        boolean confirm = false;
        try {
            confirm = askConfirmation(message, true, defAnswer);
        } catch (IOException e) {
            // !!!
        }
        return confirm;
    }

    public boolean askConfirmation(String message, boolean preferDialog,
            boolean defAnswer)
            throws IOException {
        boolean confirm = false;

        String answer = null;
        while (answer == null) {
            answer = promptLine(message + (defAnswer ? " ([yes]/no) "
                    : "(yes/[no]) "), "");
        }
        if (answer.equalsIgnoreCase("yes") || answer.equals("y")) {
            confirm = true;
        } else if (answer.equals("")) {
            confirm = defAnswer;
        }
        return confirm;
    }

    public boolean licenseDialog(String license) {
        if (license != null) {
            Component p = getDialogParent();
            if (p == null) {
                return false;
            }
            return SSHMiscDialogs.confirm("MindTerm - License agreeement",
                    license,
                    24, 55, "Accept", "Decline",
                    false, p, true);
        }
        return false;
    }

    public void connected(SSHClient client) {
        updateMenus();
        //console.println("Connected to server running " + srvVersionStr);
    }

    public void open(SSHClient client) {
        updateMenus();
        updateTitle();
    }

    public void disconnected(SSHClient client, boolean graceful) {
        sshStdIO.breakPromptLine("Login aborted by user");
        updateMenus();
        updateTitle();
    }

    public void report(String msg) {
        if (msg != null && msg.length() > 0) {
            console.println(msg);
        }
        console.println("");
    }

    public SSH2Interactor getInteractor() {
        return this;
    }

    public void alert(String msg) {
        if (msg == null) {
            msg = "Unknown error (null)";
        }
        report(msg);
    }

    public void forcedDisconnect() {
        if (isSSH2) {
            transport.normalDisconnect("Closed by user");
        } else {
            super.forcedDisconnect();
        }
    }

    public void requestLocalPortForward(String localHost, int localPort,
            String remoteHost, int remotePort,
            String plugin)
            throws IOException {
        if (isSSH2) {
            SSH2StreamFilterFactory filter = null;
            if ("ftp".equals(plugin)) {
                String serverLocalAddr = propsHandler.getProperty("real-server");
                if (serverLocalAddr == null) {
                    serverLocalAddr = propsHandler.getProperty("server");
                }
                filter = new SSH2FTPProxyFilter(localHost, serverLocalAddr);
            } else if ("sniff".equals(plugin)) {
                filter = SSH2StreamSniffer.getFilterFactory();
            }
            connection.newLocalForward(localHost, localPort,
                    remoteHost, remotePort, filter);
        } else {
            super.requestLocalPortForward(localHost, localPort,
                    remoteHost, remotePort, plugin);
        }
    }

    public void addRemotePortForward(String remoteHost, int remotePort,
            String localHost, int localPort,
            String plugin) {
        super.addRemotePortForward(remoteHost, remotePort, localHost, localPort, plugin);
        if (isSSH2) {
            connection.newRemoteForward(remoteHost, remotePort,
                    localHost, localPort);
        }
    }

    public void delLocalPortForward(String localHost, int port) {
        boolean isop = isOpened;
        if (isSSH2) {
            connection.deleteLocalForward(localHost, port);
            isOpened = false;
        }
        super.delLocalPortForward(localHost, port);
        isOpened = isop;
    }

    public void delRemotePortForward(String remoteHost, int port) {
        super.delRemotePortForward(remoteHost, port);
        if (isSSH2) {
            connection.deleteRemoteForward(remoteHost, port);
        }
    }

    void setAliveInterval(int i) {
        if (isSSH2) {
            transport.enableKeepAlive(i);
        } else {
            super.setAliveInterval(i);
        }
    }

    void runSSH2Client() throws IOException {
        try {
            SSH2Preferences prefs;
            isSSH2 = true;
            prefs = new SSH2Preferences(propsHandler.getProperties());

            if (SSH.DEBUGMORE) {
                prefs.setPreference(SSH2Preferences.LOG_LEVEL, "7");
            }
            transport = new SSH2Transport(sshSocket, prefs, null, secureRandom());

            transport.setEventHandler(new SSH2TransportEventAdapter() {
                public boolean kexAuthenticateHost(
                        SSH2Transport tp, SSH2Signature serverHostKey) {
                    try {
                        propsHandler.showFingerprint(
                                serverHostKey.getPublicKeyBlob(),
                                serverHostKey.getAlgorithmName());
                        if (fingerprintMatch(serverHostKey)) {
                            return true;
                        }
                        return propsHandler.verifyKnownSSH2Hosts(
                                SSHInteractiveClient.this,
                                serverHostKey);
                    } catch (SSH2Exception e) {
                        transport.getLog().error("SSHInteractiveClient",
                                "verifyKnownSSH2Hosts",
                                "Error " + e.getMessage());
                    } catch (IOException e) {
                        transport.getLog().error("SSHInteractiveClient",
                                "verifyKnownSSH2Hosts",
                                "Error " + e.getMessage());
                    }
                    return false;
                }

                public void gotConnectInfoText(SSH2Transport tp,
                        String text) {
                    alert(text);
                }

                public void kexComplete(SSH2Transport tp) {
                    if (!isConnected) {
                        handleKEXComplete();
                    }
                }

                public void normalDisconnect(SSH2Transport tp, String description,
                        String languageTag) {
                    transport.getLog().error("SSHInteractiveClient",
                            "normalDisconnect",
                            "Disconnected, reason: " + description);
                }

                public void fatalDisconnect(SSH2Transport tp, int reason,
                        String description, String languageTag) {
                    alert(description);
                    transport.getLog().error("SSHInteractiveClient",
                            "fatalDisconnect",
                            "Disconnected, reason: " + description);
                }

                public void peerDisconnect(SSH2Transport tp, int reason,
                        String description, String languageTag) {
                    transport.getLog().error("SSHInteractiveClient",
                            "peerDisconnect",
                            "Disconnected, reason: " + description);
                }
            });

            transport.boot();
            srvVersionStr = transport.getServerVersion();

            if (transport.isConnected() && srvVersionStr != null) {
                connected(null);
            }

        } catch (Exception e) {
            disconnect(false);
            throw new IOException("Error in ssh2: " + e.getMessage()
                    + "{" + e + "}");
        }

        if (!transport.isConnected() || srvVersionStr == null) {
            disconnect(false);
            throw new IOException("Failed to connect to server");
        }
    }

    private void handleKEXComplete() {
        isConnected = true;
        ssh2AuthNeeded = true;
        synchronized (this) {
            kexComplete = true;
            notify();
        }
    }

    private void doSSH2Auth() {
        authenticator =
                new SSH2Authenticator() {
            public void peerMethods(String methods) {
                addAuthModules(this, methods);
            }

            public void displayBanner(String banner) {
                alert(banner);
            }

            public void noMoreMethods() {
            }

            public void authSuccess(String method) {
                authCompleted();
            }

            public void moduleCancel(String method, String reason) {
            }
        };

        try {
            authenticator.setUsername(propsHandler.getUsername(null));
        } catch (IOException e) {
        }

        userAuth = new SSH2UserAuth(transport, authenticator);
        userAuth.authenticateUser("ssh-connection");
    }

    private void authCompleted() {

        connection = new SSH2Connection(userAuth, transport, null);
        connection.setEventHandler(new SSH2ConnectionEventAdapter() {
            public void localSessionConnect(SSH2Connection connection,
                    SSH2Channel channel) {
            }

            public void localDirectConnect(SSH2Connection connection,
                    SSH2Listener listener,
                    SSH2Channel channel) {
                synchronized (tunnels) {
                    tunnels.add(channel);
                }
            }

            public void channelClosed(SSH2Connection connection,
                    SSH2Channel channel) {
                synchronized (tunnels) {
                    tunnels.remove(channel);
                }
            }
        });
        transport.setConnection(connection);
        authenticator.clearSensitiveData();

        if (console != null) {
            console.serverConnect(null, null);
        }
        isOpened = true;
        open(null);

        // !!! Ouch
        // Activate tunnels at this point
        //
        propsHandler.passivateProperties();
        propsHandler.activateProperties();

        terminal = getTerminalWin();

        if (terminal != null) {
            terminal.addInputListener(this);
            termAdapter = new SSH2TerminalAdapterImpl(terminal);
            session = connection.newTerminal(termAdapter);
            // !!! OUCH must do this here since activateProperties is above
            if (propsHandler.hasKeyTimingNoise()) {
                termAdapter.startChaff();
            }

            if (session.openStatus() != SSH2Channel.STATUS_OPEN) {
                shutdown(
                        new IOException("Failed to open ssh2 session channel"));
            }

            startSession();
        } else {
            session = connection.newSession();
        }

        int status = 0;
        if (session2 != null) {
            status = session2.waitForExit(0);
        } else {
            status = session.waitForExit(0);
        }

        if (terminal != null) {
            terminal.removeInputListener(this);
        }
        termAdapter.detach();

        transport.normalDisconnect("Disconnect by user");

        console.serverDisconnect(getServerAddr().getHostName()
                + " disconnected: " + status);
        disconnect(true);

        if (propsHandler.getCompressionLevel() != 0) {
            SSH2Compressor comp;
            for (int i = 0; i < 2; i++) {
                comp = (i == 0 ? transport.getTxCompressor()
                        : transport.getRxCompressor());
                if (comp != null) {
                    String msg;
                    long compressed, uncompressed;
                    compressed = comp.numOfCompressedBytes();
                    uncompressed = (comp.numOfUncompressedBytes() > 0
                            ? comp.numOfUncompressedBytes() : 1);
                    msg = " raw data (bytes) = " + uncompressed
                            + ", compressed = " + compressed + " ("
                            + ((compressed * 100) / uncompressed) + "%)";
                    console.println((i == 0 ? "outgoing" : "incoming")
                            + msg);
                }
            }
        }

        sshStdIO.setTerminal(terminal);
    }

    private void startSession() {
        if (user.wantX11Forward()) {
            session.requestX11Forward(false, 0);
        }

        if (user.wantPTY()) {
            session.requestPTY(terminal.terminalType(),
                    terminal.rows(),
                    terminal.cols(),
                    null);
        }

        if (commandLine != null) {
            session.doSingleCommand(commandLine);
        } else if (startSFTP) {
            ModuleSFTP sftp = new ModuleSFTP();
            sftp.init(this);
            sftp.activate(this);
            session2 = sftp.getSessionChannel();
        } else {
            if (!session.doShell()) {
                shutdown(
                        new IOException("Server refused to start interactive shell"));
            }
        }
    }

    public void callback(Object arg) {
        if (session.openStatus() != SSH2Channel.STATUS_OPEN) {
            shutdown(new IOException("Failed to open ssh2 session channel"));
            return;
        }

        startSession();
    }

    public boolean fingerprintMatch(SSH2Signature serverHostKey) {
        String fp = propsHandler.getProperty("fingerprint");
        if (fp == null) {
            fp = propsHandler.getProperty("fingerprint."
                    + propsHandler.getProperty("server")
                    + "."
                    + propsHandler.getProperty("port"));
        }
        if (fp != null) {
            if (SSH2HostKeyVerifier.compareFingerprints(fp, serverHostKey)) {
                return true;
            }
            if (propsHandler.askChangeKeyConfirmation()) {
                byte[] blob = null;
                try {
                    blob = serverHostKey.getPublicKeyBlob();
                } catch (SSH2SignatureException e) {
                    return false;
                }
                String fpMD5Hex = SSH2KeyFingerprint.md5Hex(blob);
                propsHandler.setProperty("fingerprint", fpMD5Hex);
            }
        }
        return false;
    }

    public void typedChar(char c) {
    }

    public void typedChar(byte[] b) {
    }

    public void sendBytes(byte[] b) {
    }

    public void sendBytesDirect(byte[] b) {
    }

    public void sendBreak() {
    }

    public void signalWindowChanged(int rows, int cols,
            int vpixels, int hpixels) {
        updateTitle();
    }

    public void signalTermTypeChanged(String newTermType) {
    }

    private static boolean isAbsolutePath(String filename) {
        try {
            return (new File(filename)).isAbsolute();
        } catch (Throwable t) {
        }
        return false;
    }

    public void addAuthModules(SSH2Authenticator authenticator, String methods) {
        try {
            int[] authTypes = propsHandler.getAuthTypes(null);
            boolean allUnsupported = true;
            for (int i = 0; i < authTypes.length; i++) {
                int type = authTypes[i];
                if (!SSH2ListUtil.isInList(methods, SSH.getAuthName(type))
                        && !SSH2ListUtil.isInList(methods, SSH.getAltAuthName(type))
                        && !((type == AUTH_SDI)
                        && SSH2ListUtil.isInList(methods, "securid-1@ssh.com"))) {
                    continue;
                }
                allUnsupported = false;
                switch (type) {
                    case AUTH_PUBLICKEY: {
                        String keyFile = propsHandler.getProperty("idfile");
                        if (!isAbsolutePath(keyFile)) {
                            if (propsHandler.getSSHHomeDir() != null) {
                                keyFile = propsHandler.getSSHHomeDir() + keyFile;
                            }
                        }

                        transport.getLog().info(
                                "SSHInteractiveClient", "Loading file: " + keyFile);
                        SSH2KeyPairFile kpf = new SSH2KeyPairFile();
                        try {
                            kpf.load(keyFile, "");
                        } catch (SSH2FatalException e) {
                            transport.getLog().error(
                                    "SSHInteractiveClient", "addAuthModules",
                                    "Failed to load private key " + keyFile);
                            throw new IOException(e.getMessage());
                        } catch (SSH2AccessDeniedException e) {
                            String comment = kpf.getComment();
                            if (comment == null || comment.trim().length() == 0) {
                                comment = keyFile;
                            }
                            String prompt = "Key '" + comment + "' password: ";
                            String passwd =
                                    propsHandler.getIdentityPassword(prompt);
                            kpf.load(keyFile, passwd);
                        } catch (Throwable t) {
                            t.printStackTrace();
                            break;
                        }

                        String alg = kpf.getAlgorithmName();
                        SSH2Signature sign = SSH2Signature.getInstance(alg);

                        sign.initSign(kpf.getKeyPair().getPrivate());
                        sign.setPublicKey(kpf.getKeyPair().getPublic());

                        authenticator.addModule(new SSH2AuthPublicKey(sign));

                        break;
                    }
                    case AUTH_PASSWORD:
                        String pass = getProperty("password");
                        if (isFirstPasswdAuth) {
                            isFirstPasswdAuth = false;
                        } else {
                            propsHandler.eraseProperty("password");
                            pass = null;
                        }
                        String p = getProperty("username") + "@"
                                + getProperty("server") + "'s password: ";

                        authenticator.addModule(new SSH2AuthPassword(this, p, pass));
                        pass = null;
                        break;
                    case AUTH_GSSAPI:
                        String realm = getProperty("krb5-realm");
                        String kdc = getProperty("krb5-kdc");
                        authenticator.addModule(new SSH2AuthGSS(realm, kdc));
                        break;
                    case AUTH_HOSTBASED: {
                        String keyFile = propsHandler.getProperty("private-host-key");
                        if (!isAbsolutePath(keyFile)) {
                            if (propsHandler.getSSHHomeDir() != null) {
                                keyFile = propsHandler.getSSHHomeDir() + keyFile;
                            }
                        }

                        SSH2KeyPairFile kpf = new SSH2KeyPairFile();
                        try {
                            kpf.load(keyFile, "");
                        } catch (SSH2FatalException e) {
                            throw new IOException(e.getMessage());
                        } catch (SSH2AccessDeniedException e) {
                            String comment = kpf.getComment();
                            if (comment == null || comment.trim().length() == 0) {
                                comment = keyFile;
                            }
                            String prompt = "Key '" + comment + "' password: ";
                            String passwd =
                                    propsHandler.getIdentityPassword(prompt);
                            kpf.load(keyFile, passwd);
                        }

                        String alg = kpf.getAlgorithmName();
                        SSH2Signature sign = SSH2Signature.getInstance(alg);

                        sign.initSign(kpf.getKeyPair().getPrivate());
                        sign.setPublicKey(kpf.getKeyPair().getPublic());

                        authenticator.addModule(new SSH2AuthHostBased(sign));
                        break;
                    }
                    case AUTH_SDI:
                    case AUTH_TIS:
                    case AUTH_CRYPTOCARD:
                    case AUTH_KBDINTERACT:
                        pass = getProperty("password");
                        if (isFirstPasswdAuth) {
                            isFirstPasswdAuth = false;
                        } else {
                            propsHandler.eraseProperty("password");
                            pass = null;
                        }
                        authenticator.addModule(new SSH2AuthKbdInteract(pass, this));
                        authenticator.addModule(
                                new SSH2AuthSSHComSecurID(
                                this,
                                "Enter Passcode: ",
                                "Wait for token to change and enter Passcode: ",
                                "New PIN:",
                                "Confirm new PIN: ",
                                "Do you want to create your own new PIN (yes/no)? ",
                                "Accept the server assigned PIN: "));
                        break;
                    default:
                        throw new IOException("Authentication type "
                                + authTypeDesc[authTypes[i]]
                                + " is not supported in SSH2");
                }
            }
            if (allUnsupported) {
                for (int i = 0; i < authTypes.length; i++) {
                    report("Authentication method '"
                            + SSH.getAuthName(authTypes[i])
                            + "' not supported by server.");
                }
            }
        } catch (Exception e) {
            if (SSH.DEBUGMORE) {
                System.out.println("Error when setting up authentication: ");
                int[] t = propsHandler.getAuthTypes(null);
                for (int i = 0; i < t.length; i++) {
                    System.out.print(t[i] + ", ");
                }
                System.out.println("");
                e.printStackTrace();
            }
            alert("Error when setting up authentication: " + e.getMessage());
        }
    }

    public void newShell() {
        // Ouch, this is kludgy because the MindTerm class handles the thread
        ModuleTerminalImpl terminal = new ModuleTerminalImpl();
        terminal.init(this);
        terminal.run();
    }

    public String getVersionId(boolean client) {
        String idStr = "SSH-" + SSH_VER_MAJOR + "." + SSH_VER_MINOR + "-";
        idStr += propsHandler.getProperty("package-version");
        return idStr;
    }

    public void closeTunnelFromList(int listIdx) {
        if (isSSH2) {
            synchronized (tunnels) {
                SSH2Channel c = tunnels.get(listIdx);
                c.close();
            }
        } else {
            controller.closeTunnelFromList(listIdx);
        }
    }
    private ArrayList<SSH2Channel> tunnels = new ArrayList<SSH2Channel>();

    public String[] listTunnels() {
        if (isSSH2) {
            ArrayList<String> v = new ArrayList<String>();
            synchronized (tunnels) {
                for (SSH2Channel ch : tunnels) {
                    v.add(((SSH2TCPChannel) ch).toString());
                }
            }
            return v.toArray(new String[0]);
        }
        return controller.listTunnels();
    }

    //
    // MindTermApp interface implementation
    //
    public String getHost() {
        return getServerAddr().getHostName();
    }

    public int getPort() {
        return propsHandler.getSrvPort();
    }

    public Properties getProperties() {
        Properties props = new Properties(propsHandler.getProperties());
        TerminalWin term = getTerminalWin();
        Properties termProps = (term != null ? term.getProperties() : null);
        if (termProps != null) {
            Enumeration<Object> e = termProps.keys();
            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                String val = termProps.getProperty(key);
                props.put(key, val);
            }
        }
        return props;
    }

    public String getProperty(String name) {
        String value = propsHandler.getProperty(name);
        if (value == null) {
            TerminalWin term = getTerminalWin();
            if (term != null) {
                value = term.getProperty(name);
            }
        }
        return value;
    }

    public void setProperty(String name, String value) {
        propsHandler.setProperty(name, value);
    }

    public String getUserName() {
        return propsHandler.getProperty("username");
    }

    public Component getDialogParent() {
        if (parent != null) {
            return parent;
        }
        return null;
    }

    public String getAppName() {
        return "MindTerm";
    }

    public boolean isApplet() {
        return false;
//        return ((SSHMenuHandlerFull)menus).mindterm.weAreAnApplet;
    }

    public AppletContext getAppletContext() {
        return null;
//        return ((SSHMenuHandlerFull)menus).mindterm.getAppletContext();
    }

    public SSH2Transport getTransport() {
        return transport;
    }

    public SSH2Connection getConnection() {
        return connection;
    }

    public SSHConsoleRemote getConsoleRemote() {
        SSHConsoleRemote remote = null;
        if (isSSH2) {
            remote = new SSH2ConsoleRemote(getConnection());
        } else {
            quiet = true;
            try {
                remote = new SSHConsoleClient(propsHandler.getSrvHost(),
                        propsHandler.getSrvPort(),
                        propsHandler, null);
                ((SSHConsoleClient) remote).setClientUser(propsHandler);
            } catch (IOException e) {
                alert("Error creating remote console: " + e.getMessage());
            }
        }
        return remote;
    }

    protected void disconnect(boolean graceful, Exception e) {
        if (storedException == null) {
            storedException = e;
        }

        super.disconnect(graceful, e);

        synchronized (this) {
            isBusy = false;
            notify();
        }
    }

    public void waitForCompletion() throws Exception {
        if (isBusy) {
            try {
                synchronized (this) {
                    if (isBusy && !versionComplete) {
                        wait();
                    }
                }
                if (isSSHv1 && isConnected) {
                    waitForExit();
                }
                if (isSSHv2) {
                    synchronized (this) {
                        if (isBusy && !kexComplete) {
                            wait();
                        }
                    }
                    if (ssh2AuthNeeded) {
                        ssh2AuthNeeded = false;
                        doSSH2Auth();
                    }
                }
            } catch (InterruptedException e) {
            }
        }

        if (storedException != null) {
            Exception e = storedException;
            storedException = null;
            throw e;
        }
    }

    /**
     * Get the local MIT_MAGIC_COOKIE to use for X11 connections. This procedure
     * uses xauth to get the cookie for $DISPLAY if possible.
     *
     * @return byte-buffer containing cookie or null
     */
    public static byte[] getX11Cookie() {
        // Check if the required environment variables are set
        if (System.getenv("DISPLAY") == null
                || System.getenv("XAUTHORITY") == null) {
            // No, no cookie
            return null;
        }

        // xauth command used to get cookie
        try {
            Process proc = Runtime.getRuntime().exec(
                    new String[]{"xauth", "-q", "list", System.getenv("DISPLAY")});

            // Get command output
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(proc.getInputStream()));
            String line = in.readLine();
            in.close();
            if (line == null) {
                return null;
            }
            int i = line.lastIndexOf(" ");
            String hex = line.substring(i + 1);
            byte[] cookie = Util.byteArrayFromHexString(hex);
            return cookie;

        } catch (Exception e) {
            return null;
        }
    }
}
