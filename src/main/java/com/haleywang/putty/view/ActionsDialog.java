package com.haleywang.putty.view;

import com.haleywang.putty.dto.Action;
import com.haleywang.putty.service.ActionExecuteService;
import com.haleywang.putty.service.action.ActionCategoryEnum;
import com.haleywang.putty.service.action.ActionsData;
import com.haleywang.putty.util.StringUtils;
import com.intellij.util.ui.UIUtil;
import org.jdesktop.swingx.JXTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author haley
 */
public class ActionsDialog extends JDialog {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActionsDialog.class);

    public static final String ACTIONS = "Actions";
    private static final List<Action> ACTIONS_DATA = new ArrayList<>();
    private final JXTable table;
    private JTextField searchField;

    public ActionsDialog(SpringRemoteView omegaRemote) {
        super(omegaRemote, ACTIONS, true);

        JPanel panel = new JPanel(new BorderLayout());

        searchField = new JTextField(20);
        panel.add(searchField);


        searchField.addKeyListener(new KeyListener() {

            @Override
            public void keyPressed(KeyEvent e) {
                LOGGER.info("pfPassword keyPressed:{}", searchField.getText());
            }

            @Override
            public void keyReleased(KeyEvent e) {
                LOGGER.info("pfPassword keyReleased:{}", searchField.getText());

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
                LOGGER.info("pfPassword keyTyped:{}", searchField.getText());
            }
        });

        table = new JXTable(new ActionsTableModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

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
            String text = searchField.getText();
            String query = StringUtils.ifBlank(text, "").toLowerCase();
            ACTIONS_DATA.clear();

            List<Action> allActionData = new ArrayList<>();

            List<Action> staticData = ActionsData.getActionsData();
            List<Action> userData = new ArrayList<>();
            allActionData.addAll(staticData);
            DefaultMutableTreeNode commandsTreeNode = (DefaultMutableTreeNode) SideView.getInstance().getCommandsTreeView().getModel().getRoot();
            DefaultMutableTreeNode connectionsTreeNode = (DefaultMutableTreeNode) SideView.getInstance().getConnectionsInfoTreeView().getModel().getRoot();

            parseTreeNodes(userData, commandsTreeNode, ActionCategoryEnum.COMMAND);
            parseTreeNodes(userData, connectionsTreeNode, ActionCategoryEnum.SSH);
            allActionData.addAll(userData);

            allActionData.stream().filter(o -> StringUtils.isBlank(query) || o.searchText().contains(query)).collect(Collectors.toList()).forEach(ACTIONS_DATA::add
            );

            populate();

        });


    }


    void parseTreeNodes(List<Action> actions, DefaultMutableTreeNode data, ActionCategoryEnum category) {

        for (int i = 0, n = data.getChildCount(); i < n; i++) {
            TreeNode child = data.getChildAt(i);
            if (!(child instanceof DefaultMutableTreeNode)) {
                continue;
            }

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


    private void doSelectAction(int index) {
        int moveTo = index;
        if (moveTo >= table.getRowCount()) {
            moveTo = 0;
        } else if (moveTo < 0) {
            moveTo = table.getRowCount() - 1;
        }
        if (table.getRowCount() <= moveTo) {
            return;
        }
        table.setRowSelectionInterval(moveTo, moveTo);

        Rectangle rect = table.getCellRect(moveTo, 0, true);
        table.scrollRectToVisible(rect);

    }

    private void doAction(int index) {
        if (table.getRowCount() <= index || index < 0) {
            return;
        }

        this.dispose();

        SwingUtilities.invokeLater(() ->
                ActionExecuteService.getInstance().execute(ACTIONS_DATA.get(index))
        );
    }


    void setupTable(JTable table) {

        table.setSelectionBackground(Color.BLUE);
        table.setSelectionForeground(Color.WHITE);

        table.setFillsViewportHeight(true);

        table.setDefaultRenderer(Color.class, new DefaultTableCellRenderer());
        table.setFocusable(false);


        table.getColumnModel().getColumn(0).setPreferredWidth(300);
        table.getColumnModel().getColumn(1).setPreferredWidth(90);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);


        table.addMouseListener
                (
                        new MouseAdapter() {
                            @Override
                            public void mouseClicked(MouseEvent evt) {
                                JTable source = (JTable) evt.getSource();
                                int row = source.rowAtPoint(evt.getPoint());
                                doAction(row);

                            }
                        }
                );

    }


    void populate() {
        ActionsTableModel model = (ActionsTableModel) table.getModel();
        model.clear();
        for (Action action : ACTIONS_DATA) {
            model.addRow(new Object[]{action.getName(), action.getKeyMap(), action.getCategoryName()});
        }
        model.fireTableDataChanged();

        if (UIUtil.isUnderDarcula()) {
            setColor(table, new Color[]{Color.DARK_GRAY, new Color(78, 78, 78)});

        } else {
            setColor(table, new Color[]{Color.lightGray, new Color(210, 210, 210)});

        }

        doSelectAction(0);

    }

    public static void setColor(JTable table, Color[] colors) {

        List<String> categorys = ACTIONS_DATA.stream().map(Action::getCategoryName).distinct().collect(Collectors.toList());

        try {
            DefaultTableCellRenderer dtcr = new DefaultTableCellRenderer() {

                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

                    int colorIdx = categorys.indexOf(ACTIONS_DATA.get(row).getCategoryName()) % 2;

                    setBackground(colors[colorIdx]);
                    return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                }
            };
            int columnCount = table.getColumnCount();
            for (int i = 0; i < columnCount; i++) {
                table.getColumn(table.getColumnName(i)).setCellRenderer(dtcr);
            }

        } catch (Exception e) {
            LOGGER.error("setColorError", e);
        }
    }


    static class ActionsTableModel extends DefaultTableModel {

        public ActionsTableModel() {

            addColumn("Name");
            addColumn("Key");
            addColumn("Category");

        }

        void clear() {
            ActionsTableModel dm = this;
            int rowCount = dm.getRowCount();
            for (int i = rowCount - 1; i >= 0; i--) {
                dm.removeRow(i);
            }
        }


    }

}

