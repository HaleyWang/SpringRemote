package com.haleywang.putty.util;

import org.junit.Assert;
import org.junit.Test;

public class AesUtilTest {

    @Test
    public void encrypt() throws Exception {

        String key = AesUtil.generateKey("bbb");
        String text = "aa";
        String encryptText = AesUtil.encrypt(text, key);

        Assert.assertEquals(text, AesUtil.decrypt(encryptText, key));

    }


    @Test
    public void generateKey() {
        int expectedLength = 16;
        String expectedText = "bbbb1234aaaaaaaa";
        String key = AesUtil.generateKey("bbbb1234");

        Assert.assertEquals(expectedLength, key.length());
        Assert.assertEquals(expectedText, key);

        key = AesUtil.generateKey(expectedText + "bbbbb");
        Assert.assertEquals(expectedText, key);

        key = AesUtil.generateKey();
        Assert.assertEquals(expectedLength, key.length());
    }


}