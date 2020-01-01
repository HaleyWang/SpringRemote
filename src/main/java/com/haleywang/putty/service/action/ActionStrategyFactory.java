package com.haleywang.putty.service.action;

import com.haleywang.putty.dto.Action;
import com.haleywang.putty.service.action.strategy.CommandActionStrategy;
import com.haleywang.putty.service.action.strategy.LayoutActionStrategy;
import com.haleywang.putty.service.action.strategy.SshActionStrategy;
import com.haleywang.putty.service.action.strategy.TermPanelActionStrategy;

import java.util.HashMap;
import java.util.Map;

/**
 * @author haley
 */
public enum ActionStrategyFactory {

    /**
     * INSTANCE
     */
    INSTANCE;

    private static Map<ActionCategoryEnum, ActionStrategy> strategyMap = new HashMap<>();

    static {
        strategyMap.put(ActionCategoryEnum.LAYOUT, new LayoutActionStrategy());
        strategyMap.put(ActionCategoryEnum.TERM_PANEL, new TermPanelActionStrategy());
        strategyMap.put(ActionCategoryEnum.SSH, new SshActionStrategy());
        strategyMap.put(ActionCategoryEnum.COMMAND, new CommandActionStrategy());
    }

    public ActionStrategy create(ActionCategoryEnum type) {
        return strategyMap.get(type);
    }


    public ActionStrategy create(Action action) {
        return create(action.getCategory());
    }
}
