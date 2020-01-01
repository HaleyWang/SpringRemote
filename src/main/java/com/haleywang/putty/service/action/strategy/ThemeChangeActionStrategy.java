package com.haleywang.putty.service.action.strategy;

import com.haleywang.putty.dto.Action;
import com.haleywang.putty.service.action.ActionStrategy;
import com.haleywang.putty.view.SpringRemoteView;

/**
 * @author haley
 */
public class ThemeChangeActionStrategy implements ActionStrategy {
    @Override
    public boolean execute(Action action) {

        SpringRemoteView.getInstance().changeTheme(action.getAction());

        return true;
    }
}
