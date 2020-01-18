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

/*
 * Author's comment: The contents of this file is heavily based upon...
 *
 * Rijndael --pronounced Reindaal-- is a variable block-size (128-, 192- and
 * 256-bit), variable key-size (128-, 192- and 256-bit) symmetric cipher.
 *
 * Rijndael was written by Vincent Rijmen and Joan Daemen.
 *
 */
package com.mindbright.jce.provider.ciphers;

import java.security.InvalidKeyException;

public final class Rijndael extends BlockCipher {

    private int[][] Ke; // encryption round keys
    private int[][] Kd; // decryption round keys

    private int ROUNDS;

    public Rijndael() {}

    public int getBlockSize() {
        return BLOCK_SIZE;
    }

    /**
     * Expand a user-supplied key material into a session key.
     *
     * @param key  The 128/192/256-bit user-key to use.
     * @exception  InvalidKeyException If the key is invalid.
     */
    public synchronized void initializeKey(byte[] key)
    throws InvalidKeyException {
        if (key == null)
            throw new InvalidKeyException("Empty key");
        if (!(key.length == 16 || key.length == 24 || key.length == 32))
            throw new InvalidKeyException("Incorrect key length");
        ROUNDS = getRounds(key.length, BLOCK_SIZE);
        Ke = new int[ROUNDS + 1][BC]; // encryption round keys
        Kd = new int[ROUNDS + 1][BC]; // decryption round keys
        int ROUND_KEY_COUNT = (ROUNDS + 1) * BC;
        int KC = key.length / 4;
        int[] tk = new int[KC];
        int i, j;

        // copy user material bytes into temporary ints
        for (i = 0, j = 0; i < KC; )
            tk[i++] = (key[j++] & 0xFF) << 24 |
                      (key[j++] & 0xFF) << 16 |
                      (key[j++] & 0xFF) <<  8 |
                      (key[j++] & 0xFF);
        // copy values into round key arrays
        int t = 0;
        for (j = 0; (j < KC) && (t < ROUND_KEY_COUNT); j++, t++) {
            Ke[t / BC][t % BC] = tk[j];
            Kd[ROUNDS - (t / BC)][t % BC] = tk[j];
        }
        int tt, rconpointer = 0;
        while (t < ROUND_KEY_COUNT) {
            // extrapolate using phi (the round key evolution function)
            tt = tk[KC - 1];
            tk[0] ^= (S[(tt >>> 16) & 0xFF] & 0xFF) << 24 ^
                     (S[(tt >>>  8) & 0xFF] & 0xFF) << 16 ^
                     (S[ tt         & 0xFF] & 0xFF) <<  8 ^
                     (S[(tt >>> 24) & 0xFF] & 0xFF)       ^
                     (rcon[rconpointer++]   & 0xFF) << 24;
            if (KC != 8)
                for (i = 1, j = 0; i < KC; )
                    tk[i++] ^= tk[j++];
            else {
                for (i = 1, j = 0; i < KC / 2; )
                    tk[i++] ^= tk[j++];
                tt = tk[KC / 2 - 1];
                tk[KC / 2] ^= (S[ tt         & 0xFF] & 0xFF)       ^
                              (S[(tt >>>  8) & 0xFF] & 0xFF) <<  8 ^
                              (S[(tt >>> 16) & 0xFF] & 0xFF) << 16 ^
                              (S[(tt >>> 24) & 0xFF] & 0xFF) << 24;
                for (j = KC / 2, i = j + 1; i < KC; )
                    tk[i++] ^= tk[j++];
            }
            // copy values into round key arrays
            for (j = 0; (j < KC) && (t < ROUND_KEY_COUNT); j++, t++) {
                Ke[t / BC][t % BC] = tk[j];
                Kd[ROUNDS - (t / BC)][t % BC] = tk[j];
            }
        }
        for (int r = 1; r < ROUNDS; r++)    // inverse MixColumn where needed
            for (j = 0; j < BC; j++) {
                tt = Kd[r][j];
                Kd[r][j] = U1[(tt >>> 24) & 0xFF] ^
                           U2[(tt >>> 16) & 0xFF] ^
                           U3[(tt >>>  8) & 0xFF] ^
                           U4[ tt         & 0xFF];
            }
    }

    /**
     * Convenience method to encrypt exactly one block of plaintext, assuming
     * Rijndael's default block size (128-bit).
     *
     * @param  in         The plaintext.
     * @param  inOffset   Index of in from which to start considering data.
     * @param  out        The ciphertext.
     * @param  outOffset  Index in out where output of ciphertext should start.
     */
    public void blockEncrypt(byte[] in, int inOffset, byte[] out, int outOffset) {
        int[] Ker  = Ke[0];

        // plaintext to ints + key
        int t0 = ((in[inOffset++] & 0xFF) << 24 |
                  (in[inOffset++] & 0xFF) << 16 |
                  (in[inOffset++] & 0xFF) <<  8 |
                  (in[inOffset++] & 0xFF)        ) ^ Ker[0];
        int t1 = ((in[inOffset++] & 0xFF) << 24 |
                  (in[inOffset++] & 0xFF) << 16 |
                  (in[inOffset++] & 0xFF) <<  8 |
                  (in[inOffset++] & 0xFF)        ) ^ Ker[1];
        int t2 = ((in[inOffset++] & 0xFF) << 24 |
                  (in[inOffset++] & 0xFF) << 16 |
                  (in[inOffset++] & 0xFF) <<  8 |
                  (in[inOffset++] & 0xFF)        ) ^ Ker[2];
        int t3 = ((in[inOffset++] & 0xFF) << 24 |
                  (in[inOffset++] & 0xFF) << 16 |
                  (in[inOffset++] & 0xFF) <<  8 |
                  (in[inOffset++] & 0xFF)        ) ^ Ker[3];

        int a0, a1, a2, a3;
        for (int r = 1; r < ROUNDS; r++) {          // apply round transforms
            Ker = Ke[r];
            a0 = (T1[(t0 >>> 24) & 0xFF] ^
                  T2[(t1 >>> 16) & 0xFF] ^
                  T3[(t2 >>>  8) & 0xFF] ^
                  T4[ t3         & 0xFF]  ) ^ Ker[0];
            a1 = (T1[(t1 >>> 24) & 0xFF] ^
                  T2[(t2 >>> 16) & 0xFF] ^
                  T3[(t3 >>>  8) & 0xFF] ^
                  T4[ t0         & 0xFF]  ) ^ Ker[1];
            a2 = (T1[(t2 >>> 24) & 0xFF] ^
                  T2[(t3 >>> 16) & 0xFF] ^
                  T3[(t0 >>>  8) & 0xFF] ^
                  T4[ t1         & 0xFF]  ) ^ Ker[2];
            a3 = (T1[(t3 >>> 24) & 0xFF] ^
                  T2[(t0 >>> 16) & 0xFF] ^
                  T3[(t1 >>>  8) & 0xFF] ^
                  T4[ t2         & 0xFF]  ) ^ Ker[3];
            t0 = a0;
            t1 = a1;
            t2 = a2;
            t3 = a3;
        }

        // last round is special
        Ker = Ke[ROUNDS];
        int tt = Ker[0];
        out[outOffset + 0] = (byte)(S[(t0 >>> 24) & 0xFF] ^ (tt >>> 24));
        out[outOffset + 1] = (byte)(S[(t1 >>> 16) & 0xFF] ^ (tt >>> 16));
        out[outOffset + 2] = (byte)(S[(t2 >>>  8) & 0xFF] ^ (tt >>>  8));
        out[outOffset + 3] = (byte)(S[ t3         & 0xFF] ^  tt        );
        tt = Ker[1];
        out[outOffset + 4] = (byte)(S[(t1 >>> 24) & 0xFF] ^ (tt >>> 24));
        out[outOffset + 5] = (byte)(S[(t2 >>> 16) & 0xFF] ^ (tt >>> 16));
        out[outOffset + 6] = (byte)(S[(t3 >>>  8) & 0xFF] ^ (tt >>>  8));
        out[outOffset + 7] = (byte)(S[ t0         & 0xFF] ^  tt        );
        tt = Ker[2];
        out[outOffset + 8] = (byte)(S[(t2 >>> 24) & 0xFF] ^ (tt >>> 24));
        out[outOffset + 9] = (byte)(S[(t3 >>> 16) & 0xFF] ^ (tt >>> 16));
        out[outOffset +10] = (byte)(S[(t0 >>>  8) & 0xFF] ^ (tt >>>  8));
        out[outOffset +11] = (byte)(S[ t1         & 0xFF] ^  tt        );
        tt = Ker[3];
        out[outOffset +12] = (byte)(S[(t3 >>> 24) & 0xFF] ^ (tt >>> 24));
        out[outOffset +13] = (byte)(S[(t0 >>> 16) & 0xFF] ^ (tt >>> 16));
        out[outOffset +14] = (byte)(S[(t1 >>>  8) & 0xFF] ^ (tt >>>  8));
        out[outOffset +15] = (byte)(S[ t2         & 0xFF] ^  tt        );
    }

    /**
     * Convenience method to decrypt exactly one block of plaintext, assuming
     * Rijndael's default block size (128-bit).
     *
     * @param  in         The ciphertext.
     * @param  inOffset   Index of in from which to start considering data.
     * @param  out        The plaintext.
     * @param  outOffset  Index in out where output of plaintext should start. 
     */
    public void blockDecrypt(byte[] in, int inOffset, byte[] out, int outOffset) {
        int[] Kdr = Kd[0];

        // ciphertext to ints + key
        int t0 = ((in[inOffset++] & 0xFF) << 24 |
                  (in[inOffset++] & 0xFF) << 16 |
                  (in[inOffset++] & 0xFF) <<  8 |
                  (in[inOffset++] & 0xFF)        ) ^ Kdr[0];
        int t1 = ((in[inOffset++] & 0xFF) << 24 |
                  (in[inOffset++] & 0xFF) << 16 |
                  (in[inOffset++] & 0xFF) <<  8 |
                  (in[inOffset++] & 0xFF)        ) ^ Kdr[1];
        int t2 = ((in[inOffset++] & 0xFF) << 24 |
                  (in[inOffset++] & 0xFF) << 16 |
                  (in[inOffset++] & 0xFF) <<  8 |
                  (in[inOffset++] & 0xFF)        ) ^ Kdr[2];
        int t3 = ((in[inOffset++] & 0xFF) << 24 |
                  (in[inOffset++] & 0xFF) << 16 |
                  (in[inOffset++] & 0xFF) <<  8 |
                  (in[inOffset++] & 0xFF)        ) ^ Kdr[3];

        int a0, a1, a2, a3;
        for (int r = 1; r < ROUNDS; r++) {          // apply round transforms
            Kdr = Kd[r];
            a0 = (T5[(t0 >>> 24) & 0xFF] ^
                  T6[(t3 >>> 16) & 0xFF] ^
                  T7[(t2 >>>  8) & 0xFF] ^
                  T8[ t1         & 0xFF]  ) ^ Kdr[0];
            a1 = (T5[(t1 >>> 24) & 0xFF] ^
                  T6[(t0 >>> 16) & 0xFF] ^
                  T7[(t3 >>>  8) & 0xFF] ^
                  T8[ t2         & 0xFF]  ) ^ Kdr[1];
            a2 = (T5[(t2 >>> 24) & 0xFF] ^
                  T6[(t1 >>> 16) & 0xFF] ^
                  T7[(t0 >>>  8) & 0xFF] ^
                  T8[ t3         & 0xFF]  ) ^ Kdr[2];
            a3 = (T5[(t3 >>> 24) & 0xFF] ^
                  T6[(t2 >>> 16) & 0xFF] ^
                  T7[(t1 >>>  8) & 0xFF] ^
                  T8[ t0         & 0xFF]  ) ^ Kdr[3];
            t0 = a0;
            t1 = a1;
            t2 = a2;
            t3 = a3;
        }

        // last round is special
        Kdr = Kd[ROUNDS];
        int tt = Kdr[0];
        out[outOffset + 0] = (byte)(Si[(t0 >>> 24) & 0xFF] ^ (tt >>> 24));
        out[outOffset + 1] = (byte)(Si[(t3 >>> 16) & 0xFF] ^ (tt >>> 16));
        out[outOffset + 2] = (byte)(Si[(t2 >>>  8) & 0xFF] ^ (tt >>>  8));
        out[outOffset + 3] = (byte)(Si[ t1         & 0xFF] ^  tt        );
        tt = Kdr[1];
        out[outOffset + 4] = (byte)(Si[(t1 >>> 24) & 0xFF] ^ (tt >>> 24));
        out[outOffset + 5] = (byte)(Si[(t0 >>> 16) & 0xFF] ^ (tt >>> 16));
        out[outOffset + 6] = (byte)(Si[(t3 >>>  8) & 0xFF] ^ (tt >>>  8));
        out[outOffset + 7] = (byte)(Si[ t2         & 0xFF] ^  tt        );
        tt = Kdr[2];
        out[outOffset + 8] = (byte)(Si[(t2 >>> 24) & 0xFF] ^ (tt >>> 24));
        out[outOffset + 9] = (byte)(Si[(t1 >>> 16) & 0xFF] ^ (tt >>> 16));
        out[outOffset +10] = (byte)(Si[(t0 >>>  8) & 0xFF] ^ (tt >>>  8));
        out[outOffset +11] = (byte)(Si[ t3         & 0xFF] ^  tt        );
        tt = Kdr[3];
        out[outOffset +12] = (byte)(Si[(t3 >>> 24) & 0xFF] ^ (tt >>> 24));
        out[outOffset +13] = (byte)(Si[(t2 >>> 16) & 0xFF] ^ (tt >>> 16));
        out[outOffset +14] = (byte)(Si[(t1 >>>  8) & 0xFF] ^ (tt >>>  8));
        out[outOffset +15] = (byte)(Si[ t0         & 0xFF] ^  tt        );
    }

    /**
     * Return The number of rounds for a given Rijndael's key and block sizes.
     *
     * @param keySize    The size of the user key material in bytes.
     * @param blockSize  The desired block size in bytes.
     * @return The number of rounds for a given Rijndael's key and
     *      block sizes.
     */
    public static int getRounds(int keySize, int blockSize) {
        switch (keySize) {
        case 16:
            return blockSize == 16 ? 10 : (blockSize == 24 ? 12 : 14);
        case 24:
            return blockSize != 32 ? 12 : 14;
        default: // 32 bytes = 256 bits
            return 14;
        }
    }

    //
    //
    //

    private static final int BLOCK_SIZE = 16; // default block size in bytes
    private static final int BC         = 4;  // block size in 4 byte words

    private static final int[] alog  = new int[256];
    private static final int[] log   = new int[256];

    private static final byte[] S    = new byte[256];
    private static final byte[] Si   = new byte[256];
    private static final int[]  T1   = new int[256];
    private static final int[]  T2   = new int[256];
    private static final int[]  T3   = new int[256];
    private static final int[]  T4   = new int[256];
    private static final int[]  T5   = new int[256];
    private static final int[]  T6   = new int[256];
    private static final int[]  T7   = new int[256];
    private static final int[]  T8   = new int[256];
    private static final int[]  U1   = new int[256];
    private static final int[]  U2   = new int[256];
    private static final int[]  U3   = new int[256];
    private static final int[]  U4   = new int[256];
    private static final byte[] rcon = new byte[30];

    /** Static code - to intialise S-boxes and T-boxes */
    static {
        int ROOT = 0x11B;
        int i, j = 0;

        //
        // produce log and alog tables, needed for multiplying in the
        // field GF(2^m) (generator = 3)
        //
        alog[0] = 1;
        for (i = 1; i < 256; i++) {
            j = (alog[i-1] << 1) ^ alog[i-1];
            if ((j & 0x100) != 0)
                j ^= ROOT;
            alog[i] = j;
        }
        for (i = 1; i < 255; i++)
            log[alog[i]] = i;
        byte[][] A = new byte[][] {
                         {1, 1, 1, 1, 1, 0, 0, 0},
                         {0, 1, 1, 1, 1, 1, 0, 0},
                         {0, 0, 1, 1, 1, 1, 1, 0},
                         {0, 0, 0, 1, 1, 1, 1, 1},
                         {1, 0, 0, 0, 1, 1, 1, 1},
                         {1, 1, 0, 0, 0, 1, 1, 1},
                         {1, 1, 1, 0, 0, 0, 1, 1},
                         {1, 1, 1, 1, 0, 0, 0, 1}
                     };
        byte[] B = new byte[] { 0, 1, 1, 0, 0, 0, 1, 1};

        //
        // substitution box based on F^{-1}(x)
        //
        int t;
        byte[][] box = new byte[256][8];
        box[1][7] = 1;
        for (i = 2; i < 256; i++) {
            j = alog[255 - log[i]];
            for (t = 0; t < 8; t++)
                box[i][t] = (byte)((j >>> (7 - t)) & 0x01);
        }
        //
        // affine transform:  box[i] <- B + A*box[i]
        //
        byte[][] cox = new byte[256][8];
        for (i = 0; i < 256; i++)
            for (t = 0; t < 8; t++) {
                cox[i][t] = B[t];
                for (j = 0; j < 8; j++)
                    cox[i][t] ^= A[t][j] * box[i][j];
            }
        //
        // S-boxes and inverse S-boxes
        //
        for (i = 0; i < 256; i++) {
            S[i] = (byte)(cox[i][0] << 7);
            for (t = 1; t < 8; t++)
                S[i] ^= cox[i][t] << (7-t);
            Si[S[i] & 0xFF] = (byte) i;
        }
        //
        // T-boxes
        //
        byte[][] G = new byte[][] {
                         {2, 1, 1, 3},
                         {3, 2, 1, 1},
                         {1, 3, 2, 1},
                         {1, 1, 3, 2}
                     };
        byte[][] AA = new byte[4][8];
        for (i = 0; i < 4; i++) {
            for (j = 0; j < 4; j++)
                AA[i][j] = G[i][j];
            AA[i][i+4] = 1;
        }
        byte pivot, tmp;
        byte[][] iG = new byte[4][4];
        for (i = 0; i < 4; i++) {
            pivot = AA[i][i];
            if (pivot == 0) {
                t = i + 1;
                while ((AA[t][i] == 0) && (t < 4))
                    t++;
                if (t == 4)
                    throw new RuntimeException("G matrix is not invertible");
                
                for (j = 0; j < 8; j++) {
                	tmp = AA[i][j];
                	AA[i][j] = AA[t][j];
                	AA[t][j] = tmp;
                }
                pivot = AA[i][i];                
            }
            for (j = 0; j < 8; j++)
                if (AA[i][j] != 0)
                    AA[i][j] = (byte)
                               alog[(255 + log[AA[i][j] & 0xFF] - log[pivot & 0xFF]) % 255];
            for (t = 0; t < 4; t++)
                if (i != t) {
                    for (j = i+1; j < 8; j++)
                        AA[t][j] ^= mul(AA[i][j], AA[t][i]);
                    AA[t][i] = 0;
                }
        }
        for (i = 0; i < 4; i++)
            for (j = 0; j < 4; j++)
                iG[i][j] = AA[i][j + 4];

        int s;
        for (t = 0; t < 256; t++) {
            s = S[t];
            T1[t] = mul4(s, G[0]);
            T2[t] = mul4(s, G[1]);
            T3[t] = mul4(s, G[2]);
            T4[t] = mul4(s, G[3]);

            s = Si[t];
            T5[t] = mul4(s, iG[0]);
            T6[t] = mul4(s, iG[1]);
            T7[t] = mul4(s, iG[2]);
            T8[t] = mul4(s, iG[3]);

            U1[t] = mul4(t, iG[0]);
            U2[t] = mul4(t, iG[1]);
            U3[t] = mul4(t, iG[2]);
            U4[t] = mul4(t, iG[3]);
        }
        //
        // round constants
        //
        rcon[0] = 1;
        int r = 1;
        for (t = 1; t < 30; )
            rcon[t++] = (byte)(r = mul(2, r));
    }

    // multiply two elements of GF(2^m)
    private static final int mul(int a, int b) {
        return (a != 0 && b != 0) ?
               alog[(log[a & 0xFF] + log[b & 0xFF]) % 255] :
               0;
    }

    // convenience method used in generating Transposition boxes
    private static final int mul4(int a, byte[] b) {
        if (a == 0)
            return 0;
        a = log[a & 0xFF];
        int a0 = (b[0] != 0) ? alog[(a + log[b[0] & 0xFF]) % 255] & 0xFF : 0;
        int a1 = (b[1] != 0) ? alog[(a + log[b[1] & 0xFF]) % 255] & 0xFF : 0;
        int a2 = (b[2] != 0) ? alog[(a + log[b[2] & 0xFF]) % 255] & 0xFF : 0;
        int a3 = (b[3] != 0) ? alog[(a + log[b[3] & 0xFF]) % 255] & 0xFF : 0;
        return a0 << 24 | a1 << 16 | a2 << 8 | a3;
    }

}
