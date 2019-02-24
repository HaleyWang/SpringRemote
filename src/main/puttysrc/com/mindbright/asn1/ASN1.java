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

public final class ASN1 {

    public final static int MASK_CLASS        = 0xc0;
    public final static int MASK_NUMBER       = 0x1f;

    public final static int CLASS_UNIVERSAL   = 0x00;
    public final static int CLASS_APPLICATION = 0x40;
    public final static int CLASS_CONTEXT     = 0x80;
    public final static int CLASS_PRIVATE     = 0xc0;

    public final static int TYPE_PRIMITIVE    = 0x00;
    public final static int TYPE_CONSTRUCTED  = 0x20;

    // Class UNIVERSAL tag numbers
    //
    public final static int TAG_ANY             = 0;
    public final static int TAG_BOOLEAN         = 1;
    public final static int TAG_INTEGER         = 2;
    public final static int TAG_BITSTRING       = 3;
    public final static int TAG_OCTETSTRING     = 4;
    public final static int TAG_NULL            = 5;
    public final static int TAG_OID             = 6;
    public final static int TAG_ODESC           = 7;
    public final static int TAG_INSTANCEOF      = 8;
    public final static int TAG_REAL            = 9;
    public final static int TAG_ENUMERATED      = 10;
    public final static int TAG_EMBEDDED        = 11;
    public final static int TAG_UTF8STRING      = 12;
    // 13-15 reserved
    public final static int TAG_SEQUENCE        = 16;
    public final static int TAG_SET             = 17;
    // 18-22 Character string types
    public final static int TAG_NUMERICSTRING   = 18;
    public final static int TAG_PRINTABLESTRING = 19;
    public final static int TAG_T61STRING       = 20;
    public final static int TAG_TELETEXSTRING   = 20;
    public final static int TAG_VIDEOTEXSTRING  = 21;
    public final static int TAG_IA5STRING       = 22;
    // 23-24 Time types
    public final static int TAG_UTCTIME         = 23;
    public final static int TAG_GENERALIZEDTIME = 24;
    // 25-30 More character string types
    public final static int TAG_GRAPHICSTRING   = 25;
    public final static int TAG_VISIBLESTRING   = 26;
    public final static int TAG_GENERALSTRING   = 27;
    public final static int TAG_UNIVERSALSTRING = 28;
    public final static int TAG_BMPSTRING       = 30;

    // >31 reserved

}
