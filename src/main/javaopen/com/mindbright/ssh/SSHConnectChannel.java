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
import java.util.Hashtable;
import java.util.Vector;

public class SSHConnectChannel extends SSHTxChannel {
    SSHChannelController controller;

    Hashtable<String, Vector<Object>> hostMap;

    public SSHConnectChannel(SSHChannelController controller) {
        super(null, SSH.CONNECT_CHAN_NUM);
        this.controller     = controller;
        this.hostMap        = new Hashtable<String, Vector<Object>>();
    }

    public synchronized void addHostMapPermanent(String fromHost, String toHost, int toPort) {
        Vector<Object> hostPortPair = new Vector<Object>();
        hostPortPair.addElement(toHost);
        hostPortPair.addElement(Integer.valueOf(toPort));
        hostPortPair.addElement(Boolean.valueOf(true));
        hostMap.put(fromHost, hostPortPair);
    }
    public synchronized void addHostMapTemporary(String fromHost, String toHost, int toPort) {
        Vector<Object> hostPortPair = new Vector<Object>();
        hostPortPair.addElement(toHost);
        hostPortPair.addElement(Integer.valueOf(toPort));
        hostPortPair.addElement(Boolean.valueOf(false));
        hostMap.put(fromHost, hostPortPair);
    }

    public synchronized void delHostMap(String fromHost) {
        hostMap.remove(fromHost);
    }

    public synchronized Vector<Object> getHostMap(String fromHost) {
        Vector<Object> hostPortPair = hostMap.get(fromHost);
        if(hostPortPair != null && !(((Boolean)hostPortPair.elementAt(2)).booleanValue())) {
            delHostMap(fromHost);
        }
        return hostPortPair;
    }

    int displayNumber(String display) {
        int hostEnd;
        int dispEnd;
        int displayNum;
        if(display == null || display.equals("") ||
                (hostEnd = display.indexOf(':')) == -1)
            return 0;

        if((dispEnd = display.indexOf('.', hostEnd)) == -1)
            dispEnd = display.length();

        try {
            return Integer.parseInt(display.substring(hostEnd + 1, dispEnd));
        } catch (Exception e) {
            // !!!
            displayNum = 0;
        }
        return displayNum;
    }

    String displayHost(String display) {
        int hostEnd;
        if(display == null || display.equals("") ||
                display.charAt(0) == ':' || display.indexOf("unix:") == 0 ||
                (hostEnd = display.indexOf(':')) == -1)
            return "localhost";
        return display.substring(0, hostEnd);
    }

    public void serviceLoop() throws Exception {
        SSHPduInputStream inPdu;
        int               remoteChannel;
        int               port;
        String            host;
        String            origin;
        Socket            fwdSocket;

        for(;;) {
            inPdu         = (SSHPduInputStream) queue.getFirst();
            remoteChannel = inPdu.readInt();

            if(inPdu.type == SSH.SMSG_X11_OPEN) {
                if(!controller.sshAsClient().user.wantX11Forward()) {
                    controller.alert("Something is fishy with the server, unsolicited X11 forward!");
                    throw new Exception("Something is fishy with the server, unsolicited X11 forward!");
                }
                String display = controller.sshAsClient().user.getDisplay();
                host = displayHost(display);
                port = 6000 + displayNumber(display);
            } else {
                host = inPdu.readString();
                port = inPdu.readInt();
            }

            if(controller.haveHostInFwdOpen())
                origin = inPdu.readString();
            else
                origin = "unknown (origin-option not used)";

            // See if there is a translation entry for this host
            //
            Vector<Object> hostPortPair = getHostMap(host);
            if(hostPortPair != null) {
                host = (String)hostPortPair.elementAt(0);
                port = ((Integer)hostPortPair.elementAt(1)).intValue();
            }

            SSHPduOutputStream respPdu;

            try {
                fwdSocket        = new Socket(host, port);
                int newChan      = controller.newChannelId();
                SSHTunnel tunnel = new SSHTunnel(fwdSocket, newChan, remoteChannel, controller);
                controller.addTunnel(tunnel);
                tunnel.setRemoteDesc(origin);

                respPdu = new SSHPduOutputStream(SSH.MSG_CHANNEL_OPEN_CONFIRMATION,
                                                 controller.sndCipher,
                                                 controller.sndComp,
                                                 controller.secureRandom());
                respPdu.writeInt(remoteChannel);
                respPdu.writeInt(newChan);

                SSH.log("Port open (" + origin + ") : " + host + ": " + port +
                        " (#" + remoteChannel + ")" + " new: " + newChan);

                controller.transmit(respPdu);

                // We must wait until after we have put the response in the
                // controllers tx-queue with starting the tunnel
                // (to avoid data reaching the server before the response)
                //
                tunnel.start();

            } catch (IOException e) {
                respPdu = new SSHPduOutputStream(SSH.MSG_CHANNEL_OPEN_FAILURE,
                                                 controller.sndCipher,
                                                 controller.sndComp,
                                                 controller.secureRandom());
                respPdu.writeInt(remoteChannel);

                controller.alert("Failed port open (" + origin + ") : " + host + ": " + port +
                                 " (#" + remoteChannel + ")");

                controller.transmit(respPdu);
            }

        }
    }

}
