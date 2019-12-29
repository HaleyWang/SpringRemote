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

/** Interface with callback methods when Term6530Parser has parsed
 *  a command or a character.
 */
public interface ActionHandler {
    /* 2-48, 3-84 */
    public void doBell();
    /* 2-8, 3-18 */
    public void doBackspace();
    /* 2-8, 3-18 */
    public void doHTab();
    /* 2-8 */
    public void doLineFeed();
    /* 2-8, 3-18 */
    public void doCarriageReturn();
    /* 1-12 */
    public void doSetConversationalMode();
    /* 1-12 */
    public void doSetBlockMode();
    /* 3-15, 3-16 */
    public void doSetBufferAddress(int row, int column);
    /* 3-72, 3-73 */
    public void doDataCompression(int n, char c);
    /* 2-6, 3-14 */
    public void doSetCursorAddress(int row, int column);
    /* 3-84 */
    public void doDefineFieldAttribute(int row, int column, boolean useFixed,
                                       int tableRow);
    /* 3-37 */
    public void doStartField(FieldAttributes attribs);
    /* 2-41, 3-77 */
    public void doPrintScreenOrPage();
    /* 2-9, 3-19 */
    public void doSetTab();
    /* 2-9, 3-19 */
    public void doClearTab();
    /* 2-9, 3-19 */
    public void doClearAllTabs();
    /* 2-14, 3-23 */
    public void doSetVideoAttributes(char videoAttrib);
    /* 2-15, 3-24 */
    public void doSetVideoPriorConditionRegister(char videoAttrib);
    /* 3-40 */
    public void doSet40CharLineWidth();
    /* 3-40 */
    public void doSet80CharLineWidth();
    /* 2-6, 3-15 */
    public void doReadCursorAddress();
    /* 2-49, 3-92 */
    public void doUnlockKeyboard();
    /* 2-49, 3-92 */
    public void doLockKeyboard();
    /* 2-33, 3-62 */
    public void doSetStringConfigurationParameter(String strs[]);
    /* 2-32, 3-61 */
    public void doReadStringConfigurationParameter(int n);
    /* 2-49, 3-92 */
    public void doSimulateFunctionKey(char keyCode);
    /* 2-40, 3-76 */
    public void doGetMachineName();
    /* 2-48, 3-84 */
    public void doDisconnectModem();
    /* 2-40, 3-76 */
    public void doGetCurrentDirectoryAndRedirectionInformation(char drive);
    /* 2-33, 3-63 */
    public void doReadVTLAUNCHConfigurationParameter(int param);
    /* 3-19 */
    public void doBackTab();
    /* 2-36, 3-65 */
    public void doRTMControl(int startStopEvent, int buckets[]);
    /* 2-37, 3-66 */
    public void doRTMDataUpload(int id[]);
    /* 3-67 */
    public void doSetEM3270Mode(int mode);
    /* 3-70 */
    public void doReadAllLocations();
    /* 3-71 */
    public void doReadKeyboardLatch();
    /* 2-49, 3-93 */
    public void doWriteToMessageField(char msg[], char attribs[]);
    /* 3-12 */
    public void doSetMaxPageNumber(int n);
    /* 2-50, 3-93 */
    public void doReinitialize();
    /* 2-19, 3-28 */
    public void doSetColorMapTable(int startIndex, byte entries[]);
    /* 2-19, 3-28 */
    public void doResetColorMapTable();
    /* 3-39, 3-40 */
    public void doDefineDataTypeTable(int startIndex, byte entries[]);
    /* 3-90 */
    public void doResetVariableTable();
    /* 3-90 */
    public void doDefineVariableTable(int startIndex,
                                      FieldAttributes attribs[]);
    /* 3-40 */
    public void doSet40CharactersScreenWidth();
    /* 2-23, 3-32 */
    public void doSetColorConfiguration(int startIndex, byte entries[]);
    /* 2-23, 3-32 */
    public void doResetColorConfiguration();
    /* 2-22, 3-31 */
    public void doReadColorConfiguration();
    /* 2-50 */
    public void doDefineEnterKeyFunction(char str[]);
    /* 2-30, 3-59 */
    public void doSetTerminalConfiguration(ConfigParameter params[]);
    /* 2-18, 3-27 */
    public void doRead6530ColorMappingTable();
    /* 2-21, 3-30 */
    public void doReadColorMappingTable();
    /* 2-31, 3-60 */
    public void doSetIODeviceConfiguration(int device, ConfigParameter parms[]);
    /* 3-32 */
    public void doSet6530ColorMapping(boolean setEnhanced);
    /* 2-31, 3-60 */
    public void doReadIODeviceConfiguration(int device);
    /* 2-50, 3-93 */
    public void doTerminateRemote6530Operation(int exitCode);
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
    public void doReadWithAddress(int startRow, int startCol,
                                  int endRow, int endColumn);
    /* 2-11, 3-49 */
    public void doEraseToEndOfLineOrField();
    /* 3-46, 3-47 */
    public void doReadWithAddressAll(int startRow, int startCol,
                                     int endRow, int endColumn);
    /* 3-50 */
    public void doInsertLine();
    /* 3-50 */
    public void doDeleteLine();
    /* 3-92 */
    public void doDisableLocalLineEditing();
    /* 3-51 */
    public void doInsertCharacter();
    /* 2-42, 3-78 */
    public void doWriteToAux1OrAux2Device(int device, char terminator);
    /* 3-51 */
    public void doDeleteCharacter();
    /* 3-34 */
    public void doReadScreenWithAllAttributes(int startRow, int startCol,
                                              int endRow, int endColumn);
    /* 2-46, 3-82 */
    public void doLoadAndExecuteAnOperatingSystemProgram(String execString);
    /* 3-35 */
    public void doEnterProtectedSubmode();
    /* 2-47, 3-83 */
    public void doReportExecReturnCode();
    /* 3-36 */
    public void doExitProtectedSubmode();
    /* 2-30, 3-57 */
    public void doReadTerminalConfiguration();
    /* 2-38, 3-74 */
    public void doReadTerminalStatus();
    /* 2-39, 3-75 */
    public void doReadFullRevisionLevel();
    /* 2-48, 3-91 */
    public void doDelayOneSecond();
    /* 3-38 */
    public void doResetModifiedDataTags();
    /* 3-44 */
    public void doReadWholePageOrBuffer();
    /* 2-10, 3-11 */
    public void doDisplayPage(int n);
    /* 3-12 */
    public void doSelectPage(int n);
    /* 3-33 */
    public void doStartEnhancedColorField(IBM3270FieldAttributes attribs);
    /* 3-38 */
    public void doStartFieldExtended(FieldAttributes attribs);
    /* 2-43, 3-79 */
    public void doWriteToFileOrDeviceName(String device, int opCode,
                                          byte data[]);
    /* 2-44, 3-80 */
    public void doWriteOrReadToFileOrDeviceName(String device, int opCode,
                                                byte data[]);
}

