package com.haleywang.putty.storage;

import com.google.gson.Gson;
import com.haleywang.putty.common.Constants;
import com.haleywang.putty.dto.SettingDto;
import com.haleywang.putty.service.NotificationsService;
import com.haleywang.putty.util.IoTool;
import com.haleywang.putty.util.JsonUtils;
import com.haleywang.putty.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

/**
 * @author haley
 */
public enum FileStorage {

    /**
     * INSTANCE
     */
    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger(FileStorage.class);

    private static final String DATA_FOLDER = "/spring_remote_data";

    private static final String PATH_LOGIN_PASSWORDS_JSON = Constants.PATH_ROOT + DATA_FOLDER + "/setting/loginPasswordsJson.json";
    private static final String PATH_COMMANDS_JSON = Constants.PATH_ROOT + DATA_FOLDER + "/commandsJsonData.json";
    private static final String PATH_CONNECTIONS_PASSWORDS_JSON = Constants.PATH_ROOT + DATA_FOLDER + "/setting/connectionsPasswordsJson.json";
    private static final String PATH_CONNECTIONS_JSON = Constants.PATH_ROOT + DATA_FOLDER + "/connectionsJsonData.json";
    private static final String PATH_ACCOUNT = Constants.PATH_ROOT + DATA_FOLDER + "/setting/currentAccount.json";
    private static final String PATH_ACCOUNT_SETTING = Constants.PATH_ROOT + DATA_FOLDER + "/setting/setting_{key}.json";
    private static final String PATH_COMMON_SETTING = Constants.PATH_ROOT + DATA_FOLDER + "/setting/settings.json";

    static {
        LOGGER.info("DATA_FOLDER ==> {}", Constants.PATH_ROOT + DATA_FOLDER);
    }

    public SettingDto getSettingDto(String accountName) {
        if (StringUtils.isBlank(accountName)) {
            return new SettingDto();
        }
        String settingString = readToString(getSettingFile(accountName));
        SettingDto settingDto = JsonUtils.fromJson(settingString, SettingDto.class);

        return settingDto != null ? settingDto : new SettingDto();
    }

    public void saveSettingDto(String accountName, SettingDto settingDtoIn) {
        if (StringUtils.isBlank(accountName)) {
            return;
        }
        SettingDto settingDto = settingDtoIn;
        if (settingDtoIn == null) {
            settingDto = new SettingDto();
        }
        File file = getSettingFile(accountName);
        IoTool.write(new Gson().toJson(settingDto), file);
    }

    private File getSettingFile(String accountName) {
        String settingKey = accountName.replace(" ", "_");
        return new File(PATH_ACCOUNT_SETTING.replace("{key}", settingKey));
    }


    public String getLoginPasswords() {
        return readToString(new File(PATH_LOGIN_PASSWORDS_JSON));
    }

    public void saveLoginPasswordsJson(Map<String, String> hashMap) {
        IoTool.write(new Gson().toJson(hashMap), new File(PATH_LOGIN_PASSWORDS_JSON));
    }

    public String getCommandsData() {

        return readToString(new File(PATH_COMMANDS_JSON));
    }

    private String readToString(File file) {
        if (file == null || !file.exists()) {
            return null;
        }
        try {
            return IoTool.read(new FileInputStream(file));
        } catch (Exception e) {
            LOGGER.error("readToString error", e);
        }
        return null;
    }

    public void saveCommandsData(String text) {
        if (JsonUtils.validate(text)) {
            IoTool.write(text, new File(PATH_COMMANDS_JSON));
            NotificationsService.getInstance().info("Auto save commands json.");
        } else {
            NotificationsService.getInstance().warn("Invalid commands json syntax.");
        }
    }

    public void saveConnectionPassword(Map<String, Object> hashMap) {
        IoTool.write(new Gson().toJson(hashMap), new File(PATH_CONNECTIONS_PASSWORDS_JSON));
    }

    public String getConnectionsPasswords() {
        return readToString(new File(PATH_CONNECTIONS_PASSWORDS_JSON));
    }

    public void saveConnectionsInfoData(String text) {
        if (JsonUtils.validate(text)) {
            IoTool.write(text, new File(PATH_CONNECTIONS_JSON));
            NotificationsService.getInstance().info("Auto save connections info json.");
        } else {
            NotificationsService.getInstance().warn("Invalid connections json syntax.");
        }
    }


    public String getConnectionsInfoData() {
        return readToString(new File(PATH_CONNECTIONS_JSON));
    }

    public void saveAccount(String text) {
        IoTool.write(text, new File(PATH_ACCOUNT));
    }


    public String getAccount() {
        return readToString(new File(PATH_ACCOUNT));
    }

    public SettingDto getSetting() {
        String settingString = readToString(new File(PATH_COMMON_SETTING));
        SettingDto settingDto = JsonUtils.fromJson(settingString, SettingDto.class);

        return settingDto != null ? settingDto : new SettingDto();
    }


    public void saveSetting(SettingDto setting) {
        SettingDto settingDto = setting;
        if (setting == null) {
            settingDto = new SettingDto();
        }
        File file = new File(PATH_COMMON_SETTING);
        IoTool.write(new Gson().toJson(settingDto), file);
    }


    public String getTheme() {
        SettingDto settingDto = getSetting();

        return StringUtils.ifBlank(settingDto.getTheme(), "FlatIntelliJLaf");
    }

    public void saveTheme(String themeClassName) {
        SettingDto settingDto = getSetting();
        settingDto.setTheme(themeClassName);
        saveSetting(settingDto);
    }
}
