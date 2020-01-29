package com.haleywang.putty.view.puttypanel;

import com.google.common.collect.Maps;
import com.haleywang.putty.dto.RemoteSystemInfo;
import com.haleywang.putty.service.NotificationsService;
import com.haleywang.putty.util.SshUtils;
import com.haleywang.putty.util.StringUtils;
import com.haleywang.putty.view.puttypanel.connector.LocalTerminalConnector;
import com.haleywang.putty.view.puttypanel.connector.ssh.JschShellTtyConnector;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jediterm.terminal.RequestOrigin;
import com.jediterm.terminal.TtyConnector;
import com.jediterm.terminal.ui.JediTermWidget;
import com.jediterm.terminal.ui.TerminalPanelListener;
import com.jediterm.terminal.ui.TerminalSession;
import com.jediterm.terminal.ui.UIUtil;
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider;
import com.pty4j.PtyProcess;
import org.alvin.puttydemo.PuttyPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;


/**
 * @author haley
 */
public class IdeaPuttyPanel extends JPanel implements PuttyPane {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdeaPuttyPanel.class);

    JediTermWidget session;
    RemoteSystemInfo remoteSystemInfo;

    public IdeaPuttyPanel(String host, String connectionUser, String port, String connectionPassword) {
        session = new JediTermWidget(new DefaultSettingsProvider());

        this.setLayout(new BorderLayout());
        this.add(session);


        session.setTerminalPanelListener(new TerminalPanelListener() {
            @Override
            public void onPanelResize(final Dimension pixelDimension, final RequestOrigin origin) {
                //do nothing
            }

            @Override
            public void onSessionError(TerminalSession currentSession, Exception e) {

                LOGGER.info("====> onSessionError todo");
                NotificationsService.getInstance().info("Session error.");
            }

            @Override
            public void onSessionChanged(final TerminalSession currentSession) {
                LOGGER.info("====> onSessionChanged todo");

                // TODO if close

                if (!isConnected()) {
                    NotificationsService.getInstance().info(currentSession.getSessionName() + " session inactive.");
                }

            }

            @Override
            public void onTitleChanged(String title) {
                //do nothing
            }
        });


        if (StringUtils.isBlank(host)) {
            openSession(createCmdConnector());

        } else {
            try {
                int portInt = Integer.parseInt(port);

                openSession(new JschShellTtyConnector(host, portInt, connectionUser, connectionPassword));

            } catch (NumberFormatException e) {
                openSession(new JschShellTtyConnector(host, connectionUser, connectionPassword));

            }
        }

    }

    public boolean isConnected() {
        return session.getTtyConnector().isConnected();
    }

    public TtyConnector createCmdConnector() {
        try {
            Map<String, String> envs = Maps.newHashMap(System.getenv());
            String[] command;
            String charset = "UTF-8";

            if (UIUtil.isWindows) {
                command = new String[]{"cmd.exe"};
            } else {
                command = new String[]{"/bin/bash", "--login"};
                //envs.put("TERM", "xterm")
                envs.put("TERM", "xterm-256color");
            }
            String lang = System.getenv().get("LANG");
            //Solve the problem of Chinese garbled characters
            envs.put("LANG", lang != null ? lang : "en_US." + charset);

            PtyProcess process = PtyProcess.exec(command, envs, null);

            return new LocalTerminalConnector(process, Charset.forName(charset));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }


    private void openSession(TtyConnector ttyConnector) {
        session.createTerminalSession(ttyConnector);
        session.start();
    }

    public JediTermWidget getSession() {
        return session;
    }


    @Override
    public void init() {
        //do nothing
    }

    @Override
    public void typedString(String command) {

        if (StringUtils.isBlank(command)) {
            return;
        }

        for (char ch : command.toCharArray()) {
            session.getTerminalPanel().processCharacter(ch, 0);
        }
    }


    @Override
    public void setTermFocus() {
        session.requestFocus();
    }

    @Override
    public void close() {
        session.close();

    }

    public ChannelSftp openSftpChannel() throws JSchException {

        TtyConnector ttyConnector = getSession().getTtyConnector();
        if (isLocal()) {
            return null;
        }

        JschShellTtyConnector shellTtyConnector = (JschShellTtyConnector) ttyConnector;

        return shellTtyConnector.openSftpChannel();
    }

    public boolean isLocal() {
        TtyConnector ttyConnector = getSession().getTtyConnector();
        return ttyConnector instanceof LocalTerminalConnector;

    }

    public RemoteSystemInfo getRemoteSystemInfo(boolean reload) throws JSchException, IOException {
        if (!reload && remoteSystemInfo != null) {
            return remoteSystemInfo;
        }

        TtyConnector ttyConnector = getSession().getTtyConnector();

        JschShellTtyConnector shellTtyConnector = (JschShellTtyConnector) ttyConnector;
        remoteSystemInfo = new RemoteSystemInfo();

        String diskUsageString = SshUtils.sendCommand(shellTtyConnector.getMySession(), remoteSystemInfo.getDiskUsageCmd());
        String memoryUsageString = SshUtils.sendCommand(shellTtyConnector.getMySession(), remoteSystemInfo.getMemoryUsageCmd());
        String cpuUsageString = SshUtils.sendCommand(shellTtyConnector.getMySession(), remoteSystemInfo.getCpuUsageCmd());

        remoteSystemInfo.ofDiskUsageString(diskUsageString).ofMemoryUsageString(memoryUsageString).ofCpuUsageString(cpuUsageString);

        LOGGER.info("diskUsageString:{}", diskUsageString);
        LOGGER.info("memoryUsageString:{}", memoryUsageString);
        LOGGER.info("cpuUsageString:{}", cpuUsageString);


        return remoteSystemInfo;


    }

}