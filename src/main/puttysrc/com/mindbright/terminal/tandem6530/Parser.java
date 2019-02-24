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

public class Parser implements AsciiCodes {
    public final static char IGNORE = 256;

    public final static int ROWS = 24;
    public final static int COLS = 80;

    private final static int NOT_DEF = -1;

    private boolean isShifted = false; /* Use G1 char set */
    private boolean blockMode = false;

    // Enable this for debug print outs
    private boolean DEBUG = false;

    /* Major states */
    private final static int SMAJOR_GROUND           = 1;
    private final static int SMAJOR_VAR_COLLECT      = 2;
    private final static int SMAJOR_ESC              = 3;
    private final static int SMAJOR_ESC_DASH         = 4;
    private final static int SMAJOR_ESC_SPACE        = 5;

    /* Minor states */
    private final static int SMINOR_NONE                            =  1;
    private final static int SMINOR_SET_BUFFER_ADDR                 =  2;
    private final static int SMINOR_EXT_DATA_COMPR                  =  3;
    private final static int SMINOR_SET_CURSOR_ADDR                 =  4;
    private final static int SMINOR_LIMITED_DATA_COMPR              =  5;
    private final static int SMINOR_DEFINE_FIELD_ATTRIB             =  6;
    private final static int SMINOR_START_FIELD                     =  7;
    private final static int SMINOR_SET_VIDEO_ATTRIB                =  8;
    private final static int SMINOR_SET_VIDEO_PRIOR_REG             =  9;
    private final static int SMINOR_DISPLAY_PAGE                    = 10;
    private final static int SMINOR_SELECT_PAGE                     = 11;
    private final static int SMINOR_SET_MAX_PAGE                    = 12;
    private final static int SMINOR_READ_WITH_ADDRESS               = 13;
    private final static int SMINOR_READ_WITH_ADDRESS_ALL           = 14;
    private final static int SMINOR_READ_SCREEN_WITH_ATTRIBS        = 15;
    private final static int SMINOR_READ_IO_CONF                    = 16;
    private final static int SMINOR_GET_DIR_INFO                    = 17;
    private final static int SMINOR_START_FIELD_EXT                 = 18;
    private final static int SMINOR_DEFINE_ENTER_KEY                = 19;
    private final static int SMINOR_STRING_CONFIG_PARAMS            = 20;
    private final static int SMINOR_WRITE_MSG_FIELD                 = 21;
    private final static int SMINOR_SET_COLOR_MAP                   = 22;
    private final static int SMINOR_DEFINE_DATA_TYPES               = 23;
    private final static int SMINOR_DEFINE_VARIABLE_TABLE           = 24;
    private final static int SMINOR_SET_COLOR_CONFIG                = 25;
    private final static int SMINOR_SET_TERM_CONF                   = 26;
    private final static int SMINOR_SET_IO_CONF                     = 27;
    private final static int SMINOR_EXEC_PROGRAM                    = 28;
    private final static int SMINOR_WRITE_TO_FILE                   = 29;
    private final static int SMINOR_WRITE_READ_TO_FILE              = 30;
    private final static int SMINOR_START_ENHANCED_FIELD            = 31;
    private final static int SMINOR_CLEAR_MEMORY                    = 32;
    private final static int SMINOR_MODE_SWITCH                     = 33;
    private final static int SMINOR_SIMULATE_FKEY                   = 34;

    private int majorState = SMAJOR_GROUND;
    private int minorState = SMINOR_NONE;

    /* To collect arguments for Control Characters and Escape sequences */
    private Collector coll = new Collector();

    /* To collect arguments for Escape- parameters */
    private int params[] = new int[12];
    private int paramsIndex = 0;


    /* State variable for Define Enter Key Function */
    private int defineEnterCount = -1;

    /* State variables for Set String Configuration Paramter */
    private final static int ALL_STRINGS = 0;
    private String stringParams[] = null;
    private int stringParamsType = -1;

    /* State variables for Set Color Map Table */
    private int setColorMapStart = -1;
    private int setColorMapLen = -1;

    /* State variables for Define Data Type Table */
    private boolean defineDataTypesExt = false;
    private int defineDataTypesStart = -1;
    private int defineDataTypesLen = -1;

    /* State variables for Define Variable Table */
    private int defineVariableTableStart = -1;
    private int defineVariableTableLen = -1;

    /* State variables for Set Color Configuration */
    private int setColorConfigStart = -1;
    private int setColorConfigLen = -1;

    /* State variables for Load and Execute an OS program */
    private boolean execGoETX = false;

    /* State variables for Write to File or Device Name */
    private boolean writeToFileBin = false;
    private boolean writeToFileEsc = false;
    private char writeToFileCharArray[] = new char[2];

    /* State variables for Start Enhanced Color Field */
    private boolean startEnhancedGotCount = false;
    private int startEnhancedPairCount;
    private char startEnhancedVideo;
    private char startEnhancedData;
    private char startEnhancedExtData;

    private ActionHandler ah;

    public void setActionHandler(ActionHandler actionHandler) {
        ah = actionHandler;
    }
    public ActionHandler getActionHandler() {
        return ah;
    }

    public void setBlockMode() {
        blockMode = true;
    }
    public void setConversationalMode() {
        blockMode = false;
    }

    public void reset() {
        isShifted = false;
        setState(SMAJOR_GROUND);
    }


    public char parse(char inC) throws ParseException {
        if (DEBUG) {
            if ((inC >= 0x20 && inC <= 0x7e)) {
                System.err.println("Parsing 0x" +
                                   Integer.toHexString(inC) + " " + inC);
            } else {
                System.err.println("Parsing 0x" + Integer.toHexString(inC));
            }
        }

        if (inC == 0x0e) { /* SO */
            isShifted = true;
            return IGNORE;
        }
        if (inC == 0x0f) { /* SI */
            isShifted = false;
            return IGNORE;
        }
        switch (majorState) {
        case SMAJOR_GROUND:
            if (isG0(inC) || isG1(inC)) {
                /* Displayable character */
                return shiftChar(inC);
            }
            /* Control character */
            switch (inC) {
            case NUL:
            case EOT:
            case ENQ:
            	// Ignore these... They are not in the spec, but
            	// is in the sniffed packets from Verizon. They
            	// don't seems to be important.
            	break;
            case SOH:
            	setState(SMAJOR_VAR_COLLECT, SMINOR_MODE_SWITCH);
            	break;
            case BELL:
            	if (ah != null) {
            		ah.doBell();
            	}
            	break;
            case BS:
            	if (ah != null) {
            		ah.doBackspace();
            	}
            	break;
            case HT:
            	if (ah != null) {
            		ah.doHTab();
            	}
            	break;
            case LF:
            	if (ah != null) {
            		ah.doLineFeed();
            	}
            	break;
            case CR:
            	if (ah != null) {
            		ah.doCarriageReturn();
            	}
            	break;
            case DC1:
            	setState(SMAJOR_VAR_COLLECT, SMINOR_SET_BUFFER_ADDR);
            	break;
            case DC2:
            	setState(SMAJOR_VAR_COLLECT, SMINOR_EXT_DATA_COMPR);
            	break;
            case DC3:
            	setState(SMAJOR_VAR_COLLECT, SMINOR_SET_CURSOR_ADDR);
            	break;
            case DC4:
            	setState(SMAJOR_VAR_COLLECT,
            			SMINOR_LIMITED_DATA_COMPR);
            	break;
            case FS:
            	setState(SMAJOR_VAR_COLLECT,
            			SMINOR_DEFINE_FIELD_ATTRIB);
            	break;
            case GS:
            	setState(SMAJOR_VAR_COLLECT, SMINOR_START_FIELD);
            	break;
            case ESC:
            	setState(SMAJOR_ESC);
            	break;
            default:
            	error("Unknown control character 0x" +
            			Integer.toHexString(inC));
            }
            break;

        case SMAJOR_ESC:
            switch (inC) {
            case '0':
                if (ah != null) {
                    ah.doPrintScreenOrPage();
                }
                setState(SMAJOR_GROUND);
                break;
            case '1':
                if (ah != null) {
                    ah.doSetTab();
                }
                setState(SMAJOR_GROUND);
                break;
            case '2':
                if (ah != null) {
                    ah.doClearTab();
                }
                setState(SMAJOR_GROUND);
                break;
            case '3':
                if (ah != null) {
                    ah.doClearAllTabs();
                }
                setState(SMAJOR_GROUND);
                break;
            case '8':
                if (ah != null) {
                    ah.doSet40CharLineWidth();
                }
                setState(SMAJOR_GROUND);
                break;
            case '9':
                if (ah != null) {
                    ah.doSet80CharLineWidth();
                }
                setState(SMAJOR_GROUND);
                break;
            case '<':
                if (ah != null) {
                    ah.doReadWholePageOrBuffer();
                }
                setState(SMAJOR_GROUND);
                break;
            case '>':
                if (ah != null) {
                    ah.doResetModifiedDataTags();
                }
                setState(SMAJOR_GROUND);
                break;
            case '@':
                if (ah != null) {
                    ah.doDelayOneSecond();
                }
                setState(SMAJOR_GROUND);
                break;
            case 'A':
                if (ah != null) {
                    ah.doCursorUp();
                }
                setState(SMAJOR_GROUND);
                break;
            case 'C':
                if (ah != null) {
                    ah.doCursorRight();
                }
                setState(SMAJOR_GROUND);
                break;
            case 'F':
                if (ah != null) {
                    ah.doCursorHomeDown();
                }
                setState(SMAJOR_GROUND);
                break;
            case 'H':
                if (ah != null) {
                    ah.doCursorHome();
                }
                setState(SMAJOR_GROUND);
                break;
            case 'J':
                if (ah != null) {
                    ah.doEraseToEndOfPageOrMemory();
                }
                setState(SMAJOR_GROUND);
                break;
            case 'K':
                if (ah != null) {
                    ah.doEraseToEndOfLineOrField();
                }
                setState(SMAJOR_GROUND);
                break;
            case 'L':
                if (ah != null) {
                    ah.doInsertLine();
                }
                setState(SMAJOR_GROUND);
                break;
            case 'M':
                if (ah != null) {
                    ah.doDeleteLine();
                }
                setState(SMAJOR_GROUND);
                break;
            case 'N':
                if (ah != null) {
                    ah.doDisableLocalLineEditing();
                }
                setState(SMAJOR_GROUND);
                break;
            case 'O':
                if (ah != null) {
                    ah.doInsertCharacter();
                }
                setState(SMAJOR_GROUND);
                break;
            case 'P':
                if (ah != null) {
                    ah.doDeleteCharacter();
                }
                setState(SMAJOR_GROUND);
                break;
            case 'S':
                if (ah != null) {
                    ah.doRollUp();
                }
                setState(SMAJOR_GROUND);
                break;
            case 'T':
                if (ah != null) {
                    ah.doRollDown();
                }
                setState(SMAJOR_GROUND);
                break;
            case 'U':
                if (ah != null) {
                    ah.doPageDown();
                }
                setState(SMAJOR_GROUND);
                break;
            case 'V':
                if (ah != null) {
                    ah.doPageUp();
                }
                setState(SMAJOR_GROUND);
                break;
            case 'W':
                if (ah != null) {
                    ah.doEnterProtectedSubmode();
                }
                setState(SMAJOR_GROUND);
                break;
            case 'X':
                if (ah != null) {
                    ah.doExitProtectedSubmode();
                }
                setState(SMAJOR_GROUND);
                break;
            case '^':
                if (ah != null) {
                    ah.doReadTerminalStatus();
                }
                setState(SMAJOR_GROUND);
                break;
            case '_':
                if (ah != null) {
                    ah.doReadFullRevisionLevel();
                }
                setState(SMAJOR_GROUND);
                break;
            case 'a':
                if (ah != null) {
                    ah.doReadCursorAddress();
                }
                setState(SMAJOR_GROUND);
                break;
            case 'b':
                if (ah != null) {
                    ah.doUnlockKeyboard();
                }
                setState(SMAJOR_GROUND);
                break;
            case 'c':
                if (ah != null) {
                    ah.doLockKeyboard();
                }
                setState(SMAJOR_GROUND);
                break;
            case 'd':
                setState(SMAJOR_VAR_COLLECT, SMINOR_SIMULATE_FKEY);
                break;
            case 'f':
                if (ah != null) {
                    ah.doDisconnectModem();
                }
                setState(SMAJOR_GROUND);
                break;
            case 'i':
                if (ah != null) {
                    ah.doBackTab();
                }
                setState(SMAJOR_GROUND);
                break;
            case 'q':
                if (ah != null) {
                    ah.doReinitialize();
                }
                setState(SMAJOR_GROUND);
                break;
            case 't':
                if (ah != null) {
                    ah.doSet40CharactersScreenWidth();
                }
                setState(SMAJOR_GROUND);
                break;
            case '?':
                if (ah != null) {
                    ah.doReadTerminalConfiguration();
                }
                setState(SMAJOR_GROUND);
                break;
            case 'u':
                defineEnterCount = 0;
                setState(SMAJOR_VAR_COLLECT, SMINOR_DEFINE_ENTER_KEY);
                break;
            case '6':
                setState(SMAJOR_VAR_COLLECT, SMINOR_SET_VIDEO_ATTRIB);
                break;
            case '7':
                setState(SMAJOR_VAR_COLLECT,
                         SMINOR_SET_VIDEO_PRIOR_REG);
                break;
            case ';':
                setState(SMAJOR_VAR_COLLECT, SMINOR_DISPLAY_PAGE);
                break;
            case ':':
                setState(SMAJOR_VAR_COLLECT, SMINOR_SELECT_PAGE);
                break;
            case 'o':
                setState(SMAJOR_VAR_COLLECT, SMINOR_WRITE_MSG_FIELD);
                break;
            case 'p':
                setState(SMAJOR_VAR_COLLECT, SMINOR_SET_MAX_PAGE);
                break;
            case '=':
                setState(SMAJOR_VAR_COLLECT, SMINOR_READ_WITH_ADDRESS);
                break;
            case 'I':
                if (blockMode) {
                    setState(SMAJOR_VAR_COLLECT, SMINOR_CLEAR_MEMORY);
                } else {
                    if (ah != null) {
                        ah.doClearMemoryToSpaces();
                    }
                    setState(SMAJOR_GROUND);
                }
                break;
            case ']':
                setState(SMAJOR_VAR_COLLECT,
                         SMINOR_READ_WITH_ADDRESS_ALL);
                break;
            case '[':
                setState(SMAJOR_VAR_COLLECT,
                         SMINOR_START_FIELD_EXT);
                break;
            case 'Q':
                setState(SMAJOR_VAR_COLLECT,
                         SMINOR_READ_SCREEN_WITH_ATTRIBS);
                break;
            case 'y':
                setState(SMAJOR_VAR_COLLECT,
                         SMINOR_READ_IO_CONF);
                break;
            case 'v':
                setState(SMAJOR_VAR_COLLECT,
                         SMINOR_SET_TERM_CONF);
                break;
            case 'x':
                setState(SMAJOR_VAR_COLLECT,
                         SMINOR_SET_IO_CONF);
                break;
            case 'r':
                defineDataTypesStart = 1;
                defineDataTypesLen = 96;
                defineDataTypesExt = false;
                setState(SMAJOR_VAR_COLLECT,
                         SMINOR_DEFINE_DATA_TYPES);
                break;
            case '-':
                setState(SMAJOR_ESC_DASH);
                break;
            case ' ':
                setState(SMAJOR_ESC_SPACE);
                break;
            case '{':
                writeToFileBin = false;
                setState(SMAJOR_VAR_COLLECT,
                         SMINOR_WRITE_TO_FILE);
                break;
            case '}':
                writeToFileBin = false;
                setState(SMAJOR_VAR_COLLECT,
                         SMINOR_WRITE_READ_TO_FILE);
                break;
            case '`':
                startEnhancedGotCount = false;
                setState(SMAJOR_VAR_COLLECT,
                         SMINOR_START_ENHANCED_FIELD);
                break;
            default:
                setState(SMAJOR_GROUND);
                error("Unknown escape sequence Esc 0x" + 
                      Integer.toHexString(inC));
            }
            break;

        case SMAJOR_ESC_SPACE:
            switch (inC) {
            case 'v':
                if (ah != null) {
                    ah.doRead6530ColorMappingTable();
                }
                setState(SMAJOR_GROUND);
                break;

            default:
                setState(SMAJOR_GROUND);
                error("Unknown escape space sequence Esc 0x" +
                      Integer.toHexString(inC));
            }
            break;

        case SMAJOR_ESC_DASH:
            switch (inC) {
            case 'e':
                if (ah != null) {
                    ah.doGetMachineName();
                }
                setState(SMAJOR_GROUND);
                break;
            case 'f':
                setState(SMAJOR_VAR_COLLECT, SMINOR_GET_DIR_INFO);
                break;
            case 'o':
                if (ah != null) {
                    ah.doReadKeyboardLatch();
                }
                setState(SMAJOR_GROUND);
                break;
            case 'u':
                if (ah != null) {
                    ah.doReadColorConfiguration();
                }
                setState(SMAJOR_GROUND);
                break;
            case 'v':
                if (ah != null) {
                    ah.doReadColorMappingTable();
                }
                setState(SMAJOR_GROUND);
                break;

            case 'W':
                if (ah != null) {
                    ah.doReportExecReturnCode();
                }
                setState(SMAJOR_GROUND);
                break;

            case 'C': {
                    getNextParam();
                    int row = getExtendedRow("Esc-C", getParam(0));
                    int column = getExtendedColumn("Esc-C",
                                                   getParam(1));
                    if (ah != null) {
                        ah.doSetBufferAddress(row, column);
                    }
                    setState(SMAJOR_GROUND);
                    break;
                }

            case 'D': {
                    getNextParam();
                    int row = getExtendedRow("Esc-D", getParam(0));
                    int column = getExtendedColumn("Esc-D",
                                                   getParam(1));
                    if (ah != null) {
                        ah.doSetCursorAddress(row, column);
                    }
                    setState(SMAJOR_GROUND);
                    break;
                }

            case 'I': {
                    int startRow = 1;
                    int startCol = 1;
                    int endRow = ROWS;
                    int endCol = COLS;
                    getNextParam();
                    if (getNumParams() > 0) {
                        startRow = getExtendedRow("Esc-I", getParam(0));
                        startCol = getExtendedColumn("Esc-I",
                                                     getParam(1));
                        endRow = getExtendedRow("Esc-I", getParam(2));
                        endCol = getExtendedColumn("Esc-I",
                                                   getParam(3));
                    }

                    if (ah != null) {
                        ah.doClearMemoryToSpaces(startRow, startCol,
                                                 endRow, endCol);
                    }
                    setState(SMAJOR_GROUND);
                    break;
                }

            case 'J': {
                    int startRow = 1;
                    int startCol = 1;
                    int endRow = ROWS;
                    int endCol = COLS;
                    getNextParam();
                    if (getNumParams() > 0) {
                        startRow = getExtendedRow("Esc-J", getParam(0));
                        startCol = getExtendedColumn("Esc-J",
                                                     getParam(1));
                        endRow = getExtendedRow("Esc-J", getParam(2));
                        endCol = getExtendedColumn("Esc-J",
                                                   getParam(3));
                    }

                    if (ah != null) {
                        ah.doReadWithAddress(startRow, startCol,
                                             endRow, endCol);
                    }
                    setState(SMAJOR_GROUND);
                    break;
                }

            case 'K': {
                    int startRow = 1;
                    int startCol = 1;
                    int endRow = ROWS;
                    int endCol = COLS;
                    getNextParam();
                    if (getNumParams() > 0) {
                        startRow = getExtendedRow("Esc-K", getParam(0));
                        startCol = getExtendedColumn("Esc-K",
                                                     getParam(1));
                        endRow = getExtendedRow("Esc-K", getParam(2));
                        endCol = getExtendedColumn("Esc-K",
                                                   getParam(3));
                    }

                    if (ah != null) {
                        ah.doReadWithAddressAll(startRow, startCol,
                                                endRow, endCol);
                    }
                    setState(SMAJOR_GROUND);
                    break;
                }

            case 'g': {
                    getNextParam();
                    int param = getParam(0);
                    if (param == 2 || param == 3) {
                        if (ah != null) {
                            ah.doReadVTLAUNCHConfigurationParameter(
                                param);
                        }
                    }
                    setState(SMAJOR_GROUND);
                    break;
                }

            case 'i': {
                    getNextParam();

                    int startStopEvent = getParam(0);
                    if (startStopEvent == NOT_DEF ||
                            (startStopEvent != 1 && startStopEvent != 2)) {
                        setState(SMAJOR_GROUND);
                        break;
                    }

                    int numBuckets = getParam(1);
                    if (numBuckets == NOT_DEF ||
                            numBuckets < 1 || numBuckets > 10) {
                        setState(SMAJOR_GROUND);
                        break;
                    }

                    int numSpec = getNumParams() - 2;

                    if (numBuckets == 1 && numSpec == 0) {
                        /* Only one bucket, from 0 to infinity */
                        if (ah != null) {
                            ah.doRTMControl(startStopEvent, null);
                        }
                        setState(SMAJOR_GROUND);
                        break;
                    }

                    if (numBuckets - 1 != numSpec) {
                        /* Fewer bucket specifications than numBuckets
                         * said. */
                        setState(SMAJOR_GROUND);
                        break;
                    }

                    int array[] = new int[numSpec];
                    int i;
                    for (i = 0; i < numSpec; i++) {
                        array[i] = getParam(2 + i);
                    }
                    if (ah != null) {
                        ah.doRTMControl(startStopEvent, array);
                    }

                    setState(SMAJOR_GROUND);
                    break;
                }

            case 'j': {
                    getNextParam();
                    if (getNumParams() != 4) {
                        setState(SMAJOR_GROUND);
                        break;
                    }
                    int array[] = new int[4];
                    int i;
                    for (i = 0; i < array.length; i++) {
                        array[i] = getParam(i);
                        if (array[i] == NOT_DEF ||
                                array[i] < 0 || array[i] > 255) {
                            break;
                        }
                    }
                    if (i == array.length && ah != null) {
                        ah.doRTMDataUpload(array);
                    }
                    setState(SMAJOR_GROUND);
                    break;
                }

            case 'm': {
                    getNextParam();
                    if (getNumParams() != 1) {
                        setState(SMAJOR_GROUND);
                        error("Esc-m", "One parameter expected");
                    }
                    int n = getParam(0);
                    if (n < 0 || n > 3) {
                        setState(SMAJOR_GROUND);
                        error("Esc-m", "Bad parameter " + n);
                    }
                    if (ah != null) {
                        ah.doSetEM3270Mode(n);
                    }
                    setState(SMAJOR_GROUND);
                    break;
                }

            case 'O': {
                    getNextParam();
                    if (getNumParams() != 2) {
                        setState(SMAJOR_GROUND);
                        error("Esc-O", "Two parameters expected");
                    }

                    int device = getParam(0);
                    if (device != 2) {
                        device = 1;
                    }
                    int terminator = getParam(1);
                    if (terminator < 3 || terminator > 127) {
                        terminator = 0x12;
                    }
                    if (ah != null) {
                        ah.doWriteToAux1OrAux2Device(device,
                                                     (char)terminator);
                    }
                    setState(SMAJOR_GROUND);
                    break;
                }

            case 'z': {
                    getNextParam();
                    if (getNumParams() != 1) {
                        setState(SMAJOR_GROUND);
                        error("Esc-z", "One parameters expected");
                    }

                    if (ah != null) {
                        ah.doTerminateRemote6530Operation(getParam(0));
                    }
                    setState(SMAJOR_GROUND);
                    break;
                }

            case 'd': {
                    getNextParam();
                    int n = 0;
                    if (getNumParams() == 0) {
                        n = ALL_STRINGS;
                    } else if (getNumParams() == 1) {
                        n = getParam(0);
                        if (n <= 0 || n > 7) {
                            n = ALL_STRINGS;
                        }
                    } else {
                        setState(SMAJOR_GROUND);
                        error("Esc-d", "Bad format");
                    }

                    if (ah != null) {
                        ah.doReadStringConfigurationParameter(n);
                    }
                    setState(SMAJOR_GROUND);
                    break;
                }
            case 'c': {
                    getNextParam();
                    int n = 0;
                    if (getNumParams() == 0) {
                        n = ALL_STRINGS;
                    } else if (getNumParams() == 1) {
                        n = getParam(0);
                        if (n <= 0 || n > 7) {
                            n = ALL_STRINGS;
                        }
                    } else {
                        setState(SMAJOR_GROUND);
                        stringParams = null;
                        error("Esc-c", "Bad format");
                    }
                    stringParams = new String[7];
                    stringParamsType = n;
                    setState(SMAJOR_VAR_COLLECT,
                             SMINOR_STRING_CONFIG_PARAMS);
                    break;
                }

            case 'q': {
                    getNextParam();
                    if (getNumParams() < 3) {
                        setState(SMAJOR_GROUND);
                        error("Esc-q", "Too few arguments");
                    }
                    if (getNumParams() > 3) {
                        setState(SMAJOR_GROUND);
                        error("Esc-q", "Too many arguments");
                    }
                    int setOrReSet = getParam(0);
                    if (setOrReSet == 1) {
                        if (ah != null) {
                            ah.doResetColorMapTable();
                        }
                        setState(SMAJOR_GROUND);
                    } else if (setOrReSet == 0) {
                        int startIndex = getParam(1);
                        if (startIndex == NOT_DEF ||
                                startIndex < 1 || startIndex > 32) {
                            startIndex = 1;
                        }
                        int endIndex = getParam(2);
                        if (endIndex == NOT_DEF ||
                                endIndex < 1 || endIndex > 32) {
                            endIndex = 32;
                        }
                        setColorMapStart = startIndex;
                        setColorMapLen = endIndex - startIndex + 1;
                        setState(SMAJOR_VAR_COLLECT,
                                 SMINOR_SET_COLOR_MAP);
                    } else {
                        setState(SMAJOR_GROUND);
                        error("Esc-q", "Invalid mode");
                    }
                    break;
                }

            case 'r': {
                    getNextParam();
                    if (getNumParams() < 2) {
                        setState(SMAJOR_GROUND);
                        error("Esc-r", "Too few arguments");
                    }
                    if (getNumParams() > 2) {
                        setState(SMAJOR_GROUND);
                        error("Esc-r", "Too many arguments");
                    }
                    int startIndex = getParam(0);
                    if (startIndex == NOT_DEF ||
                            startIndex < 1 || startIndex > 192) {
                        startIndex = 1;
                    }
                    int endIndex = getParam(1);
                    if (endIndex == NOT_DEF ||
                            endIndex < 1 || endIndex > 192) {
                        endIndex = 192;
                    }
                    defineDataTypesStart = startIndex;
                    defineDataTypesLen = endIndex - startIndex + 1;
                    defineDataTypesExt = true;
                    setState(SMAJOR_VAR_COLLECT,
                             SMINOR_DEFINE_DATA_TYPES);
                    break;
                }

            case 's': {
                    getNextParam();
                    if (getNumParams() < 1) {
                        setState(SMAJOR_GROUND);
                        error("Esc-s", "Too few arguments");
                    }
                    if (getNumParams() > 3) {
                        setState(SMAJOR_GROUND);
                        error("Esc-s", "Too many arguments");
                    }
                    int mode = getParam(0);
                    if (mode != 0 && mode != 1) {
                        setState(SMAJOR_GROUND);
                        error("Esc-s", "Invalid mode");
                    }

                    if (mode == 1) {
                        if (ah != null) {
                            ah.doResetVariableTable();
                        }
                        setState(SMAJOR_GROUND);
                    } else {
                        int startIndex = getParam(1);
                        if (startIndex == NOT_DEF ||
                                startIndex < 1 || startIndex > 32) {
                            startIndex = 1;
                        }
                        int endIndex = getParam(2);
                        if (endIndex == NOT_DEF ||
                                endIndex < 1 || endIndex > 32) {
                            endIndex = 32;
                        }
                        defineVariableTableStart = startIndex;
                        defineVariableTableLen =
                            endIndex - startIndex + 1;
                        setState(SMAJOR_VAR_COLLECT,
                                 SMINOR_DEFINE_VARIABLE_TABLE);
                    }
                    break;
                }

            case 't': {
                    getNextParam();
                    if (getNumParams() < 1) {
                        setState(SMAJOR_GROUND);
                        error("Esc-t", "Too few arguments");
                    }
                    if (getNumParams() > 3) {
                        setState(SMAJOR_GROUND);
                        error("Esc-t", "Too many arguments");
                    }
                    int mode = getParam(0);
                    if (mode != 0 && mode != 1) {
                        setState(SMAJOR_GROUND);
                        error("Esc-t", "Invalid mode");
                    }

                    if (mode == 1) {
                        if (ah != null) {
                            ah.doResetColorConfiguration();
                        }
                        setState(SMAJOR_GROUND);
                    } else {
                        int startIndex = getParam(1);
                        if (startIndex == NOT_DEF ||
                                startIndex < 1 || startIndex > 16) {
                            startIndex = 1;
                        }
                        int endIndex = getParam(2);
                        if (endIndex == NOT_DEF ||
                                endIndex < 1 || endIndex > 16) {
                            endIndex = 16;
                        }
                        setColorConfigStart = startIndex;
                        setColorConfigLen =
                            endIndex - startIndex + 1;
                        setState(SMAJOR_VAR_COLLECT,
                                 SMINOR_SET_COLOR_CONFIG);
                    }
                    break;

                }
            case 'V':
                getNextParam();
                if (getNumParams() != 0) {
                    setState(SMAJOR_GROUND);
                    error("Esc-V: Parameters not expected");
                }
                execGoETX = false;
                setState(SMAJOR_VAR_COLLECT, SMINOR_EXEC_PROGRAM);
                break;

            case 'x': {
                    getNextParam();
                    if (getNumParams() != 1) {
                        setState(SMAJOR_GROUND);
                        error("Esc-x: Wrong number of arguments");
                    }
                    if (getParam(0) != 0 && getParam(0) != 1) {
                        setState(SMAJOR_GROUND);
                        error("Esc-x: Invalid mode");
                    }

                    if (ah != null) {
                        ah.doSet6530ColorMapping(getParam(0) == 1);
                    }
                    setState(SMAJOR_GROUND);
                    break;
                }

            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                coll.addChar(inC);
                break;
            case ';':
                getNextParam();
                break;

            default:
                setState(SMAJOR_GROUND);
                error("Unknown escape sequence Esc - 0x" +
                      Integer.toHexString(inC));
            }

            break;

        case SMAJOR_VAR_COLLECT:
            /* Collecting variables for control characters */
            switch (minorState) {
            case SMINOR_SET_BUFFER_ADDR:
                coll.addChar(inC);
                if (coll.size() == 2) {
                    int row = getNormalRow("DC1", coll.getCharAt(0));
                    int col = getNormalColumn("DC1", coll.getCharAt(1));
                    if (ah != null) {
                        ah.doSetBufferAddress(row, col);
                    }
                    setState(SMAJOR_GROUND);
                }
                break;
            case SMINOR_SET_CURSOR_ADDR:
                coll.addChar(inC);
                if (coll.size() == 2) {
                    int row = getNormalRow("DC3", coll.getCharAt(0));
                    int col = getNormalColumn("DC3", coll.getCharAt(1));
                    if (ah != null) {
                        ah.doSetCursorAddress(row, col);
                    }
                    setState(SMAJOR_GROUND);
                }
                break;
            case SMINOR_LIMITED_DATA_COMPR:
                coll.addChar(inC);
                if (coll.size() == 1) {
                    char c;
                    int n;
                    if ((inC & 0x40) != 0) {
                        c = ' ';
                        n = (inC & 0x3f);
                    } else {
                        c = '0';
                        n = (inC & 0x1f);
                    }
                    if (ah != null) {
                        ah.doDataCompression(n, c);
                    }
                    setState(SMAJOR_GROUND);
                }
                break;
            case SMINOR_EXT_DATA_COMPR:
                coll.addChar(inC);
                if (coll.size() == 2) {
                    char c = coll.getCharAt(1);
                    if (isG0(c) || isG1(c)) {
                        int n = (coll.getCharAt(0) & 0x7f) - 0x20;
                        if (n > 0 && ah != null) {
                            ah.doDataCompression(n, shiftChar(c));
                        }
                    }
                    setState(SMAJOR_GROUND);
                }
                break;
            case SMINOR_DEFINE_FIELD_ATTRIB:
                coll.addChar(inC);
                if (coll.size() == 3) {
                    int row = getNormalRow("FS", coll.getCharAt(0));
                    int col = getNormalColumn("FS", coll.getCharAt(1));

                    boolean useFixed = (coll.getCharAt(2) & 0x40) != 0;
                    int tableRow;
                    if (useFixed) {
                        tableRow = coll.getCharAt(2) & 0x3f;
                    } else {
                        tableRow = coll.getCharAt(2) & 0x1f;
                    }
                    if (ah != null) {
                        ah.doDefineFieldAttribute(row, col, useFixed,
                                                  tableRow);
                    }
                    setState(SMAJOR_GROUND);
                }
                break;
            case SMINOR_START_FIELD: /* start field */
                coll.addChar(inC);
                if (coll.size() == 2) {
                    FieldAttributes attrib = null;
                    try {
                        attrib  = new FieldAttributes(
                                      coll.getCharAt(0),
                                      coll.getCharAt(1));
                    } catch (ParseException e) {
                        error("GS", e.getMessage());
                    }
                    if (ah != null) {
                        ah.doStartField(attrib);
                    }
                    setState(SMAJOR_GROUND);
                }
                break;

            case SMINOR_SET_VIDEO_ATTRIB:
                coll.addChar(inC);
                if (coll.size() == 1) {
                    char attrib = (char) (coll.getCharAt(0) & 0x1f);
                    if (ah != null) {
                        ah.doSetVideoAttributes(attrib);
                    }
                    setState(SMAJOR_GROUND);
                }
                break;
            case SMINOR_SET_VIDEO_PRIOR_REG:
                coll.addChar(inC);
                if (coll.size() == 1) {
                    char attrib = (char) (coll.getCharAt(0) & 0x1f);
                    if (ah != null) {
                        ah.doSetVideoPriorConditionRegister(attrib);
                    }
                    setState(SMAJOR_GROUND);
                }
                break;
            case SMINOR_DISPLAY_PAGE:
                coll.addChar(inC);
                if (coll.size() == 1) {
                    int n = coll.getCharAt(0) - 0x20;
                    if (n >= 0 && ah != null) {
                        ah.doDisplayPage(n);
                    }
                    setState(SMAJOR_GROUND);
                }
                break;
            case SMINOR_SELECT_PAGE:
                coll.addChar(inC);
                if (coll.size() == 1) {
                    int n = coll.getCharAt(0) - 0x20;
                    if (n >= 1 && ah != null) {
                        ah.doSelectPage(n);
                    }
                    setState(SMAJOR_GROUND);
                }
                break;
            case SMINOR_SET_MAX_PAGE:
                coll.addChar(inC);
                if (coll.size() == 1) {
                    int n = coll.getCharAt(0) - 0x30;
                    if (n >= 1 && ah != null) {
                        ah.doSetMaxPageNumber(n);
                    }
                    setState(SMAJOR_GROUND);
                }
                break;
            case SMINOR_READ_WITH_ADDRESS:
                coll.addChar(inC);
                if (coll.size() == 4) {
                    int startRow =
                        getNormalRow("Esc=", coll.getCharAt(0));
                    int startCol =
                        getNormalColumn("Esc=", coll.getCharAt(1));
                    int endRow =
                        getNormalRow("Esc=", coll.getCharAt(2));
                    int endCol =
                        getNormalColumn("Esc=", coll.getCharAt(3));
                    if (ah != null) {
                        ah.doReadWithAddress(startRow, startCol,
                                             endRow, endCol);
                    }
                    setState(SMAJOR_GROUND);
                }
                break;
            case SMINOR_CLEAR_MEMORY:
                coll.addChar(inC);
                if (coll.size() == 4) {
                    int startRow =
                        getNormalRow("EscI", coll.getCharAt(0));
                    int startCol =
                        getNormalColumn("EscI", coll.getCharAt(1));
                    int endRow =
                        getNormalRow("EscI", coll.getCharAt(2));
                    int endCol =
                        getNormalColumn("EscI", coll.getCharAt(3));
                    if (ah != null) {
                        ah.doClearMemoryToSpaces(startRow, startCol,
                                                 endRow, endCol);
                    }
                    setState(SMAJOR_GROUND);
                }
                break;

            case SMINOR_READ_WITH_ADDRESS_ALL:
                coll.addChar(inC);
                if (coll.size() == 4) {
                    int startRow =
                        getNormalRow("Esc]", coll.getCharAt(0));
                    int startCol =
                        getNormalColumn("Esc]", coll.getCharAt(1));
                    int endRow =
                        getNormalRow("Esc]", coll.getCharAt(2));
                    int endCol =
                        getNormalColumn("Esc]", coll.getCharAt(3));
                    if (ah != null) {
                        ah.doReadWithAddressAll(startRow, startCol,
                                                endRow, endCol);
                    }
                    setState(SMAJOR_GROUND);
                }
                break;

            case SMINOR_READ_SCREEN_WITH_ATTRIBS:
                coll.addChar(inC);
                if (coll.size() == 4) {
                    int startRow =
                        getNormalRow("EscQ", coll.getCharAt(0));
                    int startCol =
                        getNormalColumn("EscQ", coll.getCharAt(1));
                    int endRow =
                        getNormalRow("EscQ", coll.getCharAt(2));
                    int endCol =
                        getNormalColumn("EscQ", coll.getCharAt(3));
                    if (ah != null) {
                        ah.doReadScreenWithAllAttributes(startRow,
                                                         startCol,
                                                         endRow,
                                                         endCol);
                    }
                    setState(SMAJOR_GROUND);
                }
                break;

            case SMINOR_READ_IO_CONF:
                coll.addChar(inC);
                if (coll.size() == 2) {
                    if (coll.getCharAt(0) != 'T') {
                        setState(SMAJOR_GROUND);
                        error("Escy", "Bad format");
                    }
                    int device = coll.getCharAt(1) - 0x31;
                    if (device != 1 && device != 2) {
                        setState(SMAJOR_GROUND);
                        error("Escy", "Bad device number");
                    }
                    if (ah != null) {
                        ah.doReadIODeviceConfiguration(device);
                    }
                    setState(SMAJOR_GROUND);
                }
                break;

            case SMINOR_GET_DIR_INFO:
                coll.addChar(inC);
                if (coll.size() == 1) {
                    if ('@' > coll.getCharAt(0) ||
                            'Z' < coll.getCharAt(0)) {
                        setState(SMAJOR_GROUND);
                        error("Esc-f", "Bad drive");
                    }
                    if (ah != null) {
                        ah.doGetCurrentDirectoryAndRedirectionInformation(coll.getCharAt(0));
                    }
                    setState(SMAJOR_GROUND);
                }
                break;
            case SMINOR_START_FIELD_EXT:
                coll.addChar(inC);
                if (coll.size() == 3) {
                    FieldAttributes attrib = null;
                    try {
                        attrib = new FieldAttributes(
                                     coll.getCharAt(0),
                                     coll.getCharAt(1),
                                     coll.getCharAt(2));
                    } catch (ParseException e) {
                        setState(SMAJOR_GROUND);
                        error("Esc[", e.getMessage());
                    }
                    if (ah != null) {
                        ah.doStartFieldExtended(attrib);
                    }
                    setState(SMAJOR_GROUND);
                }
                break;
            case SMINOR_DEFINE_ENTER_KEY:
                coll.addChar(inC);
                if (defineEnterCount == 0 && coll.size() == 1) {
                    defineEnterCount = coll.getCharAt(0) - 0x20;
                    if (defineEnterCount < 1 || defineEnterCount > 8) {
                        setState(SMAJOR_GROUND);
                        error("Escu", "Wrong character count");
                    }
                    coll.reset();
                } else if (defineEnterCount == coll.size()) {
                    char str[] = coll.getArray();
                    for (int i = 0; i < str.length; i++) {
                        if (str[i] == NUL || str[i] == ENQ) {
                            setState(SMAJOR_GROUND);
                            error("Escu", "Invalid character");
                        }
                    }
                    if (ah != null) {
                        ah.doDefineEnterKeyFunction(str);
                    }
                    setState(SMAJOR_GROUND);
                }
                break;

            case SMINOR_STRING_CONFIG_PARAMS:
                if (inC == DC2 || inC == CR) {
                    /* got end of string or config */
                    if (stringParams == null) {
                        /* Should not happend */
                        setState(SMAJOR_GROUND);
                        error("Esc-c",
                              "Internal error. stringParams == null");
                    }
                    if (inC == DC2 && stringParamsType != ALL_STRINGS) {
                        /* Should only be one string here */
                        setState(SMAJOR_GROUND);
                        error("Esc-c", "Too many strings");
                    }
                    int nextIndex = -1;
                    if (stringParamsType == ALL_STRINGS) {
                        for (int i = 0; i < stringParams.length; i++) {
                            if (stringParams[i] == null) {
                                nextIndex = i;
                                break;
                            }
                        }
                        if (nextIndex == -1) {
                            setState(SMAJOR_GROUND);
                            error("Esc-c", "Too many strings");
                        }
                    } else {
                        nextIndex = stringParamsType - 1;
                    }
                    if (inC == CR && stringParamsType == ALL_STRINGS &&
                            nextIndex < (stringParams.length - 1)) {
                        /* Got an end of config, but not all strings
                         * was recieved */
                        setState(SMAJOR_GROUND);
                        error("Esc-c", "Too few strings");
                    }

                    int maxLength[] = { 26, 34, 8, 128, 80, 8, 16 };
                    String str = new String(coll.getArray());
                    if (str.length() > maxLength[nextIndex]) {
                        str = str.substring(0, maxLength[nextIndex]);
                    }
                    stringParams[nextIndex] = str;
                    if (inC == CR) {
                        if (ah != null) {
                            ah.doSetStringConfigurationParameter(
                                stringParams);
                        }
                        setState(SMAJOR_GROUND);
                        stringParams = null;
                        stringParamsType = -1;
                    } else {
                        coll.reset();
                    }
                } else {
                    coll.addChar(inC);
                }
                break;

            case SMINOR_WRITE_MSG_FIELD:
                if (inC == CR) {
                    char curAttrib = ' ';
                    StringBuilder msg = new StringBuilder();
                    StringBuilder attrib = new StringBuilder();
                    for (int i = 0; i < coll.size(); i++) {
                        if (coll.getCharAt(i) == ESC &&
                                i+2 < coll.size() &&
                                coll.getCharAt(i+1) == '6') {
                            curAttrib = coll.getCharAt(i+2);
                            i += 2;
                        } else {
                            msg.append(coll.getCharAt(i));
                            attrib.append(curAttrib);
                        }
                    }
                    if (ah != null) {
                        String msgStr = msg.toString();
                        String attribStr = attrib.toString();
                        if (msgStr.length() > 63) {
                            msgStr = msgStr.substring(0, 63);
                        }
                        if (attribStr.length() > 63) {
                            attribStr = attribStr.substring(0, 63);
                        }
                        ah.doWriteToMessageField(
                            msgStr.toCharArray(),
                            attribStr.toCharArray());
                    }
                    setState(SMAJOR_GROUND);
                } else {
                    coll.addChar(shiftChar(inC));
                }
                break;

            case SMINOR_SET_COLOR_MAP:
                coll.addChar(inC);
                if (coll.size() == setColorMapLen * 2) {
                    byte colorMap[] = getByteArray(coll.getArray());
                    if (colorMap == null) {
                        setState(SMAJOR_GROUND);
                        error("Esc-q", "Not hex digits");
                    }
                    if (ah != null) {
                        ah.doSetColorMapTable(setColorMapStart,
                                              colorMap);
                    }
                    setState(SMAJOR_GROUND);
                }
                break;

            case SMINOR_DEFINE_DATA_TYPES:
                coll.addChar(inC);
                if (coll.size() == defineDataTypesLen * 2) {
                    byte dataTypes[] = getByteArray(coll.getArray());
                    if (dataTypes == null) {
                        setState(SMAJOR_GROUND);
                        error("Esc"+(defineDataTypesExt ? "-r" : "r"),
                              "Not hex digits");
                    }
                    if (ah != null) {
                        ah.doDefineDataTypeTable(defineDataTypesStart,
                                                 dataTypes);
                    }
                    setState(SMAJOR_GROUND);
                }
                break;

            case SMINOR_DEFINE_VARIABLE_TABLE:
                coll.addChar(inC);
                if (coll.size() == defineVariableTableLen * 2 * 3) {
                    char chars[] = fromAsciiHex(coll.getArray());
                    if (chars == null) {
                        setState(SMAJOR_GROUND);
                        error("Esc-s", "Not hex digits");
                    }

                    FieldAttributes attribs[] =
                        new FieldAttributes[defineVariableTableLen];
                    for (int i = 0; i < defineVariableTableLen; i++) {
                        attribs[i] = new FieldAttributes(
                                         chars[i*3],
                                         chars[i*3+1],
                                         chars[i*3+2]);
                    }

                    if (ah != null) {
                        ah.doDefineVariableTable(
                            defineVariableTableStart, attribs);
                    }
                    setState(SMAJOR_GROUND);
                }
                break;

            case SMINOR_SET_COLOR_CONFIG:
                coll.addChar(inC);
                if (coll.size() == setColorConfigLen * 2) {
                    byte colors[] = getByteArray(coll.getArray());
                    if (colors == null) {
                        setState(SMAJOR_GROUND);
                        error("Esc-t", "Not hex digits");
                    }
                    if (ah != null) {
                        ah.doSetColorConfiguration(setColorConfigStart,
                                                   colors);
                    }
                    setState(SMAJOR_GROUND);
                }
                break;

            case SMINOR_SET_TERM_CONF:
                if (inC == CR) {
                    ConfigParameter params[] = null;
                    try {
                        params = ConfigParameter.parse(
                                     new String(coll.getArray()));
                    } catch (ParseException e) {
                        setState(SMAJOR_GROUND);
                        error("Escv", e.getMessage());
                    }
                    if (params.length > 0 &&
                            params[params.length - 1].hasStringValue()) {
                        // Strings are not allowed here
                        // But we must be forgiving
                    }
                    if (ah != null) {
                        ah.doSetTerminalConfiguration(params);
                    }
                    setState(SMAJOR_GROUND);
                } else {
                    coll.addChar(inC);
                }
                break;

            case SMINOR_SET_IO_CONF:
                if (inC == CR) {
                    if (coll.size() < 2 ||
                            coll.getCharAt(0) != 'T' ||
                            (coll.getCharAt(1) != '2' &&
                             coll.getCharAt(1) != '3')) {
                        setState(SMAJOR_GROUND);
                        error("Escx", "Invalid device");
                    }
                    int device = 0;
                    if (coll.getCharAt(1) == '2') {
                        device = 1;
                    } else {
                        device = 2;
                    }
                    /* Don't parse device stuff */
                    char paramTxt[] = new char[coll.size() - 2];
                    System.arraycopy(coll.getArray(), 2,
                                     paramTxt, 0, paramTxt.length);

                    ConfigParameter params[] = null;
                    try {
                        params = ConfigParameter.parse(
                                     new String(paramTxt));
                    } catch (ParseException e) {
                        setState(SMAJOR_GROUND);
                        error("Escx", e.getMessage());
                    }
                    if (ah != null) {
                        ah.doSetIODeviceConfiguration(device, params);
                    }
                    setState(SMAJOR_GROUND);
                } else {
                    coll.addChar(inC);
                }
                break;

            case SMINOR_EXEC_PROGRAM:
                if (inC == ETX) {
                    execGoETX = true;
                } else if (execGoETX) {
                    /* XXX do LRC stuff first */

                    char data[] = coll.getArray();
                    if (data.length <= 1) {
                        setState(SMAJOR_GROUND);
                        error("Esc-V", "Filename missing");
                    }
                    String execString = new String(data, 0,
                                                   data.length - 1);
                    if (ah != null) {
                        ah.doLoadAndExecuteAnOperatingSystemProgram(
                            execString);
                    }
                    setState(SMAJOR_GROUND);
                } else {
                    coll.addChar(inC);
                }
                break;

            case SMINOR_START_ENHANCED_FIELD:
                coll.addChar(inC);
                if (startEnhancedGotCount) {
                    if (coll.size() == startEnhancedPairCount * 4) {
                        char data[] = fromAsciiHex(coll.getArray());
                        if (data == null) {
                            setState(SMAJOR_GROUND);
                            error("Esc`", "Not a hex digit");
                        }
                        IBM3270FieldAttributes attribs = null;
                        try {
                            attribs = new IBM3270FieldAttributes(
                                          startEnhancedVideo,
                                          startEnhancedData,
                                          startEnhancedExtData);
                        } catch (ParseException e) {
                            setState(SMAJOR_GROUND);
                            error("Esc`", e.getMessage());
                        }
                        try {
                            for (int i = 0; i < data.length; i += 2) {
                                if (data[i] == 0x42) {
                                    attribs.setFgColor(data[i+1]-0x20);
                                } else if (data[i] == 0x45) {
                                    attribs.setBgColor(data[i+1]-0x20);
                                } else {
                                    setState(SMAJOR_GROUND);
                                    error("Esc`", "Invalid color selector");
                                }
                            }
                        } catch (BadColorException e) {
                            setState(SMAJOR_GROUND);
                            error("Esc`", "Bad color");
                        }

                        if (ah != null) {
                            ah.doStartEnhancedColorField(attribs);
                        }
                        setState(SMAJOR_GROUND);
                    }
                } else {
                    if (coll.size() == 5) {
                        startEnhancedVideo   = coll.getCharAt(0);
                        startEnhancedData    = coll.getCharAt(1);
                        startEnhancedExtData = coll.getCharAt(2);
                        // We only need to check the last count byte.
                        // I assume that imbecile that wrote the spec
                        // means pair count, and not byte count.
                        startEnhancedPairCount = coll.getCharAt(4)
                                                 - 0x30;
                        if (startEnhancedPairCount < 1 ||
                                startEnhancedPairCount > 2) {
                            setState(SMAJOR_GROUND);
                            error("Esc`: Bad pair count value");
                        }

                        startEnhancedGotCount = true;
                        coll.reset();
                    }
                }
                break;

            case SMINOR_WRITE_TO_FILE:
            case SMINOR_WRITE_READ_TO_FILE: {
                    String escSeq = null;
                    if (minorState == SMINOR_WRITE_TO_FILE) {
                        escSeq = "Esc{";
                    } else {
                        escSeq = "Esc}";
                    }
                    if (inC == CR) {
                        if (writeToFileBin &&
                                writeToFileCharArray[0] != 0x00) {
                            setState(SMAJOR_GROUND);
                            error(escSeq,
                                  "Uneven number of data characters");
                        }
                        String data = new String(coll.getArray());
                        int deviceEnd = data.indexOf('"', 1);
                        if (deviceEnd == -1) {
                            setState(SMAJOR_GROUND);
                            error(escSeq, "Bad format");
                        }
                        int dataStart = data.indexOf(' ', deviceEnd);
                        if (dataStart == -1) {
                            setState(SMAJOR_GROUND);
                            error(escSeq, "Bad format");
                        }

                        String device = data.substring(1, deviceEnd);
                        char cOpCode[] = fromAsciiHex(
                                             data.substring(deviceEnd + 1,
                                                            dataStart));
                        if (cOpCode == null) {
                            setState(SMAJOR_GROUND);
                            error(escSeq, "Invalid opcode");
                        }
                        int opCode = cOpCode[0];
                        if (opCode < 0x3c || 0x43 < opCode ||
                                opCode == 0x41) {
                            setState(SMAJOR_GROUND);
                            error(escSeq, "Invalid opcode");
                        }

                        int bDataLen = coll.size() - (dataStart + 1);
                        byte bData[] = new byte[bDataLen];
                        for (int i = dataStart + 1, j = 0;
                                i < coll.size(); i++, j++) {
                            bData[j] = (byte) coll.getCharAt(i);
                        }

                        if (ah != null) {
                            if (minorState == SMINOR_WRITE_TO_FILE) {
                                ah.doWriteToFileOrDeviceName(device,
                                                             opCode,
                                                             bData);
                            } else {
                                ah.doWriteOrReadToFileOrDeviceName(
                                    device,
                                    opCode,
                                    bData);
                            }
                        }
                        setState(SMAJOR_GROUND);
                    } else if (inC == ESC) {
                        writeToFileEsc = true;
                    } else if (writeToFileEsc) {
                        if (inC == 'a') {
                            writeToFileBin = false;
                        } else if (inC == 'b') {
                            writeToFileBin = true;
                            writeToFileCharArray[0] = (char) 0x00;
                            writeToFileCharArray[1] = (char) 0x00;
                        } else {
                            setState(SMAJOR_GROUND);
                        }
                        writeToFileEsc = false;
                    } else {
                        if (writeToFileBin) {
                            if (writeToFileCharArray[0] == 0) {
                                writeToFileCharArray[0] = inC;
                            } else {
                                writeToFileCharArray[1] = inC;

                                char c[] = fromAsciiHex(
                                               writeToFileCharArray);
                                if (c == null) {
                                    setState(SMAJOR_GROUND);
                                    error(escSeq,
                                          "Invalid data character");
                                }
                                coll.addChar(c[0]);
                                writeToFileCharArray[0] = (char) 0x00;
                                writeToFileCharArray[1] = (char) 0x00;
                            }
                        } else {
                            coll.addChar(inC);
                        }
                    }
                    break;
                }
            case SMINOR_MODE_SWITCH:
                coll.addChar(inC);
                if (coll.size() == 1) {
                    if (inC != 'C' && inC != 'B') {
                        setState(SMAJOR_GROUND);
                        error("Mode switch", "Unknown mode '"+inC+"'");
                    }
                } else if (coll.size() == 2) {
                    if (inC != ETX) {
                        setState(SMAJOR_GROUND);
                        error("Mode switch", "Bad sequence");
                    }
                    setState(SMAJOR_GROUND);
// At least one emulator seems to not send any LRC-characters at all.
//                 } else if (coll.size() == 3) {

                    if (ah != null) {
                        if (coll.getCharAt(0) == 'C') {
                            ah.doSetConversationalMode();
                        } else {
                            ah.doSetBlockMode();
                        }
                    }
                    setState(SMAJOR_GROUND);
                }
                break;

            case SMINOR_SIMULATE_FKEY:
                coll.addChar(inC);
                if (coll.size() == 1) {
                    if (ah != null) {
                        ah.doSimulateFunctionKey(inC);
                    }
                    setState(SMAJOR_GROUND);
                }
                break;

            default:
                setState(SMAJOR_GROUND);
                error("Internal", "Unknown minor state");
                break;
            }
            break;

        default:
            setState(SMAJOR_GROUND);
            error("Internal", "Unknown major state");
            break;
        }

        return IGNORE;
    }

    private static class Collector {
        private char a[] = new char[10];
        private int index = 0;

        void addChar(char c) {
            if (size() == a.length) {
                char bigger[] = new char[a.length * 2];
                System.arraycopy(a, 0, bigger, 0, a.length);
                a = bigger;
            }
            a[index++] = c;
        }
        char getCharAt(int i) {
            return a[i];
        }
        int size() {
            return index;
        }
        void reset() {
            index = 0;
        }
        char[] getArray() {
            if (size() == 0) {
                return new char[0];
            }
            char ret[] = new char[size()];
            System.arraycopy(a, 0, ret, 0, size());
            return ret;
        }
    }



    private void getNextParam() throws ParseException {
        if (getNumParams() == 25) {
            // Too many parameters, 25 is arbituary choosen
            setState(SMAJOR_GROUND);
            error("Esc-: Too many parameters, aborting sequence");
        }
        if (getNumParams() == params.length) {
            int biggerParams[] = new int[params.length * 2];
            System.arraycopy(params, 0, biggerParams, 0, params.length);
            params = biggerParams;
        }
        params[paramsIndex++] = getNextNumber();
    }
    private int getParam(int index) {
        return params[index];
    }
    private int getNumParams() {
        if (paramsIndex == 1 && getParam(0) == NOT_DEF) {
            return 0;
        }
        return paramsIndex;
    }

    private int getNormalRow(String seqName, char param) throws
        ParseException {
        return getNormalParam(seqName, "Row", param, ROWS);
    }

    private int getNormalColumn(String seqName, char param) throws
        ParseException {
        return getNormalParam(seqName, "Column", param, COLS);
    }

    private int getNormalParam(String seqName, String paramName,
                               char param, int max) throws
        ParseException {
        int n = param - 0x1f;
        if (n < 1 || n > max) {
            setState(SMAJOR_GROUND);
            error(seqName, paramName + " argument was out of range");
        }
        return n;
    }

    private int getExtendedRow(String seqName, int param) throws
        ParseException {
        return getExtendedParam(seqName, "Row", param, ROWS);
    }

    private int getExtendedColumn(String seqName, int param) throws
        ParseException {
        return getExtendedParam(seqName, "Column", param, COLS);
    }

    private int getExtendedParam(String seqName, String paramName,
                                 int param, int max) throws
        ParseException {
        if (param == NOT_DEF) {
            setState(SMAJOR_GROUND);
            error(seqName, paramName + " argument is missing");
        }
        if (param < 1) {
            setState(SMAJOR_GROUND);
            error(seqName, paramName + " argument is too low");
        }
        if (param > max) {
            param = max;
        }
        return param;
    }

    private void error(String seqName, String msg)
    throws ParseException {
        error(seqName + ": " + msg);
    }

    private void error(String msg)
    throws ParseException {
        throw new ParseException(msg);
    }

    private int getNextNumber() {
        if (coll.size() == 0) {
            return NOT_DEF;
        }
        String num = new String(coll.getArray());
        int ret = NOT_DEF;
        try {
            ret = Integer.parseInt(num);
        } catch (NumberFormatException e) {}
        coll.reset();
        return ret;
    }

    private char[] fromAsciiHex(String input) {
        return fromAsciiHex(input.toCharArray());
    }
    private char[] fromAsciiHex(char input[]) {
        if (input == null || (input.length % 2 != 0)) {
            return null;
        }
        char out[] = new char[input.length / 2];
        int hi, low;
        for (int i = 0; i < out.length; i++) {
            hi  = Character.digit(input[i*2]    , 16);
            low = Character.digit(input[i*2 + 1], 16);
            if (hi == -1 || low == -1) {
                return null;
            }
            out[i] = (char) (((hi << 4) & 0xf0) | (low & 0x0f));
        }
        return out;
    }
    private byte[] getByteArray(char input[]) {
        char chars[] = fromAsciiHex(input);
        if (chars == null) {
            return null;
        }

        byte out[] = new byte[chars.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) chars[i];
        }
        return out;
    }

    private void setState(int major) {
        setState(major, SMINOR_NONE);
    }
    private void setState(int major, int minor) {
        majorState = major;
        minorState = minor;

        coll.reset();
        for (int i = 0; i < paramsIndex; i++) {
            params[i] = NOT_DEF;
        }
        paramsIndex = 0;
    }

    private boolean isG0(char c) {
        return 0x20 <= c && c <= 0x7f;
    }
    private boolean isG1(char c) {
        return 0xa0 <= c && c <= 0xff;
    }
    private char shiftChar(char c) {
        if (isShifted && isG0(c)) {
            return (char) (c + 0x80);
        }
        return c;
    }
}

