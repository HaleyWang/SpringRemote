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

public class ASN1String extends ASN1Object {

    protected byte[] value;

    public ASN1String(int tag) {
        super(tag);
    }

    public int encodeValue(ASN1Encoder encoder, OutputStream out)
    throws IOException {
        return encoder.encodeString(out, value);
    }

    public void decodeValue(ASN1Decoder decoder, InputStream in, int len)
        throws IOException {
        setRaw(decoder.decodeString(in, len, this));
    }

    public void setRaw(byte[] value) {
        setValue();
        this.value = value;
    }

    public byte[] getRaw() {
        return value;
    }

}
