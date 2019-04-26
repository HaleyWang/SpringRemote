package com.haleywang.putty.util;

import com.haleywang.putty.common.SpringRemoteException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class IOTool {

    private IOTool(){}

    public static String read(InputStream in) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))){
            String line = null;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            throw new SpringRemoteException(e);
        }

        StringBuilder sb = new StringBuilder();
        for (String str : lines) {
            sb.append(str).append("\r\n");
        }
        return sb.toString();
    }

    public static boolean ensureDirectoryExists(final File f) {
        return f.exists() || f.mkdir();
    }

    public static void write(String text, File file) {

        ensureDirectoryExists(file.getParentFile());

        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(
                file), "utf-8")){
            writer.write(text);

        } catch (IOException  e) {
            throw new SpringRemoteException(e);
        }
    }
}
