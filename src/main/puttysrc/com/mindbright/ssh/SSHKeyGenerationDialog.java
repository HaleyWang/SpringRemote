/******************************************************************************
 *
 * Copyright (c) 1999-2011 Cryptzone Group AB. All Rights Reserved.
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

package com.mindbright.ssh;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;

import java.awt.event.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

import java.security.interfaces.RSAPrivateCrtKey;

import java.util.HashSet;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import com.mindbright.gui.AWTGridBagContainer;
import com.mindbright.gui.GUI;

import com.mindbright.ssh2.SSH2AccessDeniedException;
import com.mindbright.ssh2.SSH2Exception;
import com.mindbright.ssh2.SSH2KeyPairFile;
import com.mindbright.ssh2.SSH2PublicKeyFile;

import com.mindbright.terminal.TerminalWin;
import com.mindbright.terminal.TerminalWindow;

import com.mindbright.util.Crypto;
import com.mindbright.util.Progress;

public class SSHKeyGenerationDialog  {

    private final static String EDIT_TITLE = "MindTerm - Select key file to edit";
    private final static String PASS_TITLE = "MindTerm - File Password";
    
    private final static String KEY_TYPES[] = {
        "DSA (ssh2)", "RSA (ssh2)", "RSA (ssh1)" 
    };
    private final static String KEY_TYPES_WITH_ECDSA[] = {
        "DSA (ssh2)", "RSA (ssh2)", "ECDSA (ssh2)", "RSA (ssh1)" 
    };
    private final static String KEY_LENGTHS_RSA[] = {
        "768", "1024", "1536", "2048", "4096", "8192", "16384", "32768"
    };
    private final static String KEY_LENGTHS_DSA[] = {
        "1024"
    };
    private final static String KEY_LENGTHS_ECDSA[] = {
        "256", "384", "521"
    };
    
    private final static String LBL_BTN_GENERATE = "Generate";
    private final static String LBL_BTN_CLOSE    = "Close";
    private final static String LBL_BTN_BACK     = "Back";
    private final static String LBL_BTN_SAVE     = "Save";
    private final static String LBL_BTN_CANCEL   = "Cancel";

    private final static String LBL_KEY_TYPE   = "Key type/format";
    private final static String LBL_KEY_LENGTH = "Key length (bits)";
    private final static String LBL_IDENTITY   = "Identity file";
    private final static String LBL_DOT_DOT_DOT = "...";
    private final static String LBL_PASSWORD   = "Password";
    private final static String LBL_PASSWORD_AGAIN = "Password again";
    private final static String LBL_COMMENT    = "Comment";
    private final static String LBL_SUBJECT    = "Subject";
    private final static String LBL_OPENSSH    = "OpenSSH .pub format";
    private final static String LBL_DSA        = "DSA";
    private final static String LBL_1024       = "1024";
    private final static String LBL_SSHCOM     = "SSH Comm. private file format";

    private final static String TEXT_GENERATING = "Generating keypair, please wait...";

    private final static String keyGenerationHelp =
        "This will create a publickey identity which can be used with the " +
        "publickey authentication method. Your identity will consist of two " +
        "parts: public and private keys. Your private key will be saved " +
        "in the location which you specify; the corresponding public key " +
        "is saved in a file with an identical name but with an extension of " +
        "'.pub' added to it.\n" +
        "\n" +
        "Your private key is private by encryption, if you entered a " +
        "password. If you left the password field blank, the key will " +
        "not be encrypted. This should only be used in private " +
        "environments where unattended logins are desired. The contents " +
        "of the 'comment' field are stored with your key, and displayed " +
        "each time you are prompted for the key's password.";

    private final static String keyGenerationComplete =
        "Key Generation Complete\n\n" +
        "To use the key, you must transfer the '.pub' public key " +
        "file to an SSH server and add it to the set of authorized keys. " +
        "See your server documentation for details on this.\n\n" +
        "For convenience, your public key has been copied to the clipboard.\n\n" +
        "Examples:\n" +
        "In ssh2 the '.pub' file should be pointed out in the file " +
        "'authorization' in the config directory (e.g. ~/.ssh2)\n\n" +
        "In OpenSSH's ssh2 the contents of the '.pub' file should be added " +
        "to the file 'authorized_keys' in your config directory (e.g. ~/.ssh) " +
        "on the server.\n\n" +
        "In ssh1 the contents of the '.pub' file should be added to the " +
        "file 'authorized_keys' in your ssh directory (e.g. ~/.ssh).\n\n" +
        "Press 'Back' to generate a new keypair.";

    private static SSHInteractiveClient client;
    private static Component            parent;
    private static JComboBox    comboBits, comboType;
    private static JTextField   fileText;
    private static JPasswordField pwdText, pwdText2;
    private static JTextField  commText;
    private static JTextArea   descText;
    private static JButton     okBut;
    private static JCheckBox   cbOpenSSH;
    private static JPanel      cardPanel;
    private static CardLayout  cardLayout;
    private static int         generateStatus;

    private static JTextField  fileTextEd;
    private static JPasswordField  pwdTextEd, pwdText2Ed;
    private static JTextField  subjTextEd;
    private static JTextField  commTextEd;
    private static JLabel      typeLbl;
    private static JLabel      bitLbl;
    private static JCheckBox   cbOpenSSHEd;
    private static JCheckBox   cbSSHComEd;
    private static JButton     okButEd;
    private static JButton     cancButEd;

    private static SSH2KeyPairFile   kpf;
    private static SSH2PublicKeyFile pkf;

    private static File getSaveFile() {
        return GUI.selectFile(parent, "MindTerm - Select file to save identity to",
                              client.propsHandler.getSSHHomeDir(), true);
    }

    private static void alert(String msg) {
        SSHMiscDialogs.alert("MindTerm - Alert", msg, parent);
    }

    private static String getDefaultFileName() {
        try {
            String fn = client.propsHandler.getSSHHomeDir() + SSHPropertyHandler.DEF_IDFILE;
            File   f  = new File(fn);
            int    fi = 0;
            while(f.exists()) {
                fn = client.propsHandler.getSSHHomeDir() + SSHPropertyHandler.DEF_IDFILE + fi;
                f  = new File(fn);
                fi++;
            }
            fi--;
            return SSHPropertyHandler.DEF_IDFILE + (fi >= 0 ? String.valueOf(fi) : "");
        } catch (Throwable t) {
            // !!!
            // Don't care...
        }
        return "";
    }

    public static KeyPair generateKeyPair(String alg, int bits)
        throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = Crypto.getKeyPairGenerator(alg);
        kpg.initialize(bits, client.secureRandom());
        return kpg.generateKeyPair();
    }

    private static void saveKeyPair(KeyPair kp, String passwd, String fileName, 
                                      String comment, String type, boolean openssh)
        throws IOException, SSH2Exception {
        String subject   = client.propsHandler.getProperty("usrname");
        String pubKeyStr = null;

        if (subject == null) {
            subject = SSH.VER_MINDTERM;
        }

        if ("RSA (ssh1)".equals(type)) {
            RSAPrivateCrtKey key = (RSAPrivateCrtKey)kp.getPrivate();
            SSHRSAKeyFile.createKeyFile(client, key, 
                                        passwd, expandFileName(fileName), comment);
            SSHRSAPublicKeyString pks =
                new SSHRSAPublicKeyString("", comment,
                                          key.getPublicExponent(), key.getModulus());        
            pks.toFile(expandFileName(fileName) + ".pub");
            pubKeyStr = pks.toString();
        } else {
            SSH2PublicKeyFile pkif = new SSH2PublicKeyFile(kp.getPublic(),
                                                           subject, comment);

            // When key is unencrypted OpenSSH doesn't tolerate headers...
            //
            if(passwd == null || passwd.length() == 0) {
                subject = null;
                comment = null;
            }

            SSH2KeyPairFile kpf = new SSH2KeyPairFile(kp, subject, comment);

            kpf.store(expandFileName(fileName), client.secureRandom(), passwd);

            pubKeyStr = pkif.store(expandFileName(fileName + ".pub"), !openssh);
        }

        TerminalWindow term = client.sshStdIO.getTerminal();
        if (term instanceof TerminalWin) {
            ((TerminalWin)term).getClipboard().setSelection(pubKeyStr);
        }
    }

    private static boolean checkValues(String passwd, String passwd2, 
                                         String fileName) {
        if (!passwd.equals(passwd2)) {
            alert("Please give same password twice");
            return false;
        }
        if (fileName.length() == 0) {
            alert("Filename can't be empty");
            return false;
        }

        OutputStream out = getOutput(fileName);
        if (out == null) {
            alert("Can't open '" + fileName + "' for saving.");
            return false;
        }
        try {
            out.close();
			File f = new File(expandFileName(fileName));
			f.delete();
        } catch (Exception e) { /* don't care */
        }
        try {
            out.close();
        } catch (Exception e) { /* don't care */
        }

        return true;
    }

    private static OutputStream getOutput(String fileName) {
        try {
            return new FileOutputStream(expandFileName(fileName));
        } catch (Throwable t) {
        }
        return null;
    }

    private static String expandFileName(String fileName) {
        if (fileName.indexOf(File.separator) == -1)
            fileName = client.propsHandler.getSSHHomeDir() + fileName;
        return fileName;
    }

    public static void show(String title, Component par,
                            SSHInteractiveClient cli){
        client = cli;
        parent = par;

        final JDialog dialog = GUI.newBorderJDialog(parent, title, true);

        createCardPanel();
        dialog.getContentPane().add(cardPanel, BorderLayout.CENTER);

        okBut = new JButton(LBL_BTN_GENERATE);
        okBut.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(generateStatus == 2) {
                    resetValues();
                    descText.setCaretPosition(0);
				} else if (generateStatus == 1) {
                    try {
                        final int bits = Integer.valueOf
                            ((String)comboBits.getSelectedItem()).intValue();
						String type = (String)comboType.getSelectedItem();
						type = type.substring(0, type.indexOf(' '));
						if (type.equals("ECDSA"))
							type = "EC";
                        final String alg = type;

                        descText.setText(TEXT_GENERATING);
                        descText.setCaretPosition(0);

						generateStatus = -1;

                        Thread t = new Thread(new Runnable() {
                            public void run() {
                                try {
                                    KeyPair kp = generateKeyPair(alg, bits);
                                    saveKeyPair(kp);
                                    okBut.setText(LBL_BTN_BACK);
                                    descText.setText(keyGenerationComplete);
                                    descText.setCaretPosition(0);
                                    generateStatus = 2;
                                } catch (Throwable tt) {
                                    alert("Error while generating/saving key pair: " +
                                          tt.getMessage());
                                    cardLayout.show(cardPanel, "first");
									generateStatus = 0;
                                }
                            }
                        });
                        t.start();
						
                    } catch (Throwable tt) {
                        alert("Error while generating/saving key pair: " +
                              tt.getMessage());
                        cardLayout.show(cardPanel, "first");
						generateStatus = 0;
                    }
				} else if (generateStatus == 0) {
                    if(checkValues(new String(pwdText.getPassword()),
                                   new String(pwdText2.getPassword()),
                                   fileText.getText())) {
                        cardLayout.show(cardPanel, "second");
						generateStatus = 1;
                    }
                }
            }
        });
        JButton closeBut = new JButton(LBL_BTN_CLOSE);
        closeBut.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
                resetValues();
            }
        });

        dialog.getContentPane().add(
            GUI.newButtonPanel(new JButton[] { okBut, closeBut }),
            BorderLayout.SOUTH);

        resetValues();
        descText.setCaretPosition(0);
        
        dialog.setResizable(false);
        dialog.pack();

        GUI.placeDialog(dialog);
        comboBits.requestFocus();
        dialog.addWindowListener(GUI.getWindowDisposer());
        dialog.setVisible(true);
    }

    private static void createCardPanel() {
        cardPanel  = new JPanel();
        cardLayout = new CardLayout();
        cardPanel.setLayout(cardLayout);

        JPanel p = new JPanel(new BorderLayout(10, 10));

        descText = new JTextArea(keyGenerationHelp, 12, 34);
        descText.setEditable(false);
        descText.setLineWrap(true);
        descText.setWrapStyleWord(true);
        JScrollPane sp = new JScrollPane
            (descText, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
             ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        p.add(sp, BorderLayout.CENTER);

        cardPanel.add(p, "second");

        p = new JPanel();
        AWTGridBagContainer grid = new AWTGridBagContainer(p);
        GridBagConstraints gbc = grid.getConstraints();

        gbc.anchor = GridBagConstraints.EAST;
        grid.add(new JLabel(LBL_KEY_TYPE, JLabel.RIGHT), 0, 2);
        comboType = new JComboBox
	    (Crypto.hasECDSASupport() ? KEY_TYPES_WITH_ECDSA : KEY_TYPES);
        gbc.anchor = GridBagConstraints.WEST;
        grid.add(comboType, 0, 2);
        
        gbc.anchor = GridBagConstraints.EAST;
        grid.add(new JLabel(LBL_KEY_LENGTH, JLabel.RIGHT), 1, 2);
        ComboHandler cbh = new ComboHandler();
        comboBits = new JComboBox(cbh);
        gbc.anchor = GridBagConstraints.WEST;
        grid.add(comboBits, 1, 2);
        comboType.addActionListener(cbh);

        gbc.anchor = GridBagConstraints.EAST;
        grid.add(new JLabel(LBL_IDENTITY, JLabel.RIGHT), 2, 2);
        fileText = new JTextField("", 18);
        gbc.anchor = GridBagConstraints.WEST;
        grid.add(fileText, 2, 2);

        JButton b = new JButton(LBL_DOT_DOT_DOT);
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                File f = getSaveFile();
                if (f != null)
                    fileText.setText(f.getAbsolutePath());
            }
        });
        gbc.fill = GridBagConstraints.NONE;
        grid.add(b, 2, 1);

        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.anchor = GridBagConstraints.EAST;
        grid.add(new JLabel(LBL_PASSWORD, JLabel.RIGHT), 3, 2);
        pwdText = new JPasswordField("", 18);
        pwdText.setEchoChar('*');
        gbc.anchor = GridBagConstraints.WEST;
        grid.add(pwdText, 3, 2);

        gbc.anchor = GridBagConstraints.EAST;
        grid.add(new JLabel(LBL_PASSWORD_AGAIN, JLabel.RIGHT), 4, 2);
        pwdText2 = new JPasswordField("", 18);
        pwdText2.setEchoChar('*');
        gbc.anchor = GridBagConstraints.WEST;
        grid.add(pwdText2, 4, 2);

        gbc.anchor = GridBagConstraints.EAST;
        grid.add(new JLabel(LBL_COMMENT, JLabel.RIGHT), 5, 2);
        commText = new JTextField("", 18);
        gbc.anchor = GridBagConstraints.WEST;
        grid.add(commText, 5, 2);

        cbOpenSSH = new JCheckBox(LBL_OPENSSH);
        grid.add(cbOpenSSH, 6, 4);
	cbOpenSSH.setSelected(true);

        cardPanel.add(p, "first");
    }

    ///////////////////////////////////////////////
    ///////// ComboBox update / model handling

    private static class ComboHandler implements ActionListener, ComboBoxModel {
        private String[] list = KEY_LENGTHS_DSA;
        private int idx = 0;
        private HashSet<ListDataListener> listeners = 
	    new HashSet<ListDataListener>();
        
        public void actionPerformed(ActionEvent e) {
            JComboBox cb = (JComboBox)e.getSource();
            String type = (String)cb.getSelectedItem();
            if (type.indexOf("RSA") != -1) {
                list = KEY_LENGTHS_RSA;
            } else if (type.indexOf("ECDSA") != -1) {
                list = KEY_LENGTHS_ECDSA;
            } else {
                list = KEY_LENGTHS_DSA;
            }
            idx = 0;
	    for (ListDataListener l : listeners)
		l.contentsChanged
		    (new ListDataEvent
		     (this, ListDataEvent.CONTENTS_CHANGED, 
		      0, list.length - 1));
						    
        }

        public Object getSelectedItem() {
            return list[idx];
        }
        
        public void setSelectedItem(Object o) {
            for (int i=0; i<list.length; i++)
                if (list[i].equals((String)o)) {
                    idx = i;
                    break;
                }
        }
        
        public void addListDataListener(ListDataListener l) {
	    listeners.add(l);
        }

        public void removeListDataListener(ListDataListener l) {
	    listeners.remove(l);
	}
        
	public int getSize() { return list.length; }

        public Object getElementAt(int index) {
            return list[index];
        }
    }
    
    
    ///////////
    ////////////////////////////////////////////
    
    private static void saveKeyPair(KeyPair kp)
        throws IOException, SSH2Exception {

        saveKeyPair(kp, new String(pwdText.getPassword()), fileText.getText(), 
                    commText.getText(),
                    (String)comboType.getSelectedItem(), cbOpenSSH.isSelected());  

        pwdText.setText("");
        pwdText2.setText("");
        fileText.setText(getDefaultFileName());
    }

    private static void resetValues() {
        comboType.setSelectedIndex(0);
        comboBits.setSelectedItem("1024");
        fileText.setText(getDefaultFileName());
        generateStatus = 0;
        pwdText.setText("");
        pwdText2.setText("");
        descText.setText(keyGenerationHelp);
        okBut.setText(LBL_BTN_GENERATE);
        cardLayout.show(cardPanel, "first");
    }

    public static void editKeyDialog(String title, Component par,
                                     SSHInteractiveClient cli) {
        parent = par;
        client = cli;

        File f = GUI.selectFile(parent, EDIT_TITLE, 
                                client.propsHandler.getSSHHomeDir(), null, false);

        String passwd   = null;
        String fileName = null;

        kpf = new SSH2KeyPairFile();

        if (f != null) {
            fileName = f.getAbsolutePath();
            try {
                pkf = new SSH2PublicKeyFile();
                pkf.load(fileName + ".pub");
            } catch (Exception e) {
                pkf = null;
            }
            boolean retryPasswd = false;
            do {
                try {
                    kpf.load(fileName, passwd);
                    break;
                } catch(SSH2AccessDeniedException e) {
                    /* Retry... */
                    retryPasswd = true;
                } catch(Exception e) {
                    alert("Error loading key file: " + e.getMessage());
                }
            } while((passwd = SSHMiscDialogs.password(PASS_TITLE,
                                                      "Please give password for " +
                                                      fileName,
                                                      parent)) != null);
            if(retryPasswd && passwd == null) {
                return;
            }
        } else {
            return;
        }

        if(pkf == null) {
            pkf = new SSH2PublicKeyFile(kpf.getKeyPair().getPublic(),
                                        kpf.getSubject(), kpf.getComment());
        }

        final JDialog dialog = GUI.newJDialog(parent, title, true);

        AWTGridBagContainer grid = new AWTGridBagContainer(dialog.getContentPane());
        GridBagConstraints gbc = grid.getConstraints();
        
        gbc.anchor = GridBagConstraints.EAST;
        grid.add(new JLabel(LBL_KEY_TYPE, JLabel.RIGHT), 0, 2);
        typeLbl = new JLabel(LBL_DSA);
        gbc.anchor = GridBagConstraints.WEST;
        grid.add(typeLbl, 0, 2);
        
        gbc.anchor = GridBagConstraints.EAST;
        grid.add(new JLabel(LBL_KEY_LENGTH, JLabel.RIGHT), 1, 2);        
        bitLbl = new JLabel(LBL_1024);
        gbc.anchor = GridBagConstraints.WEST;
        grid.add(bitLbl, 1, 2);
        
        gbc.anchor = GridBagConstraints.EAST;
        grid.add(new JLabel(LBL_IDENTITY, JLabel.RIGHT), 2, 2);        
        fileTextEd = new JTextField("", 18);
        gbc.anchor = GridBagConstraints.WEST;
        grid.add(fileTextEd, 2, 2);
        
        gbc.anchor = GridBagConstraints.EAST;
        grid.add(new JLabel(LBL_PASSWORD, JLabel.RIGHT), 3, 2);
        pwdTextEd = new JPasswordField("", 18);
        pwdTextEd.setEchoChar('*');
        gbc.anchor = GridBagConstraints.WEST;
        grid.add(pwdTextEd, 3, 2);
        
        gbc.anchor = GridBagConstraints.EAST;
        grid.add(new JLabel(LBL_PASSWORD_AGAIN, JLabel.RIGHT), 4, 2);        
        pwdText2Ed = new JPasswordField("", 18);
        pwdText2Ed.setEchoChar('*');
        gbc.anchor = GridBagConstraints.WEST;
        grid.add(pwdText2Ed, 4, 2);
        
        gbc.anchor = GridBagConstraints.EAST;
        grid.add(new JLabel(LBL_SUBJECT, JLabel.RIGHT), 5, 2);
        subjTextEd = new JTextField("", 18);
        gbc.anchor = GridBagConstraints.WEST;
        grid.add(subjTextEd, 5, 2);
        
        gbc.anchor = GridBagConstraints.EAST;
        grid.add(new JLabel(LBL_COMMENT, JLabel.RIGHT), 6, 2);
        commTextEd = new JTextField("", 18);
        gbc.anchor = GridBagConstraints.WEST;
        grid.add(commTextEd, 6, 2);

        cbSSHComEd = new JCheckBox(LBL_SSHCOM);
        grid.add(cbSSHComEd, 7, 4);

        cbOpenSSHEd = new JCheckBox(LBL_OPENSSH);
        grid.add(cbOpenSSHEd, 8, 4);
        
        okButEd = new JButton(LBL_BTN_SAVE);
        okButEd.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String fName = fileTextEd.getText();
                String pwd   = new String(pwdTextEd.getPassword());
                if(checkValues(pwd, new String(pwdText2Ed.getPassword()), fName)) {
                    fName = expandFileName(fName);
                    try {
                        String s = subjTextEd.getText();
                        String c = commTextEd.getText();
                        pkf.setSubject(s);
                        pkf.setComment(c);
                        pkf.store(fName + ".pub", !cbOpenSSHEd.isSelected());
                        if(!cbSSHComEd.isSelected() && pwd.length() == 0) {
                            s = null;
                            c = null;
                        }
                        kpf.setSubject(s);
                        kpf.setComment(c);
                        kpf.store(fName, client.secureRandom(), pwd,
                                  cbSSHComEd.isSelected());
                        dialog.dispose();
                    } catch (Exception ee) {
                        alert("Error saving files: " + ee.getMessage());
                    }
                }
            }
        });

        cancButEd = new JButton(LBL_BTN_CANCEL);        
        cancButEd.addActionListener(new GUI.CloseAction(dialog));

        JPanel bp = GUI.newButtonPanel(new JButton[] { okButEd, cancButEd });
        grid.add(bp, 9, GridBagConstraints.REMAINDER);

        dialog.pack();

        fileTextEd.setText(fileName);
        pwdTextEd.setText(passwd);
        pwdText2Ed.setText(passwd);
        typeLbl.setText(kpf.getAlgorithmName());
        bitLbl.setText(String.valueOf(kpf.getBitLength()));
        subjTextEd.setText(kpf.getSubject());
        commTextEd.setText(kpf.getComment());
        cbSSHComEd.setSelected(kpf.isSSHComFormat());
        cbOpenSSHEd.setSelected(!pkf.isSSHComFormat());

        GUI.placeDialog(dialog);
        dialog.addWindowListener(GUI.getWindowDisposer());
        dialog.setVisible(true);
    }
}
