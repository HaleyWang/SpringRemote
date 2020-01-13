package com.haleywang.putty.view;

import com.haleywang.putty.dto.Status;
import com.haleywang.putty.service.LoginService;
import com.haleywang.putty.storage.FileStorage;
import com.haleywang.putty.util.AesUtil;
import com.haleywang.putty.util.StringUtils;
import com.haleywang.putty.view.constraints.GridConstraints;
import com.haleywang.putty.view.constraints.MyGridBagConstraints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;


/**
 * @author haley
 */
public class LoginDialog extends JDialog {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginDialog.class);

    private static final String LOGIN = "Login";
    private static final String USERNAME = "Username";
    private static final String PW_TEXT = "Password";
    private static final String REGISTER = "Register";
    private static final String CANCEL = "Cancel";

    private static final GridConstraints GC_LB_USERNAME = new GridConstraints().ofGridx(0).ofGridy(0).ofGridwidth(1);
    private static final GridConstraints GC_TF_USERNAME = new GridConstraints().ofGridx(1).ofGridy(0).ofGridwidth(2);
    private static final GridConstraints GC_LB_PASSWORD = new GridConstraints().ofGridx(0).ofGridy(1).ofGridwidth(1);
    private static final GridConstraints GC_TF_PASSWORD = new GridConstraints().ofGridx(1).ofGridy(1).ofGridwidth(2);

    private JTextField tfUsername;
    private JPasswordField pfPassword;
    private boolean succeeded;
    private SpringRemoteView omegaRemote;

    public LoginDialog(SpringRemoteView omegaRemote) {
        super(omegaRemote, LOGIN, true);
        this.omegaRemote = omegaRemote;
        //
        JPanel panel = new JPanel(new GridBagLayout());
        MyGridBagConstraints cs = new MyGridBagConstraints();

        JLabel lbUsername = new JLabel(USERNAME + ": ");
        lbUsername.setBorder(new EmptyBorder(0, 5, 0, 5));


        tfUsername = new JTextField(20);


        tfUsername.setText(StringUtils.trim(FileStorage.INSTANCE.getAccount()));

        JLabel lbPassword = new JLabel(PW_TEXT + ": ");
        lbPassword.setBorder(new EmptyBorder(0, 5, 0, 5));


        pfPassword = new JPasswordField(20);

        panel.add(lbUsername, cs.of(GC_LB_USERNAME));
        panel.add(tfUsername, cs.of(GC_TF_USERNAME));
        panel.add(lbPassword, cs.of(GC_LB_PASSWORD));
        panel.add(pfPassword, cs.of(GC_TF_PASSWORD));

        panel.setBorder(new LineBorder(Color.GRAY));

        JButton btnLogin = new JButton(LOGIN);
        JButton btnRegister = new JButton(REGISTER);

        btnLogin.addActionListener(e -> doLogin());

        btnRegister.addActionListener(e -> doRegister());


        JButton btnCancel = new JButton(CANCEL);
        btnCancel.addActionListener(e -> {
            dispose();
            System.exit(0);
        });

        pfPassword.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {
                LOGGER.debug("pfPassword keyTyped");
            }

            @Override
            public void keyPressed(KeyEvent e) {
                int entryKeyCode = 10;
                if (e.getKeyCode() == entryKeyCode) {
                    doLogin();
                }

            }

            @Override
            public void keyReleased(KeyEvent e) {
                LOGGER.debug("pfPassword keyReleased");
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
        Status status = null;
        try {
            status = LoginService.getInstance().register(getUsername(), getPassword());
        } catch (IllegalArgumentException e) {
            status = Status.fail(e.getMessage());
        }

        if (status.isSuccess()) {
            succeeded = true;
            omegaRemote.afterLogin(getUsername(), AesUtil.generateKey(getPassword()));
            FileStorage.INSTANCE.saveAccount(getUsername());

            dispose();
        } else {
            JOptionPane.showMessageDialog(LoginDialog.this,
                    status.getMsg(),
                    REGISTER,
                    JOptionPane.ERROR_MESSAGE);

            succeeded = false;
        }
    }

    private void doLogin() {
        Status status = null;
        try {
            status = LoginService.getInstance().authenticate(getUsername(), getPassword());
        } catch (IllegalArgumentException e) {
            status = Status.fail(e.getMessage());
        }
        if (status.isSuccess()) {
            succeeded = true;
            omegaRemote.afterLogin(getUsername(), AesUtil.generateKey(getPassword()));
            dispose();
            FileStorage.INSTANCE.saveAccount(getUsername());
        } else {
            JOptionPane.showMessageDialog(LoginDialog.this,
                    status.getMsg(),
                    LOGIN,
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