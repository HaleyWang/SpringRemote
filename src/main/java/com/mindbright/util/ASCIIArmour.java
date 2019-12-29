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

package com.mindbright.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;
import java.util.StringTokenizer;

/**
 * Encode/decode binary data to/from ASCII. Typical output looks like this:
 * <pre>
 * -----BEGIN RSA PRIVATE KEY-----
 * Proc-Type: 4,ENCRYPTED
 * DEK-Info: DES-EDE3-CBC,A68BCF5FE86D7652
 *
 * ZlH6zo6nf8mBxFPGkaUkfXtzgUDTKmUYAUO24eXkoV/fbg+IPOtbPdJa7PKlhQTs
 * 2Ycjb5Gk/ZVwGZaqa01roRSmfGDHL2ZSVrXHHMxDxCn1aU+rOFHcUA==
 * -----END RSA PRIVATE KEY-----
 * </pre>
 *
 * @see Base64
 */
public final class ASCIIArmour {
    /**
     * Default length of encoded lines
     */
    public final static int DEFAULT_LINE_LENGTH = 70;

    String    EOL;
    String    headerLine;
    Hashtable<String, String> headerFields;
    Vector<String>    fieldsOrder;
    String    tailLine;

    boolean   blankHeaderSep;
    int       lineLen;
    boolean   haveChecksum;

    boolean   unknownHeaderLines;


    /**
     * Creates an instance ready for encoding or decoding
     *
     * @param headerLine the first line in the file
     * @param tailLine the last line in the file
     * @param blankHeaderSep true if a blank line should follow the
     *                       header lines
     * @param lineLen maximum length of generated lines
     */
    public ASCIIArmour(String headerLine, String tailLine,
                       boolean blankHeaderSep, int lineLen) {
        this.EOL                = "\r\n";
        this.headerLine         = headerLine;
        this.tailLine           = tailLine;
        this.blankHeaderSep     = blankHeaderSep;
        this.lineLen            = lineLen;
        this.unknownHeaderLines = false;
        this.headerFields       = new Hashtable<String, String>();
        this.fieldsOrder        = new Vector<String>();
    }

    /**
     * Creates an instance ready for encoding or decoding
     *
     * @param headerLine the first line in the file
     * @param tailLine the last line in the file
     */
    public ASCIIArmour(String headerLine, String tailLine) {
        this(headerLine, tailLine, false, DEFAULT_LINE_LENGTH);
    }

    /**
     * Creates an instance ready for decoding
     *
     * @param headerLinePrePostFix start of header and tail line
     */
    public ASCIIArmour(String headerLinePrePostFix) {
        this(headerLinePrePostFix, headerLinePrePostFix);
        this.unknownHeaderLines = true;
    }

    /**
     * Set if canoncial end of line markings should be used or not
     *
     * @param value true means CRLF which false means LF
     */
    public void setCanonicalLineEnd(boolean value) {
        if(value) {
            EOL = "\r\n";
        } else {
            EOL = "\n";
        }
    }

    /**
     * Control if a blank line should follow the headers
     */
    public void setBlankHeaderSep(boolean value) {
        this.blankHeaderSep = value;
    }

    /**
     * Set length of encoded lines
     */
    public void setLineLength(int lineLen) {
        this.lineLen = lineLen;
    }

    /**
     * Get the header line
     */
    public String getHeaderLine() {
        return headerLine;
    }

    /**
     * Set the header line
     */
    public void setHeaderLine(String headerLine) {
        unknownHeaderLines = false;
        this.headerLine = headerLine;
    }

    /**
     * Set the tailing line
     */
    public void setTailLine(String tailLine) {
        unknownHeaderLines = false;
        this.tailLine = tailLine;
    }

    /**
     * Get the header fields
     *
     * @return a Hashtable where the leys are the names of the header fields
     */
    public Hashtable<String, String> getHeaderFields() {
        return headerFields;
    }

    /**
     * Get one header field
     */
    public String getHeaderField(String headerName) {
        return headerFields.get(headerName);
    }

    /**
     * Set a header field
     */
    public void setHeaderField(String headerName, String value) {
        if(value != null) {
            headerFields.put(headerName, value);
            fieldsOrder.addElement(headerName.intern());
        } else {
            headerFields.remove(headerName);
            fieldsOrder.removeElement(headerName.intern());
        }
    }

    /**
     * Encode data and return the encoded ascii blob
     *
     * @param data the array which will be encoded
     */
    public byte[] encode(byte[] data) {
        return encode(data, 0, data.length);
    }

    /**
     * Encode data and return the encoded ascii blob
     *
     * @param data array in which the data can be found
     * @param offset offset in array where data begins
     * @param length how many bytes the data consists of
     */
    public byte[] encode(byte[] data, int offset, int length) {
        if(unknownHeaderLines) {
            return null;
        }
        int n = ((length / 3) * 4);
        StringBuilder buf = new StringBuilder(headerLine.length() +
					      tailLine.length() +
					      n + (n / lineLen) + 512);
        buf.append(headerLine);
        buf.append(EOL);

        int headLen = buf.length();

        buf.append(printHeaders());

        if(blankHeaderSep && (headLen < buf.length())) {
            buf.append(EOL);
        }

        byte[] base64 = Base64.encode(data, offset, length);
        for(int i = 0; i < base64.length; i += lineLen) {
            int j = lineLen;
            if(i + j > base64.length)
                j = base64.length - i;
            String line = new String(base64, i, j);
            buf.append(line);
            buf.append(EOL);
        }
        if(haveChecksum) {
            // !!! TODO:
        }
        buf.append(tailLine);
        buf.append(EOL);

        return buf.toString().getBytes();
    }


    /**
     * Encode data and print it to the given OutputStream
     *
     * @param out where to send the encoded blob
     * @param data the array which will be encoded
     */
    public void encode(OutputStream out, byte[] data) throws IOException {
        encode(out, data, 0, data.length);
    }

    /**
     * Encode data and print it to the given OutputStream
     *
     * @param out where to send the encoded blob
     * @param data array in which the data can be found
     * @param off offset in array where data begins
     * @param len how many bytes the data consists of
     */
    public void encode(OutputStream out, byte[] data, int off, int len)
    throws IOException {
        byte[] outData = encode(data, off, len);
        out.write(outData);
    }

    /**
     * Decode the given array. It will also populate the list of
     * header fields.
     *
     * @param data array containg the ascii blob to decode
     *
     * @return the decoded binary blob
     */
    public byte[] decode(byte[] data) {
        return decode(data, 0, data.length);
    }

    /**
     * Decode the given array. It will also populate the list of
     * header fields.
     *
     * @param data array containg the ascii blob to decode
     * @param offset offset in array where data begins
     * @param length how many bytes the data consists of
     *
     * @return the decoded binary blob
     */
    public byte[] decode(byte[] data, int offset, int length) {
        String armourChunk = new String(data, offset, length);
        StringTokenizer st = new StringTokenizer(armourChunk, "\n");
        boolean foundHeader = false;
        boolean foundData   = false;
        boolean foundTail   = false;
        String line = "";
        while(!foundHeader && st.hasMoreTokens()) {
            line = st.nextToken();
            if(line.startsWith(headerLine)) {
                foundHeader = true;
                if(unknownHeaderLines) {
                    headerLine = line;
                }
            }
        }
        headerFields = new Hashtable<String, String>();
        String lastName = null;
        while(!foundData && st.hasMoreTokens()) {
            line = st.nextToken();
            if(lastName != null) {
                String val = headerFields.get(lastName);
                headerFields.put(lastName, val + StringUtil.trimRight(line));
                lastName = null;
                continue;
            }
            int i = line.indexOf(':');
            if(i < 0) {
                foundData = true;
            } else {
                String name  = line.substring(0, i).trim();
                String value = line.substring(i + 1).trim();
                if(value.length() > 0 && value.charAt(0) == '"' &&
                        value.charAt(value.length() - 1) == '\\') {
                    lastName = name;
                    value = value.substring(0, value.length() - 1);
                }
                headerFields.put(name, value);
            }
        }
        if(blankHeaderSep) {
            // !!!
        }
        StringBuilder base64Data = new StringBuilder();
        while(!foundTail) {
            if(line.startsWith(tailLine)) {
                foundTail = true;
                if(unknownHeaderLines) {
                    tailLine = line;
                }
            } else {
                base64Data.append(line);
                if(st.hasMoreTokens())
                    line = st.nextToken();
                else
                    return null;
            }
        }

        data = Base64.decode(base64Data.toString().getBytes());

        return data;
    }

    /**
     * Decode data from the given InputStream. It will also populate
     * the list of header fields.
     *
     * @param in stream to read data from
     *
     * @return the decoded binary blob
     */
    public byte[] decode(InputStream in) throws IOException {
        StringBuilder lineBuf    = new StringBuilder();
        StringBuilder dataBuf    = new StringBuilder();
        int          found      = 0;
        int c;
        boolean eof = false;
        
        while(found < 2) {
            c = in.read();
            if(c == -1) {
                if (eof || found < 1) {
                    throw new IOException("Premature EOF, corrupt ascii-armour");
                }
                eof = true;
                c = '\n';
            }    
            if(c == '\r')
                continue;
            if(c != '\n') {
                lineBuf.append((char)c);
            } else {
                String line = new String(lineBuf);
                if(found == 0) {
                    if(line.startsWith(headerLine)) {
                        dataBuf.append(line);
                        dataBuf.append(EOL);
                        found++;
                    }
                } else {
                    dataBuf.append(line);
                    dataBuf.append(EOL);
                    if(line.startsWith(tailLine)) {
                        found++;
                    }
                }
                lineBuf.setLength(0);
            }
        }
        return decode(dataBuf.toString().getBytes());
    }

    /**
     * Print the header fields to a String.
     */
    public String printHeaders() {
        Enumeration<String> headerNames = fieldsOrder.elements();
        StringBuilder buf = new StringBuilder();
        while(headerNames.hasMoreElements()) {
            String fieldName = headerNames.nextElement();
            buf.append(fieldName);
            buf.append(": ");
            String val = headerFields.get(fieldName);
            if(val.length() > 0 && val.charAt(0) == '"' &&
                    fieldName.length() + 2 + val.length() > lineLen) {
                int n = lineLen - (fieldName.length() + 2);
                buf.append(val.substring(0, n));
                buf.append("\\");
                buf.append(EOL);
                val = val.substring(n);
            }
            buf.append(val);
            buf.append(EOL);
        }
        return buf.toString();
    }

    /* !!! DEBUG
    public static void main(String[] argv) {
    byte[] data = "Hej svejs i lingonskogen!!!".getBytes();
    ASCIIArmour armour =
     new ASCIIArmour("---- BEGIN GARBAGE ----",
       "---- END GARBAGE ----");
    armour.setHeaderField("Subject", "mats");
    armour.setHeaderField("Comment", "\"this is a comment\"");

    byte[] encoded = armour.encode(data);

    System.out.println("Encoded block:");
    System.out.println(new String(encoded));

    System.out.println("Decoded: " + new String(armour.decode(encoded)));
    System.out.println("Headers:");
    System.out.println(armour.printHeaders());

    encoded = ("---- BEGIN SSH2 PUBLIC KEY ----\r\n" +
     "Subject: root\r\n" +
     "Comment: \"host key for hal, accepted by root Mon Sep 20 1999 10:10:02 +0100\"\r\n" +
     "AAAAB3NzaC1kc3MAAACBAKpCbpj86G+05T53tn6Y+tJ1N87Kx2RbQTDC48LWHYNRZ3c4He\r\n" +
     "0tmQNFbyg14m/dYrdBI0GxPWQH0RYuyL5YLhBrcscmdz7Ca8buEgehcQULlAJ1P0gZ3hvW\r\n" +
     "qru55vgU8O0kZVNGSsA+cmXRpq689W6RU0u9qaW03FNdeH7tTq/1AAAAFQDCLg54vUWNe0\r\n" +
     "n5kMFnEH/DiV5dgQAAAIEAmlOAXHQ/3nrFDnLiTIfCkCvAj/P2rMQUViYXXi9cQ+Qd8Ie5\r\n" +
     "TmyFJ6t9iJQZ6x3HlScGfQOJcD4h4ydxuXr+rRd6yi48kSB5/g3EscL+6+LMYdMGSGA2ni\r\n" +
     "l1Vpjm49xZHxHlvTQ+KExk6Pcyb9D5zTW9uoOTBA08SPpYAlbZ4+MAAACAKEeiebGmZg5x\r\n" +
     "sbxQt6HUPU3Cov9KeXw98qmn4Rr2ENWSTriwl8uxoD8wCuURHaJ61YX5spAj4QkVESqc7Y\r\n" +
     "NBcZgpST0sUWCF0rNPZm8D6K0hgaUmtfrUJ6EzwxqfKH3YduMHFz5RSv492TSZvKKv+Ucb\r\n" +
     "X4hEjfmP6SKc+Q4wGaQ=\r\n" +
     "---- END SSH2 PUBLIC KEY ----\r\n").getBytes();
    armour =
     new ASCIIArmour("---- BEGIN SSH2 PUBLIC KEY ----",
       "---- END SSH2 PUBLIC KEY ----");

    byte[] decoded = null;
    try {
     armour =
    new ASCIIArmour("---- BEGIN SSH2 ENCRYPTED PRIVATE KEY ----",
    "---- END SSH2 ENCRYPTED PRIVATE KEY ----");
     decoded = armour.decode(new java.io.FileInputStream("/home/matsa/tstkey.prv"));
    } catch (Exception e) {
     System.out.println("Error: " + e);
    }

    System.out.println("Decoded: ");
    com.mindbright.util.HexDump.hexDump(decoded, 0, decoded.length);
    System.out.println("Headers:");
    System.out.println(armour.printHeaders());

    try {
     java.io.FileOutputStream f =
    new java.io.FileOutputStream("/home/matsa/tstkey2.prv");
     armour.setCanonicalLineEnd(false);
     armour.encode(f, decoded);
     f.close();
    } catch (Exception e) {
     System.out.println("Error: " + e);
    }
    }
    */

}
