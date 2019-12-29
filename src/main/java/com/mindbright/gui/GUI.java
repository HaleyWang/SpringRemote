/******************************************************************************
 *
 * Copyright (c) 2005-2011 Cryptzone AB. All Rights Reserved.
 * 
 * This file contains Original Code and/or Modifications of Original Code as
 * defined in and that are subject to the MindTerm Public Source License,
 * Version 2.0, (the 'License'). You may not use this file except in compliance
 * with the License.
 * 
 * You should have received a copy of the MindTerm Public Source License
 * along with this software; see the file LICENSE.  If not, write to
 * Cryptzone Group AB, Drakegatan 7, SE-41250 Goteborg, SWEDEN
 *
 *****************************************************************************/

package com.mindbright.gui;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.MenuComponent;
import java.awt.MenuItem;
import java.awt.Toolkit;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import javax.swing.border.EmptyBorder;

public class GUI {
    
    public static boolean showConfirm(String title, String message,
                                      boolean defAnswer, Component parent) {
        return showConfirm(title, message, 0, 0, "Yes", "No", 
                           defAnswer, parent, false, false);
    }

    public static void showAlert(String title, String msg, Component parent) {
        JOptionPane.showMessageDialog(parent, msg, title,
                                      JOptionPane.ERROR_MESSAGE);
    }

    public static boolean showConfirm(String title, String message,
                                      int rows, int cols,
                                      String yesLbl, String noLbl,
                                      boolean defAnswer, Component parent,
                                      boolean xscroll, boolean yscroll) {
        final JDialog dialog = newBorderJDialog(parent,
                                                                 title, true);

        Component confirmText;

        if (rows == 0 || cols == 0) {
            confirmText = new JLabel(message);
        } else {
            JTextArea ta = new JTextArea(message);
            if (rows > 0) ta.setRows(rows);
            if (cols > 0) ta.setColumns(cols);
            ta.setEditable(false);
            if (!xscroll) {
                ta.setLineWrap(true);
                ta.setWrapStyleWord(true);
            }
            if (xscroll || yscroll) {
                confirmText = new JScrollPane
                    (ta, 
                     yscroll ? JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED : 
                     JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                     xscroll ? JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED : 
                     JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            } else {
                confirmText = ta;
            }
            
        }
        dialog.getContentPane().add(confirmText, BorderLayout.CENTER);

        JButton yes = new JButton(yesLbl);
        JButton no = new JButton(noLbl);

        ActionListener al = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ((JButton)e.getSource()).setEnabled(false); // ;^)
                dialog.dispose();
            }
        };
        
        yes.addActionListener(al);
        no.addActionListener(al);

        dialog.getContentPane().add(
            newButtonPanel(new JButton[] { yes, no }),
            BorderLayout.SOUTH);

        dialog.setResizable(true);
        dialog.pack();

        placeDialog(dialog);

        if (defAnswer)
            yes.requestFocus();
        else
            no.requestFocus();

        dialog.addWindowListener(getWindowDisposer());
        dialog.setVisible(true);

        return !yes.isEnabled();
    }

    public static void showNotice(Component parent, String title, String text, 
                           int rows, int cols, boolean scrollbar) {
        JDialog dialog = newBorderJDialog(parent, title, true);
        
        JTextArea ta = new JTextArea(text, rows, cols);
        Component comp;
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        if (scrollbar) {
            comp = new JScrollPane
                (ta, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                 JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        } else {
            comp = ta;
        }

        dialog.getContentPane().add(comp, BorderLayout.CENTER);

        JButton okBut = new JButton("OK");
        okBut.addActionListener(new CloseAction(dialog));

        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        p.add(okBut);
        dialog.getContentPane().add(p, BorderLayout.SOUTH);

        dialog.setResizable(true);
        dialog.pack();

        placeDialog(dialog);
        okBut.requestFocus();
        dialog.addWindowListener(getWindowDisposer());
        dialog.setVisible(true);
    }    

    public static File selectFile(Component parent, String title, boolean save) {
        return selectFile(parent, title, null, null, save);
    }

    public static File selectFile(Component parent, String title, String cwd, boolean save) {
        return selectFile(parent, title, cwd, null, save);
    }

    public static File selectFile(Component parent, String title, String cwd,
                                  String deffile, boolean save) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(title);
        if (cwd != null && cwd.length() > 0) {
            fc.setCurrentDirectory(new File(cwd));
        }
        if (deffile != null && deffile.length() > 0) {
            fc.setSelectedFile(new File(deffile));
        }
        fc.setDialogType(save ?
                         JFileChooser.SAVE_DIALOG : JFileChooser.OPEN_DIALOG);
        int ret = fc.showOpenDialog(parent);
        return (ret == JFileChooser.APPROVE_OPTION) ?
            fc.getSelectedFile() : null;
    }

    private static String textInput;
    public static String textInput(String title, String message,
                                   Component parent, char echo,
                                   String defaultValue, String prompt) {

        final JTextField textTxtInp;
        final JDialog dialog = newBorderJDialog(parent, title, true);

        if (message != null && message.trim().length() > 0)
            dialog.getContentPane().add(
                new JLabel(message), BorderLayout.NORTH);

        dialog.getContentPane().add(new JLabel(prompt), BorderLayout.WEST);

        if (echo > (char)0) {
            JPasswordField pwd = new JPasswordField();
            pwd.setEchoChar(echo);
            textTxtInp = pwd;
        } else {
            textTxtInp = new JTextField();
        }
        textTxtInp.setText(defaultValue);
        textTxtInp.setColumns(30);
        dialog.getContentPane().add(textTxtInp, BorderLayout.CENTER);

        JButton okBut = new JButton("OK");
        ActionListener al;
        okBut.addActionListener(al = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(e.getActionCommand().equals("OK")) {
                    if (textTxtInp instanceof JPasswordField) {
                        textInput = new String(((JPasswordField)textTxtInp).getPassword());
                    } else {
                        textInput = textTxtInp.getText();
                    }
                } else {
                    textInput = null;
                }
                dialog.dispose();
            }
        });
        JButton cancBut = new JButton("Cancel");
        cancBut.addActionListener(al);
        textTxtInp.addActionListener(al);
        textTxtInp.setActionCommand("OK");
        
        JPanel bp = newButtonPanel(new JButton[] { okBut, cancBut });
        dialog.getContentPane().add(bp, BorderLayout.SOUTH);

        dialog.setResizable(false);
        dialog.pack();

        placeDialog(dialog);
        dialog.addWindowListener(getWindowDisposer());
        dialog.setVisible(true);

        return textInput;
    }

    private static boolean arrayequals(char a[], char b[]) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a.length != b.length) return false;
        for (int i=0; i<a.length; i++)
            if (a[i] != b[i]) return false;
        return true;
    }

    private static String setPwdAnswer;
    public static String setPassword(String title, String message,
                                     Component parent) {
        final JPasswordField setPwdText, setPwdText2;
        final JDialog dialog = newBorderJDialog(parent, title, true);

        dialog.getContentPane().add(new JLabel(message), BorderLayout.NORTH);

        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.fill      = GridBagConstraints.NONE; 
        gbc.insets    = new Insets(10, 4, 2, 4);
        gbc.anchor    = GridBagConstraints.EAST;;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.weightx   = 0.0;
        p.add(new JLabel("Password"), gbc);
        
        setPwdText = new JPasswordField("", 12);
        setPwdText.setEchoChar('*');
        gbc.anchor    = GridBagConstraints.WEST;
        gbc.fill      = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx   = 1.0;
        p.add(setPwdText, gbc);

        gbc.fill      = GridBagConstraints.NONE; 
        gbc.insets    = new Insets(2, 4, 4, 4);
        gbc.anchor    = GridBagConstraints.EAST;;
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.weightx   = 0.0;
        p.add(new JLabel("Password again"), gbc);

        setPwdText2 = new JPasswordField("", 12);
        setPwdText2.setEchoChar('*');
        gbc.anchor    = GridBagConstraints.WEST;
        gbc.fill      = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx   = 1.0;
        p.add(setPwdText2, gbc);

        dialog.getContentPane().add(p, BorderLayout.CENTER);

        JButton okBut = new JButton("OK");
        ActionListener al;
        okBut.addActionListener(al = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals("OK")) {
                    char[] a1 = setPwdText.getPassword();
                    char[] a2 = setPwdText2.getPassword();
                    if (arrayequals(a1, a2)) {
                        setPwdText.setText("");
                        setPwdText2.setText("");
                        setPwdText.requestFocus();
                        return;
                    }
                    setPwdAnswer = new String(a1);
                } else {
                    setPwdAnswer = null;
                }
                dialog.dispose();
            }
        });

        JButton cancBut = new JButton("Cancel");
        cancBut.addActionListener(al);

        JPanel bp = newButtonPanel(new JButton[] { okBut, cancBut });
        dialog.getContentPane().add(bp, BorderLayout.SOUTH);

        dialog.setResizable(false);
        dialog.pack();

        placeDialog(dialog);
        dialog.addWindowListener(getWindowDisposer());
        dialog.setVisible(true);

        return setPwdAnswer;
    }

    public static String[] getFontList() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().
            getAvailableFontFamilyNames();
    }

    public final static void placeDialog(Container c) {
        Dimension sDim = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension mDim = c.getSize();
        int x, y;
        x = ((sDim.width / 2) - (mDim.width / 2));
        y = ((sDim.height / 2) - (mDim.height / 2));
        c.setLocation(x, y);
    }

    public static Frame getFrame(Component c) {
        Component root = SwingUtilities.getRoot(c);
        if (root instanceof Frame) {
            return (Frame)root;
        }
        return null;
    }

    public static JPanel newButtonPanel(JComponent[] b) {
        JPanel p1 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JPanel p2 = new JPanel(new GridLayout(1, 0, 5, 0));
        p1.add(p2);
        for (int i=0; i<b.length; i++)
            p2.add(b[i]);
        return p1;
    }

    public static JDialog newBorderJDialog(Component parent, String title,
                                           boolean modal) {
        JDialog d = newJDialog(parent, title, modal);
        if (d.getContentPane() instanceof JComponent) {
            JComponent c = (JComponent)d.getContentPane();
            c.setLayout(new BorderLayout(10, 10));
            c.setBorder(new EmptyBorder(5, 5, 5, 5));
        }
        return d;
    }

    public static JDialog newJDialog(Component parent, String title,
                                     boolean modal) {
        return new JDialog(getFrame(parent), title, modal);
    }

    public static WindowAdapter disposer = null;
    public static WindowAdapter getWindowDisposer() {
        if (disposer == null) {
            disposer = new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    e.getWindow().dispose();
                }
            };
        }
        return disposer;
    }

    public final static JFrame newJComponentWithMenuBar() {
        JFrame frame = new JFrame();
        frame.setJMenuBar(new JMenuBar());
        frame.validate();
        return frame;
    }

    public static class CloseAction implements ActionListener {
        Dialog dialog;
        public CloseAction(Dialog dialog) {
            this.dialog = dialog;
        }
        public void actionPerformed(ActionEvent e) {
            dialog.setVisible(false);
            dialog.dispose();
        }
    }

    public static class CloseAdapter extends WindowAdapter {
        Object source;
        String action;
        public CloseAdapter(MenuItem mi) {
            this(mi, mi.getLabel());
        }
        public CloseAdapter(Button b) {
            this(b, b.getActionCommand());
        }
        private CloseAdapter(Object source, String action) {
            this.source = source;
            this.action = action;
        }
        public void windowClosing(WindowEvent e) {
            if(source instanceof Component) {
                ((Component)source).dispatchEvent(new ActionEvent(source,
                                                  ActionEvent.ACTION_PERFORMED,
                                                  action));
            } else if(source instanceof MenuComponent) {
                ((MenuComponent)source).dispatchEvent(new ActionEvent(source,
                                                      ActionEvent.ACTION_PERFORMED,
                                                      action));
            }
        }
    }

    private static String lookAndFeel = null;
    public static void setLookAndFeel() {
        try {
            if (lookAndFeel != null) return; // already set, so don't reset it
            if (System.getProperty("swing.defaultlaf") == null) {
                String laf = null;

                String os = System.getProperty("os.name").toLowerCase();
                boolean mac = (os != null && os.equals("mac os x"));
                boolean unix = !os.startsWith("win") && "/".equals(File.separator);

                if (!mac) {
                    try {
                        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels())
                            if ("Nimbus".equalsIgnoreCase(info.getName())) 
                                laf = info.getClassName();
                    } catch (Throwable t) {
                    }
                }

                if (laf == null) {
                    if (unix && !mac) {
                        laf = UIManager.getCrossPlatformLookAndFeelClassName();
                    } else {
                        laf = UIManager.getSystemLookAndFeelClassName();
                    }

                }

                final String laff = laf;
                lookAndFeel = laf;
                if (SwingUtilities.isEventDispatchThread()) {
                    UIManager.setLookAndFeel(laff);
                } else {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() {
                            try {
                                UIManager.setLookAndFeel(laff);
                            } catch (Exception e) { }
                        }
                    });
                }
            }
        } catch (Exception e) { }
    }

}
