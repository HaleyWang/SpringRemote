/******************************************************************************
 *
 * Copyright (c) 1999-2011 Cryptzone AB. All Rights Reserved.
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

package com.mindbright.application;

import java.awt.BorderLayout;
import java.awt.Frame;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JMenuBar;

import com.mindbright.terminal.GlobalClipboard;
import com.mindbright.terminal.TerminalFrameTitle;
import com.mindbright.terminal.TerminalMenuHandler;
import com.mindbright.terminal.TerminalMenuHandlerFull;
import com.mindbright.terminal.TerminalMenuListener;
import com.mindbright.terminal.TerminalWin;
import com.mindbright.terminal.TerminalWindow;

public abstract class ModuleBaseTerminal extends WindowAdapter
    implements MindTermModule, Runnable, TerminalMenuListener {

    protected MindTermApp mindterm;
    private Vector<ModuleBaseTerminal> instances;

    public void init(MindTermApp mindterm) {
        this.mindterm = mindterm;
    }

    protected boolean haveMenus() {
        return Boolean.valueOf(mindterm.getProperty("havemenus")).
            booleanValue();
    }

    protected boolean useChaff() {
        return Boolean.valueOf(mindterm.getProperty("key-timing-noise")).
               booleanValue();
    }

    public void activate(MindTermApp mindterm) {
        if(instances == null) {
            instances = new Vector<ModuleBaseTerminal>();
        }
        ModuleBaseTerminal bt = newInstance();
        instances.addElement(bt);
        bt.init(mindterm);
        Thread t = new Thread(bt, "Terminal_" + this.getClass());
        t.start();
    }

    public void run() {
        JFrame frame = new JFrame();

        TerminalWin terminal = new TerminalWin(frame,
                                               mindterm.getProperties());

        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(terminal.getPanelWithScrollbar(),
                                   BorderLayout.CENTER);

        TerminalFrameTitle frameTitle =
            new TerminalFrameTitle(frame, getTitle());
        frameTitle.attach(terminal);

        TerminalMenuHandler tmenus = null;

        if(haveMenus()) {
            try {
                tmenus = new TerminalMenuHandlerFull();
                tmenus.setTitleName(mindterm.getAppName());

                JMenuBar mb = new JMenuBar();
                frame.setJMenuBar(mb);
                tmenus.addBasicMenus(terminal, mb);
                tmenus.setTerminalMenuListener(this);
            } catch (Throwable t) {
                /* no menus... */
                t.printStackTrace();
            }
        } else {
            terminal.setClipboard(GlobalClipboard.getClipboardHandler());
        }

        frame.addWindowListener(this);

        frame.pack();
        frame.setVisible(true);

        try {
            runTerminal(mindterm, terminal, frame, frameTitle);
        } finally {
            frame.dispose();
            if(haveMenus() && tmenus != null) {
                GlobalClipboard.getClipboardHandler().removeMenuHandler(tmenus);
            }
            instances = null;
            mindterm = null;
        }
    }

    public void connected(MindTermApp mindterm) {}

    public void disconnected(MindTermApp mindterm) {
        if(instances != null) {
            Enumeration<ModuleBaseTerminal> e = instances.elements();
            while(e.hasMoreElements()) {
                ModuleBaseTerminal bt = e.nextElement();
                if(bt.closeOnDisconnect()) {
                    bt.doClose();
                    instances.removeElement(bt);
                }
            }
        }
    }

    public String description(MindTermApp mindterm) {
        return null;
    }

    public void windowClosing(WindowEvent e) {
        doClose();
    }

    public void close(TerminalMenuHandler originMenu) {
        doClose();
    }

    public void update() {}

    protected abstract void runTerminal(MindTermApp mindterm,
                                        TerminalWindow terminal, Frame frame,
                                        TerminalFrameTitle frameTitle);
    protected abstract boolean closeOnDisconnect();
    protected abstract String getTitle();
    protected abstract void doClose();
    protected abstract ModuleBaseTerminal newInstance();

}
