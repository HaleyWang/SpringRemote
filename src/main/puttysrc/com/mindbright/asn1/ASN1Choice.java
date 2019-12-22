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

import java.io.IOException;

import java.util.Hashtable;

public class ASN1Choice extends ASN1DynamicType {

    protected Hashtable<Integer,Object> members;

    public ASN1Choice() {
        super();
        members = new Hashtable<Integer, Object>();
    }

    public final void setMember(int id, Class<?> ofType) {
        members.put(Integer.valueOf(id), ofType);
    }

    public final void setMember(int id, ASN1Object value) {
        members.put(Integer.valueOf(id), value);
    }

    public final void setMember(ASN1Object value) {
        members.put(Integer.valueOf(value.getTag()), value);
    }

    public final Object getMember(int tag) {
        Object o = members.get(memberMapping(tag));
        if (o == null)
            o = members.get(memberMapping(tag & ~ASN1.MASK_CLASS));
        return o;
    }

    protected ASN1Object bindType(int tag) throws IOException {
        Object o = getMember(tag);        
        if(o == null) {
            throw new IOException("Invalid member of " + getType() + " (" +
                                  tag + ")");
        }
        ASN1Object value = null;
        if(o instanceof Class<?>) {
            try {
                Class<?> c = (Class<?>)o;
                value = (ASN1Object)c.newInstance();
            } catch (Exception e) {
                throw new IOException("Error decoding " + getType() + ": " +
                                      e.getMessage());
            }
        } else if(o instanceof ASN1Object) {
            value = (ASN1Object)o;
        } else {
            throw new IOException("Error decoding " + getType() +
                                  ", invalid member: " + o);
        }

        return value;
    }

    protected Object memberMapping(int tag) {
        return Integer.valueOf(tag);
    }

}

