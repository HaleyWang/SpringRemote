package com.haleywang.putty;

import com.formdev.flatlaf.FlatIntelliJLaf;
import com.haleywang.putty.view.SpringRemoteView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.UIManager;
import java.awt.EventQueue;

public class SpringRemote {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringRemote.class);

    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel(new FlatIntelliJLaf());
        } catch (Exception e) {
            LOGGER.error("setLookAndFeel error", e);
        }

        EventQueue.invokeLater(SpringRemoteView::getInstance);
    }
}
