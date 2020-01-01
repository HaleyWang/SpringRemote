package com.haleywang.putty.view.puttypanel;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.intellij.util.ui.DrawUtil;

import javax.swing.JFrame;
import javax.swing.UIManager;
import java.awt.BorderLayout;

public class SshDemo {

    public static void main(String[] args) {
        try {
            //UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            UIManager.setLookAndFeel(new FlatIntelliJLaf());
            UIManager.setLookAndFeel(new FlatDarculaLaf());
            //FlatDarkLaf

            boolean a = DrawUtil.isUnderDarcula();
            System.out.println(a);
        } catch (Exception e) {
            e.printStackTrace();
        }
        final JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());

        frame.getContentPane().add(new IdeaPuttyPanel("127.0.0.1", "haley", "22", ""));

        frame.pack();
        frame.setVisible(true);
    }
}
