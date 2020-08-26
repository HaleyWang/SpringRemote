package com.haleywang.putty.util;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class Md5UtilsTest {

    @Test
    public void getT4MD5() {
        Assert.assertEquals("f4cc399f0effd13c888e310ea2cf5399", Md5Utils.getT4Md5("123456"));
    }
}