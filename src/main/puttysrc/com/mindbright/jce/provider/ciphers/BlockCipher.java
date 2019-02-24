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

package com.mindbright.jce.provider.ciphers;

import java.security.AlgorithmParameters;
import java.security.SecureRandom;
import java.security.Key;
import java.security.InvalidKeyException;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.CipherSpi;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public abstract class BlockCipher extends CipherSpi {

    // Default is to ECB
    //
    final static int MODE_ECB = 0;
    final static int MODE_CBC = 1;
    final static int MODE_CFB = 2;
    final static int MODE_OFB = 3;
    final static int MODE_CTR = 4;

    byte[] iv;
    byte[] tmpBlk;
    byte[] ctr;
    int    opMode;
    int    feedbackMode;
    int    streamBits;
    int    blockSize;

    boolean pkcs5Padding;
    boolean ctrInit = false;

    protected byte[] engineDoFinal(byte[] input, int inputOffset, int inputLen) {
        byte[] output = new byte[engineGetOutputSize(inputLen)];
        engineDoFinal(input, inputOffset, inputLen, output, 0);
        return output;
    }

    protected byte[] engineUpdate(byte[] input, int inputOffset, int inputLen) {
        byte[] output = new byte[engineGetOutputSize(inputLen)];
        engineUpdate(input, inputOffset, inputLen, output, 0);
        return output;
    }

    protected AlgorithmParameters engineGetParameters() {
        return null;
    }    

    protected int engineDoFinal(byte[] input, int inputOffset, int inputLen,
                                byte[] output, int outputOffset) {
        int n = engineUpdate(input, inputOffset, inputLen, output, outputOffset);
        // XXX: reset engine
        return n;
    }
    
    protected int engineUpdate(byte[] input,
                               int inputOffset,
                               int inputLen,
                               byte[] output,
                               int outputOffset) {
    
        int n = 0;
        switch(feedbackMode) {
        case MODE_ECB:
            n = internalDoUpdateECB(input, inputOffset, inputLen,
                                   output, outputOffset);
            break;
        case MODE_CBC:
            n = internalDoUpdateCBC(input, inputOffset, inputLen,
                                   output, outputOffset);
            break;
        case MODE_CFB:
            n = internalDoUpdateCFB(input, inputOffset, inputLen,
                                   output, outputOffset);
            break;
        case MODE_OFB:
            n = internalDoUpdateOFB(input, inputOffset, inputLen,
                                   output, outputOffset);
            break;
        case MODE_CTR:
            n = internalDoUpdateCTR(input, inputOffset, inputLen,
                                   output, outputOffset);
            break;
        default:
            break;    
        }
        return n;
    }

    private final int internalDoUpdateECB(byte[] input, int inputOffset, int inputLen,
                                         byte[] output, int outputOffset) {
        int nBlocks = inputLen / blockSize;
        if(opMode == Cipher.ENCRYPT_MODE) {
            for(int bc = 0; bc < nBlocks; bc++) {
                blockEncrypt(input, inputOffset, output, outputOffset);
                inputOffset  += blockSize;
                outputOffset += blockSize;
            }
        } else {
            for(int bc = 0; bc < nBlocks; bc++) {
                blockDecrypt(input, inputOffset, output, outputOffset);
                inputOffset  += blockSize;
                outputOffset += blockSize;
            }
        }
        return inputLen;
    }

    private final int internalDoUpdateCBC(byte[] input, int inputOffset,
                                         int inputLen,
                                         byte[] output, int outputOffset) {
        int nBlocks = inputLen / blockSize;
        if(opMode == Cipher.ENCRYPT_MODE) {
            for(int bc = 0; bc < nBlocks; bc++) {
                blockXor(input, inputOffset, blockSize, iv, 0);
                blockEncrypt(iv, 0, output, outputOffset);
                System.arraycopy(output, outputOffset, iv, 0, blockSize);
                inputOffset  += blockSize;
                outputOffset += blockSize;
            }
            // !!! TODO Move out to engineDoFinal
            if(pkcs5Padding) {
                int rest = inputLen % blockSize;
                int pad  = blockSize - rest;
                for(int i = 0; i < blockSize; i++) {
                    if(i < rest) {
                        iv[i] ^= input[inputOffset++];
                    } else {
                        iv[i] ^= (byte)pad;
                    }
                }
                blockEncrypt(iv, 0, output, outputOffset);
                System.arraycopy(output, outputOffset, iv, 0, blockSize);
                inputLen += (blockSize - rest);
            }
        } else {
            for(int bc = 0; bc < nBlocks; bc++) {
                blockDecrypt(input, inputOffset, tmpBlk, 0);
                for(int i = 0; i < blockSize; i++) {
                    tmpBlk[i] ^= iv[i];
                    iv[i] = input[inputOffset + i];
                    output[outputOffset + i] = tmpBlk[i];
                }
                inputOffset  += blockSize;
                outputOffset += blockSize;
            }
        }
        return inputLen;
    }

    private final int internalDoUpdateCFB(byte[] input, int inputOffset, int inputLen,
                                          byte[] output, int outputOffset) {
        int endOffset = inputOffset + inputLen;
        int bs        = blockSize;
        if(opMode == Cipher.ENCRYPT_MODE) {
            for(; inputOffset < endOffset;) {
                blockEncrypt(iv, 0, iv, 0);
                if(inputOffset + bs > endOffset) {
                    bs = endOffset - inputOffset;
                }
                blockXor(input, inputOffset, bs, iv, 0);
                System.arraycopy(iv, 0, output, outputOffset, bs);
                inputOffset  += bs;
                outputOffset += bs;
            }
        } else {
            for(; inputOffset < endOffset;) {
                blockEncrypt(iv, 0, tmpBlk, 0);
                if(inputOffset + bs > endOffset) {
                    bs = endOffset - inputOffset;
                }
                System.arraycopy(input, inputOffset, iv, 0, bs);
                blockXor(iv, 0, bs, tmpBlk, 0);
                System.arraycopy(tmpBlk, 0, output, outputOffset, bs);
                inputOffset  += bs;
                outputOffset += bs;
            }
        }
        return inputLen;
    }

    private final int internalDoUpdateOFB(byte[] input, int inputOffset, int inputLen,
                                          byte[] output, int outputOffset) {
        int endOffset = inputOffset + inputLen;
        for(; inputOffset < endOffset;) {
            blockEncrypt(iv, 0, iv, 0);
            int srcLen = ((inputOffset + blockSize <= endOffset) ?
                          blockSize : endOffset - inputOffset);
            for(int i = 0; i < srcLen; i++) {
                output[outputOffset + i] = input[inputOffset + i];
                output[outputOffset + i] ^= iv[i];
            }
            inputOffset  += blockSize;
            outputOffset += blockSize;
        }
        return inputLen;
    }

    private final int internalDoUpdateCTR(byte[] input, int inputOffset,
                                          int inputLen,
                                          byte[] output, int outputOffset) {
        if (!ctrInit) {
            ctr = new byte[blockSize]; // 'X' in draft
            System.arraycopy(iv, 0, ctr, 0, blockSize);
            ctrInit = true;
        }
        int nBlocks = inputLen / blockSize;
        for(int bc = 0; bc < nBlocks; bc++) {
            blockEncrypt(ctr, 0, tmpBlk, 0);
            System.arraycopy(input, inputOffset, output, outputOffset,
                             blockSize);
            blockXor(tmpBlk, 0, blockSize, output, outputOffset);
            inputOffset  += blockSize;
            outputOffset += blockSize;
            nboIncr(ctr);
        }
        return inputLen;
    }

    private static final void blockXor(byte[] src, int srcOffset, int srcLen,
                                       byte[] dest, int destOffset) {
        for(; srcLen > 0; srcLen--) {
            dest[destOffset++] ^= src[srcOffset++];
        }
    }

    protected static final int getIntMSBO(byte[] src, int srcOffset) {
        return (((src[srcOffset    ] & 0xff) << 24) |
                ((src[srcOffset + 1] & 0xff) << 16) |
                ((src[srcOffset + 2] & 0xff) << 8)  |
                ( src[srcOffset + 3] & 0xff));
    }

    protected static final int getIntLSBO(byte[] src, int srcOffset) {
        return (( src[srcOffset    ] & 0xff)        |
                ((src[srcOffset + 1] & 0xff) << 8)  |
                ((src[srcOffset + 2] & 0xff) << 16) |
                ((src[srcOffset + 3] & 0xff) << 24));
    }

    protected static final void putIntMSBO(int val, byte[] dest, int destOffset) {
        dest[destOffset    ] = (byte)((val >>> 24) & 0xff);
        dest[destOffset + 1] = (byte)((val >>> 16) & 0xff);
        dest[destOffset + 2] = (byte)((val >>> 8 ) & 0xff);
        dest[destOffset + 3] = (byte)( val         & 0xff);
    }

    protected static final void putIntLSBO(int val, byte[] dest, int destOffset) {
        dest[destOffset    ] = (byte)( val         & 0xff);
        dest[destOffset + 1] = (byte)((val >>> 8 ) & 0xff);
        dest[destOffset + 2] = (byte)((val >>> 16) & 0xff);
        dest[destOffset + 3] = (byte)((val >>> 24) & 0xff);
    }

    protected static final void nboIncr(byte[] b) {
        for (int i = b.length-1; i >= 0 && 0 == ++b[i]; i--)
            ;
    }

    protected int engineGetBlockSize() {
        return getBlockSize();
    }

    protected byte[] engineGetIV() {
        return iv;
    }

    protected int engineGetOutputSize(int inputLen) {
        // !!! TODO: fix this
        int bs = getBlockSize();
        if(((inputLen % bs) > 0) || pkcs5Padding) {
            inputLen += (bs - (inputLen % bs));
        }
        return inputLen;
    }

    protected void engineInit(int opmode, Key key,
                              AlgorithmParameterSpec params,
                              SecureRandom random) throws InvalidKeyException { 
        initializeKey(((SecretKeySpec)key).getEncoded());
        blockSize = getBlockSize();
        if(params == null) {
            params = new IvParameterSpec(new byte[blockSize]);
        }
        this.opMode = opmode;
        this.iv     = ((IvParameterSpec)params).getIV();
        this.tmpBlk = new byte[blockSize];
    }

    protected void engineInit(int opmode,
                              Key key,
                              AlgorithmParameters params,
                              SecureRandom random) throws InvalidKeyException {
        // XXX:
        engineInit(opmode, key, (AlgorithmParameterSpec)null, random);
    }

    protected void engineInit(int opmode, Key key,
                              SecureRandom random) throws InvalidKeyException {
        engineInit(opmode, key, (AlgorithmParameterSpec)null, random);
    }

    protected void engineSetMode(String mode) {
        if("CBC".equalsIgnoreCase(mode)) {
            feedbackMode = MODE_CBC;
        } else if("ECB".equalsIgnoreCase(mode)) {
            feedbackMode = MODE_ECB;
        } else if("CFB".equalsIgnoreCase(mode)) {
            feedbackMode = MODE_CFB;
        } else if("OFB".equalsIgnoreCase(mode)) {
            feedbackMode = MODE_OFB;
        } else if("CTR".equalsIgnoreCase(mode)) {
            feedbackMode = MODE_CTR;
        }
    }

    protected void engineSetPadding(String padding) {
        if("PKCS5Padding".equalsIgnoreCase(padding)) {
            pkcs5Padding = true;
        } 
//         else if("NoPadding".equalsIgnoreCase(padding)) {}
//         else if("SSL3Padding".equalsIgnoreCase(padding)) {}
//         else {
//             // !!! TODO Throw NoSuchPaddingException
//         }
    }

    public abstract int getBlockSize();

    public abstract void initializeKey(byte[] key) throws InvalidKeyException;

    public abstract void blockEncrypt(byte[] in, int offset,
                                      byte[] out, int outOffset);

    public abstract void blockDecrypt(byte[] in, int offset,
                                      byte[] out, int outOffset);
}
