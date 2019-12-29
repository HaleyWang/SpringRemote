package com.haleywang.putty.view;

import com.haleywang.putty.service.NotificationsService;
import com.haleywang.putty.util.StringUtils;
import com.jediterm.ssh.jsch.JSchShellTtyConnector;
import com.jediterm.terminal.RequestOrigin;
import com.jediterm.terminal.TtyConnector;
import com.jediterm.terminal.ui.JediTermWidget;
import com.jediterm.terminal.ui.TerminalPanelListener;
import com.jediterm.terminal.ui.TerminalSession;
import com.jediterm.terminal.ui.TerminalWidget;
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider;
import com.jediterm.terminal.ui.settings.SettingsProvider;
import org.alvin.puttydemo.PuttyPane;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;

public class IdeaPuttyPanel extends JPanel implements PuttyPane {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpringRemoteView.class);

    JediTermWidget session;

    public IdeaPuttyPanel(String host, String connectionUser, String port, String connectionPassword) {
        session = new JediTermWidget(new DefaultSettingsProvider());

        this.setLayout(new BorderLayout());
        this.add(session);


        session.setTerminalPanelListener(new TerminalPanelListener() {
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
                NotificationsService.getInstance().info("Session inactive.");
            }

            @Override
            public void onTitleChanged(String title) {
                //do nothing
            }
        });

        try {
            int portInt = Integer.parseInt(port);

            openSession(new JSchShellTtyConnector(host, portInt, connectionUser, connectionPassword));

        }catch (NumberFormatException e) {
            openSession(new JSchShellTtyConnector(host, connectionUser, connectionPassword));

        }



    }

    private void openSession( TtyConnector ttyConnector) {
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

        if(StringUtils.isBlank(command)) {
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
}
