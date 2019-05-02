package com.haleywang.putty.util;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class AESUtilTest {

    @Test
    public void encrypt() throws Exception {

        String key = AESUtil.generateKey("bbb");
        String text = "aa";
        String encryptText = AESUtil.encrypt(text, key);

        Assert.assertEquals(text, AESUtil.decrypt(encryptText, key));

    }


    @Test
    public void generateKey() {
        int expectedLength = 16;
        String expectedText = "bbbb1234aaaaaaaa";
        String key = AESUtil.generateKey("bbbb1234");

        Assert.assertEquals(expectedLength, key.length());
        Assert.assertEquals(expectedText, key);

        key = AESUtil.generateKey(expectedText + "bbbbb");
        Assert.assertEquals(expectedText, key);

        key = AESUtil.generateKey();
        Assert.assertEquals(expectedLength, key.length());
    }


}