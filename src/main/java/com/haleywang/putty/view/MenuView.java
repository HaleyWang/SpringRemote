package com.haleywang.putty.view;

import com.haleywang.putty.service.action.ActionsData;
import com.haleywang.putty.dto.Action;
import com.haleywang.putty.storage.FileStorage;
import com.haleywang.putty.util.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MenuView extends JPanel {
    private static final Logger LOGGER = LoggerFactory.getLogger(MenuView.class);
    private final ButtonGroup layoutButtonsGroup;

    private List<AbstractButton> layoutBtns = new ArrayList<>();


    public static MenuView getInstance(){
        return MenuView.SingletonHolder.sInstance;
    }

    public void setLayoutButtonsStatus() {

        int tabLayout = FileStorage.INSTANCE.getSettingDto(SpringRemoteView.getInstance().getUserName()).getTabLayout();

        AbstractButton btn = CollectionUtils.getItem(layoutBtns, tabLayout-1);
        Optional.ofNullable(btn).orElse(layoutBtns.get(0)).doClick();
    }

    private static class SingletonHolder {
        private static final MenuView sInstance = new MenuView();
    }

    private MenuView() {

        JPanel menuPanel = this;
        menuPanel.setLayout(new FlowLayout(FlowLayout.LEFT,4,2));

        JButton refreshBtn = new JButton("Reload");
        JButton pasteBtn = new JButton("Paste");
        JButton aboutBtn = new JButton("About");
        JButton actionsBtn = new JButton("Actions");
        menuPanel.add(refreshBtn);
        menuPanel.add(pasteBtn);
        menuPanel.add(aboutBtn);
        menuPanel.add(actionsBtn);

        layoutButtonsGroup = new ButtonGroup();

        List<Action> layoutActions = ActionsData.getLayoutActionsData();
        for(int i = 0, n = ActionsData.getLayoutActionsData().size(); i< n; i++) {
            AbstractButton btn = new JToggleButton(layoutActions.get(i).getName());
            menuPanel.add(btn);
            layoutBtns.add(btn);
            layoutButtonsGroup.add(btn);

            btn.addActionListener(e -> {
                Object source = e.getSource();

                if(source instanceof AbstractButton) {
                    AbstractButton layoutButton = (AbstractButton) source;
                    String layoutButtonText = layoutButton.getText();
                    LOGGER.info("layout button:{}" , layoutButtonText);

                    SpringRemoteView.getInstance().changeAndSaveTermIndex(layoutButtonText);

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

        actionsBtn.addActionListener(e ->
                new ActionsDialog(SpringRemoteView.getInstance()).setVisible(true)
        );

    }

    public ButtonGroup getLayoutButtonsGroup() {
        return layoutButtonsGroup;
    }
}
