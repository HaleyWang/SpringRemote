package com.haleywang.putty.view.puttypanel.connector.ssh;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;

/**
 * @author haley
 * @date 2020/8/15
 */
public class JschShellPemTtyConnector extends JschShellTtyConnector {

    /**
     * /your path to your pem/gateway.pem
     */
    private final String pem;

    public JschShellPemTtyConnector(String host, String user, String pem) {
        super(host, user, null);
        this.pem = pem;
    }

    public JschShellPemTtyConnector(String host, int port, String user, String pem) {
        super(host, port, user, null);
        this.pem = pem;
    }

    @Override
    protected void configureJsch(JSch jsch) throws JSchException {
        super.configureJsch(jsch);
        //jsch.setConfig("StrictHostKeyChecking", "no")
        jsch.addIdentity(pem);
    }
}
