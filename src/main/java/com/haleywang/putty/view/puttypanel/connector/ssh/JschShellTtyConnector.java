package com.haleywang.putty.view.puttypanel.connector.ssh;

import com.haleywang.putty.service.NotificationsService;
import com.haleywang.putty.view.SpringRemoteView;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;

/**
 * @author haley
 */
public class JschShellTtyConnector extends AbstractJschTtyConnector<ChannelShell> {
    private static final Logger LOGGER = LoggerFactory.getLogger(JschShellTtyConnector.class);


    public JschShellTtyConnector(String host, String user, String password) {
        super(host, DEFAULT_SSH_PORT, user, password);
    }

    public JschShellTtyConnector(String host, int port, String user, String password) {
        super(host, port, user, password);
    }

    @Override
    public ChannelShell openChannel(Session session) throws JSchException {
        NotificationsService.getInstance().info(session.getHost() + " connected");
        SwingUtilities.invokeLater(() ->
                SpringRemoteView.getInstance().showRemoteSystemInfo(true)
        );
        LOGGER.info("openChannel shell");
        return (ChannelShell) session.openChannel("shell");
    }


    @Override
    public void configureChannelShell(ChannelShell channel) {
        String lang = System.getenv().get("LANG");
        channel.setEnv("LANG", lang != null ? lang : "en_US.UTF-8");
        channel.setPtyType("xterm");
    }

    @Override
    protected void setPtySize(ChannelShell channel, int col, int row, int wp, int hp) {
        channel.setPtySize(col, row, wp, hp);
    }

}
