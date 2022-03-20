package com.haleywang.putty.util;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * @author haley
 */
public class CollectionUtils {

    private CollectionUtils() {
    }

    public static <E> List<E> notNullList(List<E> list) {
        return Optional.ofNullable(list).orElseGet(ArrayList::new);
    }

    public static boolean isEmpty(List<?> hms) {
        return hms == null || hms.isEmpty();
    }

    public static int size(List<?> hList) {
        if (hList == null) {
            return -1;
        }
        return hList.size();
    }

    public static <T> T getItem(List<T> list, int index) {
        if (list == null || index < 0 || index >= list.size()) {
            return null;
        }
        return list.get(index);
    }

}
