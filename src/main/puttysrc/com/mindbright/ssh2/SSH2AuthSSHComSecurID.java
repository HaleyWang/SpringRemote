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
 * This class implements a module for SecurID authentication as defined by SSH
 * Inc as the 'local' method 'securid-1@ssh.com'. It uses the interface
 * <code>SSH2Interactor</code> for all interactions generated in the
 * authentication process. This method is not part of any public standard, it's
 * currently only available with SSH2 servers from SSH Communcations.
 *
 * @see SSH2AuthModule
 * @see SSH2Interactor
 */
public class SSH2AuthSSHComSecurID implements SSH2AuthModule {

    public final static String STANDARD_NAME = "securid-1@ssh.com";

    SSH2Interactor interactor;
    String         promptPIN;
    String         promptNext;
    String         promptNew;
    String         promptNew2;
    String         promptSelect;
    String         promptSystem;

    public SSH2AuthSSHComSecurID(SSH2Interactor interactor,
                                 String promptPIN,
                                 String promptNext,
                                 String promptNew,
                                 String promptNew2,
                                 String promptSelect,
                                 String promptSystem) {
        this.interactor   = interactor;
        this.promptPIN    = promptPIN;
        this.promptNext   = promptNext;
        this.promptNew    = promptNew;
        this.promptNew2   = promptNew2;
        this.promptSelect = promptSelect;
        this.promptSystem = promptSystem;
    }

    public String getStandardName() {
        return STANDARD_NAME;
    }

    public SSH2TransportPDU processMethodMessage(SSH2UserAuth userAuth,
            SSH2TransportPDU pdu)
    throws SSH2UserCancelException {
        switch(pdu.getType()) {
        case SSH2.MSG_USERAUTH_SECURID_CHALLENGE:
            pdu = getPasscodeResponse(userAuth, true);
            break;
        case SSH2.MSG_USERAUTH_SECURID_NEW_PIN_REQD:
            int     minPinLen      = pdu.readInt();
            int     maxPinLen      = pdu.readInt();
            int     userSelectable = pdu.readByte();
            boolean alphaNumeric   = pdu.readBoolean();
            String  systemPIN      = pdu.readJavaString();

            String newPIN = getNewPIN(systemPIN, userSelectable, alphaNumeric,
                                      minPinLen, maxPinLen);

            pdu = createResponse(userAuth, newPIN, true);
            break;
        default:
            userAuth.getTransport().getLog().
            warning("SSH2AuthSSHComSecurID",
                    "received unexpected packet of type: " + pdu.getType());
            pdu = null;
        }
        return pdu;
    }

    public SSH2TransportPDU startAuthentication(SSH2UserAuth userAuth)
    throws SSH2UserCancelException {
        return getPasscodeResponse(userAuth, false);
    }

    private String getNewPIN(String systemPIN, int userSelectable,
                             boolean alphaNumeric,
                             int minLen, int maxLen)
    throws SSH2UserCancelException {
        String newPIN = "";
        String a      = null;
        if(userSelectable == SSH2.SSH_SECURID_USER_SELECTABLE_PIN) {
            while (a == null) {
                a = interactor.promptLine(promptSelect, true);
            }
            if(a.equalsIgnoreCase("yes") || a.equalsIgnoreCase("y")) {
                userSelectable = SSH2.SSH_SECURID_MUST_CHOOSE_PIN;
            } else {
                userSelectable = SSH2.SSH_SECURID_CANNOT_CHOOSE_PIN;
            }
        }
        if(userSelectable == SSH2.SSH_SECURID_CANNOT_CHOOSE_PIN) {
            a = null;
            while (a == null) {
                a = interactor.promptLine(promptSystem + systemPIN +
                                          " (yes/no)? ", true);
            }
            if(a.equalsIgnoreCase("yes") || a.equalsIgnoreCase("y")) {
                newPIN = systemPIN;
            }
        } else if(userSelectable == SSH2.SSH_SECURID_MUST_CHOOSE_PIN) {
            String[] aa;
            do {
                aa = interactor.promptMulti(new String[] { promptNew +
                                            " (" + minLen + " - " + maxLen +
                                            (alphaNumeric ? "characters" :
                                             "digits") + ")", promptNew2 },
                                            new boolean[] { false, false });
            } while(!aa[0].equals(aa[1]));
            newPIN = aa[0];
        }
        return newPIN;
    }

    private SSH2TransportPDU getPasscodeResponse(SSH2UserAuth userAuth,
            boolean challenge)
    throws SSH2UserCancelException {
        String prompt;
        if(challenge) {
            prompt = promptNext;
        } else {
            prompt = promptPIN;
        }

        String pin = null;
        while (pin == null) {
            pin = interactor.promptLine(prompt, true);
        }

        return createResponse(userAuth, pin, challenge);
    }

    private SSH2TransportPDU createResponse(SSH2UserAuth userAuth,
                                            String pin,
                                            boolean challenge) {
        SSH2TransportPDU pdu =
            userAuth.createUserAuthRequest(STANDARD_NAME);
        pdu.writeBoolean(challenge);
        pdu.writeString(pin);
        return pdu;
    }

    public void clearSensitiveData() {
        // Nothing to do
    }

    public boolean retryPointless() {
        return false;
    }
}
