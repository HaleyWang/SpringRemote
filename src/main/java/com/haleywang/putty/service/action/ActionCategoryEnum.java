package com.haleywang.putty.service.action;

/**
 * @author haley
 */
public enum ActionCategoryEnum {

    /**
     * All actions category list
     */
    LAYOUT("Layout"),
    TERM_PANEL("TerminalPanel"),
    SSH("SSH"),
    THEME("Theme"),
    COMMAND("Command"),
    MENU("Menu");


    private String name;

    ActionCategoryEnum(String name) {
        this.name = name;

    }

    public String getName() {
        return name;
    }
}
