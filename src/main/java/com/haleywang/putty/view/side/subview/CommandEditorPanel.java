package com.haleywang.putty.view.side.subview;

import com.haleywang.putty.dto.CommandDto;
import com.haleywang.putty.view.PlaceholderTextField;
import com.haleywang.putty.view.SpringRemoteView;
import com.haleywang.putty.view.side.SideView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class CommandEditorPanel extends JPanel implements TextAreaMenu.RunAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandEditorPanel.class);

    private JTextArea updateCommandTextArea;
    private PlaceholderTextField commandNameTextField;
    private CommandDto currentEditCommand;

    public CommandEditorPanel() {
        createUpdateCommandPanel();
    }

    private void createUpdateCommandPanel() {
        JPanel updateCommandPanel = this;
        updateCommandPanel.setLayout(new BorderLayout());

        JTextArea textArea = new TextAreaMenu(this);

        JScrollPane sp = new JScrollPane(textArea);
        updateCommandTextArea = textArea;
        updateCommandTextArea.setLineWrap(true);

        updateCommandTextArea.setEditable(true);

        updateCommandTextArea.setTransferHandler(new TransferHandler() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean importData(JComponent comp, Transferable t) {
                try {
                    Object o = "";
                    String filepath = o.toString();

                    try {
                        o = t.getTransferData(DataFlavor.javaFileListFlavor);
                        filepath = o.toString();

                        if (filepath.startsWith("[")) {
                            filepath = filepath.substring(1);
                        }
                        if (filepath.endsWith("]")) {
                            filepath = filepath.substring(0, filepath.length() - 1);
                        }
                    } catch (UnsupportedFlavorException ex) {
                        o = t.getTransferData(DataFlavor.stringFlavor);
                        filepath = o.toString();
                    }


                    int pos = updateCommandTextArea.getCaretPosition();

                    String text = updateCommandTextArea.getText();
                    text = text.substring(0, pos) + filepath + text.substring(pos);

                    updateCommandTextArea.setText(text);
                    updateCommandTextArea.setCaretPosition(pos + filepath.length());
                    return true;

                } catch (Exception e) {
                    LOGGER.error("updateCommandTextArea TransferHandler error", e);
                }
                return false;
            }

            @Override
            public boolean canImport(JComponent comp, DataFlavor[] flavors) {
                for (int i = 0; i < flavors.length; i++) {
                    if (DataFlavor.javaFileListFlavor.equals(flavors[i])) {
                        return true;
                    }
                }
                return false;
            }
        });

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
                        SpringRemoteView.getInstance().onTypedString(updateCommandTextArea.getText())
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
