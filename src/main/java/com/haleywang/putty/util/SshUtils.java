package com.haleywang.putty.util;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.IOException;
import java.io.InputStream;

public class SshUtils {
    private SshUtils() {
    }

    public static String sendCommand(Session sesConnection, String command) {
        if (sesConnection == null) {
            return null;
        }
        StringBuilder outputBuffer = new StringBuilder();

        try {
            ChannelExec channelExec = (ChannelExec) sesConnection.openChannel("exec");
            channelExec.setCommand(command);
            channelExec.setCommand(command);
            InputStream commandOutput = channelExec.getInputStream();
            channelExec.connect();

            int readByte = commandOutput.read();

            while (readByte != 0xffffffff) {
                outputBuffer.append((char) readByte);
                readByte = commandOutput.read();
            }


            channelExec.disconnect();
        } catch (IOException ioX) {
            //logWarning(ioX.getMessage())
            return null;
        } catch (JSchException jschX) {
            //logWarning(jschX.getMessage())
            return null;
        }

        return outputBuffer.toString();
    }
}
