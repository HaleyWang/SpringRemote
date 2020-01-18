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

public class ASN1Any extends ASN1Choice {

    public static class ASN1SequenceOfAny extends ASN1SequenceOf {
        public ASN1SequenceOfAny() {
            super(ASN1Any.class);
        }
    }

    public static class ASN1SetOfAny extends ASN1SetOf {
        public ASN1SetOfAny() {
            super(ASN1Any.class);
        }
    }

    public static class ASN1AnyStructure extends ASN1Structure {
        public ASN1AnyStructure() {
            super(0);
            addComponent(new ASN1Any());
        }

        public void decodeValue(ASN1Decoder decoder, InputStream in,
                                int tag, int len)
        throws IOException {
            decodeValue(decoder, in, len);
        }
    }

    public ASN1Any() {
        super();
        setMember(ASN1.TAG_BOOLEAN,     ASN1Boolean.class);
        setMember(ASN1.TAG_INTEGER,     ASN1Integer.class);
        setMember(ASN1.TAG_BITSTRING,   ASN1BitString.class);
        setMember(ASN1.TAG_OCTETSTRING, ASN1OctetString.class);
        setMember(ASN1.TAG_NULL,        ASN1Null.class);
        setMember(ASN1.TAG_OID,         ASN1OID.class);
        setMember(ASN1.TAG_SEQUENCE | ASN1.TYPE_CONSTRUCTED,
                  ASN1SequenceOfAny.class);
        setMember(ASN1.TAG_SET | ASN1.TYPE_CONSTRUCTED,
                  ASN1SetOfAny.class);
        setMember(ASN1.TAG_IA5STRING,    ASN1IA5String.class);
        setMember(ASN1.TAG_BMPSTRING,    ASN1BMPString.class);
        setMember(ASN1.TAG_PRINTABLESTRING, ASN1PrintableString.class);
        setMember(ASN1.TYPE_CONSTRUCTED, ASN1AnyStructure.class);
    }

    protected Object memberMapping(int tag) {
        Object o = super.memberMapping(tag);
        if(o == null) {
            tag &= (ASN1.MASK_NUMBER | ASN1.TYPE_CONSTRUCTED);
            o = super.memberMapping(tag);
        }
        if(o == null) {
            tag &= ASN1.MASK_NUMBER;
            o = super.memberMapping(tag);
        }
        return o;
    }

}
