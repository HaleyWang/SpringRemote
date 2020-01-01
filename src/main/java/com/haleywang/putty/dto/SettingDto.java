package com.haleywang.putty.dto;

import java.io.Serializable;

/**
 * @author haley
 */
public class SettingDto implements Serializable {
    private int tabLayout;

    public int getTabLayout() {
        return tabLayout;
    }

    public void setTabLayout(int tabLayout) {
        this.tabLayout = tabLayout;
    }
}
