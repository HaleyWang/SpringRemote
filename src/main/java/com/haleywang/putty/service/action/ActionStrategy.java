package com.haleywang.putty.service.action;


import com.haleywang.putty.dto.Action;
import com.haleywang.putty.view.SpringRemoteView;
import org.alvin.puttydemo.PuttyPane;

import java.awt.Component;


/**
 * @author haley
 */
public interface ActionStrategy {


    /**
     * Execute an action
     *
     * @param action
     * @return
     */
    boolean execute(Action action);

    default void focusTerm() {
        Component component = SpringRemoteView.getInstance().getCurrentTabPanel().getSelectedComponent();
        if (component instanceof PuttyPane) {
            PuttyPane puttyPane = (PuttyPane) component;
            puttyPane.setTermFocus();
        }
    }
}
