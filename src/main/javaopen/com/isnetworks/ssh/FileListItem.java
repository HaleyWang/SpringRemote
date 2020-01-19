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
* Representation of a file on either the local or remote file systems.
* Knows whether it's a directory as well as its name and path.
*/
package com.isnetworks.ssh;

import com.mindbright.util.ArraySort;

public final class FileListItem implements ArraySort.Comparable {

    private String  name;
    private String  parent;
    private boolean directory;
    private long    size;

    public FileListItem(String name, String parent, boolean directory,
                        String separator) {
        this(name, parent, directory, separator, -1);
    }

    public FileListItem(String name, String parent, boolean directory,
                        String separator, long size) {
        if(!parent.endsWith(separator)) {
            parent += separator;
        }
        this.name      = name;
        this.parent    = parent;
        this.directory = directory;
        this.size      = size;
    }

    /**
     * Get fully qualified name
     */
    public String getAbsolutePath() {
        return parent + name;
    }

    /**
     * Get name of file relative to its parent directory
     */
    public String getName() {
        return name;
    }

    /**
     * Get size of file
     */
    public long getSize() {
        return size;
    }

    /**
     * Get full path of directory this file lives in
     */
    public String getParent() {
        return parent;
    }

    public boolean isDirectory() {
        return directory;
    }

    public void sort(FileListItem[] list) {
        ArraySort.sort(list);
    }

    public int compareTo(ArraySort.Comparable o) {
        if (name.equals("..")) return -1;
        FileListItem other = (FileListItem)o;
        return name.toUpperCase().
               compareTo(other.name.toUpperCase());
    }
}
