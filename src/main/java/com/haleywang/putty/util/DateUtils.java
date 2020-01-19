package com.haleywang.putty.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * @author haley
 */
public class DateUtils {
    private DateUtils() {
    }


    public static LocalDateTime convertMillisecondsToLocalDateTime(long milliseconds) {

        return Instant.ofEpochMilli(milliseconds).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public static long convertLocalDateTimeToMilliseconds(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
