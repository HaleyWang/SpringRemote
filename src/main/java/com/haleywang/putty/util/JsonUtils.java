package com.haleywang.putty.util;

import com.google.gson.Gson;

public class JsonUtils {

    public static  <T> T fromJson(String json, Class<T> classOfT, T defaultVal) {
        try {
            return new Gson().fromJson(json, classOfT);
        }catch (Exception e) {
            return defaultVal;
        }

    }


}
