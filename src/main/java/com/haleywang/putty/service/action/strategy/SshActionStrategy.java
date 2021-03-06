package com.haleywang.putty.service.action.strategy;

import com.haleywang.putty.dto.Action;
import com.haleywang.putty.dto.ConnectionDto;
import com.haleywang.putty.service.action.ActionStrategy;
import com.haleywang.putty.view.side.SideView;

/**
 * @author haley
 */
public class SshActionStrategy implements ActionStrategy {

    @Override
    public boolean execute(Action action) {
        if (action instanceof ConnectionDto) {
            SideView.getInstance().createConnectionsTab((ConnectionDto) action);
        }
        focusTerm();

        return true;
    }
}
