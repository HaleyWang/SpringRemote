package com.haleywang.putty.util;

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

    public static String read(InputStream in) {
        List<String> lines = new ArrayList<>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(in));
            String line = null;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        StringBuffer sb = new StringBuffer();
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

        OutputStreamWriter writer =
                null;
        try {
            writer = new OutputStreamWriter(
                    new FileOutputStream(
                            file), "utf-8");
            writer.write(text);
            //writer.flush();

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
