package com.haleywang.putty.util;

import sun.swing.SwingLazyValue;

import javax.swing.UIManager;
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;

/**
 * @author haley
 * @date 2020/2/2
 */
public class FontUtilities {

    private FontUtilities(){}

    private static Map<String, Font> originals;

    public static void setFontScale(float scale) {

        if (originals == null) {
            originals = new HashMap<>(25);
            for (Map.Entry<Object, Object> entry : UIManager.getDefaults().entrySet()) {
                Object key = entry.getKey();
                if (key.toString().toLowerCase().contains(".font")) {
                    Object value = entry.getValue();
                    if (value instanceof SwingLazyValue) {
                        SwingLazyValue lazy = (SwingLazyValue) entry.getValue();
                        value = lazy.createValue(UIManager.getDefaults());
                    }

                    if (value instanceof Font) {
                        Font font = (Font) value;
                        originals.put(key.toString(), font);
                    }
                }
            }
        }

        for (Map.Entry<String, Font> entry : originals.entrySet()) {
            String key = entry.getKey();
            Font font = entry.getValue();

            float size = font.getSize();
            size *= scale;

            font = font.deriveFont(Font.PLAIN, size);
            UIManager.put(key, font);
        }
    }

}