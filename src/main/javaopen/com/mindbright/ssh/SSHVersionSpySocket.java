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

package com.mindbright.ssh;

import java.net.Socket;
import java.net.SocketException;
import java.net.InetAddress;
import java.io.*;

public class SSHVersionSpySocket extends Socket {
    PushbackInputStream pbIn;
    Socket              origSocket;

    public SSHVersionSpySocket(Socket origSocket) throws IOException {
        this.origSocket = origSocket;
        pbIn = new PushbackInputStream(origSocket.getInputStream(), 256);
    }

    public int getMajorVersion() throws IOException {
        byte[] buf = new byte[256];
        int    len = 0;
        int    c   = 0;
        String verStr = null;
        int    srvVersionMajor;
        int    srvVersionMinor;
        int    hyphenCnt = 0;

        String stdInit = "SSH-";

        while(hyphenCnt < 2) {
            c = pbIn.read();
            if(c == -1)
                throw new IOException("Server closed connection before sending identification");
            if(c == '-')
                hyphenCnt++;
            buf[len++] = (byte)c;
            if(len < 5) {
                //
                // Handle the fact that in SSH2 there can come lines of text
                // before the identity string
                //
                if(c != stdInit.charAt(len - 1)) {
                    pbIn.unread(buf, 0, len);
                    return 2;
                }
            }
        }
        verStr = new String(buf, 0, len);

        srvVersionMajor = getMajor(verStr);
        srvVersionMinor = getMinor(verStr);
        if(srvVersionMinor == 99)
            srvVersionMajor = 2;

        pbIn.unread(buf, 0, len);

        return srvVersionMajor;
    }

    private static int getMajor(String versionStr) throws IOException {
        try {
            int r = versionStr.indexOf('.', 4);
            return Integer.parseInt(versionStr.substring(4, r));
        } catch (NumberFormatException e) {
            throw new IOException("corrupt version string: " + versionStr);
        }
    }

    private static int getMinor(String versionStr) throws IOException {
        try {
            int l = versionStr.indexOf('.', 4) + 1;
            int r = versionStr.indexOf('-', l);
            return Integer.parseInt(versionStr.substring(l, r));
        } catch (NumberFormatException e) {
            throw new IOException("corrupt version string: " + versionStr);
        }
    }

    public InetAddress getInetAddress() {
        return origSocket.getInetAddress();
    }


    public InetAddress getLocalAddress() {
        return origSocket.getLocalAddress();
    }

    public int getPort() {
        return origSocket.getPort();
    }

    public int getLocalPort() {
        return origSocket.getLocalPort();
    }

    public InputStream getInputStream() throws IOException {
        return pbIn;
    }

    public OutputStream getOutputStream() throws IOException {
        return origSocket.getOutputStream();
    }

    public void setTcpNoDelay(boolean on) throws SocketException {
        origSocket.setTcpNoDelay(on);
    }

    public boolean getTcpNoDelay() throws SocketException {
        return origSocket.getTcpNoDelay();
    }

    public void setSoLinger(boolean on,
                            int val) throws SocketException {
        origSocket.setSoLinger(on, val);
    }

    public int getSoLinger() throws SocketException {
        return origSocket.getSoLinger();
    }

    public synchronized void setSoTimeout(int timeout)
    throws SocketException {
        origSocket.setSoTimeout(timeout);
    }

    public synchronized int getSoTimeout()
    throws SocketException {
        return origSocket.getSoTimeout();
    }

    public synchronized void close()
    throws IOException {
        origSocket.close();
    }

    public String toString() {
        return origSocket.toString();
    }


}
