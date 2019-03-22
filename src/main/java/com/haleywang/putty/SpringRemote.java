package com.haleywang.putty;

import com.haleywang.putty.view.SpringRemoteView;

import javax.swing.UIManager;
import java.awt.EventQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SpringRemote {

    private static final Logger LOGGER = Logger.getLogger(SpringRemoteView.class.getName());

    public static void main(String[] args) {

        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, null, e);
        }

        EventQueue.invokeLater(SpringRemoteView::getInstance);
    }
}
