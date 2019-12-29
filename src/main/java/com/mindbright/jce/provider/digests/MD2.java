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

package com.mindbright.jce.provider.digests;

import java.security.DigestException;
import java.security.MessageDigestSpi;

public final class MD2 extends MessageDigestSpi implements Cloneable {

    public static final int DIGEST_LENGTH = 16;

    private byte[] checksum;
    private byte[] state;
    private byte[] buffer;
    private int    rest;

    final static byte[] PI_SUBST = {
        41, 46, 67, (byte)201, (byte)162, (byte)216, 124, 1, 61, 54, 84,
        (byte)161, (byte)236, (byte)240, 6, 19, 98, (byte)167, 5, (byte)243,
        (byte)192, (byte)199, 115, (byte)140, (byte)152, (byte)147, 43,
        (byte)217, (byte)188, 76, (byte)130, (byte)202, 30, (byte)155, 87,
        60, (byte)253, (byte)212, (byte)224, 22, 103, 66, 111, 24,
        (byte)138, 23, (byte)229, 18, (byte)190, 78, (byte)196, (byte)214,
        (byte)218, (byte)158, (byte)222, 73, (byte)160, (byte)251, (byte)245,
        (byte)142, (byte)187, 47, (byte)238, 122, (byte)169, 104, 121,
        (byte)145, 21, (byte)178, 7, 63, (byte)148, (byte)194, 16, (byte)137,
        11, 34, 95, 33, (byte)128, 127, 93, (byte)154, 90, (byte)144, 50, 39,
        53, 62, (byte)204, (byte)231, (byte)191, (byte)247, (byte)151, 3,
        (byte)255, 25, 48, (byte)179, 72, (byte)165, (byte)181, (byte)209,
        (byte)215, 94, (byte)146, 42, (byte)172, 86, (byte)170, (byte)198, 79,
        (byte)184, 56, (byte)210, (byte)150, (byte)164, 125, (byte)182, 118,
        (byte)252, 107, (byte)226, (byte)156, 116, 4, (byte)241, 69, (byte)157,
        112, 89, 100, 113, (byte)135, 32, (byte)134, 91, (byte)207, 101,
        (byte)230, 45, (byte)168, 2, 27, 96, 37, (byte)173, (byte)174,
        (byte)176, (byte)185, (byte)246, 28, 70, 97, 105, 52, 64, 126, 15,
        85, 71, (byte)163, 35, (byte)221, 81, (byte)175, 58, (byte)195, 92,
        (byte)249, (byte)206, (byte)186, (byte)197, (byte)234, 38, 44, 83, 13,
        110, (byte)133, 40, (byte)132, 9, (byte)211, (byte)223, (byte)205,
        (byte)244, 65, (byte)129, 77, 82, 106, (byte)220, 55, (byte)200, 108,
        (byte)193, (byte)171, (byte)250, 36, (byte)225, 123, 8, 12, (byte)189,
        (byte)177, 74, 120, (byte)136, (byte)149, (byte)139, (byte)227, 99,
        (byte)232, 109, (byte)233, (byte)203, (byte)213, (byte)254, 59, 0, 29,
        57, (byte)242, (byte)239, (byte)183, 14, 102, 88, (byte)208, (byte)228,
        (byte)166, 119, 114, (byte)248, (byte)235, 117, 75, 10, 49, 68, 80,
        (byte)180, (byte)143, (byte)237, 31, 26, (byte)219, (byte)153,
        (byte)141, 51, (byte)159, 17, (byte)131, 20
    };
    
    final static byte[][] padding = {
        { 0 }, // dummy
        { 1 },
        { 2, 2 },
        { 3, 3, 3 },
        { 4, 4, 4, 4 },
        { 5, 5, 5, 5, 5 },
        { 6, 6, 6, 6, 6, 6 },
        { 7, 7, 7, 7, 7, 7, 7 },
        { 8, 8, 8, 8, 8, 8, 8, 8 },
        { 9, 9, 9, 9, 9, 9, 9, 9, 9 },
        { 10, 10, 10, 10, 10, 10, 10, 10, 10, 10 },
        { 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11 },
        { 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12 },
        { 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13 },
        { 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14 },
        { 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15 },
        { 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16 },
    };
    
    private void transform(byte data[], int offset) {
        int i, j, t;
        byte[] x = new byte[48];

        for(i = 0; i < DIGEST_LENGTH; i++) {
            byte s = state[i];
            byte d = data[offset + i];
            x[i]      = s;
            x[16 + i] = d;
            x[32 + i] = (byte)(s ^ d);
        }

        t = 0;
        for(i = 0; i < 18; i++) {
            for(j = 0; j < 48; j++) {
                x[j] ^= PI_SUBST[t];
                t = x[j] & 0xff;
            }
            t = (t + i) & 0xff;
        }

        t = checksum[15] & 0xff;
        for(i = 0; i < DIGEST_LENGTH; i++) {
            state[i]     = x[i];
            checksum[i] ^= PI_SUBST[(data[offset + i] & 0xff) ^ t];
            t            = checksum[i] & 0xff;
        }
    }

    public MD2() {
        super();
        checksum = new byte[DIGEST_LENGTH];
        state    = new byte[DIGEST_LENGTH];
        buffer   = new byte[DIGEST_LENGTH];
        engineReset();
    }

    private MD2(MD2 c) {
        this();
        System.arraycopy(c.checksum, 0, checksum, 0, DIGEST_LENGTH);
        System.arraycopy(c.state, 0, state, 0, DIGEST_LENGTH);
        System.arraycopy(c.buffer, 0, buffer, 0, DIGEST_LENGTH);
        rest = c.rest;
    }

    public Object clone() {
        return new MD2(this);
    }

    protected void engineReset() {
        for(int i = 0; i < DIGEST_LENGTH; i++) {
            checksum[i] = (byte)0;
            state[i]    = (byte)0;
            buffer[i]   = (byte)0;
        }
        rest = 0;
    }

    protected void engineUpdate(byte input) {
        engineUpdate(new byte[] { input }, 0, 1);
    }

    protected void engineUpdate(byte[] data, int offset, int length) {
        int left = DIGEST_LENGTH - rest;

        if(rest > 0 && length >= left) {
            System.arraycopy(data, offset, buffer, rest, left);
            transform(buffer, 0);
            offset += left;
            length -= left;
            rest   =  0;
        }

        while(length > 15) {
            transform(data, offset);
            offset += DIGEST_LENGTH;
            length -= DIGEST_LENGTH;
        }

        if(length > 0) {
            System.arraycopy(data, offset, buffer, rest, length);
            rest += length;
        }
    }

    protected byte[] engineDigest() {
        byte[] buf = new byte[DIGEST_LENGTH];
        try {
            engineDigest(buf, 0, buf.length);
        } catch(DigestException e) {
            /* Can't happen... */
        }
        return buf;
    }

    protected int engineDigest(byte[] dest, int off, int len)
    throws DigestException {
        int padlen = DIGEST_LENGTH - rest;

        if(len < DIGEST_LENGTH) {
            throw new DigestException("MD2, output buffer too short");
        }

        engineUpdate(padding[padlen], 0, padlen);
        engineUpdate(checksum, 0, DIGEST_LENGTH);

        System.arraycopy(state, 0, dest, off, DIGEST_LENGTH);

        engineReset();

        return DIGEST_LENGTH;
    }

    protected int engineGetDigestLength() {
        return DIGEST_LENGTH;
    }

}
