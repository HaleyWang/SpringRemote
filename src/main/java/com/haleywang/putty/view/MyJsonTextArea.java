package com.haleywang.putty.view;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * @author haley
 * @date 2020/2/2
 */
public class MyJsonTextArea extends RSyntaxTextArea {

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

            String prettyJsonString = getFormatString(text);

            MyJsonTextArea.this.setText(prettyJsonString);
            if (afterFormatAction != null) {
                afterFormatAction.doAfterFormat();
            }
        });

        popupMenu.add(formatMenu);
        return popupMenu;
    }

    public static String getFormatString(String text) {
        if(text == null) {
            return null;
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(text);
        return gson.toJson(je);
    }
}
