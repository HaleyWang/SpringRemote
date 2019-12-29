package com.haleywang.putty.view.tab.close;


import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.UIManager;

public class DemoCBT {

    public static void main(String[] args) {

        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            //do nothing
        }

        JFrame jFrame = new JFrame();
        jFrame.setSize(600, 600);

        JSplitPane jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        DnDCloseButtonTabbedPane tp = new DnDCloseButtonTabbedPane(null);
        DnDCloseButtonTabbedPane tp2 = new DnDCloseButtonTabbedPane(null);

        tp.add("1", new JButton("1"));
        tp.add("11", new JButton("1"));
        tp2.add("2", new JButton("1"));


        jsp.setLeftComponent(tp);
        jsp.setRightComponent(tp2);

        jFrame.setContentPane(jsp);

        jFrame.setVisible(true);

    }
}
