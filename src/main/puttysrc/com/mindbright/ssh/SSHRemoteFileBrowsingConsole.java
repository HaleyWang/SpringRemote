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

package com.mindbright.ssh;

import java.io.*;
import java.util.*;

import com.mindbright.sshcommon.SSHConsoleRemote;
import com.mindbright.util.ExpectOutputStream;

import com.isnetworks.ssh.*;

public class SSHRemoteFileBrowsingConsole extends AbstractFileBrowser
    implements ExpectOutputStream.Expector {

    private SSHConsoleRemote remote;
    private OutputStream     stdin;
    private String           remoteLSCommand;

    /** Name of current directory on remote machine */
    private String mCurrentDirectory;

    public SSHRemoteFileBrowsingConsole(FileDisplayControl fileDisplay,
                                        String remoteLSCommand,
                                        SSHConsoleRemote remote,
                                        String cwd) {
        super(fileDisplay);
        this.remoteLSCommand = remoteLSCommand;
        this.remote          = remote;
        mCurrentDirectory    = cwd;
    }

    /**
     * Kick up a new connection to the remote machine, killing the current
     * one if it's still active
     */
    public void initialize() throws Exception {
        if(!remote.shell()) {
            throw
            new Exception( "Error when connecting with remote machine");
        }
        stdin = remote.getStdIn();
        remote.changeStdOut(new ExpectOutputStream(this, "___END_LS___"));
        changeDirectory(mCurrentDirectory);
        refresh();
    }

    /**
     * Shut down the connection to the remote machine if it's active
     */
    public void disconnect() {
        remote.close();
    }

    /**
     * Rather ugly way to get the current directory on the server and a list
     * of files
     */
    public void refresh() throws Exception {
        StringBuilder command = new StringBuilder();
        command.append("echo ___START_PWD___\n");
        command.append("pwd\n");
        command.append(remoteLSCommand);
        command.append("echo ___END_LS___\n");
        doCommand(command);
    }

    /**
     * Executes a "mkdir" on the remote machine
     */
    public void makeDirectory( String directoryName ) throws Exception {
        StringBuilder command = new StringBuilder();
        command.append( "mkdir \"" );
        command.append( directoryName );
        command.append( "\"\n" );
        doCommand(command);
    }

    /**
     * Executes a "mv" on the remote machine
     */
    public void rename( FileListItem oldFile, String newName ) throws Exception {
        StringBuilder command = new StringBuilder();
        command.append("mv \"");
        command.append(oldFile.getAbsolutePath());
        command.append("\" \"");
        command.append(oldFile.getParent());
        command.append(newName);
        command.append("\"\n");
        doCommand(command);
    }

    /**
     * Does a "cd" on the remote machine
     */
    public void changeDirectory( String directoryName ) throws Exception {
        StringBuilder command = new StringBuilder();
        command.append( "cd \"" );
        command.append( directoryName );
        command.append( "\"\n" );
        doCommand(command);
    }

    /**
     * Does a "rmdir" for directories in the array and a "rm" for files
     * Will not delete non-empty directories
     */
    public void delete( FileListItem[] fileListItem ) throws Exception {
        StringBuilder command = new StringBuilder();
        for( int i = 0; i < fileListItem.length; i++ ) {
            if ( fileListItem[ i ].isDirectory() ) {
                command.append( "rmdir \"" );
            } else {
                command.append( "rm -f \"" );
            }
            command.append( fileListItem[ i ].getAbsolutePath() );
            command.append( "\"\n" );
        }
        doCommand(command);
    }

    public void doCommand(StringBuilder command) throws Exception {
        try {
            stdin.write(command.toString().getBytes());
        } catch (IOException e) {
            throw new Exception("Error sending command to remote machine");
        }
    }

    /**
     * User double clicked on a file in the list.  Check if it's a directory
     * and change to it if it is.
     */
    public void fileDoubleClicked(FileListItem fileListItem)
    throws Exception {
        if(fileListItem != null && fileListItem.isDirectory()) {
            StringBuilder command = new StringBuilder();
            command.append( "cd \"" );
            command.append( fileListItem.getAbsolutePath() );
            command.append( "\"\n" );
            doCommand(command);
        }
    }

    public void reached(ExpectOutputStream out, byte[] buf, int len) {
        String captOutput  = new String(buf, 0, len);
        StringTokenizer st = new StringTokenizer(captOutput, "\n");

        String line = st.nextToken();
        while(line.indexOf("___START_PWD___") == -1) {
            line = st.nextToken();
        }

        mCurrentDirectory = st.nextToken();
        if(!mCurrentDirectory.endsWith("/")) {
            mCurrentDirectory = mCurrentDirectory + "/";
        }

        if(!mCurrentDirectory.equals("/")) {
            dirs.addElement(new FileListItem("..", mCurrentDirectory,
                                             true, "/" ));
        }

        // Parse the file listing
        while(st.hasMoreTokens()) {
            line = st.nextToken();
            boolean directory = line.endsWith("/");
            boolean strip     = directory || line.endsWith("@") ||
                                line.endsWith("*");
            String       name = (strip ? line.substring(0, line.length() - 1) :
                                 line);
            FileListItem item = new FileListItem(name,
                                                 mCurrentDirectory,
                                                 directory, "/");
            if(directory) {
                dirs.addElement(item);
            } else {
                files.addElement(item);
            }
        }

        mFileDisplay.setFileList(dirs, files, mCurrentDirectory, "/", false);
    }

    public void closed(ExpectOutputStream out, byte[] buf, int len) {
        /* partial result on close? we're not interested... */
    }

}
