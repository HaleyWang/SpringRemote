package com.haleywang.putty.util;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class StringUtilsTest {

    @Test
    public void trim() {
        Assert.assertNull(StringUtils.trim(null));
        Assert.assertEquals("", StringUtils.trim(" "));
        Assert.assertEquals("a b", StringUtils.trim(" a b "));
    }

    @Test
    public void isBlank() {
        Assert.assertTrue(StringUtils.isBlank(null));
        Assert.assertTrue(StringUtils.isBlank(""));
        Assert.assertTrue(StringUtils.isBlank(" "));
        Assert.assertFalse(StringUtils.isBlank(" 1 "));
    }

    @Test
    public void ifBlank() {
        String defaultValue = "0";
        Assert.assertEquals(defaultValue, StringUtils.ifBlank(null, defaultValue));
        Assert.assertEquals(defaultValue, StringUtils.ifBlank("", defaultValue));
        Assert.assertEquals(defaultValue, StringUtils.ifBlank(" ", defaultValue));
        Assert.assertEquals("1", StringUtils.ifBlank("1", defaultValue));
    }
}