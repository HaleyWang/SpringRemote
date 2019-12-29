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

import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;

import java.util.Properties;
import java.util.Enumeration;

import com.mindbright.terminal.DisplayView;
import com.mindbright.terminal.DisplayModel;
import com.mindbright.terminal.SearchContext;
import com.mindbright.terminal.Terminal;
import com.mindbright.terminal.TerminalOption;
import com.mindbright.terminal.TerminalWindow;

public class Terminal6530
    implements Terminal, DisplayModel, Terminal6530Callback, ActionHandler, AsciiCodes {


    private static String MY_NAME = "tn6530-8";

    /* I don't have the faintest idea of what kind of revision we are
     * trying to emulate here. Horizon's host seems to bark on major
     * revision 'B'. Minor revision 40 is used as an example in the spec.
     */
    private static String MY_REVISION = "G40";

    // Enable these for debug print outs
    private boolean DEBUG = false;
    private boolean LIMITED_DEBUG = false;

    // Options
    private static int OPT_BAUD_RATE                =  0;
    private static int OPT_BELL_COLUMN              =  1;
    private static int OPT_BELL_VOLUME              =  2;
    private static int OPT_CHAR_SET                 =  3;
    private static int OPT_CHAR_SIZE                =  4;
    private static int OPT_COLOR_SUPPORT            =  5;
    private static int OPT_COMPRESSION_ENHANCE      =  6;
    private static int OPT_CURSOR_TYPE              =  7;
    private static int OPT_DEFAULT_DEVICE_ID        =  8;
    private static int OPT_DUPLEX                   =  9;
    private static int OPT_EM3270_SUPPORT           = 10;
    private static int OPT_KEYBOARD                 = 11;
    private static int OPT_KEY_CLICK_VOLUME         = 12;
    private static int OPT_LANGUAGE                 = 13;
    private static int OPT_LOCAL_TRANSMIT_COLUMN    = 14;
    private static int OPT_MLAN_HOST_INIT           = 15;
    private static int OPT_NORMAL_INTENSITY         = 16;
    private static int OPT_PACKET_BLOCKING          = 17;
    private static int OPT_PARITY                   = 18;
    private static int OPT_RETURN_KEY_FUNCTION      = 19;
    private static int OPT_RTM_SUPPORT              = 20;
    private static int OPT_SAVE_CONFIG              = 21;
    private static int OPT_SCREEN_SAVER             = 22;
    private static int OPT_SINGLE_PAGE_SUBMODE      = 23;
    private static int OPT_STATUS_LINE_BORDER       = 24;
    private static int OPT_SWITCH_REFRESH_RATE      = 25;
    private static int OPT_SCREEN_FORMAT            = 26;
    private static int OPT_MODE                     = 27;
    private static int OPT_LAST_OPTION              = 28;
    private int options[] = new int[OPT_LAST_OPTION];

    private static int ATTR_6530_DIM_INTENSITY      = 0x01;
    private static int ATTR_6530_BLINKING           = 0x02;
    private static int ATTR_6530_REVERSE_VIDEO      = 0x04;
    private static int ATTR_6530_INVISIBLE          = 0x08;
    private static int ATTR_6530_UNDERSCORE         = 0x10;

    private static final int MODE_CONV          = 0;
    private static final int MODE_BLOCK         = 1;
    private static final int MODE_PROTECT_BLOCK = 2;
    private int currentMode;
    private Mode modes[];
    private ConvMode convMode;
    private BlockMode blockMode;
    private ProtectBlockMode protectBlockMode;

    private Parser hostParser = new Parser();
    private Parser keyboardParser = new Parser();

    private TerminalWindow termWin;
    private DisplayView display;

    private static int STATUS_LINE = 24;
    private static int COLUMNS = 80;
    private static int MAX_PAGES = 10;
    private StatusLine statusLine;

    private boolean colorSupport = false;
    private ColorMap colorMap;

    private boolean keyboardLocked = false;
    private boolean haveError = false;

    private boolean showStatusLine = true;

    private boolean enterIsFuctionKey = false;
    private boolean localLineEditing = true;
    private boolean viewBlankPage = false;
    private boolean powerUp = false;


    public Terminal6530() {

        statusLine = new StatusLine(this, DisplayModel.ATTR_CHARDRAWN);
        colorMap = new ColorMap();
        props = new Properties(defaultProperties);

        convMode = new ConvMode(this, keyboardParser);
        blockMode = new BlockMode(MAX_PAGES);
        protectBlockMode = new ProtectBlockMode(this, MAX_PAGES);

        modes = new Mode[3];
        modes[MODE_CONV] = convMode;
        modes[MODE_BLOCK] = blockMode;
        modes[MODE_PROTECT_BLOCK] = protectBlockMode;

        hostParser.setActionHandler(this);
        keyboardParser.setActionHandler(this);
        currentMode = MODE_BLOCK; // Just to get doSetConversationalMode to work
        doSetConversationalMode();


        // Set default options
        options[OPT_BAUD_RATE]                =  8;
        options[OPT_BELL_COLUMN]              = 72;
        options[OPT_BELL_VOLUME]              =  1;
        options[OPT_CHAR_SET]                 =  0;
        options[OPT_CHAR_SIZE]                =  1;
        options[OPT_COLOR_SUPPORT]            =  0;
        options[OPT_COMPRESSION_ENHANCE]      =  0;
        options[OPT_CURSOR_TYPE]              =  3;
        options[OPT_DEFAULT_DEVICE_ID]        =  0;
        options[OPT_DUPLEX]                   =  0;
        options[OPT_EM3270_SUPPORT]           =  0;
        options[OPT_KEYBOARD]                 =  7;
        options[OPT_KEY_CLICK_VOLUME]         =  0;
        options[OPT_LANGUAGE]                 =  0;
        options[OPT_LOCAL_TRANSMIT_COLUMN]    =  1;
        options[OPT_MLAN_HOST_INIT]           =  0;
        options[OPT_NORMAL_INTENSITY]         =  0;
        options[OPT_PACKET_BLOCKING]          =  1;
        options[OPT_PARITY]                   =  3;
        options[OPT_RETURN_KEY_FUNCTION]      =  0;
        options[OPT_RTM_SUPPORT]              =  0;
        options[OPT_SAVE_CONFIG]              =  0;
        options[OPT_SCREEN_SAVER]             =  0;
        options[OPT_SINGLE_PAGE_SUBMODE]      =  0;
        options[OPT_STATUS_LINE_BORDER]       =  0;
        options[OPT_SWITCH_REFRESH_RATE]      =  0;
        options[OPT_SCREEN_FORMAT]            =  0;
        options[OPT_MODE]                     =  0;
    }

    static private Properties defaultProperties;
    private Properties props;
    private boolean propsChanged;

    static private TerminalOption optColorMapping =
        new TerminalOption("color-mapping", "Use colors", "false");

    static private TerminalOption optionsDef[] = {
                optColorMapping,
            };

    static {
        int i;
        defaultProperties = new Properties();

        for (i = 0; i < optionsDef.length; i++) {
            defaultProperties.put(optionsDef[i].getKey(),
                                  optionsDef[i].getDefault());
        }
    }



    public static Terminal getTerminal(String name) {
        if (MY_NAME.equals(name)) {
            return new Terminal6530();
        }
        return null;
    }

    public static String[] getTerminalTypes() {
        String ret[] = { MY_NAME };
        return ret;
    }

    public boolean setTerminalType(String type) {
        return false;
    }

    public void setDumbMode(boolean dumb) {
        // Do nothing
    }

    //
    // ActionHandler interface
    //
    public void doBell() {
        debug("doBell");
        if (display != null) {
            display.doBell();
        }
    }
    public void doBackspace() {
        debug("doBackspace");
        modes[currentMode].doBackspace();
    }
    public void doHTab() {
        debug("doHTab");
        modes[currentMode].doHTab();
    }
    public void doLineFeed() {
        debug("doLineFeed");
        modes[currentMode].doLineFeed();
    }
    public void doCarriageReturn() {
        debug("doCarriageReturn");
        modes[currentMode].doCarriageReturn();
    }
    public void doSetConversationalMode() {
        if (currentMode == MODE_CONV) {
            // XXX Why several setConvMode in a row?
            // Something that needs resetting?
            return;
        }
        debug("doSetConversationalMode");
        hostParser.setConversationalMode();
        keyboardParser.setConversationalMode();
        setMode(MODE_CONV);
        statusLine.setStatus("CONV");
    }
    public void doSetBlockMode() {
        debug("doSetBlockMode");
        hostParser.setBlockMode();
        keyboardParser.setBlockMode();
        setMode(MODE_BLOCK);
        statusLine.setStatus("BLOCK");
    }
    public void doSetBufferAddress(int row, int column) {
        debug("doSetBufferAddress " + row + "," + column);
        modes[currentMode].doSetBufferAddress(row, column);
    }
    public void doDataCompression(int n, char c) {
        debug("doDataCompression "+n+"*'"+c+"'");
        for (int i = 0; i < n; i++) {
            modes[currentMode].hostChar(c);
        }
    }
    public void doSetCursorAddress(int row, int column) {
        debug("doSetCursorAddress " + row + "," + column);
        modes[currentMode].doSetCursorAddress(false, row, column);
    }
    public void doDefineFieldAttribute(int row, int column, boolean useFixed,
                                       int tableRow) {
        debug("doDefineFieldAttribute " + row + "," + column + " fixed="+
              useFixed + " row = " + tableRow);
        modes[currentMode].doDefineFieldAttribute(row, column, useFixed,
                                                  tableRow);
    }
    public void doStartField(FieldAttributes attribs) {
        debug("doStartField " + attribs);
        modes[currentMode].doStartField(attribs);
    }
    public void doPrintScreenOrPage() {
        notImplemented("doPrintScreenOrPage");
    }
    public void doSetTab() {
        debug("doSetTab");
        modes[currentMode].doSetTab();
    }
    public void doClearTab() {
        debug("doClearTab");
        modes[currentMode].doClearTab();
    }
    public void doClearAllTabs() {
        debug("doClearAllTabs");
        modes[currentMode].doClearAllTabs();
    }
    public void doSetVideoAttributes(char videoAttrib) {
        debug("doSetVideoAttributes");
        modes[currentMode].doSetVideoAttribute(videoAttrib & 0x1f);
    }
    public void doSetVideoPriorConditionRegister(char videoAttrib) {
        debug("doSetVideoPriorConditionRegister");
        modes[currentMode].doSetDefaultVideoAttribute(videoAttrib & 0x1f);
    }
    public void doSet40CharLineWidth() {
        debug("doSet40CharLineWidth");
    }
    public void doSet80CharLineWidth() {
        debug("doSet80CharLineWidth");
    }
    public void doReadCursorAddress() {
        debug("doReadCursorAddress");
        StringBuilder buf = new StringBuilder();
        buf.append(SOH).append('!');

        buf.append((char) (0x20 + modes[currentMode].getPage()));
        buf.append((char) (0x1f + modes[currentMode].getRow()));
        buf.append((char) (0x1f + modes[currentMode].getCol()));

        if (currentMode == MODE_CONV) {
            buf.append(CR);
        } else {
            buf.append(ETX);
            buf.append(LRC);
        }
        sendDirect(buf.toString());
    }
    public void doUnlockKeyboard() {
        debug("doUnlockKeyboard");
        keyboardLocked = false;
        statusLine.setStatus(currentMode == MODE_CONV ? "CONV" : "BLOCK");
    }
    public void doLockKeyboard() {
        debug("doLockKeyboard");
        keyboardLocked = true;
        statusLine.setStatus("KBD LOCKED");
    }
    public void doSetStringConfigurationParameter(String strs[]) {
        debug("doSetStringConfigurationParameter");
    }
    public void doReadStringConfigurationParameter(int n) {
        debug("doReadStringConfigurationParameter");
    }
    public void doSimulateFunctionKey(char keyCode) {
        debug("doSimulateFunctionKey");
        sendFunctionKey(keyCode);
    }
    public void doGetMachineName() {
        debug("doGetMachineName");
    }
    public void doDisconnectModem() {
        debug("doDisconnectModem");
    }
    public void doGetCurrentDirectoryAndRedirectionInformation(char drive) {
        debug("doGetCurrentDirectoryAndRedirectionInformation");
    }
    public void doReadVTLAUNCHConfigurationParameter(int param) {
        debug("doReadVTLAUNCHConfigurationParameter");
    }
    public void doBackTab() {
        debug("doBackTab");
        modes[currentMode].doBackTab();
    }
    public void doRTMControl(int startStopEvent, int buckets[]) {
        debug("doRTMControl");
        notImplemented("RTM, Control");
    }
    public void doRTMDataUpload(int id[]) {
        debug("doRTMDataUpload");
        notImplemented("RTM, Data Upload");
    }
    public void doSetEM3270Mode(int mode) {
        debug("doSetEM3270Mode");
        notImplemented("EM3270, Set EM3270 mode");
    }
    public void doReadAllLocations() {
        debug("doReadAllLocations");
        notImplemented("EM3270, Read All Locations");
    }
    public void doReadKeyboardLatch() {
        debug("doReadKeyboardLatch");
        notImplemented("EM3270, Read Keyboard Latch");
    }
    public void doWriteToMessageField(char msg[], char attribs[]) {
        debug("doWriteToMessageField");
        statusLine.setMessage(msg, attribs);
    }
    public void doSetMaxPageNumber(int n) {
        debug("doSetMaxPageNumber");
        modes[currentMode].doSetMaxPageNumber(n);
    }
    public void doReinitialize() {
        debug("doReinitialize");
        doSetBlockMode();
    }
    public void doSetColorMapTable(int startIndex, byte entries[]) {
        debug("doSetColorMapTable");
        colorMap.set(startIndex, entries);
    }
    public void doResetColorMapTable() {
        debug("doResetColorMapTable");
        colorMap.reset();
    }
    public void doReadColorMappingTable() {
        debug("doReadColorMappingTable");

        StringBuilder buf = new StringBuilder();
        buf.append(SOH).append('0');
        buf.append(colorMap.read());

        if (currentMode == MODE_CONV) {
            buf.append(CR);
        } else {
            buf.append((char) (0x20 + MAX_PAGES));
            buf.append(ETX);
            buf.append((char) 0x00);
        }
        sendDirect(buf.toString());
    }
    public void doDefineDataTypeTable(int startIndex, byte entries[]) {
        debug("doDefineDataTypeTable");
        modes[currentMode].doDefineDataTypeTable(startIndex, entries);
    }
    public void doResetVariableTable() {
        debug("doResetVariableTable");
        modes[currentMode].doResetVariableTable();
    }
    public void doDefineVariableTable(int startIndex,
                                      FieldAttributes attribs[]) {
        debug("doDefineVariableTable");
        modes[currentMode].doDefineVariableTable(startIndex, attribs);
    }
    public void doSet40CharactersScreenWidth() {
        debug("doSet40CharactersScreenWidth");
    }
    public void doSetColorConfiguration(int startIndex, byte entries[]) {
        debug("doSetColorConfiguration");
        notImplemented("Color, Set Color Configuration");
    }
    public void doResetColorConfiguration() {
        debug("doResetColorConfiguration");
        notImplemented("Color, Reset Color Configuration");
    }
    public void doReadColorConfiguration() {
        debug("doReadColorConfiguration");
        notImplemented("Color, Read Color Configuration");
    }
    public void doDefineEnterKeyFunction(char str[]) {
        debug("doDefineEnterKeyFunction");
        convMode.setEnterKeyFunction(str);
    }

    public void doSetTerminalConfiguration(ConfigParameter params[]) {
        debug("doSetTerminalConfiguration");
        if (params == null) {
            return;
        }

        int value;
        for (int i = 0; i < params.length; i++) {
            switch (params[i].getCode()) {
            case 'A':
                value = params[i].getValue();
                if (value == 1 || value == 3) {
                    options[OPT_CURSOR_TYPE] = value;
                }
                break;
            case 'B':
                value = params[i].getValue();
                if (0 <= value && value <= 80) {
                    options[OPT_BELL_COLUMN] = value;
                }
                break;
            case 'C':
                value = params[i].getValue();
                if (value == 0 || value == 10) {
                    options[OPT_BELL_VOLUME] = value;
                }
                break;
            case 'D':
                notImplemented("Setting of keyboard click");
                break;
            case 'E':
                // Status line border is read only
                break;
            case 'F':
                value = params[i].getValue();
                if ((0 <= value && value <= 14 && value != 9) ||
                        (19 <= value && value <= 32 && value != 28)) {
                    options[OPT_LANGUAGE] = value;
                }
                break;
            case 'G':
                value = params[i].getValue();
                if (value == 0 || value == 3) {
                    options[OPT_MODE] = value;
                }
                break;
            case 'H':
                value = params[i].getValue();
                if (0 <= value && value <= 16) {
                    options[OPT_BAUD_RATE] = value;
                }
                break;
            case 'I':
                value = params[i].getValue();
                if (1 <= value && value <= 3) {
                    options[OPT_PARITY] = value;
                }
                break;
            case 'J':
                value = params[i].getValue();
                if (value == 0 || value == 1) {
                    options[OPT_DUPLEX] = value;
                    convMode.setHalfDuplex((value == 1) ? true : false);
                }
                break;
            case 'K':
                // Default device ID is read only
                break;
            case 'L':
                value = params[i].getValue();
                if (0 <= value && value <= 8) {
                    options[OPT_PACKET_BLOCKING] = value;
                }
                break;
            case 'M':
                value = params[i].getValue();
                if (value == 1) {
                    enterIsFuctionKey = true;
                } else if (value == 0) {
                    enterIsFuctionKey = false;
                }
                break;
            case 'N':
                // Single page submode is read only
                break;
            case 'O':
                // Save config is read only
                break;
            case 'P':
                value = params[i].getValue();
                if (0 <= value && value <= 20) {
                    options[OPT_SCREEN_SAVER] = value;
                }
                break;
            case 'S':
                // Screen format is read only
                break;
            case 'T':
                value = params[i].getValue();
                if (value == 0 || value == 1) {
                    options[OPT_NORMAL_INTENSITY] = value;
                }
                break;
            case 'U':
                value = params[i].getValue();
                if (0 <= value && value <= 80) {
                    if (value == 0) {
                        // 0 is valid acording to 2-27, but what is it
                        // supposed to do?
                        value = 1;
                    }
                    options[OPT_LOCAL_TRANSMIT_COLUMN] = value;
                    convMode.setLocalTransmitColumn(value);
                }
                break;
            case 'V':
                value = params[i].getValue();
                if (value == 0 || value == 1) {
                    options[OPT_CHAR_SIZE] = value;
                }
                break;
            case 'W':
                // Char set is read only
                break;
            case 'X':
                // Keyboard is read only
                break;
            case 'e':
                notImplemented("Setting of color support");
                break;
            case 'f':
                notImplemented("Setting of compression enhance");
                break;
            case 'i':
                // What's this??
                notImplemented("Setting of MLAN host init IXF");
                break;
            case 'j':
                notImplemented("Setting of RTM support");
                break;
            case 'h':
                notImplemented("Setting of EM3270 support");
                break;
            default:
                debug("Got unknown configuration code " +
                      (int) params[i].getCode() + ".");
            }
        }
    }
    public void doRead6530ColorMappingTable() {
        notImplemented("Color, Read 6530 Color Mapping Table");
    }
    public void doSetIODeviceConfiguration(int device, ConfigParameter parms[]) {
        debug("doSetIODeviceConfiguration");
        notImplemented("Aux*, Set IO Device Configuration");
    }
    public void doSet6530ColorMapping(boolean setEnhanced) {
        debug("doSet6530ColorMapping");
        notImplemented("EM3270, Set 6530 Color Mapping");
    }
    public void doReadIODeviceConfiguration(int device) {
        debug("doReadIODeviceConfiguration");
        notImplemented("Aux*, Read IO Device Configuration");
    }
    public void doTerminateRemote6530Operation(int exitCode) {
        debug("doTerminateRemote6530Operation");
        notImplemented("OS stuff, Terminate Remote 6530 Operation");
    }
    public void doCursorUp() {
        debug("doCursorUp");
        modes[currentMode].doCursorUp();
    }
    public void doCursorRight() {
        debug("doCursorRight");
        modes[currentMode].doCursorRight();
    }
    public void doCursorHomeDown() {
        debug("doCursorHomeDown");
        modes[currentMode].doCursorHomeDown();
    }
    public void doCursorHome() {
        debug("doCursorHome");
        modes[currentMode].doCursorHome();
    }
    public void doRollUp() {
        debug("doRollUp");
        modes[currentMode].doRollUp();
    }
    public void doRollDown() {
        debug("doRollDown");
        modes[currentMode].doRollDown();
    }
    public void doPageUp() {
        debug("doPageUp");
        modes[currentMode].doPageUp();
    }
    public void doPageDown() {
        debug("doPageDown");
        modes[currentMode].doPageDown();
    }
    public void doClearMemoryToSpaces() {
        debug("doClearMemoryToSpaces");
        modes[currentMode].doClearMemoryToSpaces();
    }
    public void doClearMemoryToSpaces(int startRow, int startCol,
                                      int endRow, int endColumn) {
        debug("doClearMemoryToSpaces " + startRow + "," + startCol +
              " - " + endRow + "," + endColumn);
        modes[currentMode].doClearMemoryToSpaces(startRow, startCol,
                                                 endRow, endColumn);
    }
    public void doEraseToEndOfPageOrMemory() {
        debug("doEraseToEndOfPageOrMemory");
        modes[currentMode].doEraseToEndOfPageOrMemory();
    }
    public void doReadWithAddress(int startRow, int startCol,
                                  int endRow, int endColumn) {
        debug("doReadWithAddress " + startRow + "x" + startCol + " - " +
              endRow +"x" + endColumn);
        sendDirect(modes[currentMode].doReadWithAddress(startRow, startCol,
                                                        endRow, endColumn));
    }
    public void doEraseToEndOfLineOrField() {
        debug("doEraseToEndOfLineOrField");
        modes[currentMode].doEraseToEndOfLineOrField();
    }
    public void doReadWithAddressAll(int startRow, int startCol,
                                     int endRow, int endColumn) {
        debug("doReadWithAddressAll");
        sendDirect(modes[currentMode].doReadWithAddressAll(startRow, startCol,
                                                           endRow, endColumn));
    }
    public void doInsertLine() {
        debug("doInsertLine");
        modes[currentMode].doInsertLine();
    }
    public void doDeleteLine() {
        debug("doDeleteLine");
        modes[currentMode].doDeleteLine();
    }
    public void doDisableLocalLineEditing() {
        debug("doDisableLocalLineEditing");
        localLineEditing = false;
    }
    public void doInsertCharacter() {
        debug("doInsertCharacter");
        modes[currentMode].doInsertCharacter();
    }
    public void doWriteToAux1OrAux2Device(int device, char terminator) {
        debug("doWriteToAux1OrAux2Device");
        notImplemented("Aux*, Write To Aux1 Or Aux2 Device");
    }
    public void doDeleteCharacter() {
        debug("doDeleteCharacter");
        modes[currentMode].doDeleteCharacter();
    }
    public void doReadScreenWithAllAttributes(int startRow, int startCol,
            int endRow, int endColumn) {
        debug("doReadScreenWithAllAttributes");
        notImplemented("EM3270, Read Screen With All Attributes");
    }
    public void doLoadAndExecuteAnOperatingSystemProgram(String execString) {
        debug("doLoadAndExecuteAnOperatingSystemProgram");
        notImplemented("OS stuff, Load And Execute An OS Program");
    }
    public void doEnterProtectedSubmode() {
        debug("doEnterProtectedSubmode");
        setMode(MODE_PROTECT_BLOCK);
    }
    public void doExitProtectedSubmode() {
        debug("doExitProtectedSubmode");
        setMode(MODE_BLOCK);
    }
    public void doReportExecReturnCode() {
        debug("doReportExecReturnCode");
        notImplemented("OS stuff, Report Exec Return Code");
    }

    public void doReadTerminalConfiguration() {
        debug("doReadTerminalConfiguration");
        String ret = SOH + "!" +
                     configString('A', options[OPT_CURSOR_TYPE])  +
                     configString('B', options[OPT_BELL_COLUMN])  +
                     configString('C', options[OPT_BELL_VOLUME])  +
                     configString('D', options[OPT_KEY_CLICK_VOLUME])  +
                     configString('E', options[OPT_STATUS_LINE_BORDER])  +
                     configString('F', options[OPT_LANGUAGE])  +
                     configString('G', options[OPT_MODE])  +
                     configString('H', options[OPT_BAUD_RATE])  +
                     configString('I', options[OPT_PARITY])  +
                     configString('J', options[OPT_DUPLEX])  +
                     configString('K', options[OPT_DEFAULT_DEVICE_ID])  +
                     configString('L', options[OPT_PACKET_BLOCKING])  +
                     configString('M', options[OPT_RETURN_KEY_FUNCTION])  +
                     configString('N', options[OPT_SINGLE_PAGE_SUBMODE])  +
                     configString('O', options[OPT_SAVE_CONFIG])  +
                     configString('P', options[OPT_SCREEN_SAVER])  +
                     configString('S', options[OPT_SCREEN_FORMAT])  +
                     configString('T', options[OPT_NORMAL_INTENSITY])  +
                     configString('U', options[OPT_LOCAL_TRANSMIT_COLUMN])  +
                     configString('V', options[OPT_CHAR_SIZE])  +
                     configString('W', options[OPT_CHAR_SET])  +
                     configString('X', options[OPT_KEYBOARD])  +
                     configString('e', options[OPT_COLOR_SUPPORT])  +
                     configString('f', options[OPT_COMPRESSION_ENHANCE])  +
                     configString('i', options[OPT_MLAN_HOST_INIT])  +
                     configString('j', options[OPT_RTM_SUPPORT]);

        switch (options[OPT_EM3270_SUPPORT]) {
        case  0:
            ret += "h00";
            break;
        case  1:
            ret += "h01";
            break;
        case 10:
            ret += "h10";
            break;
        case 11:
            ret += "h11";
            break;
        }

        if (currentMode == MODE_CONV) {
            ret += CR;
        } else {
            ret += ETX;
            ret += LRC;
        }

        sendDirect(ret);
    }
    public void doReadTerminalStatus() {
        debug("doReadTerminalStatus");
        StringBuilder buf = new StringBuilder();
        buf.append(SOH).append('?');
        if (!powerUp) {
            buf.append((char) (0x40 + 0x01));
            powerUp = true;
        } else {
            buf.append((char) (0x40 + 0x03));
        }
        buf.append('F');
        buf.append(MY_REVISION.charAt(0));

        if (currentMode == MODE_CONV) {
            buf.append(CR);
        } else {
            // We support 4094 fields per page
            buf.append((char) (0x20 + MAX_PAGES));
            buf.append('?');
            buf.append('>');
            buf.append(ETX);
            buf.append(LRC);
        }
        sendDirect(buf.toString());
    }

    public void doReadFullRevisionLevel() {
        debug("doReadFullRevisionLevel");
        StringBuilder buf = new StringBuilder();
        buf.append(SOH).append('#');
        buf.append(MY_REVISION);
        buf.append("T0");
        buf.append(MY_REVISION);

        if (currentMode == MODE_CONV) {
            buf.append(CR);
        } else {
            buf.append(ETX);
            buf.append(LRC);
        }
        sendDirect(buf.toString());
    }

    public void doDelayOneSecond() {
        debug("doDelayOneSecond");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {}
    }
    public void doResetModifiedDataTags() {
        debug("doResetModifiedDataTags");
        modes[currentMode].doResetModifiedDataTags();
    }
    public void doReadWholePageOrBuffer() {
        debug("doReadWholePageOrBuffer");
        sendDirect(modes[currentMode].doReadWholePageOrBuffer());
    }
    public void doDisplayPage(int n) {
        debug("doDisplayPage " + n);
        if (n == 0) {
            viewBlankPage = true;
            doLockKeyboard();
            repaint();
            statusLine.setMessage("blank page");
        } else {
            viewBlankPage = false;
            modes[currentMode].doDisplayPage(n);
            statusLine.setMessage("");
        }
    }
    public void doSelectPage(int n) {
        debug("doSelectPage " + n);
        modes[currentMode].doSelectPage(n);
    }
    public void doStartEnhancedColorField(IBM3270FieldAttributes attribs) {
        debug("doStartEnhancedColorField");
        notImplemented("EM3270, Enhanced Color Field");
    }
    public void doStartFieldExtended(FieldAttributes attribs) {
        debug("doStartFieldExtended");
        modes[currentMode].doStartFieldExtended(attribs);
    }
    public void doWriteToFileOrDeviceName(String device, int opCode,
                                          byte data[]) {
        debug("doWriteToFileOrDeviceName");
        notImplemented("Write to file or device");
    }
    public void doWriteOrReadToFileOrDeviceName(String device, int opCode,
            byte data[]) {
        debug("doWriteOrReadToFileOrDeviceName");
        notImplemented("Write/read to file or device");
    }


    //
    // Terminal interface
    //
    public String terminalType() {
        debug("terminalType");
        return MY_NAME;
    }
    public void reset() {
        debug("reset");
        modes[currentMode].switchReset();
    }
    public int getRows() {
        debug("getRows");
        return STATUS_LINE + 1;
    }
    public int getCols() {
        debug("getCols");
        return COLUMNS;
    }

    public synchronized void keyHandler(char c, int virtualKey, int modifiers) {
        debug("keyHandler: '"+c+"' ("+(int) c+") "+virtualKey+" "+modifiers);
        if (haveError) {
            haveError = false;
            statusLine.resetError();
        }

        // Possible to reset terminal even if keyboard is locked
        if (virtualKey == KeyEvent.VK_BACK_SPACE) {
            if ((modifiers & InputEvent.CTRL_MASK) != 0) {
                softReset();
                return;
            } else if ((modifiers & InputEvent.ALT_MASK) != 0) {
                programReset();
                return;
            }
        }

        if (virtualKey == KeyEvent.VK_5) {
            if ((modifiers & InputEvent.CTRL_MASK) != 0) {
                DEBUG = !DEBUG;
                debug("Turning debug on...");
                return;
            }
        }

        if (keyboardLocked) {
            debug("keyboard locked");
            return;
        }

        if (virtualKey == KeyEvent.VK_PAGE_UP &&
                (modifiers & InputEvent.CTRL_MASK) == InputEvent.CTRL_MASK) {
            showStatusLine = true;
            updateLine(STATUS_LINE);
            return;
        }

        if (virtualKey == KeyEvent.VK_PAGE_DOWN &&
                (modifiers & InputEvent.CTRL_MASK) == InputEvent.CTRL_MASK) {
            showStatusLine = false;
            updateLine(STATUS_LINE);
            return;
        }

        if (handleFunctionKey(c, virtualKey, modifiers)) {
            // Was function key
            return;
        }

        if (virtualKey == KeyEvent.VK_PAUSE ||
                virtualKey == KeyEvent.VK_SCROLL_LOCK ) {
            // Send break
            termWin.sendBreak();
            return;
        }

        modes[currentMode].keyHandler(c, virtualKey, modifiers);

    }

    public void mouseHandler(int visTop, int x, int y, boolean press,
                             int modifiers) {
        debug("mouseHandler");
        if (keyboardLocked) {
            return;
        }

        if (x == STATUS_LINE) {
            return;
        }

        if (!press) {
            modes[currentMode].doSetCursorAddress(true, x + 1, y + 1);
        }
    }

    private boolean doupdate = true;
    public void setUpdate(boolean enabled) {
        doupdate = enabled;
    }

    public synchronized void fromHost(char c) {
        char parsed = Parser.IGNORE;

        try {
            parsed = hostParser.parse(c);
        } catch (ParseException e) {
            error(e.getMessage());
            return;
        }

        if (parsed != Parser.IGNORE) {
            //debug("fromHost: '"+c+"' (" + (int) c +") sent to currentMode");
            modes[currentMode].hostChar(c);
        } else {
            //debug("fromHost: '"+c+"' (" + (int) c +") consumed by parser");
        }
    }
    public void setTerminalWindow(TerminalWindow termWin) {
        debug("setTerminalWindow");
        this.termWin = termWin;
    }
    public void setDisplay(DisplayView display) {
        debug("setDisplay");
        this.display = display;
        statusLine.setDisplay(display);
        modes[currentMode].setDisplay(display);
        if (display != null) {
            display.setModel(this);
            display.setGeometry(STATUS_LINE + 1, COLUMNS);
        }

    }

    public void close() {
        display = null;
        termWin = null;
    }

    public static TerminalOption[] getTerminalOptions() {
        return optionsDef;
    }

    public void setProperties(Properties newProps) {
        debug("setProperties");
        Enumeration<?> e = newProps.keys();
        while(e.hasMoreElements()) {
            String name  = (String)e.nextElement();
            String value = newProps.getProperty(name);
            setProperty(name, value);
        }
    }

    public boolean setProperty(String key, String value) {
        return setProperty(key, value, false);
    }

    public boolean setProperty(String key, String value, boolean forceSet) {
        debug("setProperty");
        boolean isEqual = false;
        String val = getProperty(key);
        boolean boolVal = Boolean.valueOf(value).booleanValue();

        if(val != null && val.equals(value)) {
            isEqual = true;
            if(!forceSet)
                return true;
        }

        if (optColorMapping.getKey().equals(key)) {
            if(!value.equals(val)) {
                colorSupport = boolVal;
                if (display != null) {
                    display.updateDirtyArea(0, 0, STATUS_LINE + 1, COLUMNS);
                    if (doupdate)
                        display.repaint();
                }
            }
        } else {
            return false;
        }
        props.put(key, value);

        if(!isEqual) {
            propsChanged = true;
        }
        return true;
    }
    public Properties getProperties() {
        debug("getProperties");
        return props;
    }
    public String getProperty(String key) {
        debug("getProperty");
        return props.getProperty(key);
    }
    public boolean getPropsChanged() {
        debug("getPropsChanged");
        return propsChanged;
    }
    public void setPropsChanged(boolean value) {
        debug("setPropsChanged");
        propsChanged = value;
    }
    public String getDefaultProperty(String key) {
        debug("getDefaultProperty");
        return defaultProperties.getProperty(key);
    }
    public TerminalOption[] getOptions() {
        debug("getOptions");
        return optionsDef;
    }

    public SearchContext search(SearchContext lastContext, String key,
                                boolean reverse, boolean caseSens) {
        debug("search: key='"+key+"'");
        return modes[currentMode].search(lastContext, key, reverse, caseSens);
    }

    // These methods are used when the terminal is started, where
    // the user types in where he wants to connect
    private void makeTerminalReady() {
        if (currentMode != MODE_CONV) {
            doSetConversationalMode();
        }
    }

    public void setAttributeBold(boolean set) {
        debug("setAttributeBold");
        makeTerminalReady();
        if (set
           ) {
            doSetVideoAttributes('"');
        }
        else {
            doSetVideoAttributes(' ');
        }
    }
    public void clearScreen() {
        debug("clearScreen");
        makeTerminalReady();
        modes[currentMode].doClearMemoryToSpaces();
    }
    public void ringBell() {
        debug("ringBell");
        doBell();
    }
    public void setCursorPos(int row, int col) {
        debug("setCursorPos");
        makeTerminalReady();
        modes[currentMode].doSetCursorAddress(true, row + 1, col + 1);
    }
    public void clearLine() {
        debug("clearLine");
        makeTerminalReady();
        int currentRow = modes[currentMode].getRow();
        modes[currentMode].doSetCursorAddress(true, currentRow, 1);
        modes[currentMode].doEraseToEndOfLineOrField();
    }

    public boolean setSize(int rows, int cols) {
        debug("setSize");
        if (display != null) {
            // Ignore new size, we this just in case the font size has
            // changed.
            display.setGeometry(getRows(), getCols());
        }
        return true;
    }
    public boolean setSaveLines(int lines) {
        debug("setSaveLines");
        return true;
    }
    public void clearSaveLines() {
        debug("clearSaveLines");
    }

    public void paste(String selection) {
        debug("paste");
        if (selection == null) {
            return;
        }
        for (int i = 0; i < selection.length(); i++) {
            keyHandler(selection.charAt(i), 0, 0);
        }
    }
    public void doClickSelect(int visTop, int row, int col,
                              String selectDelims) {
        debug("doClickSelect");
        if (row == STATUS_LINE) {
            return;
        }
        modes[currentMode].doClickSelect(visTop + row, col, selectDelims);
    }
    public String getSelection(String eol) {
        debug("getSelection");
        return modes[currentMode].getSelection(eol);
    }
    public void setSelection(int visTop, int row1, int col1,
                             int row2, int col2) {
        debug("setSelection1");
        if (row1 == STATUS_LINE || row2 == STATUS_LINE)
            return;
        setSelection(visTop + row1, col1, visTop + row2, col2);
    }
    public void setSelection(int row1, int col1, int row2, int col2) {
        debug("setSelection2");
        modes[currentMode].setSelection(row1, col1, row2, col2);
    }
    public void selectAll() {
        debug("selectAll");
        modes[currentMode].selectAll();
    }
    public void resetSelection() {
        debug("resetSelection");
        modes[currentMode].resetSelection();
    }
    public void resetClickSelect() {
        debug("resetClickSelect");
        modes[currentMode].resetClickSelect();
    }
    public void setInputCharset(String charset) {
        debug("setInputCharset");
    }

    //
    // DisplayModel interface
    //
    public char[] getChars(int visTop, int row) {
        if (!viewBlankPage && 0 <= row && row <= (STATUS_LINE - 1)) {
            return modes[currentMode].getChars(visTop, row);
        } else if (row == STATUS_LINE && showStatusLine) {
            return statusLine.getChars();
        }
        return null;
    }
    public int[] getAttribs(int visTop, int row) {
        if (!viewBlankPage && 0 <= row && row <= (STATUS_LINE - 1)) {
            int attribs[] = modes[currentMode].getAttribs(visTop, row);
            if (attribs == null) {
                return null;
            }
            int ret[] = new int[attribs.length];
            for (int i = 0; i < ret.length; i++) {

                if (colorSupport) {
                    ret[i] = colorMap.map(attribs[i]);
                    continue;
                }

                ret[i] = ATTR_CHARDRAWN;
                if ((attribs[i] & ATTR_6530_DIM_INTENSITY) != 0) {
                    ret[i] |= ATTR_LOWINTENSITY;
                }
                if ((attribs[i] & ATTR_6530_BLINKING) != 0) {
                    ret[i] |= ATTR_BLINKING;
                }
                if ((attribs[i] & ATTR_6530_REVERSE_VIDEO) != 0) {
                    ret[i] |= ATTR_INVERSE;
                }
                if ((attribs[i] & ATTR_6530_INVISIBLE) != 0) {
                    ret[i] |= ATTR_INVISIBLE;
                }
                if ((attribs[i] & ATTR_6530_UNDERSCORE) != 0) {
                    /* According to the spec., when the Video Attribute
                     * Character contains underscore, the VAC should not
                     * contain underscore. This is an approximation (it
                     * don't work for field accross more than one screen
                     * line) of that behaviour.
                     */
                    if (i > 0) {
                        if ((attribs[i-1] & ATTR_6530_UNDERSCORE) != 0) {
                            ret[i] |= ATTR_UNDERLINE;
                        }
                    } else {
                        /* Always show underline for first char on a line. */
                        ret[i] |= ATTR_UNDERLINE;
                    }
                }
            }
            return ret;
        } else if (row == STATUS_LINE && showStatusLine) {
            return statusLine.getAttribs();
        }
        return null;
    }
    public int getDisplayRows() {
        debug("getDisplayRows");
        return getRows();
    }
    public int getDisplayCols() {
        debug("getDisplayCols");
        return getCols();
    }
    public int getBufferRows() {
        debug("getBufferRows");
        return modes[currentMode].getBufferRows() + 1;
    }

    //
    // Terminal6530Callback interface
    public void send(char c) {
        debug("send: '"+c+"' ("+(int)c+")");
        if (termWin != null) {
            termWin.typedChar(c);
        }
    }

    public void send(String str) {
        debug("send: "+str);
        if (termWin != null && str != null) {
            char ca[] = str.toCharArray();
            if (DEBUG) {
                StringBuilder buf = new StringBuilder();
                for (int i = 0; i < ca.length; i++) {
                    if (Character.isLetterOrDigit(ca[i]) || ca[i] == ' ') {
                        buf.append(ca[i]);
                    } else {
                        buf.append('.');
                    }
                    buf.append(" (");
                    buf.append(Integer.toHexString(ca[i]));
                    buf.append(")\n");
                }
                debug(buf.toString());
            }

            termWin.sendBytes(str.getBytes());
        }
    }

    public void sendDirect(String str) {
        debug("sendDirect: "+str);
        if (termWin != null && str != null) {
            char ca[] = str.toCharArray();
            if (DEBUG) {
                StringBuilder buf = new StringBuilder();
                for (int i = 0; i < ca.length; i++) {
                    if (Character.isLetterOrDigit(ca[i]) || ca[i] == ' ') {
                        buf.append(ca[i]);
                    } else {
                        buf.append('.');
                    }
                    buf.append(" (");
                    buf.append(Integer.toHexString(ca[i]));
                    buf.append(")\n");
                }
                debug(buf.toString());
            }
            termWin.sendBytesDirect(str.getBytes());
        }
    }

    public void error(String msg) {
        debug("error: " + msg);
        haveError = true;
        statusLine.setError(msg);
        doBell();
    }

    public void statusLineUpdated() {
        updateLine(STATUS_LINE);
    }


    //
    // private methods
    //
    private String configString(char code, int value) {
        if (value < 10) {
            return code + " " + value;
        } else if (10 <= value && value < 100) {
            return code + String.valueOf(value);
        } else if (100 <= value && value < 1000) {
            return code + String.valueOf(value);
        } else {
            return "";
        }
    }

    private void debug(String msg) {
        if (DEBUG) {
            System.out.println(msg);
        }
    }
    private void notImplemented(String msg) {
        if (LIMITED_DEBUG) {
            System.out.println("Not implemented: " + msg);
        }
    }

    private void softReset() {
        // 1-14
        debug("softReset (can unlock keyboard: "+(!viewBlankPage)+")");
        if (!viewBlankPage) {
            doUnlockKeyboard();
        }
        hostParser.reset();
        keyboardParser.reset();
    }

    private void programReset() {
        // 1-14
        notImplemented("programReset()");
    }

    private boolean haveAlt(int modifiers) {
        return haveModifiers(modifiers, InputEvent.ALT_MASK);
    }
    private boolean isAlt(int modifiers) {
        return modifiers == InputEvent.ALT_MASK;
    }
    private boolean haveShift(int modifiers) {
        return haveModifiers(modifiers, InputEvent.SHIFT_MASK);
    }
    private boolean isShift(int modifiers) {
        return modifiers == InputEvent.SHIFT_MASK;
    }
    private boolean haveCtrl(int modifiers) {
        return haveModifiers(modifiers, InputEvent.CTRL_MASK);
    }
    private boolean isCtrl(int modifiers) {
        return modifiers == InputEvent.CTRL_MASK;
    }
    private boolean haveNone(int modifiers) {
        return modifiers == 0;
    }
    private boolean haveModifiers(int modifiers, int mask) {
        return (modifiers & mask) == mask;
    }

    private boolean handleFunctionKey(char c, int virtualKey, int modifiers) {
        char sendC = (char) 0x00;

        if (!haveCtrl(modifiers)) {
            switch (virtualKey) {
            case KeyEvent.VK_F1:
                if (haveAlt(modifiers)) {
                    sendC = 'J';    // F11
                } else {
                    sendC = '@';
                }
                break;
            case KeyEvent.VK_F2:
                if (haveAlt(modifiers)) {
                    sendC = 'K';    // F12
                } else {
                    sendC = 'A';
                }
                break;
            case KeyEvent.VK_F3:
                if (haveAlt(modifiers)) {
                    sendC = 'L';    // F13
                } else {
                    sendC = 'B';
                }
                break;
            case KeyEvent.VK_F4:
                if (haveAlt(modifiers)) {
                    sendC = 'M';    // F14
                } else {
                    sendC = 'C';
                }
                break;
            case KeyEvent.VK_F5:
                if (haveAlt(modifiers)) {
                    sendC = 'N';    // F15
                } else {
                    sendC = 'D';
                }
                break;
            case KeyEvent.VK_F6:
                if (haveAlt(modifiers)) {
                    sendC = 'O';    // F16
                } else {
                    sendC = 'E';
                }
                break;
            case KeyEvent.VK_F7:
                sendC = 'F';
                break;
            case KeyEvent.VK_F8:
                sendC = 'G';
                break;
            case KeyEvent.VK_F9:
                sendC = 'H';
                break;
            case KeyEvent.VK_F10:
                sendC = 'I';
                break;
            case KeyEvent.VK_F11:
                sendC = 'J';
                break;
            case KeyEvent.VK_F12:
                sendC = 'K';
                break;
            }

// XXX
//             if (sendC != 0) {
//                 if (haveShift(modifiers)) {
//                     sendC += 0x20;
//                 }
//             }
        }

        if (sendC == 0 && currentMode != MODE_CONV) {
            switch (virtualKey) {
            case KeyEvent.VK_UP:
                if (haveAlt(modifiers)) {
                    if (!haveShift(modifiers)) {
                        sendC = 'P';
                    } else {
                        sendC = 'p';
                    }
                }
                break;

            case KeyEvent.VK_DOWN:
                if (haveAlt(modifiers)) {
                    if (!haveShift(modifiers)) {
                        sendC = 'Q';
                    } else {
                        sendC = 'q';
                    }
                }
                break;

            case KeyEvent.VK_PAGE_DOWN:
                if (haveNone(modifiers)) {
                    sendC = 'R';
                } else if (isAlt(modifiers)) {
                    sendC = 'r';
                }
                break;

            case KeyEvent.VK_PAGE_UP:
                if (haveNone(modifiers)) {
                    sendC = 'S';
                } else if (isAlt(modifiers)) {
                    sendC = 's';
                }
                break;

            case KeyEvent.VK_INSERT:
                if (!localLineEditing && isCtrl(modifiers)) {
                    sendC = 'T';
                }
                break;

            case KeyEvent.VK_DELETE:
                if (!localLineEditing && isCtrl(modifiers)) {
                    sendC = 't';
                }
                break;

            case KeyEvent.VK_ENTER:
                if (enterIsFuctionKey) {
                    if (haveNone(modifiers)) {
                        sendC = 'V';
                    } else if (isShift(modifiers)) {
                        sendC = 'v';
                    }
                }
                break;
            }
        }

        if (sendC != 0) {
            if (currentMode != MODE_CONV) {
                /* The documentation says that the keyboard should be locked
                 * in conversational mode as well, but that behaviour seems
                 * to break one of Verizon's applications.
                 */
                doLockKeyboard();
            }
            sendFunctionKey(sendC);
            return true;
        }

        return false;
    }

    private void sendFunctionKey(char keyCode) {
        StringBuilder buf = new StringBuilder();
        buf.append(SOH).append(keyCode);

        if (currentMode != MODE_CONV) {
            buf.append((char) (0x20 + modes[currentMode].getPage()));
        }
        buf.append((char) (0x1f + modes[currentMode].getRow()));
        buf.append((char) (0x1f + modes[currentMode].getCol()));

        if (currentMode == MODE_CONV) {
            buf.append(CR);
        } else {
            buf.append(ETX);
            buf.append(LRC);
        }
        debug("sendFunctionKey " + keyCode + " " + modes[currentMode].getRow() +
              "x" + modes[currentMode].getCol());
        sendDirect(buf.toString());
    }

    private void updateLine(int lineno) {
        if (display != null) {
            int line = lineno + modes[currentMode].getVisTop();
            display.updateDirtyArea(line, 0, line+1, COLUMNS);
            if (doupdate)
                display.repaint();
        }
    }

    private void setMode(int mode) {
        debug("setMode:" + mode);
        int oldMode = currentMode;
        if (mode != currentMode) {
            switch (mode) {
            case MODE_CONV:
                doUnlockKeyboard();
                break;
            case MODE_BLOCK:
                localLineEditing = true;
                doLockKeyboard();
                break;
            case MODE_PROTECT_BLOCK:
                localLineEditing = false;
                doLockKeyboard();
                break;
            }
            modes[currentMode].setDisplay(null);
            currentMode = mode;
            modes[currentMode].setDisplay(display);
            modes[currentMode].switchReset();
            if (oldMode == MODE_CONV && currentMode == MODE_BLOCK) {
                blockMode.setBuffer(convMode.getBuffer());
            }
            modes[currentMode].doSetDefaultVideoAttribute(0);
            statusLine.setMessage("");
        }
    }

    private void repaint() {
        if (display != null && doupdate) {
            display.repaint();
        }
    }
}


