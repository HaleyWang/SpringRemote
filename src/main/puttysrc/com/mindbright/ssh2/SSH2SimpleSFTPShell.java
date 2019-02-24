/******************************************************************************
 *
 * Copyright (c) 1999-2011 Cryptzone Group AB. All Rights Reserved.
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

package com.mindbright.ssh2;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;
import java.io.FileInputStream;

import com.mindbright.terminal.TerminalWin;
import com.mindbright.terminal.LineReaderTerminal;

import javax.swing.JFrame;

/**
 * This class implements a basic interactive sftp session. It opens a
 * terminal window in which the user may interact with sftp using
 * typed commands.
 */
public final class SSH2SimpleSFTPShell implements Runnable {
    JFrame             frame;
    TerminalWin        terminal;
    LineReaderTerminal linereader;
    SSH2Connection     connection;
    SSH2SFTPClient     sftp;

    String localDir;
    String remoteDir;

    /**
     * Class implementing a progress bar which gets printed in a
     * terminal window.
     */
    public static final class ProgressBar implements SSH2SFTP.AsyncListener {
        LineReaderTerminal linereader;
        long               totalSize;
        long               curSize;
        long               start;
        static final String[] prefix = { "", "k", "M", "G", "T" };
        public ProgressBar(LineReaderTerminal linereader) {
            this.linereader = linereader;
        }
        public void start(long totalSize) {
            this.totalSize = totalSize;
            this.curSize   = 0;
            start = System.currentTimeMillis();
        }
        public void progress(long size) {
            long now = System.currentTimeMillis();

            curSize += size;
            int perc = (int)(totalSize > 0 ?
                             ((100 * curSize) / totalSize) : 100);
            int i;
            StringBuilder buf = new StringBuilder();

            buf.append("\r " + perc + "%\t|");
            int len = (int)(32 * ((double)perc / 100));

            for(i = 0; i < len; i++) {
                buf.append('*');
            }
            for(i = len; i < 32; i++) {
                buf.append(' ');
            }
            long printSize = curSize;
            i = 0;
            while(printSize >= 100000) {
                i++;
                printSize >>>= 10;
            }
            buf.append("|  " + printSize + " " + prefix[i] + "bytes");

            long elapsed = (now - start);
            if(elapsed == 0) {
                elapsed = 1;
            }

            int curSpeed = (int)(curSize / ((double)elapsed / 1000));

            i = 0;
            while(curSpeed >= 100000) {
                i++;
                curSpeed >>>= 10;
            }
            buf.append(" (" + curSpeed + " " + prefix[i] + "B/sec)   ");

            linereader.print(buf.toString());
        }
    }

    /**
     * This constructor needs an open connection to the server and a
     * window name. It will create an sftp session on the connection
     * and popup a new terminal window with the given title. It will
     * also spawn a thread to run the sftp session.
     *
     * @param connection The connection to use.
     * @param title Title of window.
     */
    public SSH2SimpleSFTPShell(SSH2Connection connection, String title) {
        this.connection = connection;
        this.frame      = new JFrame();
        this.terminal   = new TerminalWin(frame);
        linereader      = new LineReaderTerminal(terminal);

        frame.setLayout(new BorderLayout());
        frame.add(terminal.getPanelWithScrollbar(),
                  BorderLayout.CENTER);

        frame.setTitle(title);

        frame.addWindowListener(new WindowAdapter() {
                                    public void windowClosing(WindowEvent e) {
                                        linereader.breakPromptLine("Exit");
                                        sftp.terminate();
                                    }
                                }
                               );

        frame.pack();
        frame.setVisible(true);

        Thread shell = new Thread(this);

        shell.setDaemon(true);
        shell.start();
    }

    /**
     * The thread running this gets created in the constructor. So
     * there is no need to call this function explicitely.
     */
    public void run() {
        linereader.println("Starting sftp...");

        doHelp();

        try {
            sftp = new SSH2SFTPClient(connection, false);

            File f = new File(".");
            localDir = f.getCanonicalPath();

            SSH2SFTP.FileAttributes attrs = sftp.realpath(".");
            remoteDir = attrs.name;

            linereader.println("local dir:\t" + localDir);
            linereader.println("remote dir:\t" + remoteDir);

            boolean keepRunning = true;
            while(keepRunning) {
                try {
                    String   cmdLine = linereader.promptLine("sftp> ", null,
                                       false);
                    if (cmdLine == null) {
                        continue;
                    }
                    SSH2SFTP.FileHandle handle = null;
                    String[] argv;
                    String   cmd;
                    cmdLine = cmdLine.trim();

                    if(cmdLine.equals("")) {
                        doHelp();
                        continue;
                    }

                    argv = makeArgv(cmdLine);
                    cmd  = argv[0];

                    if(cmd.equals("ls")) {
                        try {
                            handle = sftp.opendir(remoteDir);
                            SSH2SFTP.FileAttributes[] list = sftp.readdir(handle);
                            for(int i = 0; i < list.length; i++) {
                                linereader.println(list[i].toString());
                            }
                        } finally {
                            try {
                                sftp.close(handle);
                            } catch (Exception e) { /* don't care */
                            }
                        }

                    } else if(cmd.equals("cd")) {
                        if(argv.length < 2) {
                            doHelp();
                            continue;
                        }
                        String newDir = expandRemote(argv[1]);
                        try {
                            attrs = sftp.realpath(newDir);
                        } catch (SSH2SFTP.SFTPException e) {
                            linereader.println(newDir +
                                               ": No such remote directory.");
                            continue;
                        }
                        newDir = attrs.name;
                        try {
                            handle = sftp.opendir(newDir);
                            sftp.close(handle);
                            remoteDir = newDir;
                            linereader.println("remote: " + remoteDir);
                        } catch (SSH2SFTP.SFTPException e) {
                            linereader.println(newDir + ": Not a remote directory.");
                        }

                    } else if(cmd.equals("lls")) {
                        f = new File(localDir);
                        String[] list = f.list();
                        if(list == null) {
                            linereader.print(localDir +
                                             " is empty or not accessible");
                            continue;
                        }

                        for(int i = 0; i < list.length; i++) {
                            f = new File(localDir + File.separator + list[i]);
                            linereader.print((f.isDirectory() ? "d" : "-") +
                                             (f.canRead() ? "r" : "-") +
                                             (f.canWrite() ? "w" : "-") + "\t");
                            linereader.print(f.length() + "\t");

                            java.util.Date d = new java.util.Date(f.lastModified());
                            java.text.DateFormat df =
                                java.text.DateFormat.getDateInstance();
                            linereader.print(df.format(d) + "\t");
                            linereader.println(list[i]);
                        }

                    } else if(cmd.equals("lcd")) {
                        if(argv.length < 2) {
                            doHelp();
                            continue;
                        }
                        String newDir;
                        newDir = expandLocal(argv[1]);
                        f = new File(newDir);
                        if(f.exists() && f.isDirectory() && f.canRead()) {
                            localDir = f.getCanonicalPath();
                            linereader.println("local: " + localDir);
                        } else {
                            linereader.println(newDir + ": No such local directory");
                        }

                    } else if(cmd.equals("get")) {
                        if(argv.length < 2) {
                            doHelp();
                            continue;
                        }

                        String localFile  = argv[1]; // Default to same name
                        String remoteFile = expandRemote(localFile);

                        localFile = localFile.replace('/', File.separatorChar);

                        if(argv.length > 2) {
                            localFile = argv[2];
                        }
                        localFile = expandLocal(localFile);

                        handle = sftp.open(remoteFile,
                                           SSH2SFTP.SSH_FXF_READ,
                                           new SSH2SFTP.FileAttributes());

                        attrs = sftp.fstat(handle);

                        linereader.println("download from remote '" +
                                           remoteFile + "',");
                        linereader.println("         to local '" +
                                           localFile + "', " + attrs.size +
                                           " bytes");

                        java.io.RandomAccessFile file =
                            new java.io.RandomAccessFile(localFile, "rw");

                        int len   = (int)attrs.size;
                        int foffs = 0;
                        int cnt   = 0;

                        ProgressBar bar = new ProgressBar(linereader);
                        handle.addAsyncListener(bar);
                        bar.start(len);

                        while(foffs < len) {
                            int n = (32768 < (len - foffs)) ? 32768 : len - foffs;
                            sftp.read(handle, foffs, file, n);
                            foffs += n;
                            if(++cnt == 24) {
                                cnt = 0;
                                handle.asyncWait(12);
                            }
                            if(linereader.ctrlCPressed()) {
                                handle.asyncClose();
                            }
                        }
                        handle.asyncWait();

                        linereader.println("");

                        file.close();
                        sftp.close(handle);

                    } else if(cmd.equals("put")) {
                        if(argv.length < 2) {
                            doHelp();
                            continue;
                        }
                        String remoteFile  = argv[1]; // Default to same name
                        String localFile = expandLocal(remoteFile);

                        remoteFile = remoteFile.replace(File.separatorChar,
                                                        '/');

                        if(argv.length > 2) {
                            remoteFile = argv[2];
                        }
                        remoteFile = expandRemote(remoteFile);

                        handle = sftp.open(remoteFile,
                                           SSH2SFTP.SSH_FXF_WRITE |
                                           SSH2SFTP.SSH_FXF_TRUNC |
                                           SSH2SFTP.SSH_FXF_CREAT,
                                           new SSH2SFTP.FileAttributes());
                        f = new File(localFile);
                        FileInputStream fin = new FileInputStream(f);

                        linereader.println("upload to remote '" +
                                           remoteFile + "',");
                        linereader.println("       from local '" +
                                           localFile + "', " + f.length() +
                                           " bytes");

                        ProgressBar bar = new ProgressBar(linereader);
                        handle.addAsyncListener(bar);
                        bar.start((int)f.length());

                        sftp.writeFully(handle, fin);

                        /* !!! Can't interrupt now !!!
                            if(linereader.ctrlCPressed()) {
                        	handle.asyncClose();
                            }
                        */

                        linereader.println("");
                        fin.close();

                    } else if(cmd.equals("pwd")) {
                        linereader.println("local dir:\t" + localDir);
                        linereader.println("remote dir:\t" + remoteDir);

                    } else if(cmd.equals("rm")) {
                        if(argv.length < 2) {
                            doHelp();
                            continue;
                        }
                        sftp.remove(expandRemote(argv[1]));

                    } else if(cmd.startsWith("ren")) {
                        if(argv.length < 3) {
                            doHelp();
                            continue;
                        }
                        sftp.rename(expandRemote(argv[1]),
                                    expandRemote(argv[2]));

                    } else if(cmd.startsWith("lren")) {
                        if(argv.length < 3) {
                            doHelp();
                            continue;
                        }
                        f = new File(expandLocal(argv[1]));
                        f.renameTo(new File(expandLocal(argv[2])));

                    } else if(cmd.equals("lrm") || cmd.equals("lrmdir")) {
                        if(argv.length < 2) {
                            doHelp();
                            continue;
                        }
                        f = new File(expandLocal(argv[1]));
                        f.delete();

                    } else if(cmd.equals("rmdir")) {
                        if(argv.length < 2) {
                            doHelp();
                            continue;
                        }
                        sftp.rmdir(expandRemote(argv[1]));

                    } else if(cmd.equals("mkdir")) {
                        if(argv.length < 2) {
                            doHelp();
                            continue;
                        }
                        sftp.mkdir(expandRemote(argv[1]),
                                   new SSH2SFTP.FileAttributes());

                    } else if(cmd.equals("lmkdir")) {
                        if(argv.length < 2) {
                            doHelp();
                            continue;
                        }
                        f = new File(expandLocal(argv[1]));
                        f.mkdir();

                    } else if(cmd.equals("q") || cmd.equals("quit")) {
                        linereader.println("exiting...");
                        keepRunning = false;

                    } else {
                        doHelp();
                    }
                } catch (SSH2SFTP.SFTPNoSuchFileException e) {
                    linereader.println("No such file or directory");
                } catch (SSH2SFTP.SFTPPermissionDeniedException e) {
                    linereader.println("Permission denied");
                } catch (SSH2SFTP.SFTPAsyncAbortException e) {
                    linereader.println("");
                    linereader.println("Async transfer aborted");
                }
            }
        } catch (SSH2SFTP.SFTPDisconnectException e) {
            /* Exit because session was closed */
        } catch (SSH2SFTP.SFTPException e) {
            String msg = e.getMessage();
            linereader.println("sftp error: " + ((msg != null &&
                                                  msg.length() > 0) ? msg :
                                                 e.toString()));
        } catch (LineReaderTerminal.ExternalMessageException e) {
            /* Exit on window close */
            frame.dispose();
            return;
        } catch (Exception e) {
            System.out.println("Fatal error in SFTP:");
            e.printStackTrace();
            linereader.println("fatal error: " + e);
        }

        try {
            linereader.promptLine("\n\rPress <return> to exit sftp shell", null,
                                  false);
        } catch (LineReaderTerminal.ExternalMessageException e) {}

        if(sftp != null) {
            sftp.terminate();
        }
        frame.dispose();
    }

    /**
     * If needed, expand the given filename to include full path on
     * the remote side.
     *
     * @param name Name to expand
     *
     * @return A full path.
     */
    public String expandRemote(String name) {
        if(name.charAt(0) != '/')
            name = remoteDir + "/" + name;
        return name;
    }

    /**
     * If needed, expand the given filename to include full path on
     * the local side.
     *
     * @param name Name to expand
     *
     * @return A full path.
     */
    public String expandLocal(String name) {
        if(name.length() > 1 &&
                name.charAt(0) != File.separatorChar &&
                name.charAt(1) != ':') {
            name = localDir + File.separator + name;
        }
        return name;
    }

    /**
     * Extract the next argument from a space-separated list of arguments.
     *
     * @param args List of arguments. This list is modified in the process.
     *
     * @return The fisr argument in the list.
     */
    public String getNextArg(String args) {
        int i = args.indexOf(' ');
        if(i > -1)
            args = args.substring(0, i);
        return args;
    }

    /**
     * Split a command line into arguments. This will split the given
     * command line into individual arguments. The arguments are
     * assumed to be separated by spaces. There is no quote handling
     * or escape mechanism.
     *
     * @param cmdLine Command line to extract arguments from.
     *
     * @return An ordered array of arguments.
     */
    public String[] makeArgv(String cmdLine) {
        String[] argv = new String[32];
        String[] argvRet;
        int n = 0, i;
        while(cmdLine != null) {
            argv[n++] = getNextArg(cmdLine);
            i = cmdLine.indexOf(' ');
            if(i > -1) {
                cmdLine = cmdLine.substring(i);
                cmdLine = cmdLine.trim();
            } else
                cmdLine = null;
        }
        argvRet = new String[n];
        System.arraycopy(argv, 0, argvRet, 0, n);
        return argvRet;
    }

    /**
     * Print help text.
     */
    public void doHelp() {
        linereader.println("");
        linereader.println("The following commands are available:");
        linereader.println("");
        linereader.println("pwd\t\t\t\t\tshow current local/remote directory");
        linereader.println("cd <dir>\t\t\t\tchange current directory (remote)");
        linereader.println("lcd <dir>\t\t\t\tchange current directory (local)");
        linereader.println("ls\t\t\t\t\tlist current directory (remote)");
        linereader.println("lls\t\t\t\t\tlist current directory (local)");
        linereader.println("get <remote-file> [<local-file>]\tdownload file from remote host");
        linereader.println("put <local-file> [<remote-file>]\tupload file to remote host");
        linereader.println("ren <from-file> <to-file>\t\trename remote file");
        linereader.println("lren <from-file> <to-file>\t\trename local file");
        linereader.println("rm <remote-file>\t\t\tremove remote file");
        linereader.println("lrm <local-file>\t\t\tremove local file");
        linereader.println("mkdir <remote-dir>\t\t\tcreate remote directory");
        linereader.println("lmkdir <local-dir>\t\t\tcreate local directory");
        linereader.println("rmdir <remote-dir>\t\t\tremove remote directory");
        linereader.println("lrmdir <local-dir>\t\t\tremove local directory");
        linereader.println("quit\t\t\t\t\tquit this sftp session");
        linereader.println("");
    }

    public TerminalWin getTerminal() {
        return terminal;
    }

    /*
     SSH2SFTPClient sftp = new SSH2SFTPClient(connection);
     SSH2SFTP.FileHandle handle = sftp.opendir(".");
     SSH2SFTP.FileAttributes[] list = sftp.readdir(handle);

     System.out.println("length: " + list.length);
     for(i = 0; i < list.length; i++) {
    System.out.println(list[i].lname);
     }

     SSH2SFTP.FileAttributes attrs = sftp.stat(".");
     System.out.println("attrs: " + attrs);

     attrs = sftp.realpath(".");
     System.out.println("lname-rp: " + attrs.lname);
     System.out.println("attrs-rp: " + attrs);

     handle = sftp.open("/tmp/hostkey", SSH2SFTP.SSH_FXF_READ,
          new SSH2SFTP.FileAttributes());
     java.io.FileOutputStream fout = new java.io.FileOutputStream("/tmp/keycopy");
     byte[] buf = new byte[1024];
     long foffs = 0;
     int len;
     while((len = sftp.read(handle, foffs, buf, 0, 1024)) > 0) {
    System.out.println("read: " + len);
    fout.write(buf, 0, len);
    foffs += len;
     }
     sftp.close(handle);
     fout.close();

     handle = sftp.open("/tmp/foobar", SSH2SFTP.SSH_FXF_WRITE |
          SSH2SFTP.SSH_FXF_CREAT,
          new SSH2SFTP.FileAttributes());
     FileInputStream fin = new FileInputStream("/tmp/keycopy");
     buf = new byte[1024];
     foffs = 0;
     while((len = fin.read(buf, 0, 1024)) > 0) {
    System.out.println("wrote: " + len);
    sftp.write(handle, foffs, buf, 0, len);
    foffs += len;
     }
     sftp.close(handle);
     fin.close();

     sftp.terminate();

    */

}
