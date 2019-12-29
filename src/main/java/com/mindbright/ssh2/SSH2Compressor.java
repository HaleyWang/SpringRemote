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

package com.mindbright.ssh2;

/**
 * This is the abstract base class for compressors which compress/uncompress
 * the ssh stream.
 */
public abstract class SSH2Compressor {

    public final static int COMPRESS_MODE   = 1;
    public final static int UNCOMPRESS_MODE = 2;

    /**
     * Get an instance of a compressor which implements the given
     * algorithm.
     *
     * @param algorithm The compression algorithm to implement.
     *
     * @return An instance of <code>SSH2Compressor</code> or <code>null</code>.
     */
    public static SSH2Compressor getInstance(String algorithm)
        throws SSH2CompressionException {
        String className = null;
        if ("zlib".equals(algorithm)) {
            className = "com.mindbright.ssh2.SSH2CompressorZLib";
        } else if ("zlib@openssh.com".equals(algorithm)) {
            className = "com.mindbright.ssh2.SSH2CompressorZLibOpenSSH";
        }
        if (className != null) {
            try {
                Class<?> compCl = Class.forName(className);
                SSH2Compressor comp = (SSH2Compressor)compCl.newInstance();
                return comp;
            } catch (Exception e) {
                throw new SSH2CompressionException(e.toString());
            }
        }
        return null;
    }

    /**
     * Initialize the compressor. Sets the mode (compress or uncompress) as
     * well as the desired level of compression.
     *
     * @param mode Either <code>COMPRESS_MODE</code> or
     *             <code>UNCOMPRESS_MODE</code>.
     *
     * @param level The level of compression. The exact meaning of this
     *              is algorithm dependent.
     */
    public abstract void init(int mode, int level);

    /**
     * Compress a block of data.
     *
     * @param data The block of data to compress. The compressed data is left
     *             in the same buffer.
     */
    public abstract void compress(SSH2DataBuffer data)
    throws SSH2CompressionException;

    /**
     * Uncompress a block of data. The uncompressed data is stored in the
     * <code>SSH2DataBuffer</code> given so this one has to be big enough
     * to fit the uncompressed data block.
     *
     * @param data The block of data to uncompress.
     *
     * @param len  How much data can be stored in the given buffer.
     */
    public abstract int uncompress(SSH2DataBuffer data, int len)
    throws SSH2CompressionException;

    /**
     * Get the total number of bytes compressed.
     *
     * @return The number of bytes sent into the algorithm.
     */
    public abstract long numOfCompressedBytes();

    /**
     * Get the total number of bytes uncompressed.
     *
     * @return The number of bytes output by the algorithm.
     */
    public abstract long numOfUncompressedBytes();

    /**
     * Tell compression library that uiser has authenticated
     */
    public void authSucceeded() {}
}
