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

package com.mindbright.net.ftp;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Interface which describes FTP protocol functions.
 */
public interface FTPServerEventHandler {
    /**
     * Login to server.
     *
     * @param user Username to login as.
     * @param pass Password.
     *
     * @return Returns true if the login was successful.
     */
    public boolean login(String user, String pass);

    /**
     * Close the ftp session.
     */
    public void quit();

    /**
     * Check if the name refers to a normal file as opposed to for example
     * a directory.
     *
     * @param file The name of the file.
     *
     * @return True if the name refers to a plain file.
     */
    public boolean isPlainFile(String file);

    /**
     * Change current directory on the server.
     *
     * @param dir Name of new directory
     */
    public void changeDirectory(String dir) throws FTPException;

    /**
     * The first step in a rename operation. Specifies which file to rename.
     * Must be folloed by a call to <code>renameTo</code>.
     *
     * @param from The name of the file to rename.
     */
    public void renameFrom(String from) throws FTPException;

    /**
     * The second step in the rename operation. Specifies the new name of the
     * file. Must be preceeded by a call to <code>renameFrom</code>
     *
     * @param to The new name of the file.
     */
    public void renameTo(String to) throws FTPException;

    /**
     * Delete the given file.
     *
     * @param file Name of the file to delete.
     */
    public void delete(String file) throws FTPException;

    /**
     * Remove the given directory.
     *
     * @param dir Directory to remove.
     */
    public void rmdir(String dir) throws FTPException;

    /**
     * Create a new directory.
     *
     * @param dir Name of directory to create.
     */
    public void mkdir(String dir) throws FTPException;

    /**
     * Modify the file permissions.
     *
     * @param mod  New file permissions.
     * @param file Name of file to modify.
     */
    public void chmod(int mod, String file) throws FTPException;

    /**
     * Get the current working directory.
     *
     * @return The path to the current working directory.
     */
    public String pwd();

    /**
     * Get the type of system the server is running.
     *
     * @return A string describing the system.
     */
    public String system();

    /**
     * Find out when a file was last modified.
     *
     * @param file Name of file to investigate.
     *
     * @return The timestamp of the file.
     */
    public long modTime(String file) throws FTPException;

    /**
     * Get the size of a file.
     *
     * @param file Name of file to get the size of.
     *
     * @return The size of the file.
     */
    public long size(String file) throws FTPException;

    //
    // !!! TODO, store/retrieve can return size and we can do some stats...
    //

    /**
     * Store a file on the server. Line endings are translated into
     * the proper form for non-binary files.
     *
     * @param file Name to store file as.
     * @param data Stream from which the contents of the file is read.
     * @param binary Controls the translating of line endings.
     */

    public void store(String file, InputStream data, boolean binary)
    throws FTPException;

    /**
     * Append to a file on the server. Line endings are translated into
     * the proper form for non-binary files.
     *
     * @param file File to append to.
     * @param data Stream from which the contents of the file is read.
     * @param binary Controls the translating of line endings.
     */
    public void append(String file, InputStream data, boolean binary)
    throws FTPException;

    /**
     * Retrieve a file from the server. Line endings are translated into
     * the proper form for non-binary files.
     *
     * @param file Name of file to retrieve.
     * @param data Stream to which the file content is written.
     * @param binary Controls the translating of line endings.
     */
    public void retrieve(String file, OutputStream data, boolean binary)
    throws FTPException;

    /**
     * Lists the names of the files and directories in the given directory.
     * The difference between this function and <code>nameList</code> is
     * that the latter appends a trailing '/' to directory names.
     *
     * @param path Directory to list the contens of.
     * @param data Stream to write the results to.
     */
    public void list(String path, OutputStream data) throws FTPException;

    /**
     * Lists the contents of the given directory.
     * The difference between this function and <code>list</code> is
     * that the this appends a trailing '/' to directory names.
     *
     * @param path Directory to list the contens of.
     * @param data Stream to write the results to.
     */
    public void nameList(String path, OutputStream data) throws FTPException;

    public void abort();
}
