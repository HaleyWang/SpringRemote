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

import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;

/**
 * This is the base class for an "authenticator" representing a user and the
 * methods available to authenticate that user. This class is also used to
 * receive callbacks from the <code>SSH2UserAuth</code> with which it is
 * used. Since this class is not abstract it can be used as is, however to be
 * able to monitor the authentication process it is recommeded to subclass it.
 * <p>
 * The ordered list of available authentication methods is created by adding
 * each method together with its <code>SSH2AuthModule</code>. The order in which
 * modules are added is the order in which they will be used.
 *
 * @see SSH2UserAuth
 * @see SSH2AuthModule
 */
public class SSH2Authenticator {

    private Hashtable<String, SSH2AuthModule> authModules = new Hashtable<String, SSH2AuthModule>();
    private Vector<String> methodList = new Vector<String>();

    private String username;

    /**
     * Special constructor for creating an "anonymouse" authenticator. Note,
     * when using this constructor the username has to be set before
     * authentication using the method <code>setUsername</code>.
     */
    public SSH2Authenticator() {}

    /**
     * Basic constructor most commonly used.
     *
     * @param username the name of the user we represent
     */
    public SSH2Authenticator(String username) {
        this.username = username;
    }

    /**
     * Gets a comma separated list of the authentication methods currently set.
     *
     * @return a comma separated list of authentication methods 
     */
    public synchronized String getMethods() {
        StringBuilder buf = new StringBuilder();
        for(int i = 0; i < methodList.size(); i++) {
            buf.append(methodList.elementAt(i));
            if(i < methodList.size() - 1)
                buf.append(",");
        }
        return buf.toString();
    }

    /**
     * Gets an updated list of possible authentication methods. This
     * will be called if a method failed with the partial flag
     * set. This method may return a new list of auth methods (or null
     * which means keep the old one).
     *
     * @return a comma separated list of authentication methods 
     */
    public synchronized String getUpdatedMethods() {
        return null;
    }
    
    /**
     * Gets the name of the user we represent.
     *
     * @return the name of the user we represent
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the name of the user to be represented.
     *
     * @param username the name of the user to be represented
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Callback from <code>SSH2UserAuth</code> which gives a banner message as
     * part of the authentication. As defined in the spec. of the userauth
     * protocol. This method does nothing, it should be implemented in a
     * subclass.
     *
     * @param banner the banner message
     */
    public void displayBanner(String banner) {}

    /**
     * Gets the <code>SSH2AuthModule</code> implementing the given
     * authentication method.
     *
     * @param method the authentication method wanted
     */
    public synchronized SSH2AuthModule getModule(String method) {
        return authModules.get(method);
    }

    /**
     * Callback from <code>SSH2UserAuth</code> which gives the available
     * authentication methods as reported by peer. The list is comma
     * separated. This method does nothing, it should be implemented in a
     * subclass.
     *
     * @param methods the comma separated list of methods
     */
    public void peerMethods(String methods) {}

    /**
     * Callback from <code>SSH2UserAuth</code> which reports that there are no
     * more authentication methods left to try to authenticate with. This
     * method does nothing, it should be implemented in a subclass.
     */
    public void noMoreMethods() {}

    /**
     * Callback from <code>SSH2UserAuth</code> which reports that an exception
     * occured while running the given authentication method. This method does
     * nothing, it should be implemented in a subclass.
     *
     * @param method the authentication method that failed
     * @param e      the exception that occured in that method's module
     */
    public void moduleFailure(String method, SSH2Exception e) {}

    /**
     * Callback from <code>SSH2UserAuth</code> which reports that the user
     * canceled the given authentication method. That is a
     * <code>SSH2UserCancelException</code> was thrown from the current
     * <code>SSH2AuthModule</code>. This method does nothing, it should be
     * implemented in a subclass.
     *
     * @param method the authentication method that was canceled
     * @param reason a string giving the reason for cancelation if any
     */
    public void moduleCancel(String method, String reason) {}

    /**
     * Callback from <code>SSH2UserAuth</code> which reports that the given
     * authentication method failed. This is not an error, authentication will
     * continue if more methods are available. This method does nothing, it
     * should be implemented in a subclass.
     *
     * @param method the authentication method that failed
     * @param partial a boolean indicating if this failure was a
     * partial success or not (i.e. the method succeeded but more
     * methods are needed for full authentication).
     */
    public void authFailure(String method, boolean partial) {}

    /**
     * Callback from <code>SSH2UserAuth</code> which reports that the given
     * authentication method succeeded and that the user is now authenticated.
     * This method does nothing, it should be implemented in a subclass.
     *
     * @param method the authentication method that succeeded
     */
    public void authSuccess(String method) {}

    /**
     * Callback from <code>SSH2UserAuth</code> which reports that an error has
     * occured. That is the server disconnected or that the authentication has
     * been explicitly terminated (someone called the method
     * <code>terminate</code> in <code>SSH2UserAuth</code>) and that the
     * authentication is aborted. This method does nothing, it should be
     * implemented in a subclass.
     */
    public void authError() {}

    /**
     * Adds an authentication module (a class implementing
     * <code>SSH2AuthModule</code>) to the list of available authentication
     * methods. The module is added last in the current list, hence modules must
     * be added in the order of preference.
     *
     * @param module the authentication module
     */
    public final synchronized void addModule(SSH2AuthModule module) {
        addModule(module.getStandardName(), module);
    }

    /**
     * This method should only be used to add modules when one wants to asociate
     * them with another name than the standard name that the module itself
     * gives (method <code>getStandardName()</code>).
     *
     * @param method the name of the module used in the userauth protocol
     * @param module the authentication module
     */
    public final synchronized void addModule(String method,
            SSH2AuthModule module) {
        if(method == null || module == null)
            return;
        authModules.put(method, module);
        methodList.addElement(method);
    }

    /**
     * Removes the module associated with the given authentication method from
     * the list of available methods.
     */
    public final synchronized void removeModule(String method) {
        authModules.remove(method);
        methodList.removeElement(method);
    }

    /**
     * Clear the list of auth modules
     */
    public final synchronized void clearModules() {
        methodList.clear();
    }

    /**
     * Clears sensitive data from all modules
     */
    public void clearSensitiveData() {
        for (Enumeration<SSH2AuthModule> e = authModules.elements() ; e.hasMoreElements() ;) {
            e.nextElement().clearSensitiveData();
        }
    }
}
