package com.haleywang.putty.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class DebouncerTest {

    @Test
    public void debounce() throws InterruptedException {

        final List<String> list = new ArrayList<>();
        final Debouncer debouncer = new Debouncer(TimeUnit.MILLISECONDS, 500);
        debouncer.debounce(Void.class, () -> list.add("1"));
        debouncer.debounce(Void.class, new Runnable() {
            @Override public void run() {
                list.add("1");
            }
        });
        debouncer.debounce(Void.class, new Runnable() {
            @Override public void run() {
                list.add("1");
            }
        });
        while (list.isEmpty()) {
            TimeUnit.MILLISECONDS.sleep(200);
        }
        Assert.assertEquals(1, list.size());

    }
}