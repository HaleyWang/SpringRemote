package com.haleywang.putty.util;


import cookxml.cookswing.CookSwing;
import cookxml.core.IdReference;

import javax.annotation.Resource;
import java.lang.reflect.Field;

/**
 * @author haley
 */
public class CookSwingUtils {

    private CookSwingUtils() {
    }

    public static void fillFieldsValue(Object me, CookSwing cookSwing) {
        for (Field field : FieldUtils.findSelfFields(me.getClass(), Resource.class)) {

            IdReference idRef = cookSwing.getId(field.getName());

            if (!field.getType().isInstance(idRef.object)) {
                continue;
            }
            try {
                field.set(me, idRef.object);
            } catch (IllegalAccessException e) {
                throw new com.haleywang.putty.common.IllegalAccessException(e);
            }
        }
    }


}
