package com.haleywang.putty.dto;

import org.slf4j.event.Level;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author haley
 */
public class EventDto implements Serializable {
    private LocalDateTime time;
    private String message;
    private Level level;

    public static EventDto log(String message, Level level) {
        return new EventDto(LocalDateTime.now(), message, level);
    }

    public static EventDto info(String message) {
        return new EventDto(LocalDateTime.now(), message, Level.INFO);
    }

    public static EventDto warn(String message) {
        return new EventDto(LocalDateTime.now(), message, Level.WARN);
    }

    public static EventDto error(String message) {
        return new EventDto(LocalDateTime.now(), message, Level.ERROR);
    }


    public EventDto() {

    }

    public EventDto(LocalDateTime time, String message, Level level) {
        this.time = time;
        this.message = message;
        this.level = level;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }
}