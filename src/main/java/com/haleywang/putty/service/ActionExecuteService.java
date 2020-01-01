package com.haleywang.putty.service;

import com.haleywang.putty.dto.Action;
import com.haleywang.putty.service.action.ActionStrategy;
import com.haleywang.putty.service.action.ActionStrategyFactory;

/**
 * @author haley
 */
public class ActionExecuteService {

    private static class SingletonHolder {
        private static final ActionExecuteService INSTANCE = new ActionExecuteService();
    }

    private ActionExecuteService() {
    }

    public static final ActionExecuteService getInstance() {
        return ActionExecuteService.SingletonHolder.INSTANCE;
    }


    public boolean execute(Action actionDto) {
        ActionStrategy strategy = ActionStrategyFactory.INSTANCE.create(actionDto);
        if (strategy == null) {
            return false;
        }
        return strategy.execute(actionDto);
    }
}
