package com.haleywang.putty.dto;

import com.haleywang.putty.service.action.ActionCategoryEnum;


/**
 * @author haley
 */
public interface Action {

    /**
     * Get action
     *
     * @return
     */
    String getAction();

    /**
     * get action name
     *
     * @return
     */
    String getName();

    /**
     * Get key map
     *
     * @return
     */
    String getKeyMap();

    /**
     * Get Action category
     *
     * @return
     */
    ActionCategoryEnum getCategory();


    /**
     * Get text for search
     *
     * @return
     */
    default String searchText() {
        return getName() + " " + getAction();
    }


    /**
     * Get category name
     *
     * @return
     */
    default String getCategoryName() {
        ActionCategoryEnum category = getCategory();
        return category == null ? "" : category.getName();
    }


}
