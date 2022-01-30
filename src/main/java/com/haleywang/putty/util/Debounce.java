package com.haleywang.putty.util;


import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * If this method is called repeatedly within the delay time with the same key,
 * it will only be called the last time
 * @author haley
 * eg:
 *         Debounce.debounce("key1", () -> {System.out.println(111);}, 3, TimeUnit.SECONDS);
 *         Debounce.debounce("key1", () -> {System.out.println(222);}, 3, TimeUnit.SECONDS);
 *         Debounce.debounce("key1", () -> {System.out.println(333);}, 3, TimeUnit.SECONDS);
 */
public class Debounce {

    private Debounce(){}


    private static final ScheduledExecutorService SCHEDULE = new ScheduledThreadPoolExecutor(1,
            new ThreadFactoryBuilder().setNameFormat("debounce-schedule-pool-%d").setDaemon(true).build());
    private static final ConcurrentHashMap<Object, Future<?>> DELAYED_MAP = new ConcurrentHashMap<>();

    public static void debounce(final Object key, final Runnable runnable, long delay, TimeUnit unit) {

        final Future<?> prev = DELAYED_MAP.put(key, SCHEDULE.schedule(() -> {

            try {
                runnable.run();
            } finally {
                DELAYED_MAP.remove(key);
            }

        }, delay, unit));

        if (prev != null) {
            prev.cancel(true);
        }
    }

    public static void shutdown() {
        SCHEDULE.shutdownNow();
    }

}