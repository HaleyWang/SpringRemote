/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.alvin.puttydemo;

import com.mindbright.ssh.SSH;
import com.mindbright.ssh.SSHInteractiveClient;
import com.mindbright.ssh.SSHPropertyHandler;
import com.mindbright.ssh.SSHStdIO;
import com.mindbright.terminal.GlobalClipboard;
import com.mindbright.terminal.TerminalWin;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Properties;
import javax.swing.JPanel;

/**
 *
 * @author Administrator
 */
public class PuttyPane extends JPanel {

    private TerminalWin term;
    private SSHInteractiveClient client;
    private SSHStdIO console;
    private Properties props;
    private String host;
    private String user;
    private String port;
    private String password;

    public PuttyPane(final String host, final String user, final String port, final String password) {
        setLayout(new BorderLayout());
        props = new Properties();
        this.host = host;
        this.user = user;
        this.port = port;
        this.password = password;
        setBackground(Color.BLACK);
    }

    public void init() {
        term = new TerminalWin(PuttyPane.this, props, true);

        SSHPropertyHandler propsHandler = new SSHPropertyHandler(host, user, port, password);
        client = new SSHInteractiveClient(true, false, propsHandler);
        SSHPropertyHandler props = client.getPropertyHandler();
        props.setDefaultProperty("server", host);
        props.setDefaultProperty("username", user);
        console = (SSHStdIO) client.getConsole();

        client.setDialogParent(PuttyPane.this);
        term.setClipboard(GlobalClipboard.getClipboardHandler());
        term.setIgnoreClose();
        add(term.getPanelWithScrollbar());
        updateUI();
        term.requestFocus();

        console.setTerminal(term);
        console.setOwnerContainer(PuttyPane.this);
        console.setOwnerName(SSH.VER_MINDTERM);
//        console.updateTitle();

        props.setAutoSaveProps(false);
        props.setAutoLoadProps(false);
        props.setSavePasswords(false);
        client.startSFTP(false);
        Thread clientThread = new Thread(client);
        clientThread.start();
//        try {
//            clientThread.join();
//        } catch (InterruptedException ex) {
//            Logger.getLogger(PuttyPane.class.getName()).log(Level.SEVERE, null, ex);
//        }

    }

    public void close()
    {
        this.client.getConsoleRemote().close();
    }

    public void setTermFocus() {
        if (term != null) {
            term.requestFocus();
        }
    }

    public SSHInteractiveClient getClient() {
        return client;
    }

    public SSHStdIO getConsole() {
        return console;
    }

    public TerminalWin getTerm() {
        return term;
    }

    public void typedString(String str) {
        term.typedCharInt(str.getBytes());
    }
}
