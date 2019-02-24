package com.haleywang.putty.storage;

import com.google.gson.Gson;
import com.haleywang.putty.common.Constants;
import com.haleywang.putty.util.IOTool;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

public enum  FileStorage {

    INSTANCE;


    private static final String PATH_LOGIN_PASSWORDS_JSON = Constants.PATH_ROOT + "/data/loginPasswordsJson.json";
    private static final String PATH_COMMANDS_JSON = Constants.PATH_ROOT + "/data/updateCommandsJson.json";
    private static final String PATH_CONNECTIONS_PASSWORDS_JSON = Constants.PATH_ROOT + "/data/connectionsPasswordsJson.json";
    private static final String PATH_CONNECTIONS_JSON = Constants.PATH_ROOT + "/data/updateConnectionsJson.json";
    private static final String PATH_ACCOUNT = Constants.PATH_ROOT + "/data/account.json";

    public String getLoginPasswords() {
        return readToString(new File(PATH_LOGIN_PASSWORDS_JSON));
    }

    public static void saveLoginPasswordsJson(HashMap hashMap) {
        IOTool.write(new Gson().toJson(hashMap), new File(PATH_LOGIN_PASSWORDS_JSON));
    }

    public String getCommandsData() {

        return readToString(new File(PATH_COMMANDS_JSON));
    }

    private String readToString(File file) {
        if(file == null || !file.exists()) {
            return null;
        }
        try {
            return IOTool.read(new FileInputStream(file));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void saveCommandsData(String text) {
        IOTool.write(text, new File(PATH_COMMANDS_JSON));
    }

    public void saveConnectionPassword(Map<String, Object> hashMap) {
        IOTool.write(new Gson().toJson(hashMap), new File(PATH_CONNECTIONS_PASSWORDS_JSON));
    }

    public String getConnectionsPasswords() {
        return readToString(new File(PATH_CONNECTIONS_PASSWORDS_JSON));
    }

    public void saveConnectionsInfoData(String text) {
        IOTool.write(text, new File(PATH_CONNECTIONS_JSON));
    }



    public String getConnectionsInfoData() {
        return readToString(new File(PATH_CONNECTIONS_JSON));
    }

    public void saveAccount(String text) {
        IOTool.write(text, new File(PATH_ACCOUNT));
    }



    public String getAccount() {
        return readToString(new File(PATH_ACCOUNT));
    }

}
