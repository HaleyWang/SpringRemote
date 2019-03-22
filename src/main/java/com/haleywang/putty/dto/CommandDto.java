package com.haleywang.putty.dto;

import com.haleywang.putty.util.StringUtils;

public class CommandDto extends GroupDto<CommandDto> {

    private String command;

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String toString() {

        return StringUtils.ifBlank(getName(), getCommand());
    }
}
