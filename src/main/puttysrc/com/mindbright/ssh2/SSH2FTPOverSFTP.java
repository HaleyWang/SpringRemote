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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import com.mindbright.net.ftp.FTPServer;
import com.mindbright.net.ftp.FTPServerEventHandler;
import com.mindbright.net.ftp.FTPException;

/**
 * Implements a proxy which proxies between an ftp client and an sftp server.
 */
public class SSH2FTPOverSFTP implements FTPServerEventHandler {

    /**
     * The <code>SSH2Connection</code> to use
     */
    protected SSH2Connection connection;
    
    /**
     * SFTP client instance
     */
    protected SSH2SFTPClient sftp;

    /**
     * FTP server instance
     */
    protected FTPServer      ftp;

    private String         remoteDir;
    private String         renameFrom;
    private String         user;
    
    private SSH2SFTP.FileAttributes attrs;
    private boolean remoteIsWindows;

    protected SSH2FTPOverSFTP(InputStream ftpInput, OutputStream ftpOutput,
                              String identity, boolean needPassword) {
        initFTP(ftpInput, ftpOutput, false, identity, needPassword);
    }

    public SSH2FTPOverSFTP(SSH2Connection connection,
                           InputStream ftpInput, OutputStream ftpOutput,
                           String identity)
    throws SSH2SFTP.SFTPException {
        this(connection, ftpInput, ftpOutput, true, identity);
    }

    public SSH2FTPOverSFTP(SSH2Connection connection,
                           InputStream ftpInput, OutputStream ftpOutput,
                           boolean remoteIsWindows, String identity)
    throws SSH2SFTP.SFTPException {
        initSFTP(connection);
        initFTP(ftpInput, ftpOutput, remoteIsWindows, identity, false);
    }

    /**
     * Connect this instance with an <code>SSH2Connection</code> which is
     * connected to the server we want to transfer files to/from.
     *
     * @param connection Established connection to the server.
     */
    protected void initSFTP(SSH2Connection connection)
    throws SSH2SFTP.SFTPException {
        this.connection = connection;
        this.attrs      = null;
        try {
            this.sftp = new SSH2SFTPClient(connection, false);
        } catch (SSH2SFTP.SFTPException e) {
            if (ftp != null)
                ftp.terminate();
            throw e;
        }
    }

    /**
     * Initialize the FTP server portion of this class.
     *
     * @param ftpInput The ftp command input stream.
     * @param ftpOutput The ftp command output stream.
     * @param identity Username to log in as
     * @param needPassword Tells the instance if it should request a
     *                     password or not from the user. The actual password
     *                     the user then gives is ignored.
     */
    protected void initFTP(InputStream ftpInput, OutputStream ftpOutput,
                           String identity, boolean needPassword) {
        initFTP(ftpInput, ftpOutput, false, identity, needPassword);
    }
    
    /**
     * Initialize the FTP server portion of this class.
     *
     * @param ftpInput The ftp command input stream.
     * @param ftpOutput The ftp command output stream.
     * @param remoteIsWindows Whether remote system is a Windows system or not.
     * @param identity Username to log in as
     * @param needPassword Tells the instance if it should request a
     *                     password or not from the user. The actual password
     *                     the user then gives is ignored.
     */
    protected void initFTP(InputStream ftpInput, OutputStream ftpOutput,
                           boolean remoteIsWindows,
                           String identity, boolean needPassword) {
        this.remoteIsWindows = remoteIsWindows;
        this.ftp = new FTPServer(identity, this, ftpInput, ftpOutput,
                                 needPassword);
    }

    /**
     * Login to server. This is actually a null operation for this class
     * since the user is already authenticated as part of the SSH connection.
     *
     * @param user Username to login as.
     * @param pass Password.
     *
     * @return true if the login was successful.
     */
    public boolean login(String user, String pass) {
        connection.getLog().notice("SSH2FTPOverSFTP", "user " + user + " login");
        try {
            attrs = sftp.realpath(".");
        } catch (SSH2SFTP.SFTPException e) {
            // !!! TODO, should disconnect ???
            return false;
        }
        remoteDir = attrs.name;
        this.user = user;
        return true;
    }

    public void quit() {
        connection.getLog().notice("SSH2FTPOverSFTP", "user " + user + " logout");
        sftp.terminate();
    }

    public boolean isPlainFile(String file) {
        try {
            file  = expandRemote(file);
            attrs = sftp.lstat(file);
            return attrs.isFile();
        } catch (SSH2SFTP.SFTPException e) {
            return false;
        }
    }

    public void changeDirectory(String dir) throws FTPException {
        if(dir != null) {
            String newDir = expandRemote(dir);
            try {
                attrs = sftp.realpath(newDir);
            } catch (SSH2SFTP.SFTPException e) {
                throw new FTPException(550, dir + ": No such directory.");
            }
            newDir = attrs.name;
            try {
                SSH2SFTP.FileHandle f = sftp.opendir(newDir);
                sftp.close(f);
            } catch (SSH2SFTP.SFTPException e) {
                throw new FTPException(550, dir + ": Not a directory.");
            }
            remoteDir = newDir;
        }
    }

    public void renameFrom(String from) throws FTPException {
        try {
            String fPath = expandRemote(from);
            attrs = sftp.lstat(fPath);
            renameFrom = fPath;
        } catch (SSH2SFTP.SFTPException e) {
            throw new FTPException(550, from + ": No such file or directory.");
        }
    }

    public void renameTo(String to) throws FTPException {
        if(renameFrom != null) {
            try {
                sftp.rename(renameFrom, expandRemote(to));
            } catch (SSH2SFTP.SFTPException e) {
                throw new FTPException(550, "rename: Operation failed.");
            } finally {
                renameFrom = null;
            }
        } else {
            throw new FTPException(503, "Bad sequence of commands.");
        }
    }

    public void delete(String file) throws FTPException {
        try {
            sftp.remove(expandRemote(file));
        } catch (SSH2SFTP.SFTPException e) {
            String msg = (e instanceof SSH2SFTP.SFTPPermissionDeniedException) ?
                         "access denied." : file + ": no such file.";
            throw new FTPException(550, msg);
        }
    }

    public void rmdir(String dir) throws FTPException {
        try {
            sftp.rmdir(expandRemote(dir));
        } catch (SSH2SFTP.SFTPException e) {
            String msg = (e instanceof SSH2SFTP.SFTPPermissionDeniedException) ?
                         "access denied." : dir + ": no such directory.";
            throw new FTPException(550, msg);
        }
    }

    public void mkdir(String dir) throws FTPException {
        try {
            sftp.mkdir(expandRemote(dir), new SSH2SFTP.FileAttributes());
        } catch (SSH2SFTP.SFTPException e) {
        }
    }

    public void chmod(int mod, String file) throws FTPException {
        try { 
            SSH2SFTP.FileAttributes fa = new SSH2SFTP.FileAttributes();
            fa.permissions = mod;
            fa.hasPermissions = true;
            sftp.setstat(expandRemote(file), fa);
        } catch (SSH2SFTP.SFTPException e) {
        }
    }
    
    public String pwd() {
        return remoteDir;
    }

    public String system() {
        return "UNIX Type: L8";
    }

    public long modTime(String file) throws FTPException {
        return (timeAndSize(file))[0];
    }

    public long size(String file) throws FTPException {
        return (timeAndSize(file))[1];
    }

    private long[] timeAndSize(String file) throws FTPException {
        try {
            long[] ts = new long[2];
            String fPath = expandRemote(file);
            attrs = sftp.lstat(fPath);
            if(!attrs.hasSize || !attrs.hasModTime) {
                throw new FTPException(550,
                                       "SFTP server don't return time/size.");
            }
            ts[0] = attrs.mtime * 1000L;
            ts[1] = attrs.size;
            return ts;
        } catch (SSH2SFTP.SFTPException e) {
            throw new FTPException(550, file + ": No such file or directory.");
        }
    }

    private static int ENDPOINT_UNIX    = 0;
    private static int ENDPOINT_WINDOWS = 1;

    private static class ASCIIFilterIn extends InputStream {
        private int type, bufch = -1;
        private InputStream in;

        ASCIIFilterIn(InputStream in, int type) {
            super();
            this.in = in;
            this.type = type;
        }
        
        public int read() throws IOException {
            int ch, ch2;
            if (bufch != -1) {
                ch = bufch;
                bufch = -1;
                return ch;
            }

            for (;;) {
                ch = in.read();
                if (ch == -1) break;
                if (type == ENDPOINT_UNIX) {
                    if (ch == '\r') {
                        ch2 = in.read();
                        if (ch2 == '\n') {
                            ch = ch2;
                        } else {
                            bufch = ch2;
                        }
                    }
                } else if (type == ENDPOINT_WINDOWS) {
                    if (ch == '\r') {
                        bufch = in.read();
                    } else if (ch == '\n') {
                        bufch = '\n';
                        ch = '\r';
                    }
                } 
                break;
            }
            return ch;
        }

        public void close() throws IOException { in.close(); }
    }

    private static class ASCIIFilterOut extends OutputStream {
        private int type;
        private OutputStream out;
        private boolean prevcr = false;

        ASCIIFilterOut(OutputStream out, int type) {
            super();
            this.out = out;
            this.type = type;
        }
        
        public void write(int ch) throws IOException {
            if (type == ENDPOINT_UNIX) {
                if (prevcr && ch != '\n')
                    out.write('\r');
                prevcr = false;
                if (ch == '\r') {
                    prevcr = true;
                    return;
                }
            } else if (type == ENDPOINT_WINDOWS) {
                if (ch == '\r') {
                    prevcr = true;
                } else {
                    if (ch == '\n') {
                        if (!prevcr)
                            write('\r');
                    }
                    prevcr = false;
                }               
            }
            out.write(ch);
        }

        public void flush() throws IOException {
            out.flush();
        }

        public void close() throws IOException {
            if (prevcr) out.write('\r');
            out.close();
        }
    }

    public void append(String file, InputStream data, boolean binary)
    throws FTPException {
	store(file, data, binary, true);
    }

    public void store(String file, InputStream data, boolean binary)
    throws FTPException {
	store(file, data, binary, false);
    }

    private void store(String file, InputStream data, boolean binary, boolean append)
    throws FTPException {
        SSH2SFTP.FileHandle handle = null;
        try { 
	    int flags = SSH2SFTP.SSH_FXF_WRITE | SSH2SFTP.SSH_FXF_CREAT;
	    flags |= (append ? SSH2SFTP.SSH_FXF_APPEND : SSH2SFTP.SSH_FXF_TRUNC);
            file = expandRemote(file);
            handle = sftp.open(file, flags, new SSH2SFTP.FileAttributes());
            if (!binary)
                data = new ASCIIFilterIn
                    (data, remoteIsWindows ? ENDPOINT_WINDOWS : ENDPOINT_UNIX);
            sftp.writeFully(handle, data, append);            
        } catch (IOException e) {
            throw new FTPException(425, "Error writing to data connection: " +
                                   e.getMessage());
        } catch (SSH2SFTP.SFTPPermissionDeniedException e) {
            throw new FTPException(553, file + ": Permission denied.");
        } catch (SSH2SFTP.SFTPException e) {
            throw new FTPException(550, file + ": Error in sftp connection, " +
                                   e.getMessage());
        } finally {
            try {
                data.close();
            } catch (Exception e) { /* don't care */
            }
        }
    }

    public void retrieve(String file, OutputStream data, boolean binary)
    throws FTPException {
        SSH2SFTP.FileHandle handle = null;
        try {
            String eFile = expandRemote(file);
            handle = sftp.open(eFile, SSH2SFTP.SSH_FXF_READ,
                               new SSH2SFTP.FileAttributes());
            if (!binary) {
                String os = System.getProperty("os.name");
                if (os == null) os = "";
                os = os.toLowerCase();
                int type = os.startsWith("win") ? ENDPOINT_WINDOWS :  ENDPOINT_UNIX;
                data = new ASCIIFilterOut(data, type);
            }
            sftp.readFully(handle, data);

        } catch (SSH2SFTP.SFTPNoSuchFileException e) {
            throw new FTPException(550, file + ": No such file or directory.");
        } catch (SSH2SFTP.SFTPException e) {
            throw new FTPException(550, file + ": Error in sftp connection, " +
                                   e.getMessage());
        } catch (IOException e) {
            throw new FTPException(550, file + ": Error in sftp connection, " +
                                   e.getMessage());
        } finally {
            try {
                data.close();
            } catch (Exception e) { /* don't care */
            }
        }
    }

    public void list(String path, OutputStream data) throws FTPException {
        try {
            SSH2SFTP.FileAttributes[] list = dirList(path);
            String dir = path != null ? path : remoteDir;
            for(int i = 0; i < list.length; i++) {
                if(".".equals(list[i].name) || "..".equals(list[i].name)) {
                    continue;
                }
                if (list[i].isLink() && list[i].ldest == null) {
                    try {
                        list[i].ldest = sftp.readlink(dir+'/'+list[i].name);
                    } catch (SSH2SFTP.SFTPException e) {}
                }
                String row = list[i].toString();
                if (row.endsWith("/")) {
                    row = row.substring(0, row.length() - 1);
                }
                row += "\r\n";
                data.write(row.getBytes());
            }
        } catch (IOException e) {
            throw new FTPException(425, "Error writing to data connection: " +
                                   e.getMessage());
        }
    }

    public void nameList(String path, OutputStream data) throws FTPException {
        // !!! TODO some *-expansion maybe
        try {
            SSH2SFTP.FileAttributes[] list = dirList(path);
            for(int i = 0; i < list.length; i++) {
                if(".".equals(list[i].name) || "..".equals(list[i].name)) {
                    continue;
                }
                String row = list[i].name + "\r\n";
                data.write(row.getBytes());
            }
        } catch (IOException e) {
            throw new FTPException(425, "Error writing to data connection: " +
                                   e.getMessage());
        }
    }

    private SSH2SFTP.FileAttributes[] dirList(String path) throws FTPException {
        SSH2SFTP.FileHandle       handle = null;
        SSH2SFTP.FileAttributes[] list   = new SSH2SFTP.FileAttributes[0];

        try {
            String fPath = expandRemote(path);
            attrs = sftp.lstat(fPath);
            if(attrs.isDirectory()) {
                handle = sftp.opendir(fPath);
                list = sftp.readdir(handle);
            } else {
                list            = new SSH2SFTP.FileAttributes[1];
                list[0]         = attrs;
                list[0].name    = path;
		list[0].hasName = true;
            }
        } catch (SSH2SFTP.SFTPException e) {
            throw new FTPException(550, path + ": Not a directory.");
        } finally {
            try {
                if(handle != null)
                    sftp.close(handle);
            } catch (Exception e) { /* don't care */
            }
        }

        return list;
    }

    public void abort() {
        // !!! TODO !!!
    }

    private String expandRemote(String name) {
        if(name == null || name.length() == 0) {
            return remoteDir;
        }
        if(name.charAt(0) != '/')
            name = remoteDir + "/" + name;
        return name;
    }

}
