/******************************************************************************
 *
 * Copyright (c) 1999-2011 Cryptzone AB. All Rights Reserved.
 * 
 * This file contains Original Code and/or Modifications of Original Code as
 * defined in and that are subject to the MindTerm Public Source License,
 * Version 2.0, (the 'License'). You may not use this file except in compliance
 * with the License.
 * 
 * You should have received a copy of the MindTerm Public Source License
 * along with this software; see the file LICENSE.  If not, write to
 * Cryptzone Group AB, Drakegatan 7, SE-41250 Goteborg, SWEDEN
 *
 *****************************************************************************/

package com.mindbright.sshcommon;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;

import java.awt.event.*;

import java.io.File;
import java.util.ArrayList;

import javax.swing.*;

import com.mindbright.application.MindTermApp;

import com.mindbright.gui.GUI;

import com.mindbright.util.Progress;
import com.mindbright.util.StringUtil;
import com.mindbright.util.Util;

/**
 * Copies a bunch of files, optionally recursively, to or from the
 * remote server while giving graphical feedback. This class is meant
 * to be created from the GUI-thread. The actual file transfers will
 * take place in a separate thread.
 */
public final class SSHFileTransferGUIThread extends Thread
    implements SSHFileTransferProgress, ActionListener {


    private static class ProgressBar extends JProgressBar implements Progress {
    	private static final long serialVersionUID = 1L;
        private static final int STEPS = 2048;
        private long max;

        ProgressBar(long max) {
            super(0, STEPS);
            this.max = max;
			setMinimumSize(new Dimension(400, 30));
        }

        public void progress(long value) {
            setValueL(value);
        }

        private void setValueL(long value) {
            super.setValue((int)(value/(double)max * STEPS));
        }

        public void setValue(long value) {
            setValueL(value);
        }

        public void setMaximum(long value) {
            max = value;
        }

		public Dimension getPreferredSize() {
			Dimension ps = super.getPreferredSize();
			Dimension ms = getMinimumSize();
			int w = ps.width < ms.width  ? ms.width : ps.width;
			int h = ps.height < ms.height ? ms.height: ps.height;
			return new Dimension(w, h);
		}
    }

    boolean               recursive, background, toRemote;
    SSHFileTransferDialogControl xferDialog;
    MindTermApp           client;

    String[]    localFileList;
    String[]    remoteFileList;

    JDialog         copyIndicator;
    ProgressBar     progress;
    SSHFileTransfer fileXfer;
    Thread          copyThread;
    JLabel          srcLbl, dstLbl, sizeLbl, nameLbl, speedLbl;
    JButton         cancB;
    long            startTime;
    long            lastTime;
    long            totTransSize;
    long            lastSize;
	long            numFiles;
	long            totFileSize;
    int             fileCnt;
    boolean         doneCopying;

    volatile boolean userCancel;

    boolean isCopyThread;

    /**
     * Create the GUI and start copying the specified files.
     *
     * @param client a connected SSH client which will be used for
     * transport
     * @param fileXfer class resposible for transferring the files
     * @param localFileList List of local files
     * @param remoteFileList List of remote files
     * @param recursive true if the transfer should include the
     * contents of directories.
     * @param background run in the background
     * @param toRemote true if the files should be copied from the
     * local machine to the remote.
     * @param xferDialog dialog causing the file transfer
     */
    public SSHFileTransferGUIThread(MindTermApp client,
                                    SSHFileTransfer fileXfer,
                                    String[] localFileList,
                                    String[] remoteFileList,
                                    boolean recursive, boolean background,
                                    boolean toRemote,
                                    SSHFileTransferDialogControl xferDialog)
        throws Exception {
        setName("SSHFileTransferGUIThread1");

        isCopyThread = false;

        this.localFileList  = localFileList;
        this.remoteFileList = remoteFileList;

        if(!toRemote) {
            if(localFileList.length > 1) {
                throw new Exception("Ambiguous local target");
            }
        } else {
            if(remoteFileList.length > 1) {
                throw new Exception("Ambiguous remote target");
            }
        }

		if (toRemote) {
			long numdirs   = 0;
			long numfiles  = 0;
			long totalsize = 0;
			boolean linkok = true;

			ArrayList<String> a = new ArrayList<String>();

			for (String s : localFileList)
				a.add(s);

			String last = localFileList[localFileList.length-1];

			for(;;) {
				if (a.isEmpty()) break;
				String s = a.remove(0);
				File f = new File(s);
				if (!linkok && Util.isLink(f))
					continue;
				if (f.isDirectory()) {
					numdirs++;
					String[] l = f.list();
					for (String ss : l)
						a.add(s + "/" + ss);
				} else {
					numfiles++;
					totalsize += f.length();
				}
				if (s == last)
					linkok = false;
			}

			numFiles = numfiles;
			totFileSize = totalsize;
		} else {
			long[] x = fileXfer.getFileSizeCount(remoteFileList);
			numFiles = x[0];
			totFileSize = x[1];
		}

        this.client        = client;
        this.fileXfer      = fileXfer;
        this.recursive     = recursive;
        this.background    = background;
        this.toRemote      = toRemote;
        this.fileCnt       = 0;
        this.doneCopying   = false;
        this.startTime     = 0;
        this.lastTime      = 0;
        this.totTransSize  = 0;
        this.lastSize      = 0;
        this.xferDialog  = xferDialog;
        this.start();
    }

    public void run() {
        if(isCopyThread) {
            copyThreadMain();
        } else {
            createGUIAndCopyThread();
        }
    }

    private void copyThreadMain() {

        try {
            nameLbl.setText("...connected");
            fileXfer.setProgress(this);
            if(toRemote) {
                fileXfer.copyToRemote(localFileList, remoteFileList[0],
                                      recursive);
            } else {
                fileXfer.copyToLocal(localFileList[0], remoteFileList,
                                     recursive);
            }

            copyThread.setPriority(Thread.NORM_PRIORITY);
        } catch (Exception e) {
            if(!userCancel) {
                client.alert("File Transfer Error: " + e.getMessage());
            }
        } finally {
            try {
                Toolkit.getDefaultToolkit().beep();
            } catch (Throwable t) {
                /* What can we do... */
            }
        }

        nameLbl.setText("Copied " + fileCnt + " file" + (fileCnt != 1 ? "s" : "") + ".");
        sizeLbl.setText(StringUtil.nBytesToString(totTransSize, 4));
        doneCopying = true;
        if(fileXfer != null)
            fileXfer.abort();
        cancB.setText("Done");

        xferDialog.refresh();
    }

    private void createGUIAndCopyThread() {
        String sourceFile = "localhost:" + unQuote(localFileList[0]);
        String destFile   = client.getHost() + ":" + unQuote(remoteFileList[0]);

        if(!toRemote) {
            String tmp;
            tmp        = sourceFile;
            sourceFile = destFile;
            destFile   = tmp;
        }

        copyIndicator = GUI.newBorderJDialog
            (client.getDialogParent(), "MindTerm - File Transfer", false);

        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.fill      = GridBagConstraints.NONE;
        gbc.insets    = new Insets(2,4,2,4);
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.anchor    = GridBagConstraints.EAST;

        p.add(new JLabel("Source:"), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor    = GridBagConstraints.WEST;
        srcLbl = new JLabel(cutName(sourceFile, 48));
        p.add(srcLbl, gbc);

        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.anchor    = GridBagConstraints.EAST;
        p.add(new JLabel("Destination:"), gbc);
        dstLbl = new JLabel(cutName(destFile, 48));
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor    = GridBagConstraints.WEST;
        p.add(dstLbl, gbc);

        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.anchor    = GridBagConstraints.EAST;
        p.add(new JLabel("Current:"), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor    = GridBagConstraints.WEST;
        nameLbl = new JLabel("connecting...");
        p.add(nameLbl, gbc);

        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.anchor    = GridBagConstraints.EAST;
        p.add(new JLabel("Size:"), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor    = GridBagConstraints.WEST;
        sizeLbl = new JLabel("");
        p.add(sizeLbl, gbc);

        gbc.weightx = 1.0;
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.anchor  = GridBagConstraints.WEST;
        gbc.insets  = new Insets(12, 12, 12, 12);

        progress = new ProgressBar(512);
        p.add(progress, gbc);

		progress.setMaximum(totFileSize);

        gbc.weightx = 0.0;
        gbc.insets  = new Insets(4, 4, 4, 4);
        gbc.fill    = GridBagConstraints.NONE;
        gbc.anchor  = GridBagConstraints.CENTER;
        speedLbl = new JLabel("0.0 kB/sec");
        p.add(speedLbl, gbc);

        copyIndicator.getContentPane().add(p, BorderLayout.CENTER);

        cancB = new JButton("Cancel");
        cancB.addActionListener(this);
        copyIndicator.getContentPane().add(GUI.newButtonPanel
                          (new JButton[] { cancB }), BorderLayout.SOUTH);

        copyIndicator.setResizable(true);
        copyIndicator.pack();

        GUI.placeDialog(copyIndicator);

        isCopyThread = true;
        copyThread = new Thread(this, "SSHFileTransferGUIThread2");

        if(background) {
            copyThread.setPriority(Thread.MIN_PRIORITY);
        }

        copyThread.start();

        copyIndicator.setVisible(true);
    }

    public void startFile(String file, long size) {
		sizeLbl.setText(size >= 0 ? StringUtil.nBytesToString(size, 4) : "unknown");
		nameLbl.setText("[" + fileCnt + " / " + ((numFiles==0) ? "?":numFiles) +"] " + unQuote(file));
		if (lastTime == 0)
            lastTime = System.currentTimeMillis();
        if(startTime == 0)
            startTime = lastTime;
        fileCnt++;
    }

    public void startDir(String file) {
        if(startTime == 0)
            startTime = System.currentTimeMillis();
        if(toRemote) {
            srcLbl.setText(cutName("localhost:" + unQuote(file), 48));
        } else {
            dstLbl.setText(cutName("localhost:" + unQuote(file), 48));
        }
    }

    public void endFile() {
		progress.setValue(totTransSize);
    }

    public void endDir() {}

    public void progress(long size) {
		totTransSize += size;
        long now = System.currentTimeMillis();

        // Update display if count has changed at least 1% and 0.5 second
        // has passed.
		if ((now-lastTime > 500) &&
			(totFileSize == 0 || (((totTransSize - lastSize) * 100)/totFileSize) >= 1)) {
			if (totFileSize > 0)
				progress.setValue(totTransSize);
			long elapsed = now - startTime;
			if (elapsed > 0) {
				long cs = (long) (totTransSize / ((double)elapsed / 1000));
				speedLbl.setText(StringUtil.nBytesToString(cs, 4) + "/s");
			}
			lastSize = totTransSize;
			lastTime = now;
		}
    }

    private static String cutName(String name, int len) {
        if(name.length() > len) {
            len -= 3;
            String pre = name.substring(0, len / 2);
            String suf = name.substring(name.length() - (len / 2));
            name = pre + "..." + suf;
        }
        return name;
    }

    private static String unQuote(String str) {
        if(str.charAt(0) == '"') {
            str = str.substring(1, str.length() - 1);
        }
        return str;
    }

    @SuppressWarnings("deprecation")
	public void actionPerformed(ActionEvent e) {
        if(!doneCopying) {
            userCancel = true;
            if(fileXfer != null)
                fileXfer.abort();
            Thread.yield();
            if(copyThread != null)
                copyThread.stop();
        }
        copyIndicator.dispose();
    }
}
