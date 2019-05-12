package com.haleywang.putty.service.action;

public enum ActionCategoryEnum {
    LAYOUT("Layout"),
    TERM_PANEL("Terminal Panel"),
    SSH("SSH"),
    COMMAND("Command"),
    MENU("Menu")
    ;


    private String name;
    ActionCategoryEnum(String name) {
        this.name = name;

    }

    public String getName() {
        return name;
    }
}
