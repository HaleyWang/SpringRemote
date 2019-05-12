package com.haleywang.putty.service;

import com.haleywang.putty.dto.Action;
import com.haleywang.putty.service.action.ActionStrategy;
import com.haleywang.putty.service.action.ActionStrategyFactory;

public class ActionExecuteService {

    public boolean execute(Action actionDto) {
        ActionStrategy strategy = ActionStrategyFactory.INSTANCE.create(actionDto);
        if(strategy == null) {
            return false;
        }
        return strategy.execute(actionDto);
    }
}
