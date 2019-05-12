package com.haleywang.putty.view;

import com.haleywang.putty.dto.Action;
import com.haleywang.putty.dto.EventDto;
import com.haleywang.putty.service.ActionExecuteService;
import com.haleywang.putty.service.NotificationsService;
import com.haleywang.putty.service.action.ActionCategoryEnum;
import com.haleywang.putty.service.action.data.ActionsData;
import com.haleywang.putty.util.StringUtils;
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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EventsDialog extends JDialog {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActionsDialog.class);

    private final JTable table;

    public EventsDialog(SpringRemoteView omegaRemote) {
        super(omegaRemote, "Event Log", true);


        table = new JTable(new EventTableModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

        };

        setupTable(table);
        populate();

        getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);

        pack();
        setResizable(true);
        setLocationRelativeTo(omegaRemote);
    }




    void setupTable(JTable table) {

        table.setFillsViewportHeight(true);

        table.setDefaultRenderer(Object.class, new EventStringRenderer());
        table.setSelectionBackground(Color.BLACK);
        table.setFocusable(false);
    }



    void populate() {
        EventTableModel model = (EventTableModel) table.getModel();
        for (EventDto eventDto : NotificationsService.getInstance().getEvents()) {
            model.addRow(new Object[]{ eventDto});
        }
        model.fireTableDataChanged();
    }

}


class EventStringRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value,
                isSelected, hasFocus, row, column);

        if (value instanceof EventDto) {
            EventDto eventDto = (EventDto) value;
            setText(eventDto.getTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " " + eventDto.getMessage());

            switch (eventDto.getLevel()) {
                case INFO:
                    setForeground(Color.BLUE);
                    break;
                case WARN:
                    setForeground(Color.ORANGE);
                    break;
                case ERROR:
                    setForeground(Color.RED);
                    break;
                default:
                    setForeground(Color.lightGray);

            }

        }
        return this;
    }
}

class EventTableModel extends DefaultTableModel {

    public EventTableModel() {

        addColumn("Event");

    }

}