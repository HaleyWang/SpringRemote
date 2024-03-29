package com.haleywang.putty.view.puttypanel.connector.ssh;

import com.google.common.net.HostAndPort;
import com.haleywang.putty.service.NotificationsService;
import com.haleywang.putty.util.StringUtils;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jediterm.terminal.Questioner;
import com.jediterm.terminal.TtyConnector;
import org.slf4j.Logger;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author haley
 */
public abstract class AbstractJschTtyConnector<T extends Channel> implements TtyConnector {
    public static final Logger LOG = org.slf4j.LoggerFactory.getLogger(AbstractJschTtyConnector.class);

    public static final int DEFAULT_SSH_PORT = 22;

    private InputStream myInputStream = null;
    private OutputStream myOutputStream = null;
    private Session mySession;
    private T myChannelShell;
    private AtomicBoolean isInitiated = new AtomicBoolean(false);

    private int myPort = DEFAULT_SSH_PORT;

    private String myUser;
    private String myHost;
    private final String myPassword;

    private Dimension myPendingTermSize;
    private Dimension myPendingPixelSize;
    private InputStreamReader myInputStreamReader;

    public AbstractJschTtyConnector(String host, int port, String user, String password) {
        this.myHost = host;
        this.myPort = port;
        this.myUser = user;
        this.myPassword = password;
    }

    @Override
    public void resize(Dimension termSize, Dimension pixelSize) {
        myPendingTermSize = termSize;
        myPendingPixelSize = pixelSize;
        if (myChannelShell != null) {
            resizeImmediately();
        }
    }

    /**
     * setPtySize
     *
     * @param channel
     * @param col
     * @param row
     * @param wp
     * @param hp
     */
    protected abstract void setPtySize(T channel, int col, int row, int wp, int hp);

    private void resizeImmediately() {
        if (myPendingTermSize != null && myPendingPixelSize != null) {
            setPtySize(myChannelShell, myPendingTermSize.width, myPendingTermSize.height, myPendingPixelSize.width, myPendingPixelSize.height);
            myPendingTermSize = null;
            myPendingPixelSize = null;
        }
    }

    public ChannelSftp openSftpChannel() throws JSchException {
        NotificationsService.getInstance().info(mySession.getHost() + " sftp connected");
        return (ChannelSftp) mySession.openChannel("sftp");
    }

    @Override
    public void close() {
        if (mySession != null) {
            mySession.disconnect();
            mySession = null;
            myInputStream = null;
            myOutputStream = null;
        }
    }

    /**
     * openChannel
     *
     * @param session
     * @return
     * @throws JSchException
     */
    public abstract T openChannel(Session session) throws JSchException;

    /**
     * configureChannelShell
     *
     * @param channel
     */
    public abstract void configureChannelShell(T channel);

    @Override
    public boolean init(Questioner q) {

        getAuthDetails(q);

        try {
            mySession = connectSession(q);
            myChannelShell = openChannel(mySession);
            configureChannelShell(myChannelShell);
            myInputStream = myChannelShell.getInputStream();
            myOutputStream = myChannelShell.getOutputStream();
            myInputStreamReader = new InputStreamReader(myInputStream, "utf-8");
            myChannelShell.connect();
            resizeImmediately();
            return true;
        } catch (final IOException e) {
            q.showMessage(e.getMessage());
            LOG.error("Error opening channel", e);
            return false;
        } catch (final JSchException e) {
            q.showMessage(e.getMessage());
            LOG.error("Error opening session or channel", e);
            return false;
        } finally {
            isInitiated.set(true);
        }
    }

    private Session connectSession(Questioner questioner) throws JSchException {
        JSch jsch = new JSch();
        configureJsch(jsch);

        Session session = jsch.getSession(myUser, myHost, myPort);

        final SshUserInfo ui = new SshUserInfo(questioner);
        if (myPassword != null) {
            session.setPassword(myPassword);
            ui.setPassword(myPassword);
        }
        session.setUserInfo(ui);

        final java.util.Properties config = new java.util.Properties();
        config.put("compression.s2c", "zlib,none");
        config.put("compression.c2s", "zlib,none");
        configureSession(session, config);
        session.connect();

        return session;
    }

    protected void configureJsch(JSch jsch) throws JSchException {
        //do nothing

    }

    protected void configureSession(Session session, final java.util.Properties config) throws JSchException {
        session.setConfig(config);
        session.setTimeout(50000);
        session.setConfig("StrictHostKeyChecking", "no");
    }

    private void getAuthDetails(Questioner q) {
        while (StringUtils.isBlank(myUser)) {
            if (myHost == null) {
                myHost = q.questionVisible("host: ", "localhost");
            }
            if (!StringUtils.isBlank(myHost)) {

                try {
                    HostAndPort hostAndPort = HostAndPort.fromString(myHost);
                    myHost = hostAndPort.getHost();
                    myPort = hostAndPort.getPortOrDefault(myPort);
                } catch (IllegalArgumentException e) {
                    q.showMessage(e.getMessage());
                    myHost = q.questionVisible("host: ", myHost);
                    continue;
                }

                if (myUser == null) {
                    myUser = q.questionVisible("user: ", System.getProperty("user.name").toLowerCase());
                }
            }
        }
    }

    @Override
    public String getName() {
        return myHost != null ? myHost : "Remote";
    }

    @Override
    public int read(char[] buf, int offset, int length) throws IOException {
        return myInputStreamReader.read(buf, offset, length);
    }

    public int read(byte[] buf, int offset, int length) throws IOException {
        return myInputStream.read(buf, offset, length);
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        if (myOutputStream != null) {
            myOutputStream.write(bytes);
            myOutputStream.flush();
        }
    }

    @Override
    public boolean isConnected() {
        return myChannelShell != null && myChannelShell.isConnected();
    }

    @Override
    public void write(String string) throws IOException {
        write(string.getBytes(StandardCharsets.UTF_8));
    }

    public Session getMySession() {
        return mySession;
    }

    @Override
    public int waitFor() throws InterruptedException {
        if (myChannelShell == null) {
            return 0;
        }
        while (!isInitiated.get() || isRunning(myChannelShell)) {
            Thread.sleep(200);
        }
        return Optional.ofNullable(myChannelShell).map(Channel::getExitStatus).orElse(0);
    }

    private static boolean isRunning(Channel channel) {
        return channel != null && channel.getExitStatus() < 0 && channel.isConnected();
    }

}
