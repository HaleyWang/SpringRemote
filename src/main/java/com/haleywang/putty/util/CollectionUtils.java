package com.haleywang.putty.util;

import com.sun.istack.internal.Nullable;

import java.util.List;

public class CollectionUtils {

    private CollectionUtils(){}

    @Nullable
    public static  <T> T getItem(List<T> list, int index) {
        if(list == null || index < 0 || index >= list.size()) {
            return null;
        }
        return list.get(index);
    }
}
