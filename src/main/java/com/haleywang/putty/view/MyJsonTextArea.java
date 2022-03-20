package com.haleywang.putty.view;

import com.haleywang.putty.util.JsonUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * @author haley
 * @date 2020/2/2
 */
public class MyJsonTextArea extends RSyntaxTextArea {

    private static final long serialVersionUID = 870542040613430069L;

    public interface AfterFormatAction {

        /**
         * do something after format
         */
        void doAfterFormat();
    }

    private transient AfterFormatAction afterFormatAction;

    public MyJsonTextArea(int rows, int cols) {
        super(rows, cols);
    }

    public void setAfterFormatAction(AfterFormatAction afterFormatAction) {
        this.afterFormatAction = afterFormatAction;
    }

    @Override
    protected JPopupMenu createPopupMenu() {
        JPopupMenu popupMenu = super.createPopupMenu();

        JMenuItem formatMenu = new JMenuItem("Format");

        formatMenu.addActionListener(e -> {

            String text = MyJsonTextArea.this.getText();

            String prettyJsonString = JsonUtils.getFormatJsonString(text);

            MyJsonTextArea.this.setText(prettyJsonString);
            if (afterFormatAction != null) {
                afterFormatAction.doAfterFormat();
            }
        });

        popupMenu.add(formatMenu);
        return popupMenu;
    }


}
