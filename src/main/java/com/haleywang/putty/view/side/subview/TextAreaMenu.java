package com.haleywang.putty.view.side.subview;

import com.haleywang.putty.util.StringUtils;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class TextAreaMenu extends JTextArea implements MouseListener {

    public interface RunAction {

        void runWithSelectedText(String selectedText);

    }

    private static final long serialVersionUID = -2308615404205560110L;

    private JPopupMenu pop;
    private JMenuItem runMenu;
    private JMenuItem copy;
    private JMenuItem paste;
    private JMenuItem cut;
    private RunAction runAction;

    public TextAreaMenu(RunAction runAction) {
        super();
        this.runAction = runAction;
        init();
    }

    private void init() {
        this.addMouseListener(this);
        pop = new JPopupMenu();
        pop.add(runMenu = new JMenuItem("Run"));
        pop.add(copy = new JMenuItem("Copy"));
        pop.add(paste = new JMenuItem("Paste"));
        pop.add(cut = new JMenuItem("Cut"));
        copy.setAccelerator(KeyStroke.getKeyStroke('C', InputEvent.CTRL_MASK));
        paste.setAccelerator(KeyStroke.getKeyStroke('V', InputEvent.CTRL_MASK));
        cut.setAccelerator(KeyStroke.getKeyStroke('X', InputEvent.CTRL_MASK));
        runMenu.addActionListener(e -> action(e));
        copy.addActionListener(e -> action(e));
        paste.addActionListener(e -> action(e));
        cut.addActionListener(e -> action(e));
        this.add(pop);
    }


    public void action(ActionEvent e) {
        String str = e.getActionCommand();
        if (str.equals(copy.getText())) {
            this.copy();
        } else if (str.equals(runMenu.getText())) {
            this.run();
        } else if (str.equals(paste.getText())) {
            this.paste();
        } else if (str.equals(cut.getText())) {
            this.cut();
        }
    }

    private void run() {
        String text = this.getSelectedText();

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
        }
        return b;
    }


    public boolean isCanCopy() {
        boolean b = false;
        int start = this.getSelectionStart();
        int end = this.getSelectionEnd();
        if (start != end)
            b = true;
        return b;
    }

    public void mouseClicked(MouseEvent e) {
        //do nothing
    }

    public void mouseEntered(MouseEvent e) {
        //do nothing
    }

    public void mouseExited(MouseEvent e) {
        //do nothing
    }

    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            copy.setEnabled(isCanCopy());
            paste.setEnabled(isClipboardString());
            cut.setEnabled(isCanCopy());
            runMenu.setEnabled(!StringUtils.isBlank(this.getText()));
            pop.show(this, e.getX(), e.getY());
        }
    }

    public void mouseReleased(MouseEvent e) {
        //do nothing
    }

}