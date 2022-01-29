package com.haleywang.putty.view;

import com.haleywang.putty.common.Constants;
import com.haleywang.putty.dto.Action;
import com.haleywang.putty.service.ActionExecuteService;
import com.haleywang.putty.service.action.ActionCategoryEnum;
import com.haleywang.putty.service.action.ActionsData;
import com.haleywang.putty.util.StringUtils;
import com.haleywang.putty.view.side.SideView;
import com.intellij.util.ui.DrawUtil;
import org.demo.Autocomplete;
import org.jdesktop.swingx.JXTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.demo.Autocomplete.COMMIT_ACTION;

/**
 * @author haley
 */
public class ActionsDialog extends JDialog {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActionsDialog.class);

    public static final String ACTIONS = "Actions";
    private static final List<Action> ACTIONS_DATA = new ArrayList<>();
    private final JXTable table;
    private final JTextField searchField;

    public ActionsDialog(SpringRemoteView omegaRemote) {
        super(omegaRemote, ACTIONS, true);

        JPanel panel = new JPanel(new BorderLayout());

        searchField = initSearchField();
        panel.add(searchField);

        initTopBtns(panel);

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
        searchField.requestFocus();

    }

    private JTextField initSearchField() {

        JTextField mainTextField = new JTextField();

// Without this, cursor always leaves text field
        mainTextField.setFocusTraversalKeysEnabled(false);

// Our words to complete
        List<String> keywords = new ArrayList<>(5);

        for (ActionCategoryEnum item : ActionCategoryEnum.values()) {
            keywords.add("@" + item.getName().toLowerCase());

        }

        Autocomplete autoComplete = new Autocomplete(mainTextField, keywords);
        mainTextField.getDocument().addDocumentListener(autoComplete);

// Maps the tab key to the commit action, which finishes the autocomplete
// when given a suggestion
        mainTextField.getInputMap().put(KeyStroke.getKeyStroke("TAB"), COMMIT_ACTION);
        mainTextField.getActionMap().put(COMMIT_ACTION, autoComplete.new CommitAction());

        return mainTextField;
    }

    public void initTopBtns(JPanel panel) {
        JPanel filterPanel = new JPanel(new FlowLayout());

        JButton aBtn = new JButton("All");
        aBtn.setActionCommand("");
        aBtn.addActionListener(e -> {
            searchField.setText(e.getActionCommand());
            doSearch();
        });
        filterPanel.add(aBtn);
        for (ActionCategoryEnum item : ActionCategoryEnum.values()) {
            JButton btn = new JButton(item.getName());
            btn.setActionCommand(item.getName());
            btn.addActionListener(e -> {
                searchField.setText("@" + e.getActionCommand() + " ");
                searchField.requestFocus();
                doSearch();
            });
            filterPanel.add(btn);

        }

        panel.add(filterPanel, BorderLayout.PAGE_START);
    }


    private void doSearch() {

        SwingUtilities.invokeLater(() -> {
            String text = searchField.getText();
            String categoryText = "";
            String queryText = StringUtils.ifBlank(text, "").toLowerCase().trim();
            if (queryText.startsWith(Constants.AT_CHAR)) {
                int index = queryText.indexOf(' ');
                index = index <= 0 ? queryText.length() : index;

                categoryText = queryText.substring(1, index);
                queryText = queryText.substring(index).trim();
            }
            final String query = queryText.toLowerCase();
            final String category = categoryText.toLowerCase();
            ACTIONS_DATA.clear();

            List<Action> staticData = ActionsData.getActionsData();
            List<Action> userData = new ArrayList<>();
            List<Action> allActionData = new ArrayList<>(staticData);

            DefaultMutableTreeNode commandsTreeNode = (DefaultMutableTreeNode) SideView.getInstance().getCommandsTreeView().getModel().getRoot();
            DefaultMutableTreeNode connectionsTreeNode = (DefaultMutableTreeNode) SideView.getInstance().getConnectionsInfoTreeView().getModel().getRoot();

            parseTreeNodes(userData, commandsTreeNode);
            parseTreeNodes(userData, connectionsTreeNode);
            allActionData.addAll(userData);

            ACTIONS_DATA.addAll(allActionData.stream()
                    .filter(o -> StringUtils.isBlank(category) || o.getCategoryName().toLowerCase().contains(category))
                    .filter(o -> StringUtils.isBlank(query) || o.searchText().toLowerCase().contains(query))
                    .collect(Collectors.toList()));

            populate();

        });


    }


    void parseTreeNodes(List<Action> actions, DefaultMutableTreeNode data) {

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
                parseTreeNodes(actions, treeChild);
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

        if (DrawUtil.isUnderDarcula()) {
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

