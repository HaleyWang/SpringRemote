package com.haleywang.putty.util;

import com.google.gson.Gson;

public class JsonUtils {
    private JsonUtils(){}

    public static  <T> T fromJson(String json, Class<T> classOfT, T defaultVal) {
        try {
            return new Gson().fromJson(json, classOfT);
        }catch (Exception e) {
            return defaultVal;
        }

    }

    public static  <T> T fromJson(String json, Class<T> classOfT) {
            return fromJson(json, classOfT, null);
    }

    public static  String toJson(Object object) {
            return new Gson().toJson(object);

    }


}
