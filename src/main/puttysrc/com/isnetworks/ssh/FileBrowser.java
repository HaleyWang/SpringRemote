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
* Interface which defines the operations a user can perform on either
* the local or remote file systems
*/
package com.isnetworks.ssh;

public interface FileBrowser {

    /**
     * User wants to switch to a directory
     */
    public void fileDoubleClicked(FileListItem file) throws Exception;

    /**
     * Refresh the file list
     */
    public void refresh() throws Exception;

    /**
     * Delete a file or set of files from the file system
     */
    public void delete(FileListItem[] file) throws Exception;

    /**
     * Connect with the file system if needed
     */
    public void initialize() throws Exception;

    /**
     * Create a new empty directory
     */
    public void makeDirectory(String directoryName) throws Exception;

    /**
     * Rename an existing file
     */
    public void rename(FileListItem file, String newFileName) throws Exception;

    /**
     * Switch to a specific, user-input directory
     */
    public void changeDirectory(String directoryName) throws Exception;

    /**
     * Shut down the file browser
     */
    public void disconnect();

}
