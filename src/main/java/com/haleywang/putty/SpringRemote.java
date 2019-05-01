package com.haleywang.putty;

import com.haleywang.putty.view.SpringRemoteView;

import javax.swing.UIManager;
import java.awt.EventQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpringRemote {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringRemoteView.class);

    public static void main(String[] args) {

        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.error("setLookAndFeel error", e);
        }
        EventQueue.invokeLater(SpringRemoteView::getInstance);
    }
}
