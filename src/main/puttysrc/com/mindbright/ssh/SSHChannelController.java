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

import java.io.*;
import java.util.Vector;

import com.mindbright.nio.NQueue;
import com.mindbright.nio.NQueueCallback;
import com.mindbright.nio.NonBlockingOutput;
import com.mindbright.nio.Switchboard;

import com.mindbright.util.Queue;

public final class SSHChannelController extends SSH
    implements SSHChannelListener, NQueueCallback {

    protected SSHConnectChannel   cnChan;
    protected NQueue              txQueue;
    protected Queue               cnQueue;

    protected int      totalTunnels;
    protected int      nextEmptyChan;
    protected Object[] tunnels;

    protected Vector<SSHListenChannel> listenChannels;

    protected SSH        sshHook;
    protected SSHConsole console;

    protected SSHCipher     sendCipher;
    protected SSHCompressor sendComp;
    
    private NonBlockingOutput sshOut;
    private Object            disconnectedMonitor;
    private boolean           isConnected;

    public SSHChannelController(SSH sshHook, Switchboard switchboard,
                                NonBlockingOutput sshOut,
                                SSHCipher sendCipher, SSHCompressor sendComp,
                                SSHConsole console, boolean haveCnxWatch) {
        this.sendCipher = sendCipher;
        this.sendComp   = sendComp;
        this.sshOut     = sshOut;

        this.sshHook = sshHook;
        this.console = console;

        this.tunnels         = new Object[16];
        this.nextEmptyChan   = 0;
        this.totalTunnels    = 0;
        this.listenChannels  = new Vector<SSHListenChannel>();

        this.disconnectedMonitor = new Object();

        txQueue = new NQueue(this);

        if(haveCnxWatch) {
            cnChan = new SSHConnectChannel(this);
            cnChan.setSSHChannelListener(this);
            cnQueue = cnChan.getQueue();
        } else {
            cnQueue = new Queue();
        }

    }

    public void start() {
        isConnected = true;
        if(cnChan != null)
            cnChan.start();
    }

    public void exit() {
        // Trigger an exit
        synchronized(disconnectedMonitor) {
            disconnectedMonitor.notify();
        }
    }

    public void waitForExit() {
        waitForExit(0); // Wait forever...
    }

    public void waitForExit(long msWait) {
        synchronized(disconnectedMonitor) {
            boolean interrupted;
            do {
                interrupted = false;
                try {
                    disconnectedMonitor.wait(msWait);
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            } while (interrupted);
        }
        killAll();
    }

    @SuppressWarnings("deprecation")
	public void killAll() {
        killAllTunnels();
        killListenChannels();
        if(cnChan != null && cnChan.isAlive())
            cnChan.stop();

        cnChan = null;
        System.runFinalization();
    }

    public synchronized int newChannelId() {
        int newChan = nextEmptyChan;
        if(nextEmptyChan < tunnels.length) {
            int i;
            for(i = nextEmptyChan + 1; i < tunnels.length; i++)
                if(tunnels[i] == null)
                    break;
            nextEmptyChan = i;
        } else {
            Object[] tmp = new Object[tunnels.length + 16];
            System.arraycopy(tunnels, 0, tmp, 0, tunnels.length);
            tunnels = tmp;
            nextEmptyChan++;
        }

        return newChan;
    }

    public synchronized String[] listTunnels() {
        int i, cnt = 0;
        String[] list1 = new String[tunnels.length];

        for(i = 0; i < tunnels.length; i++) {
            if(tunnels[i] == null)
                continue;
            list1[cnt++] = ((SSHTunnel)tunnels[i]).getDescription();
        }

        String[] list2 = new String[cnt];
        System.arraycopy(list1, 0, list2, 0, cnt);

        return list2;
    }

    public synchronized void closeTunnelFromList(int listIdx) {
        int i;
        for(i = 0; i < tunnels.length; i++) {
            if(tunnels[i] == null)
                continue;
            listIdx--;
            if(listIdx < 0)
                break;
        }
        if(i < tunnels.length) {
            ((SSHTunnel)tunnels[i]).terminateNow();
        }
    }

    public synchronized void killAllTunnels() {
        for(int i = 0; i < tunnels.length; i++) {
            if(tunnels[i] == null)
                continue;
            ((SSHTunnel)tunnels[i]).openFailure(); // !!! Forced close
            tunnels[i] = null;
        }
        tunnels = new Object[16];
    }

    public synchronized void addTunnel(SSHTunnel tunnel) {
        totalTunnels++;
        tunnels[tunnel.channelId] = tunnel;
    }

    public synchronized SSHTunnel delTunnel(int channelId) {
        SSHTunnel tunnelToDelete = (SSHTunnel) tunnels[channelId];
        tunnels[channelId] = null;
        nextEmptyChan = (channelId < nextEmptyChan ? channelId :nextEmptyChan);
        totalTunnels--;
        return tunnelToDelete;
    }

    public boolean haveHostInFwdOpen() {
        return sshHook.isProtocolFlagSet(PROTOFLAG_HOST_IN_FWD_OPEN);
    }

    public SSHListenChannel newListenChannel(String localHost, int localPort,
                                             String remoteHost, int remotePort,
                                             String plugin) throws IOException{
        SSHListenChannel newListenChan = null;
        newListenChan = SSHProtocolPlugin.getPlugin(plugin).localListener(
            localHost, localPort, remoteHost, remotePort, this);
        newListenChan.setSSHChannelListener(this);
        newListenChan.start();
        synchronized(listenChannels) {
            listenChannels.addElement(newListenChan);
        }
        return newListenChan;
    }

    public void killListenChannel(String localHost, int listenPort) {
        SSHListenChannel listenChan;
        synchronized(listenChannels) {
            for(int i = 0; i < listenChannels.size(); i++) {
                listenChan = listenChannels.elementAt(i);
                if(listenChan.getListenPort() == listenPort
                   && listenChan.getListenHost().equals(localHost)) {
                    listenChannels.removeElementAt(i);
                    listenChan.forceClose();
                    break;
                }
            }
        }
    }

    public void killListenChannels() {
        SSHListenChannel listenChan;
        synchronized(listenChannels) {
            while(listenChannels.size() > 0) {
                listenChan = listenChannels.elementAt(0);
                listenChan.forceClose();
                listenChannels.removeElementAt(0);
            }
        }
    }

    public SSHPdu prepare(SSHPdu pdu) {
        return pdu;
    }

    public void transmit(SSHPdu pdu) {
        txQueue.append(pdu);
    }

    public void receive(SSHPdu pdu) {
        SSHPduInputStream inPdu = (SSHPduInputStream) pdu;
        SSHTunnel         tunnel;
        int               channelNum;
        try {
            switch(inPdu.type) {
            case SMSG_STDOUT_DATA:
                if(console != null)
                    console.stdoutWriteString(inPdu.readStringAsBytes());
                break;
            case SMSG_STDERR_DATA:
                if(console != null)
                    console.stderrWriteString(inPdu.readStringAsBytes());
                break;
            case SMSG_EXITSTATUS:
                SSHPduOutputStream exitPdu =
                    new SSHPduOutputStream(CMSG_EXIT_CONFIRMATION, sendCipher,
                                           sendComp, secureRandom());
                int status = inPdu.readInt();
                if(console != null) {
                    String rhost = "remote host";
		    if (sshAsClient().getServerAddr() != null)
			    rhost = sshAsClient().getServerAddr().getHostName();
                    if(status != 0)
                        console.serverDisconnect(rhost + " disconnected: " + status);
                    else
                        console.serverDisconnect("Connection to " + rhost + " closed.");
                }
                transmit(exitPdu);
                sshAsClient().disconnect(true);

                // Notify any waiters
                isConnected = false;
                synchronized(disconnectedMonitor) {
                    disconnectedMonitor.notify();
                }
                break;
            case SMSG_X11_OPEN:
                // Fallthrough
            case MSG_PORT_OPEN:
                cnQueue.putLast(inPdu);
                break;
            case MSG_CHANNEL_DATA:
                channelNum = inPdu.readInt();
                tunnel     = (SSHTunnel)tunnels[channelNum];
                if(tunnel != null)
                    tunnel.transmit(pdu);
                else
                    throw new Exception("Data on nonexistent channel: " +
                                        channelNum);
                break;
            case MSG_CHANNEL_OPEN_CONFIRMATION:
                channelNum  = inPdu.readInt();
                tunnel = (SSHTunnel)tunnels[channelNum];
                if(tunnel != null) {
                    if(!tunnel.setRemoteChannelId(inPdu.readInt()))
                        throw new Exception(
                            "Open confirmation on already opened channel!");
                    tunnel.start();
                } else
                    throw new Exception("Open confirm on nonexistent: " +
                                        channelNum);
                break;
            case MSG_CHANNEL_OPEN_FAILURE:
                SSHTunnel failTunnel;
                channelNum = inPdu.readInt();
                if((failTunnel = delTunnel(channelNum)) != null) {
                    alert("Channel open failure on " + failTunnel.remoteDesc);
                    failTunnel.openFailure();
                } else
                    throw new Exception(
                        "Open failure on nonexistent channel: " + channelNum);
                break;
            case MSG_CHANNEL_INPUT_EOF:
                channelNum = inPdu.readInt();
                tunnel     = (SSHTunnel)tunnels[channelNum];
                if(tunnel != null) {
                    tunnel.receiveInputEOF();
                } else
                    throw new Exception(
                        "Input eof on nonexistent channel: " + channelNum);
                break;
            case MSG_CHANNEL_OUTPUT_CLOSED:
                channelNum  = inPdu.readInt();
                ;
                if (channelNum < tunnels.length
                    && ((tunnel = (SSHTunnel)tunnels[channelNum]) != null)) {
                    tunnel.receiveOutputClosed();
                }
                break;
            case MSG_DISCONNECT:
                disconnect("Peer disconnected: " + inPdu.readString());
                break;
            case CMSG_WINDOW_SIZE:
                break;
            case CMSG_STDIN_DATA:
                break;
            case CMSG_EOF:
                System.out.println("!!! EOF received...");
                break;
            case CMSG_EXIT_CONFIRMATION:
                break;
            default:
                throw new Exception("Unknown packet type (" + inPdu.type +
                                    "), disconnecting...");
            }
        } catch(Exception e) {
            // !!! Are there known BUGS in here?? Nah... :-)
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            System.out.println("\nBug found: " + e.getMessage());
            System.out.println(sw.toString());
            sendDisconnect("Bug found: " + e.getMessage() + "\n\r" +
                           kludgeLF2CRLFMap(sw.toString()));
            // !!!
        }
    }

    static String kludgeLF2CRLFMap(String orig) {
        int o = 0, n;
        StringBuilder sb = new StringBuilder("");
        while((n = orig.indexOf('\n', o)) != -1) {
            sb.append(orig.substring(o, n)).append("\n\r");
            o = n + 1;
        }
        sb.append(orig.substring(o));
        return sb.toString();
    }

    public void close(SSHChannel chan) {
        // !!!
        if (chan instanceof SSHConnectChannel) {
            SSH.logExtra("Controller connect-channel closed");
        } else if (chan instanceof SSHTxChannel) {
            SSH.logExtra("Controller TX-channel closed");
        } else if (chan instanceof SSHListenChannel) {
            SSH.logExtra("Listen channel for port " +
                         ((SSHListenChannel)chan).getListenPort() + " closed");
        } else {
            alert("Bug in SSHChannelController.close 'chan' is: " + chan);
        }
    }

    public void disconnect(String reason) {
        if (sshHook.isAnSSHClient) {
            sshAsClient().disconnect(false);
        }
        try {
            sshOut.close();
        } catch (IOException e) {}
        if(console != null) {
            console.serverDisconnect("\r\nDisconnecting, " + reason);
        } else {
            SSH.log("\r\nDisconnecting, " + reason);
        }

        isConnected = false;
        synchronized(disconnectedMonitor) {
            disconnectedMonitor.notify();
        }
    }

    public void sendDisconnect(String reason) {
        if (!isConnected) {
            // There is no need to do this if we are not connected
            return;
        }
        try {
            SSHPduOutputStream pdu = new SSHPduOutputStream(MSG_DISCONNECT,
                                     sendCipher, sendComp,
                                     secureRandom());
            pdu.writeString(reason);
            if(txQueue != null)
                txQueue.append(pdu);
            Thread.sleep(300);
            disconnect(reason);
        } catch (Exception e) {
            alert("Error in sendDisconnect: " + e.toString());
        }
    }

    public void alert(String msg) {
        if(sshHook.isAnSSHClient) {
            SSHInteractor interactor = sshAsClient().user.getInteractor();
            if(interactor != null)
                interactor.alert(msg);
        } else {
            SSH.log(msg);
        }
    }

    protected SSHClient sshAsClient() {
        return (SSHClient)sshHook;
    }

    public Queue getCnQueue() {
        return cnQueue;
    }

    public void addHostMapTemporary(String fromHost, String toHost,int toPort){
        cnChan.addHostMapTemporary(fromHost, toHost, toPort);
    }

    public void addHostMapPermanent(String fromHost, String toHost,int toPort){
        cnChan.addHostMapPermanent(fromHost, toHost, toPort);
    }

    public void delHostMap(String fromHost) {
        cnChan.delHostMap(fromHost);
    }

    public Vector<Object> getHostMap(String fromHost) {
        return cnChan.getHostMap(fromHost);
    }

    /*
     * NQueueCallback interface
     */
    public void handleQueue(Object obj) {
        SSHPdu pdu = (SSHPdu)obj;
        try {
            pdu.writeTo(sshOut);
        } catch (IOException e) {
            // Ignore
        }
    }
}
