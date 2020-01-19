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

package com.mindbright.ssh2;

import java.io.IOException;


/**
 * This class implements the most basic variant of a ssh2 client. It
 * creates the userauth, and connection layers (i.e. instances of
 * <code>SSH2UserAuth</code>, and <code>SSH2Connection</code>). The
 * only thing which needs to be provided is an initialized
 * <code>SSH2Transport</code> instance, user authentication data. The
 * constructor is active in that it does all the required work to set
 * up the complete protocol stack, hence it can throw exceptions which
 * can occur.
 * <p>
 * This simple client can easily be used as the basis for example to build
 * tunneling capabilities into any java app. requiring secure connections. For
 * doing remote command execution and/or controlling input/output of a command
 * or shell the class <code>SSH2ConsoleRemote</code> can be used to have easy
 * access to command execution and/or input/output as
 * <code>java.io.InputStream</code> and <code>java.io.OutpuStream</code>
 *
 * @see SSH2Transport
 * @see SSH2Connection
 * @see SSH2ConsoleRemote
 * @see SSH2Preferences
 * @see examples.RunRemoteCommand
 * @see examples.RemoteShellScript
 */
public class SSH2SimpleClient {

    protected SSH2Transport  transport;
    protected SSH2Connection connection;
    protected SSH2UserAuth   userAuth;

    /**
     * Simple constructor to use for password authentication. This
     * will actually be able to use both the password and keyboard
     * interactive authentication protocols.
     *
     * @param transport connected transport layer
     * @param username  name of user
     * @param password  password of user
     * @param timeout   timeout in milliseconds for authentication
     * phase (0 means no timeout)
     *
     * @see SSH2Transport
     */
    public SSH2SimpleClient(SSH2Transport transport,
                            String username, String password, long timeout)
    throws SSH2Exception {
        SSH2Authenticator authenticator = new SSH2Authenticator(username);
        authenticator.addModule(new SSH2AuthPassword(password));
        authenticator.addModule(new SSH2AuthKbdInteract(password));
        init(transport, authenticator, timeout);
    }

    /**
     * Simple constructor to use for password authentication. This
     * will actually be able to use both the password and keyboard
     * interactive authentication protocols.
     *
     * @param transport connected transport layer
     * @param username  name of user
     * @param password  password of user
     *
     * @see SSH2Transport
     */
    public SSH2SimpleClient(SSH2Transport transport,
                            String username, String password)
    throws SSH2Exception {
        this(transport, username, password, 0);
    }

    /**
     * Simple constructor to use for publickey authentication.
     *
     * @param transport    connected transport layer
     * @param username     name of user
     * @param keyFile      name of private key file to use for authentication
     * @param keyPassword  password protecting private key file (null if none)
     *
     * @see SSH2Transport
     */
    public SSH2SimpleClient(SSH2Transport transport,
                            String username,
                            String keyFile, String keyPassword)
    throws SSH2Exception, IOException {
        this(transport, false, username, keyFile, keyPassword, 0);
    }

    /**
     * Simple constructor to use for publickey authentication.
     *
     * @param transport    connected transport layer
     * @param username     name of user
     * @param keyFile      name of private key file to use for authentication
     * @param keyPassword  password protecting private key file (null if none)
     * @param timeout      timeout in milliseconds for authentication phase (0 means no timeout)
     *
     * @see SSH2Transport
     */
    public SSH2SimpleClient(SSH2Transport transport,
                            String username,
                            String keyFile, String keyPassword,
                            long timeout)
    throws SSH2Exception, IOException {
        this(transport, false, username, keyFile, keyPassword, timeout);
    }

    /**
     * Simple constructor to use for publickey / hostbased authentication.
     *
     * @param transport    connected transport layer
     * @param hostbased    whether hostbased or publickey auth. should be used,
     *                     set to true to use hostbased, and false to use publickey
     * @param username     name of user
     * @param keyFile      name of private key file to use for authentication
     * @param keyPassword  password protecting private key file (null if none)
     *
     * @see SSH2Transport
     */
    public SSH2SimpleClient(SSH2Transport transport,
                            boolean hostbased,
                            String username,
                            String keyFile, String keyPassword)
    throws SSH2Exception, IOException {
        this(transport, hostbased, username, keyFile, keyPassword, 0);
    }

    /**
     * Simple constructor to use for publickey / hostbased authentication.
     *
     * @param transport    connected transport layer
     * @param hostbased    whether hostbased or publickey auth. should be used,
     *                     set to true to use hostbased, and false to use publickey
     * @param username     name of user
     * @param keyFile      name of private key file to use for authentication
     * @param keyPassword  password protecting private key file (null if none)
     * @param timeout      timeout in milliseconds for authentication phase (0 means no timeout)
     *
     * @see SSH2Transport
     */
    public SSH2SimpleClient(SSH2Transport transport,
                            boolean hostbased,
                            String username,
                            String keyFile, String keyPassword,
                            long timeout)
    throws SSH2Exception, IOException {
        SSH2KeyPairFile kpf = new SSH2KeyPairFile();

        kpf.load(keyFile, keyPassword);

        String        alg  = kpf.getAlgorithmName();
        SSH2Signature sign = SSH2Signature.getInstance(alg);

        sign.initSign(kpf.getKeyPair().getPrivate());
        sign.setPublicKey(kpf.getKeyPair().getPublic());

        SSH2AuthModule am;
        if (hostbased) {
            am = new SSH2AuthHostBased(sign);
        } else { 
            am = new SSH2AuthPublicKey(sign);
        }
        init(transport, username, am, timeout);
    }


    /**
     * Constructor to use for keyboard interactive authentication.
     *
     * @param transport  connected transport layer
     * @param username   name of user
     * @param interactor interactor instance to handle user
     * interaction in authentication
     * @param timeout    timeout in milliseconds for authentication phase (0 means no timeout)
     *
     * @see SSH2Transport
     */
    public SSH2SimpleClient(SSH2Transport transport,
                            String username, SSH2Interactor interactor,
                            long timeout)
    throws SSH2Exception {
        init(transport, username, new SSH2AuthKbdInteract(interactor), timeout);
    }

    /**
     * Constructor to use for keyboard interactive authentication.
     *
     * @param transport  connected transport layer
     * @param username   name of user
     * @param interactor interactor instance to handle user
     * interaction in authentication
     *
     * @see SSH2Transport
     */
    public SSH2SimpleClient(SSH2Transport transport,
                            String username, SSH2Interactor interactor)
    throws SSH2Exception {
        init(transport, username, new SSH2AuthKbdInteract(interactor), 0);
    }

    /**
     * Constructor to use for GSSAPI authentication.
     *
     * @param transport  connected transport layer
     * @param username   name of user
     * @param timeout    timeout in milliseconds for authentication phase (0 means no timeout)
     *
     * @see SSH2Transport
     */
    public SSH2SimpleClient(SSH2Transport transport,
                            String username, 
                            long timeout)
    throws SSH2Exception {
        init(transport, username, new SSH2AuthGSS(), timeout);
    }

    /**
     * Constructor to use when more than one authentication method
     * need to be used and/or other methods than the ones supported above.
     *
     * @param transport     connected transport layer
     * @param authenticator authenticator instance prepared with needed methods
     *
     * @see SSH2Transport
     */
    public SSH2SimpleClient(SSH2Transport transport,
                            SSH2Authenticator authenticator)
    throws SSH2Exception {
        init(transport, authenticator, 0);
    }

    /**
     * Constructor to use when more than one authentication method
     * need to be used and/or other methods than the ones supported above.
     *
     * @param transport     connected transport layer
     * @param authenticator authenticator instance prepared with needed methods
     * @param timeout       timeout in milliseconds for authentication phase (0 means no timeout)
     *
     * @see SSH2Transport
     */
    public SSH2SimpleClient(SSH2Transport transport,
                            SSH2Authenticator authenticator,
                            long timeout)
    throws SSH2Exception {
        init(transport, authenticator, timeout);
    }

    private void init(SSH2Transport transport,
                      String username, SSH2AuthModule authModule,
                      long authTimeout)
    throws SSH2Exception {
        SSH2Authenticator authenticator = new SSH2Authenticator(username);
        authenticator.addModule(authModule);
        init(transport, authenticator, authTimeout);
    }

    private void init(SSH2Transport transport, SSH2Authenticator authenticator,
                      long authTimeout)
    throws SSH2Exception {
        this.transport = transport;

        transport.boot();

        if(!transport.waitForKEXComplete()) {
            throw new SSH2FatalException("Key exchange failed: " +
                                         transport.getDisconnectMessage());
        }

        userAuth = new SSH2UserAuth(transport, authenticator);
        if(!userAuth.authenticateUser("ssh-connection", authTimeout)) {
            throw new SSH2FatalException("Permission denied");
        }

        connection = new SSH2Connection(userAuth, transport);
        transport.setConnection(connection);
        authenticator.clearSensitiveData();

        int alive = transport.getOurPreferences().getIntPreference("alive");
        transport.enableKeepAlive(alive);
    }

    public SSH2Transport getTransport() {
        return transport;
    }

    public SSH2Connection getConnection() {
        return connection;
    }

}
