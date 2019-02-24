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
* Subclass of java.awt.List which allows the List to be treated as though
* it directly help FileListItems instead of Strings.  Handles the setting
* and getting of the List's contents as FileListItems.
*/
package com.isnetworks.ssh;

import javax.swing.JList;
import javax.swing.ListModel;

import com.mindbright.util.StringUtil;

public class FileList extends JList {
    private static final long serialVersionUID = 1L;

    /**
     * Array of FileListItems which corresponds to the contents of the List
     */
    private FileListItem[] mListItems;

    /**
     * Set the contents of the List to be the array
     */
    public void setListItems(FileListItem[] listItems) {
        mListItems = listItems;
        String[] a = new String[listItems.length];
        for (int i = 0; i < listItems.length; i++) {
            FileListItem item = listItems[i];
            if (item.isDirectory()) {
                a[i] = "[" + item.getName() + "]";
            } else {
                long sz = item.getSize();
                a[i] = item.getName() + (sz >= 0 ?
                                         " (" +
                                         StringUtil.nBytesToString(sz, 4) +
                                         ")" : "");
            }
        }
        setListData(a);
    }

    /**
     * Gets the first selected item in the list
     */
    public FileListItem getSelectedFileListItem() {
        FileListItem item = null;
        if (getSelectedIndex() != -1) {
            item = mListItems[getSelectedIndex()];
        }
        return item;
    }

    /**
     * Gets all of the current selected items in the list.  Filters out the
     * entry ".." if it exists since the user can't do anything with
     * it, except double click on it to move to the parent directory
     */
    public FileListItem[] getSelectedFileListItems() {
        int[] selectedIndexes = getSelectedIndices();
        boolean skipZero = isSelectedIndex(0) &&
                           mListItems[0].getName().equals("..");
        FileListItem[] selectedItems =
            new FileListItem[getSelectionCount()];

        // Don't count .. in the selected list
        int itemIndex = 0;
        for (int i = 0; i < selectedIndexes.length; i++) {
            if (selectedIndexes[i] != 0 || !skipZero) {
                selectedItems[itemIndex++] = mListItems[selectedIndexes[i]];
            }
        }

        return selectedItems;
    }

    /**
     * @return Number of items currently selected in the list, not counting ".." if it is selected
     */
    public int getSelectionCount() {
        int[] selectedIndexes = getSelectedIndices();
        // Don't count .. in the selected list
        if (isSelectedIndex( 0 ) && mListItems[0].getName().equals(".." )) {
            return selectedIndexes.length - 1;
        }
        return selectedIndexes.length;
    }

    /**
     * @return The FileListItem that matches the given String, displayed in the list, or null if it does not exist
     */
    public FileListItem getFileListItem(String name) {
        if (name != null) {
            ListModel m = getModel();
            for (int i = 0; i < m.getSize(); i++)
                if (name.equals(m.getElementAt(i)))
                    return mListItems[i];
        }
        
        return null;
    }

}
