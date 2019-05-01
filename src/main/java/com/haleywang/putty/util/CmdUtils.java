package com.haleywang.putty.util;

import com.haleywang.putty.dto.CommandDto;

import java.io.IOException;

public class CmdUtils {
    private CmdUtils(){}

    public static void run(CommandDto commandDto) {
        if(commandDto == null || commandDto.getCommand() == null) {
            return;
        }
        try {
            Runtime rt = Runtime.getRuntime();
            String command = commandDto.getCommand().split("cmd>")[1];
            rt.exec("cmd.exe /c cd . & start cmd.exe /k \" " +command+ " \"");
        } catch (IOException e1) {
            e1.printStackTrace();

            String command = commandDto.getCommand().split("cmd>")[1];

            String[] args = new String[] {"/bin/bash", "-c", command, "with", "args"};
            try {
                new ProcessBuilder(args).start();
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
    }
}
