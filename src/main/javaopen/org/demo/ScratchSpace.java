package org.demo;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class ScratchSpace {

    public static void main(String[] arguments) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                UIManager.put("PopupMenu.consumeEventOnClose", Boolean.TRUE);
                JFrame frame = new JFrame("Toolbar with Popup Menu demo");

                //final JToolBar toolBar = new JToolBar();
                //toolBar.add(createMoreButton());

                final JPanel panel = new JPanel(new BorderLayout());
                panel.add(createMoreButton(), BorderLayout.NORTH);
                panel.setPreferredSize(new Dimension(600, 400));
                frame.getContentPane().add(panel);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
    }

    private static AbstractButton createMoreButton() {
        final JToggleButton moreButton = new JToggleButton("More...");
        moreButton.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    createAndShowMenu((JComponent) e.getSource(), moreButton);
                }
            }
        });
        moreButton.setFocusable(false);
        moreButton.setHorizontalTextPosition(SwingConstants.LEADING);
        return moreButton;
    }

    private static void createAndShowMenu(final JComponent component, final AbstractButton moreButton) {
        JPopupMenu menu = new JPopupMenu();
        menu.add(new JMenuItem("Black"));
        menu.add(new JMenuItem("Red"));

        menu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                moreButton.setSelected(false);
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                moreButton.setSelected(false);
            }
        });

        menu.show(component, 0, component.getHeight());
    }
}