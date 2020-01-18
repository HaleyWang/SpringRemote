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

import java.net.*;
import java.io.*;

public class SSHFtpTunnel extends SSHTunnel {

    public final static String TUNNEL_NAME       = "#FTP";
    public final static int    MAX_REMOTE_LISTEN = 10;

    boolean havePORT;

    boolean     waitingPASVResponse;
    String      localAddrPASVStr;
    InetAddress localAddr;

    byte[][] newPortMsg;

    static int timeWaitKludgeToggler;
    static Object timeWaitKludgeCrit = new Object();

    public SSHFtpTunnel(Socket ioSocket, int channelId, int remoteChannelId,
                        SSHChannelController controller)
    throws IOException {
        super(ioSocket, channelId, remoteChannelId, controller);

        int a1, a2, a3, a4;

        havePORT = controller.sshAsClient().havePORTFtp;

        if(havePORT) {
            int firstPort = controller.sshAsClient().firstFTPPort;
            byte[] serverAddr = controller.sshAsClient().getServerRealAddr().getAddress();
            a1 = serverAddr[0] & 0xff;
            a2 = serverAddr[1] & 0xff;
            a3 = serverAddr[2] & 0xff;
            a4 = serverAddr[3] & 0xff;
            String msg;
            newPortMsg = new byte[MAX_REMOTE_LISTEN][1];
            for(int i = 0; i < MAX_REMOTE_LISTEN; i++) {
                int p1 = ((firstPort + i) >>> 8) & 0xff;
                int p2 = (firstPort + i) & 0xff;
                msg = "PORT " + a1 + "," + a2 + "," + a3 + "," + a4 + "," + p1 + "," + p2 + "\n";
                newPortMsg[i] = msg.getBytes();
            }
        }

        localAddr = controller.sshAsClient().getLocalAddr();

        byte[] localAddrArr = controller.sshAsClient().getLocalAddr().getAddress();

        if(localAddrArr[0] == 0) {
            localAddrArr = InetAddress.getLocalHost().getAddress();
        }

        a1 = localAddrArr[0] & 0xff;
        a2 = localAddrArr[1] & 0xff;
        a3 = localAddrArr[2] & 0xff;
        a4 = localAddrArr[3] & 0xff;
        localAddrPASVStr = a1 + "," + a2 + "," + a3 + "," + a4;
    }

    boolean parseHostAndPort(String msg, int[] d) {
        boolean ok = true;
        int   cl   = 0;
        int   cn   = 0;
        try {
            for(int i = 0; i < 6; i++) {
                String num;
                if(i == 5) {
                    cn = msg.indexOf(')', cl);
                    if(cn == -1)
                        cn = msg.indexOf('\r', cl);
                    else if(cn == -1)
                        cn = msg.indexOf('\n', cl);
                } else
                    cn = msg.indexOf(',', cl);
                num = msg.substring(cl, cn);
                cl = cn + 1;
                d[i] = Integer.parseInt(num);
            }
        } catch (Exception e) {
            ok = false;
        }
        return ok;
    }

    public void receive(SSHPdu pdu) {
        String msg = new String(pdu.rawData(), pdu.rawOffset(), pdu.rawSize());

        if(msg.startsWith("PASV") || msg.startsWith("pasv")) {
            waitingPASVResponse = true;
        } else if(msg.startsWith("PORT ") || msg.startsWith("port ")) {
            if(!havePORT) {
                controller.alert("Ftp-client is using PORT commands, either \n" +
                                 "enable 'passive mode' in the ftp-client or \n" +
                                 "enable 'ftp PORT' in 'SSH Settings' and reconnect.");
            } else {
                msg = msg.substring(5);
                int[] d = new int[6];

                if(parseHostAndPort(msg, d)) {
                    byte[] dst = pdu.rawData();
                    byte[] newmsg;
                    String mapName;

                    synchronized(timeWaitKludgeCrit) {
                        newmsg  = newPortMsg[timeWaitKludgeToggler];
                        mapName = TUNNEL_NAME + timeWaitKludgeToggler;
                        timeWaitKludgeToggler = (timeWaitKludgeToggler + 1) % MAX_REMOTE_LISTEN;
                    }

                    int len    = newmsg.length;
                    int off    = pdu.rawOffset() - 4;
                    dst[off++] = (byte)((len >>> 24) & 0xff);
                    dst[off++] = (byte)((len >>> 16) & 0xff);
                    dst[off++] = (byte)((len >>> 8)  & 0xff);
                    dst[off++] = (byte)(len & 0xff);
                    System.arraycopy(newmsg, 0, dst, off, len);
                    pdu.rawAdjustSize(off + len);

                    String toHost = d[0] + "." + d[1] + "." + d[2] + "." + d[3];
                    int    toPort = (d[4] << 8) | d[5];
                    controller.addHostMapTemporary(mapName, toHost, toPort);
                } else {
                    controller.alert("Bug in SSHFtpTunnel (PORT), please report: " + msg);
                }
            }
        }

        super.receive(pdu);
    }

    public void transmit(SSHPdu pdu) {
        SSHListenChannel listenChan = null;
        String           msg        = new String(pdu.rawData(), pdu.rawOffset(), pdu.rawSize());

        if(waitingPASVResponse && msg.startsWith("227 ")) {
            byte[] newmsg;
            waitingPASVResponse = false;
            msg = msg.substring(27);
            int[] d = new int[6];
            if(parseHostAndPort(msg, d)) {
                String toHost = d[0] + "." + d[1] + "." + d[2] + "." + d[3];
                int    toPort = (d[4] << 8) | d[5];
                int    localPort;
                try {
                    listenChan = controller.newListenChannel(localAddr.getHostAddress(), 0,
                                 toHost, toPort, "general");
                } catch(IOException e) {
                    controller.alert("Error in FtpTunnel: " + e.toString());
                }
                if(listenChan != null) {
                    listenChan.setTemporaryListener(true); // Go away after next accept...
                    localPort = listenChan.getListenPort();
                    int p1 = (localPort >>> 8) & 0xff;
                    int p2 = localPort & 0xff;
                    msg = "227 Entering Passive Mode (" + localAddrPASVStr + "," + p1 + "," + p2 + ")\n";
                    newmsg = msg.getBytes();
                    pdu.rawSetData(newmsg);
                }
            } else {
                controller.alert("Bug in SSHFtpTunnel (PASV), please report: " + msg);
            }
        }

        super.transmit(pdu);
    }

    public String getDescription() {
        if(ioSocket != null)
            return ioSocket.getInetAddress().getHostAddress() + ":" + ioSocket.getPort() + " <-ftp-> " +
                   getLocalHost() + ":" + ioSocket.getLocalPort() + " <-ssh-> " + remoteDesc;
		return "< N/A >";
    }

}
