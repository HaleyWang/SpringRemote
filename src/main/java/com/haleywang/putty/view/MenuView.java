package com.haleywang.putty.view;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class MenuView extends JPanel {

    public static MenuView getInstance(){
        return MenuView.SingletonHolder.sInstance;
    }
    private static class SingletonHolder {
        private static final MenuView sInstance = new MenuView();
    }

    private MenuView() {

        JPanel menuPanel = this;
        menuPanel.setLayout(new FlowLayout(FlowLayout.LEFT,4,2));

        JButton refreshBtn = new JButton("Refresh");
        JButton pasteBtn = new JButton("Paste");
        JButton aboutBtn = new JButton("About");

        menuPanel.add(refreshBtn);
        menuPanel.add(pasteBtn);
        menuPanel.add(aboutBtn);

        pasteBtn.addActionListener(e -> {
            try {
                String data = (String) Toolkit.getDefaultToolkit()
                        .getSystemClipboard().getData(DataFlavor.stringFlavor);
                SpringRemoteView.getInstance().typedString(data);

            } catch (UnsupportedFlavorException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        });

        refreshBtn.addActionListener(e -> {
            SideView.getInstance().reloadData();

        });

        aboutBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(MenuView.this,
                    "SpringRemote 0.1",
                    "About",
                    JOptionPane.INFORMATION_MESSAGE);
        });

    }
}
