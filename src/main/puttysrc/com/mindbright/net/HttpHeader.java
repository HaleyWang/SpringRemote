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

package com.mindbright.net;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Enumeration;
import java.util.NoSuchElementException;

import com.mindbright.util.Base64;
import com.mindbright.net.RFC822Head;

public class HttpHeader {

    private final static String WHITE_SPACE = " \t\r";

    String     startLine;
    RFC822Head headerFields;

    public HttpHeader() {
        headerFields = new RFC822Head();
    }

    public HttpHeader(String fullHeader) throws IOException {
        this(new ByteArrayInputStream(fullHeader.getBytes()));
    }

    public HttpHeader(InputStream input) throws IOException {
        // According to RFC2616 we should accept and ignore initial empty lines
        startLine = readLine(input);
        while(startLine.trim().length() == 0) {
            startLine = readLine(input);
        }
        headerFields = new RFC822Head(input);
    }

    private String readLine(InputStream in) throws IOException {
        StringBuilder lineBuf = new StringBuilder();
        int c;
        while(true) {
            c = in.read();
            if(c == -1)
                throw new IOException("HttpHeader, corrupt header, input stream closed");
            if(c == '\n')
                continue;
            if(c != '\r') {
                lineBuf.append((char)c);
            } else {
                break;
            }
        }
        return new String(lineBuf);
    }

    public String getStartLine() {
        return startLine;
    }

    public void setStartLine(String startLine) {
        this.startLine = startLine;
    }

    public RFC822Head getHeader() {
        return headerFields;
    }

    public String getHeaderField(String headerName) {
        return headerFields.getHeaderField(headerName);
    }

    public void setHeaderField(String headerName, String value) {
        headerFields.setHeaderField(headerName, value);
    }

    public void writeTo(OutputStream output) throws IOException {
        String fullHeader = toString();
        output.write(fullHeader.getBytes());
        output.flush();
    }

    public int getStatus() {
        StringTokenizer items =
            new StringTokenizer(startLine, WHITE_SPACE, false);

        int    status  = -1;
        /* From RFC2616 a respone-line is defined to be:
         * HTTP-Version SP Status-Code SP Reason-Phrase CRLF
         */
        try {
            items.nextToken();
            status  = Integer.parseInt(items.nextToken());
            items.nextToken();
        } catch (NoSuchElementException e) {
            status = -1;
        } catch (NumberFormatException e) {
            status = -1;
        }
        return status;
    }

    // !!! Should have HttpRequest and HttpResponse classes instead...
    //
    /* From RFC2616 a request-line is:
     * Method SP Request-URI SP HTTP-Version CRLF
     */

    public void setBasicProxyAuth(String username, String password) {
        String authStr = username + ":" + password;
        byte[] authB64enc = Base64.encode(authStr.getBytes());
        setHeaderField("Proxy-Authorization", "Basic " +
                       (new String(authB64enc)));
    }

    public String getProxyAuthMethod() {
        String challenge = headerFields.getHeaderField("Proxy-Authenticate");
        String method    = null;
        if(challenge != null) {
            int n = challenge.indexOf(' ');
            if (n >= 0)
                method = challenge.substring(0, n);
            else
                method = challenge;
        }
        return method;
    }

    public String getProxyAuthRealm() {
        String challenge = headerFields.getHeaderField("Proxy-Authenticate");
        String realm = null;
        if(challenge != null) {
            int l, r = challenge.indexOf('=');
            while(r >= 0) {
                l = challenge.lastIndexOf(' ', r);
                realm = challenge.substring(l + 1, r);
                if(realm.equalsIgnoreCase("realm")) {
                    l = r + 2;
                    r = challenge.indexOf('"', l);
                    realm = challenge.substring(l, r);
                    break;
                }
                r = challenge.indexOf('=', r + 1);
            }
        }
        return realm;
    }

    public String toString() {
	StringBuilder sb = new StringBuilder(startLine + "\r\n");
        Enumeration<?> headerNames = headerFields.getHeaderFieldNames();
        while(headerNames.hasMoreElements()) {
            String fieldName = (String)headerNames.nextElement();
	    sb.append(fieldName).append(": ").append
		(headerFields.getHeaderField(fieldName)).append("\r\n");
        }
        sb.append("\r\n");
        return sb.toString();
    }

    /*
    public static void main(String[] argv) {
    try {
     String fullHeader =
    "CONNECT " + "www.foobar.com" + ":" + 4711 + " HTTP/1.0\r\n" +
    "User-Agent: Proxologist/0.1\r\n" +
    "Pragma: No-Cache\r\n" +
    "Proxy-Connection: Keep-Alive\r\n";

    HttpHeader requestHeader = new HttpHeader(fullHeader);

     String authStr    = "foobar" + ":" + "zippo";
     byte[] authB64enc = Base64.encode(authStr.getBytes());
     requestHeader.setHeaderField("Proxy-Authorization", "Basic " + (new String(authB64enc)));

     System.out.println("HTTP header:");

     String req = requestHeader.toString();
     System.out.print(req);

     requestHeader = new HttpHeader(req);

     System.out.println("HTTP startline: " + requestHeader.getStartLine());
     System.out.println("HTTP proxy-auth: " + requestHeader.getHeaderField("proxy-authorization"));
    } catch (Exception e) {
     System.out.println("Error: " + e);
    }
    }
    */

}
