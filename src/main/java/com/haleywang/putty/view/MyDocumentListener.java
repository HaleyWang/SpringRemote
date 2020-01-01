package com.haleywang.putty.view;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * @author haley
 */
public interface MyDocumentListener extends DocumentListener {


    /**
     * Gives notification that a portion of the document has been
     * removed.
     *
     * @param e the document event
     */
    @Override
    default void removeUpdate(DocumentEvent e) {

    }

    /**
     * Gives notification that an attribute or set of attributes changed.
     *
     * @param e the document event
     */
    @Override
    default void changedUpdate(DocumentEvent e) {

    }
}
