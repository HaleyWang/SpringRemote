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

package com.mindbright.terminal;

/**
 * This class handles line based interaction with the user over a
 * terminal window. That is the class contains methods for printing
 * lines and read input from the user.
 *
 * @see TerminalWindow
 */
public final class LineReaderTerminal implements TerminalInputListener {

    TerminalWindow  terminal;
    StringBuffer    readLineStr;
    boolean         echoStar;
    boolean         isReadingLine;
    boolean         terminalChanged;
    boolean         terminalHadlLocalEcho ;

    volatile boolean ctrlCPressed;
    //volatile boolean ctrlDPressed;

    ExternalMessageException extMsg;

    /**
     * Represents an external message which came in while the
     * LineReaderTerminal was waiting for input.
     */
    static public class ExternalMessageException extends Exception {
    	private static final long serialVersionUID = 1L;
        public ExternalMessageException(String msg) {
            super(msg);
        }
    }

    /**
     * Creates a new instance and attaches to the given terminal.
     *
     * @param terminal the terminal window to work on
     */
    public LineReaderTerminal(TerminalWindow terminal) {
        this.terminal = terminal;
        terminal.addInputListener(this);

        String v = terminal.getProperty("local-echo");
        terminalHadlLocalEcho = Boolean.parseBoolean(v);
        if (terminalHadlLocalEcho) {
            boolean changed = terminal.getPropsChanged();
            terminal.setProperty("local-echo", "false");
            terminal.setPropsChanged(changed);
        }
    }

    /**
     * Detach from the terminal window
     */
    public void detach() {
        terminal.removeInputListener(this);

        if (terminalHadlLocalEcho) {
            boolean changed = terminal.getPropsChanged();
            terminal.setProperty("local-echo", "true");
            terminal.setPropsChanged(changed);
        }
    }

    /**
     * Print a string on the terminal
     *
     * @param str text to print
     */
    public void print(String str) {
        if(terminal != null) {
            terminal.write(str);
        } else {
            System.out.print(str);
        }
    }

    /**
     * Print a string on the terminal followind by a newline
     *
     * @param str text to print
     */
    public void println(String str) {
        if(terminal != null) {
            terminal.write(str + "\n\r");
        } else {
            System.out.println(str);
        }
    }

    /**
     * Abort waiting for user input. This causes any outstanding call
     * to <code>promptLine</code> to throw an
     * <code>ExternalMessageException</code> with the given message.
     *
     * @param msg message for exception
     */
    public void breakPromptLine(String msg) {
        if(isReadingLine) {
            synchronized(this) {
                extMsg = new ExternalMessageException(msg);
                this.notify();
            }
        }
    }

    /**
     * Read a line of input from the user. It is also possible to
     * specify a default value which is returned if the user just presses
     * return without entering anything.
     *
     * @param defaultVal default value which is returned if the user
     * did not enter anything.
     *
     * @return the entered text or defaultVal if no text was entered
     */
    public String readLine(String defaultVal) {
        synchronized(this) {
            if(defaultVal != null) {
                readLineStr = new StringBuffer(defaultVal);
                terminal.write(defaultVal);
            } else {
                readLineStr = new StringBuffer();
            }
            isReadingLine = true;
            terminalChanged = false;
            try {
                this.wait();
            } catch (InterruptedException e) {
                /* don't care */
            }
            isReadingLine = false;
        }
        if (terminalChanged) {
            return null;
        }
        return readLineStr.toString();
    }

    /**
     * Print a prompt and wait for user input. It is possibke to give
     * a default value and also to make the terminal echo '*' instead of
     * the entered text (for password entry)
     *
     * @param prompt lime of text to show at the start of the line
     * @param defaultVal default value which is returned if the user
     * just presses return without entering any text.
     * @param echoStar if true then the terminal echoes a '*; instead
     * of the typed character.
     * @return the entered text or defaultVal if no text was entered
     * @throws ExternalMessageException this is thrown if somebody
     * called <code>breakPromptLine</code> while this call was waiting
     * for the user.
     */
    public String promptLine(String prompt, String defaultVal,boolean echoStar)
        throws ExternalMessageException {
        String line = null;
        if(terminal != null) {
            terminal.setAttributeBold(true);
            terminal.write(prompt);
            terminal.setAttributeBold(false);
            this.echoStar = echoStar;
            line = readLine(defaultVal);
            this.echoStar = false;
        }
        if(extMsg != null) {
            ExternalMessageException msg = extMsg;
            extMsg = null;
            throw msg;
        }
        return line;
    }

    /**
     * Check if the user has pressed control-C since the last time
     * this function was called.
     *
     * @return true if control-C has been pressed
     */
    public boolean ctrlCPressed() {
        boolean pressed = ctrlCPressed;
        ctrlCPressed = false;
        return pressed;
    }

    // TerminalInputListener interface
    //
    public synchronized void typedChar(char c) {
        if(isReadingLine) {
            if(c == (char)127 || c == (char)0x08) {
                if(readLineStr.length() > 0) {
                    boolean ctrlChar = false;
                    if(readLineStr.charAt(readLineStr.length() - 1) < ' ') {
                        ctrlChar = true;
                    }
                    readLineStr.setLength(readLineStr.length() - 1);
                    terminal.write((char)8);
                    if(ctrlChar)
                        terminal.write((char)8);
                    terminal.write(' ');
                    if(ctrlChar)
                        terminal.write(' ');
                    terminal.write((char)8);
                    if(ctrlChar)
                        terminal.write((char)8);
                } else
                    terminal.ringBell();
            } else if(c == '\r') {
                this.notify();
                terminal.write("\n\r");
            } else {
                ctrlCPressed = false;
                //ctrlDPressed = false;
                readLineStr.append(c);
                if(echoStar)
                    terminal.write('*');
                else
                    terminal.write(c);
            }
        } else {
            if(c == (char)0x03) {
                ctrlCPressed = true;
            } // else if(c == (char)0x04) {
            //     ctrlDPressed = true;
            // }
        }
    }
    public void typedChar(byte[] b) {
        for (int i=0; i<b.length; i++) {
            typedChar((char)b[i]);
        }
    }

    public void sendBytes(byte[] b) {
        for(int i = 0; i < b.length; i++)
            typedChar((char)b[i]);
    }

    public void sendBytesDirect(byte[] b) {
        sendBytes(b);
    }

    public void signalWindowChanged(int rows, int cols, int vpixels,
                                    int hpixels) {}

    public synchronized void signalTermTypeChanged(String newTermType) {
        terminalChanged = true;
        this.notify();
    }

    public void sendBreak() {}


    /* DEBUG/TEST
    public static void main(String[] argv) {
    java.awt.Frame frame = new java.awt.Frame();
    TerminalWin terminal = new TerminalWin(frame);
    LineReaderTerminal linereader = new LineReaderTerminal(terminal);

    frame.setLayout(new java.awt.BorderLayout());
    frame.add(terminal.getPanelWithScrollbar(),
    java.awt.BorderLayout.CENTER);

    frame.pack();
    frame.show();

    linereader.println("Now entering lines...");
    String line;
    try {
     while(true) {
    line = linereader.promptLine("prompt> ", "", false);
    System.out.println("line: " + line);
     }
    } catch (Exception e) {
     System.out.println("Error: " + e);
    }
    }
    */

}
