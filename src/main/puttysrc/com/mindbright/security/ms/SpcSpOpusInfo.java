/******************************************************************************
 *
 * Copyright (c) 2009-2011 Cryptzone Group AB. All Rights Reserved.
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

package com.mindbright.security.ms;

import com.mindbright.asn1.ASN1Explicit;
import com.mindbright.asn1.ASN1Sequence;

public final class SpcSpOpusInfo extends ASN1Sequence
{
    public SpcString progName;
    public SpcLink moreInfo;
    public SpcLink publisherInfo;

    public SpcSpOpusInfo() {
        super();
        progName = new SpcString();
        moreInfo = new SpcLink();
        publisherInfo = new SpcLink();
        addOptional(new ASN1Explicit(0, progName));
        addOptional(new ASN1Explicit(1, moreInfo));
        addOptional(new ASN1Explicit(2, publisherInfo));
    }
}
