package com.haleywang.putty.dto;

import com.haleywang.putty.service.action.ActionCategoryEnum;
import com.haleywang.putty.util.StringUtils;

public class CommandDto extends GroupDto<CommandDto> implements Action {

    private String command;

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    @Override
    public String toString() {

        return StringUtils.ifBlank(getName(), getCommand());
    }

    @Override
    public String getAction() {
        return command;
    }

    @Override
    public String getKeyMap() {
        return null;
    }

    @Override
    public ActionCategoryEnum getCategory() {
        return ActionCategoryEnum.COMMAND;
    }
}
