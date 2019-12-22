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
 * This class implements a module for password authentication as defined in the
 * userauth protocol spec. It can either be used passively (i.e. the password is
 * known beforehand), or it can be used interactively through the
 * <code>SSH2Interactor</code> callback interface.
 *
 * @see SSH2AuthModule
 */
public class SSH2AuthPassword implements SSH2AuthModule {

    public final static String STANDARD_NAME = "password";

    private String password;
    private String newPassword;
    private SSH2Interactor interactor;
    private String prompt;

    /**
     * Creates an instance which will never query the user. It will
     * only use the given password. The password is only tried once,
     * after that the module gives up.
     *
     * @param password Password to authenticate with once
     */
    public SSH2AuthPassword(String password) {
        this(null, null, password);
    }

    /**
     * Creates an instance which will always query the user.
     *
     * @param interactor Interactor used to query the user
     * @param prompt The prompt which will be shown to the user
     */
    public SSH2AuthPassword(SSH2Interactor interactor, String prompt) {
        this(interactor, prompt, null);
    }

    /**
     * Creates an instance which will first test with the given
     * password. If that fails then the user will be queried interactively.
     *
     * @param interactor Interactor used to query the user
     * @param prompt The prompt which will be shown to the user
     * @param password Password to try to authenticate with once
     */
    public SSH2AuthPassword(SSH2Interactor interactor, String prompt,
                            String password) {
        this.interactor = interactor;
        this.prompt     = prompt;
        setPassword(password);
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    protected String getPassword() throws SSH2UserCancelException {
        if(password != null) {
            String tmp = password;
            password = null;
            return tmp;
        }
        if(interactor != null) {
            while (password == null) {
                password = interactor.promptLine(prompt, false);
            }
        } else {
            password = "";
        }
        return password;
    }

    protected String getNewPassword(String prompt, String language)
    throws SSH2UserCancelException {
        if(newPassword != null)
            return newPassword;
        if(interactor != null) {
            // !!! TODO how is one expected to give user a chance to
            // rewrite password... (given only one prompt)
            //
            // TODO 2 language tag
            //
            while (newPassword == null) {
                newPassword = interactor.promptLine(prompt, false);
            }
        } else {
            newPassword = "";
        }
        return newPassword;
    }

    public String getStandardName() {
        return STANDARD_NAME;
    }

    public SSH2TransportPDU processMethodMessage(SSH2UserAuth userAuth,
            SSH2TransportPDU pdu)
    throws SSH2UserCancelException {
        switch(pdu.getType()) {
        case SSH2.MSG_USERAUTH_PASSWD_CHANGEREQ:
            String prompt   = pdu.readJavaString(); // FIXME: should be: readUTF8String()
            String language = pdu.readJavaString();
            pdu = createChangeRequest(userAuth, prompt, language);
            break;

        default:
            userAuth.getTransport().getLog().
            warning("SSH2AuthPassword",
                    "received unexpected packet of type: " + pdu.getType());
            pdu = null;
        }

        return pdu;
    }

    public SSH2TransportPDU startAuthentication(SSH2UserAuth userAuth)
    throws SSH2UserCancelException {
        SSH2TransportPDU pdu = userAuth.createUserAuthRequest(STANDARD_NAME);
        pdu.writeBoolean(false);
        pdu.writeUTF8String(getPassword());
        return pdu;
    }

    private SSH2TransportPDU createChangeRequest(SSH2UserAuth userAuth,
            String prompt,
            String language)
    throws SSH2UserCancelException {
        SSH2TransportPDU pdu = userAuth.createUserAuthRequest(STANDARD_NAME);
        pdu.writeBoolean(true);
        if (password != null && newPassword != null) {
            pdu.writeUTF8String(getPassword());
            pdu.writeUTF8String(getNewPassword(prompt, language));
        } else {
            String prompts[] = new String[3];
            boolean echos[] = { false, false, false };
            String passwords[];
            String badpass = "";
            
            prompts[0] = "Old password: ";
            prompts[1] = "New password: ";
            prompts[2] = "Retype new password: ";
            
            do {       
                passwords = interactor.promptMultiFull(
                    "Change password",
                    badpass + 
                    (prompt!=null ? prompt : ""),
                    prompts, echos);            
                badpass = "New passwords differs. ";
            } while ( false == passwords[1].equals(passwords[2]));
            pdu.writeUTF8String(passwords[0]);
            pdu.writeUTF8String(passwords[1]);

        }
        return pdu;
    }

    public void clearSensitiveData() {
        password = null;
        newPassword = null;
    }

    public boolean retryPointless() {
        return password == null && interactor == null;
    }
}
