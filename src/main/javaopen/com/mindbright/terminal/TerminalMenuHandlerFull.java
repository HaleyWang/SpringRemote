/******************************************************************************
 *
 * Copyright (c) 1999-2013 Cryptzone AB. All Rights Reserved.
 *
 * This file contains Original Code and/or Modifications of Original Code as
 * defined in and that are subject to the MindTerm Public Source License,
 * Version 2.0, (the 'License'). You may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the MindTerm Public Source License
 * along with this software; see the file LICENSE.  If not, write to
 * Cryptzone Group AB, Drakegatan 7, SE-41250 Goteborg, SWEDEN
 *
 *****************************************************************************/

package com.mindbright.terminal;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.SystemColor;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;

import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import javax.swing.JMenu;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JCheckBox;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JPanel;
import javax.swing.JDialog;
import javax.swing.SwingConstants;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuBar;
import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import com.mindbright.gui.GUI;

/**
 * The actual implementation of the standard terminal menus.
 *
 * @see TerminalWin
 */
public class TerminalMenuHandlerFull extends TerminalMenuHandler
    implements ActionListener, ItemListener, TerminalPrinter
{
    private void setProperty(String key, String value) {
        try {
            term.setProperty(key, value);
        } catch (IllegalArgumentException e) {
            GUI.showAlert("Failed to set property", e.getMessage(),
                          term.container);
        }
    }

    private String getProperty(String key) {
        return term.getProperty(key);
    }

    private TerminalWin term;
    private String titleName;
    private TerminalMenuListener listener;

    public final static int MENU_FILE     = 0;
    public final static int MENU_EDIT     = 1;
    public final static int MENU_SETTINGS = 2;

    final static int M_FILE_PRINT_SCREEN = 1;
    final static int M_FILE_PRINT_BUFFER = 2;
    final static int M_FILE_CAPTURE      = 3;
    final static int M_FILE_SEND         = 4;
    final static int M_FILE_CLOSE        = 6;

    final static int M_SET_TERM     = 1;

    final static int M_EDIT_COPY    = 1;
    final static int M_EDIT_PASTE   = 2;
    final static int M_EDIT_CPPASTE = 3;
    final static int M_EDIT_SELALL  = 4;
    final static int M_EDIT_FIND    = 5;
    final static int M_EDIT_CLS     = 7;
    final static int M_EDIT_CLEARSB = 8;
    final static int M_EDIT_VTRESET = 9;

    private final static String[][] menuTexts = {
		{ "File",
          "Print screen...", "Print buffer...",
		  "_Capture To File...", "Send ASCII File...", null, "Close"
		},
		{ "Edit",
		  "Copy Ctrl+Ins", "Paste Shift+Ins", "Copy & Paste", "Select All",
		  "Find...", null,
		  "Clear Screen", "Clear Scrollback", "VT Reset"
		},
		{ "Settings",
		  "Terminal...",
		}
    };

    private final static int NO_SHORTCUT = -1;
    private final static int[][] menuShortCuts = {
		{ NO_SHORTCUT, NO_SHORTCUT, NO_SHORTCUT, NO_SHORTCUT, NO_SHORTCUT,
          NO_SHORTCUT, KeyEvent.VK_E },

		{ NO_SHORTCUT, NO_SHORTCUT, NO_SHORTCUT, NO_SHORTCUT, KeyEvent.VK_A,
		  KeyEvent.VK_F, NO_SHORTCUT, NO_SHORTCUT, NO_SHORTCUT, NO_SHORTCUT },

		{ NO_SHORTCUT, NO_SHORTCUT },

		{ NO_SHORTCUT, NO_SHORTCUT, NO_SHORTCUT, NO_SHORTCUT, NO_SHORTCUT,
		  NO_SHORTCUT, NO_SHORTCUT, NO_SHORTCUT, NO_SHORTCUT, NO_SHORTCUT,
		  NO_SHORTCUT, NO_SHORTCUT, NO_SHORTCUT, NO_SHORTCUT, NO_SHORTCUT,
		  NO_SHORTCUT, NO_SHORTCUT, NO_SHORTCUT, NO_SHORTCUT, NO_SHORTCUT },
    };

    private SearchContext lastSearch;
    private Object[][] menuItems;
    private JComponent vtoptions[];
    private TerminalOption toptions[];
    private JComboBox comboTE, comboEN, comboFN, comboSB, comboPB;
    private JComboBox comboFG, comboBG, comboCC;
    private JTextField textFS, textRows, textCols;
    private JTextField textSL, textSD;
    private JTextField textFG, textBG, textCC;
    private JLabel lblAlert;
    private JCheckBox checkIN;
    private JTextField  findText;
    private JCheckBox   dirCheck, caseCheck;
    private JButton     findBut, cancBut;
    private JPopupMenu popupMenu = null;

    private TFileOutputStream printerOut;
	private boolean printingToFile;

    public TerminalMenuHandlerFull() {
		this("MindTerm");
    }

    public TerminalMenuHandlerFull(String titleName) {
		setTitleName(titleName);
    }

    public void setTitleName(String titleName) {
		this.titleName = titleName;
    }

    public void setTerminalWin(TerminalWin term) {
		this.term = term;
		term.attachPrinter(this);
    }

    public void setReadOnlyMode(boolean readOnly) {
        setEnabled(MENU_FILE, M_FILE_SEND,    !readOnly);
        setEnabled(MENU_EDIT, M_EDIT_PASTE,   !readOnly);
        setEnabled(MENU_EDIT, M_EDIT_CPPASTE, !readOnly);
    }

    public void setTerminalMenuListener(TerminalMenuListener listener) {
		this.listener = listener;
    }

    public void addBasicMenus(TerminalWin terminal, JMenuBar mb) {
		setTerminalWin(terminal);
		mb.add(getMenu(MENU_FILE));
		mb.add(getMenu(MENU_EDIT));
		mb.add(getMenu(MENU_SETTINGS));
		terminal.setMenus(this);
		terminal.setClipboard(GlobalClipboard.getClipboardHandler(this));
		terminal.updateMenus();
    }

    public void updateSelection(boolean selectionAvailable) {
        setEnabled(MENU_EDIT, M_EDIT_COPY, selectionAvailable);
        setEnabled(MENU_EDIT, M_EDIT_CPPASTE, selectionAvailable);
    }

    public void update() {
		if (listener != null)
			listener.update();
    }

    // stuff for terminal settings dialog
    private final static String[] PASTE_BUTTON   = { "middle", "right", "shift+left" };
    private final static String[] SCROLLBAR_POS  = { "left", "right", "none" };
    private final static String[] TERMINAL_TYPES = TerminalWin.getTerminalTypes();
    private final static String[] FONT_LIST      = GUI.getFontList();
    private final static String[] ENCODINGS      = {"iso-8859-1", "utf-8", "euc-jp", "euc-kr", "us-ascii"};

    private final static String LBL_TERMINAL_TYPE     = "Terminal type";
    private final static String LBL_COLUMNS           = "Columns";
    private final static String LBL_ROWS              = "Rows";
    private final static String LBL_FONT              = "Font";
    private final static String LBL_ENCODING          = "Encoding";
    private final static String LBL_SIZE              = "Size";
    private final static String LBL_SCROLLBACK_BUFFER = "Scrollback buffer";
    private final static String LBL_SCROLLBAR_POS     = "Scrollbar position";
    private final static String LBL_FG_COLOR          = "Foreground color";
    private final static String LBL_BG_COLOR          = "Background color";
    private final static String LBL_CURS_COLOR        = "Cursor color";
    private final static String LBL_PASTE_BUTTON      = "Paste button";
    private final static String LBL_SELECT_DELIM      = "Select delim.";
    private final static String LBL_IGN_NULL          = "Ignore null bytes";
    private final static String LBL_TAB_GENERAL       = "General";
    private final static String LBL_TAB_MISC          = "Misc";
    private final static String LBL_TAB_COLORS        = "Colors";
    private final static String LBL_TAB_VTOPTIONS1    = "VT 1";
    private final static String LBL_TAB_VTOPTIONS2    = "VT 2";
    private final static String LBL_CUSTOM_RGB        = "custom rgb";
    private final static String LBL_FIND              = "Find";
    private final static String LBL_CASE_SENSITIVE    = "Case sensitive";
    private final static String LBL_FIND_BACKWARDS    = "Find backwards";
    private final static String LBL_BTN_OK            = "OK";
    private final static String LBL_BTN_CANCEL        = "Cancel";
    private final static String LBL_BTN_FIND          = "Find";

    /**
     * Show the terminal settings dialog.
     *
     * @param title desired title of dialog
     */
    public void termSettingsDialog(String title) {
        final JDialog dialog = GUI.newJDialog(term.container, title, true);

        // general tab
        GridBagConstraints gridcl = new GridBagConstraints();
        GridBagConstraints gridcr = new GridBagConstraints();

        JPanel mp = new JPanel(new GridBagLayout());

        gridcl.fill      = GridBagConstraints.HORIZONTAL;
        gridcl.insets    = new Insets(2, 2, 2, 2);
        gridcl.gridwidth = 1;
        gridcr.anchor    = GridBagConstraints.WEST;
        gridcr.insets    = new Insets(2, 2, 2, 2);
        gridcr.gridwidth = GridBagConstraints.REMAINDER;
        gridcr.weightx   = 1.0;


        comboTE = new JComboBox(TERMINAL_TYPES);
        mp.add(new JLabel(LBL_TERMINAL_TYPE, SwingConstants.RIGHT), gridcl);
        mp.add(comboTE, gridcr);

        textCols = new JTextField("", 3);
        mp.add(new JLabel(LBL_COLUMNS, SwingConstants.RIGHT), gridcl);
        mp.add(textCols, gridcr);

        textRows = new JTextField("", 3);
        mp.add(new JLabel(LBL_ROWS, SwingConstants.RIGHT), gridcl);
        mp.add(textRows, gridcr);

        comboEN = new JComboBox(ENCODINGS);
        comboEN.setEditable(true);
        mp.add(new JLabel(LBL_ENCODING, SwingConstants.RIGHT), gridcl);
        mp.add(comboEN, gridcr);

        comboFN = new JComboBox(FONT_LIST);
        mp.add(new JLabel(LBL_FONT, SwingConstants.RIGHT), gridcl);
        mp.add(comboFN, gridcr);

        textFS = new JTextField("", 3);
        mp.add(new JLabel(LBL_SIZE, SwingConstants.RIGHT), gridcl);
        mp.add(textFS, gridcr);

        textSL = new JTextField("", 3);
        mp.add(new JLabel(LBL_SCROLLBACK_BUFFER, SwingConstants.RIGHT),gridcl);
        mp.add(textSL, gridcr);

        comboSB = new JComboBox(SCROLLBAR_POS);
        mp.add(new JLabel(LBL_SCROLLBAR_POS, SwingConstants.RIGHT), gridcl);
        mp.add(comboSB, gridcr);

        lblAlert = new JLabel("", SwingConstants.CENTER);
        GridBagConstraints gridc = new GridBagConstraints();
        gridc.fill      = GridBagConstraints.BOTH;
        gridc.weighty   = 1.0;
        gridc.anchor    = GridBagConstraints.CENTER;
        gridc.gridwidth = GridBagConstraints.REMAINDER;
        mp.add(lblAlert, gridc);

        JTabbedPane tp = new JTabbedPane();
        tp.addTab(LBL_TAB_GENERAL, mp);

        // color tab
        mp = new JPanel(new GridBagLayout());

        gridc.weightx   = 0.0;
        gridc.weighty   = 0.0;
        gridc.gridwidth = 1;
        gridc.insets    = new Insets(2, 2, 2, 2);

        ItemListener ilC = new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                updateColors();
            }
        };
        comboFG = new JComboBox();
        comboFG.addItemListener(ilC);
        textFG = new JTextField("", 10);
        mp.add(new JLabel(LBL_FG_COLOR, SwingConstants.RIGHT), gridc);
        mp.add(comboFG, gridc);
        gridc.gridwidth = GridBagConstraints.REMAINDER;
        mp.add(textFG, gridc);

        gridc.gridwidth = 1;
        comboBG = new JComboBox();
        comboBG.addItemListener(ilC);
        textBG = new JTextField("", 10);
        mp.add(new JLabel(LBL_BG_COLOR, SwingConstants.RIGHT), gridc);
        mp.add(comboBG, gridc);
        gridc.gridwidth = GridBagConstraints.REMAINDER;
        mp.add(textBG, gridc);

        gridc.gridwidth = 1;
        comboCC = new JComboBox();
        comboCC.addItemListener(ilC);
        textCC = new JTextField("", 10);
        mp.add(new JLabel(LBL_CURS_COLOR, SwingConstants.RIGHT), gridc);
        mp.add(comboCC, gridc);
        gridc.gridwidth = GridBagConstraints.REMAINDER;
        mp.add(textCC, gridc);

        gridc.fill = GridBagConstraints.BOTH;
        gridc.weightx = 1.0;
        gridc.weighty = 1.0;
        mp.add(new JLabel(""), gridc);

        tp.addTab(LBL_TAB_COLORS, mp);


        // misc tab
        gridc = new GridBagConstraints();
        mp = new JPanel(new GridBagLayout());
        mp.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        gridc.insets    = new Insets(2, 2, 2, 2);
        gridc.anchor    = GridBagConstraints.WEST;
        gridc.gridwidth = 1;
        mp.add(new JLabel(LBL_PASTE_BUTTON, SwingConstants.RIGHT), gridc);

        comboPB = new JComboBox(PASTE_BUTTON);
        gridc.gridwidth = GridBagConstraints.REMAINDER;
        mp.add(comboPB, gridc);

        gridc.gridwidth = 1;
        mp.add(new JLabel(LBL_SELECT_DELIM, SwingConstants.RIGHT), gridc);

        textSD = new JTextField("", 8);
        gridc.gridwidth = GridBagConstraints.REMAINDER;
        mp.add(textSD, gridc);

        gridc.gridwidth = 1;
        mp.add(new JLabel(), gridc);

        checkIN = new JCheckBox(
            LBL_IGN_NULL, Boolean.parseBoolean(getProperty("ignore-null")));
        gridc.gridwidth = GridBagConstraints.REMAINDER;
        mp.add(checkIN, gridc);

        gridc.fill = GridBagConstraints.BOTH;
        gridc.weightx = 1.0;
        gridc.weighty = 1.0;
        mp.add(new JLabel(""), gridc);

        tp.addTab(LBL_TAB_MISC, mp);


        // VT options tab (top half)
        Component vtp = getVTOptionsPanel(true);
        if (vtp != null) {
            tp.addTab(LBL_TAB_VTOPTIONS1, vtp);
        }

        // VT options tab (bottom half)
        vtp = getVTOptionsPanel(false);
        if (vtp != null) {
            tp.addTab(LBL_TAB_VTOPTIONS2, vtp);
        }

        dialog.getContentPane().add(tp, BorderLayout.CENTER);
        tp.setSelectedIndex(0);

        JButton ok = new JButton(LBL_BTN_OK);
        JButton cancel = new JButton(LBL_BTN_CANCEL);
        JPanel bp = GUI.newButtonPanel(new JButton[] {ok,cancel});
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    setVTOptions();

                    setProperty("term-type",
                                TERMINAL_TYPES[comboTE.getSelectedIndex()]);
                    setProperty("encoding", (String)comboEN.getSelectedItem());
                    setProperty("font-name",
                                FONT_LIST[comboFN.getSelectedIndex()]);
                    setProperty("font-size", textFS.getText());
                    setProperty("scrollbar",
                                SCROLLBAR_POS[comboSB.getSelectedIndex()]);
                    setProperty("save-lines", textSL.getText());
                    setProperty("geometry",
                                textCols.getText() + "x" + textRows.getText());
                    setProperty("paste-button",
                                PASTE_BUTTON[comboPB.getSelectedIndex()]);
                    setProperty("select-delim", textSD.getText());
                    setProperty("ignore-null",
                                Boolean.toString(checkIN.isSelected()));
                    setProperty("fg-color", getSelectedColor(comboFG, textFG));
                    setProperty("bg-color", getSelectedColor(comboBG, textBG));
                    setProperty("cursor-color",
                                getSelectedColor(comboCC, textCC));
                    dialog.dispose();
                } catch (Exception ee) {
                    lblAlert.setText(ee.getMessage());
                }
            }
        });
        cancel.addActionListener(new GUI.CloseAction(dialog));

        dialog.getContentPane().add(bp, BorderLayout.SOUTH);

		comboTE.setSelectedItem(getProperty("term-type"));
		comboEN.setSelectedItem(getProperty("encoding"));
		comboFN.setSelectedItem(getProperty("font-name"));
		textFS.setText(getProperty("font-size"));
		textCols.setText(String.valueOf(term.cols()));
		textRows.setText(String.valueOf(term.rows()));
		comboSB.setSelectedItem(getProperty("scrollbar"));
		textSL.setText(getProperty("save-lines"));

		comboPB.setSelectedItem(getProperty("paste-button"));
		String sdSet = getProperty("select-delim");
		if ((sdSet.charAt(0) == '"'
             && sdSet.charAt(sdSet.length() - 1) == '"')) {
			sdSet = sdSet.substring(1, sdSet.length() - 1);
		}
		textSD.setText(sdSet);

		comboBG.addItem(LBL_CUSTOM_RGB);
		comboFG.addItem(LBL_CUSTOM_RGB);
		comboCC.addItem(LBL_CUSTOM_RGB);
		for (int i = 0; i < TerminalWin.termColorNames.length; i++) {
			comboBG.addItem(TerminalWin.termColorNames[i]);
			comboFG.addItem(TerminalWin.termColorNames[i]);
			comboCC.addItem(TerminalWin.termColorNames[i]);
		}
		initColorSelect(comboFG, textFG, getProperty("fg-color"));
		initColorSelect(comboBG, textBG, getProperty("bg-color"));
		initColorSelect(comboCC, textCC, getProperty("cursor-color"));
		updateColors();

		lblAlert.setText("");

        dialog.setResizable(true);
        dialog.pack();

        GUI.placeDialog(dialog);

		comboTE.requestFocus();
        dialog.addWindowListener(GUI.getWindowDisposer());
		dialog.setVisible(true);
    }

    /**
     * Show the terminal settings dialog with the default title.
     */
    public void termSettingsDialog() {
        termSettingsDialog("Terminal Settings");
    }

    /**
     * Show the find dialog.
     *
     * @param title desired title of dialog
     */
    public void findDialog(String title) {
        lastSearch = null;

        JDialog dialog = GUI.newBorderJDialog(term.container, title, false);

        dialog.getContentPane().setLayout(new FlowLayout(FlowLayout.LEFT));

        Box b = new Box(BoxLayout.Y_AXIS);
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.add(new JLabel(LBL_FIND));
        p.add(findText  = new JTextField("", 20));
        b.add(p);

        p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.add(caseCheck = new JCheckBox(LBL_CASE_SENSITIVE));
        p.add(dirCheck  = new JCheckBox(LBL_FIND_BACKWARDS));
        b.add(p);

        dialog.getContentPane().add(b);

        p = new JPanel(new GridLayout(0, 1, 0, 5));
        p.add(findBut = new JButton(LBL_BTN_FIND));
        p.add(cancBut = new JButton(LBL_BTN_CANCEL));
        dialog.getContentPane().add(p);

        cancBut.addActionListener(new GUI.CloseAction(dialog));
        findBut.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String txt = findText.getText();
                if (txt != null && txt.length() > 0)
                    doFind(txt, caseCheck.isSelected(), dirCheck.isSelected());
            }
        });

        dialog.setResizable(true);
        dialog.pack();

		GUI.placeDialog(dialog);
		findText.requestFocus();
        dialog.addWindowListener(GUI.getWindowDisposer());
		dialog.setVisible(true);
    }

    private void doFind(String findStr, boolean caseSens, boolean revFind) {
        lastSearch = term.search(lastSearch, findStr, revFind, caseSens);
        if (lastSearch == null)
            term.ringBell();
    }

    /**
     * Show the send file dialog.
     */
    public final void sendFileDialog() {
        File file = GUI.selectFile(term.container, titleName +
                                   " - Select ASCII-file to send", false);

        if (file == null) return;

        try {
            FileInputStream fileIn = new FileInputStream(file);
			byte[] bytes;
			try {
				bytes = new byte[fileIn.available()];
				fileIn.read(bytes);
			} finally {
				try { fileIn.close(); } catch (IOException ioe) {}
			}
            term.sendBytes(bytes);
        } catch (Throwable t) {
            GUI.showAlert(titleName + " - Alert", t.getMessage(),
                          term.container);
        }
    }

    /**
     * Shows a save dialog with the specified title. The selected file
     * is returned as an opened <code>FileOutputStream</code>
     *
     * @param title title of dialog
     * @return the opened file or null if the user aborted or an error
     * ocurred.
     *
     */
    public final TFileOutputStream chooseFileDialog(String title) {
        File file = GUI.selectFile
            (term.container, titleName + " - " + title, true);

        if (file == null) return null;

        try {
            boolean append = false;
            if (file.exists()) {
                append = GUI.showConfirm(titleName + " - File exists",
                                         "File exists, overwrite or append?",
                                         0, 0, "Append", "Overwrite",
                                         true, term.container, false, false);
            }
            return new TFileOutputStream(file, append);
        } catch (Throwable t) {
            GUI.showAlert(titleName + " - Alert", t.getMessage(),
                          term.container);
        }
		return null;
    }

	private static class TFileOutputStream extends FileOutputStream {
		private File file;

		TFileOutputStream(File f, boolean append) throws IOException {
			super(f.getCanonicalPath(), append);
			this.file = f;
		}
	}

    private TerminalCapture termCapture;

    /**
     * Start capturing data to file. This function will cause a save
     * dialog to appear and if a file was successfully specified then a
     * log of the terminal session from now on will be stored in the file.
     */
    public final boolean captureToFileDialog() {
		FileOutputStream fileOut =
            chooseFileDialog("Select file to capture to");
		if (fileOut != null) {
			termCapture = new TerminalCapture(new BufferedOutputStream(fileOut, 2048));
			termCapture.startCapture(term);
			return true;
		}

		return false;
    }

    /**
     * Stop capturing data in a file. Capturing is started by calling
     * <code>captureToFileDialog()</code>.
     */
    public void endCapture() {
		if(termCapture != null) {
			termCapture.endCapture();
			try {
				termCapture.getTarget().close();
			} catch (IOException e) {
				GUI.showAlert(titleName + " - Alert", e.getMessage(),
                              term.container);
			}
		}
    }

    private void setEnabled(int i, int j, boolean v) {
        ((JMenuItem)menuItems[i][j]).setEnabled(v);
    }

    private void setState(int i, int j, boolean v) {
        ((JCheckBoxMenuItem)menuItems[i][j]).setSelected(v);
    }

    private boolean getState(int i, int j) {
        return ((JCheckBoxMenuItem)menuItems[i][j]).isSelected();
    }

    public JMenu getMenu(int idx) {
		JMenu m = new JMenu(menuTexts[idx][0]);
		int len = menuTexts[idx].length;
		JMenuItem mi;
		String   t;

		if (menuItems == null)
			menuItems = new JMenuItem[menuTexts.length][];
		if (menuItems[idx] == null) {
			menuItems[idx] = new JMenuItem[menuTexts[idx].length];
        }

		for (int i = 1; i < len; i++) {
			t = menuTexts[idx][i];
			if (t == null) {
				m.addSeparator();
				continue;
			}
			if (t.charAt(0) == '_') {
				t = t.substring(1);
				mi = new JCheckBoxMenuItem(t);
				((JCheckBoxMenuItem)mi).addItemListener(this);
			} else {
				mi = new JMenuItem(t);
				mi.addActionListener(this);
			}

			if (menuShortCuts[idx][i] != NO_SHORTCUT) {
				mi.setAccelerator
                    (KeyStroke.getKeyStroke
                     (menuShortCuts[idx][i],
                      java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() |
                      java.awt.event.InputEvent.SHIFT_MASK));
			}

			menuItems[idx][i] = mi;
			m.add(mi);
		}
		return m;
    }

    private int[] mapAction(Object o) {
		int[] id = new int[2];
		int i = 0, j = 0;

        for(i = 0; i < menuItems.length; i++) {
            for(j = 1; menuItems[i] != null && j < menuItems[i].length;
                j++)
            {
                if(menuItems[i][j] == o) {
                    id[0] = i;
                    id[1] = j;
                    return id;
                }
            }
        }
        return id;
    }

    /**
     * Called when a menu item was selected
     */
    public void actionPerformed(ActionEvent e) {
		handleMenuAction(mapAction(e.getSource()));
    }

    /**
     * Called when a checkbox menu item has changed state
     */
    public void itemStateChanged(ItemEvent e) {
		handleMenuAction(mapAction(e.getSource()));
    }

    /**
     * Actually handle the selection of all the menu elements.
     */
    public void handleMenuAction(int[] id) {
		switch(id[0]) {
            case MENU_FILE:
                switch(id[1]) {
                    case M_FILE_PRINT_SCREEN:
                        printScreen();
                        break;

                    case M_FILE_PRINT_BUFFER:
                        printBuffer();
                        break;

                    case M_FILE_CAPTURE:
                        if (getState(MENU_FILE, M_FILE_CAPTURE)) {
                            if(!captureToFileDialog())
                                setState(MENU_FILE, M_FILE_CAPTURE, false);
                        } else {
                            endCapture();
                        }
                        break;
                    case M_FILE_SEND:
                        sendFileDialog();
                        break;
                    case M_FILE_CLOSE:
                        if (listener != null)
                            listener.close(this);
                        break;
                }
                break;

            case MENU_EDIT:
                switch(id[1]) {
                    case M_EDIT_COPY:
                        term.doCopy();
                        break;
                    case M_EDIT_PASTE:
                        term.doPaste();
                        break;
                    case M_EDIT_CPPASTE:
                        term.doCopy();
                        term.doPaste();
                        break;
                    case M_EDIT_SELALL:
                        term.selectAll();
                        break;
                    case M_EDIT_FIND:
                        findDialog(titleName + " - Find");
                        break;
                    case M_EDIT_CLS:
                        term.clearScreen();
                        term.setCursorPos(0, 0);
                        break;
                    case M_EDIT_CLEARSB:
                        term.clearSaveLines();
                        break;
                    case M_EDIT_VTRESET:
                        term.reset();
                        break;
                }
                break;

            case MENU_SETTINGS:
                switch(id[1]) {
                    case M_SET_TERM:
                        termSettingsDialog();
                        break;
                }
                break;
		}
    }

    private Component getVTOptionsPanel(boolean top) {
        int start, end;
        if (top) {
            toptions = term.getOptions();
            vtoptions = new JComponent[toptions.length];
            start = 0;
            end = toptions.length/2;
        } else {
            start = toptions.length/2;
            end = toptions.length;
        }

        if (toptions == null || toptions.length == 0)
            return null;

        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints gridc = new GridBagConstraints();
        gridc.fill      = GridBagConstraints.NONE;
        gridc.anchor    = GridBagConstraints.WEST;
        gridc.gridwidth = GridBagConstraints.REMAINDER;
        gridc.insets    = new Insets(2, 5, 2, 5);

        for (int i=start; i<end; i++) {
            String cs[] = toptions[i].getChoices();
            if (cs == null) {
                vtoptions[i] = new JCheckBox(toptions[i].getDescription(),
                                             toptions[i].getValueB());
            } else {
                gridc.gridwidth = 1;
                gridc.fill      = GridBagConstraints.HORIZONTAL;
                p.add(new JLabel(toptions[i].getDescription(),
                                 SwingConstants.RIGHT), gridc);
                vtoptions[i] = new JComboBox(cs);
                ((JComboBox)vtoptions[i]).setSelectedItem(
                    toptions[i].getValue().toLowerCase());
                gridc.gridwidth = GridBagConstraints.REMAINDER;
                gridc.fill      = GridBagConstraints.NONE;
            }
            p.add(vtoptions[i], gridc);
        }

        gridc.fill      = GridBagConstraints.BOTH;
        gridc.weightx   = 1.0;
        gridc.weighty   = 1.0;
        p.add(new JLabel(""), gridc);

        JScrollPane sp = new JScrollPane
            (p, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
             JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        sp.getViewport().setBackground(new Color(p.getBackground().getRGB()));
        return sp;
    }

    private void setVTOptions() {
        if (vtoptions == null) return;

        for (int i=0; i<vtoptions.length; i++) {
            String val;
            if (vtoptions[i] instanceof JComboBox) {
                val = (String)((JComboBox)vtoptions[i]).getSelectedItem();
            } else {
                val = ((JCheckBox)vtoptions[i]).isSelected() ? "true" : "false";
            }
            setProperty(toptions[i].getKey(), val);
        }

        vtoptions = null;
        toptions = null;
    }

    private static void initColorSelect(JComboBox c, JTextField t, String colStr) {
		if (Character.isDigit(colStr.charAt(0))) {
			c.setSelectedItem(LBL_CUSTOM_RGB);
			t.setText(colStr);
		} else {
			t.setText("");
			t.setEnabled(false);
			c.setSelectedItem(colStr);
		}
    }

    private static void checkColorSelect(JComboBox c, JTextField t) {
		int cs = c.getSelectedIndex();

		if (cs <= 0) {
			if (!t.isEnabled()) {
				t.setEditable(true);
				t.setEnabled(true);
				t.setBackground(SystemColor.text);
				t.requestFocus();
			}
		} else {
			t.setText("");
			t.setEditable(false);
			t.setEnabled(false);
			// on the Mac, Combos can't get keyboard focus
			// so we may need to move focus away from the JTextField
			t.setBackground(TerminalWin.termColors[cs - 1]);
		}
    }

    private void updateColors() {
		checkColorSelect(comboFG, textFG);
		checkColorSelect(comboBG, textBG);
		checkColorSelect(comboCC, textCC);
    }

    private static String getSelectedColor(JComboBox c, JTextField t) {
		return (c.getSelectedIndex() == 0) ? t.getText() : (String)c.getSelectedItem();
    }



    public void setPopupMenu(Object menu) {
        popupMenu = (JPopupMenu)menu;
    }

    public void showPopupMenu(int x, int y) {
        if (popupMenu != null) {
            popupMenu.show(term.container, x, y);
        }
    }

    /**
     * Write a character to the printer.
     *
     * @param c charcter to write
     */
    public void write(char c) {
		if(printerOut != null) {
			try {
				printerOut.write(c);
			} catch (IOException e) {
				GUI.showAlert(titleName + " - Alert", e.getMessage(),
                              term.container);
			}
		}
    }

    /**
     * Start printing data to printer. That is currently it only saves
     * data in a file.
     */
    public void startPrinter(boolean tofile) {
		printingToFile = tofile;
		if (tofile) {
			printerOut = chooseFileDialog("Passthrough print to file");
		} else {
			try {
				File tfile = File.createTempFile("mtprint", null);
				tfile.deleteOnExit();
				printerOut = new TFileOutputStream(tfile, false);
			} catch (IOException ioe) {
			}
		}
    }

    /**
     * Stop sending data to printer.
     */
    public void stopPrinter(boolean resetterminal) {
		if(printerOut != null) {
			if (resetterminal) term.clearScreen();
			try { printerOut.close(); }
			catch (IOException e) { /* don't care... */ }
			File file = printerOut.file;
			printerOut = null;
			if (printingToFile) {
				GUI.showAlert(titleName + " - Alert",
							  "Passthrough printing ended, file saved.",
							  term.container);
			} else {
                            doPrint(file);
                        }
                        if (resetterminal) term.reset();
		}
    }

    public void printScreen() {
        doPrint(TerminalPrintable.SCREEN);
    }

    public void printBuffer() {
        doPrint(TerminalPrintable.BUFFER);
    }

    private void doPrint(int what) {
        PrinterJob job = PrinterJob.getPrinterJob();

        job.setPrintable(
            new TerminalPrintable(term.getDisplay().getModel(), what,
                                  term.printFontName, term.printFontSize));
		job.setCopies(1);
        if (job.printDialog()) {
            try {
                job.print();
            } catch (PrinterException e) {
                // Handle Exception
            }
        }
    }

    private void doPrint(File f) {
        PrinterJob job = PrinterJob.getPrinterJob();

        job.setPrintable(new PrintListingPainter(f, term.printFontName, term.printFontSize));
		job.setCopies(1);
        if (job.printDialog()) {
            try {
                job.print();
            } catch (PrinterException e) {
                // Handle Exception
            }
        }
	}

	private static class PrintListingPainter implements Printable {
		private RandomAccessFile raf;
		private Font fnt;
		private int fontsize;
		private int rememberedPageIndex = -1;
		private long rememberedFilePointer = -1;
		private boolean rememberedEOF = false;

		public PrintListingPainter(File file, String font, int fontsize) {
			this.fontsize = fontsize;
			fnt = new Font(font, Font.PLAIN, fontsize);
			try {
				raf = new RandomAccessFile(file, "r");
			} catch (Exception e) { rememberedEOF = true; }
		}

		public int print(Graphics g, PageFormat pf, int pageIndex)
			throws PrinterException {
			try {
				// For catching IOException
				if (pageIndex != rememberedPageIndex) {
					// First time we've visited this page
					rememberedPageIndex = pageIndex;
					// If encountered EOF on previous page, done
					if (rememberedEOF) return Printable.NO_SUCH_PAGE;
					// Save current position in input file
					rememberedFilePointer = raf.getFilePointer();
				}
				else raf.seek(rememberedFilePointer);
				g.setColor(Color.black);
				g.setFont(fnt);
				int x = (int) pf.getImageableX() + fontsize;
				int y = (int) pf.getImageableY() + fontsize;
				// // Title line
				// g.drawString("File: " + fileName + ", page: " + (pageIndex+1),  x, y);
				// Generate as many lines as will fit in imageable area
				//y += 36;
				while (y + fontsize + 2 < pf.getImageableY()+pf.getImageableHeight()) {
					String line = raf.readLine();
					if (line == null) {
						rememberedEOF = true;
						break;
					}
					g.drawString(line, x, y);
					y += fontsize + 2;
				}
				return Printable.PAGE_EXISTS;
			}
			catch (Exception e) { return Printable.NO_SUCH_PAGE;}
		}
	}
}
