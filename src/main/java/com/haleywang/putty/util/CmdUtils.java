package com.haleywang.putty.util;

import com.haleywang.putty.dto.CommandDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


/**
 * @author haley
 */
public class CmdUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(CmdUtils.class);

    private CmdUtils() {
    }

    public static void run(CommandDto commandDto) {
        if (commandDto == null || commandDto.getCommand() == null) {
            return;
        }
        try {
            Runtime rt = Runtime.getRuntime();
            String command = commandDto.getCommand().split("cmd>|term>")[1];
            rt.exec("cmd.exe /c cd . & start cmd.exe /k \" " + command + " \"");
        } catch (IOException e1) {

            String command = commandDto.getCommand().split("cmd>|term>")[1];

            String[] args = new String[]{"/bin/bash", "-c", command, "with", "args"};
            try {
                new ProcessBuilder(args).start();
            } catch (IOException e) {
                LOGGER.error("CmdUtils run error", e);
            }
        }
    }
}
