package com.haleywang.putty.dto;

import java.io.Serializable;

/**
 * @author haley
 */
public class SettingDto implements Serializable {
    private int tabLayout;
    private String theme;

    public int getTabLayout() {
        return tabLayout;
    }

    public void setTabLayout(int tabLayout) {
        this.tabLayout = tabLayout;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }
}
