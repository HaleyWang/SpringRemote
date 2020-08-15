package com.haleywang.putty.view.side.subview;

import com.haleywang.putty.dto.AccountDto;
import com.haleywang.putty.util.StringUtils;
import com.haleywang.putty.view.constraints.MyGridBagConstraints;
import com.haleywang.putty.view.side.SideView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * @author haley
 * @date 2020/2/2
 */
public class AccountPasswordPanel extends JPanel {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountPasswordPanel.class);
    private static final String FOR_GROUP = "For group: ";

    private final JLabel connectGroupLabel;
    private final JPasswordField passwordField;
    private JTextField accountField;


    public AccountPasswordPanel() {
        setLayout(new BorderLayout());
        JPanel updatePasswordPanel = new JPanel();
        updatePasswordPanel.setBorder(new LineBorder(Color.LIGHT_GRAY));
        this.setBorder(new EmptyBorder(2, 2, 2, 2));
        this.add(updatePasswordPanel);
        MyGridBagConstraints cs = new MyGridBagConstraints();
        cs.insets = new Insets(6, 6, 6, 6);
        updatePasswordPanel.setLayout(new GridBagLayout());

        accountField = new JTextField(null, null, 20);
        passwordField = new JPasswordField(null, null, 20);

        connectGroupLabel = new JLabel(FOR_GROUP);
        connectGroupLabel.setSize(200, 30);

        cs.ofGridx(0).ofGridy(0).ofWeightx(1);
        updatePasswordPanel.add(connectGroupLabel, cs);

        cs.ofGridx(0).ofGridy(1).ofWeightx(1);
        updatePasswordPanel.add(connectGroupLabel, cs);

        cs.ofGridx(0).ofGridy(2).ofWeightx(1);
        updatePasswordPanel.add(new JLabel("Account:"), cs);

        cs.ofGridx(0).ofGridy(3).ofWeightx(1);
        updatePasswordPanel.add(accountField, cs);

        cs.ofGridx(0).ofGridy(4).ofWeightx(1);
        updatePasswordPanel.add(new JLabel("Password:"), cs);

        cs.ofGridx(0).ofGridy(5).ofWeightx(1);
        updatePasswordPanel.add(passwordField, cs);

        JButton updatePasswordBtn = new JButton("OK");
        cs.ofGridx(0).ofGridy(6).ofWeightx(1);
        updatePasswordPanel.add(updatePasswordBtn, cs);
        updatePasswordBtn.addActionListener(e ->
        {
            LOGGER.info("saveConnectionPassword");
            SideView.getInstance().saveConnectionPassword();
        });
    }


    public void changePasswordToConnectGroupLabel(DefaultMutableTreeNode node) {
        if (connectGroupLabel != null) {
            TreeNode groupNode = node.isLeaf() ? node.getParent() : node;

            AccountDto dto = SideView.getInstance().getConnectionAccountByNodeName(groupNode.toString());
            if (dto != null) {
                passwordField.setText(dto.getPassword());
                accountField.setText(dto.getName());
            } else {
                accountField.setText("");
                passwordField.setText("");
            }

            String nodeName = groupNode.toString();
            connectGroupLabel.setText(FOR_GROUP + StringUtils.ifBlank(nodeName, ""));

        }

    }

    public JPasswordField getPasswordField() {
        return passwordField;
    }

    public String getNodeName() {
        return connectGroupLabel.getText().split(FOR_GROUP)[1];
    }

    public JTextField getAccountField() {
        return accountField;
    }
}
