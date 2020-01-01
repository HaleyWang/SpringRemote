package com.haleywang.putty.service;

import com.haleywang.putty.dto.EventDto;
import com.haleywang.putty.view.SpringRemoteView;
import org.slf4j.event.Level;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;

/**
 * @author haley
 */
public class NotificationsService {

    private List<EventDto> events = new ArrayList<>();


    private static class SingletonHolder {
        private static final NotificationsService INSTANCE = new NotificationsService();
    }

    private NotificationsService() {
    }

    public static final NotificationsService getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public void info(String message) {
        log(message, Level.INFO);

    }

    private void log(String message, Level info) {
        SwingUtilities.invokeLater(() -> {
            EventDto eventDto = EventDto.log(message, info);
            events.add(eventDto);
            SpringRemoteView.getInstance().fillNotificationLabel(eventDto);
        });

    }

    public void warn(String message) {
        log(message, Level.WARN);

    }

    public void error(String message) {
        log(message, Level.ERROR);
    }


    public List<EventDto> getEvents() {
        return events;
    }
}
