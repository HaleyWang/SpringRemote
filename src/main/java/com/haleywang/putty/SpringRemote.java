package com.haleywang.putty;

import com.haleywang.putty.view.SpringRemoteView;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
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

        } catch (IllegalAccessException e) {
            LOGGER.log(Level.SEVERE, null, e);
        } catch (InstantiationException e) {
            LOGGER.log(Level.SEVERE, null, e);
        } catch (UnsupportedLookAndFeelException e) {
            LOGGER.log(Level.SEVERE, null, e);
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, null, e);
        }

        /*
The complete Swing processing is done in a thread called EDT (Event Dispatching Thread). Therefore you would block the GUI if you would compute some long lasting calculations within this thread.

The way to go here is to process your calculation within a different thread, so your GUI stays responsive. At the end you want to update your GUI, which have to be done within the EDT. Now EventQueue.invokeLater comes into play. It posts an event (your Runnable) at the end of Swings event list and is processed after all previous GUI events are processed.

Also the usage of EventQueue.invokeAndWait is possible here. The difference is, that your calculation thread blocks until your GUI is updated. So it is obvious that this must not be used from the EDT.

Be careful not to update your Swing GUI from a different thread. In most cases this produces some strange updating/refreshing issues.

Still there is Java code out there that starts a JFrame simple from the main thread. This could cause issues, but is not prevented from Swing. Most modern IDEs now create something like this to start the GUI:
        */
        EventQueue.invokeLater(SpringRemoteView::getInstance);
    }
}
