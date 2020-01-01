package com.haleywang.putty.service.action;

import com.haleywang.putty.dto.Action;
import com.haleywang.putty.dto.ActionDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author haley
 */
public class ActionsData {

    private ActionsData() {
    }

    private static final List<Action> THEME_ACTIONS_DATA = Arrays.asList(
            ActionDto.ofTheme("FlatDarculaLaf"),
            ActionDto.ofTheme("FlatIntelliJLaf"));

    private static final List<Action> LAYOUT_ACTIONS_DATA = Arrays.asList(ActionDto.ofLayout("Grid 1"),
            ActionDto.ofLayout("Grid H2"),
            ActionDto.ofLayout("Grid V2"),
            ActionDto.ofLayout("Grid 4"));


    private static final List<Action> TERM_VIEW_ACTIONS_DATA = Arrays.asList(ActionDto.ofTermView("Terminal Panel 1"),
            ActionDto.ofTermView("Terminal Panel 2"),
            ActionDto.ofTermView("Terminal Panel 3"),
            ActionDto.ofTermView("Terminal Panel 4"));


    private static final List<Action> ACTIONS_DATA = new ArrayList<>();

    static {
        ACTIONS_DATA.addAll(LAYOUT_ACTIONS_DATA);
        ACTIONS_DATA.addAll(TERM_VIEW_ACTIONS_DATA);
        ACTIONS_DATA.addAll(THEME_ACTIONS_DATA);
    }


    public static List<Action> getLayoutActionsData() {
        return LAYOUT_ACTIONS_DATA;
    }


    public static List<Action> getActionsData() {
        return ACTIONS_DATA;
    }


    public static int getIndex(List<Action> actions, String actionName) {
        for (int i = 0, n = actions.size(); i < n; i++) {
            if (actions.get(i).getName().equals(actionName)) {
                return i;
            }
        }
        return 0;
    }


    public static List<Action> getTermViewActionsData() {
        return TERM_VIEW_ACTIONS_DATA;
    }
}
