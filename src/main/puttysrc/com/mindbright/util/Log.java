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

package com.mindbright.util;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * A class which handles logging messages to a stream. All printed
 * messages are time-stamped. Messages can have different levels and
 * it is possible to change which levels should be written to the
 * stream. The levels are ordered like this (in decreasing order of
 * severity):
 * <ol>
 *   <li><code>LEVEL_EMERG</code>   - emergency messages</li>
 *   <li><code>LEVEL_ALERT</code>   - alert messages</li>
 *   <li><code>LEVEL_ERROR</code>   - error messages</li>
 *   <li><code>LEVEL_WARNING</code> - warning messages</li>
 *   <li><code>LEVEL_NOTICE</code>  - notices</li>
 *   <li><code>LEVEL_INFO</code>    - informational messages</li>
 *   <li><code>LEVEL_DEBUG</code>   - debug messages</li>
 *   <li><code>LEVEL_DEBUG2</code>  - detailed debug messages</li>
 * </ol>
 */
public class Log {
    public final static int LEVEL_EMERG   = 0;
    public final static int LEVEL_ALERT   = 1;
    public final static int LEVEL_ERROR   = 2;
    public final static int LEVEL_WARNING = 3;
    public final static int LEVEL_NOTICE  = 4;
    public final static int LEVEL_INFO    = 5;
    public final static int LEVEL_DEBUG   = 6;
    public final static int LEVEL_DEBUG2  = 7;

    volatile int currentLevel = 0;

    private OutputStream logOut = null;

    /**
     * Construct an instance which logs to System.err and which ignores
     * messages below the given level.
     *
     * @param level the lowest level of messages to log
     */
    public Log(int level) {
        this(System.err, level);
    }

    /**
     * Construct an instance which logs to the given stream and which ignores
     * messages below the given level.
     *
     * @param logOut the stream to print log messages to
     * @param level the lowest level of messages to log
     */
    public Log(OutputStream logOut, int level) {
        this.logOut       = logOut;
        this.currentLevel = level;
    }

    /**
     * Changes the stream log messages are printed on
     *
     * @param logOut the new stream to print messages on
     */
    public synchronized void setLogOutputStream(OutputStream logOut) {
        this.logOut = logOut;
    }

    /**
     * Closes the output stream (unless it is System.err)
     */
    public void close() {
        if(logOut != null && logOut != System.err) {
            try {
                logOut.close();
            } catch (IOException e) { }
        }
    }

    /**
     * Log a message
     *
     * @param level the severity of this message
     * @param callClass name of class generating the message
     * @param message the log message
     */
    public void message(int level, String callClass, String message) {
        message(level, callClass, null, message);
    }


    /**
     * Log a message
     *
     * @param level the severity of this message
     * @param callClass name of class generating the message
     * @param callMethod name of method in calling class which
     *                   generated message
     * @param message the log message
     */
    public synchronized void message(int level, String callClass,
                                     String callMethod,
                                     String message) {
        if (logOut == null) return;

        if(level <= currentLevel) {
            String methStr = (callMethod != null ? "." + callMethod + "()" :
                              "");
            DateFormat df = new SimpleDateFormat( "HH:mm:ss.SSS" );
            df.setTimeZone( TimeZone.getDefault() ); // JDK 1.1

            String logStr  = "** " + df.format(new Date()) + " "
                             + callClass + methStr + " : '" + message + "'" + "\n";

            byte[] logMsg = logStr.getBytes();
            try {
                if(logOut != null) {
                    logOut.write(logMsg, 0, logMsg.length);
                    logOut.flush();
                }
            } catch (IOException e) {
                if (!System.err.equals(logOut)) {
                    e.printStackTrace();
                    System.err.println("ERROR: Couldn't write to log");
                    System.err.println(logStr);
                }
            }
        }
    }

    /**
     * Log an error message
     *
     * @param callClass name of class generating the message
     * @param callMethod name of method in calling class which
     *                   generated message
     * @param message the log message
     */
    public void error(String callClass, String callMethod, String message) {
        message(LEVEL_ERROR, callClass, callMethod, message);
    }

    /**
     * Log a warning message
     *
     * @param callClass name of class generating the message
     * @param message the log message
     */
    public void warning(String callClass, String message) {
        message(LEVEL_ERROR, callClass, null, message);
    }

    /**
     * Log a notice message
     *
     * @param callClass name of class generating the message
     * @param message the log message
     */
    public void notice(String callClass, String message) {
        message(LEVEL_NOTICE, callClass, null, message);
    }

    /**
     * Log an informational message
     *
     * @param callClass name of class generating the message
     * @param message the log message
     */
    public void info(String callClass, String message) {
        message(LEVEL_INFO, callClass, null, message);
    }

    /**
     * Log a debug message
     *
     * @param callClass name of class generating the message
     * @param callMethod name of method in calling class which
     *                   generated message
     * @param message the log message
     */
    public void debug(String callClass, String callMethod, String message) {
        message(LEVEL_DEBUG, callClass, callMethod, message);
    }

    /**
     * Log a debug message
     *
     * @param callClass name of class generating the message
     * @param message the log message
     */
    public void debug(String callClass, String message) {
        message(LEVEL_DEBUG, callClass, null, message);
    }

    /**
     * Log a detailed debug message
     *
     * @param callClass name of class generating the message
     * @param callMethod name of method in calling class which
     *                   generated message
     * @param message the log message
     */
    public void debug2(String callClass, String callMethod, String message) {
        message(LEVEL_DEBUG2, callClass, callMethod, message);
    }

    /**
     * Log a detailed debug message which also includes a hex-dump of
     * some data.
     *
     * @param callClass name of class generating the message
     * @param callMethod name of method in calling class which
     *                   generated message
     * @param message the log message
     * @param dumpBuf array containing data to be dumped
     * @param off offset of first byte to dump
     * @param len number of bytes to dump
     */
    public synchronized void debug2(String callClass, String callMethod,
                                    String message,
                                    byte[] dumpBuf, int off, int len) {
        if (logOut == null) return;
        message(LEVEL_DEBUG2, callClass, callMethod, message);
        if(currentLevel >= LEVEL_DEBUG2) {
            HexDump.print(logOut, dumpBuf, off, len);
            try { logOut.flush(); } catch (IOException e) { }
        }
    }

    /**
     * Log a detailed debug message which also includes a hex-dump of
     * some data.
     *
     * @param callClass name of class generating the message
     * @param callMethod name of method in calling class which
     *                   generated message
     * @param message the log message
     * @param dumpBuf array containing data to be dumped
     */
    public void debug2(String callClass, String callMethod, String message,
                       byte[] dumpBuf) {
        debug2(callClass, callMethod, message, dumpBuf, 0, dumpBuf.length);
    }

    /**
     * Change the log cutoff level.
     *
     * @param level the lowest level of messages to log
     */
    public void setLevel(int level) {
        currentLevel = level;
    }

    /**
     * Write a throwable to the log
     *
     * @param t throwable to dump
     */
    public void debug(Throwable t) {
        try {
            if (t != null && logOut != null)
                t.printStackTrace(new PrintStream(logOut));
        } catch (Throwable tt) {
            tt.printStackTrace();
        } 
    }
}
