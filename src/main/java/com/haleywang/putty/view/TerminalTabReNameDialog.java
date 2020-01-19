package com.haleywang.putty.view;

import com.haleywang.putty.util.StringUtils;
import com.haleywang.putty.view.constraints.GridConstraints;
import com.haleywang.putty.view.constraints.MyGridBagConstraints;
import com.intellij.util.ArrayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagLayout;


/**
 * @author haley
 */
public class TerminalTabReNameDialog extends JDialog {

    private static final Logger LOGGER = LoggerFactory.getLogger(TerminalTabReNameDialog.class);


    private static final GridConstraints GC_LB_NAME = new GridConstraints().ofGridx(0).ofGridy(0).ofGridwidth(1);
    private static final GridConstraints GC_TF_NAME = new GridConstraints().ofGridx(1).ofGridy(0).ofGridwidth(2);

    private JTextField tfName;
    private JPanel tabNamePanel;

    public TerminalTabReNameDialog(SpringRemoteView omegaRemote, JPanel tabNamePanel) {
        super(omegaRemote, "Rename Session", true);
        this.tabNamePanel = tabNamePanel;
        //
        JPanel panel = new JPanel(new GridBagLayout());
        MyGridBagConstraints cs = new MyGridBagConstraints();

        JLabel lbName = new JLabel("New name: ");
        lbName.setBorder(new EmptyBorder(0, 5, 0, 5));

        tfName = new JTextField(20);

        panel.add(lbName, cs.of(GC_LB_NAME));
        panel.add(tfName, cs.of(GC_TF_NAME));


        panel.setBorder(new LineBorder(Color.GRAY));

        JButton btnOk = new JButton("Ok");

        btnOk.addActionListener(e -> doChangeName());

        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(e -> dispose());

        JPanel bp = new JPanel();
        bp.add(btnOk);
        bp.add(btnCancel);

        getContentPane().add(panel, BorderLayout.CENTER);
        getContentPane().add(bp, BorderLayout.PAGE_END);

        pack();
        setResizable(false);
        setLocationRelativeTo(omegaRemote);
    }


    private void doChangeName() {

        Component[] comps = tabNamePanel.getComponents();
        if (!ArrayUtil.isEmpty(comps)) {
            for (Component comp : comps) {
                if (comp instanceof JLabel && !StringUtils.isBlank(tfName.getText())) {
                    ((JLabel) comp).setText(tfName.getText());

                }
            }
        } else {
            LOGGER.warn("Unknown parameter: tabNamePanel");
        }
        dispose();

    }


}