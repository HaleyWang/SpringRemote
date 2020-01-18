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

package com.mindbright.ssh2;

/**
 * This interface defines the different types of prompts which are needed for
 * interactive authentication. It's made generic to be able to allow
 * flexibility in the level of sophistication one wants for user
 * interaction.
 * <p>
 * Which of these functions are called and with which prompts are
 * provided is entirely up to the ssh-server. The server expects that
 * these functions will interact via graphical dialogs with the
 * user. Therefore the function calls includes things as instructions
 * and suggested names of dialogs and settings which tells if the user
 * should see what they type <code>echo</code> or not, as when
 * entering passwords.
 *
 * @see SSH2AuthKbdInteract
 * @see SSH2AuthPassword
 */
public interface SSH2Interactor {
    /**
     * Prompt for a single string.
     *
     * @param prompt The prompt string to show
     * @param echo   True if the text the user enters should be
     *               echoed.
     *
     * @return The text entered by the user.
     */
    public String promptLine(String prompt, boolean echo)
    throws SSH2UserCancelException;

    /**
     * Prompt for multiple strings. The expectation here is that the
     * client will put up a dialog where the user sees multiple prompts
     * and input fields.
     *
     * @param prompts List of prompts to show
     * @param echos   List of boolean values which indicates if the
     *                text entered for the corresponding prompt should
     *                be echoed.
     *
     * @return An array of strings which contains on element for each
     * prompt, in the same order. The elements should contain the text
     * the user entered.
     */
    public String[] promptMulti(String[] prompts, boolean[] echos)
    throws SSH2UserCancelException;

    /**
     * Prompt for multiple strings. The expectation here is that the
     * client will put up a dialog where the user sees multiple prompts
     * and input fields. This version of the call includes more
     * elements which should be shown in the dialog.
     *
     * @param name    Suggested title of the dialog
     * @param instruction Instructions to show to user in the dialog
     * @param prompts List of prompts to show
     * @param echos   List of boolean values which indicates if the
     *                text entered for the corresponding prompt should
     *                be echoed.
     *
     * @return An array of strings which contains on element for each
     * prompt, in the same order. The elements should contain the text
     * the user entered.
     */
    public String[] promptMultiFull(String name, String instruction,
                                    String[] prompts, boolean[] echos)
    throws SSH2UserCancelException;

    /**
     * This function is only used by AppGate internal code so there is
     * no need to actually implement it in any other code.
     * <p>
     * It is used to let the user select one element in a list.
     *
     * @param name        Suggested title of dialog
     * @param instruction Instructions to show to user
     * @param choices     Array of possible choices
     *
     * @return index of selected list item
     */
    public int promptList(String name, String instruction, String[] choices)
    throws SSH2UserCancelException;
}
