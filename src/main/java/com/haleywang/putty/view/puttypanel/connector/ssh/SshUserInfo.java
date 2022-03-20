package com.haleywang.putty.view.puttypanel.connector.ssh;

import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;
import com.jediterm.terminal.Questioner;

class SshUserInfo implements UserInfo, UIKeyboardInteractive {
    private final Questioner myQuestioner;
    private String myPassword;
    private String myPassPhrase;

    public SshUserInfo(Questioner questioner) {
        this.myQuestioner = questioner;
    }

    @Override
    public String getPassphrase() {
        return myPassPhrase;
    }

    @Override
    public String getPassword() {
        return myPassword;
    }

    public void setPassword(String password) {
        this.myPassword = password;
    }

    @Override
    public boolean promptPassphrase(String message) {
        myPassPhrase = myQuestioner.questionHidden(message + ":");
        return true;
    }

    @Override
    public boolean promptPassword(String message) {
        myPassword = myQuestioner.questionHidden(message + ":");
        return true;
    }

    @Override
    public boolean promptYesNo(String message) {
        String yn = myQuestioner.questionVisible(message + " [Y/N]:", "Y");
        String lyn = yn.toLowerCase();
        return ("y".equals(lyn) || "yes".equals(lyn));
    }

    @Override
    public void showMessage(String message) {
        myQuestioner.showMessage(message);
    }

    @Override
    public String[] promptKeyboardInteractive(final String destination, final String name,
                                              final String instruction, final String[] prompt, final boolean[] echo) {
        int len = prompt.length;
        String[] results = new String[len];
        if (destination != null && destination.length() > 0) {
            myQuestioner.showMessage(destination);
        }
        if (name != null && name.length() > 0) {
            myQuestioner.showMessage(name);
        }
        myQuestioner.showMessage(instruction);
        for (int i = 0; i < len; i++) {
            String promptStr = prompt[i];
            results[i] = echo[i] ? myQuestioner.questionVisible(promptStr, null) :
                    myQuestioner.questionHidden(promptStr);
        }
        return results;
    }
}