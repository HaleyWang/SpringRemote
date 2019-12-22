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

package com.mindbright.terminal;

/** 
 * Handles the sending of chaff to disguise the timing of key
 * presses. Scientific research has shown that it may be possible to
 * use keyboard timings to greatly reduce the work needed to perform a
 * brute force password guessing attack. That is one can see the
 * timing between the different keys as the user types them then this
 * gives information about the password. To combat this MindTerm
 * includes a technique called chaffing. When chaffic the client send
 * a steady stream of data with fixed intervals, if there is no typed
 * character to send a fake character is send instead. This kind of
 * assumes an encrypted session since otherwise an attacker who can
 * see timings coudl also see the password.
 * <p>
 * Chaffing means extra traffic so it should only be enabled when
 * eeded. Fortunately it is often not needed since normally the
 * terminal buffers the typed characters and sends them line-by
 * line. The big exception is when sending passwords when the terminal
 * is in non-echo mode.
 * <p>
 * Mote that this is not an issue for the initial password exchange in
 * the SSH protocol but only applies when one sends passwords over a
 * terminal session running over the encrypted link.
 */
public abstract class TerminalInputChaff implements TerminalInputListener,
    Runnable {
    private Thread           chaffThread;
    private volatile boolean chaffActive;
    private volatile long    chaffLastKeyTime;

    private int[] lastKeyBuf;
    private int   lastKeyROffs;
    private int   lastKeyWOffs;

    /**
     * Start sending chaff
     */
    public void startChaff() {
        if(!chaffActive) {
            chaffActive  = true;
            lastKeyBuf   = new int[4];
            lastKeyROffs = 0;
            lastKeyWOffs = 0;
            chaffThread  = new Thread(this, "SSH2TerminalAdapterImpl.chaff");
            chaffThread.setDaemon(true);
            chaffThread.start();
        }
    }

    /**
     * Stop sending chaff
     */
    public void stopChaff() {
        if(chaffActive) {
            chaffActive = false;
            synchronized(chaffThread) {
                chaffThread.notify();
            }
            chaffThread = null;
        }
    }

    /**
     * Tell if chaffing is active or not
     *
     * @return true if chaff is being sent
     */
    protected boolean isChaffActive() {
        return chaffActive;
    }

    /**
     * Receive a character typed by the user, the whole point of
     * chaffing is to disguise the timing of calls to this function.
     *
     * @param c typed character
     */
    public void typedChar(char c) {
        if(chaffThread == null) {
            sendTypedChar(c);
        } else {
            synchronized(this) {
                lastKeyBuf[lastKeyWOffs++] = c;
                lastKeyWOffs &= 0x03;
            }
            dispenseChaff();
        }
    }

    /**
     * Receive a character typed by the user, the whole point of
     * chaffing is to disguise the timing of calls to this function.
     *
     * @param b byte array contained the encoded version of the character
     */
    public void typedChar(byte[] b) {
        for (int i=0; i<b.length; i++) {
            typedChar((char)b[i]);
        }
    }

    /**
     * This callback is only interesting for local input listeners
     * such as LineReaderTerminal
     */
    public void signalTermTypeChanged(String newTermType) {
    }

    /**
     * Classes derived from this class that are capable of sending
     * a break signal can implement this.
     */
    public void sendBreak() {
    }

    /**
     * The thread which actually sends the chaff or real data.
     */
    public void run() {
        long now;
        int  wait;
        while(chaffActive) {
            try {
                synchronized(chaffThread) {
                    chaffThread.wait();
                }
                wait = (int)(System.currentTimeMillis() ^
                             (new Object()).hashCode()) & 0x1ff;
                do {
                    int lastKeyChar = -1;
                    synchronized(this) {
                        if(lastKeyWOffs != lastKeyROffs) {
                            lastKeyChar = lastKeyBuf[lastKeyROffs++];
                            lastKeyROffs &= 0x03;
                        }
                    }
                    if(lastKeyChar >= 0) {
                        sendTypedChar(lastKeyChar);
                    } else {
                        sendFakeChar();
                    }
                    Thread.sleep(30);
                    now = System.currentTimeMillis();
                } while(chaffActive &&
                        (now - chaffLastKeyTime) < (1500 + wait));
            } catch (InterruptedException e) {
                /* don't care... */
            }
        }
    }

    /**
     * Kick chaff thread
     */
    public void dispenseChaff() {
        if(chaffThread != null) {
            long now = System.currentTimeMillis();
            if((now - chaffLastKeyTime) > 1000) {
                chaffLastKeyTime = now;
                synchronized(chaffThread) {
                    chaffThread.notify();
                }
            }
        }
    }

    /**
     * Send a real typed character to the server.
     */
    protected abstract void sendTypedChar(int c);

    /**
     * Send a fake character to the server.
     */
    protected abstract void sendFakeChar();
}
