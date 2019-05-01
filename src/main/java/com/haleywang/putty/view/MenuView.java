package com.haleywang.putty.view;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class MenuView extends JPanel {
    private static final Logger LOGGER = LoggerFactory.getLogger(MenuView.class);


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

        List<String> layoutButtons = Arrays.asList("1", "H2", "V2", "4");
        ButtonGroup bg = new ButtonGroup();

        for(int i = 0, n = layoutButtons.size(); i< n; i++) {
            JButton btn = new JButton(layoutButtons.get(i));
            menuPanel.add(btn);
            bg.add(btn);

            btn.addActionListener(e -> {
                Object source = e.getSource();

                if(source instanceof JButton) {
                    JButton layoutButton = (JButton) source;
                    String layoutButtonText = layoutButton.getText();
                    LOGGER.info("layout button:{}" , layoutButtonText);
                    int index = layoutButtons.indexOf(layoutButtonText) + 1;

                    SpringRemoteView.getInstance().setTermCount(index);
                    if(index == 3) {
                        SpringRemoteView.getInstance().setTermCount(2);
                        SpringRemoteView.getInstance().setOrientation(JSplitPane.VERTICAL_SPLIT);

                    }else if(index == 2) {
                        SpringRemoteView.getInstance().setOrientation(JSplitPane.HORIZONTAL_SPLIT);
                    }
                    SpringRemoteView.getInstance().changeLayout();
                }

            });
        }

        pasteBtn.addActionListener(e -> {
            try {
                String data = (String) Toolkit.getDefaultToolkit()
                        .getSystemClipboard().getData(DataFlavor.stringFlavor);
                SpringRemoteView.getInstance().typedString(data);

            } catch (UnsupportedFlavorException e1) {
                LOGGER.error("pasteBtn UnsupportedFlavorException", e1);
            } catch (IOException e1) {
                LOGGER.error("pasteBtn IOException", e1);
            }

        });

        refreshBtn.addActionListener(e ->
            SideView.getInstance().reloadData()
        );

        aboutBtn.addActionListener(e ->
            JOptionPane.showMessageDialog(MenuView.this,
                    "SpringRemote 0.1",
                    "About",
                    JOptionPane.INFORMATION_MESSAGE)
        );

    }
}
