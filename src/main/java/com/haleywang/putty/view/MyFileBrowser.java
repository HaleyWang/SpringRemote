package com.haleywang.putty.view;


import com.haleywang.putty.util.DateUtils;
import com.haleywang.putty.util.FileUtils;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import org.jdesktop.swingx.JXTable;

import javax.swing.DefaultRowSorter;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;


public class MyFileBrowser extends JDialog {

    public static interface OpenActionListener {

        void doOpen(String s);
    }


    private final JTextField fieldCurrentPath;
    private ChannelSftp sftpChannel;
    private DefaultTableModel tableModel;
    private JXTable table;
    private JTextField aTextField;
    private JTextField bTextField;
    List<ChannelSftp.LsEntry> lsEntrys;
    private String currentPath;
    final OpenActionListener openActionListener;


    public static void main(String[] args) throws SftpException, JSchException {
        JFrame jf = new JFrame();

        MyFileBrowser myFileBrowser = new MyFileBrowser("Remote file browser", "/", p -> {
            System.out.println(p);
        });

        jf.getContentPane().setLayout(new BorderLayout());

        jf.setSize(640, 480);
        //myFileBrowser.showOpenDialog();
        JButton a = new JButton("ls");
        a.addActionListener(e -> myFileBrowser.showOpenDialog());
        jf.getContentPane().add(a);

        jf.pack();
        jf.setVisible(true);


    }


    public MyFileBrowser setSftpChannel(ChannelSftp sftpChannel) {
        this.sftpChannel = sftpChannel;
        return this;
    }

    public void showOpenDialog() {
        System.out.println("====>");
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        SwingUtilities.invokeLater(() -> {
            try {
                if (sftpChannel == null) {
                    JSch jsch = new JSch();
                    Session session = null;


                /*
                String username = "root";
                String hostname = "47.88.13.53";
                byte[] password = "II3haley".getBytes();
                */
                    String username = "haley";
                    String hostname = "127.0.0.1";
                    byte[] password = "mddvideo".getBytes();

                    session = jsch.getSession(username, hostname, 22);
                    session.setConfig("StrictHostKeyChecking", "no");


                    session.setPassword(password);
                    session.connect();
                    Channel channel = session.openChannel("sftp");
                    channel.connect();
                    sftpChannel = (ChannelSftp) channel;
                    System.out.println("=======>2");
                }


                changeFolder(currentPath);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        });

        setVisible(true);
    }


    public MyFileBrowser(String title, String path, OpenActionListener openActionListener) throws JSchException, SftpException {
        super();

        setTitle(title);
        this.openActionListener = openActionListener;
        this.currentPath = path;


        setBounds(100, 100, 500, 400);
        String[] columnNames = {"File name", "Permission", "Date modified", "Size"};
        String[][] tableVales = {};


        setModal(true);


        tableModel = new DefaultTableModel(tableVales, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;//This causes all cells to be not editable
            }
        };
        table = new JXTable(tableModel);


        table.setSortable(true);


        table.getColumnModel().getColumn(0).setPreferredWidth(380);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(180);
        table.getColumnModel().getColumn(3).setPreferredWidth(80);


        JScrollPane scrollPane = new JScrollPane(table);
        getContentPane().add(scrollPane, BorderLayout.CENTER);

        JPanel topPane = new JPanel(new GridBagLayout());
        topPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        GridBagConstraints topGbc = new GridBagConstraints();
        topGbc.fill = GridBagConstraints.HORIZONTAL;

        topGbc.gridx = 0;
        topGbc.gridy = 0;
        topGbc.gridwidth = 1;
        topPane.add(new JLabel("Look in: "), topGbc);


        fieldCurrentPath = new JTextField(10);
        topGbc.gridx = 1;
        topGbc.gridy = 0;
        topGbc.weightx = 2;

        topPane.add(fieldCurrentPath, topGbc);

        topGbc.gridx = 2;
        topGbc.gridy = 0;
        topGbc.weightx = 0;
        JButton btnGo = new JButton("Go");
        btnGo.addActionListener(e -> {
            try {
                changeFolder(fieldCurrentPath.getText());
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });
        topPane.add(btnGo, topGbc);

        getContentPane().add(topPane, BorderLayout.NORTH);

        //table.setAutoCreateRowSorter(true);
        // DefaultRowSorter has the sort() method
        DefaultRowSorter sorter = new TableRowSorter<>(tableModel);
        ArrayList list1 = new ArrayList();
        list1.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        sorter.setSortKeys(list1);
        sorter.sort();


        //jdk1.6
        //排序:
        table.setRowSorter(sorter);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);


        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int selectedRow = table.getSelectedRow();

                if (selectedRow <= -1) {
                    return;
                }

                int modelSelectedRow = table.convertRowIndexToModel(selectedRow);
                ChannelSftp.LsEntry item = lsEntrys.get(modelSelectedRow);

                SftpATTRS itemAttrs = item.getAttrs();

                if (e.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    // your valueChanged overridden method

                    if (itemAttrs.isDir() || itemAttrs.isLink()) {

                        System.out.println("===>3");
                        try {
                            changeFolder(currentPath + "/" + item.getFilename());
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                    return;
                }

                Object oa = tableModel.getValueAt(modelSelectedRow, 0);
                Object ob = tableModel.getValueAt(modelSelectedRow, 1);
                System.out.println("getSelectedRow " + modelSelectedRow + " " + oa + " " + ob);

                if (itemAttrs.isDir()) {

                    aTextField.setText(oa.toString());


                } else {
                    aTextField.setText(oa.toString());

                }

            }
        });


        scrollPane.setViewportView(table);
        final JPanel panel = new JPanel(new BorderLayout());
        getContentPane().add(panel, BorderLayout.SOUTH);


        final JPanel selectedFilePanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        selectedFilePanel.add(new JLabel("Selected: "), gbc);
        aTextField = new JTextField();
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1;
        selectedFilePanel.add(aTextField, gbc);
        selectedFilePanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        panel.add(selectedFilePanel, BorderLayout.CENTER);


        final JButton addButton = new JButton("Open");
        addButton.addActionListener(e -> {
            String selectedPath = currentPath + "/" + aTextField.getText();
            selectedPath = selectedPath.replaceAll("//", "/");
            openActionListener.doOpen(selectedPath);
            setVisible(false);
        });
        JPanel openCancelPanel = new JPanel();
        openCancelPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 20, 5));

        panel.add(openCancelPanel, BorderLayout.SOUTH);
        openCancelPanel.add(addButton);

        final JButton delButton = new JButton("Cancel");
        delButton.addActionListener(e -> {
            setVisible(false);

        });

        openCancelPanel.add(delButton);

        setSize(700, 500);

    }

    private void changeFolder(String folderPath) throws SftpException, JSchException {
        if (!sftpChannel.isConnected()) {
            sftpChannel.connect();
        }
        sftpChannel.cd(folderPath);
        lsEntrys = Collections.list(sftpChannel.ls(folderPath).elements());
        System.out.println(" ====> " + sftpChannel.pwd());

        Vector data = tableModel.getDataVector();
        data.clear();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (ChannelSftp.LsEntry item : lsEntrys) {
            Vector rowValues = new Vector();
            String mTime = formatter.format(DateUtils.convertMillisecondsToLocalDateTime(item.getAttrs().getMTime() * 1000L));

            rowValues.add(item.getFilename());
            rowValues.add(item.getAttrs().getPermissionsString());
            rowValues.add(mTime);
            String sizeText = FileUtils.readableFileSize(item.getAttrs().getSize());
            sizeText = (item.getAttrs().isDir() || item.getAttrs().isLink()) ? "" : sizeText;
            rowValues.add(sizeText);

            data.add(rowValues);

        }

        //String[] columnNames = {"File name", "Permission", "Date modified", "Size"};

        //Vector cdata = new Vector();

        //for (String c : columnNames) {

        //    cdata.add(c);
        //}

        //tableModel.setDataVector(data, cdata);
        tableModel.setRowCount(data.size());
        tableModel.fireTableDataChanged();
        currentPath = sftpChannel.pwd();
        fieldCurrentPath.setText(currentPath);

    }


}
