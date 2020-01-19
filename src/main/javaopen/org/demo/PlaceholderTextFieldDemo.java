package org.demo;

import com.haleywang.putty.view.PlaceholderTextField;

import javax.swing.JOptionPane;
import java.awt.Font;

public class PlaceholderTextFieldDemo {

    public static void main(final String[] args) {
        final PlaceholderTextField tf = new PlaceholderTextField("");
        tf.setColumns(20);
        tf.setPlaceholder("All your base are belong to us!");
        final Font f = tf.getFont();
        tf.setFont(new Font(f.getName(), f.getStyle(), 30));
        JOptionPane.showMessageDialog(null, tf);
    }
}
