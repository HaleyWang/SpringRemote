package com.haleywang.putty.view;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unknown.VerticalButton;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Font;


/**
 * @author haley
 */
public class LeftMenuView extends JPanel {

    private static final Logger LOGGER = LoggerFactory.getLogger(LeftMenuView.class);
    private final VerticalButton connectionsTabBtn;
    private final VerticalButton commandsJsonTabBtn;
    private final VerticalButton commandTabBtn;
    private final VerticalButton connectionsJsonTabBtn;
    private final VerticalButton commandsTabBtn;
    private final VerticalButton passwordTabBtn;
    private final ButtonGroup topButtonGroup;
    private final ButtonGroup bottomButtonGroup;

    public static LeftMenuView getInstance() {
        return LeftMenuView.SingletonHolder.S_INSTANCE;
    }

    private static class SingletonHolder {
        private static final LeftMenuView S_INSTANCE = new LeftMenuView();
    }

    private LeftMenuView() {
        JPanel sideTabPanel = this;
        sideTabPanel.setPreferredSize(new Dimension(36, 200));

        sideTabPanel.setLayout(new BoxLayout(sideTabPanel, BoxLayout.Y_AXIS));

        this.connectionsTabBtn = VerticalButton.rotateLeftBtn("Connections");
        connectionsTabBtn.setSelected(true);

        sideTabPanel.add(connectionsTabBtn);

        this.commandsJsonTabBtn = VerticalButton.rotateLeftBtn("Commands json");
        this.commandTabBtn = VerticalButton.rotateLeftBtn("Edit command");

        Font font = commandsJsonTabBtn.getFont();
        Font newFont = new java.awt.Font(
                font.getName(), font.getStyle(), 11);
        commandsJsonTabBtn.setFont(newFont);
        commandTabBtn.setFont(newFont);


        sideTabPanel.add(commandsJsonTabBtn);
        sideTabPanel.add(commandTabBtn);

        topButtonGroup = new ButtonGroup();
        topButtonGroup.add(connectionsTabBtn);
        topButtonGroup.add(commandsJsonTabBtn);
        topButtonGroup.add(commandTabBtn);

        connectionsTabBtn.setFont(newFont);


        sideTabPanel.add(Box.createVerticalGlue());

        this.connectionsJsonTabBtn = VerticalButton.rotateLeftBtn("Connections json");
        sideTabPanel.add(connectionsJsonTabBtn);

        this.commandsTabBtn = VerticalButton.rotateLeftBtn("Commands");
        commandsTabBtn.setSelected(true);
        sideTabPanel.add(commandsTabBtn);

        this.passwordTabBtn = VerticalButton.rotateLeftBtn("Password");
        sideTabPanel.add(passwordTabBtn);

        bottomButtonGroup = new ButtonGroup();

        bottomButtonGroup.add(connectionsJsonTabBtn);
        bottomButtonGroup.add(commandsTabBtn);
        bottomButtonGroup.add(passwordTabBtn);

        connectionsJsonTabBtn.setFont(newFont);
        commandsTabBtn.setFont(newFont);
        passwordTabBtn.setFont(newFont);

        LOGGER.info("init LeftMenuView");
    }

    public VerticalButton getConnectionsTabBtn() {
        return connectionsTabBtn;
    }

    public VerticalButton getCommandsJsonTabBtn() {
        return commandsJsonTabBtn;
    }

    public VerticalButton getCommandTabBtn() {
        return commandTabBtn;
    }

    public VerticalButton getConnectionsJsonTabBtn() {
        return connectionsJsonTabBtn;
    }

    public VerticalButton getCommandsTabBtn() {
        return commandsTabBtn;
    }

    public VerticalButton getPasswordTabBtn() {
        return passwordTabBtn;
    }

    public ButtonGroup getTopButtonGroup() {
        return topButtonGroup;
    }

    public ButtonGroup getBottomButtonGroup() {
        return bottomButtonGroup;
    }
}
