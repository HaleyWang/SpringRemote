package com.haleywang.putty.view;

import com.haleywang.putty.common.data.ActionsData;
import com.haleywang.putty.dto.Action;
import com.haleywang.putty.dto.ActionDto;
import com.haleywang.putty.dto.Status;
import com.haleywang.putty.service.ActionExecuteService;
import com.haleywang.putty.service.LoginService;
import com.haleywang.putty.service.action.ActionCategoryEnum;
import com.haleywang.putty.storage.FileStorage;
import com.haleywang.putty.util.AESUtil;
import com.haleywang.putty.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ActionsDialog extends JDialog {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActionsDialog.class);

    public static final String ACTIONS = "Actions";
    public static final List<Action> actionsData = new ArrayList<>();
    private final JTable table;
    private JTextField searchField;
    private SpringRemoteView omegaRemote;

    public ActionsDialog(SpringRemoteView omegaRemote) {
        super(omegaRemote, ACTIONS, true);
        this.omegaRemote = omegaRemote;
        //
        JPanel panel = new JPanel(new BorderLayout());

        searchField = new JTextField(20);
        panel.add(searchField);


        searchField.addKeyListener(new KeyListener() {

            @Override
            public void keyPressed(KeyEvent e) {
                LOGGER.info("pfPassword keyPressed:" + searchField.getText());
            }

            @Override
            public void keyReleased(KeyEvent e) {
                LOGGER.info("pfPassword keyReleased:" + searchField.getText());

                int entryKeyCode = 10;
                int index = table.getSelectedRow();

                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    doSelectAction(--index);
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    doSelectAction(++index);
                } else if (e.getKeyCode() == entryKeyCode) {
                    doAction(index);
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    ActionsDialog.this.dispose();
                } else {
                    doSearch();

                }

            }

            @Override
            public void keyTyped(KeyEvent e) {
                LOGGER.info("pfPassword keyTyped:" + searchField.getText());

                LOGGER.debug("pfPassword keyReleased");
            }
        });

        table = new JTable(new SimpleColorTableModel()) {
            private static final long serialVersionUID = 1L;

            public boolean isCellEditable(int row, int column) {
                return false;
            }

            ;
        };

        setupTable(table);

        doSearch();

        getContentPane().add(panel, BorderLayout.NORTH);
        getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);

        pack();
        setResizable(true);
        setLocationRelativeTo(omegaRemote);
    }

    private void doSearch() {

        SwingUtilities.invokeLater(() -> {
            String query = searchField.getText();
            actionsData.clear();

            List<Action> allActionData = new ArrayList<>();

            List<Action> staticData = ActionsData.getActionsData();
            List<Action> userData = new ArrayList<>();
            allActionData.addAll(staticData);
            DefaultMutableTreeNode commandsTreeNode = (DefaultMutableTreeNode) SideView.getInstance().getCommandsTreeView().getModel().getRoot();
            DefaultMutableTreeNode connectionsTreeNode = (DefaultMutableTreeNode) SideView.getInstance().getConnectionsInfoTreeView().getModel().getRoot();

            parseTreeNodes(userData, commandsTreeNode, ActionCategoryEnum.COMMAND);
            parseTreeNodes(userData, connectionsTreeNode, ActionCategoryEnum.SSH);
            allActionData.addAll(userData);

            System.out.println(allActionData);
            allActionData.stream().filter(o -> StringUtils.isBlank(query) || o.searchText().contains(query)).collect(Collectors.toList()).forEach(o -> {
                actionsData.add(o);
            });

            populate();

        });


    }


    void parseTreeNodes(List<Action> actions, DefaultMutableTreeNode data, ActionCategoryEnum category) {
        if (data.getChildCount() == 0) {
            return;
        }
        for (int i = 0, n = data.getChildCount(); i < n; i++) {
            TreeNode child = data.getChildAt(i);
            if (child instanceof DefaultMutableTreeNode) {

                DefaultMutableTreeNode treeChild = (DefaultMutableTreeNode) child;
                if (treeChild.isLeaf()) {
                    Object userObject = treeChild.getUserObject();

                    if (userObject instanceof Action) {
                        Action userAction = (Action) userObject;
                        if (!StringUtils.isBlank(userAction.getName())) {
                            actions.add(userAction);
                        }
                    }

                } else {
                    parseTreeNodes(actions, treeChild, category);
                }

            }
        }

    }


    private void doSelectAction(int index) {
        int moveTo = index;
        if (moveTo >= table.getRowCount()) {
            moveTo = 0;
        } else if (moveTo < 0) {
            moveTo = table.getRowCount() - 1;
        }
        System.out.println(moveTo);
        if (table.getRowCount() <= moveTo) {
            return;
        }
        table.setRowSelectionInterval(moveTo, moveTo);

    }

    private void doAction(int index) {
        if (table.getRowCount() <= index || index < 0) {
            return;
        }

        this.dispose();

        System.out.println(table.getValueAt(table.getSelectedRow(), 0).toString());

        SwingUtilities.invokeLater(() ->
                new ActionExecuteService().execute(actionsData.get(index))
        );
    }


    void setupTable(JTable table) {

        table.setFillsViewportHeight(true);

        table.setDefaultRenderer(Color.class, new DefaultTableCellRenderer());
        table.setSelectionBackground(Color.BLACK);
        table.setFocusable(false);

        table.addMouseListener
                (
                        new MouseAdapter() {
                            public void mouseClicked(MouseEvent evt) {
                                JTable source = (JTable)evt.getSource();
                                int row = source.rowAtPoint( evt.getPoint() );
                                doAction(row);

                            }
                        }
                );

    }



    void populate() {
        SimpleColorTableModel model = (SimpleColorTableModel) table.getModel();
        model.clear();
        for (Action action : actionsData) {
            model.addRow(new Object[]{action.getName(), action.getCategoryName()});
        }
        //table.setModel(model);
        model.fireTableDataChanged();
        //table.repaint();
        doSelectAction(0);


    }

}


class SimpleColorTableModel extends DefaultTableModel {

    public SimpleColorTableModel() {

        addColumn("Name");
        addColumn("Category");

    }

    void clear() {
        SimpleColorTableModel dm = this;
        int rowCount = dm.getRowCount();
        for (int i = rowCount - 1; i >= 0; i--) {
            dm.removeRow(i);
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {

        Class clazz = String.class;

        switch (columnIndex) {

            case 1:
                clazz = Color.class;
                break;

        }

        return clazz;

    }

}