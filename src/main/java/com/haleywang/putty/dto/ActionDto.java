package com.haleywang.putty.dto;

import com.haleywang.putty.service.action.ActionCategoryEnum;

import java.io.Serializable;

/**
 * @author haley
 */
public class ActionDto implements Action, Serializable {

    private String name;
    private String action;
    private String keyMap;
    private ActionCategoryEnum category;

    public ActionDto() {
    }

    public static ActionDto ofLayout(String action) {
        return new ActionDto(action, action, ActionCategoryEnum.LAYOUT);
    }

    public static ActionDto ofTermView(String action) {
        return new ActionDto(action, action, ActionCategoryEnum.TERM_PANEL);
    }


    public ActionDto(String name, String action, ActionCategoryEnum category) {
        this.name = name;
        this.action = action;
        this.category = category;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getKeyMap() {
        return keyMap;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public ActionCategoryEnum getCategory() {
        return category;
    }

    @Override
    public String getCategoryName() {
        return category == null ? "" : category.getName();
    }

    public void setCategory(ActionCategoryEnum category) {
        this.category = category;
    }

    @Override
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }


    public void setKeyMap(String keyMap) {
        this.keyMap = keyMap;
    }

    @Override
    public String searchText() {
        return name.toLowerCase() + "," + getCategoryName().toLowerCase();
    }
}
