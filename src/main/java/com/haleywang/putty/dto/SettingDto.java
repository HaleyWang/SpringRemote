package com.haleywang.putty.dto;

import java.io.Serializable;

/**
 * @author haley
 */
public class SettingDto implements Serializable {
    private int tabLayout;
    private String theme;
    private String localFolder;
    private String remoteFolder;
    private String remoteFile;
    private String localFile;
    private int frameWidth;
    private int frameHeight;

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

    public String getLocalFolder() {
        return localFolder;
    }

    public void setLocalFolder(String localFolder) {
        this.localFolder = localFolder;
    }

    public String getRemoteFolder() {
        return remoteFolder;
    }

    public void setRemoteFolder(String remoteFolder) {
        this.remoteFolder = remoteFolder;
    }

    public String getRemoteFile() {
        return remoteFile;
    }

    public void setRemoteFile(String remoteFile) {
        this.remoteFile = remoteFile;
    }

    public String getLocalFile() {
        return localFile;
    }

    public void setLocalFile(String localFile) {
        this.localFile = localFile;
    }

    public int getFrameWidth() {
        if (frameWidth < 300) {
            return 880;
        }
        return frameWidth;
    }

    public void setFrameWidth(int frameWidth) {
        this.frameWidth = frameWidth;
    }

    public int getFrameHeight() {
        if (frameHeight < 300) {
            return 700;
        }
        return frameHeight;
    }

    public void setFrameHeight(int frameHeight) {
        this.frameHeight = frameHeight;
    }
}
