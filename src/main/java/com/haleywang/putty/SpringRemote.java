package com.haleywang.putty;

import com.haleywang.putty.util.FontUtils;
import com.haleywang.putty.view.SpringRemoteView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.UIManager;
import java.awt.EventQueue;

/**
 * @author haley
 */
public class SpringRemote {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringRemote.class);

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(SpringRemoteView.getLookAndFeel());
            FontUtils.setWindowsDefaultUiFont();
        } catch (Exception e) {
            LOGGER.error("setLookAndFeel error", e);
        }

        EventQueue.invokeLater(SpringRemoteView::getInstance);
    }
}
