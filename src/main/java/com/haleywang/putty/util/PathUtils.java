package com.haleywang.putty.util;

import java.io.File;

public class PathUtils {

    public static<T> boolean isStartupFromJar(Class<T> clazz) {
        File file = new File(clazz.getProtectionDomain().getCodeSource().getLocation().getPath());
        return file.isFile();

    }

    public static String getRoot() {
        String res = "";
        if(isStartupFromJar(PathUtils.class)) {
            res =  new File("").getAbsolutePath();

        }else {
            res = Thread.currentThread().getContextClassLoader().getResource("").getPath();
        }
        res = res.replace("/target/test-classes", "");
        res = res.replace("/target/classes", "");
        if(res.endsWith("/")) {
            res = res.substring(0, res.length()-1);
        }
        return res;
    }
}