package com.haleywang.putty.view.puttypanel;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.haleywang.putty.service.NotificationsService;
import com.haleywang.putty.util.StringUtils;
import com.haleywang.putty.view.SpringRemoteView;
import com.haleywang.putty.view.puttypanel.ssh.JSchShellTtyConnector;
import com.jediterm.terminal.ProcessTtyConnector;
import com.jediterm.terminal.RequestOrigin;
import com.jediterm.terminal.TtyConnector;
import com.jediterm.terminal.ui.JediTermWidget;
import com.jediterm.terminal.ui.TerminalPanelListener;
import com.jediterm.terminal.ui.TerminalSession;
import com.jediterm.terminal.ui.UIUtil;
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider;
import com.pty4j.PtyProcess;
import com.pty4j.WinSize;
import org.alvin.puttydemo.PuttyPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * @author haley
 */
public class IdeaPuttyPanel extends JPanel implements PuttyPane {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpringRemoteView.class);

    JediTermWidget session;

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
                NotificationsService.getInstance().info("Session inactive.");
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

                openSession(new JSchShellTtyConnector(host, portInt, connectionUser, connectionPassword));

            } catch (NumberFormatException e) {
                openSession(new JSchShellTtyConnector(host, connectionUser, connectionPassword));

            }
        }

    }

    public TtyConnector createCmdConnector() {
        try {
            Map<String, String> envs = Maps.newHashMap(System.getenv());
            String[] command;

            if (UIUtil.isWindows) {
                command = new String[]{"cmd.exe"};
            } else {
                command = new String[]{"/bin/bash", "--login"};
                envs.put("TERM", "xterm");
            }

            PtyProcess process = PtyProcess.exec(command, envs, null);

            return new MyProcessTtyConnector(process, Charset.forName("UTF-8"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }


    public class MyProcessTtyConnector extends ProcessTtyConnector {
        private PtyProcess myProcess;


        public MyProcessTtyConnector(PtyProcess process, Charset charset) {
            super(process, charset);
            myProcess = process;

        }

        @Override
        protected void resizeImmediately() {
            if (getPendingTermSize() != null && getPendingPixelSize() != null) {
                myProcess.setWinSize(
                        new WinSize(getPendingTermSize().width, getPendingTermSize().height, getPendingPixelSize().width, getPendingPixelSize().height));
            }
        }

        @Override
        public boolean isConnected() {
            return myProcess.isRunning();
        }

        @Override
        public String getName() {
            return "Local";
        }

        private List<char[]> myDataChunks = Lists.newArrayList();


        @Override
        public int read(char[] buf, int offset, int length) throws IOException {
            int len = super.read(buf, offset, length);
            if (len > 0) {
                char[] arr = Arrays.copyOfRange(buf, offset, len);
                myDataChunks.add(arr);
            }
            return len;
        }

        public List<char[]> getChunks() {
            return Lists.newArrayList(myDataChunks);
        }

        @Override
        public void write(String string) throws IOException {
            super.write(string);
        }

        @Override
        public void write(byte[] bytes) throws IOException {
            super.write(bytes);
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
}
