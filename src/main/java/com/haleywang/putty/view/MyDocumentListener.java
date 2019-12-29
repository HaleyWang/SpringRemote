package com.haleywang.putty.view;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public interface MyDocumentListener extends DocumentListener {


    @Override
    default void removeUpdate(DocumentEvent e) {

    }

    @Override
    default void changedUpdate(DocumentEvent e) {

    }
}
