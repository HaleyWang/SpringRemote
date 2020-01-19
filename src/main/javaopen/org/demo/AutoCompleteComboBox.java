package org.demo;

import org.jdesktop.swingx.JXTextField;
import org.jdesktop.swingx.autocomplete.AutoCompleteComboBoxEditor;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;
import org.jdesktop.swingx.autocomplete.ObjectToStringConverter;

import javax.swing.ComboBoxEditor;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.plaf.metal.MetalComboBoxEditor;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

public class AutoCompleteComboBox {

    public static void main(String[] args) {
        JFrame jf = new JFrame();

        jf.getContentPane().setLayout(new BorderLayout());

        jf.setSize(640, 480);

        jf.getContentPane().add(new AutoCompleteComboBox().initUi2(), BorderLayout.NORTH);

        jf.setVisible(true);
    }


    public JTextComponent initUi2() {

        List<String> list = new ArrayList<>();
        list.add("@aaaa");
        list.add("@abc");
        list.add("@ab123c");

        JXTextField tf = new JXTextField();

        AutoCompleteDecorator.decorate(tf, list, false);

        return tf;
    }

    public JComboBox initUi3() {

        JComboBox c = new JComboBox();
        c.addItem("aaaa");
        c.addItem("abc");
        c.addItem("ab123c");
        AutoCompleteDecorator.decorate(c);

        return c;
    }

    public Component initUi() {

        ComboBoxEditor e = new MetalComboBoxEditor();
        e.setItem("aaa");
        e.setItem("abcd");
        e.setItem("abc123");

        ObjectToStringConverter c = new ObjectToStringConverter() {
            @Override
            public String getPreferredStringForItem(Object item) {
                return item.toString();
            }
        };
        return new AutoCompleteComboBoxEditor(e, c).getEditorComponent();

    }
}