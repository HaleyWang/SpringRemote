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

import com.mindbright.util.Progress;

/**
 * Interface for tracking progress when transferring files
 */
public interface SSHFileTransferProgress extends Progress {
    /**
     * Called when a file starts to transfer
     *
     * @param file name of file to transfer
     * @param size file size
     */
    public void startFile(String file, long size);

    /**
     * Called when the transfer starts to transfer all files in a
     * directory.
     *
     * @param file name of directory
     */
    public void startDir(String file);

    /**
     * Called when a file has been successfully transferred
     */
    public void endFile();

    /**
     * Called when all files in a directory have been transferred
     */
    public void endDir();
}
