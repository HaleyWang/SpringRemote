package com.haleywang.putty.view.side.subview;

import com.haleywang.putty.util.StringUtils;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * @author haley
 * @date 2020/2/2
 */
public class TextAreaMenu extends JTextArea implements MouseListener {

    public interface RunAction {

        /**
         * exec selected cmd
         *
         * @param selectedText The text you selected
         */
        void runWithSelectedText(String selectedText);

    }

    private static final long serialVersionUID = -2308615404205560110L;

    private JPopupMenu pop;
    private JMenuItem runMenu;
    private JMenuItem copy;
    private JMenuItem paste;
    private final transient RunAction runAction;

    public TextAreaMenu(RunAction runAction) {
        super();
        this.runAction = runAction;
        init();
    }

    private void init() {
        this.addMouseListener(this);
        pop = new JPopupMenu();
        runMenu = new JMenuItem("Run");
        copy = new JMenuItem("Copy");
        paste = new JMenuItem("Paste");
        pop.add(runMenu);
        pop.add(copy);
        pop.add(paste);
        copy.setAccelerator(KeyStroke.getKeyStroke('C', InputEvent.CTRL_DOWN_MASK));
        paste.setAccelerator(KeyStroke.getKeyStroke('V', InputEvent.CTRL_DOWN_MASK));
        runMenu.addActionListener(this::action);
        copy.addActionListener(this::action);
        paste.addActionListener(this::action);
        this.add(pop);
    }


    public void action(ActionEvent e) {
        String txt = e.getActionCommand();
        String actionCommand = StringUtils.ifBlank(txt, StringUtils.EMPTY);

        if (actionCommand.equals(copy.getText())) {
            this.copy();
        } else if (actionCommand.equals(runMenu.getText())) {
            this.run();
        } else if (actionCommand.equals(paste.getText())) {
            this.paste();
        }
    }

    @Override
    public void copy() {
        String text = this.getSelectedText();

        StringSelection stringSelection = new StringSelection(text);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }

    private void run() {
        String text = this.getSelectedText();

        runAction.runWithSelectedText(text);
    }

    public JPopupMenu getPop() {
        return pop;
    }

    public void setPop(JPopupMenu pop) {
        this.pop = pop;
    }


    public boolean isClipboardString() {
        boolean b = false;
        Clipboard clipboard = this.getToolkit().getSystemClipboard();
        Transferable content = clipboard.getContents(this);
        try {
            if (content.getTransferData(DataFlavor.stringFlavor) instanceof String) {
                b = true;
            }
        } catch (Exception e) {
            //do nothing
        }
        return b;
    }


    public boolean isCanCopy() {
        boolean b = false;
        int start = this.getSelectionStart();
        int end = this.getSelectionEnd();
        if (start != end) {
            b = true;
        }
        return b;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
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
    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            copy.setEnabled(isCanCopy());
            paste.setEnabled(isClipboardString());
            runMenu.setEnabled(!StringUtils.isBlank(this.getText()));
            pop.show(this, e.getX(), e.getY());
        }
    }
    @Override
    public void mouseReleased(MouseEvent e) {
        //do nothing
    }

}