package com.haleywang.putty.view;

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

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;

public class IdeaPuttyPanel extends JPanel implements PuttyPane {

    JediTermWidget session;

    public IdeaPuttyPanel(String host, String connectionUser, String port, String connectionPassword) {
        session = new JediTermWidget(new DefaultSettingsProvider());

        this.setLayout(new BorderLayout());
        this.add(session);


        session.setTerminalPanelListener(new TerminalPanelListener() {
            public void onPanelResize(final Dimension pixelDimension, final RequestOrigin origin) {
                if (origin == RequestOrigin.Remote) {
                   // sizeFrameForTerm(frame);
                }
            }

            @Override
            public void onSessionError(TerminalSession currentSession, Exception e) {
                System.out.println("================ sss error");

            }

            @Override
            public void onSessionChanged(final TerminalSession currentSession) {
                //frame.setTitle(currentSession.getSessionName());
                System.out.println("================ sss change");
            }

            @Override
            public void onTitleChanged(String title) {
                //frame.setTitle(myTerminal.getCurrentSession().getSessionName());
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
