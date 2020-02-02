package com.haleywang.putty.view;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public class MyJsonTextArea extends RSyntaxTextArea {

    public interface AfterFormatAction {

        void doAfterFormat();
    }

    private AfterFormatAction afterFormatAction;

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


            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonParser jp = new JsonParser();
            JsonElement je = jp.parse(MyJsonTextArea.this.getText());
            String prettyJsonString = gson.toJson(je);

            MyJsonTextArea.this.setText(prettyJsonString);
            if (afterFormatAction != null) {
                afterFormatAction.doAfterFormat();
            }
        });

        popupMenu.add(formatMenu);
        return popupMenu;
    }
}
