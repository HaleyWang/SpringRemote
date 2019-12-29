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
import java.io.FilterOutputStream;
import java.io.IOException;

/**
 * Implements a stream filter which handles the X11 authentication
 * cookie replacement.
 */
public class SSH2X11Filter implements SSH2StreamFilter, SSH2StreamFilterFactory {

    protected final static String X11AUTH_PROTO = "MIT-MAGIC-COOKIE-1";

    class X11Output extends FilterOutputStream {

        byte[]  buf;
        int     idx;
        int     wantedLen;
        int     protoLen;
        int     cookieLen;
        boolean readAuth;

        public X11Output(OutputStream toBeFiltered) {
            super(toBeFiltered);
            this.buf       = new byte[1024];
            this.readAuth  = false;
            this.idx       = 0;
            this.wantedLen = 12; // header len
        }

        public void write(byte b[], int off, int len) throws IOException {
            if(!readAuth) {
                int n;

                // Read header of authentication packet
                //
                if(idx < 12) {
                    n = readMore(b, off, len);
                    len -= n;
                    off += n;
                    if(wantedLen == 0) {
                        if(buf[0] == 0x42) {
                            protoLen  =
                                ((buf[6] & 0xff) << 8) | (buf[7] & 0xff);
                            cookieLen =
                                ((buf[8] & 0xff) << 8) | (buf[9] & 0xff);
                        } else if(buf[0] == 0x6c) {
                            protoLen  =
                                ((buf[7] & 0xff) << 8) | (buf[6] & 0xff);
                            cookieLen =
                                ((buf[9] & 0xff) << 8) | (buf[8] & 0xff);
                        } else {
                            throw new IOException("Corrupt X11 authentication");
                        }
                        wantedLen  = (protoLen + 0x03) & ~0x03;
                        wantedLen += (cookieLen + 0x03) & ~0x03;
                        if(wantedLen + idx > buf.length) {
                            throw new IOException("Corrupt X11 authentication");
                        }
                        if(wantedLen == 0) {
                            throw
                            new IOException("No X11 authentication cookie");
                        }
                    }
                }

                // Read payload of authentication packet
                //
                if(len > 0) {
                    n = readMore(b, off, len);
                    len -= n;
                    off += n;
                    if(wantedLen == 0) {
                        byte[] fakeCookie = connection.getX11FakeCookie();
                        String protoStr   = new String(buf, 12, protoLen);
                        byte[] recCookie  = new byte[fakeCookie.length];

                        protoLen = ((protoLen + 0x03) & ~0x03);

                        System.arraycopy(buf, 12 + protoLen,
                                         recCookie, 0, fakeCookie.length);
                        if(!X11AUTH_PROTO.equals(protoStr) ||
                                !compareCookies(fakeCookie, recCookie,
                                                fakeCookie.length)) {
                            throw new IOException("X11 authentication failed");
                        }
                        byte[] realCookie = connection.getX11RealCookie();
                        if(realCookie.length != cookieLen) {
                            throw new IOException("X11 wrong cookie length");
                        }
                        System.arraycopy(realCookie, 0, buf, 12 + protoLen,
                                         realCookie.length);
                        readAuth = true;
                        out.write(buf, 0, idx);
                        buf = null;
                    }
                }

                if(!readAuth || len == 0) {
                    return;
                }
            }

            out.write(b, off, len);
        }

        private boolean compareCookies(byte[] src, byte[] dst, int len) {
            int i = 0;
            for(; i < len; i++) {
                if(src[i] != dst[i]) {
                    break;
                }
            }
            return i == len;
        }

        private int readMore(byte[] b, int off, int len) {
            if(len > wantedLen) {
                System.arraycopy(b, off, buf, idx, wantedLen);
                idx      += wantedLen;
                len       = wantedLen;
                wantedLen = 0;
            } else {
                System.arraycopy(b, off, buf, idx, len);
                idx       += len;
                wantedLen -= len;
            }
            return len;
        }

    }

    private static SSH2X11Filter factoryInstance;

    public SSH2X11Filter() {
        //
        // Factory instance constructor
        //
    }

    public static synchronized SSH2X11Filter getFilterFactory() {
        if(factoryInstance == null) {
            factoryInstance = new SSH2X11Filter();
        }
        return factoryInstance;
    }

    protected SSH2Connection    connection;
    protected X11Output         x11Out;

    protected SSH2X11Filter(SSH2Connection connection,
                            SSH2StreamChannel channel) {
        this.connection = connection;
    }

    public SSH2StreamFilter createFilter(SSH2Connection connection,
                                         SSH2StreamChannel channel) {
        return new SSH2X11Filter(connection, channel);
    }

    public InputStream getInputFilter(InputStream toBeFiltered) {
        return toBeFiltered;
    }

    public OutputStream getOutputFilter(OutputStream toBeFiltered) {
        this.x11Out = new X11Output(toBeFiltered);
        return this.x11Out;
    }

}
