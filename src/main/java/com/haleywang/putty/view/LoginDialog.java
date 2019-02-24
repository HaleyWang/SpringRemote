package com.haleywang.putty.view;

import com.haleywang.putty.dto.Status;
import com.haleywang.putty.service.Login;
import com.haleywang.putty.storage.FileStorage;
import com.haleywang.putty.util.AESUtil;
import com.haleywang.putty.util.StringUtils;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
 
public class LoginDialog extends JDialog {

    private JTextField tfUsername;
    private JPasswordField pfPassword;
    private boolean succeeded;
    private SpringRemoteView omegaRemote;
 
    public LoginDialog(SpringRemoteView omegaRemote) {
        super(omegaRemote, "Login", true);
        this.omegaRemote = omegaRemote;
        //
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints cs = new GridBagConstraints();
 
        cs.fill = GridBagConstraints.HORIZONTAL;

        JLabel lbUsername = new JLabel("Username: ");
        cs.gridx = 0;
        cs.gridy = 0;
        cs.gridwidth = 1;
        panel.add(lbUsername, cs);
 
        tfUsername = new JTextField(20);
        cs.gridx = 1;
        cs.gridy = 0;
        cs.gridwidth = 2;
        panel.add(tfUsername, cs);
        tfUsername.setText(StringUtils.trim(FileStorage.INSTANCE.getAccount()));

        JLabel lbPassword = new JLabel("Password: ");
        cs.gridx = 0;
        cs.gridy = 1;
        cs.gridwidth = 1;
        panel.add(lbPassword, cs);
 
        pfPassword = new JPasswordField(20);
        cs.gridx = 1;
        cs.gridy = 1;
        cs.gridwidth = 2;
        panel.add(pfPassword, cs);
        panel.setBorder(new LineBorder(Color.GRAY));

        JButton btnLogin = new JButton("Login");
        JButton btnRegister = new JButton("Register");


        btnLogin.addActionListener(e -> doLogin());

        btnRegister.addActionListener(e -> doRegister());


        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(e -> {
            dispose();
            System.exit(0);
        });

        pfPassword.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                System.out.println(e);
                int entryKeyCode = 10;
                if(e.getKeyCode() == entryKeyCode) {
                    doLogin();
                }

            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });


        JPanel bp = new JPanel();
        bp.add(btnLogin);
        bp.add(btnRegister);
        bp.add(btnCancel);
 
        getContentPane().add(panel, BorderLayout.CENTER);
        getContentPane().add(bp, BorderLayout.PAGE_END);
 
        pack();
        setResizable(false);
        setLocationRelativeTo(omegaRemote);
    }

    private void doRegister() {
        Status status = Login.register(getUsername(), getPassword());

        if (status.isSuccess()) {
            succeeded = true;
            omegaRemote.afterLogin(AESUtil.generateKey(getPassword()));
            FileStorage.INSTANCE.saveAccount(getUsername());

            dispose();
        } else {
            JOptionPane.showMessageDialog(LoginDialog.this,
                    status.getMsg(),
                    "Register",
                    JOptionPane.ERROR_MESSAGE);

            succeeded = false;

        }
    }

    private void doLogin() {
        Status status = Login.authenticate(getUsername(), getPassword());
        if (status.isSuccess()) {
            succeeded = true;
            omegaRemote.afterLogin(AESUtil.generateKey(getPassword()));
            dispose();
            FileStorage.INSTANCE.saveAccount(getUsername());
        } else {
            JOptionPane.showMessageDialog(LoginDialog.this,
                    status.getMsg(),
                    "Login",
                    JOptionPane.ERROR_MESSAGE);
            succeeded = false;

        }
    }

    public String getUsername() {
        return tfUsername.getText().trim();
    }
 
    public String getPassword() {
        return new String(pfPassword.getPassword());
    }
 
    public boolean isSucceeded() {
        return succeeded;
    }
}