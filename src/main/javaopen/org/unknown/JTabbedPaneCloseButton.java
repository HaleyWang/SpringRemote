package org.unknown;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.plaf.metal.MetalIconFactory;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * @author 6dc
 * <p>
 * A class which creates a JTabbedPane and auto sets a close button when you add a tab
 */
public class JTabbedPaneCloseButton extends JTabbedPane {

    public JTabbedPaneCloseButton() {
        super();
    }

    /* Override Addtab in order to add the close Button everytime */
    @Override
    public void addTab(String title, Icon icon, Component component, String tip) {
        super.addTab(title, icon, component, tip);
        int count = this.getTabCount() - 1;
        setTabComponentAt(count, new CloseButtonTab(component, title, icon));
    }

    @Override
    public void addTab(String title, Icon icon, Component component) {
        addTab(title, icon, component, null);
    }

    @Override
    public void addTab(String title, Component component) {
        addTab(title, null, component);
    }

    /* addTabNoExit */
    public void addTabNoExit(String title, Icon icon, Component component, String tip) {
        super.addTab(title, icon, component, tip);
    }

    public void addTabNoExit(String title, Icon icon, Component component) {
        addTabNoExit(title, icon, component, null);
    }

    public void addTabNoExit(String title, Component component) {
        addTabNoExit(title, null, component);
    }

    /* Button */
    public class CloseButtonTab extends JPanel {
        private Component tab;

        public CloseButtonTab(final Component tab, String title, Icon icon) {
            this.tab = tab;
            setOpaque(false);
            FlowLayout flowLayout = new FlowLayout(FlowLayout.CENTER, 1, 1);
            setLayout(flowLayout);
            JLabel jLabel = new JLabel(title);
            jLabel.setIcon(icon);
            add(jLabel);
            JButton button = new JButton(MetalIconFactory.getInternalFrameCloseIcon(16));
            button.setMargin(new Insets(0, 0, 0, 0));
            button.setBorder(BorderFactory.createEmptyBorder());

            button.addMouseListener(new CloseListener(tab));
            button.setBorderPainted(false);
            add(button);
        }

        public Component getTab() {
            return tab;
        }
    }

    /* ClickListener */
    public class CloseListener implements MouseListener {
        private Component tab;

        public CloseListener(Component tab) {
            this.tab = tab;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getSource() instanceof JButton) {
                JButton clickedButton = (JButton) e.getSource();
                JTabbedPane tabbedPane = (JTabbedPane) clickedButton.getParent().getParent().getParent();
                tabbedPane.remove(tab);
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            //do nothing
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            //do nothing
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            //do nothing
        }

        @Override
        public void mouseExited(MouseEvent e) {
            //do nothing
        }
    }
}