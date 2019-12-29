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

import java.util.Properties;
import java.util.NoSuchElementException;

/**
 * Core interface for a terminal window. This is implemented by the
 * actual implementation of the terminal window.
 */
public interface TerminalWindow {

    /**
     * Set title of terminal window.
     *
     * @param title the title
     */
    public void setTitle(String title);

    /**
     * Get the title
     *
     * @return the title of the terminal window
     */
    public String getTitle();

    /**
     * Get the number of rows shown.
     *
     * @return the number of rows
     */
    public int    rows();

    /**
     * Get the number of columns shown.
     *
     * @return the number of columns
     */
    public int    cols();

    /**
     * Get the number of vertical pixels used to represent the terminal.
     *
     * @return the number of vertical pixels
     */
    public int vpixels();

    /**
     * Get the number of horizontal pixels used to represent the terminal.
     *
     * @return the number of horizontal pixels
     */
    public int hpixels();

    /**
     * Write a byte to the terminal. The byte will be assumed to be in
     * the current remote encoding.
     *
     * @param b byte to write
     */
    public void write(byte b);

    /**
     * Write a character to the terminal.
     * <p>
     * The character is assumed to be in the java internal encoding.
     *
     * @param c character to write
     */
    public void write(char c);

    /**
     * Write a bunch of characters to the terminal.
     * <p>
     * The characters are assumed to be in the java internal encoding.
     *
     * @param c array containg the characters to write
     * @param off index in array of first character to write
     * @param len number of characters to write
     */
    public void write(char[] c, int off, int len);

    /**
     * Write a bunch of bytes to the terminal. The bytes will be
     * assumed to be in the current remote encoding.
     *
     * @param c array containg the characters to write
     * @param off index in array of first character to write
     * @param len number of characters to write
     */
    public void write(byte[] c, int off, int len);

    /**
     * Write a string to the terminal. The string is assumed to be in
     * the java internal encoding.
     *
     * @param str string to write
     */
    public void write(String str);

    /**
     * Add a input listener which listens to data from the user.
     *
     * @param listener input listener to add
     */
    public void addInputListener(TerminalInputListener listener);

    /**
     * Removes a previously added input listener
     *
     * @param listener input listener to remove
     */
    public void removeInputListener(TerminalInputListener listener);

    /**
     * Add a output listener which listens to data from the server.
     *
     * @param listener output listener to add
     */
    public void addOutputListener(TerminalOutputListener listener);

    /**
     * Removes a previously added output listener
     *
     * @param listener output listener to remove
     */
    public void removeOutputListener(TerminalOutputListener listener);

    /**
     * Attach a printer to this terminal.
     *
     * @param printer printer to attach
     */
    public void attachPrinter(TerminalPrinter printer);

    /**
     * Detach the previously attached printer.
     */
    public void detachPrinter();

    /**
     * Called whn the user tpyes a character
     *
     * @param c typed character
     */
    public void typedChar(char c);

    /**
     * May be called when the user pastes data. It may also be called
     * by some external entity whishing to simulate multiple key
     * presses.
     *
     * @param b array of bytes representing characters to input
     */
    public void sendBytes(byte[] b);

    /**
     * Send some bytes directly to the host. This does not echo the
     * characters and bypasses any line buffering. This is typically
     * used when replying to some query from the host.
     *
     * @param b array of bytes representing characters to send
     */
    public void sendBytesDirect(byte[] b);

    /**
     * Send a break singal to the server.
     */
    public void sendBreak();

    /**
     * Reset the terminal emulator to the default state.
     */
    public void reset();

    /**
     * Dump the current screen to the attached printer.
     */
    public void printScreen();

    /**
     * Start dumping a log of everything which is printed on the
     * terminal to the attached printer.
     */
    public void startPrinter();

    /**
     * Stop printing.
     */
    public void stopPrinter();

    /**
     * Get the terminal type which the window currently emulates.
     *
     * @return the name of the emulated terminal
     */
    public String terminalType();

    /**
     * Set a bunch of terminal properties
     *
     * @param newProps new properties
     * @param merge if true the new properties are merged with any
     * previous properties. If false all old properties are deleted.
     */
    public void setProperties(Properties newProps, boolean merge)
        throws IllegalArgumentException, NoSuchElementException;

    /**
     * Set a single terminal property.
     *
     * @param key property to set
     * @param value value to set it to
     */
    public void setProperty(String key, String value)
        throws IllegalArgumentException, NoSuchElementException;

    /**
     * Get the current terminal properties
     *
     * @return the current properties
     */
    public Properties getProperties();

    /**
     * Get the value of a single property.
     *
     * @param key name of perty to get value of
     * @return the value of the property. Null if the peroperty is not
     * defined and there is no default value.
     */
    public String getProperty(String key);

    /**
     * Reset all properties to their default values.
     */
    public void resetToDefaults();

    /**
     * Check if any properties have changed.
     *
     * @return true if any properties has changed
     */
    public boolean getPropsChanged();

    /**
     * Change the properties changed flag.
     *
     * @param value new value.
     */
    public void  setPropsChanged(boolean value);

    /**
     * Get the terminal options.
     */
    public TerminalOption[] getOptions();

    /**
     * Search for a string in the terminal window
     *
     * @param lastContext used as a starying point, if not null, so
     * tha the search may continue from the last position
     * @param key string to search for
     * @param reverse if true the search is performed backwards
     * @param caseSens if true the key is case sensitive
     *
     * @return a search context which describes where the key was
     * found or <code>null</code> if the key was not found.
     */
    public SearchContext search(SearchContext lastContext, String key,
                                boolean reverse, boolean caseSens);

    /**
     * This shortcut menthod is used to enable/disable bold
     * characters.
     * <p>
     * The shortcut methods are used by local code to achieve certain
     * effects without knowing which terminal emulation is active.
     *
     * @param set if true turn on bold for subsequent characters.
     */
    public void setAttributeBold(boolean set);

    /**
     * This shortcut method clears the screen.
     * <p>
     * The shortcut methods are used by local code to achieve certain
     * effects without knowing which terminal emulation is active.
     */
    public void clearScreen();

    /**
     * This shortcut method rings the terminal bell.
     * <p>
     * The shortcut methods are used by local code to achieve certain
     * effects without knowing which terminal emulation is active.
     */
    public void ringBell();

    /**
     * This shortcut method moves the cursor to the given position.
     * <p>
     * The shortcut methods are used by local code to achieve certain
     * effects without knowing which terminal emulation is active.
     *
     * @param row row to place cursor on
     * @param col column to place cursor on
     */
    public void setCursorPos(int row, int col);

    /**
     * This shortcut method clears the current line.
     * <p>
     * The shortcut methods are used by local code to achieve certain
     * effects without knowing which terminal emulation is active.
     */
    public void clearLine();
}
