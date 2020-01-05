package com.haleywang.putty.view;

import com.haleywang.putty.common.Preconditions;
import com.haleywang.putty.service.NotificationsService;
import com.haleywang.putty.util.StringUtils;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * @author haley
 */
public class SftpDialog extends JDialog {

    private static final Logger LOGGER = LoggerFactory.getLogger(SftpDialog.class);

    private static final String TITLE = "Sftp";

    ChannelSftp sftpChannel;

    public SftpDialog(SpringRemoteView omegaRemote) {
        super(omegaRemote, TITLE, false);
        setResizable(false);

        try {
            sftpChannel = SpringRemoteView.getInstance().openSftpChannel();
        } catch (JSchException e) {
            e.printStackTrace();
        }


        JButton uploadBtn = new JButton("Upload");
        JButton downloadBtn = new JButton("Download");

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
            //progressBarBox.setLayout(new BoxLayout(progressBarBox, BoxLayout.Y_AXIS));

            cs.gridx = 1;
            cs.gridy = 2;
            cs.gridwidth = 2;

            JPanel progressBarOuter = new JPanel(new BorderLayout());
            progressBarOuter.setPreferredSize(new Dimension(500, 36));
            progressBarOuter.add(progressBarBox, BorderLayout.CENTER);

            panel.add(progressBarOuter, cs);

            tfLocalPth.setText(".");
            tfRemote.setText("tmp/aa.apk");


            JButton okBtn = new JButton("OK");


            add(panel);


            add(okBtn, BorderLayout.SOUTH);
            System.out.println("===> Thread " + Thread.currentThread().getId());

            okBtn.addActionListener(e ->

                    startUpload()

            );


        }

        private void startUpload() {
            SftpProgressMonitor monitor = new MyProgressBarMonitor(progressBarBox);
            System.out.println("progressBarBox : " + progressBarBox.getPreferredSize());

            try {


                int mode = ChannelSftp.OVERWRITE;
                //mode = ChannelSftp.RESUME;
                //mode = ChannelSftp.APPEND;


                new Thread(() -> {


                    System.out.println("===> tt2 " + Thread.currentThread().getId());

                    String p2 = tfRemote.getText().trim();
                    String p1 = tfLocalPth.getText().trim();
                    Preconditions.checkArgument(!StringUtils.isBlank(p1), "Remote file path is empty");
                    Preconditions.checkArgument(!StringUtils.isBlank(p2), "Local folder path is empty");


                    try {

                        if (!sftpChannel.isConnected()) {
                            sftpChannel.connect();
                        }

                        sftpChannel.put(p1, p2, monitor, mode);
                    } catch (SftpException e) {
                        NotificationsService.getInstance().showErrorDialog(this, null, e.getMessage());
                    } catch (Exception e) {
                        NotificationsService.getInstance().showErrorDialog(this, null, e.getMessage());

                    }
                }).start();


            } catch (Exception e) {
                NotificationsService.getInstance().showErrorDialog(this, null, e.getMessage());

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
            //progressBarBox.setLayout(new BoxLayout(progressBarBox, BoxLayout.Y_AXIS));

            cs.gridx = 1;
            cs.gridy = 2;
            cs.gridwidth = 2;

            JPanel progressBarOuter = new JPanel(new BorderLayout());
            progressBarOuter.setPreferredSize(new Dimension(500, 36));
            progressBarOuter.add(progressBarBox, BorderLayout.CENTER);

            panel.add(progressBarOuter, cs);

            tfLocalPth.setText(".");
            tfRemote.setText("tmp/aa.apk");


            JButton okBtn = new JButton("OK");


            add(panel);


            add(okBtn, BorderLayout.SOUTH);
            System.out.println("===> Thread " + Thread.currentThread().getId());

            okBtn.addActionListener(e ->

                    startDownload()

            );


        }

        private void startDownload() {
            SftpProgressMonitor monitor = new MyProgressBarMonitor(progressBarBox);
            System.out.println("progressBarBox : " + progressBarBox.getPreferredSize());

            try {


                int mode = ChannelSftp.OVERWRITE;
                //mode = ChannelSftp.RESUME;
                //mode = ChannelSftp.APPEND;


                new Thread(() -> {


                    System.out.println("===> tt2 " + Thread.currentThread().getId());


                    String p1 = tfRemote.getText().trim();
                    String p2 = tfLocalPth.getText().trim();
                    Preconditions.checkArgument(!StringUtils.isBlank(p1), "Remote file path is empty");
                    Preconditions.checkArgument(!StringUtils.isBlank(p2), "Local folder path is empty");


                    try {

                        if (!sftpChannel.isConnected()) {
                            sftpChannel.connect();
                        }

                        sftpChannel.get(p1, p2, monitor, mode);
                    } catch (SftpException e) {
                        NotificationsService.getInstance().showErrorDialog(this, null, e.getMessage());
                    } catch (Exception e) {
                        NotificationsService.getInstance().showErrorDialog(this, null, e.getMessage());

                    }
                }).start();


            } catch (Exception e) {
                NotificationsService.getInstance().showErrorDialog(this, null, e.getMessage());

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

            progressBarBox.removeAll();
            progressBar = new JProgressBar();
            progressBar.setPreferredSize(new Dimension(490, 28));

            progressBar.setMaximum((int) max);
            progressBar.setMinimum((int) 0);
            progressBar.setValue((int) count);
            progressBar.setStringPainted(true);


            progressBarBox.add(progressBar);


            progressBarBox.validate();


            System.out.println("===> tt" + Thread.currentThread().getId());
            System.out.println("!info:" + info + ", max=" + max + " " + progressBar);
        }

        @Override
        public void init(int op, String src, String dest, long max) {
            init(src, max);
        }

        public boolean count(long count) {
            this.count += count;
            //System.out.println("count: " + count);

            progressBar.setValue((int) this.count);

            return true;
        }

        public void end() {
            System.out.println("end");
            progressBar.setValue((int) this.max);
            //frame.setVisible(false);
        }
    }


}

