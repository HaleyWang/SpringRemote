package com.haleywang.putty.dto;

import java.io.Serializable;

/**
 * @author haley
 */
public class SettingDto implements Serializable {
    public static final int LIMIT_FRAME_WIDTH = 300;
    public static final int LIMIT_FRAME_HEIGHT = 300;
    private static final long serialVersionUID = 6407626038956860473L;
    private int tabLayout;
    private String theme;
    private String localFolder;
    private String remoteFolder;
    private String remoteFile;
    private String localFile;
    private String currentCommandPath;
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
        if (frameWidth < LIMIT_FRAME_WIDTH) {
            return 880;
        }
        return frameWidth;
    }

    public void setFrameWidth(int frameWidth) {
        this.frameWidth = frameWidth;
    }

    public int getFrameHeight() {
        if (frameHeight < LIMIT_FRAME_HEIGHT) {
            return 700;
        }
        return frameHeight;
    }

    public String getCurrentCommandPath() {
        return currentCommandPath;
    }

    public void setCurrentCommandPath(String currentCommandPath) {
        this.currentCommandPath = currentCommandPath;
    }

    public void setFrameHeight(int frameHeight) {
        this.frameHeight = frameHeight;
    }
}
