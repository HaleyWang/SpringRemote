package com.haleywang.putty.service.action.strategy;

import com.haleywang.putty.dto.Action;
import com.haleywang.putty.dto.CommandDto;
import com.haleywang.putty.service.action.ActionStrategy;
import com.haleywang.putty.view.SpringRemoteView;

public class CommandActionStrategy implements ActionStrategy {

    @Override
    public boolean execute(Action action) {
        if(action instanceof CommandDto){
            SpringRemoteView.getInstance().onTypedString(((CommandDto)action).getCommand());
        }

        return true;
    }
}
