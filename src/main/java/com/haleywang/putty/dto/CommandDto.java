package com.haleywang.putty.dto;

import com.haleywang.putty.service.action.ActionCategoryEnum;
import com.haleywang.putty.util.StringUtils;

import java.io.Serializable;

/**
 * @author haley
 */
public class CommandDto extends GroupDto<CommandDto> implements Action, Serializable {

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
        return getCommand();
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
