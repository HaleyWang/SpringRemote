package com.haleywang.putty.dto;

import java.io.Serializable;
import java.util.List;

/**
 * @author haley
 */
public class TmpCommandsDto implements Serializable {

    private static final long serialVersionUID = 4029082883893430824L;
    private List<String> commands;

    public List<String> getCommands() {
        return commands;
    }

    public void setCommands(List<String> commands) {
        this.commands = commands;
    }
}
