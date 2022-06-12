package com;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;

public final class ReflectUtil {
    public static final String PROXY_PACKAGE = "com.sun.proxy";

    private ReflectUtil() {
    }

    public static Class<?> forName(String var0) throws ClassNotFoundException {
        checkPackageAccess(var0);
        return Class.forName(var0);
    }

    public static Object newInstance(Class<?> var0) throws InstantiationException, IllegalAccessException {
        checkPackageAccess(var0);
        return var0.newInstance();
    }


    private static boolean isSubclassOf(Class<?> var0, Class<?> var1) {
        while(var0 != null) {
            if (var0 == var1) {
                return true;
            }

            var0 = var0.getSuperclass();
        }

        return false;
    }



    public static void checkPackageAccess(Class<?> var0) {
        checkPackageAccess(var0.getName());
        if (isNonPublicProxyClass(var0)) {
            checkProxyPackageAccess(var0);
        }

    }

    public static void checkPackageAccess(String var0) {
        SecurityManager var1 = System.getSecurityManager();
        if (var1 != null) {
            String var2 = var0.replace('/', '.');
            int var3;
            if (var2.startsWith("[")) {
                var3 = var2.lastIndexOf(91) + 2;
                if (var3 > 1 && var3 < var2.length()) {
                    var2 = var2.substring(var3);
                }
            }

            var3 = var2.lastIndexOf(46);
            if (var3 != -1) {
                var1.checkPackageAccess(var2.substring(0, var3));
            }
        }

    }

    public static boolean isPackageAccessible(Class<?> var0) {
        try {
            checkPackageAccess(var0);
            return true;
        } catch (SecurityException var2) {
            return false;
        }
    }

    private static boolean isAncestor(ClassLoader var0, ClassLoader var1) {
        ClassLoader var2 = var1;

        do {
            var2 = var2.getParent();
            if (var0 == var2) {
                return true;
            }
        } while(var2 != null);

        return false;
    }

    public static boolean needsPackageAccessCheck(ClassLoader var0, ClassLoader var1) {
        if (var0 != null && var0 != var1) {
            if (var1 == null) {
                return true;
            } else {
                return !isAncestor(var0, var1);
            }
        } else {
            return false;
        }
    }

    public static void checkProxyPackageAccess(Class<?> var0) {
        SecurityManager var1 = System.getSecurityManager();
        if (var1 != null && Proxy.isProxyClass(var0)) {
            Class[] var2 = var0.getInterfaces();
            int var3 = var2.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                Class var5 = var2[var4];
                checkPackageAccess(var5);
            }
        }

    }

    public static void checkProxyPackageAccess(ClassLoader var0, Class<?>... var1) {
        SecurityManager var2 = System.getSecurityManager();
        if (var2 != null) {
            Class[] var3 = var1;
            int var4 = var1.length;

            for(int var5 = 0; var5 < var4; ++var5) {
                Class var6 = var3[var5];
                ClassLoader var7 = var6.getClassLoader();
                if (needsPackageAccessCheck(var0, var7)) {
                    checkPackageAccess(var6);
                }
            }
        }

    }

    public static boolean isNonPublicProxyClass(Class<?> var0) {
        String var1 = var0.getName();
        int var2 = var1.lastIndexOf(46);
        String var3 = var2 != -1 ? var1.substring(0, var2) : "";
        return Proxy.isProxyClass(var0) && !var3.equals("com.sun.proxy");
    }

    public static void checkProxyMethod(Object var0, Method var1) {
        if (var0 != null && Proxy.isProxyClass(var0.getClass())) {
            if (Modifier.isStatic(var1.getModifiers())) {
                throw new IllegalArgumentException("Can't handle static method");
            } else {
                Class var2 = var1.getDeclaringClass();
                if (var2 == Object.class) {
                    String var3 = var1.getName();
                    if (var3.equals("hashCode") || var3.equals("equals") || var3.equals("toString")) {
                        return;
                    }
                }

                if (!isSuperInterface(var0.getClass(), var2)) {
                    throw new IllegalArgumentException("Can't handle: " + var1);
                }
            }
        } else {
            throw new IllegalArgumentException("Not a Proxy instance");
        }
    }

    private static boolean isSuperInterface(Class<?> var0, Class<?> var1) {
        Class[] var2 = var0.getInterfaces();
        int var3 = var2.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            Class var5 = var2[var4];
            if (var5 == var1) {
                return true;
            }

            if (isSuperInterface(var5, var1)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isVMAnonymousClass(Class<?> var0) {
        return var0.getName().indexOf("/") > -1;
    }
}
