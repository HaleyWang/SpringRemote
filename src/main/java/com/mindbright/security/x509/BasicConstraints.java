/******************************************************************************
 *
 * Copyright (c) 2005-2011 Cryptzone Group AB. All Rights Reserved.
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

package com.mindbright.security.x509;

import com.mindbright.asn1.ASN1Boolean;
import com.mindbright.asn1.ASN1Integer;
import com.mindbright.asn1.ASN1Sequence;

/**
 * Represents the basic constraints extension of a certificate.
 *
 * <pre>
 * BasicConstraintsSyntax ::= SEQUENCE {
 *      cA	BOOLEAN DEFAULT FALSE,
 *      pathLenConstraint INTEGER (0..MAX) OPTIONAL
 * }
 * </pre>
 */

public class BasicConstraints extends ASN1Sequence {

    public ASN1Boolean ca;
    public ASN1Integer pathlenconstraint;

    public BasicConstraints() {
        ca = new ASN1Boolean();
        pathlenconstraint = new ASN1Integer();
        addOptional(ca, false);
        addOptional(pathlenconstraint, -1);
    }

    public String toString() {
        String len = null;
        try {
            if (pathlenconstraint.getValue().intValue() >= 0)
                len = pathlenconstraint.getValue().toString();
        } catch (Throwable t) {}

        return "basicConstraints: ca=" + ca.getValue() +
               ((len == null)?"": (", pathlenconstraint=" + len));
    }
}
