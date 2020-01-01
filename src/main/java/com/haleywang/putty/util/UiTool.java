package com.haleywang.putty.util;

import java.awt.Color;

/**
 * @author haley
 */
public class UiTool {

    private UiTool() {
    }

    public static Color toColorFromString(String colorStr) {
        return new Color(Integer.parseInt(colorStr, 16));
    }

}
