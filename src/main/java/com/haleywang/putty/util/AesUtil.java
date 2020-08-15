package com.haleywang.putty.util;


import com.haleywang.putty.common.AesException;
import com.haleywang.putty.common.Preconditions;
import org.unknown.AES;


/**
 * @author haley
 */
public class AesUtil {

    public static final int KEY_LENGTH = 16;

    private AesUtil() {
    }

    public static String encrypt(String content, String key) {
        Preconditions.checkArgument(content != null, "Content cannot be null");
        Preconditions.checkArgument(key != null, "Key cannot be null");
        try {
            return AES.encrypt(content, key);
        } catch (Exception e) {
            throw new AesException(e);
        }
    }


    public static String decrypt(String encryptedText, String key) {
        Preconditions.checkArgument(encryptedText != null, "EncryptedText cannot be null");
        Preconditions.checkArgument(key != null, "Key cannot be null");

        try {
            return AES.decrypt(encryptedText, key);
        } catch (Exception e) {
            throw new AesException(e);
        }
    }

    public static String generateKey() {
        return AES.generateKey();
    }

    public static String generateKey(String in) {
        if (in == null || in.length() <= 0) {
            throw new AesException("input should not be empty");
        }

        String input = in.toLowerCase();
        if (input.length() > KEY_LENGTH) {
            return input.substring(0, KEY_LENGTH);
        }
        while (input.length() < KEY_LENGTH) {
            input = appendA(input);
        }
        return input;

    }

    private static String appendA(String in) {
        return in + "a";
    }
}