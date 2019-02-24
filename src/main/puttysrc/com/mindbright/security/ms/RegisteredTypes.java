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
 * Cryptzone Group AB, Drakegatan 7, SE-41250 Goteborg, 41121 Goteborg, SWEDEN
 *
 *****************************************************************************/

package com.mindbright.security.ms;

import com.mindbright.asn1.ASN1OIDRegistry;

public class RegisteredTypes extends ASN1OIDRegistry {
	private static final long serialVersionUID = 1L;

    public RegisteredTypes() {
        put("1.3.6.1.4.1.311.10.1", 
            "com.mindbright.security.ms.CtlInfo");
        put("1.3.6.1.4.1.311.12.2.1",
            "com.mindbright.security.ms.CatNameValue");
        put("1.3.6.1.4.1.311.12.2.2",
            "com.mindbright.security.ms.CatMemberInfo");
        put("1.3.6.1.4.1.311.2.1.4",
            "com.mindbright.security.ms.SpcIndirectDataContent");
        put("1.3.6.1.4.1.311.2.1.11",
            "com.mindbright.security.ms.SpcStatementType");
        put("1.3.6.1.4.1.311.2.1.12",
            "com.mindbright.security.ms.SpcSpOpusInfo");
        put("1.3.6.1.4.1.311.2.1.15",
            "com.mindbright.security.ms.SpcPeImageData");
        put("1.3.6.1.4.1.311.2.1.25",
            "com.mindbright.security.ms.SpcString");
    }
}
