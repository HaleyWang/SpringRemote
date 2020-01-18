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

import java.io.IOException;

import java.util.Date;
import java.text.SimpleDateFormat;

import java.nio.ByteBuffer;
import com.mindbright.nio.NIOCallback;
import com.mindbright.nio.NonBlockingInput;
import com.mindbright.nio.NonBlockingOutput;

/**
 * Describes the sftp protocol. The actual implementation can then be
 * found in <code>SSH2SFTPClient</code>. These classes implement
 * version 3 of the sftp protocol as documented in
 * draft-ietf-secsh-filexfer-02.txt
 * <p>
 * The classes are compatible with protocol versions 2 and later.
 *
 * @see SSH2SFTPClient
 */
public class SSH2SFTP {

    private static final int DATA_BUFFER_SIZE_DEFAULT = 34000;
    private static final int FAILSAFE_READS = 1024;

    /**
     * Describes a file. Client applications should not access
     * the methods in this class directory.
     *
     * @see SSH2SFTPClient
     */
    public static final class FileHandle {

        private byte[]        handle;
        private String        name;
        private boolean       isDirectory;
        private boolean       isOpen;
        private boolean       asyncEOF;
        private int           asyncCnt;
        private int           reqLeft;
        private SFTPException asyncException;
        private AsyncListener listener;

        /**
         * This is used by SSH2SFTPClient to protect against out-of
         * order packets.
         */
        protected volatile long lastOffset;

        /**
         * Construct a new FileHandle with the provided data
         *
         * @param name name of file
         * @param handle handle to use in sftp protocol
         * @param isDirectory true if this is a directory
         */
        protected FileHandle(String name, byte[] handle, boolean isDirectory) {
            this.name           = name;
            this.handle         = handle;
            this.isDirectory    = isDirectory;
            this.isOpen         = true;
            this.asyncCnt       = 0;
            this.reqLeft        = 0;
            this.asyncException = null;
            this.lastOffset     = 0L;
        }

        /**
         * Check if thius handle refers to a directory. Client
         * applications should ideally use any of the stat functions
         * in SSH2SFTPClient to get this data.
         */
        public boolean isDirectory() {
            return isDirectory;
        }

        /**
         * Check if this handle is open or not
         */
        public boolean isOpen() {
            return isOpen;
        }

        /**
         * Get name of underlying file.
         */
        public String getName() {
            return name;
        }

        
        /**
         * Get the short name of underlying file.
         */
        public String getShortName() {
            int p = name.lastIndexOf('/');
            if (p == -1) return name;
            return name.substring(p+1);
        }
        
        /**
         * Get name of parent dir.
         */
        public String getParentDir() {
            int p1 = name.indexOf('/');
            int p2 = name.lastIndexOf('/');
            if (p2 == -1 || p1 == p2) return null;
            return name.substring(0, p2);
        }

        /**
         * Get the sftp protocol handle
         */
        protected byte[] getHandle() {
            return handle;
        }

        /**
         * Register a listener which will be notified when
         * asynchronous operations ends. The argument to the progress
         * function will be the number of bytes read.
         */
        public void addAsyncListener(AsyncListener listener) {
            this.listener = listener;
        }

        /**
         * An asynchronous operation has been started
         */
        protected synchronized void asyncStart(int len) {
            asyncCnt++;
        }

        /**
         * An asynchronous operation has completed
         */
        protected synchronized void asyncEnd(int len) {
            asyncCnt--;
            if(asyncCnt <= reqLeft) {
                this.notifyAll();
            }
            asyncProgress(len);
        }
        
        /**
         * Update progress
         */
        protected synchronized void asyncProgress(int len) {
            if (listener != null) {
                listener.progress(len);
            }
        }
        
        /**
         * Got an asyncronous EOF from server.
         */
        protected synchronized void asyncReadEOF() {
            asyncEOF = true;
            asyncEnd(0);
        }

        /**
         * Got an asyncronous exception
         */
        protected synchronized void asyncException(SFTPException e) {
            asyncException = e;
            this.notifyAll();
        }

        /**
         * Close an asynchronous file. No asynchronous request may be
         * outstanding.
         */
        protected synchronized void asyncClose() {
            if(asyncCnt > 0) {
                asyncException(new SFTPAsyncAbortException());
            }
            isOpen = false;
        }

        /**
         * Wait for all asynchronous operations to complete.
         *
         * @return true if eof has been received
         */
        public synchronized boolean asyncWait()
            throws SFTPException {
            return asyncWait(0);
        }

        /**
         * Wait until there is less than a certain number of
         * asynchronous operations outstanding.
         *
         * @return true if eof has been received
         */
        protected synchronized boolean asyncWait(int reqLeft)
            throws SFTPException {
            if(this.reqLeft < reqLeft) {
                this.reqLeft = reqLeft;
            }
            while(asyncCnt > reqLeft && asyncException == null && !asyncEOF) {
                try {
                    this.wait();
                } catch (InterruptedException e) {}
            }
            if(asyncException != null) {
                throw (SFTPException)asyncException.fillInStackTrace();
            }
            boolean eof = asyncEOF;
            asyncEOF    = false;
            return eof;
        }

        /**
         * Create a string representation of this object
         */
        public synchronized String toString() {
            return "FileHandle[name='" + name +
                   "',isDir=" + isDirectory +
                   ",isOpen=" + isOpen +
                   ",asyncEOF=" + asyncEOF +
                   ",asyncCnt=" + asyncCnt +
                   ",reqLeft=" + reqLeft +
                   ",asyncE=" + asyncException +
                   ",asyncL=" + listener + "]";
        }

    }

    /**
     * An interface implemented by FTP controllers which want progress reports.
     */
    public static interface AsyncListener {
        /**
         * Gets called periodically during the file transfer.
         *
         * @param size How many bytes have been transferred since the
         *             last call to this function.
         */
        public void progress(long size);
    }

    /**
     * Class describing the different file attributes.
     *
     * @see SSH2SFTPClient
     */
    public static final class FileAttributes {

        char[] types = { 'p', 'c', 'd', 'b', '-', 'l', 's', };

        /** Format mask, used to mask off the format flags from the mode */
        public final static int S_IFMT   = 0170000;
        /** Socket flag, set if this is a socket */
        public final static int S_IFSOCK = 0140000;
        /** Link flag, set if this is a symbolic link */
        public final static int S_IFLNK  = 0120000;
        /** Regular file flag, set if this is a regular file */
        public final static int S_IFREG  = 0100000;
        /** Block device flag, set if this is a block device */
        public final static int S_IFBLK  = 0060000;
        /** Directory flag, set if this is a directory */
        public final static int S_IFDIR  = 0040000;
        /** Character device flag, set if this is a character device */
        public final static int S_IFCHR  = 0020000;
        /** FIFO flag, set if this is a FIFO pipe */
        public final static int S_IFIFO  = 0010000;

        /** set-uid (SUID) flag */
        public final static int S_ISUID = 0004000;

        /** set-gid (SGID) flag */
        public final static int S_ISGID = 0002000;

        /** User (owner) read rights bit */
        public final static int S_IRUSR = 00400;
        /** User (owner) write rights bit */
        public final static int S_IWUSR = 00200;
        /** User (owner) execute rights bit */
        public final static int S_IXUSR = 00100;
        /** Group read rights bit */
        public final static int S_IRGRP = 00040;
        /** Group write rights bit */
        public final static int S_IWGRP = 00020;
        /** Group execute rights bit */
        public final static int S_IXGRP = 00010;
        /** Other read rights bit */
        public final static int S_IROTH = 00004;
        /** Other write rights bit */
        public final static int S_IWOTH = 00002;
        /** Other execute rights bit */
        public final static int S_IXOTH = 00001;

        /** True if the name field has been initialized */
        public boolean hasName;

        /** True if the size field has been initialized */
        public boolean hasSize;

        /** True if the user and group fields has been initialized */
        public boolean hasUserGroup;

        /** True if the permissions field has been initialized */
        public boolean hasPermissions;

        /** True if the mod time fiels has been initialized */
        public boolean hasModTime;

        /** Name of file */
        public String  name;

        /** Only valid for symbolic links, the name the link points at */
        public String  ldest;

        /** Size of file */
        public long    size;

        /** uid of file */
        public int     uid;

        /** gid of file */
        public int     gid;

        /** Permissions flags */
        public int     permissions;

        /** Time of last access */
        public int     atime;

        /** Time of last modification */
        public int     mtime;

        /**
         * Return a string identifying the file. The generated string
         * looks like the output of <code>ls -l</code>.
         */
        public String toString() {
            return toString(hasName ? name : "<noname>");
        }

        /**
         * Utility function which right-justifies a string within a
         * given width.
         */
        private static String rightJustify(String s, int width) {
            String res = s;
            while (res.length() < width)
                res = " " + res;
            return res;
        }

        /**
         * Return a string identifying the file. The generated string
         * looks like the output of <code>ls -l</code>.
         *
         * @param name name of file to print
         */
        public String toString(String name) {
            StringBuilder str = new StringBuilder();
            str.append(permString());
            str.append("    1 ");
            str.append(rightJustify(Integer.toString(uid), 8));
            str.append(" ");
            str.append(rightJustify(Integer.toString(gid), 8));
            str.append(" ");
            str.append(rightJustify(Long.toString(size), 16));
            str.append(" ");
            str.append(modTimeString());
            str.append(" ");
            str.append(name);
            if (isLink() && ldest != null) {
                str.append(" -> ");
                str.append(ldest);
            }
            return str.toString();
        }

        /**
         * Generate the permissions part of the <code>ls -l</code>
         * simulated string.
         */
        public String permString() {
            StringBuilder str = new StringBuilder();
            str.append(types[(permissions & S_IFMT) >>> 13]);
            str.append(rwxString(permissions, 6));
            str.append(rwxString(permissions, 3));
            str.append(rwxString(permissions, 0));
            return str.toString();
        }

        /**
         * Print modification time in a format similar to that of
         * <code>ls -l</code>. That is for dates whiel is less than
         * six months old "MMM dd hh:mm" and for older dates "MMM dd  yyyy".
         */
        public String modTimeString() {
            SimpleDateFormat df;
            long mt  = (mtime * 1000L);
            long now = System.currentTimeMillis();
            if((now - mt) > (6 * 30 * 24 * 60 * 60 * 1000L)) {
                df = new SimpleDateFormat("MMM dd  yyyy");
            } else {
                df = new SimpleDateFormat("MMM dd hh:mm");
            }
            return df.format(new Date(mt));
        }

        /**
         * Generate part of the permissions string
         */
        private String rwxString(int v, int r) {
            v >>>= r;
            String rwx = ((((v & 0x04) != 0) ? "r" : "-") +
                          (((v & 0x02) != 0) ? "w" : "-"));
            if((r == 6 && isSUID()) ||
                    (r == 3 && isSGID())) {
                rwx += (((v & 0x01) != 0) ? "s" : "S");
            } else {
                rwx += (((v & 0x01) != 0) ? "x" : "-");
            }
            return rwx;
        }

        /**
         * Returns true if this is a socket.
         */
        public boolean isSocket() {
            return ((permissions & S_IFSOCK) == S_IFSOCK);
        }

        /**
         * Returns true if this is a symbolic link.
         */
        public boolean isLink() {
            return ((permissions & S_IFLNK) == S_IFLNK);
        }

        /**
         * Returns true if this is a regular file.
         */
        public boolean isFile() {
            return ((permissions & S_IFREG) == S_IFREG);
        }

        /**
         * Returns true if this is a block device.
         */
        public boolean isBlock() {
            return ((permissions & S_IFBLK) == S_IFBLK);
        }

        /**
         * Returns true if this is a directory.
         */
        public boolean isDirectory() {
            return ((permissions & S_IFDIR) == S_IFDIR);
        }

        /**
         * Returns true if this is a character device.
         */
        public boolean isCharacter() {
            return ((permissions & S_IFCHR) == S_IFCHR);
        }

        /**
         * Returns true if this is a FIFO pipe.
         */
        public boolean isFifo() {
            return ((permissions & S_IFIFO) == S_IFIFO);
        }

        /**
         * Returns true if this object has the setuid flag set.
         */
        public boolean isSUID() {
            return ((permissions & S_ISUID) == S_ISUID);
        }

        /**
         * Returns true if this object has the setgid flag set.
         */
        public boolean isSGID() {
            return ((permissions & S_ISGID) == S_ISGID);
        }

    }

    /**
     * Class describing the different file system attributes.
     *
     * @see SSH2SFTPClient
     */
    public static final class FileSystemAttributes {
	public long f_bsize;	/* file system block size */
	public long f_frsize;	/* fundamental fs block size */
	public long f_blocks;	/* number of blocks (unit f_frsize) */
	public long f_bfree;	/* free blocks in file system */
	public long f_bavail;	/* free blocks for non-root */
	public long f_files;	/* total file inodes */
	public long f_ffree;	/* free file inodes */
	public long f_favail;	/* free file inodes for to non-root */
	public long f_fsid;	/* file system id */
	public long f_flag;	/* bit mask of f_flag values */
	public long f_namemax;	/* maximum filename length */

        public String toString() {
            return "FileSystemAttributes[" +
                "f_bsize="  + f_bsize +
                ",f_frsize=" + f_frsize +
                ",f_blocks=" + f_blocks +
                ",f_bfree=" + f_bfree +
                ",f_bavail=" + f_bavail +
                ",f_files=" + f_files +
                ",f_ffree=" + f_ffree +
                ",f_favail=" + f_favail +
                ",f_fsid=" + f_fsid +
                ",f_flag=" + f_flag +
                ",f_namemax=" + f_namemax + "]";
        }
    }

    /**
     * An exception in the SFTP code. It only holds a string message.
     */
    public static class SFTPException extends Exception {
    	private static final long serialVersionUID = 1L;
        public SFTPException() {}
        public SFTPException(String msg) { super(msg); }
    }

    public static class SFTPEOFException extends SFTPException {
    	private static final long serialVersionUID = 1L;
        public SFTPEOFException() { super(); }
        public SFTPEOFException(String msg) { super(msg); }
    }

    public static class SFTPNoSuchFileException extends SFTPException {
    	private static final long serialVersionUID = 1L;
        public SFTPNoSuchFileException(String msg) { super(msg); }
    }

    public static class SFTPPermissionDeniedException extends SFTPException {
    	private static final long serialVersionUID = 1L;
        public SFTPPermissionDeniedException(String msg) { super(msg); }
    }

    public static class SFTPFailureException extends SFTPException {
    	private static final long serialVersionUID = 1L;
        public SFTPFailureException(String msg) { super(msg); }
    }

    public static class SFTPDisconnectException extends SFTPException {
    	private static final long serialVersionUID = 1L;
    	public SFTPDisconnectException() { }
        public SFTPDisconnectException(String msg) { super(msg); }
    }

    public static class SFTPAsyncAbortException extends SFTPException {
    	private static final long serialVersionUID = 1L;
    }

    public interface Callback {
        public void packetReceived(SFTPPacket pkt);
        public void readFailed(SFTPException e);
    }

    public static class SFTPUnsupportedException extends SFTPException {
    	private static final long serialVersionUID = 1L;
        public SFTPUnsupportedException() { }
        public SFTPUnsupportedException(String msg) { super(msg); }
    }

    /**
     * Handles sftp data packets. Contains functions to read and write
     * the sftp types.
     */
    protected static final class SFTPPacket extends SSH2DataBuffer 
        implements NIOCallback {

        private int type;
        private int id;
        private int len;
        private ByteBuffer dataBuf;

        private boolean lookForPacket = false;
        private boolean packetReady = false;
        private int failsafeReads;
        private NonBlockingInput nbin;
        private SFTPException storedException;
        private Callback callback;

        public SFTPPacket() {
            super(DATA_BUFFER_SIZE_DEFAULT);
        }

        public void reset(int type, int id) {
            reset();
            writeInt(0); // dummy length
            writeByte(type);
            writeInt(id);
            this.type = type;
            this.id   = id;
            lookForPacket = false;
            packetReady   = false;
            callback      = null;
            failsafeReads = 0;
        }

        public int getType() {
            return type;
        }

        public int getId() {
            return id;
        }

        public int getLength() {
            return len;
        }

        public void writeAttrs(FileAttributes attrs) {
            writeInt((attrs.hasSize ? SSH_ATTR_SIZE : 0) |
                     (attrs.hasUserGroup ? SSH_ATTR_UIDGID : 0) |
                     (attrs.hasPermissions ? SSH_ATTR_PERM : 0) |
                     (attrs.hasModTime ? SSH_ATTR_MODTIME : 0));
            if(attrs.hasSize) {
                writeLong(attrs.size);
            }
            if(attrs.hasUserGroup) {
                writeInt(attrs.uid);
                writeInt(attrs.gid);
            }
            if(attrs.hasPermissions) {
                writeInt(attrs.permissions);
            }
            if(attrs.hasModTime) {
                writeInt(attrs.atime);
                writeInt(attrs.mtime);
            }
        }

        public FileAttributes readAttrs() {
            FileAttributes attrs = new FileAttributes();
            int            flags = readInt();
            attrs.hasSize        = ((flags & SSH_ATTR_SIZE) != 0);
            attrs.hasUserGroup   = ((flags & SSH_ATTR_UIDGID) != 0);
            attrs.hasPermissions = ((flags & SSH_ATTR_PERM) != 0);
            attrs.hasModTime     = ((flags & SSH_ATTR_MODTIME) != 0);
            if (attrs.hasSize) {
                attrs.size = readLong();
            }
            if (attrs.hasUserGroup) {
                attrs.uid = readInt();
                attrs.gid = readInt();
            }
            if (attrs.hasPermissions) {
                attrs.permissions = readInt();
            }
            if (attrs.hasModTime) {
                attrs.atime = readInt();
                attrs.mtime = readInt();
            }
            // Skip any extended attrs
            if ((flags & SSH_ATTR_EXTENDED) != 0) {
                int numExt = readInt();
                for (int i=0; i<numExt; i++) {
                    readString(); // extended_type
                    readString(); // extended_data
                }
            }
            return attrs;
        }

        public FileSystemAttributes readFSAttrs() {
            FileSystemAttributes attrs = new FileSystemAttributes();
            attrs.f_bsize   = readLong();
            attrs.f_frsize  = readLong();
            attrs.f_blocks  = readLong();
            attrs.f_bfree   = readLong();
            attrs.f_bavail  = readLong();
            attrs.f_files   = readLong();
            attrs.f_ffree   = readLong();
            attrs.f_favail  = readLong();
            attrs.f_fsid    = readLong();
            attrs.f_flag    = readLong();
            attrs.f_namemax = readLong();
            return attrs;
        }

        /**
         * Failsafe read method. Tries to read a packet from the given stream.
         * but does ignore stuff which does not look like a packet. The
         * algorithm is that a probable packet starts with two bytes containing
         * zeros. 
         * <p>
         * This is useful when reading the first version packet which is
         * small but may be, on broken systems, preceded by some ascii
         * characters.
         *
         * @param in Stream to read data from
         */
        public void failsafeReadFrom(NonBlockingInput in) throws SFTPException{
            lookForPacket = true;
            internalRead(in, 2);
            waitForPacket();
        }

        public void readFrom(NonBlockingInput in) throws SFTPException {
            internalRead(in, 5);
            waitForPacket();
        }

        public void asyncRead(NonBlockingInput in, Callback callback) 
            throws SFTPException {
            this.callback = callback;
            lookForPacket = false;
            internalRead(in, 5);
        }

        // Launch read
        private void internalRead(NonBlockingInput in, int length)
            throws SFTPException {
            if (dataBuf == null) {
                dataBuf = in.createBuffer(data);
            }
            packetReady = false;
            nbin = in;
            try {
                in.read(dataBuf, 0, length, this, false, false);
            } catch (IOException e) {
                throw new SFTPException(e.getMessage());
            }
        }

        // Wait until packet is received
        private void waitForPacket() throws SFTPException {
            synchronized(this) {
                if (!packetReady) {
                    try {
                        wait();
                    } catch (InterruptedException e) {}
                }
            }
            if (storedException != null) {
                throw storedException;
            }
        }

		// private static java.io.FileOutputStream fos;

		// private static void logpkt(char ch, byte[] data, int off, int len) {
		// 	try {
		// 		if (fos == null)
		// 			fos = new java.io.FileOutputStream("/tmp/mtsftp.log");
		// 		fos.write(ch);
		// 		fos.write(data, off, len);
		// 	} catch (Throwable t) {
		// 		t.printStackTrace();
		// 	}
		// }

        public void writeTo(NonBlockingOutput out) throws SFTPException {
            len = getWPos() - 5;
            setWPos(0);
            writeInt(len + 1);
            try {
				//logpkt('W', data, 0, len + 5);
                out.write(data, 0, len + 5);
            } catch (IOException e) {
                throw new SFTPException(e.getMessage());
            }
        }

        private ByteBuffer handleReadData(ByteBuffer buf) {
            // Are we searching through the 'crud' before the first packet
            if (lookForPacket) {
                if (failsafeReads++ >= FAILSAFE_READS) {
                    // Too much crud, give up
                    storedException =
                        new SFTPException("Failed to find first packet");
                    packetComplete();
                }
                if (data[0] != 0 || data[1] != 0) {
                    data[0] = data[1];
                    buf.limit(2);
                    buf.position(1);
                    return buf;
                }
                lookForPacket = false;
                buf.limit(5);
                return buf;
            }

            // If we just got the preamble, then parse the length
            if (buf.position() == 5) {
                len  = readInt() + 4;
                type = readByte();
                buf.limit(len);
                return buf;
            }

			//logpkt('R', data, 0, len);

            // Now we have a complete packet
            packetComplete();
            return null;
        }

        private void packetComplete() {
            if (callback != null) {
                if (storedException != null) {
                    callback.readFailed(storedException);
                } else {
                    callback.packetReceived(this);
                }
            } else {
                synchronized(this) {
                    packetReady = true;
                    notify();
                }
            }
        }

        /*
         * NIOCallback interface
         */
        public void completed(ByteBuffer buf) {
            try {
                do {
                    buf = handleReadData(buf);
                } while(buf != null && nbin.read(buf, this, true, false));
            } catch (IOException e) {
                storedException = new SFTPException(e.getMessage());
                packetComplete();
            }
        }

        public void readFailed(Exception e) {
            storedException = new SFTPDisconnectException();
            packetComplete();
        }
        public void writeFailed() {}
        public void connected(boolean timeout) {}
        public void connectionFailed(Exception e) {}
    }

    protected final static int SSH_FILEXFER_VERSION =    3;

    /* Packet types. */
    protected final static int SSH_FXP_INIT =            1;
    protected final static int SSH_FXP_VERSION =         2;
    protected final static int SSH_FXP_OPEN =            3;
    protected final static int SSH_FXP_CLOSE =           4;
    protected final static int SSH_FXP_READ =            5;
    protected final static int SSH_FXP_WRITE =           6;
    protected final static int SSH_FXP_LSTAT =           7;
    protected final static int SSH_FXP_FSTAT =           8;
    protected final static int SSH_FXP_SETSTAT =         9;
    protected final static int SSH_FXP_FSETSTAT =       10;
    protected final static int SSH_FXP_OPENDIR =        11;
    protected final static int SSH_FXP_READDIR =        12;
    protected final static int SSH_FXP_REMOVE =         13;
    protected final static int SSH_FXP_MKDIR =          14;
    protected final static int SSH_FXP_RMDIR =          15;
    protected final static int SSH_FXP_REALPATH =       16;
    protected final static int SSH_FXP_STAT =           17;
    protected final static int SSH_FXP_OLD_STAT =        7;
    protected final static int SSH_FXP_RENAME =         18;
    protected final static int SSH_FXP_READLINK =       19;
    protected final static int SSH_FXP_SYMLINK =        20;

    protected final static int SSH_FXP_STATUS =         101;
    protected final static int SSH_FXP_HANDLE =         102;
    protected final static int SSH_FXP_DATA =           103;
    protected final static int SSH_FXP_NAME =           104;
    protected final static int SSH_FXP_ATTRS =          105;
    protected final static int SSH_FXP_EXTENDED =       200;
    protected final static int SSH_FXP_EXTENDED_REPLY = 201;

    /* Status/error codes. */
    public final static int SSH_FX_OK =                0;
    public final static int SSH_FX_EOF =               1;
    public final static int SSH_FX_NO_SUCH_FILE =      2;
    public final static int SSH_FX_PERMISSION_DENIED = 3;
    public final static int SSH_FX_FAILURE =           4;
    public final static int SSH_FX_BAD_MESSAGE =       5;
    public final static int SSH_FX_NO_CONNECTION =     6;
    public final static int SSH_FX_CONNECTION_LOST =   7;
    public final static int SSH_FX_OP_UNSUPPORTED =    8;
    public final static int SSH_FX_INVALID_HANDLE =    9;

    /* Portable versions of O_RDONLY etc. */
    public final static int SSH_FXF_READ =            0x0001;
    public final static int SSH_FXF_WRITE =           0x0002;
    public final static int SSH_FXF_APPEND =          0x0004;
    public final static int SSH_FXF_CREAT =           0x0008;
    public final static int SSH_FXF_TRUNC =           0x0010;
    public final static int SSH_FXF_EXCL =            0x0020;

    /* Flags indicating presence of file attributes. */
    protected final static int SSH_ATTR_SIZE =         0x00000001;
    protected final static int SSH_ATTR_UIDGID =       0x00000002;
    protected final static int SSH_ATTR_PERM =         0x00000004;
    protected final static int SSH_ATTR_MODTIME =      0x00000008;
    protected final static int SSH_ATTR_EXTENDED =     0x80000000;

}
