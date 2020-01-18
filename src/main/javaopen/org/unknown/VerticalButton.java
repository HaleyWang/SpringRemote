package org.unknown;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;

public class VerticalButton extends JToggleButton {
    public final static int ROTATE_RIGHT = 1;

    public final static int DONT_ROTATE = 0;

    public final static int ROTATE_LEFT = -1;

    private int rotation = DONT_ROTATE;

    private boolean painting = false;

    public VerticalButton() {
        super();
    }


    public VerticalButton(Icon image) {
        super(image);
    }

    public static VerticalButton rotateLeftBtn(String text) {
        VerticalButton btn = new VerticalButton(text);
        btn.setRotation(VerticalButton.ROTATE_LEFT);
        return btn;
    }


    public VerticalButton(String text) {
        super(text);
    }

    public int getRotation() {
        return rotation;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
    }

    public boolean isRotated() {
        return rotation != DONT_ROTATE;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        if (isRotated())
            g2d.rotate(Math.toRadians(90 * rotation));
        if (rotation == ROTATE_RIGHT)
            g2d.translate(0, -this.getWidth());
        else if (rotation == ROTATE_LEFT)
            g2d.translate(-this.getHeight(), 0);
        painting = true;

        super.paintComponent(g2d);

        painting = false;
        if (isRotated())
            g2d.rotate(-Math.toRadians(90 * rotation));
        if (rotation == ROTATE_RIGHT)
            g2d.translate(-this.getWidth(), 0);
        else if (rotation == ROTATE_LEFT)
            g2d.translate(0, -this.getHeight());
    }

    @Override
    public Insets getInsets(Insets insets) {
        insets = super.getInsets(insets);
        if (painting) {
            if (rotation == ROTATE_LEFT) {
                int temp = insets.bottom;
                insets.bottom = insets.left;
                insets.left = insets.top;
                insets.top = insets.right;
                insets.right = temp;
            } else if (rotation == ROTATE_RIGHT) {
                int temp = insets.bottom;
                insets.bottom = insets.right;
                insets.right = insets.top;
                insets.top = insets.left;
                insets.left = temp;
            }
        }
        return insets;
    }

    @Override
    public Insets getInsets() {
        Insets insets = super.getInsets();
        if (painting) {
            if (rotation == ROTATE_LEFT) {
                int temp = insets.bottom;
                insets.bottom = insets.left;
                insets.left = insets.top;
                insets.top = insets.right;
                insets.right = temp;
            } else if (rotation == ROTATE_RIGHT) {
                int temp = insets.bottom;
                insets.bottom = insets.right;
                insets.right = insets.top;
                insets.top = insets.left;
                insets.left = temp;
            }
        }
        return insets;
    }

    @Override
    public int getWidth() {
        if ((painting) && (isRotated()))
            return super.getHeight();
        return super.getWidth();
    }

    @Override
    public int getHeight() {
        if ((painting) && (isRotated()))
            return super.getWidth();
        return super.getHeight();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        if (isRotated()) {
            int width = d.width;
            d.width = d.height;
            d.height = width;
        }
        return d;
    }

    @Override
    public Dimension getMinimumSize() {
        Dimension d = super.getMinimumSize();
        if (isRotated()) {
            int width = d.width;
            d.width = d.height;
            d.height = width;
        }
        return d;
    }

    @Override
    public Dimension getMaximumSize() {
        Dimension d = super.getMaximumSize();
        if (isRotated()) {
            int width = d.width;
            d.width = d.height + 10;
            d.height = width + 10;
        }
        return d;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        final JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new FlowLayout());
        VerticalButton label = new VerticalButton("Testing something1");
        VerticalButton label2 = new VerticalButton("Testing something2");
        VerticalButton label3 = new VerticalButton("Testing something3");
        String filename = "shortcut.png";
        label.setIcon(new ImageIcon(filename));
        label2.setIcon(new ImageIcon(filename));
        label3.setIcon(new ImageIcon(filename));
        label.setRotation(VerticalButton.ROTATE_LEFT);
        label2.setRotation(VerticalButton.DONT_ROTATE);
        label3.setRotation(VerticalButton.ROTATE_RIGHT);
        frame.getContentPane().add(label);
        frame.getContentPane().add(label2);
        frame.getContentPane().add(label3);
        frame.pack();
        frame.setVisible(true);
    }

}