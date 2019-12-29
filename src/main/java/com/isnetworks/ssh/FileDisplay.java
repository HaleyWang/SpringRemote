/*
 * ====================================================================
 *
 * License for ISNetworks' MindTerm SCP modifications
 *
 * Copyright (c) 2001 ISNetworks, LLC.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include an acknowlegement that the software contains
 *    code based on contributions made by ISNetworks, and include
 *    a link to http://www.isnetworks.com/.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 */

/**
* Swing Panel that represents a file system.  Has buttons for basic
* file administration operations and a list of the files in a
* given directory.
* 
* This code is based on a LayoutManager tutorial on Sun's Java web site.
* http://developer.java.sun.com/developer/onlineTraining/GUI/AWTLayoutMgr/shortcourse.html
*/
package com.isnetworks.ssh;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.*;

import java.io.File;

import javax.swing.*;
import javax.swing.event.*;

import java.util.*;

import com.mindbright.ssh.SSHMiscDialogs;
import com.mindbright.sshcommon.SSHFileTransferDialogControl;
import com.mindbright.util.ArraySort;
import com.mindbright.util.StringUtil;

/** This class represents a small pane which will list the files present
 *  on a given platform.  This pane was made into its own class to allow
 *  easy reuse as both the local and remote file displays
 *  
 *  This GUI is built up using "lazy instantiation" via method calls
 *  for each part of the component.
 */
public class FileDisplay extends JPanel implements 
   ActionListener, ListSelectionListener, MouseListener, FileDisplayControl {
	private static final long serialVersionUID = 1L;

    public void actionPerformed(ActionEvent e) {
        try {
            String cmd = e.getActionCommand();
            if ("setdir".equals(cmd)) {
                String directoryName = getFileSystemLocation();
                
                if ( directoryName == null || directoryName.equals(mCurrDir)) 
                    return;
                
                mBrowser.changeDirectory( directoryName );                
            } else if ("chdir".equals(cmd)) {
                String directoryName =
                    SSHMiscDialogs.textInput("Change directory", "Directory", 
                                             mOwner, getFileSystemLocation() );
                if ( directoryName != null ) {
                    mBrowser.changeDirectory( directoryName );
                }
            } else if ("mkdir".equals(cmd)) {
                String directoryName =
                    SSHMiscDialogs.textInput("Make directory relative to current path", 
                                             "Directory name", mOwner );
                if ( directoryName != null ) {
                    mBrowser.makeDirectory( directoryName );
                }
            } else if ("rename".equals(cmd)) {
                FileListItem mFileListItem = getSelectedFile();
                String newName =
                    SSHMiscDialogs.textInput("Rename file",
                                             "New file name", mOwner,
                                             mFileListItem.getName());
                if (newName != null) {
                    mBrowser.rename(mFileListItem, newName);
                }
            } else if ("delete".equals(cmd)) {
                mBrowser.delete(getSelectedFiles());
            } else if ("refresh".equals(cmd)) {
                // done below
            } 
            mBrowser.refresh();
        } catch (Exception ex) {
            mFileXferDialog.logError(ex);
            try {
                mBrowser.refresh();
            } catch (Throwable t) {
            }
        }
    }

    public void valueChanged(ListSelectionEvent e) {
        enableButtons();
        mFileList.ensureIndexIsVisible(mFileList.getLeadSelectionIndex());
    }

    public void mouseClicked(MouseEvent e)  {
        if (e.getClickCount() >= 2) {
            int idx = mFileList.locationToIndex(e.getPoint());    
            mFileList.ensureIndexIsVisible(idx);
            FileListItem item = mFileList.getFileListItem
                ((String)mFileList.getModel().getElementAt(idx));
            if (item != null) {
                try {
                    mBrowser.fileDoubleClicked(item);
                    mBrowser.refresh();
                } catch (Exception ex) {
                    mFileXferDialog.logError(ex);
                }
            }
        }
    }
    public void mouseEntered(MouseEvent e)  {}
    public void mouseExited(MouseEvent e)   {}
    public void mousePressed(MouseEvent e)  {}
    public void mouseReleased(MouseEvent e) {}

    private FileBrowser mBrowser;

    private JButton     mChgDirButton;
    private JButton     mDeleteButton;
    private JPanel      mFileHeaderPanel;
    private FileList    mFileList;
    private JLabel      mMachineDescriptionLabel;
    private String      mMachineDescriptionText;
    private JComboBox   mFileSystemLocationComboBox;
    private String      mCurrDir;
    private JButton     mMkDirButton;
    private JButton     mRefreshButton;
    private JButton     mRenameButton;

    /**
     * Component to own dialog boxes
     */
    private Component mOwner;

    /**
     * Reference to SCP main dialog box to send error messages to
     */
    private SSHFileTransferDialogControl mFileXferDialog;

    /** Constructor
     *  This defines the overall GUI for this component
     *  It's a BorderLayout with a header, a set of buttons & a list
     */
    public FileDisplay(Component owner, String name,
                       SSHFileTransferDialogControl fileXferDialog) {
        super(new BorderLayout());

        mOwner     = owner;
        mFileXferDialog = fileXferDialog;

        mMachineDescriptionLabel = new JLabel( name );
        mMachineDescriptionText  = name;

        add("North",  getFileHeaderPanel());
        JComponent c = getFileList();
        JScrollPane sp = new JScrollPane
            (c, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
             ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);        
        add("Center", sp);
        add("South",  getFileButtonsPanel());
    }

    /** The header panel -- contains labels for Remote/Local and the current directory */
    private JPanel getFileHeaderPanel() {
        if (mFileHeaderPanel == null) {
            mFileHeaderPanel = new JPanel(new GridLayout(2, 1));
            mFileHeaderPanel.add(getMachineDescriptionLabel());
            mFileHeaderPanel.add(getFileSystemLocationComboBox());
        }
        return mFileHeaderPanel;
    }

    /** The label to show which system this file display refers to */
    private JLabel getMachineDescriptionLabel() {
        // Created in constructor
        return mMachineDescriptionLabel;
    }

    /** The label to show which directory this display refers to */
    private JComboBox getFileSystemLocationComboBox() {
        if (mFileSystemLocationComboBox == null) {
            mFileSystemLocationComboBox = new JComboBox();
            mFileSystemLocationComboBox.addActionListener(this);
            mFileSystemLocationComboBox.setActionCommand("setdir");
        }
        return mFileSystemLocationComboBox;
    }

    /** The panel containing the buttons for the file list */
    private JPanel getFileButtonsPanel() {
        JPanel p = new JPanel(new GridLayout(2, 3, 5, 5));
        p.add(getChgDirButton());
        p.add(getMkDirButton());
        p.add(getRenameButton());
        p.add(getDeleteButton());
        p.add(getRefreshButton());

        JPanel p1 = new JPanel(new BorderLayout());
        p1.add(new JLabel(" "), BorderLayout.NORTH);
        p1.add(p, BorderLayout.WEST);
        p1.add(new JLabel(""), BorderLayout.CENTER);

        return p1;
    }


    //----- Buttons -----
    private JButton makeButton(String label, String acmd) {
        JButton b = new JButton(label);
        b.setActionCommand(acmd);
        b.addActionListener(this);
        b.setMargin(new Insets(2,2,2,2));
        return b;
    }

    private JButton getChgDirButton() {
        if (mChgDirButton == null)
            mChgDirButton = makeButton("ChDir", "chdir");
        return mChgDirButton;
    }

    private JButton getMkDirButton() {
        if (mMkDirButton == null)
            mMkDirButton = makeButton("MkDir", "mkdir");
        return mMkDirButton;
    }

    private JButton getRenameButton() {
        if (mRenameButton == null)
            mRenameButton = makeButton("Rename", "rename");
        return mRenameButton;
    }

    private JButton getDeleteButton() {
        if (mDeleteButton == null)
            mDeleteButton = makeButton("Delete", "delete");
        return mDeleteButton;
    }

    private JButton getRefreshButton() {
        if (mRefreshButton == null)
            mRefreshButton = makeButton("Refresh", "refresh");
        return mRefreshButton;
    }

    /** The list of files */
    private FileList getFileList() {
        if (mFileList == null) {
            mFileList = new FileList();
            mFileList.addListSelectionListener(this);
            mFileList.addMouseListener(this);
        }
        return mFileList;
    }

    private void enableButtons() {
        mRenameButton.setEnabled( mFileList.getSelectionCount() == 1 );
        mDeleteButton.setEnabled( mFileList.getSelectionCount() > 0 );
    }

    //----- public methods that make the file system label a property -----

    public String getFileSystemLocation() {
        return (String)getFileSystemLocationComboBox().getSelectedItem();
    }

    private void setFileSystemLocations(String[] roots, String separator, String currdir) {
        ArrayList<String> dirs = new ArrayList<String>();
        
        if (roots != null) {
            dirs.add(currdir);
            try {
                File cd = new File(currdir);
                while (cd != null) {
                    cd = cd.getParentFile();
                    String p = cd.getCanonicalPath();
                    if (!p.endsWith(separator))
                        p += separator;
                    dirs.add(0, p);
                }            
            } catch (Throwable t) {
            }
            for (int i=0; i<roots.length; i++)
                dirs.add(roots[i]);
        } else {
            String cd = currdir;
            for(;;) {
                dirs.add(0, cd);
                int idx = cd.lastIndexOf(separator, cd.length()-2);
                if (idx < 0) break;
                cd = cd.substring(0, idx+1);
            }            
        }
        mCurrDir = currdir;
        getFileSystemLocationComboBox().setModel
            (new DefaultComboBoxModel(dirs.toArray(new String[dirs.size()])));
        getFileSystemLocationComboBox().setSelectedItem(currdir);
    }

    public void setFileList(Vector<FileListItem> dirs, 
                            Vector<FileListItem> files, 
                            String directory,
                            String separator,
                            boolean addroots) {
        if(!directory.endsWith(separator)) {
            directory += separator;
        }

        String[] roots = null;
        if (addroots) {
            try {
                ArrayList<String> a = new ArrayList<String>();
                File[] r = File.listRoots();
                if (r.length > 1) {
                    String cwdpath = (new File(directory)).getCanonicalPath();
                    for(int i=0;i<r.length;i++) {
                        String root = r[i].toString();
                        if (!cwdpath.startsWith(root))
                            a.add(root);                        
                    }
                }
                roots = a.toArray(new String[a.size()]);
            } catch (Throwable t) {
            }
        }

        setFileSystemLocations(roots, separator, directory);

        int i, dl = dirs.size(), fl = files.size();
        long totSize = 0;
        FileListItem[] list = new FileListItem[dl + fl];
        for(i = 0; i < dl; i++) {
            list[i] = dirs.elementAt(i);
        }
        for(i = 0; i < fl; i++) {
            FileListItem item = files.elementAt(i);
            list[i + dl] = item;
            totSize += item.getSize();
        }

        String sizeStr = "";
        if(totSize > 0) {
            sizeStr = " (" + StringUtil.nBytesToString(totSize, 4) + ")";
        }

        getMachineDescriptionLabel().setText(mMachineDescriptionText + " : " +
                                             fl + " file" + (fl > 1 ? "s" : "")
                                             + sizeStr);

        ArraySort.sort(list, 0, dl);
        ArraySort.sort(list, dl, dl + fl);

        dirs.setSize(0);
        files.setSize(0);

        mFileList.setListItems(list);

        enableButtons();
    }

    public void setFileBrowser(FileBrowser browser) {
        mBrowser = browser;
    }

    public FileListItem getSelectedFile() {
        return mFileList.getSelectedFileListItem();
    }

    public FileListItem[] getSelectedFiles() {
        return mFileList.getSelectedFileListItems();
    }

}
