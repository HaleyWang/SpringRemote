package com.haleywang.putty.view.side.subview;

import com.google.common.collect.Iterables;
import com.haleywang.putty.dto.CommandDto;
import com.haleywang.putty.dto.TmpCommandsDto;
import com.haleywang.putty.storage.FileStorage;
import com.haleywang.putty.util.Debounce;
import com.haleywang.putty.util.StringUtils;
import com.haleywang.putty.view.CommandEditor;
import com.haleywang.putty.view.CommandHistoryDialog;
import com.haleywang.putty.view.CommandTreeDialog;
import com.haleywang.putty.view.PlaceholderTextField;
import com.haleywang.putty.view.SpringRemoteView;
import com.haleywang.putty.view.side.SideView;
import org.fife.ui.rsyntaxtextarea.FoldingAwareIconRowHeader;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.GutterIconInfo;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;


/**
 * @author haley
 * @date 2020/2/2
 */
public class CommandEditorPanel extends JPanel implements TextAreaMenu.RunAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandEditorPanel.class);
    private static final long serialVersionUID = 9180496585788208988L;

    private JTextArea updateCommandTextArea;
    private PlaceholderTextField commandNameTextField;
    private Gutter gutter;
    private JLabel pathLb;

    public CommandEditorPanel() {
        createUpdateCommandPanel();
    }

    public void onUpdateCommandTextAreaKeyPress(KeyEvent e) {
        if ((e.getKeyCode() == KeyEvent.VK_E)
                && (e.isControlDown())) {

            SwingUtilities.invokeLater(() ->
                    SpringRemoteView.getInstance().onTypedString(updateCommandTextArea.getText())
            );

        } else if ((e.getKeyCode() == KeyEvent.VK_S)
                && (e.isControlDown())) {
            LOGGER.info("save command");
            SideView.getInstance().saveCommand();

        } else {
            Debounce.debounce("savetmpCommands", () -> {

                String tmpCommand = updateCommandTextArea.getText();
                if (StringUtils.isBlank(tmpCommand)) {
                    return;
                }
                TmpCommandsDto tmpCommandsDto = FileStorage.INSTANCE.getTmpCommandsJson();

                String pre = Iterables.getLast(tmpCommandsDto.getCommands(), "");
                boolean same = tmpCommand.equals(pre);
                if (same) {
                    return;
                }

                tmpCommandsDto.getCommands().add(tmpCommand);
                FileStorage.INSTANCE.saveTmpCommandsData(tmpCommandsDto);

            }, 3, TimeUnit.SECONDS);

        }
    }

    public void onIconAreaClick(MouseEvent e) {

        Component[] comps = gutter.getComponents();
        FoldingAwareIconRowHeader iconComp = (FoldingAwareIconRowHeader) Arrays.stream(comps)
                .filter(o -> o instanceof FoldingAwareIconRowHeader)
                .findFirst().orElse(null);
        if (iconComp == null) {
            return;
        }
        int offs = gutter.getTextArea().viewToModel(e.getPoint());
        try {
            int currLine = gutter.getTextArea().getLineOfOffset(offs);
            GutterIconInfo[] trackingIcons = iconComp.getTrackingIcons(currLine);

            if (trackingIcons.length >= 1) {
                //run command
                int lineStartOffset = gutter.getTextArea().getLineStartOffset(currLine);

                int lineEndOffset = gutter.getTextArea().getLineEndOffset(currLine);
                String commandText = gutter.getTextArea().getText(lineStartOffset, (lineEndOffset - lineStartOffset));
                commandText = StringUtils.trim(commandText);
                SideView.getInstance().runWithSelectedText(commandText);
                return;
            }
            gutter.removeAllTrackingIcons();

            URL url = ClassLoader.getSystemClassLoader().getResource("Play16.png");
            assert url != null;
            ImageIcon icon = new ImageIcon(url);

            gutter.addOffsetTrackingIcon(offs, icon, "Run");
        } catch (Exception ex) {
            LOGGER.error("click run icon error", ex);
        }
    }

    private void createUpdateCommandPanel() {
        JPanel updateCommandPanel = new JPanel();
        updateCommandPanel.setLayout(new BorderLayout());

        updateCommandTextArea = new CommandEditor();

        RTextScrollPane sp = new RTextScrollPane(updateCommandTextArea);
        sp.setIconRowHeaderEnabled(true);

        gutter = sp.getGutter();

        gutter.getIconArea().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                CommandEditorPanel.this.onIconAreaClick(e);
            }
        });

        updateCommandTextArea.setLineWrap(true);
        updateCommandTextArea.setEditable(true);

        updateCommandTextArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                CommandEditorPanel.this.onUpdateCommandTextAreaKeyPress(e);
            }
        });

        JPanel btnsPanel = new JPanel();
        JButton execBtn = new JButton("Run");
        JButton saveBtn = new JButton("Save");

        btnsPanel.add(saveBtn);
        btnsPanel.add(execBtn);
        btnsPanel.add(createMoreButton());


        execBtn.addActionListener(e ->
                SwingUtilities.invokeLater(() ->
                        SpringRemoteView.getInstance().onTypedString(StringUtils.ifBlank(updateCommandTextArea.getSelectedText(), updateCommandTextArea.getText()))
                )
        );
        saveBtn.addActionListener(e -> SideView.getInstance().saveCommand());

        updateCommandPanel.add(sp, BorderLayout.CENTER);
        updateCommandPanel.add(btnsPanel, BorderLayout.SOUTH);

        commandNameTextField = new PlaceholderTextField();
        commandNameTextField.setPlaceholder("Command name");
        JPanel jp = new JPanel(new BorderLayout());
        pathLb = new JLabel();
        pathLb.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                SwingUtilities.invokeLater(() ->
                        new CommandTreeDialog(SpringRemoteView.getInstance()).setVisible(true)
                );
            }
        });
        jp.add(pathLb, BorderLayout.NORTH);
        jp.add(commandNameTextField, BorderLayout.SOUTH);

        updateCommandPanel.add(jp, BorderLayout.NORTH);
        updateCommandPanel.setBorder(new LineBorder(Color.LIGHT_GRAY));


        JPanel updateCommandOuterPanel = this;
        updateCommandOuterPanel.setLayout(new BorderLayout());
        updateCommandOuterPanel.add(updateCommandPanel);
        updateCommandOuterPanel.setBorder(new EmptyBorder(2, 2, 2, 2));

        Arrays.stream(gutter.getComponents()).filter(o -> o instanceof FoldingAwareIconRowHeader)
                .findFirst().ifPresent(o -> o.setCursor(new Cursor(Cursor.HAND_CURSOR)));
    }


    private AbstractButton createMoreButton() {
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

    private void createAndShowMenu(final JComponent component, final AbstractButton moreButton) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem saveAsMenu = new JMenuItem("Save as");
        saveAsMenu.addActionListener((e) ->
                SwingUtilities.invokeLater(() ->
                        new CommandTreeDialog(SpringRemoteView.getInstance()).setVisible(true)
                )
        );
        JMenuItem hisMenu = new JMenuItem("Command history");
        hisMenu.addActionListener((e) ->
                SwingUtilities.invokeLater(() ->
                        new CommandHistoryDialog(SpringRemoteView.getInstance()).setVisible(true)
                )
        );
        menu.add(saveAsMenu);
        menu.add(hisMenu);

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

    @Override
    public void runWithSelectedText(String selectedText) {


        SideView.getInstance().runWithSelectedText(selectedText);
    }

    public void resetUpdateCommandView(CommandDto obj, String path) {

        if (updateCommandTextArea != null) {
            updateCommandTextArea.setText(obj.getCommand());
            commandNameTextField.setText(obj.getName());
        }

        pathLb.setText(path);
    }

    public JTextArea getUpdateCommandTextArea() {
        return updateCommandTextArea;
    }

    public PlaceholderTextField getCommandNameTextField() {
        return commandNameTextField;
    }

    public void syncCommandsTree() {
        String command = getUpdateCommandTextArea().getText();
        String commandName = getCommandNameTextField().getText();
        CommandDto obj = SideView.getInstance().getCommandsTreePanel().getCurrentCommandDto();
        obj.setName(commandName);
        obj.setCommand(command);

    }
}
