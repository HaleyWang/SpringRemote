package com;


import javax.swing.UIDefaults;
import javax.swing.UIDefaults.LazyValue;
import javax.swing.plaf.ColorUIResource;
import java.awt.Color;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class SwingLazyValue implements LazyValue {
    private final String className;
    private final String methodName;
    private Object[] args;

    public SwingLazyValue(String var1) {
        this(var1, (String)null);
    }

    public SwingLazyValue(String var1, String var2) {
        this(var1, var2, (Object[])null);
    }

    public SwingLazyValue(String var1, Object[] var2) {
        this(var1, (String)null, var2);
    }

    public SwingLazyValue(String var1, String var2, Object[] var3) {
        this.className = var1;
        this.methodName = var2;
        if (var3 != null) {
            this.args = (Object[])var3.clone();
        }

    }

    @Override
    public Object createValue(UIDefaults var1) {
        try {
            ReflectUtil.checkPackageAccess(this.className);
            Class var2 = Class.forName(this.className, true, (ClassLoader)null);
            Class[] var3;
            if (this.methodName != null) {
                var3 = this.getClassArray(this.args);
                Method var6 = var2.getMethod(this.methodName, var3);
                this.makeAccessible(var6);
                return var6.invoke(var2, this.args);
            } else {
                var3 = this.getClassArray(this.args);
                Constructor var4 = var2.getConstructor(var3);
                this.makeAccessible(var4);
                return var4.newInstance(this.args);
            }
        } catch (Exception var5) {
            return null;
        }
    }

    private void makeAccessible(final AccessibleObject var1) {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                var1.setAccessible(true);
                return null;
            }
        });
    }

    private Class[] getClassArray(Object[] var1) {
        Class[] var2 = null;
        if (var1 != null) {
            var2 = new Class[var1.length];

            for(int var3 = 0; var3 < var1.length; ++var3) {
                if (var1[var3] instanceof Integer) {
                    var2[var3] = Integer.TYPE;
                } else if (var1[var3] instanceof Boolean) {
                    var2[var3] = Boolean.TYPE;
                } else if (var1[var3] instanceof ColorUIResource) {
                    var2[var3] = Color.class;
                } else {
                    var2[var3] = var1[var3].getClass();
                }
            }
        }

        return var2;
    }
}
