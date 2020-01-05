package com.haleywang.putty.view;

import com.haleywang.putty.common.Preconditions;
import com.haleywang.putty.dto.SettingDto;
import com.haleywang.putty.service.NotificationsService;
import com.haleywang.putty.storage.FileStorage;
import com.haleywang.putty.util.StringUtils;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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

    ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>());

    public SftpDialog(SpringRemoteView omegaRemote, ChannelSftp sftpChannel) {
        super(omegaRemote, TITLE, false);
        setResizable(false);

        this.sftpChannel = sftpChannel;

        JButton uploadBtn = new JButton("Upload");
        JButton downloadBtn = new JButton("Download");
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(uploadBtn);
        buttonGroup.add(downloadBtn);
        buttonGroup.setSelected(downloadBtn.getModel(), true);

        GridBagConstraints cs1 = new GridBagConstraints();

        cs1.fill = GridBagConstraints.HORIZONTAL;

        JPanel topPanel = new JPanel(new GridBagLayout());
        cs1.gridx = 0;
        cs1.gridy = 0;
        cs1.gridwidth = 1;
        topPanel.add(uploadBtn, cs1);
        cs1.gridx = 1;
        cs1.gridy = 0;
        cs1.gridwidth = 1;

        topPanel.add(downloadBtn, cs1);

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


    public class UploadPanel extends JPanel {

        private final JTextField tfRemote;
        private final JPanel progressBarBox;
        private final JTextField tfLocalPth;

        public UploadPanel() {
            super();

            this.setLayout(new BorderLayout());


            GridBagConstraints cs = new GridBagConstraints();

            cs.fill = GridBagConstraints.HORIZONTAL;

            JPanel panel = new JPanel(new GridBagLayout());


            JLabel lbLocalPath = new JLabel("Local file: ");
            lbLocalPath.setBorder(new EmptyBorder(0, 5, 0, 5));
            cs.gridx = 0;
            cs.gridy = 0;
            cs.gridwidth = 1;
            panel.add(lbLocalPath, cs);

            tfLocalPth = new JTextField(20);
            cs.gridx = 1;
            cs.gridy = 0;
            cs.gridwidth = 2;
            panel.add(tfLocalPth, cs);


            JLabel lbUsername = new JLabel("Remote folder: ");
            lbUsername.setBorder(new EmptyBorder(0, 5, 0, 5));
            cs.gridx = 0;
            cs.gridy = 1;
            cs.gridwidth = 1;
            panel.add(lbUsername, cs);

            tfRemote = new JTextField(20);
            cs.gridx = 1;
            cs.gridy = 1;
            cs.gridwidth = 2;
            panel.add(tfRemote, cs);


            JLabel lbProgress = new JLabel("Progress : ");
            lbProgress.setBorder(new EmptyBorder(0, 5, 0, 5));
            cs.gridx = 0;
            cs.gridy = 2;
            cs.gridwidth = 1;
            panel.add(lbProgress, cs);

            progressBarBox = new JPanel();

            cs.gridx = 1;
            cs.gridy = 2;
            cs.gridwidth = 2;

            JPanel progressBarOuter = new JPanel(new BorderLayout());
            progressBarOuter.setPreferredSize(new Dimension(500, 36));
            progressBarOuter.add(progressBarBox, BorderLayout.CENTER);

            panel.add(progressBarOuter, cs);


            tfLocalPth.setText(FileStorage.INSTANCE.getSetting().getRemoteFolder());
            tfRemote.setText(FileStorage.INSTANCE.getSetting().getLocalFile());


            JButton okBtn = new JButton("OK");


            add(panel);


            add(okBtn, BorderLayout.SOUTH);

            okBtn.addActionListener(e -> startUpload());


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


    public class DownloadPanel extends JPanel {
        private final JTextField tfRemote;
        private final JPanel progressBarBox;
        private final JTextField tfLocalPth;

        public DownloadPanel() {
            super();
            this.setLayout(new BorderLayout());


            GridBagConstraints cs = new GridBagConstraints();

            cs.fill = GridBagConstraints.HORIZONTAL;

            JPanel panel = new JPanel(new GridBagLayout());


            JLabel lbUsername = new JLabel("Remote file: ");
            lbUsername.setBorder(new EmptyBorder(0, 5, 0, 5));
            cs.gridx = 0;
            cs.gridy = 0;
            cs.gridwidth = 1;
            panel.add(lbUsername, cs);

            tfRemote = new JTextField(20);
            cs.gridx = 1;
            cs.gridy = 0;
            cs.gridwidth = 2;
            panel.add(tfRemote, cs);

            JLabel lbLocalPath = new JLabel("Local folder: ");
            lbLocalPath.setBorder(new EmptyBorder(0, 5, 0, 5));
            cs.gridx = 0;
            cs.gridy = 1;
            cs.gridwidth = 1;
            panel.add(lbLocalPath, cs);

            tfLocalPth = new JTextField(20);
            cs.gridx = 1;
            cs.gridy = 1;
            cs.gridwidth = 2;
            panel.add(tfLocalPth, cs);


            JLabel lbProgress = new JLabel("Progress : ");
            lbProgress.setBorder(new EmptyBorder(0, 5, 0, 5));
            cs.gridx = 0;
            cs.gridy = 2;
            cs.gridwidth = 1;
            panel.add(lbProgress, cs);

            progressBarBox = new JPanel();

            cs.gridx = 1;
            cs.gridy = 2;
            cs.gridwidth = 2;

            JPanel progressBarOuter = new JPanel(new BorderLayout());
            progressBarOuter.setPreferredSize(new Dimension(500, 36));
            progressBarOuter.add(progressBarBox, BorderLayout.CENTER);

            panel.add(progressBarOuter, cs);


            tfLocalPth.setText(FileStorage.INSTANCE.getSetting().getLocalFolder());
            tfRemote.setText(FileStorage.INSTANCE.getSetting().getRemoteFile());

            JButton okBtn = new JButton("OK");

            add(panel);

            add(okBtn, BorderLayout.SOUTH);

            okBtn.addActionListener(e -> startDownload());
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
            LOGGER.error("sftp end");

            progressBar.setValue((int) this.max);
        }
    }


}

