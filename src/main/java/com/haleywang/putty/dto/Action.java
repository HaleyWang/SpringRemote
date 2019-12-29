package com.haleywang.putty.dto;

import com.haleywang.putty.service.action.ActionCategoryEnum;

public interface Action {

    String getAction();
    String getName();
    String getKeyMap();
    ActionCategoryEnum getCategory();

    default String searchText() {
        return getName() + " " + getAction();
    }

    default String getCategoryName() {
        ActionCategoryEnum category = getCategory();
        return category == null ? "" : category.getName();
    }


}
