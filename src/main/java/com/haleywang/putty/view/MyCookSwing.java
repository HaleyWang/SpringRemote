package com.haleywang.putty.view;

import com.haleywang.putty.util.FieldUtils;
import cookxml.cookswing.CookSwing;
import cookxml.core.IdReference;

import javax.annotation.Resource;
import java.awt.Container;
import java.lang.reflect.Field;

/**
 * @author haley
 */
public class MyCookSwing extends CookSwing {

    private final Container container;

    public MyCookSwing(Container parent, String xml) {
        super(parent);
        container = render(xml);
    }

    public Container getContainer() {
        return container;
    }

    public MyCookSwing fillFieldsValue(Object me) {
        for (Field field : FieldUtils.findSelfFields(me.getClass(), Resource.class)) {

            IdReference idRef = this.getId(field.getName());

            if (idRef == null || !field.getType().isInstance(idRef.object)) {
                continue;
            }
            try {
                field.setAccessible(true);
                field.set(me, idRef.object);
            } catch (IllegalAccessException e) {
                throw new com.haleywang.putty.common.IllegalAccessException(e);
            }
        }
        return this;
    }

}
