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

public class ASN1AnyDefinedBy extends ASN1DynamicType {

    private ASN1OID definedById;
    private boolean fallBackToAny = false;

    public ASN1AnyDefinedBy(ASN1OID definedById) {
        this(definedById, false);
    }

    public ASN1AnyDefinedBy(ASN1OID definedById, boolean fallBackToAny) {
        super();
        this.definedById = definedById;
        this.fallBackToAny = fallBackToAny;
    }

    @SuppressWarnings("unchecked")
	protected ASN1Object bindType(int tag) throws IOException {
        if(definedById.getValue() == null) {
            throw new IOException("Unresolved ANY DEFINED BY, OID not set");
        }
        Class<ASN1Any> type = (Class<ASN1Any>)ASN1OIDRegistry.lookupType(definedById);
        if(type == null) {
            if (fallBackToAny)
                type = ASN1Any.class;
            else
                throw new IOException("Unknown member of " + getType() + " (" +
                                      definedById.getString() + ")");
        }
        ASN1Object value = null;
        try {
            value = type.newInstance();
        } catch (Exception e) {
            throw new IOException("Error decoding " + getType() + ": " +
                                  e.getMessage());
        }

        return value;
    }

}
