package com.haleywang.putty.service.action;


import com.haleywang.putty.dto.Action;


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
}
