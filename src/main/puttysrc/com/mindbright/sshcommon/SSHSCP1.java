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

package com.mindbright.sshcommon;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.EOFException;

/**
 * Copy files using the scp1 protocol. The scp1 protocol is not
 * officially documented but it is very similar to the rcp
 * protocol. The protocol was first introduce together with version 1
 * of ssh. Hence the '1' in the name which is very confusing because
 * nothing prevents this protocol to run on top of SSHv2. In fact the
 * protocol is completely transport neutral and instead relies on the
 * presence of a compatible scp command on the server.
 */
public final class SSHSCP1 implements SSHFileTransfer {

    /**
     * SIze of chunks used when copying files
     */
    public final static int DEFAULT_COPY_BUFFER_SZ = 16384;

    private SSHConsoleRemote        remote;
    private OutputStream            out;
    private InputStream             in;
    private File                    cwd;
    private boolean                 verbose;
    private SSHFileTransferProgress progress;
    private byte[]                  copyBuf;

    /**
     * Create a new instance ready to copy files and which uses the
     * provided SSHConsoleRemote instance.
     *
     * @param cwd local directory names are specified relatively to.
     * @param remote the remote ssh console instance to transfer files
     * over
     * @param verbose if true the verbose progress is reported
     */
    public SSHSCP1(File cwd, SSHConsoleRemote remote, boolean verbose) {
        this.remote    = remote;
        this.cwd       = cwd;
        this.verbose   = verbose;
        this.copyBuf   = new byte[DEFAULT_COPY_BUFFER_SZ];
    }

    public void setProgress(SSHFileTransferProgress progress) {
        this.progress = progress;
    }

    public void abort() {
        remote.close(false);
    }

    /**
     * Copy a single local file or directory to the remote server
     *
     * @param localFile local file to copy
     * @param remoteFile remote name
     * @param recursive if true and the local file is a directory then
     * the entire content of that directory is copied as well.
     */
    public void copyToRemote(String localFile, String remoteFile,
                             boolean recursive)
    throws IOException {
        File lf = new File(localFile);

        if(!lf.isAbsolute())
            lf = new File(cwd, localFile);

        if(!lf.exists()) {
            throw new IOException("File: " + localFile + " does not exist");
        }
        if(!lf.isFile() && !lf.isDirectory()) {
            throw new IOException("File: " + localFile +
                                  " is not a regular file or directory");
        }
        if(lf.isDirectory() && !recursive) {
            throw new IOException("File: " + localFile +
                                  " is a directory, use recursive mode");
        }
        if(remoteFile == null || remoteFile.equals(""))
            remoteFile = ".";
        remoteConnect("scp " + (lf.isDirectory() ? "-d " : "") + "-t " +
                      (recursive ? "-r " : "") + (verbose ? "-v " : "") +
                      remoteFile);
        failsafeReadResponse("After starting remote scp");
        writeFileToRemote(lf, recursive);
        remote.close(true);
    }

    public void copyToRemote(String[] localFiles, String remoteFile,
                             boolean recursive)
    throws IOException {
        if(remoteFile == null || remoteFile.equals(""))
            remoteFile = ".";
        if(localFiles.length == 1) {
            copyToRemote(localFiles[0], remoteFile, recursive);
        } else {
            remoteConnect("scp " + "-d -t " + (recursive ? "-r " : "") +
                          (verbose ? "-v " : "") + remoteFile);
            readResponse("After starting remote scp");
            for(int i = 0; i < localFiles.length; i++) {
                File lf = new File(localFiles[i]);
                if(!lf.isAbsolute())
                    lf = new File(cwd, localFiles[i]);
                if(!lf.isFile() && !lf.isDirectory()) {
                    throw new IOException("File: " + lf.getName() +
                                          " is not a regular file or directory");
                }
                writeFileToRemote(lf, recursive);
            }
            remote.close();
        }
    }

	private static String buildFileList(String remoteFiles[]) {
        StringBuilder buf = new StringBuilder();
        for(int i = 0; i < remoteFiles.length; i++) {
            buf.append("\"");
            buf.append(remoteFiles[i]);
            buf.append("\" ");
        }
		return buf.toString().trim();
	}

    public void copyToLocal(String localFile, String remoteFiles[],
                            boolean recursive)
    throws IOException {
        copyToLocal(localFile, buildFileList(remoteFiles), recursive);
    }

    /**
     * Copy a single remote file or directory to the local system
     *
     * @param localFile destination file name
     * @param remoteFile remote file or directory to copy
     * @param recursive if true and the remote file is a directory then
     * the entire content of that directory is copied as well.
     */
    public void copyToLocal(String localFile, String remoteFile,
                            boolean recursive)
    throws IOException {
        if(localFile == null || localFile.equals(""))
            localFile = ".";

        File lf = new File(localFile);
        if(!lf.isAbsolute())
            lf = new File(cwd, localFile);

        if(lf.exists() && !lf.isFile() && !lf.isDirectory()) {
            throw new IOException("File: " + localFile +
                                  " is not a regular file or directory");
        }
        remoteConnect("scp " + "-f " + (recursive ? "-r " : "") +
                      (verbose ? "-v " : "") + remoteFile);
        readFromRemote(lf);
        remote.close(true);
    }

    private boolean writeDirToRemote(File dir, boolean recursive)
    throws IOException {
        if(!recursive) {
            writeError("File " + dir.getName() +
                       " is a directory, use recursive mode");
            return false;
        }
        writeString("D0755 0 " + dir.getName() + "\n");
        if(progress != null)
            progress.startDir(dir.getAbsolutePath());
        readResponse("After sending dirdata");
        String[] dirList = dir.list();
        for(int i = 0; i < dirList.length; i++) {
            File f = new File(dir, dirList[i]);
            writeFileToRemote(f, recursive);
        }
        writeString("E\n");
        if(progress != null)
            progress.endDir();
        return true;
    }

    private void writeFileToRemote(File file, boolean recursive)
    throws IOException {
        if(file.isDirectory()) {
            if(!writeDirToRemote(file, recursive))
                return;
        } else if(file.isFile()) {
            writeString("C0644 " + file.length() + " " + file.getName() + "\n");
            if(progress != null)
                progress.startFile(file.getName(), file.length());
            readResponse("After sending filedata");
            FileInputStream fi = new FileInputStream(file);
	    try {
		writeFully(fi, file.length());
		writeByte(0);
		if(progress != null)
		    progress.endFile();
	    } finally {
		fi.close();
	    }
        } else {
            throw new IOException("Not ordinary file: " + file.getName());
        }
        readResponse("After writing file");
    }

    private void readFromRemote(File file) throws IOException {
        String   cmd;
        String[] cmdParts = new String[3];
        writeByte(0);
        while(true) {
            try {
                cmd = readString();
            } catch (EOFException e) {
                return;
            }
            char cmdChar = cmd.charAt(0);
            switch(cmdChar) {
            case 'E':
                writeByte(0);
                return;
            case 'T':
                throw new IOException("SSHSCP1: (T)ime not supported: " + cmd);
            case 'C':
            case 'D':
                String targetName = file.getAbsolutePath();
                parseCommand(cmd, cmdParts);
                if(file.isDirectory()) {
                    targetName += File.separator + cmdParts[2];
                }
                File targetFile = new File(targetName);
                if(cmdChar == 'D') {
                    if(targetFile.exists()) {
                        if(!targetFile.isDirectory()) {
                            String msg = "Invalid target " +
                                         targetFile.getName() + ", must be a directory";
                            writeError(msg);
                            throw new IOException(msg);
                        }
                    } else {
                        if(!targetFile.mkdir()) {
                            String msg = "Could not create directory: " +
                                         targetFile.getName();
                            writeError(msg);
                            throw new IOException(msg);
                        }
                    }
                    if(progress != null)
                        progress.startDir(targetFile.getAbsolutePath());
                    readFromRemote(targetFile);
                    if(progress != null)
                        progress.endDir();
                    continue;
                }
                FileOutputStream fo = new FileOutputStream(targetFile);
                writeByte(0);
                long len = Long.parseLong(cmdParts[1]);
                if(progress != null)
                    progress.startFile(targetFile.getName(), len);
                readFully(fo, len);
                readResponse("SSHSCP.readFromRemote After reading file");
                if(progress != null)
                    progress.endFile();
                writeByte(0);
                break;
            default:
                writeError("Unexpected cmd: " + cmd);
                throw new IOException("Unexpected cmd: " + cmd);
            }
        }
    }

    private void parseCommand(String cmd, String[] cmdParts) throws IOException {
        int l, r;
        l = cmd.indexOf(' ');
        r = cmd.indexOf(' ', l + 1);
        if(l == -1 || r == -1) {
            writeError("Syntax error in cmd");
            throw new IOException("Syntax error in cmd");
        }
        cmdParts[0] = cmd.substring(1, l);
        cmdParts[1] = cmd.substring(l + 1, r);
        cmdParts[2] = cmd.substring(r + 1);
    }

    private void failsafeReadResponse(String where) throws IOException {
        int r;
        do {
            r = readByte();
        } while (r >= 10 && r < 127);
        if(r == 0) {
            // All is well, no error
            return;
        }
        if(r == -1) {
            throw new EOFException("SSHSCP1: premature EOF");
        }
        String errMsg = readString();
        if(r == (byte)'\02')
            throw new IOException(errMsg);
        throw new IOException("SSHSCP1.readResponse, error: " + errMsg);
    }

    private void readResponse(String where) throws IOException {
        int r = readByte();
        if(r == 0) {
            // All is well, no error
            return;
        }
        if(r == -1) {
            throw new EOFException("SSHSCP1: premature EOF");
        }
        String errMsg = readString();
        if(r == (byte)'\02')
            throw new IOException(errMsg);
        throw new IOException("SSHSCP1.readResponse, error: " + errMsg);
    }

    private void writeError(String reason) throws IOException {
        writeByte(1);
        writeString(reason);
    }

    private int readByte() throws IOException {
        return in.read();
    }

    private String readString() throws IOException {
        int ch, i = 0;
        while(((ch = readByte()) != ('\n')) && ch >= 0) {
            copyBuf[i++] = (byte)ch;
        }
        if(ch == -1) {
            throw new EOFException("SSHSCP1: premature EOF");
        }
        if(copyBuf[0] == (byte)'\n')
            throw new IOException("Unexpected <NL>");
        if(copyBuf[0] == (byte)'\02' || copyBuf[0] == (byte)'\01') {
            String errMsg = new String(copyBuf, 1, i - 1);
            if(copyBuf[0] == (byte)'\02')
                throw new IOException(errMsg);
            throw new IOException("SSHSCP.readString, error: " + errMsg);
        }
        return new String(copyBuf, 0, i);
    }

    private void readFully(FileOutputStream file, long size)
        throws IOException {
        long cnt = 0, n;
        try {
            while(cnt < size) {
                n = in.read(copyBuf, 0, (int)
                                  ((size - cnt) < DEFAULT_COPY_BUFFER_SZ ?
                                   (size - cnt) : DEFAULT_COPY_BUFFER_SZ));
                if(n == -1) {
                    throw new EOFException("SSHSCP1: premature EOF");
                }
                cnt += n;
                file.write(copyBuf, 0, (int)n);
                if(progress != null)
                    progress.progress((int)n);
            }
        } finally {
            file.close();
        }
    }

    private void writeByte(int b) throws IOException {
        out.write(b);
    }

    private void writeString(String str) throws IOException {
        byte[] buf = str.getBytes();
        out.write(buf);
    }

    private void writeFully(FileInputStream file, long size)
    throws IOException {
        long cnt = 0, n;
        try {
            while(cnt < size) {
                n = file.read(copyBuf, 0, (int)
                                   ((size - cnt) < DEFAULT_COPY_BUFFER_SZ ?
                                    (size - cnt) : DEFAULT_COPY_BUFFER_SZ));
                if(n == -1) {
                    throw new EOFException("SSHSCP1: premature EOF");
                }
                cnt += n;
                out.write(copyBuf, 0, (int)n);
                if(progress != null)
                    progress.progress((int)n);
            }
        } finally {
            file.close();
        }
    }

    private void remoteConnect(String command) throws IOException {
        if(!remote.command(command)) {
            throw new IOException("SSHSCP.remoteConnect, failed to run: " +
                                  command);
        }
        this.in  = remote.getStdOut();
        this.out = remote.getStdIn();
    }

	private long[] remoteConnectList(String command) {
		long numfiles = 0;
		long totsize = 0;
		try {
			if(!remote.command(command))
				return new long[] { 0, 0 };

			BufferedReader r = new BufferedReader(new InputStreamReader(new BufferedInputStream(remote.getStdOut())));
			for(;;) {
				String line = r.readLine();
				if (line == null) break;
				if (line.indexOf(':') == -1 ||
					!(line.charAt(0) == '-' || line.charAt(0) == 'l'))
					continue;
				String[] ss = line.split("\\s");
				if (ss.length < 5)
					continue;
				for (int i=3; i<ss.length; i++) {
					try {
						long n = Long.parseLong(ss[i]);
						numfiles++;
						totsize += n;
						break;
					} catch (Throwable t) {
					}
				}
			}
		} catch (Throwable t) {
		}
		return new long[] { numfiles, totsize };
    }

    public long[] getFileSizeCount(String remoteFiles[]) {
		return remoteConnectList("ls -lLR " + buildFileList(remoteFiles));
	}
}
