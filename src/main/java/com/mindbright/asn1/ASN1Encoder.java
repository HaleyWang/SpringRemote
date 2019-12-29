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

import java.io.OutputStream;
import java.io.IOException;
import java.math.BigInteger;

public interface ASN1Encoder {
    public int encode(OutputStream out, ASN1Object object) throws IOException;
    public int encodeBoolean(OutputStream out, boolean b) throws IOException;
    public int encodeInteger(OutputStream out, BigInteger i) throws IOException;
    public int encodeNull(OutputStream out) throws IOException;
    public int encodeOID(OutputStream out, int[] oid) throws IOException;
    public int encodeString(OutputStream out, byte[] string) throws IOException;
    public int encodeStructure(OutputStream out, ASN1Structure struct)
    throws IOException;
}
