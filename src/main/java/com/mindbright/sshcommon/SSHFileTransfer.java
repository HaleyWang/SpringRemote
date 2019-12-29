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

package com.mindbright.sshcommon;

import java.io.IOException;

/**
 * Common interface for all file transfer classes.
 */
public interface SSHFileTransfer {
    /**
     * Associate a <code>SSHFileTransferProgress</code> object with
     * this file transfer object. The associated object will get
     * reports on any transfer progress.
     *
     * @param progress Object which will get progress reports.
     */
    public void setProgress(SSHFileTransferProgress progress);

    /**
     * Copy local files to the server. If the <code>remoteFile</code>
     * parameter refers to a directory the all local files are placed
     * into it. However it is kind of meaningless to specify multiple
     * local files and one remote regular file since all files will be
     * copied to the same name.
     *
     * @param localFiles Array of local files to copy.
     * @param remoteFile Name to store file(s) as on server. If this
     * is a directory then all files are copied to that directory.
     * @param recursive If true recurse into directories and copy all
     * files found. The directory structure is recreated on the server.
     */
    public void copyToRemote(String[] localFiles, String remoteFile,
                             boolean recursive)
        throws IOException;

    /**
     * Copy remote files to the local system. If the <code>localFile</code>
     * parameter refers to a directory the all remote files are placed
     * into it. However it is kind of meaningless to specify multiple
     * remote files and one local regular file since all files will be
     * copied to the same name.
     *
     * @param localFile Name to store file(s) as locally. If this
     * is a directory then all files are copied to that directory.
     * @param remoteFiles Array of files to copy.
     * @param recursive If true recurse into directories and copy all
     * files found. The directory structure is recreated on the server.
     */
    public void copyToLocal(String localFile, String remoteFiles[],
                            boolean recursive)
        throws IOException;

    public long[] getFileSizeCount(String remoteFiles[])
		throws IOException;

    /**
     * Abort all operations
     */
    public void abort();
}
