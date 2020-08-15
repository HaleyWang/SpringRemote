package com.haleywang.putty.util;

import com.jediterm.terminal.ui.UIUtil;

import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import java.awt.Font;
import java.util.Enumeration;

/**
 * @author haley
 * @date 2020/2/2
 */
public class FontUtils {
    private FontUtils() {
    }


    /**
     * setUiFont
     * <p>
     * Call by:
     * setUIFont (new FontUIResource("Serif",Font.ITALIC,12));
     *
     * @param uiFont
     */
    public static void setUiFont(FontUIResource uiFont) {
        Enumeration keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, uiFont);
            }
        }
    }

    public static void setWindowsDefaultUiFont() {
        if (UIUtil.isWindows) {
            setUiFont(new FontUIResource("宋体", Font.PLAIN, 12));
        }
    }

}
