package com.haleywang.putty.storage;

import com.google.gson.Gson;
import com.haleywang.putty.common.Constants;
import com.haleywang.putty.dto.CommandDto;
import com.haleywang.putty.dto.SettingDto;
import com.haleywang.putty.dto.TmpCommandsDto;
import com.haleywang.putty.service.NotificationsService;
import com.haleywang.putty.util.CollectionUtils;
import com.haleywang.putty.util.IoTool;
import com.haleywang.putty.util.JsonUtils;
import com.haleywang.putty.util.StringUtils;
import com.haleywang.putty.view.SpringRemoteView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author haley
 */
public enum FileStorage {

    /**
     * INSTANCE
     */
    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger(FileStorage.class);

    public static final String DATA_FOLDER = "/spring_remote_data";
    public static final String DATA_FOLDER_PATH = Constants.PATH_ROOT + DATA_FOLDER;

    private static final String PATH_LOGIN_PASSWORDS_JSON = Constants.PATH_ROOT + DATA_FOLDER + "/setting/loginPasswordsJson.json";
    public static final String PATH_COMMANDS_JSON = Constants.PATH_ROOT + DATA_FOLDER + "/commandsJsonData.json";
    public static final String PATH_TMP_COMMANDS_JSON = Constants.PATH_ROOT + DATA_FOLDER + "/tmpCommandsJsonData.json";
    private static final String PATH_CONNECTIONS_PASSWORDS_JSON = Constants.PATH_ROOT + DATA_FOLDER + "/setting/connectionsPasswordsJson.json";
    public static final String PATH_CONNECTIONS_JSON = Constants.PATH_ROOT + DATA_FOLDER + "/connectionsJsonData.json";
    private static final String PATH_ACCOUNT = Constants.PATH_ROOT + DATA_FOLDER + "/setting/currentAccount.json";
    private static final String PATH_ACCOUNT_SETTING = Constants.PATH_ROOT + DATA_FOLDER + "/setting/setting_{key}.json";
    private static final String PATH_COMMON_SETTING = Constants.PATH_ROOT + DATA_FOLDER + "/setting/settings.json";

    public static final Pattern NAME_PATTERN = Pattern.compile(".*[ ]+\\(([0-9]+)\\)");


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
            return IoTool.read(file);
        } catch (Exception e) {
            LOGGER.error("readToString error", e);
        }
        return null;
    }

    public static String getNewName(String currentName, Set<String> names) {
        if (names.contains(currentName)) {

            String currentShortName = currentName;
            if (currentName.endsWith(")")) {
                Matcher m = NAME_PATTERN.matcher(currentName);
                if (m.find()) {
                    currentShortName = currentName.substring(0, currentName.lastIndexOf("(")).trim();
                }
            }

            for (int i = 0; ; i++) {
                String newName = currentShortName + " (" + i + ")";
                if (!names.contains(newName)) {
                    currentName = newName;
                    break;
                }
            }
        }
        return currentName;
    }

    public void saveCommandsData(CommandDto dto, String currentPath) {

        renameChildIfDuplicate(dto);

        String commandsJson = JsonUtils.toJson(dto);
        saveCommandsStr(commandsJson, currentPath);
    }

    private void renameChildIfDuplicate(CommandDto dto) {
        List<CommandDto> commandDtoList = CollectionUtils.notNullList(dto.getChildren());
        if (commandDtoList.isEmpty()) {
            return;
        }
        List<String> nameList = new ArrayList<>();
        commandDtoList.forEach(o -> nameList.add(o.getName()));
        Set<String> nameSet = new HashSet<>(nameList);

        boolean isDuplicate = nameList.size() != nameSet.size();
        if (isDuplicate) {
            Set<Integer> indexList = getDuplicateIndex(nameList);
            for (Integer index : indexList) {
                CommandDto commandDto = commandDtoList.get(index);
                String name = commandDto.getName();
                String newName = getNewName(name, nameSet);
                if (!StringUtils.isEq(name, newName)) {
                    nameSet.add(newName);
                    commandDto.setName(newName);
                }
            }
        }

        for (CommandDto commandDto : commandDtoList) {
            //Recursive call
            renameChildIfDuplicate(commandDto);
        }
    }

    //getDuplicateIndex
    public Set<Integer> getDuplicateIndex(List<String> nameList) {
        Set<Integer> duplicates = new HashSet<>();
        int len = nameList.size();
        for (int i = 0; i < len; i++) {
            if (duplicates.contains(i)) {
                continue;
            }
            for (int i1 = 0; i1 < len; i1++) {
                if (duplicates.contains(i1)) {
                    continue;
                }
                if (i != i1 && StringUtils.isEq(nameList.get(i), nameList.get(i1))) {
                    duplicates.add(i1);
                }
            }
        }
        return duplicates;
    }

    public void saveCommandsData(String text, String currentPath) {
        CommandDto dto = JsonUtils.fromJson(text, CommandDto.class);
        saveCommandsData(dto, currentPath);
    }

    private void saveCommandsStr(String commandsJson, String currentCommandPath) {
        if (JsonUtils.validate(commandsJson)) {
            IoTool.write(JsonUtils.getFormatJsonString(commandsJson), new File(PATH_COMMANDS_JSON));
            NotificationsService.getInstance().info("Auto save commands json.");
        } else {
            NotificationsService.getInstance().warn("Invalid commands json syntax.");
        }

        SettingDto accountSetting = FileStorage.INSTANCE.getSettingDto(SpringRemoteView.getInstance().getUserName());
        accountSetting.setCurrentCommandPath(currentCommandPath);
        FileStorage.INSTANCE.saveSettingDto(SpringRemoteView.getInstance().getUserName(), accountSetting);
    }

    public void saveTmpCommandsData(TmpCommandsDto tmpCommandsDto) {
        int maxSize = 100;
        if(tmpCommandsDto.getCommands().size() > maxSize) {
            tmpCommandsDto.getCommands().remove(0);
        }
        String text = JsonUtils.toJson(tmpCommandsDto);
        if (JsonUtils.validate(text)) {
            IoTool.write(text, new File(PATH_TMP_COMMANDS_JSON));
            NotificationsService.getInstance().info("Auto save tmp commands.");
        } else {
            NotificationsService.getInstance().warn("Invalid commands json syntax.");
        }
    }

    public TmpCommandsDto getTmpCommandsJson() {
        String tmpCommandsDataText = getTmpCommandsData();

        TmpCommandsDto tmpCommandsDto;
        if (tmpCommandsDataText != null) {
            tmpCommandsDto = JsonUtils.fromJson(tmpCommandsDataText, TmpCommandsDto.class, new TmpCommandsDto());
        }else{
            tmpCommandsDto = new TmpCommandsDto();
        }
        if(tmpCommandsDto.getCommands() == null) {
            tmpCommandsDto.setCommands(new ArrayList<>());
        }
        return tmpCommandsDto;
    }

    public String getTmpCommandsData() {
        return readToString(new File(PATH_TMP_COMMANDS_JSON));
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
