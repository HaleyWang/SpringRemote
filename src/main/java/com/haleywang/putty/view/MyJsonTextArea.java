package com.haleywang.putty.view;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public class MyJsonTextArea extends RSyntaxTextArea {

    public MyJsonTextArea(int rows, int cols) {
        super(rows, cols);
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
        });

        popupMenu.add(formatMenu);
        return popupMenu;
    }
}
