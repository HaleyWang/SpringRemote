/******************************************************************************
 *
 * Copyright (c) 1999-2011 Cryptzone Group AB. All Rights Reserved.
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

package com.mindbright.ssh2;

import com.isnetworks.ssh.AbstractFileBrowser;
import com.isnetworks.ssh.FileListItem;
import com.isnetworks.ssh.FileDisplayControl;

/**
 * Implements a file browser which browses files through an SFTP connection.
 */
public class SSH2SFTPFileBrowser extends AbstractFileBrowser {

    private SSH2Connection connection;
    private SSH2SFTPClient client;
    private String         cwd;

    /**
     * @param connection Connection to use.
     * @param fileDisplay A display object which refers to the actual window.
     * @param cwd Directory to start in.
     */
    public SSH2SFTPFileBrowser(SSH2Connection connection,
                               FileDisplayControl fileDisplay,
                               String cwd) {
        super(fileDisplay);
        this.connection = connection;
        this.cwd        = cwd;
    }

    /*
     * Get the SSH2SFTPClient for this file browser
     *
     * @return returns the SSH2SFTPClient for this file browser
     */
	public SSH2SFTPClient getSFTPClient() {
		return client;
	}

    /**
     * Handles doubleclick events on a file. If the object is a
     * directory then we change current working directory to that
     * directory. Otherwise nothing happens.
     *
     * @param file File doubleclicked.
     */
    public void fileDoubleClicked(FileListItem file) throws Exception {
        if(file != null & file.isDirectory()) {
            if (cwd.endsWith("/")) {
                changeDirectory(cwd + file.getName());
            } else {
                changeDirectory(cwd + "/" + file.getName());
            }
        }
    }

    /**
     * Refres the listing of the current directory.
     */
    public void refresh() throws Exception {
        SSH2SFTP.FileHandle handle = null;
        try {
            handle = client.opendir(cwd);
            SSH2SFTP.FileAttributes[] list = client.readdir(handle);

            if(!cwd.equals("/") && !cwd.equals("")) {
                dirs.addElement(new FileListItem("..", "", true, "/"));
            }

            for(int i = 0; i < list.length; i++) {
                String name = list[i].name;
                if(!("..".equals(name)) && !(".".equals(name))) {
                    boolean isDirectory = list[i].isDirectory();
                    if(list[i].isLink()) {
                        isDirectory = isDirectory(name);
                    }
                    FileListItem item = new FileListItem
                        (name, cwd, isDirectory,
                         "/", list[i].hasSize ? list[i].size : -1);
                    if(isDirectory) {
                        dirs.addElement(item);
                    } else {
                        files.addElement(item);
                    }
                }
            }

            mFileDisplay.setFileList(dirs, files, cwd, "/", false);

        } catch (Exception e) {
            throw new Exception(e.getMessage());
        } finally {
            try {
                client.close(handle);
            } catch (Exception e) { /* don't care */
            }
        }
    }

    /**
     * Delete a set of files.
     *
     * @param files Array of files to delete.
     */
    public void delete(FileListItem[] files) throws Exception {
        String file = null;
        try {
            for(int i = 0; i < files.length; i++) {
                file = files[i].getAbsolutePath();
                SSH2SFTP.FileAttributes attrs = client.stat(file);
                if(attrs.isDirectory()) {
                    client.rmdir(file);
                } else {
                    client.remove(file);
                }
            }
        } catch (SSH2SFTP.SFTPException e) {
            throw new Exception("Unable to delete " + file +
                                " - may not have permission or directory may not be empty");
        }
    }

    /**
     * Initialize the object.
     */
    public void initialize() throws Exception {
        try {
            client = new SSH2SFTPClient(connection, false);
            SSH2SFTP.FileAttributes attrs = client.realpath(cwd);
            cwd = attrs.name;
            refresh();
        } catch (SSH2SFTP.SFTPException e) {
            throw new Exception("Could not start sftp session: " +
                                e.getMessage());
        }
    }

    /**
     * Create a directory on the server.
     *
     * @param directoryName Name of directory to create.
     */
    public void makeDirectory(String directoryName) throws Exception {
        try {
            if(!directoryName.startsWith("/")) {
                directoryName = cwd + "/" + directoryName;
            }
            client.mkdir(directoryName, new SSH2SFTP.FileAttributes());
        } catch (SSH2SFTP.SFTPException e) {
            throw new Exception(e.getMessage());
        }
    }

    /**
     * Renames a file.
     *
     * @param file File to rename.
     * @param newFileName New name.
     */
    public void rename(FileListItem file, String newFileName)
    throws Exception {
        try {
            client.rename(file.getAbsolutePath(), file.getParent() + "/" +
                          newFileName);
        } catch (SSH2SFTP.SFTPException e) {
            throw new Exception(e.getMessage());
        }
    }

    /**
     * Change current working directory.
     *
     * @param newDir Directory to change into.
     */
    public void changeDirectory(String newDir) throws Exception {
        try {
            if(!newDir.startsWith("/")) {
                newDir = cwd + "/" + newDir;
            }
            SSH2SFTP.FileAttributes attrs  = client.realpath(newDir);
            SSH2SFTP.FileHandle     handle = client.opendir(newDir);
            newDir = attrs.name;
            client.close(handle);
        } catch (SSH2SFTP.SFTPException e) {
            newDir = cwd;
        }
        cwd = newDir;
    }

    private boolean isDirectory(String dir) {
        try {
            dir = cwd + "/" + dir;
            SSH2SFTP.FileHandle handle = client.opendir(dir);
            client.close(handle);
            return true;
        } catch (SSH2SFTP.SFTPException e) {}
        return false;
    }

    /**
     * Disconnect from the server.
     */
    public void disconnect() {
        if(client != null) {
            client.terminate();
        }
    }

}
