package com.haleywang.putty.view;

import com.haleywang.putty.dto.Action;
import com.haleywang.putty.service.NotificationsService;
import com.haleywang.putty.service.action.ActionsData;
import com.haleywang.putty.storage.FileStorage;
import com.haleywang.putty.util.CollectionUtils;
import com.jcraft.jsch.ChannelSftp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * @author haley
 */
public class MenuView extends JPanel {
    private static final Logger LOGGER = LoggerFactory.getLogger(MenuView.class);
    private final ButtonGroup layoutButtonsGroup;

    private List<AbstractButton> layoutBtns = new ArrayList<>();


    public static MenuView getInstance() {
        return MenuView.SingletonHolder.S_INSTANCE;
    }

    public void setLayoutButtonsStatus() {

        int tabLayout = FileStorage.INSTANCE.getSettingDto(SpringRemoteView.getInstance().getUserName()).getTabLayout();

        AbstractButton btn = CollectionUtils.getItem(layoutBtns, tabLayout - 1);
        Optional.ofNullable(btn).orElse(layoutBtns.get(0)).doClick();
    }

    public void changeLayoutButtonsStatus(int termCount, int or) {

        if (layoutButtonsGroup == null) {
            return;
        }
        layoutButtonsGroup.clearSelection();

        if (JSplitPane.VERTICAL_SPLIT == or && termCount == 2) {
            layoutButtonsGroup.setSelected(layoutBtns.get(termCount).getModel(), true);
            return;
        }
        layoutButtonsGroup.setSelected(layoutBtns.get(termCount - 1).getModel(), true);
    }

    private static class SingletonHolder {
        private static final MenuView S_INSTANCE = new MenuView();
    }

    private MenuView() {

        JPanel menuPanel = this;
        menuPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 2));

        JButton refreshBtn = new JButton("Reload");
        JButton aboutBtn = new JButton("About");
        JButton actionsBtn = new JButton("Actions");
        JButton sftpBtn = new JButton("Sftp");
        menuPanel.add(refreshBtn);
        menuPanel.add(aboutBtn);
        menuPanel.add(actionsBtn);
        menuPanel.add(sftpBtn);

        layoutButtonsGroup = new ButtonGroup();

        List<Action> layoutActions = ActionsData.getLayoutActionsData();
        for (int i = 0, n = ActionsData.getLayoutActionsData().size(); i < n; i++) {
            AbstractButton btn = new JToggleButton(layoutActions.get(i).getName());
            menuPanel.add(btn);
            layoutBtns.add(btn);
            layoutButtonsGroup.add(btn);

            btn.addActionListener(e -> {
                Object source = e.getSource();

                if (source instanceof AbstractButton) {
                    AbstractButton layoutButton = (AbstractButton) source;
                    String layoutButtonText = layoutButton.getText();
                    LOGGER.info("layout button:{}", layoutButtonText);

                    SpringRemoteView.getInstance().changeAndSaveTermIndex(layoutButtonText);

                }

            });
        }

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
        sftpBtn.addActionListener(e -> {

            ChannelSftp sftpChannel = null;
            try {
                sftpChannel = SpringRemoteView.getInstance().openSftpChannel();
                new SftpDialog(SpringRemoteView.getInstance(), sftpChannel).setVisible(true);

            } catch (Exception e1) {
                NotificationsService.getInstance().showErrorDialog(SpringRemoteView.getInstance(), null, "Can not open sftp");
                e1.printStackTrace();
            }

        });

    }

    public ButtonGroup getLayoutButtonsGroup() {
        return layoutButtonsGroup;
    }
}
