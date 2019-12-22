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

package com.mindbright.terminal.tandem6530;

import com.mindbright.terminal.DisplayView;
import com.mindbright.terminal.SearchContext;


/** Interface that is implemented by the classes that implements the diffent
 * modes the terinal can be in, conversational, block and protected block mode.
 */
public interface Mode {
    public void keyHandler(char c, int virtualKey, int modifiers);
    public void hostChar(char c);
    public int getBufferRows();
    public void setDisplay(DisplayView display);
    public int getVisTop();
    public void switchReset();

    /* 2-8, 3-18 */
    public void doBackspace();
    /* 2-8, 3-18 */
    public void doHTab();
    public void doTab();
    /* 2-8 */
    public void doLineFeed();
    /* 2-8, 3-18 */
    public void doCarriageReturn();
    /* 3-15, 3-16 */
    public void doSetBufferAddress(int row, int column);
    /* 2-6, 3-14 */
    public void doSetCursorAddress(boolean displayedPage, int row, int column);
    /* 3-84 */
    public void doDefineFieldAttribute(int row, int column, boolean useFixed,
                                       int tableRow);
    /* 3-37 */
    public void doStartField(FieldAttributes attribs);
    /* 2-9, 3-19 */
    public void doSetTab();
    /* 2-9, 3-19 */
    public void doClearTab();
    /* 2-9, 3-19 */
    public void doClearAllTabs();
    /* 2-14, 3-23 */
    public void doSetVideoAttribute(int attrib);
    /* 2-15, 3-24 */
    public void doSetDefaultVideoAttribute(int attrib);
    /* 2-6, 3-15 */
    public String doReadCursorAddress();
    /* 3-19 */
    public void doBackTab();
    /* 3-12 */
    public void doSetMaxPageNumber(int n);
    /* 3-39, 3-40 */
    public void doDefineDataTypeTable(int startIndex, byte entries[]);
    /* 3-90 */
    public void doResetVariableTable();
    /* 3-90 */
    public void doDefineVariableTable(int startIndex,
                                      FieldAttributes attribs[]);
    /* 2-50 */
    public void doDefineEnterKeyFunction(char str[]);
    /* 2-7, 3-17 */
    public void doCursorUp();
    /* 2-8, 3-17 */
    public void doCursorRight();
    /* 2-8, 3-18 */
    public void doCursorHomeDown();
    /* 2-8, 3-18 */
    public void doCursorHome();
    /* 2-9 */
    public void doRollUp();
    /* 2-9 */
    public void doRollDown();
    /* 2-10 */
    public void doPageUp();
    /* 2-10 */
    public void doPageDown();
    /* 2-10 */
    public void doClearMemoryToSpaces();
    /* 3-48, 3-49 */
    public void doClearMemoryToSpaces(int startRow, int startCol,
                                      int endRow, int endColumn);
    /* 2-10, 3-49 */
    public void doEraseToEndOfPageOrMemory();
    /* 3-45, 3-46 */
    public String doReadWithAddress(int startRow, int startCol,
                                    int endRow, int endColumn);
    /* 2-11, 3-49 */
    public void doEraseToEndOfLineOrField();
    /* 3-46, 3-47 */
    public String doReadWithAddressAll(int startRow, int startCol,
                                       int endRow, int endColumn);
    /* 3-50 */
    public void doInsertLine();
    /* 3-50 */
    public void doDeleteLine();
    /* 3-92 */
    public void doDisableLocalLineEditing();
    /* 3-51 */
    public void doInsertCharacter();
    /* 3-51 */
    public void doDeleteCharacter();
    /* 3-38 */
    public void doResetModifiedDataTags();
    /* 3-44 */
    public String doReadWholePageOrBuffer();
    /* 2-10, 3-11 */
    public void doDisplayPage(int n);
    /* 3-12 */
    public void doSelectPage(int n);
    /* 3-38 */
    public void doStartFieldExtended(FieldAttributes attribs);

    public char[] getChars(int visTop, int row);
    public int[] getAttribs(int visTop, int row);
    public int getRow();
    public int getCol();
    public int getPage();

    public void doClickSelect(int row, int col, String selectDelims);
    public void resetClickSelect();
    public void setSelection(int row1, int col1, int row2, int col2);
    public void selectAll();
    public void resetSelection();
    public String getSelection(String eol);

    public SearchContext search(SearchContext lastContext, String key,
                                boolean reverse, boolean caseSens);
}

