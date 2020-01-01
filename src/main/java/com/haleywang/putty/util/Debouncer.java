package com.haleywang.putty.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * @author haley
 */
public class Debouncer {
    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(1,
            new ThreadFactoryBuilder().setNameFormat("debouncer-thread-%d").build());
    private final ConcurrentHashMap<Object, Future<?>> delayedMap = new ConcurrentHashMap<>();

    private TimeUnit unit;
    private long delay;

    public Debouncer() {
    }

    public Debouncer(TimeUnit unit, long delay) {
        this.unit = unit;
        this.delay = delay;
    }

    public void debounce(final Object key, final Runnable runnable) {
        debounce(key, runnable, delay, unit);
    }


    public void debounce(final Object key, final Runnable runnable, long delay, TimeUnit unit) {
        final Future<?> prev = delayedMap.put(key, scheduler.schedule(() -> {
            try {
                runnable.run();
            } finally {
                delayedMap.remove(key);
            }
        }, delay, unit));
        if (prev != null) {
            prev.cancel(true);
        }
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}