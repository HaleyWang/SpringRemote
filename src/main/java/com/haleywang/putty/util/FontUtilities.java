package com.haleywang.putty.util;
import sun.swing.SwingLazyValue;

import java.awt.Font;
import java.util.HashMap;
import java.util.Map;
import javax.swing.UIManager;

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
            for (Map.Entry entry : UIManager.getDefaults().entrySet()) {
                Object key = entry.getKey();
                if (key.toString().toLowerCase().contains(".font")) {
                    Object value = entry.getValue();
                    Font font = null;
                    if (value instanceof SwingLazyValue) {
                        SwingLazyValue lazy = (SwingLazyValue) entry.getValue();
                        value = lazy.createValue(UIManager.getDefaults());
                    }

                    if (value instanceof Font) {
                        font = (Font) value;
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