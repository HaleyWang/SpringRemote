package com.haleywang.putty.dto;

public class CommandDto extends GroupDto<CommandDto> {

    private String command;

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }
}
