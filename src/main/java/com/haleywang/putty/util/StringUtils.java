package com.haleywang.putty.util;


/**
 * @author haley
 */
public class StringUtils {
    public static final String EMPTY = "";

    private StringUtils() {
    }

    public static String trim(String cs) {
        if (cs == null) {
            return null;
        }
        return cs.trim();

    }

    public static boolean isBlank(CharSequence cs) {
        int strLen = 0;

        if ((cs == null) || ((strLen = cs.length()) == 0)) {
            return true;
        }

        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static String ifBlank(String name, String defaultValue) {
        return isBlank(name) ? defaultValue : name;
    }

    public static boolean isAnyBlank(String name, String host) {
        return isBlank(name) || isBlank(host);

    }

    public static boolean isEq(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }
}
