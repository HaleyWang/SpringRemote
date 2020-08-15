package com.haleywang.putty.util;

import com.haleywang.putty.common.SpringRemoteException;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 * @author haley
 */
public class Md5Utils {

    private Md5Utils() {
    }

    private static String getMd5(String str) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(str.getBytes(StandardCharsets.UTF_8));
        return new BigInteger(1, md.digest()).toString(16);
    }


    public static String getT4Md5(String str) {
        try {
            return getMd5(getMd5(getMd5(getMd5(str))));
        } catch (Exception e) {
            throw new SpringRemoteException("encrypt error", e);
        }

    }

}
