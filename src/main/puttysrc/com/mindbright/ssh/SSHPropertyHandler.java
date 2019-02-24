/**
 * ****************************************************************************
 *
 * Copyright (c) 1999-2011 Cryptzone Group AB. All Rights Reserved.
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

import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.Hashtable;
import java.util.Properties;
import java.util.NoSuchElementException;
import java.util.Enumeration;
import java.util.Date;

import java.security.interfaces.RSAPublicKey;

import com.mindbright.nio.NetworkConnection;
import com.mindbright.net.*;
import com.mindbright.terminal.*;
import com.mindbright.util.EncryptedProperties;
import com.mindbright.util.Util;

import com.mindbright.ssh2.*;

public final class SSHPropertyHandler implements SSHClientUser, SSHAuthenticator, ProxyAuthenticator {

    static public final int PROP_NAME = 0;
    static public final int PROP_VALUE = 1;
    static public final String PROPS_FILE_EXT = ".mtp";
    static public final String DEF_IDFILE = "identity";
    public final static String def_hostKeyAlgs = "ssh-rsa,ssh-dss";
    public final static String def_macAlgs =
            "hmac-md5,hmac-sha1,hmac-sha1-96,hmac-md5-96,hmac-ripemd160,hmac-sha256@openssh.com,hmac-sha256-96@openssh.com";
    public final static String def_ciphAlgsSort =
            "aes128-ctr,aes128-cbc,aes192-ctr,aes192-cbc,aes256-ctr,aes256-cbc,"
            + "blowfish-ctr,blowfish-cbc,twofish128-ctr,twofish128-cbc,twofish192-ctr,"
            + "twofish192-cbc,twofish256-ctr,twofish256-cbc,cast128-cbc,"
            + "3des-ctr,3des-cbc,"
            + "arcfour128,arcfour256,arcfour";
    public String hostKeyAlgs = def_hostKeyAlgs;
    public String macAlgs = def_macAlgs;
    public String ciphAlgsSort = def_ciphAlgsSort;
    static public final Properties defaultProperties = new Properties();
    static public final Hashtable<String, String> defaultPropNames = new Hashtable<String, String>();
    static public final Hashtable<String, String> oldPropNames = new Hashtable<String, String>();
    static public final String[][] defaultPropDesc = {
        {"protocol", "auto"},
        {"server", null},
        {"real-server", null},
        {"local-bind", "127.0.0.1"},
        {"port", String.valueOf(SSH.DEFAULTPORT)},
        {"connect-timeout", "60"},
        {"hello-timeout", "10"},
        {"kex-timeout", "180"},
        {"proxy-type", "none"},
        {"proxy-host", null},
        {"proxy-port", null},
        {"proxy-user", null},
        {"proxy-timeout", "30"},
        {"username", null},
        {"auto-username", "false"},
        {"password", null},
        {"tispassword", null},
        {"passphrase", null},
        {"proxy-password", null},
        {"ssh1-cipher", SSH.getCipherName(SSH.CIPHER_ANY)},
        {"auth-method", "password,kbd-interact"},
        {"private-key", DEF_IDFILE},
        {"private-host-key", null},
        {"x11-display", "auto"},
        {"mtu", "0"},
        {"alive", "30"},
        {"compression", "0"},
        {"x11-forward", "false"},
        {"force-pty", "true"},
        {"remfwd", "false"},
        {"portftp", "false"},
        {"sftpbridge-host", ""},
        {"sftpbridge-port", ""},
        {"sftpbridge-hosttype", null},
        {"socksproxy-host", ""},
        {"socksproxy-port", ""},
        {"strict-hostid", "false"},
        {"key-timing-noise", "false"},
        {"kex-algorithms",
            "diffie-hellman-group-exchange-sha256,diffie-hellman-group-exchange-sha1,"
            + "diffie-hellman-group14-sha1,diffie-hellman-group1-sha1"},
        {"server-host-key-algorithms", def_hostKeyAlgs},
        {"enc-algorithms-cli2srv", def_ciphAlgsSort},
        {"enc-algorithms-srv2cli", def_ciphAlgsSort},
        {"mac-algorithms-cli2srv", def_macAlgs},
        {"mac-algorithms-srv2cli", def_macAlgs},
        {"comp-algorithms-cli2srv", "none"},
        {"comp-algorithms-srv2cli", "none"},
        {"languages-cli2srv", ""},
        {"languages-srv2cli", ""},
        {"package-version", "MindTerm_" + Version.version},
        {"filelist-remote-command", "ls -A -L -F -1\n"},
        {"fingerprint", null},
        {"allow-new-server", "true"},
        {"jar-path", "."},
        {"module0", "com.mindbright.application.ModuleTelnet"},
        {"module1", "com.mindbright.application.ModuleSFTP"},
        {"module2", "com.mindbright.application.ModuleSCP"},
        {"module3", "com.mindbright.application.ModuleFTPOverSFTP"},
        {"module4", "com.mindbright.application.ModuleSocksProxy"},
        {"module5", "com.mindbright.application.ModuleTelnetProxy"},
        {"module6", "com.mindbright.application.ModuleTerminal"},
        {"module0.label", "Telnet Terminal"},
        {"module1.label", "SFTP File Transfer..."},
        {"module2.label", "SCP File Transfer..."},
        {"module3.label", "FTP To SFTP Bridge..."},
        {"module4.label", "SOCKS Proxy..."},
        {"module5.label", "Telnet Proxy..."},
        {"module.telnet.inhibit", "false"},
        {"module.telnet.havemenus", "true"},
        {"module.terminal.havemenus", "true"},
        {"module.scp.cwd-local", null},
        {"module.sftp.cwd-local", null},
        {"menu.modules.disable", "false"},
        {"menu.tunnels.disable", "false"},};

    static {
        for (int i = 0; i < defaultPropDesc.length; i++) {
            String name = defaultPropDesc[i][PROP_NAME];
            String value = defaultPropDesc[i][PROP_VALUE];
            if (value != null) {
                defaultProperties.put(name, value);
            }
            defaultPropNames.put(name, "");
        }
        oldPropNames.put("realsrv", "real-server");
        oldPropNames.put("localhst", "local-bind");
        oldPropNames.put("usrname", "username");
        oldPropNames.put("passwd", "password");
        oldPropNames.put("rsapassword", "passphrase");
        oldPropNames.put("proxytype", "proxy-type");
        oldPropNames.put("proxyhost", "proxy-host");
        oldPropNames.put("proxyport", "proxy-port");
        oldPropNames.put("proxyuser", "proxy-user");
        oldPropNames.put("prxpassword", "proxy-password");
        oldPropNames.put("cipher", "ssh1-cipher");
        oldPropNames.put("authtyp", "auth-method");
        oldPropNames.put("idfile", "private-key");
        oldPropNames.put("x11fwd", "x11-forward");
        oldPropNames.put("forcpty", "force-pty");
        oldPropNames.put("stricthostid", "strict-hostid");
        oldPropNames.put("display", "x11-display");
    }

    public static String backwardCompatProp(String key) {
        String newName = oldPropNames.get(key);
        if (newName != null) {
            key = newName;
        }
        return key;
    }

    public static void setAsDefault(Properties props) {
        Enumeration<?> e = props.keys();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            String value = props.getProperty(name);
            name = backwardCompatProp(name);
            defaultProperties.put(name, value);
        }
    }
    String sshHomeDir;
    String knownHosts;
    SSHRSAKeyFile keyFile;
    SSHInteractiveClient client;
    SSHInteractor interactor;
    boolean activeProps;
    private EncryptedProperties props;
    protected String currentPropsFile;
    protected String currentAlias;
    boolean autoSaveProps;
    boolean autoLoadProps;
    boolean savePasswords;
    boolean readonly;
    private String propertyPassword;
    public Properties initTermProps;
    protected boolean propsChanged;
    //
    private String host = "10.20.65.30";
    private int port = 22;
    private String username = "root";
    private String password = "Keqisoft1";

    public SSHPropertyHandler() {
        init(new Properties(), false);
    }

    public SSHPropertyHandler(String host, String user, String port, String password) {
        this.host = host;
        this.port = Integer.parseInt(port);
        this.username = user;
        this.password = password;
        init(new Properties(), false);
    }

    public SSHPropertyHandler(Properties initProps, boolean setAsDefault) {
        init(initProps, setAsDefault);
    }

    public SSHPropertyHandler(SSHPropertyHandler clone) {
        this(clone.props, false);
        this.sshHomeDir = clone.sshHomeDir;
        this.keyFile = clone.keyFile;
        this.initTermProps = clone.initTermProps;
        this.propertyPassword = clone.propertyPassword;
        this.readonly = true;
    }

    public static SSHPropertyHandler fromFile(String fileName, String password) throws IOException {
        SSHPropertyHandler fileProps = new SSHPropertyHandler();
        fileProps.setPropertyPassword(password);
        fileProps.loadAbsoluteFile(fileName, false);

        setAsDefault(fileProps.props);

        return fileProps;
    }

    public void setInteractor(SSHInteractor interactor) {
        this.interactor = interactor;
    }

    public void setClient(SSHInteractiveClient client) {
        this.client = client;
    }

    public void setAutoLoadProps(boolean value) {
        if (sshHomeDir != null) {
            autoLoadProps = false;
        }
    }

    public void setAutoSaveProps(boolean value) {
        if (sshHomeDir != null) {
            autoSaveProps = false;
        }
    }

    public void setSavePasswords(boolean value) {
        savePasswords = false;
    }

    public void setReadOnly(boolean value) {
        readonly = value;
    }

    public boolean isReadOnly() {
        return readonly;
    }

    public void setPropertyPassword(String password) {
        if (password != null) {
            this.propertyPassword = password;
        }
    }

    public boolean emptyPropertyPassword() {
        return propertyPassword == null;
    }

    public boolean setSSHHomeDir(String sshHomeDir) {
        if (sshHomeDir == null || sshHomeDir.trim().length() == 0) {
            return true;
        }

        if (!sshHomeDir.endsWith(File.separator)) {
            sshHomeDir += File.separator;
        }

        try {
            // sshHomeDir always ends with a trailing
            // File.separator. Strip before we try to create it (some
            // platforms don't like ending 'separator' in name)
            File sshDir =
                    new File(sshHomeDir.substring(0, sshHomeDir.length() - 1));
            if (!sshDir.exists()) {

                ByteArrayOutputStream baos =
                        readResource("/defaults/license.txt");
                if (null == baos || !interactor.licenseDialog(baos.toString())) {
                    return false;
                }

                if (interactor.askConfirmation(
                        "MindTerm home directory: '" + sshHomeDir
                        + "' does not exist, create it?", true)) {
                    try {
                        sshDir.mkdir();
                    } catch (Throwable t) {
                        interactor.alert("Could not create home directory, "
                                + "file operations disabled.");
                        sshHomeDir = null;
                    }
                } else {
                    interactor.report(
                            "No home directory, file operations disabled.");
                    sshHomeDir = null;
                }
            }
        } catch (Throwable t) {
            if (interactor != null && interactor.isVerbose()) {
                interactor.report("Can't access local file system, "
                        + "file operations disabled.");
            }
            sshHomeDir = null;
        }
        this.sshHomeDir = sshHomeDir;
        if (this.sshHomeDir == null) {
            autoSaveProps = false;
            autoLoadProps = false;
        }

        if (interactor != null) {
            interactor.propsStateChanged(this);
        }

        return true;
    }

    public String getSSHHomeDir() {
        return sshHomeDir;
    }

    public boolean hasHomeDir() {
        return sshHomeDir != null;
    }

    //
    // Methods delegated to Properties and other property related methods
    //
    public void resetToDefaults() {
        clearServerSetting();
        clearAllForwards();
        Enumeration<String> e = defaultPropNames.keys();
        while (e.hasMoreElements()) {
            String name = e.nextElement();
            String value = defaultProperties.getProperty(name);
            if (value != null) {
                setProperty(name, value);
            } else {
                props.remove(name);
            }
        }
        TerminalWindow term = getTerminal();
        if (term != null) {
            term.resetToDefaults();
        }
    }

    public static boolean isProperty(String key) {
        key = backwardCompatProp(key);
        Properties ssh2Prefs = SSH2Preferences.getDefaultProperties();
        return defaultPropNames.containsKey(key)
                || (key.indexOf("local") == 0 && Character.isDigit(key.charAt(5)))
                || (key.indexOf("remote") == 0 && Character.isDigit(key.charAt(6)))
                || (key.indexOf("module") == 0)
                || ssh2Prefs.containsKey(key)
                || key.startsWith("fingerprint")
                || key.startsWith(SSH2Preferences.SOCK_OPT);
    }

    public String getProperty(String key) {
        key = backwardCompatProp(key);
        return props.getProperty(key);
    }

    public boolean getPropertyB(String key) {
        key = getProperty(key);
        return Boolean.valueOf(key).booleanValue();
    }

    public int getPropertyI(String key) {
        key = getProperty(key);
        return Integer.valueOf(key).intValue();
    }

    public String getDefaultProperty(String key) {
        key = backwardCompatProp(key);
        return (String) defaultProperties.get(key);
    }

    public void setDefaultProperty(String key, String value) {
        key = backwardCompatProp(key);
        defaultProperties.put(key, value);
    }

    public void resetProperty(String key) {
        key = backwardCompatProp(key);
        setProperty(key, getDefaultProperty(key));
    }

    public void eraseProperty(String key) {
        key = backwardCompatProp(key);
        props.remove(key);
    }

    public void setProperty(String key, boolean value)
            throws IllegalArgumentException, NoSuchElementException {
        setProperty(key, String.valueOf(value));
    }

    public void setProperty(String key, int value)
            throws IllegalArgumentException, NoSuchElementException {
        setProperty(key, String.valueOf(value));
    }

    public void setProperty(String key, String value)
            throws IllegalArgumentException, NoSuchElementException {

        if (value == null) {
            return;
        }

        key = backwardCompatProp(key);

        boolean equalProp = !(value.equals(getProperty(key)));

        validateProperty(key, value);

        if (activeProps) {
            activateProperty(key, value);
        }

        if (equalProp) {
            if (interactor != null) {
                interactor.propsStateChanged(this);
            }
            propsChanged = equalProp;
        }

        props.put(key, value);
    }

    final void validateProperty(String key, String value)
            throws IllegalArgumentException, NoSuchElementException {
        //
        // Some sanity checks...
        //
        if (key.equals("auth-method")) {
            SSH.getAuthTypes(value);
            //
        } else if (key.equals("x11-forward")
                || key.equals("force-pty") || key.equals("remfwd")
                || key.equals("strict-hostid") || key.equals("portftp")
                || key.equals("key-timing-noise")) {
            if (!(value.equals("true") || value.equals("false"))) {
                throw new IllegalArgumentException("Value for " + key + " must be 'true' or 'false'");
            }
            //
        } else if (key.equals("port") || key.equals("proxy-port") || key.equals("mtu")
                || key.equals("alive") || key.equals("compression")) {
            try {
                int val = Integer.valueOf(value).intValue();
                if ((key.equals("port") || key.equals("proxy-port")) && (val > 65535 || val < 0)) {
                    throw new IllegalArgumentException("Not a valid port number: " + value);
                } else if (key.equals("mtu") && val != 0 && (val > (256 * 1024) || val < 4096)) {
                    throw new IllegalArgumentException("Mtu must be between 4k and 256k");
                } else if (key.equals("alive")) {
                    if (val < 0 || val > 600) {
                        throw new IllegalArgumentException("Alive interval must be 0-600");
                    }
                } else if (key.equals("compression")) {
                    if (val < 0 || val > 9) {
                        throw new IllegalArgumentException("Compression Level must be 0-9");
                    }
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Value for " + key + " must be an integer");
            }
            //
        } else if (key.equals("server")) {
            if (client != null && client.isOpened()) {
                throw new IllegalArgumentException("Server can only be set while not connected");
            }
        } else if (key.equals("real-server") || key.equals("local-bind")) {
            try {
                InetAddress.getByName(value);
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException(key + " address must be a legal/known host name");
            }
        } else if (key.equals("proxy-type")) {
            SSH.getProxyType(value);
        } else if (key.startsWith("local") || key.startsWith("remote")) {
            try {
                if (value.startsWith("/general/")) {
                    value = value.substring(9);
                }
                if (key.startsWith("local")) {
                    addLocalPortForward(value, false);
                } else {
                    addRemotePortForward(value, false);
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Not a valid port forward: " + key + " : " + value);
            }
        } else if (!isProperty(key)) {
            throw new NoSuchElementException("Unknown ssh property '" + key + "'");
        }
    }

    void activateProperty(String key, String value) {
        //
        // The properties that needs an action to "activated"
        //
        if (key.equals("remfwd")) {
            try {
                SSHListenChannel.setAllowRemoteConnect(Boolean.parseBoolean(value));
            } catch (Throwable t) {
                // Ignore if we don't have the SSHListenChannel class
            }
        } else if (key.equals("portftp")) {
            client.havePORTFtp = Boolean.parseBoolean(value);
            if (client.havePORTFtp && SSHProtocolPlugin.getPlugin("ftp") != null) {
                SSHProtocolPlugin.getPlugin("ftp").initiate(client);
            }
            //
        } else if (key.equals("key-timing-noise")) {
            if (client.isSSH2 && client.termAdapter != null) {
                if ("true".equals(value)) {
                    client.termAdapter.startChaff();
                } else {
                    client.termAdapter.stopChaff();
                }
            }
        } else if (key.equals("alive")) {
            if (client.isConnected()) {
                client.setAliveInterval(Integer.parseInt(value));
            }
        } else if (key.equals("real-server")) {
            try {
                if (value != null && value.length() > 0) {
                    client.setServerRealAddr(InetAddress.getByName(value));
                } else {
                    client.setServerRealAddr(null);
                }
            } catch (UnknownHostException e) {
                // !!!
            }
        } else if (key.equals("local-bind")) {
            try {
                client.setLocalAddr(value);
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException("localhost address must be a legal/known host name");
            }
        } else if (key.startsWith("local")) {
            int n = Integer.parseInt(key.substring(5));
            if (n > client.localForwards.size()) {
                throw new IllegalArgumentException("Port forwards must be given in unbroken sequence");
            }
            if (value.startsWith("/general/")) {
                value = value.substring(9);
            }
            try {
                addLocalPortForward(value, true);
            } catch (IOException e) {
                if (!interactor.askConfirmation("Error setting up tunnel '"
                        + value + "', continue anyway?",
                        true)) {
                    throw new IllegalArgumentException("Error creating tunnel: " + e.getMessage());
                }
            }
        } else if (key.startsWith("remote")) {
            try {
                int n = Integer.parseInt(key.substring(6));
                if (n > client.remoteForwards.size()) {
                    throw new IllegalArgumentException("Port forwards must be given in unbroken sequence");
                }
                if (value.startsWith("/general/")) {
                    value = value.substring(9);
                }
                addRemotePortForward(value, true);
            } catch (Exception e) {
                throw new IllegalArgumentException("Not a valid port forward: " + key + " : " + value);
            }
        }
    }

    public void setProperties(Properties newProps) throws IllegalArgumentException,
            NoSuchElementException {
        props = new EncryptedProperties(defaultProperties);
        mergeProperties(newProps);
    }

    public Properties getProperties() {
        return props;
    }

    public void mergeProperties(Properties newProps)
            throws IllegalArgumentException {
        String name, value;
        Enumeration<String> e = defaultPropNames.keys();
        while (e.hasMoreElements()) {
            name = e.nextElement();
            value = newProps.getProperty(name);
            if (value != null) {
                name = backwardCompatProp(name);
                props.put(name, value);
            }
        }
        int i = 0;
        while ((value = newProps.getProperty("local" + i)) != null) {
            props.put("local" + i, value);
            i++;
        }
        i = 0;
        while ((value = newProps.getProperty("remote" + i)) != null) {
            props.put("remote" + i, value);
            i++;
        }
    }

    public Properties getInitTerminalProperties() {
        return initTermProps;
    }

    public void activateProperties() {
        if (activeProps) {
            return;
        }

        String name, value;
        Enumeration<String> e = defaultPropNames.keys();

        activeProps = true;

        while (e.hasMoreElements()) {
            name = e.nextElement();
            value = props.getProperty(name);
            if (value != null) {
                activateProperty(name, value);
            }
        }
        int i = 0;
        while ((value = props.getProperty("local" + i)) != null) {
            activateProperty("local" + i, value);
            i++;
        }
        i = 0;
        while ((value = props.getProperty("remote" + i)) != null) {
            activateProperty("remote" + i, value);
            i++;
        }
    }

    public void passivateProperties() {
        activeProps = false;
    }

    private void saveProperties(String fname) throws IOException {
        FileOutputStream f;
        TerminalWindow term = getTerminal();
        Properties termProps = (term != null ? term.getProperties() : null);
        if (termProps != null) {
            Enumeration<Object> e = termProps.keys();
            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                String val = termProps.getProperty(key);
                props.put(key, val);
            }
        }

        f = new FileOutputStream(fname);

        try {
            if (savePasswords) {
                if (propertyPassword == null) {
                    propertyPassword = "";
                }
                // TODO: should take default cipher from defaultProperties
                props.save(f, "MindTerm ssh settings",
                        propertyPassword, SSH.cipherClasses[SSH.CIPHER_DEFAULT][0]);
            } else {
                String prxPwd, stdPwd, tisPwd, rsaPwd;
                stdPwd = props.getProperty("password");
                prxPwd = props.getProperty("proxy-password");
                tisPwd = props.getProperty("tispassword");
                rsaPwd = props.getProperty("passphrase");
                clearPasswords();
                props.store(f, "MindTerm ssh settings");
                if (stdPwd != null) {
                    props.put("password", stdPwd);
                }
                if (prxPwd != null) {
                    props.put("proxy-password", prxPwd);
                }
                if (tisPwd != null) {
                    props.put("tispassword", tisPwd);
                }
                if (rsaPwd != null) {
                    props.put("passphrase", rsaPwd);
                }
            }
        } finally {
            f.close();
        }

        propsChanged = false;
        if (term != null) {
            term.setPropsChanged(false);
        }

        interactor.propsStateChanged(this);
    }

    private void loadProperties(String fname, boolean promptPwd)
            throws IOException {
        TerminalWindow term = getTerminal();
        ByteArrayInputStream bytein;
        FileInputStream f = new FileInputStream(fname);
        try {
            byte[] bytes = new byte[f.available()];
            f.read(bytes);
            bytein = new ByteArrayInputStream(bytes);
        } finally {
            f.close();
        }

        EncryptedProperties loadProps = new EncryptedProperties();

        try {
            loadProps.load(bytein, "");
        } catch (SSHAccessDeniedException e) {
            try {
                bytein.reset();
                loadProps.load(bytein, propertyPassword);
            } catch (SSHAccessDeniedException ee) {
                try {
                    if (promptPwd) {
                        bytein.reset();
                        propertyPassword = interactor.promptPassword(
                                "File " + fname + " password: ");
                        loadProps.load(bytein, propertyPassword);
                    } else {
                        throw new SSHAccessDeniedException("");
                    }
                } catch (SSHAccessDeniedException eee) {
                    clearServerSetting();
                    throw new SSHClient.AuthFailException(
                            "Access denied for '" + fname + "'");
                }
            }
        }

        savePasswords = !loadProps.isNormalPropsFile();

        String name;
        String value;

        Properties sshProps = new Properties();
        Properties termProps = new Properties();

        Enumeration<?> e = loadProps.keys();
        while (e.hasMoreElements()) {
            name = (String) e.nextElement();
            value = loadProps.getProperty(name);

            if (isProperty(name)) {
                name = backwardCompatProp(name);
                sshProps.put(name, value);
            } else if (TerminalWin.isProperty(name)) {
                name = TerminalDefProps.backwardCompatProp(name);
                termProps.put(name, value);
            } else {
                if (interactor != null) {
                    interactor.report("Unknown property '" + name + "' found in file: " + fname);
                } else {
                    System.out.println("Unknown property '" + name + "' found in file: " + fname);
                }
            }
        }

        if (client != null) {
            client.clearAllForwards();
        }

        passivateProperties();

        setProperties(sshProps);

        initTermProps = termProps;

        if (term != null) {
            term.setProperties(initTermProps, false);
            term.setPropsChanged(false);
        }

        propsChanged = false;
        if (interactor != null) {
            interactor.propsStateChanged(this);
        }
    }

    final void clearPasswords() {
        props.remove("password");
        props.remove("tispassword");
        props.remove("passphrase");
        props.remove("proxy-password");
    }

    final void clearServerSetting() {
        setProperty("server", "");
        currentPropsFile = null;
        currentAlias = null;
        if (interactor != null) {
            interactor.propsStateChanged(this);
        }
    }

    final void clearAllForwards() {
        int i = 0;
        if (client != null) {
            client.clearAllForwards();
        }
        for (i = 0; i < 1024; i++) {
            String key = "local" + i;
            if (!props.containsKey(key)) {
                break;
            }
            props.remove(key);
        }
        for (i = 0; i < 1024; i++) {
            String key = "remote" + i;
            if (!props.containsKey(key)) {
                break;
            }
            props.remove(key);
        }
    }

    public boolean wantSave() {
        boolean somePropsChanged = (propsChanged
                || (getTerminal() != null
                ? getTerminal().getPropsChanged() : false));
        return (!isReadOnly() && somePropsChanged && sshHomeDir != null);
    }

    public final void checkSave() throws IOException {
        if (autoSaveProps) {
            saveCurrentFile();
        }
    }

    public void saveCurrentFile() throws IOException {
        if (currentPropsFile != null && wantSave()) {
            saveProperties(currentPropsFile);
        }
    }

    public void saveAsCurrentFile(String fileName) throws IOException {
        propsChanged = true;
        currentPropsFile = fileName;
        saveCurrentFile();
        currentAlias = null;
    }

    public void loadAbsoluteFile(String fileName, boolean promptPwd)
            throws IOException {
        currentAlias = null;
        currentPropsFile = fileName;

        loadProperties(currentPropsFile, promptPwd);
        if (interactor != null) {
            interactor.propsStateChanged(this);
        }
    }

    public void setAlias(String alias) {
        if (sshHomeDir == null) {
            return;
        }
        currentAlias = alias;
        currentPropsFile = sshHomeDir + alias + PROPS_FILE_EXT;
    }

    public String getAlias() {
        return currentAlias;
    }

    public void loadAliasFile(String alias, boolean promptPwd)
            throws IOException {
        String oldAlias = currentAlias;
        setAlias(alias);
        if (oldAlias == null || !oldAlias.equals(alias)) {
            loadProperties(currentPropsFile, promptPwd);
        }
    }

    public String[] availableAliases() {
        if (sshHomeDir == null) {
            return null;
        }

        // sshHomeDir always ends with a trailing
        // File.separator. Strip before we try to create it (some
        // platforms don't like ending 'separator' in name)
        File dir = new File(sshHomeDir.substring(0, sshHomeDir.length() - 1));
        String[] list, alist;
        int i, cnt = 0, lastIndex = 0;
        long last = 0;

        list = dir.list();
        if (list == null) {
            return null;
        }

        for (i = 0; i < list.length; i++) {
            if (!list[i].endsWith(PROPS_FILE_EXT)) {
                list[i] = null;
                cnt++;
            } else {
                File f = new File(sshHomeDir, list[i]);
                if (0 == last || f.lastModified() > last) {
                    last = f.lastModified();
                    lastIndex = i;
                }
            }
        }
        if (lastIndex != 0) {
            String t = list[lastIndex];
            list[lastIndex] = list[0];
            list[0] = t;
        }
        if (cnt == list.length) {
            return null;
        }
        alist = new String[list.length - cnt];
        cnt = 0;
        for (i = 0; i < list.length; i++) {
            if (list[i] != null) {
                int pi = list[i].lastIndexOf(PROPS_FILE_EXT);
                alist[cnt++] = list[i].substring(0, pi);
            }
        }

        return alist;
    }

    public boolean isAlias(String alias) {
        String[] aliases = availableAliases();
        boolean isAlias = false;
        if (aliases != null) {
            for (int i = 0; i < aliases.length; i++) {
                if (alias.equals(aliases[i])) {
                    isAlias = true;
                    break;
                }
            }
        }
        return isAlias;
    }

    public boolean isAbsolutFile(String fileName) {
        if (sshHomeDir == null) {
            return false;
        }

        File file = new File(fileName);
        return (file.isFile() && file.exists());
    }

    public TerminalWindow getTerminal() {
        if (client == null || client.console == null) {
            return null;
        }
        TerminalWindow term = client.console.getTerminal();
        return term;
    }

    public void removeLocalTunnelAt(int idx, boolean kill) {
        int i, sz = client.localForwards.size();
        props.remove("local" + idx);
        for (i = idx; i < sz - 1; i++) {
            props.put("local" + i, props.get("local" + (i + 1)));
            props.remove("local" + (i + 1));
        }
        propsChanged = true;
        if (kill) {
            SSHClient.LocalForward fwd = client.localForwards.elementAt(idx);
            client.delLocalPortForward(fwd.localHost, fwd.localPort);
        } else {
            client.localForwards.removeElementAt(idx);
        }
    }

    public void removeRemoteTunnelAt(int idx) {
        int i, sz = client.remoteForwards.size();
        props.remove("remote" + idx);
        for (i = idx; i < sz - 1; i++) {
            props.put("remote" + i, props.get("remote" + (i + 1)));
            props.remove("remote" + (i + 1));
        }
        propsChanged = true;
        if (client.isSSH2) {
            SSHClient.RemoteForward fwd = client.remoteForwards.elementAt(idx);
            if (fwd != null) {
                client.delRemotePortForward(fwd.remoteHost, fwd.remotePort);
            }
        } else {
            client.remoteForwards.removeElementAt(idx);
        }
    }

    public void addLocalPortForward(String fwdSpec, boolean commit)
            throws IllegalArgumentException, IOException {
        Object[] components = Util.parseForwardSpec(
                fwdSpec, client.getLocalAddr().getHostAddress());
        if (commit) {
            client.addLocalPortForward((String) components[1],
                    ((Integer) components[2]).intValue(),
                    (String) components[3],
                    ((Integer) components[4]).intValue(),
                    (String) components[0]);
        }
    }

    public void addRemotePortForward(String fwdSpec, boolean commit)
            throws IllegalArgumentException {
        Object[] components = Util.parseForwardSpec(
                fwdSpec, client.getLocalAddr().getHostAddress());
        if (commit) {
            client.addRemotePortForward((String) components[1],
                    ((Integer) components[2]).intValue(),
                    (String) components[3],
                    ((Integer) components[4]).intValue(),
                    (String) components[0]);
        }
    }

    //
    // SSHAuthenticator interface
    //
    public String getUsername(SSHClientUser origin) throws IOException {
        return this.username;
    }

    public String getPassword(SSHClientUser origin) throws IOException {
        return this.password;
    }

    public String getChallengeResponse(SSHClientUser origin, String challenge) throws IOException {
        String tisPassword = getProperty("tispassword");
        if (tisPassword == null) {
            tisPassword = interactor.promptPassword(challenge);
            setProperty("tispassword", tisPassword);
        }
        return tisPassword;
    }

    public int[] getAuthTypes(SSHClientUser origin) {
        return SSH.getAuthTypes(getProperty("auth-method"));
    }

    public int getCipher(SSHClientUser origin) {
        int cipher = SSH.getCipherType(getProperty("ssh1-cipher"));
        if (cipher == SSH.CIPHER_NOTSUPPORTED) {
            interactor.report("Cipher '" + getProperty("ssh1-cipher")
                    + "' not supported in ssh1, using default");
            resetProperty("ssh1-cipher");
        }
        return SSH.getCipherType(getProperty("ssh1-cipher"));
    }

    public SSHRSAKeyFile getIdentityFile(SSHClientUser origin) throws IOException {
        String idFile = getProperty("private-key");
        if (idFile.indexOf(File.separator) == -1) {
            idFile = sshHomeDir + idFile;
        }

        keyFile = new SSHRSAKeyFile(idFile);
        return keyFile;
    }

    public String getIdentityPassword(SSHClientUser origin) throws IOException {
        String rsaPassword = getProperty("passphrase");
        if (rsaPassword == null) {
            rsaPassword = interactor.promptPassword("key file '" + keyFile.getComment()
                    + "' password: ");
            setProperty("passphrase", rsaPassword);
        }
        return rsaPassword;
    }

    public String getIdentityPassword(String prompt)
            throws IOException {
        String rsaPassword = getProperty("passphrase");
        if (rsaPassword == null) {
            rsaPassword = interactor.promptPassword(prompt);
            setProperty("passphrase", rsaPassword);
        }
        return rsaPassword;
    }

    // !!! TODO Make SSHHostKeyVerify which can do both ssh1 and ssh2
    // !!! verifyHostKey(PublicKey key, byte[] keyBlob, String type)
    //
    public boolean verifyKnownHosts(RSAPublicKey hostPub) throws IOException {
        File tmpFile;
        String fileName = null;
        InputStream knownHostsIn = null;
        int hostCheck = 0;
        boolean confirm = true;
        boolean strict = strictHostKeyCheck();

        byte[] rawN = hostPub.getModulus().toByteArray();
        byte[] rawE = hostPub.getPublicExponent().toByteArray();
        int nCutZero = ((rawN[0] == 0) ? 1 : 0);
        int eCutZero = ((rawE[0] == 0) ? 1 : 0);

        byte[] blob = new byte[rawN.length + rawE.length - nCutZero - eCutZero];
        System.arraycopy(rawN, nCutZero, blob, 0, rawN.length - nCutZero);
        System.arraycopy(rawE, eCutZero, blob, rawN.length - nCutZero,
                rawE.length - eCutZero);
        showFingerprint(blob, "rsa1");

        SSHRSAPublicKeyFile file = null;

        knownHostsIn = this.getClass().getResourceAsStream("/defaults/known_hosts.txt");

        try {
            boolean tryingResource = true;
            while (tryingResource) {
                if (knownHostsIn != null) {
                    fileName = "<resource>/defaults/known_hosts.txt";
                    if (interactor.isVerbose()) {
                        interactor.report("Found preinstalled 'known_hosts' file.");
                    }
                } else {
                    tryingResource = false;
                    if (sshHomeDir == null && !strict) {
//                        if (interactor.isVerbose()) {
//                            interactor.report("File operations disabled, server identity can't be verified");
//                        }
                        return true;
                    }

                    fileName = sshHomeDir + knownHosts;
                    tmpFile = new File(fileName);

                    if (!tmpFile.exists()) {
                        if (interactor.askConfirmation("File '" + fileName + "' not found, create it?", true)) {
                            FileOutputStream f = new FileOutputStream(tmpFile);
                            f.close();
                        } else if (!strict) {
                            interactor.report("Verification of server key disabled in this session.");
                            return true;
                        }
                    }

                    knownHostsIn = new FileInputStream(fileName);
                }

                file = new SSHRSAPublicKeyFile(knownHostsIn, fileName, true);

                if ((hostCheck = file.checkPublic(hostPub.getModulus(), getProperty("server")))
                        == SSH.SRV_HOSTKEY_KNOWN) {
                    return true;
                }

                if (tryingResource) {
                    if (!interactor.askConfirmation("Host was not found in preinstalled 'known_hosts' file! Continue anyway?", false)) {
                        return false;
                    }
                }

                knownHostsIn = null;
            }

            if (strict) {
                strictHostFailed();
                return false;
            }

            if (hostCheck == SSH.SRV_HOSTKEY_NEW) {
                if (!askSaveKeyConfirmation(fileName)) {
                    return true;
                }
                confirm = true;
            } else {
                confirm = askChangeKeyConfirmation();
                file.removePublic(getProperty("server"));
            }

            if (confirm) {
                file.addPublic(getProperty("server"), null,
                        hostPub.getPublicExponent(),
                        hostPub.getModulus());
                tmpFile = new File(fileName + ".tmp");
                File oldFile = new File(fileName);
                oldFile.renameTo(tmpFile);
                try {
                    file.saveToFile(fileName);
                } catch (IOException e) {
                    oldFile = new File(fileName);
                    tmpFile.renameTo(oldFile);
                    throw e;
                }
                tmpFile.delete();
            } else {
                return false;
            }
        } finally {
            try {
                knownHostsIn.close();
            } catch (Exception e) {
            }
        }

        return true;
    }

    public boolean verifyKnownSSH2Hosts(SSHInteractiveClient cli,
            SSH2Signature serverHostKey)
            throws IOException, SSH2Exception {
        File tmpFile;
        String fileName = null;
        boolean strict = strictHostKeyCheck();

        if (!strict && sshHomeDir == null) {
//            if (interactor.isVerbose()) {
//                interactor.report("File operations disabled, server identity can't be verified");
//            }
            return true;
        }

        fileName = sshHomeDir + "hostkeys";
        tmpFile = new File(fileName);

        if (!strict && !tmpFile.exists()) {
            if (interactor.askConfirmation("Known hosts directory: '"
                    + fileName
                    + "' does not exist, create it?", true)) {
                try {
                    tmpFile.mkdir();
                } catch (Throwable t) {
                    interactor.alert("Could not create known hosts directory.");
                }
            }
        }

        if (!strict && (!tmpFile.exists() || !tmpFile.isDirectory())) {
            return interactor.askConfirmation("No hostkeys directory, can't verify host, continue anyway?", false);
        }

        fileName += File.separator
                + "key_" + getProperty("port") + "_" + getProperty("server") + ".pub";
        tmpFile = new File(fileName);

        if (!tmpFile.exists()) {
            if (strict) {
                strictHostFailed();
                return false;
            }
            if (!askSaveKeyConfirmation(fileName)) {
                return true;
            }
        } else {
            SSH2PublicKeyFile pkif = new SSH2PublicKeyFile();
            pkif.load(fileName);

            if (pkif.sameAs(serverHostKey.getPublicKey())) {
                return true;
            }

            if (!askChangeKeyConfirmation()) {
                return false;
            }

            tmpFile.delete();
        }

        String user = getProperty("username");
        // !!! OUCH
        if (user == null) {
            user = SSH.VER_MINDTERM;
        }

        SSH2PublicKeyFile pkif =
                new SSH2PublicKeyFile(serverHostKey.getPublicKey(),
                user,
                "\"host key for " + getProperty("server")
                + ", accepted by " + user + " "
                + (new Date()) + "\"");
        pkif.store(fileName);

        return true;
    }

    boolean strictHostKeyCheck() {
        return getPropertyB("strict-hostid");
    }

    void strictHostFailed() {
        interactor.report("Strict host key checking enabled, please add host key.");
    }

    boolean hasKeyTimingNoise() {
        return getPropertyB("key-timing-noise");
    }

    boolean askSaveKeyConfirmation(String fileName) {
//        if (interactor.isVerbose()) {
//            interactor.report("Host key not found in '" + fileName + "'");
//        }
//        if (!interactor.askConfirmation("Do you want to add this host to your set of known hosts (check fingerprint)", true)) {
//            interactor.report("Verification of server key disabled in this session.");
//            return false;
//        }
//        return true;
        return false;
    }

    boolean askChangeKeyConfirmation() {
//        interactor.alert("WARNING: HOST IDENTIFICATION HAS CHANGED! "
//                + "IT IS POSSIBLE THAT SOMEONE IS DOING SOMETHING NASTY, "
//                + "ONLY PROCEED IF YOU KNOW WHAT YOU ARE DOING!");
//        return interactor.askConfirmation("Do you want to replace the identification of this host?",
//                false);
        return true;
    }

    void showFingerprint(byte[] blob, String type) {
//        StringBuilder msg = new StringBuilder();
//
//        msg.append("\r\nServer's hostkey (" + type + ") fingerprint:\r\n");
//        if (!com.mindbright.util.Crypto.isFipsMode()) {
//            msg.append("openssh md5:  ");
//            msg.append(SSH2KeyFingerprint.md5Hex(blob));
//        }
//        msg.append("\r\nbubblebabble: ");
//        msg.append(SSH2KeyFingerprint.bubbleBabble(blob));
//
//        interactor.report(msg.toString());
    }

    //
    // ProxyAuthenticator interface
    //
    public String getProxyUsername(String type, String challenge) throws IOException {
        String username = getProperty("proxy-user");
        if (interactor != null
                && (!interactor.quietPrompts()
                || (username == null || username.equals("")))) {
            String chStr = (challenge != null ? (" '" + challenge + "'") : "");
            String tmp = null;
            while (tmp == null) {
                tmp = interactor.promptLine(type + chStr + " username: ",
                        username);
            }
            username = tmp;
            setProperty("proxy-user", username);
        }
        return username;
    }

    public String getProxyPassword(String type, String challenge) throws IOException {
        String prxPassword = getProperty("proxy-password");
        if (prxPassword == null && interactor != null) {
            String chStr = (challenge != null ? (" '" + challenge + "'") : "");
            prxPassword = interactor.promptPassword(type + chStr + " password: ");
            setProperty("proxy-password", prxPassword);
        }
        return prxPassword;
    }
    //
    // SSHClientUser interface
    //
    boolean kludgeSrvPrompt;

    public String getSrvHost() throws IOException {
        return this.host;
    }

    public int getSrvPort() {
        return this.port;
    }

    public NetworkConnection getProxyConnection() throws IOException {
        NetworkConnection proxyConn = null;
        String proxyType = getProperty("proxy-type");
        int proxyTypeId = SSH.PROXY_NONE;

        try {
            proxyTypeId = SSH.getProxyType(proxyType);
        } catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage());
        }

        if (proxyTypeId == SSH.PROXY_NONE) {
            return null;
        }

        String prxHost = getProperty("proxy-host");
        int prxPort = -1;

        try {
            prxPort = Integer.valueOf(getProperty("proxy-port")).intValue();
        } catch (Exception e) {
            prxPort = -1;
        }

        if (prxHost == null || prxPort == -1) {
            throw new IOException("When 'proxytype' is set, 'proxyhost' and 'proxyport' must also be set");
        }

        String sshHost = getProperty("server");
        int sshPort = getSrvPort();
        String prxProt = getProperty("proxyproto");

        long prxTimeout = getPropertyI("proxy-timeout") * 1000;

        if (SSH.DEBUG) {
            System.out.println("Connecting to " + proxyType + " proxy "
                    + prxHost + ":" + prxPort);
        }
        switch (proxyTypeId) {
            case SSH.PROXY_HTTP:
                proxyConn = WebProxyTunnelSocket.getProxy(
                        sshHost, sshPort, prxHost, prxPort, prxTimeout, prxProt,
                        this, "MindTerm/" + Version.version);
                break;
            case SSH.PROXY_SOCKS4:
                proxyConn = SocksProxySocket.getSocks4Proxy(sshHost, sshPort, prxHost, prxPort, prxTimeout,
                        getProxyUsername("SOCKS4", null));
                break;
            case SSH.PROXY_SOCKS5_DNS:
                proxyConn = SocksProxySocket.getSocks5Proxy(sshHost, sshPort, prxHost, prxPort, prxTimeout, false, this);
                break;
            case SSH.PROXY_SOCKS5_IP:
                proxyConn = SocksProxySocket.getSocks5Proxy(sshHost, sshPort, prxHost, prxPort, prxTimeout, true, this);
                break;
        }

        return proxyConn;
    }

    public ByteArrayOutputStream readResource(String name) {
        InputStream in = getClass().getResourceAsStream(name);
        ByteArrayOutputStream baos = null;
        if (in != null) {
            baos = new ByteArrayOutputStream(50000);
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

    public String getDisplay() {
        return getProperty("display");
    }

    public int getMaxPacketSz() {
        return getPropertyI("mtu");
    }

    public int getAliveInterval() {
        return getPropertyI("alive");
    }

    public int getCompressionLevel() {
        return getPropertyI("compression");
    }

    public int getConnectTimeout() {
        return getPropertyI("connect-timeout");
    }

    public int getHelloTimeout() {
        return getPropertyI("hello-timeout");
    }

    public int getKexTimeout() {
        return getPropertyI("kex-timeout");
    }

    public boolean wantX11Forward() {
        return getPropertyB("x11-forward");
    }

    public boolean wantPTY() {
        return getPropertyB("force-pty");
    }

    public SSHInteractor getInteractor() {
        return interactor;
    }

    private void init(Properties initProps, boolean setAsDefault) throws IllegalArgumentException, NoSuchElementException {
        this.knownHosts = SSH.KNOWN_HOSTS_FILE;
        SSH2Preferences p = new SSH2Preferences(com.mindbright.util.Crypto.isFipsMode());
        hostKeyAlgs = p.getPreference(SSH2Preferences.HOST_KEY_ALG);
        ciphAlgsSort = p.getPreference(SSH2Preferences.CIPHERS_C2S);
        macAlgs = p.getPreference(SSH2Preferences.MACS_C2S);

        defaultProperties.put("kex-algorithms", p.getPreference(SSH2Preferences.KEX_ALGORITHMS));
        defaultProperties.put("server-host-key-algorithms", hostKeyAlgs);
        defaultProperties.put("enc-algorithms-cli2srv", ciphAlgsSort);
        defaultProperties.put("enc-algorithms-srv2cli", ciphAlgsSort);
        defaultProperties.put("mac-algorithms-cli2srv", macAlgs);
        defaultProperties.put("mac-algorithms-src2cli", macAlgs);

        initProps.put("kex-algorithms", p.getPreference(SSH2Preferences.KEX_ALGORITHMS));
        initProps.put("server-host-key-algorithms", hostKeyAlgs);
        initProps.put("enc-algorithms-cli2srv", ciphAlgsSort);
        initProps.put("enc-algorithms-srv2cli", ciphAlgsSort);
        initProps.put("mac-algorithms-cli2srv", macAlgs);
        initProps.put("mac-algorithms-src2cli", macAlgs);

        if (setAsDefault) {
            setAsDefault(initProps);
        }
        //---
        initProps.put("server", this.host);
        initProps.put("port", this.port);
        initProps.put("username", this.username);
        if(this.password != null) {
            initProps.put("password", this.password);
        }
        setProperties(initProps);
        //---

        this.activeProps = false;
        this.propsChanged = false;
    }
}
