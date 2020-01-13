package com.haleywang.putty.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class DateUtils {
    private DateUtils() {
    }


    public static LocalDateTime convertMillisecondsToLocalDateTime(long milliseconds) {

        return Instant.ofEpochMilli(milliseconds).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public static long convertLocalDateTimeToMilliseconds(LocalDateTime localDT) {
        return localDT.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
