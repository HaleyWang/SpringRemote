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

package com.mindbright.asn1;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public class ASN1OID extends ASN1Object {

    private int[] value;

    public ASN1OID() {
        super(ASN1.TAG_OID);
    }

    public int encodeValue(ASN1Encoder encoder, OutputStream out)
    throws IOException {
        return encoder.encodeOID(out, value);
    }

    public void decodeValue(ASN1Decoder decoder, InputStream in, int len)
    throws IOException {
        setValue(decoder.decodeOID(in, len));
    }

    public void setValue(int[] value) {
        setValue();
        this.value = value;
    }

    public int[] getValue() {
        return value;
    }

    public void setString(String oid) throws NumberFormatException {
        int[] value = new int[(oid.length() / 2) + 1];
        int i = 0;
        int l = 0;
        int r = 0;
        while(r < oid.length()) {
            r = oid.indexOf('.', r + 1);
            if(r == -1) {
                r = oid.length();
            }
            value[i++] = Integer.parseInt(oid.substring(l, r));
            l = r + 1;
        }
        int[] tmp = value;
        value = new int[i];
        System.arraycopy(tmp, 0, value, 0, i);
        setValue(value);
    }

    public String getString() {
        StringBuilder buf = new StringBuilder();
        for(int i = 0; i < value.length - 1; i++) {
            buf.append(value[i] + ".");
        }
        buf.append(value[value.length - 1]);
        return buf.toString();
    }

    public boolean equals(Object obj) {
        if(obj == null || !(obj instanceof ASN1OID)) {
            return false;
        }
        ASN1OID other = (ASN1OID)obj;
        return getString().equals(other.getString());
    }

    public int hashCode() {
        int hc = super.hashCode();
        if(value != null) {
            hc = getString().hashCode();
        }
        return hc;
    }

}
