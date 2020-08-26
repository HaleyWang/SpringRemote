package com.haleywang.putty.util;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class PathUtilsTest {

    @Test
    public void isStartupFromJar() {
        Assert.assertFalse(PathUtils.isStartupFromJar(PathUtilsTest.class));
    }

    @Test
    public void getRoot() {
        String p = PathUtils.getRoot();
        Assert.assertTrue(p.endsWith("test"));
    }
}