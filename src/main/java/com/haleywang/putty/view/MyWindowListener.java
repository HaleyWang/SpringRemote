package com.haleywang.putty.view;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public interface MyWindowListener extends WindowListener {



    default void windowOpened(WindowEvent e) {

    }

    default void windowClosing(WindowEvent e) {
        System.exit(0);
    }

    default void windowClosed(WindowEvent e) {

    }

    default void windowIconified(WindowEvent e) {

    }

    default void windowDeiconified(WindowEvent e) {

    }

    default void windowActivated(WindowEvent e) {

    }

    default void windowDeactivated(WindowEvent e) {

    }
}
