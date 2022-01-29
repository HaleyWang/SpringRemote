package com.haleywang.putty.view.side.subview;

import com.haleywang.putty.dto.CommandDto;
import com.haleywang.putty.util.StringUtils;
import com.haleywang.putty.view.CommandEditor;
import com.haleywang.putty.view.PlaceholderTextField;
import com.haleywang.putty.view.SpringRemoteView;
import com.haleywang.putty.view.side.SideView;
import com.mindbright.util.StringUtil;
import org.fife.ui.rsyntaxtextarea.FoldingAwareIconRowHeader;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.GutterIconInfo;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.Arrays;


/**
 * @author haley
 * @date 2020/2/2
 */
public class CommandEditorPanel extends JPanel implements TextAreaMenu.RunAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandEditorPanel.class);

    private JTextArea updateCommandTextArea;
    private PlaceholderTextField commandNameTextField;
    private CommandDto currentEditCommand;
    private Gutter gutter;

    public CommandEditorPanel() {
        createUpdateCommandPanel();
    }

    private void createUpdateCommandPanel() {
        JPanel updateCommandPanel = this;
        updateCommandPanel.setLayout(new BorderLayout());

        updateCommandTextArea = new CommandEditor();

        RTextScrollPane sp = new RTextScrollPane(updateCommandTextArea);
        sp.setIconRowHeaderEnabled(true);

        gutter = sp.getGutter();

        gutter.getIconArea().addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                Component[] comps = gutter.getComponents();
                FoldingAwareIconRowHeader iconComp = (FoldingAwareIconRowHeader) Arrays.stream(comps)
                        .filter(o -> o instanceof FoldingAwareIconRowHeader)
                        .findFirst().orElse(null);
                if(iconComp == null) {
                    return;
                }
                int offs = gutter.getTextArea().viewToModel(e.getPoint());
                try {
                    int currLine = gutter.getTextArea().getLineOfOffset(offs);
                    GutterIconInfo[] trackingIcons = iconComp.getTrackingIcons(currLine);

                    if(trackingIcons.length >= 1){
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



        });

        

        updateCommandTextArea.setLineWrap(true);

        updateCommandTextArea.setEditable(true);

        updateCommandTextArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {

                if ((e.getKeyCode() == KeyEvent.VK_E)
                        && (e.isControlDown())) {

                    SwingUtilities.invokeLater(() ->
                            SpringRemoteView.getInstance().onTypedString(updateCommandTextArea.getText())
                    );

                } else if ((e.getKeyCode() == KeyEvent.VK_S)
                        && (e.isControlDown())) {
                    LOGGER.info("save command");
                    SideView.getInstance().saveCommand();

                }

            }
        });

        JPanel btnsPanel = new JPanel();
        JButton execBtn = new JButton("Run");
        JButton saveBtn = new JButton("Save");
        btnsPanel.add(saveBtn);
        btnsPanel.add(execBtn);

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

        updateCommandPanel.add(commandNameTextField, BorderLayout.NORTH);
        updateCommandPanel.setBorder(new LineBorder(Color.LIGHT_GRAY));


        JPanel updateCommandOuterPanel = new JPanel();
        updateCommandOuterPanel.setLayout(new BorderLayout());
        updateCommandOuterPanel.add(updateCommandPanel);
        updateCommandOuterPanel.setBorder(new EmptyBorder(2, 2, 2, 2));

        Arrays.stream(gutter.getComponents()).filter(o -> o instanceof FoldingAwareIconRowHeader)
                .findFirst().ifPresent(o -> o.setCursor(new Cursor(Cursor.HAND_CURSOR)));
    }

    @Override
    public void runWithSelectedText(String selectedText) {


        SideView.getInstance().runWithSelectedText(selectedText);
    }

    public void resetUpdateCommandView(CommandDto obj) {
        currentEditCommand = obj;
        if (updateCommandTextArea != null) {
            updateCommandTextArea.setText(obj.getCommand());
            commandNameTextField.setText(obj.getName());
        }
    }

    public JTextArea getUpdateCommandTextArea() {
        return updateCommandTextArea;
    }

    public PlaceholderTextField getCommandNameTextField() {
        return commandNameTextField;
    }

    public CommandDto getCurrentEditCommand() {
        return currentEditCommand;
    }
}
