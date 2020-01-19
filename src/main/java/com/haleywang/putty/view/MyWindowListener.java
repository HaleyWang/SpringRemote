package com.haleywang.putty.view;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;


/**
 * @author haley
 */
public interface MyWindowListener extends WindowListener {


    /**
     * Invoked the first time a window is made visible.
     *
     * @param e
     */
    @Override
    default void windowOpened(WindowEvent e) {

    }

    /**
     * Invoked when the user attempts to close the window
     * from the window's system menu.
     *
     * @param e
     */
    @Override
    default void windowClosing(WindowEvent e) {

    }

    /**
     * Invoked when a window has been closed as the result
     * of calling dispose on the window.
     *
     * @param e
     */
    @Override
    default void windowClosed(WindowEvent e) {
        System.exit(0);
    }

    /**
     * Invoked when a Window is no longer the active Window.
     *
     * @param e
     */
    @Override
    default void windowIconified(WindowEvent e) {

    }

    /**
     * Invoked when a window is changed from a normal to a
     * minimized state.
     *
     * @param e
     */
    @Override
    default void windowDeiconified(WindowEvent e) {

    }

    /**
     * Invoked when the Window is set to be the active Window. Only a Frame or
     * a Dialog can be the active Window.
     *
     * @param e
     */
    @Override
    default void windowActivated(WindowEvent e) {

    }

    /**
     * Invoked when a Window is no longer the active Window.
     *
     * @param e
     */
    @Override
    default void windowDeactivated(WindowEvent e) {

    }
}
