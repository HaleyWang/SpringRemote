package com.haleywang.putty.util;

public class StringUtils {

    public static String trim(String cs)
    {
        if(cs == null){
            return null;
        }
        return cs.trim();

    }
    public static boolean isBlank(CharSequence cs)
    {
        int strLen =0;

        if ((cs == null) || ((strLen = cs.length()) == 0))
            return true;

        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static String ifBlank(String name, String host) {
        return isBlank(name) ? host : name;
    }
}
