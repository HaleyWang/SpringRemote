package com.haleywang.putty.util;


import java.util.List;


/**
 * @author haley
 */
public class CollectionUtils {

    private CollectionUtils() {
    }

    public static <T> T getItem(List<T> list, int index) {
        if (list == null || index < 0 || index >= list.size()) {
            return null;
        }
        return list.get(index);
    }
}
