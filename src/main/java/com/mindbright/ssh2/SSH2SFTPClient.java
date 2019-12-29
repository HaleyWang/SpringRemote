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
import java.io.RandomAccessFile;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import com.mindbright.nio.NonBlockingInput;
import com.mindbright.nio.NonBlockingOutput;
import com.mindbright.nio.NonBlockingPipe;


/**
 * Implements the client side of the sftp protocol. File reads and writes
 * can be either synchronous (blocking) or asynchronous
 * (non-blocking). Asynchronous operation is much faster but is more
 * complicated to use when the interface is used for reading
 * parts of files. In asynchronous mode <code>read</code> and
 * <code>write</code> return immediately before the operation has
 * finished. The <code>asyncWait</code> method waits until all
 * operations have been completed and must be called before read
 * buffers are accessed.
 */
public final class SSH2SFTPClient extends SSH2SFTP
    implements SSH2SFTP.Callback {

    private final static int MAX_OUTSTANDING_ASYNCH = 24;
	
    private class ReplyLock {
        protected int        expectType;
        protected SFTPPacket replyPkt;

        protected ReplyLock(int expectType) {
            this.expectType = expectType;
            this.replyPkt   = null;
        }

        protected synchronized SFTPPacket expect()
        throws SFTPException {
            while(replyPkt == null) {
                try {
                    this.wait();
                } catch (InterruptedException e) {}
            }
            checkType(replyPkt, expectType);
            return replyPkt;
        }

        protected synchronized void received(SFTPPacket replyPkt) {
            this.replyPkt = replyPkt;
            this.notify();
        }

        protected synchronized void cancel() {
            this.replyPkt = createPacket(SSH_FXP_STATUS);
            this.replyPkt.writeInt(SSH_FX_CONNECTION_LOST);
            this.notify();
        }

    }

    private class WriteReplyLock extends ReplyLock {

        private FileHandle handle;
        private int        len;

        protected WriteReplyLock(FileHandle handle, int len) {
            super(SSH_FXP_STATUS);
            this.handle = handle;
            this.len    = len;
            handle.asyncStart(len);
        }

        protected synchronized void received(SFTPPacket replyPkt) {
            try {
                if(!handle.isOpen()) {
                    /* Ignore and discard packets after close */
                    return;
                }
                checkType(replyPkt, expectType);
                handle.asyncEnd(len);
            } catch (SFTPException e) {
                handle.asyncException(e);
            }
            releasePacket(replyPkt);
        }

        protected synchronized void cancel() {
            handle.asyncException(new SFTPDisconnectException());
            this.notify();
        }

    }

    private class ReadReplyLock extends ReplyLock {

        private FileHandle handle;
        private long       fileOffset;
        private byte[]     buf;
        private int        off;
        private int        len;
        
        private RandomAccessFile fileTarget;
        private OutputStream     strmTarget;

        private ReadReplyLock(FileHandle handle, long fileOffset, int len) {
            super(SSH_FXP_DATA);
            this.handle     = handle;
            this.fileOffset = fileOffset;
            this.len        = len;
            handle.asyncStart(len);
        }

        protected ReadReplyLock(FileHandle handle, long fileOffset,
                                OutputStream strmTarget, int len) {
            this(handle, fileOffset, len);
            this.strmTarget = strmTarget;
        }

        protected ReadReplyLock(FileHandle handle, long fileOffset,
                                RandomAccessFile fileTarget, int len) {
            this(handle, fileOffset, len);
            this.fileTarget = fileTarget;
        }

        protected ReadReplyLock(FileHandle handle, long fileOffset,
                                byte[] buf, int off, int len) {
            this(handle, fileOffset, len);
            this.buf        = buf;
            this.off        = off;
        }

        protected synchronized void received(SFTPPacket replyPkt) {
            try {
                int n;
                if(!handle.isOpen()) {
                    /* Ignore and discard packets after close */
                    return;
                }
                checkType(replyPkt, expectType);
                if (fileTarget != null) {
                    n = replyPkt.readInt();
                    fileTarget.seek(fileOffset);
                    if (n > 0)
                        fileTarget.write(replyPkt.getData(), replyPkt.getRPos(), n);
                } else if (strmTarget != null) {
                    if (handle.lastOffset != fileOffset) {
                        handle.asyncException(
                            new SFTPException(
                                "Out of order packets can't be handled yet!"));
                    }
                    n = replyPkt.readInt();
                    if (n > 0) {
                        strmTarget.write(replyPkt.getData(), replyPkt.getRPos(), n);
                        handle.lastOffset = fileOffset + n;
                    }
                } else {
                    n = replyPkt.readString(buf, off);
                }

                if (n < 0)
                    throw new SFTPEOFException("");

                if (n < len) {
                    resend(replyPkt, n);
                } else {
                    handle.asyncEnd(len);
                    releasePacket(replyPkt);
                }
            } catch (IOException e) {
                handle.asyncException(new SFTPException(e.getMessage()));
            } catch (SFTPEOFException e) {
                handle.asyncReadEOF();
            } catch (SFTPException e) {
                handle.asyncException(e);
            }
        }

        private void resend(SFTPPacket pkt, int n) {
            int     i  = getNextId();
            Integer id = Integer.valueOf(i);

            fileOffset += n;
            len        -= n;
            off        += n;
            pkt.reset(SSH_FXP_READ, i);
            pkt.writeString(handle.getHandle());
            pkt.writeLong(fileOffset);
            pkt.writeInt(len);
            replyLocks.put(id, this);
            transmit(pkt);
        }

        protected synchronized void cancel() {
            handle.asyncException(new SFTPDisconnectException());
            this.notify();
        }

    }

    private final static int   POOL_SIZE = 16;

    private SSH2Connection     connection;
    private SSH2SessionChannel session;

    private int                id;
    private int                version;
    private int                maxDataSize;
    private boolean            isBlocking;
    private boolean            isOpen;

    private Hashtable<Integer, ReplyLock> replyLocks;

    private ArrayList<SFTPPacket> pktPool;

    private NonBlockingInput   in;
    private NonBlockingOutput  out;

    private boolean            extensionPosixRename = false;
    private boolean            extensionStatvfs     = false;
    private boolean            extensionFStatvfs    = false;
    private boolean            extensionHardLink    = false;
	
    /**
     * Create a new SFTP client which connects to the server.
     *
     * @param connection Connection to run over.
     * @param isBlocking True if read and write operations should be
     *                   synchronous.
     */
    public SSH2SFTPClient(SSH2Connection connection, boolean isBlocking)
        throws SFTPException {
        this(connection, isBlocking, 32768);
    }

    /**
     * Create a new SFTP client which connects to the server.
     *
     * @param connection Connection to run over.
     * @param isBlocking True if read and write operations should be
     *                   synchronous.
     * @param maxSize    Max packet size, must be 1..32768.
     */
    public SSH2SFTPClient(SSH2Connection connection, boolean isBlocking, int maxSize)
        throws SFTPException {
        
        if (maxSize < 0 || maxSize > 32768)
            throw new IllegalArgumentException("illegal max data size");
        this.connection = connection;
        this.id         = 0;
        this.isBlocking = isBlocking;
        this.maxDataSize = maxSize;
        this.restart();

        // INIT pkt don't have an id but version is in same place
        //
        SFTPPacket pkt = createPacket();
        pkt.reset(SSH_FXP_INIT, SSH_FILEXFER_VERSION);
        pkt.writeTo(out);

        pkt.reset();
        pkt.failsafeReadFrom(in);
        checkType(pkt, SSH_FXP_VERSION);
        version = pkt.readInt();

        connection.getLog().debug("SSH2SFTPClient",
                                  "constructor",
                                  "remote FXP version is: " + version);

        // Read extensions
        while (pkt.getRPos() < pkt.getLength()) {
            String name = pkt.readJavaString();
            String data = pkt.readJavaString();
            
            connection.getLog().debug("SSH2SFTPClient",
                                      "constructor",
                                      "server supports " + name +
                                      " [" + data + "]");
            if (name.equals("posix-rename@openssh.com") && data.equals("1")) {
                extensionPosixRename = true;
            } else if (name.equals("statvfs@openssh.com") && data.equals("2")){
                extensionStatvfs = true;
            } else if (name.equals("fstatvfs@openssh.com") && data.equals("2")){
                extensionFStatvfs = true;
            } else if (name.equals("hardlink@openssh.com") && data.equals("1")){
                extensionHardLink = true;
            }
        }

        releasePacket(pkt);

        if(!isBlocking) {
            startNonblocking();
        }
    }

    /**
     * Terminate the connection and abort any asynchronous calls which
     * are in progress.
     */
    public synchronized void terminate() {
        isOpen = false;
        try {
            in.close();
        } catch (Throwable t) {
        }
        try {
            out.close();
        } catch (Throwable t) {
        }
        if(session != null) {
            session.close();
        }
        cancelAllAsync();
        session = null;
        if(pktPool != null) {
            pktPool.clear();
        }
    }

	public SSH2SessionChannel getSessionChannel() {
		return session;
	}

    /**
     * Reopens the connection to the server. Any outstanding
     * asynchronous operations are aborted.
     */
    public void restart() throws SFTPException {
        terminate();
        try {
            NonBlockingPipe inPipe =  new NonBlockingPipe();
            NonBlockingPipe outPipe = new NonBlockingPipe();
            in = inPipe.getSource();
            out = outPipe.getSink();
            session = connection.newSession(outPipe.getSource(),
                                            inPipe.getSink(),
                                            inPipe.getSink(),
											false);
        } catch (IOException e) {
            throw new SFTPException("failed to create pipes");
        }
        if(!session.doSubsystem("sftp")) {
            throw new SFTPException(
                "sftp subsystem couldn't be started on server");
        }
        isOpen = true;
	pktPool = new ArrayList<SFTPPacket>(POOL_SIZE);
	for (int i=0; i<POOL_SIZE; i++)
	    pktPool.add(new SFTPPacket());	
    }

    /**
     * Open a file on the server.
     *
     * @param name Name of file
     * @param flags Mode to open file with. Valid values are
     * <code>SSH2SFTP.SSH_FXF_*</code>.
     * @param attrs File attributes for new files.
     *
     * @return A handle identifying the file.
     */
    public FileHandle open(String name, int flags, FileAttributes attrs)
        throws SFTPException {
        SFTPPacket pkt = createPacket(SSH_FXP_OPEN);
        pkt.writeString(name);
        pkt.writeInt(flags);
        pkt.writeAttrs(attrs);

        pkt = transmitExpectReply(pkt, SSH_FXP_HANDLE);
        FileHandle handle = new FileHandle(name, pkt.readString(), false);
        releasePacket(pkt);
        return handle;
    }

    /**
     * Close a file. This will wait for any outstanding asynchronous
     * operations.
     *
     * @param handle Handle identifying file.
     */
    public void close(FileHandle handle) throws SFTPException {
        SFTPPacket pkt = createPacket(SSH_FXP_CLOSE, handle);
        pkt = transmitExpectReply(pkt, SSH_FXP_STATUS);
        releasePacket(pkt);
        handle.asyncClose();
    }

    /**
     * Wait for all outstanding asynchoronous operations on the given
     * file to complete.
     *
     * @param handle Handle identifying file.
     */
    public void asyncWait(FileHandle handle) throws SFTPException {
        handle.asyncWait();
    }

    /**
     * Read data from an open file on the server and stores it in a
     * local file. The data is stored at the same position in the
     * local file as it is read from in the remote file.
     * <p>
     * Note that if the client is operating in
     * asynchronous mode then the method will return before data has been
     * written to the stream. In this case the <code>asyncWait</code>
     * method must be called, or the sftp file closed, before the stream
     * can be closed.
     *
     * @param handle Handle identifying file.
     * @param fileOffset Where in the file to start to read.
     * @param fileTarget Local file to write the data into.
     * @param len Number of bytes to read. Must be less than
     *            maxDataSize, which by default is 32768 bytes.
     *
     * @return The number of read bytes.
     */
    public int read(FileHandle handle, long fileOffset,
                    RandomAccessFile fileTarget, int len)
        throws SFTPException, IOException {
        if(!handle.isOpen()) {
            throw new SFTPAsyncAbortException();
        }
        if (len <= 0 || len > maxDataSize) {
            throw new IllegalArgumentException("len must be: 0 < len <= " + maxDataSize);
        }
        SFTPPacket pkt = createPacket(SSH_FXP_READ, handle);
        pkt.writeLong(fileOffset);
        pkt.writeInt(len);

        if(isBlocking) {
            try {
                pkt = transmitExpectReply(pkt, SSH_FXP_DATA);
                len = pkt.readInt();
                fileTarget.seek(fileOffset);
                fileTarget.write(pkt.getData(), pkt.getRPos(), len);
                return len;
            } catch (SFTPEOFException e) {
                return 0;
            } finally {
                if(pkt != null)
                    releasePacket(pkt);
            }
        }
	Integer   id    = Integer.valueOf(pkt.getId());
	ReplyLock reply = new ReadReplyLock(handle, fileOffset, fileTarget, len);
	replyLocks.put(id, reply);
	transmit(pkt);
	return len;
    }

    /**
     * Read data from an open file on the server and stores it in a
     * local buffer. Note that if the client is operating in
     * asynchronous mode then the method will return before data has been
     * placed in the buffer. In this case the <code>asyncWait</code>
     * method must be called, or the file closed, before the data can
     * be safely accessed.
     *
     * @param handle Handle identifying file.
     * @param fileOffset Where in the file to start to read.
     * @param buf Local buffer to store data in. Must hold
     *            <code>len</code> bytes at the given offset.
     * @param off Offset in buffer to store data at.
     * @param len Number of bytes to read.  Must be less than
     *            maxDataSize, which by default is 32768 bytes.
     *
     * @return The number of read bytes.
     */
    public int read(FileHandle handle, long fileOffset,
                    byte[] buf, int off, int len)
        throws SFTPException {
        if(!handle.isOpen()) {
            throw new SFTPAsyncAbortException();
        }
        if (len <= 0 || len > maxDataSize) {
            throw new IllegalArgumentException("len must be: 0 < len <= " + maxDataSize);
        }
        SFTPPacket pkt = createPacket(SSH_FXP_READ, handle);
        pkt.writeLong(fileOffset);
        pkt.writeInt(len);

        if(isBlocking) {
            try {
                pkt = transmitExpectReply(pkt, SSH_FXP_DATA);
                return pkt.readString(buf, off);
            } catch (SFTPEOFException e) {
                return 0;
            } finally {
                if(pkt != null)
                    releasePacket(pkt);
            }
        }
	if(!isOpen) {
		throw new SFTPDisconnectException();
	}
	Integer   id    = Integer.valueOf(pkt.getId());
	ReplyLock reply = new ReadReplyLock(handle, fileOffset,
					    buf, off, len);
	replyLocks.put(id, reply);
	transmit(pkt);
	return len;
    }

	
    private long getRealSize(FileHandle handle) throws SFTPException {
        // workaround for buggy SSH servers which returns different size
        // depending on whether fstat or stat is used - the bigger value
        // seems to be the correct one.
	long len = -1;

        FileAttributes attrs1 = null, attrs2 = null;
        try {
            attrs1 = fstat(handle);
        } catch (SFTPException e) {}
        try {
            attrs2 = stat(handle.getName());
        } catch (SFTPException e) {}

        if (attrs1 == null && attrs2 == null) {
	    len = -1;
        } else if (attrs1 == null) {
            len = attrs2.size;
        } else if (attrs2 == null) {
            len = attrs1.size;
        } else if (attrs1.hasSize && attrs2.hasSize) {
            len = (attrs1.size > attrs2.size) ? attrs1.size : attrs2.size;
        } else if (attrs1.hasSize) {
            len = attrs1.size;
        } else if (attrs2.hasSize) {
            len = attrs2.size;
        }

	if (len == -1) {
            // ok then, let's see if readdir returns file size
            try {
                FileHandle dir = opendir(handle.getParentDir());
                FileAttributes list[] = readdir(dir);
                String sname = handle.getShortName();
                for (int i=0; i<list.length; i++) {
                    if (list[i].hasName && list[i].name.equals(sname)) {
                        if (list[i].hasSize)
                            len = list[i].size;
                        break;
                    }
                }
                close(dir);
            } catch (Throwable t) {
                // just ignore
            }
        }

	return len;
    }

    /**
     * Read the entire file on the server and store in a local
     * stream. This method will be much faster if asynchronous mode is
     * used. It will always wait until the operation has completed
     * before returning, even if running in asynchronous mode.
     *
     * @param handle Handle identifying file. The handle will be
     *               closed when the transfer has completed.
     * @param out Stream to store data in.
     *
     * @return Number of bytes read.
     */
    public int readFully(FileHandle handle, OutputStream out)
        throws SFTPException, IOException {
        if(!handle.isOpen()) {
            throw new SFTPAsyncAbortException();
        }

        long rlen = 0, len = getRealSize(handle);

        boolean useAsyncRead = !isBlocking && len >= 0;
        long foffs = 0;
        int cnt   = 0;

        try {
            while (len == -1 || foffs < len) {
                int toread =
                    (len == -1) ? maxDataSize :
                    ((maxDataSize < (len - foffs) ? maxDataSize : (int)(len - foffs)));

                SFTPPacket pkt = createPacket(SSH_FXP_READ, handle);
                pkt.writeLong(foffs);
                pkt.writeInt(toread);

                if (!useAsyncRead) {
                    try {
                        pkt = transmitExpectReply(pkt, SSH_FXP_DATA);
                        int n = pkt.readInt();
                        if (n >= 0) {
                            out.write(pkt.getData(), pkt.getRPos(), n);
                            foffs += n;
                            rlen += n;
                            handle.asyncProgress(n);
                        } else {
                            if (len != -1)
                                throw new SFTPEOFException("Unexpected end of file");
                            break;
                        }
                    } finally {
                        if (pkt != null)
                            releasePacket(pkt);
                    }
                } else {
                    Integer   id    = Integer.valueOf(pkt.getId());
                    ReplyLock reply = new ReadReplyLock(handle, foffs, out, toread);
                    replyLocks.put(id, reply);
                    transmit(pkt);
                    foffs += toread;
                    rlen += toread;
               }

                if (useAsyncRead && ++cnt == MAX_OUTSTANDING_ASYNCH) {
                    cnt = 0;
                    handle.asyncWait(MAX_OUTSTANDING_ASYNCH/2);
                }
            }

            if (useAsyncRead) {
                handle.asyncWait();
            }

        } catch (SFTPEOFException eof) {
            if (len != -1)
                throw new SFTPEOFException("Unexpected end of file");
        } finally {
            try {
                close(handle);
            } catch (SFTPException e) {
                // ignore
            }
        }

        return (int) rlen; 
    }

    /**
     * Internal write function.
     */
    protected void writeInternal(FileHandle handle, SFTPPacket pkt, int len)
    throws SFTPException {
        if(isBlocking) {
            pkt = transmitExpectReply(pkt, SSH_FXP_STATUS);
            releasePacket(pkt);
        } else {
            if(!isOpen) {
                throw new SFTPDisconnectException();
            }
            Integer   id    = Integer.valueOf(pkt.getId());
            ReplyLock reply = new WriteReplyLock(handle, len);
            replyLocks.put(id, reply);
            transmit(pkt);
        }
    }

    /**
     * Write data to a remote file.
     *
     * @param handle Handle identifying file.
     * @param fileOffset Offset to store data at.
     * @param buf Buffer containing data to write.
     * @param off Offset in <code>buf</code> to read data at.
     * @param len Number of bytes to write. Must be less than
     *            maxDataSize, which by default is 32768 bytes.
     */
    public void write(FileHandle handle, long fileOffset,
                      byte[] buf, int off, int len)
    throws SFTPException {
        if(!handle.isOpen()) {
            throw new SFTPAsyncAbortException();
        }
        if (len <= 0 || len > maxDataSize) {
            throw new IllegalArgumentException("len must be: 0 < len <= " + maxDataSize);
        }
        SFTPPacket pkt = createPacket(SSH_FXP_WRITE, handle);
        pkt.writeLong(fileOffset);
        pkt.writeString(buf, off, len);

        writeInternal(handle, pkt, len);
    }

    /**
     * Write an entire stream to a file on the server.
     * This method will be much faster if asynchronous mode is
     * used. It will always wait until the operation has completed
     * before returning, even if running in asynchronous mode.
     *
     * @param handle Handle identifying file. The handle will be
     *               closed when the transfer has completed.
     * @param in Stream to read data to write from.
     */
    public long writeFully(FileHandle handle, InputStream in)
        throws SFTPException, IOException {
        return writeFully(handle, in, false);
    }

    /**
     * Write an entire stream to a file on the server.
     * This method will be much faster if asynchronous mode is
     * used. It will always wait until the operation has completed
     * before returning, even if running in asynchronous mode.
     *
     * @param handle Handle identifying file. The handle will be
     *               closed when the transfer has completed.
     * @param in Stream to read data to write from.
     * @param append if true then the data is written at the end of
     *        the opened file. In this case the handle must have been opened
     *        in append mode.
     */
    public long writeFully(FileHandle handle, InputStream in, boolean append)
    throws SFTPException, IOException {
        if(!handle.isOpen()) {
            throw new SFTPAsyncAbortException();
        }

        int len   = 0;
        long foffs = 0;
        int cnt   = 0;
        int lPos  = 0;

        if (append) {
            foffs = getRealSize(handle);
	    if (foffs == -1)
		foffs = 0;
	}

        try {
            for(;;) {
                SFTPPacket pkt = createPacket(SSH_FXP_WRITE, handle);
                pkt.writeLong(foffs);
                lPos = pkt.getWPos();
                pkt.writeInt(0); // write dummy length
                int n = pkt.getMaxWriteSize();
                n = (n > (maxDataSize - lPos - 4) ? (maxDataSize - lPos - 4) : n);
                len = in.read(pkt.getData(), pkt.getWPos(), n);
		
                if(len > 0) {
                    pkt.setWPos(lPos);
                    pkt.writeInt(len); // write real length
                    pkt.setWPos(lPos + 4 + len);
                    writeInternal(handle, pkt, len);
                    foffs += len;
                    if(!isBlocking && ++cnt == MAX_OUTSTANDING_ASYNCH) {
                        cnt = 0;
                        handle.asyncWait(MAX_OUTSTANDING_ASYNCH/2);
                    }

                    if((cnt % (MAX_OUTSTANDING_ASYNCH/4) == 1))
                         Thread.yield();

                } else {
                    break;
                }
            }

            if(!isBlocking) {
                handle.asyncWait();
            }

        } finally {
            close(handle);
        }

        return foffs;
    }

    /**
     * Write data buffer to a file on the server.
     * This method will be much faster if asynchronous mode is
     * used. It will always wait until the operation has completed
     * before returning, even if running in asynchronous mode.
     *
     * @param handle Handle identifying file. The handle will be
     *               closed when the transfer has completed.
     * @param in Buffer containing data to write.
     * @param append if true then the data is written at the end of
     *        the opened file. In this case the handle must have been opened
     *        in append mode.
     */
    public long writeFully(FileHandle handle, byte[] in, boolean append)
        throws SFTPException, IOException {
        return writeFully(handle, new java.io.ByteArrayInputStream(in), append);
    }


    /**
     * Get attributes of a file on the server. If the name refers to a
     * symbolic link, then this version will return information about
     * the actual link.
     *
     * @param name Name of file to get attributes of.
     *
     * @return The attributes of the given name.
     */
    public FileAttributes lstat(String name)
    throws SFTPException {
        SFTPPacket pkt = createPacket(SSH_FXP_LSTAT);
        pkt.writeString(name);

        return statInternal(pkt);
    }

    /**
     * Get attributes of a file on the server. If the name refers to a
     * symbolic link, then this version will return information about
     * the file the link points at.
     *
     * @param name Name of file to get attributes of.
     *
     * @return The attributes of the given name.
     */
    public FileAttributes stat(String name)
    throws SFTPException {
        SFTPPacket pkt = createPacket((version > 0) ? SSH_FXP_STAT : SSH_FXP_OLD_STAT);
        pkt.writeString(name);

        return statInternal(pkt);
    }

    /**
     * Get attributes of an open file on the server.
     *
     * @param handle Handle identifying file.
     *
     * @return The attributes of the given name.
     */
    public FileAttributes fstat(FileHandle handle)
    throws SFTPException {
        SFTPPacket pkt = createPacket(SSH_FXP_FSTAT, handle);

        return statInternal(pkt);
    }

    private FileAttributes statInternal(SFTPPacket pkt) throws SFTPException {
        pkt = transmitExpectReply(pkt, SSH_FXP_ATTRS);
        FileAttributes attrs = pkt.readAttrs();
        releasePacket(pkt);
        return attrs;
    }

    /**
     * Set attributes on a file.
     *
     * @param name Name of file to set attributes on.
     * @param attrs Attributes to set.
     */
    public void setstat(String name, FileAttributes attrs)
    throws SFTPException {
        SFTPPacket pkt = createPacket(SSH_FXP_SETSTAT);
        pkt.writeString(name);
        pkt.writeAttrs(attrs);

        pkt = transmitExpectReply(pkt, SSH_FXP_STATUS);
        releasePacket(pkt);
    }

    /**
     * Set attributes on an open file.
     *
     * @param handle Handle identifying the file.
     * @param attrs Attributes to set.
     */
    public void fsetstat(FileHandle handle, FileAttributes attrs)
    throws SFTPException {
        SFTPPacket pkt = createPacket(SSH_FXP_FSETSTAT, handle);

        pkt.writeAttrs(attrs);

        pkt = transmitExpectReply(pkt, SSH_FXP_STATUS);
        releasePacket(pkt);
    }

    /**
     * Get attributes of a filesystem on the server.
     * This is an OpenSSH extension
     *
     * @param name Path into the filesystem to get data about
     *
     * @return The attributes of the given filesystem.
     */
    public FileSystemAttributes statvfs(String name)
    throws SFTPException {
        if (!extensionStatvfs) {
            throw new SFTPUnsupportedException();
        }
        SFTPPacket pkt = createPacket(SSH_FXP_EXTENDED);
        pkt.writeString("statvfs@openssh.com");
        pkt.writeString(name);

        return statvfsInternal(pkt);
    }

    /**
     * Get attributes of a filesystem on the server.
     * This is an OpenSSH extension
     *
     * @param handle Handle of a file in the filesystem
     *
     * @return The attributes of the given filesystem.
     */
    public FileSystemAttributes fstatvfs(FileHandle handle)
    throws SFTPException {
        if (!extensionFStatvfs) {
            throw new SFTPUnsupportedException();
        }
        SFTPPacket pkt = createPacket(SSH_FXP_EXTENDED);
        pkt.writeString("fstatvfs@openssh.com");
        pkt.writeString(handle.getHandle());

        return statvfsInternal(pkt);
    }


    private FileSystemAttributes statvfsInternal(SFTPPacket pkt)
        throws SFTPException {
        pkt = transmitExpectReply(pkt, SSH_FXP_EXTENDED_REPLY);
        FileSystemAttributes attrs = pkt.readFSAttrs();
        releasePacket(pkt);
        return attrs;
    }


    /**
     * Create a hard link on the server.
     * This is an OpenSSH extension
     *
     * @param oldpath Old path
     * @param newpath New path
     *
     */
    public void hardlink(String oldpath, String newpath)
    throws SFTPException {
        if (!extensionHardLink) {
            throw new SFTPUnsupportedException();
        }
        SFTPPacket pkt = createPacket(SSH_FXP_EXTENDED);
        pkt.writeString("hardlink@openssh.com");
        pkt.writeString(oldpath);
        pkt.writeString(newpath);

        pkt = transmitExpectReply(pkt, SSH_FXP_STATUS);
        releasePacket(pkt);
    }

    /**
     * Opens a directory on the server. This must be done before one
     * can get a list of files contained in the directory.
     *
     * @param path name of directory to open
     *
     * @return A handle to the open directory.
     */
    public FileHandle opendir(String path)
    throws SFTPException {
        SFTPPacket pkt = createPacket(SSH_FXP_OPENDIR);
        pkt.writeString(path);

        pkt = transmitExpectReply(pkt, SSH_FXP_HANDLE);
        FileHandle handle = new FileHandle(path, pkt.readString(), true);
        releasePacket(pkt);
        return handle;
    }

    /**
     * Gets a list of files, and other objects, in an open
     * directory. The handle used here must have been obtained with an
     * earlier call to <code>opendir</code>.
     *
     * @param handle Handle identifying the remote directory.
     *
     * @return An array of attributes with one entry per contained file.
     */
    public FileAttributes[] readdir(FileHandle handle)
    throws SFTPException {
        ArrayList<FileAttributes> result = new ArrayList<FileAttributes>(256);
        FileAttributes item = null;

        try {
            while(true) {
                SFTPPacket pkt = createPacket(SSH_FXP_READDIR, handle);

                pkt = transmitExpectReply(pkt, SSH_FXP_NAME);

                int count = pkt.readInt();

                if(count == 0) {
                    /* Server should send EOF but better safe than sorry... */
                    break;
                }

                for(int i = 0; i < count; i++) {
                    String name     = pkt.readJavaString();
                    pkt.readJavaString();
                    item            = pkt.readAttrs();
                    item.name       = name;
                    item.hasName    = true;
                    result.add(item);
                }

                releasePacket(pkt);
            }
        } catch (SFTPEOFException e) {
            /* End of directory listing */
        }

        return result.toArray(new FileAttributes[result.size()]);
    }

    /**
     * Remove a file from the server.
     *
     * @param name Name of file to remove.
     */
    public void remove(String name) throws SFTPException {
        SFTPPacket pkt = createPacket(SSH_FXP_REMOVE);
        pkt.writeString(name);

        pkt = transmitExpectReply(pkt, SSH_FXP_STATUS);
        releasePacket(pkt);
    }

    /**
     * Rename a file on the server.
     *
     * @param oldName current name of file to rename.
     * @param newName desired new name of file.
     */
    public void rename(String oldName, String newName)
    throws SFTPException {
        if (version < 2) {
            throw new SFTPUnsupportedException();
        }
        SFTPPacket pkt = createPacket(SSH_FXP_RENAME);
        pkt.writeString(oldName);
        pkt.writeString(newName);

        pkt = transmitExpectReply(pkt, SSH_FXP_STATUS);
        releasePacket(pkt);
    }

    /**
     * Rename a file on the server with Posix semantics.
     * This is an OpenSSH extension
     *
     * @param oldName current name of file to rename.
     * @param newName desired new name of file.
     */
    public void posixRename(String oldName, String newName)
    throws SFTPException {
        if (!extensionPosixRename) {
            throw new SFTPUnsupportedException();
        }
        SFTPPacket pkt = createPacket(SSH_FXP_EXTENDED);
        pkt.writeString("posix-rename@openssh.com");
        pkt.writeString(oldName);
        pkt.writeString(newName);

        pkt = transmitExpectReply(pkt, SSH_FXP_STATUS);
        releasePacket(pkt);
    }

    /**
     * Create a new directory on the server.
     *
     * @param name name of directory to create.
     * @param attrs Attributes to apply to the new directory.
     */
    public void mkdir(String name, FileAttributes attrs)
    throws SFTPException {
        SFTPPacket pkt = createPacket(SSH_FXP_MKDIR);
        pkt.writeString(name);
        pkt.writeAttrs(attrs);

        pkt = transmitExpectReply(pkt, SSH_FXP_STATUS);
        releasePacket(pkt);
    }

    /**
     * Removes a directory from the server
     *
     * @param name Name of directory to remove.
     */
    public void rmdir(String name) throws SFTPException {
        SFTPPacket pkt = createPacket(SSH_FXP_RMDIR);
        pkt.writeString(name);

        pkt = transmitExpectReply(pkt, SSH_FXP_STATUS);
        releasePacket(pkt);
    }

    /**
     * Read target of a symbolic link
     *
     * @param path   name of link to read
     */
    public String readlink(String path)
        throws SFTPException {
        if (version < 3) {
            throw new SFTPUnsupportedException();
        }

        SFTPPacket pkt = createPacket(SSH_FXP_READLINK);
        pkt.writeString(path);
        pkt = transmitExpectReply(pkt, SSH_FXP_NAME);

        pkt.readInt(); // Count, always 1
        String name  = pkt.readJavaString();
        pkt.readJavaString(); // longname
        pkt.readAttrs();      // dummy attributes
        releasePacket(pkt);

        return name;
    }

    /**
     * Create a symbolic link on the server.
     *
     * @param linkpath   name of link to create
     * @param targetpath target of link
     */
    public void symlink(String linkpath, String targetpath)
    throws SFTPException {
        if (version < 3) {
            throw new SFTPUnsupportedException();
        }
        SFTPPacket pkt = createPacket(SSH_FXP_SYMLINK);
        if (connection.getTransport().incompatibleSFTPSymlink) {
            pkt.writeString(targetpath);
            pkt.writeString(linkpath);
        } else {
            pkt.writeString(linkpath);
            pkt.writeString(targetpath);
        }

        pkt = transmitExpectReply(pkt, SSH_FXP_STATUS);
        releasePacket(pkt);
    }

    /**
     * Canonalize a given path. The canonalized path will start from
     * the root and will not contain and '<code>..</code>'.
     *
     * @param nameIn Path to canonalize.
     *
     * @return A <code>FileAttributes</code> object with the name
     * filled in.
     */
    public FileAttributes realpath(String nameIn)
    throws SFTPException {
        SFTPPacket pkt = createPacket(SSH_FXP_REALPATH);
        pkt.writeString(nameIn);

        pkt = transmitExpectReply(pkt, SSH_FXP_NAME);
        pkt.readInt();
        String         name     = pkt.readJavaString();
        pkt.readJavaString();
        FileAttributes attrs    = pkt.readAttrs();
        attrs.name    = name;
        attrs.hasName = true;
        releasePacket(pkt);

        return attrs;
    }

    private SFTPPacket transmitExpectReply(SFTPPacket pkt, int expectType)
    throws SFTPException {
        if(!isOpen) {
            throw new SFTPDisconnectException();
        }
        if(isBlocking) {
            synchronized(this) {
                int expectId = pkt.getId();
                pkt.writeTo(out);
                pkt.reset();
                pkt.readFrom(in);
                if(expectId != pkt.readInt()) {
                    throw new SFTPException("SFTP error, invalid packet id");
                }
                checkType(pkt, expectType);
                return pkt;
            }
        }

        Integer   id    = Integer.valueOf(pkt.getId());
        ReplyLock reply = new ReplyLock(expectType);
        replyLocks.put(id, reply);
        transmit(pkt);
        return reply.expect();
    }

    private void startNonblocking() {
        replyLocks = new Hashtable<Integer, ReplyLock>();
        try {
            launchAsyncReceive();
        } catch (SFTPException e) {
            terminate();
        }
    }

    private void transmit(SFTPPacket pkt) {
        try {
            pkt.writeTo(out);
            releasePacket(pkt);
        } catch (Throwable t) {
            connection.getLog().error("SSH2SFTPClient",
                                      "sftpTransmitLoop",
                                      "session was probably closed");
            terminate();
        }
    }

    private void launchAsyncReceive() throws SFTPException {
        SFTPPacket pkt = createPacket();
        pkt.reset();
        pkt.asyncRead(in, this);
    }

    private SFTPPacket createPacket(int type, FileHandle handle) {
        SFTPPacket pkt = createPacket(type);
        pkt.writeString(handle.getHandle());
        return pkt;
    }

    private SFTPPacket createPacket(int type) {
        SFTPPacket pkt = createPacket();
        pkt.reset(type, getNextId());
        return pkt;
    }

    private SFTPPacket createPacket() {
	synchronized(pktPool) {
	    if (pktPool.size() == 0)
		return new SFTPPacket();
	    return pktPool.remove(0);
	}
    }

    private void releasePacket(SFTPPacket pkt) {
	synchronized(pktPool) {
	    pktPool.add(pkt);		
	}
    }

    private static void checkType(SFTPPacket pkt, int type) throws SFTPException {
        if(pkt.getType() == SSH_FXP_STATUS) {
            int error = pkt.readInt();
            String errorMsg = null;
            try {
                // Early versions may no include the error message
                errorMsg = pkt.readJavaString();
            } catch (Throwable t) {}

            if(error == SSH_FX_OK)
                return;
            if(error == SSH_FX_EOF)
                throw new SFTPEOFException(errorMsg);
            if(error == SSH_FX_NO_SUCH_FILE)
                throw new SFTPNoSuchFileException(errorMsg);
            if(error == SSH_FX_PERMISSION_DENIED)
                throw new SFTPPermissionDeniedException(errorMsg);
            if(error == SSH_FX_FAILURE)
                throw new SFTPFailureException(errorMsg);
            if(error == SSH_FX_CONNECTION_LOST)
                throw new SFTPDisconnectException(errorMsg);
            if(error == SSH_FX_OP_UNSUPPORTED)
                throw new SFTPUnsupportedException(errorMsg);
            // !!! TODO: provide error
            throw new SFTPException("Got error: " + error + " ("+errorMsg+")");
        } else if(pkt.getType() != type) {
            // !!! TODO: provide fatal error
            throw new SFTPException("Got unexpected packet: " + pkt.getType() + " != " + type);
        }
    }

    private void cancelAllAsync() {
        if(replyLocks == null) {
            return;
        }
        Enumeration<Integer> ids = replyLocks.keys();
        while(ids.hasMoreElements()) {
            ReplyLock l = replyLocks.remove(ids.nextElement());
            l.cancel();
        }
    }

    private synchronized int getNextId() {
        return id++;
    }

    /*
     * SSH2SFTP.Callback
     */
    public void packetReceived(SFTPPacket pkt) {
        Integer    id;
        ReplyLock  reply;

	if (!isOpen) return; // just drop if already closed

        id    = Integer.valueOf(pkt.readInt());
        reply = replyLocks.remove(id);
        if (reply == null) {
            connection.getLog().error("SSH2SFTPClient",
                                      "packetReceived",
                                      "received unsent id: " +
                                      id);
            connection.getLog().debug2("SSH2SFTPClient",
                                       "sftpReceiveLoop",
                                       "sftp packet: ",
                                       pkt.getData(),
                                       0,
                                       pkt.getLength() + 5);
            terminate();
            return;
        }

        reply.received(pkt);

        try {
            launchAsyncReceive();
        } catch (SFTPException e) {
            terminate();
        }
    }

    public void readFailed(SFTPException e) {
        terminate();
    }
}
