package org.demo;

import com.haleywang.putty.util.CookSwingUtils;
import cookxml.cookswing.CookSwing;

import javax.swing.JLabel;
import java.awt.Container;

public class CookSwingDemo {
    public JLabel lb1;

    public CookSwingDemo() {
        CookSwing cookSwing = new CookSwing();
        Container c = cookSwing.render("demo/helloworld.xml");


        CookSwingUtils.fillFieldsValue(this, cookSwing);

        String aa = lb1.getText();

        System.out.println("end" + aa);
    }


    public static void main(String[] args) {
        new CookSwingDemo();

    }
}
