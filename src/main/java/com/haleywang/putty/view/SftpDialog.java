package com.haleywang.putty.view;

import com.haleywang.putty.common.Preconditions;
import com.haleywang.putty.dto.SettingDto;
import com.haleywang.putty.service.NotificationsService;
import com.haleywang.putty.storage.FileStorage;
import com.haleywang.putty.util.StringUtils;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author haley
 */
public class SftpDialog extends JDialog {

    private static final Logger LOGGER = LoggerFactory.getLogger(SftpDialog.class);

    private static final String TITLE = "Sftp";

    private transient ChannelSftp sftpChannel;

    private transient ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>());

    //todo
    public SftpDialog(SpringRemoteView omegaRemote, ChannelSftp sftpChannel) {
        super(omegaRemote, TITLE, false);
        setResizable(false);

        this.sftpChannel = sftpChannel;

        JButton downloadBtn = new JButton("Download");
        JButton uploadBtn = new JButton("Upload");

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(downloadBtn);
        buttonGroup.add(uploadBtn);

        buttonGroup.setSelected(downloadBtn.getModel(), true);

        JPanel topPanel = new JPanel();


        topPanel.add(downloadBtn);
        topPanel.add(uploadBtn);


        UploadPanel uploadPanel = new UploadPanel();
        DownloadPanel downloadPanel = new DownloadPanel();

        JPanel mainPanel = new JPanel();
        mainPanel.add(uploadPanel);
        uploadPanel.setVisible(false);
        mainPanel.add(downloadPanel);

        getContentPane().add(mainPanel, BorderLayout.CENTER);


        getContentPane().add(topPanel, BorderLayout.NORTH);

        downloadBtn.addActionListener(e -> {

            uploadPanel.setVisible(false);
            downloadPanel.setVisible(true);
            getContentPane().validate();

        });

        uploadBtn.addActionListener(e ->
                {
                    uploadPanel.setVisible(true);
                    downloadPanel.setVisible(false);
                    getContentPane().validate();

                }
        );


        pack();
        setLocationRelativeTo(omegaRemote);

    }

    public static class MyFileChooserBuilder {

        /**
         * Setup the GUI components
         */
        public JFileChooser buildLocalFileChooser(String path, int mode) {

            JFileChooser fc = new JFileChooser();

            fc.setFileSelectionMode(mode);

            return fc;
        }

        public MyFileBrowser buildRemoteFileChooser(String path, int mode, MyFileBrowser.OpenActionListener openActionListener) throws SftpException, JSchException {

            MyFileBrowser fb = new MyFileBrowser("Remote file browser", path, openActionListener);
            fb.setMode(mode);

            return fb;
        }

    }


    public class UploadPanel extends JPanel {

        @Resource
        private JTextField tfRemote;
        @Resource
        private JPanel progressBarBox;
        @Resource
        private JTextField tfLocalPth;
        @Resource
        private JButton okBtn;
        @Resource
        private JButton btnOpenLocal;
        @Resource
        private JButton btnOpenRemote;
        @Resource
        private JButton btnCancel;

        public UploadPanel() {
            super();

            this.setLayout(new BorderLayout());

            MyCookSwing cookSwing = new MyCookSwing(this, "view/uploadPanel.xml").fillFieldsValue(this);
            add(cookSwing.getContainer());
            //cookSwing.getContainer().setPreferredSize(new);

            JFileChooser fileChooser = new MyFileChooserBuilder().buildLocalFileChooser("/", JFileChooser.FILES_ONLY);

            btnOpenLocal.addActionListener(e -> {

                fileChooser.addActionListener(p -> {
                    System.out.println(p);
                    tfLocalPth.setText(fileChooser.getSelectedFile().getPath());

                });
                fileChooser.showOpenDialog(SftpDialog.this);

            });


            btnOpenRemote.addActionListener(e -> {
                MyFileBrowser remoteFileChooser = null;
                try {
                    remoteFileChooser = new MyFileChooserBuilder().buildRemoteFileChooser("/", JFileChooser.DIRECTORIES_ONLY, p -> {
                        System.out.println(p);
                        tfRemote.setText(p);

                    });
                } catch (SftpException e1) {
                    e1.printStackTrace();
                } catch (JSchException e1) {
                    e1.printStackTrace();
                }

                remoteFileChooser.setSftpChannel(sftpChannel).showOpenDialog();
            });

            tfLocalPth.setText(FileStorage.INSTANCE.getSetting().getRemoteFolder());
            tfRemote.setText(FileStorage.INSTANCE.getSetting().getLocalFile());

            okBtn.addActionListener(e -> startUpload());
            btnCancel.addActionListener(e -> SftpDialog.this.setVisible(false));

        }

        private void startUpload() {
            SftpProgressMonitor monitor = new MyProgressBarMonitor(progressBarBox);

            try {

                int mode = ChannelSftp.OVERWRITE;
                //ChannelSftp.RESUME
                //ChannelSftp.APPEND

                threadPoolExecutor.execute(() -> {

                    String localFile = tfRemote.getText().trim();
                    String remoteFolder = tfLocalPth.getText().trim();
                    Preconditions.checkArgument(!StringUtils.isBlank(remoteFolder), "Remote folder path is empty");
                    Preconditions.checkArgument(!StringUtils.isBlank(localFile), "Local file path is empty");


                    SettingDto setting = FileStorage.INSTANCE.getSetting();
                    setting.setLocalFile(localFile);
                    setting.setRemoteFolder(remoteFolder);
                    FileStorage.INSTANCE.saveSetting(setting);

                    try {

                        if (!sftpChannel.isConnected()) {
                            sftpChannel.connect();
                        }

                        sftpChannel.put(remoteFolder, localFile, monitor, mode);
                    } catch (SftpException e) {
                        NotificationsService.getInstance().showErrorDialog(this, null, e.getMessage());
                        LOGGER.error("startUpload sftp_error", e);

                    } catch (Exception e) {
                        NotificationsService.getInstance().showErrorDialog(this, null, e.getMessage());
                        LOGGER.error("startUpload put error", e);

                    }
                });


            } catch (Exception e) {
                NotificationsService.getInstance().showErrorDialog(this, null, e.getMessage());
                LOGGER.error("startUpload error", e);
            }
        }
    }


    public final class DownloadPanel extends JPanel {
        @Resource
        private JTextField tfRemote;
        @Resource
        private JPanel progressBarBox;
        @Resource
        private JTextField tfLocalPth;
        @Resource
        private JButton okBtn;
        @Resource
        private JButton btnCancel;
        @Resource
        private JButton btnOpenLocal;
        @Resource
        private JButton btnOpenRemote;

        public DownloadPanel() {
            super();
            this.setLayout(new BorderLayout());

            MyCookSwing cookSwing = new MyCookSwing(this, "view/downloadPanel.xml").fillFieldsValue(this);
            add(cookSwing.getContainer());

            tfLocalPth.setText(FileStorage.INSTANCE.getSetting().getLocalFolder());
            tfRemote.setText(FileStorage.INSTANCE.getSetting().getRemoteFile());

            okBtn.addActionListener(e -> startDownload());
            btnCancel.addActionListener(e -> SftpDialog.this.setVisible(false));

            JFileChooser fileChooser = new MyFileChooserBuilder().buildLocalFileChooser("/", JFileChooser.DIRECTORIES_ONLY);

            btnOpenLocal.addActionListener(e -> {

                fileChooser.addActionListener(p -> {
                    System.out.println(p);
                    if (fileChooser.getSelectedFile() != null) {
                        tfLocalPth.setText(fileChooser.getSelectedFile().getPath());
                    }
                });
                fileChooser.showOpenDialog(SftpDialog.this);

            });

            btnOpenRemote.addActionListener(e -> {
                MyFileBrowser remoteFileChooser = null;
                try {
                    remoteFileChooser = new MyFileChooserBuilder().buildRemoteFileChooser("/", JFileChooser.FILES_ONLY, p -> {
                        System.out.println(p);
                        tfRemote.setText(p);

                    });
                } catch (SftpException e1) {
                    e1.printStackTrace();
                } catch (JSchException e1) {
                    e1.printStackTrace();
                }

                remoteFileChooser.setSftpChannel(sftpChannel).showOpenDialog();
            });
        }

        private void startDownload() {
            SftpProgressMonitor monitor = new MyProgressBarMonitor(progressBarBox);

            try {


                int mode = ChannelSftp.OVERWRITE;


                threadPoolExecutor.execute(() -> {

                    String remoteFile = tfRemote.getText().trim();
                    String localFolder = tfLocalPth.getText().trim();
                    Preconditions.checkArgument(!StringUtils.isBlank(remoteFile), "Remote file path is empty");
                    Preconditions.checkArgument(!StringUtils.isBlank(localFolder), "Local folder path is empty");

                    SettingDto setting = FileStorage.INSTANCE.getSetting();
                    setting.setRemoteFile(remoteFile);
                    setting.setLocalFolder(localFolder);
                    FileStorage.INSTANCE.saveSetting(setting);

                    try {

                        if (!sftpChannel.isConnected()) {
                            sftpChannel.connect();
                        }

                        sftpChannel.get(remoteFile, localFolder, monitor, mode);
                    } catch (SftpException e) {
                        NotificationsService.getInstance().showErrorDialog(this, null, e.getMessage());
                        LOGGER.error("sftp get exception", e);

                    } catch (Exception e) {
                        NotificationsService.getInstance().showErrorDialog(this, null, e.getMessage());
                        LOGGER.error("sftp get common exception", e);
                    }
                });


            } catch (Exception e) {
                NotificationsService.getInstance().showErrorDialog(this, null, e.getMessage());
                LOGGER.error("sftp start exception", e);

            }

        }

    }


    public static class MyProgressBarMonitor implements SftpProgressMonitor {
        JProgressBar progressBar;
        JPanel progressBarBox;
        long count = 0;
        long max = 0;

        public MyProgressBarMonitor(JPanel progressBarBox) {
            this.progressBarBox = progressBarBox;
        }


        public void init(String info, long max) {
            this.max = max;

            count = 0;
            LOGGER.info(" sftp_init {}", info);

            progressBarBox.removeAll();
            progressBarBox.setLayout(new BorderLayout());
            progressBar = new JProgressBar();
            progressBar.setPreferredSize(new Dimension(490, 28));

            progressBar.setMaximum((int) max);
            progressBar.setMinimum((int) 0);
            progressBar.setValue((int) count);
            progressBar.setStringPainted(true);

            progressBarBox.add(progressBar);

            progressBarBox.validate();

        }

        @Override
        public void init(int op, String src, String dest, long max) {
            init(src, max);
        }

        @Override
        public boolean count(long count) {
            this.count += count;
            progressBar.setValue((int) this.count);

            return true;
        }

        @Override
        public void end() {
            LOGGER.info("sftp end");

            progressBar.setValue((int) this.max);
        }
    }


}

