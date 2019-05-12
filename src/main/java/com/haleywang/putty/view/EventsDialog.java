package com.haleywang.putty.view;

import com.haleywang.putty.dto.EventDto;
import com.haleywang.putty.service.NotificationsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.time.format.DateTimeFormatter;

public class EventsDialog extends JDialog {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventsDialog.class);

    private final JTable table;

    public EventsDialog(SpringRemoteView omegaRemote) {
        super(omegaRemote, "Event Log", true);

        LOGGER.debug("init dlg");

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
            model.addRow(new Object[]{eventDto});
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