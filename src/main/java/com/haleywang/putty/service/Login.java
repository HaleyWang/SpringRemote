package com.haleywang.putty.service;

import com.google.gson.Gson;
import com.haleywang.putty.util.Md5Utils;
import com.haleywang.putty.dto.Status;
import com.haleywang.putty.storage.FileStorage;
import com.haleywang.putty.util.StringUtils;

import java.util.HashMap;

public class Login {

    public static Status register(String username, String password) {
        if (StringUtils.isBlank(username)){
            return Status.fail("Enter account name.");

        }
        if (StringUtils.isBlank(password)){
            return Status.fail("Enter a combination of at least 4 numbers, letters.");
        }

        if (!password.matches("[0-9A-Za-z]{4,30}")){
            return Status.fail("Enter a combination of 4 ~ 30 numbers, letters.");
        }

        if(getLoginPasswordsMap().containsKey(username.toLowerCase())) {
            return Status.fail("The account name already exists.");
        }
        String name = username.trim();

        saveLoginPassword(name, password);
        return Status.ok();
    }

    public static Status authenticate(String name, String password) {
        // hardcoded username and password
        if (StringUtils.isBlank(name)){
            return Status.fail("Enter account name.");

        }
        if (StringUtils.isBlank(password)){
            return Status.fail("Enter a combination of at least 4 numbers, letters.");
        }

        String username = name.trim();

        String pass = (String) getLoginPasswordsMap().get(username);
        if(pass == null) {
            return Status.fail("Invalid username or password");
        }

        String passT4Md5 = (String) getLoginPasswordsMap().getOrDefault(username, "");

        if (passT4Md5.equals(Md5Utils.getT4MD5(password))) {
            return Status.ok();
        }
        return Status.fail("Invalid username or password.");
    }

    private static void saveLoginPassword(String user, String password) {
        if(password == null || password.length() == 0) {
            return;
        }
        String pass = password;
        try {
            pass = Md5Utils.getT4MD5(pass);
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        HashMap hashMap = getLoginPasswordsMap();
        hashMap.put(user.toLowerCase(), pass);
        FileStorage.INSTANCE.saveLoginPasswordsJson(hashMap);
    }

    private static HashMap getLoginPasswordsMap() {
        String str = FileStorage.INSTANCE.getLoginPasswords();
        if(str == null) {
            return new HashMap();
        }

        HashMap map = new Gson().fromJson(str, HashMap.class);
        if(map == null) {
            return new HashMap();
        }

        return map;
    }

}
