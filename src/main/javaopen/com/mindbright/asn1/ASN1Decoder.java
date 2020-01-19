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
import java.io.IOException;
import java.math.BigInteger;

public interface ASN1Decoder {
    public int        decode(InputStream in, ASN1Object object)
    throws IOException;
    public void decodeValue(InputStream in, int tag, int len,
                            ASN1Object object) throws IOException;
    public boolean    decodeBoolean(InputStream in, int len) throws IOException;
    public BigInteger decodeInteger(InputStream in, int len) throws IOException;
    public void       decodeNull(InputStream in, int len) throws IOException;
    public int[]      decodeOID(InputStream in, int len) throws IOException;
    public byte[]     decodeString(InputStream in, int len, ASN1String obj) throws IOException;
    public void       decodeStructure(InputStream in, int len,
                                      ASN1Structure struct) throws IOException;
}
