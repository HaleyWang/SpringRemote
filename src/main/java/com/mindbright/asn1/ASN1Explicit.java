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

public final class ASN1Explicit extends ASN1Structure {

    private ASN1Object underlying;

    public ASN1Explicit(int tag, ASN1Object underlying) {
        this(tag, ASN1.CLASS_CONTEXT, underlying);
    }

    public ASN1Explicit(int tag, int cl, ASN1Object underlying) {
        super(tag | cl);
        this.underlying = underlying;
        addComponent(underlying);
    }

    public void setValue() {
        underlying.setValue();
    }

    public boolean isSet() {
        return underlying.isSet();
    }

    public boolean equals(Object obj) {
        return underlying.equals(obj);
    }

    public int hashCode() {
        return underlying.hashCode();
    }

}
