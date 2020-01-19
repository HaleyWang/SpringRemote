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

package com.mindbright.jce.provider.keystore;

import java.io.RandomAccessFile;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;

public class DBHash {

    public final static int    HASHMAGIC   = 0x061561;
    public final static int    HASHVERSION = 2;
    public final static String CHARKEY     = "%$sniglet^&";

    // javac 1.1, does not like these in inner class
    public final static int OVFLPAGE      = 0;
    public final static int PARTIAL_KEY   = 1;
    public final static int FULL_KEY      = 2;
    public final static int FULL_KEY_DATA = 3;
    public final static int REAL_KEY      = 4;

    /**
     * (from page.h in dbm)
     * page format:
     *          +------------------------------+
     * p	| n | keyoff | datoff | keyoff |
     *          +------------+--------+--------+
     *          | datoff | free  |  ptr  | --> |
     *          +--------+---------------------+
     *          |	 F R E E A R E A       |
     *          +--------------+---------------+
     *          |  <---- - - - | data          |
     *          +--------+-----+----+----------+
     *          |  key   | data     | key      |
     *          +--------+----------+----------+
     *
     * Pointer to the free space is always:  p[p[0] + 2]
     * Amount of free space on the page is:  p[p[0] + 1]
     */
    public class Page {

        private byte[] data;
        private int    pageNumber;
        private int    rPos;
        private int    numberOfEntries;        
        private int    freeSpc;
        private int[]  offsPtrs;

        public Page(byte[] data) {
            this.data = data;
        }

        public Page(int pageNumber, byte[] data) {
            this(data);
            this.pageNumber      = pageNumber;
            this.rPos            = 0;
            this.numberOfEntries = readShort();
            this.offsPtrs        = new int[numberOfEntries];
            for(int i = 0; i < numberOfEntries; i++) {
                offsPtrs[i] = readShort();
            }
                           readShort(); // freePtr
            this.freeSpc = readShort();
        }

        public int pageNumber() {
            return pageNumber;
        }

        public int numberOfEntries() {
            return numberOfEntries;
        }

        public int freeSpace() {
            return freeSpc;
        }

        public int keyOffset(int n) {
            return offsPtrs[n * 2];
        }

        public int dataOffset(int n) {
            return offsPtrs[(n * 2) + 1];
        }

        public byte[] getKey(int n) {
            int    o = keyOffset(n);
            int    e = (n == 0 ? bsize : dataOffset(n - 1));
            byte[] k = new byte[e - o];
            System.arraycopy(data, o, k, 0, k.length);
            return k;
        }

        public byte[] getData(int n) {
            int    o = dataOffset(n);
            int    e = keyOffset(n);
            byte[] d = new byte[e - o];
            System.arraycopy(data, o, d, 0, d.length);
            return d;
        }

        public byte[] getBuf() {
            return data;
        }

        public boolean isOverflowed() {
            return !isEmpty() &&
                   ((offsPtrs[numberOfEntries - 1] == OVFLPAGE) ||
                    (numberOfEntries > 2 && offsPtrs[1] < REAL_KEY));
        }

        public int overflowType() {
            return offsPtrs[1];
        }

        public int overflowAddress() {
            return offsPtrs[numberOfEntries - 2];
        }

        public boolean isEmpty() {
            return numberOfEntries == 0;
        }

        public final int readByte() {
            return (data[rPos++]) & 0xff;
        }

        public final int readShort() {
            int b1 = readByte();
            int b2 = readByte();
            if(lorder == 1234) {
                return ((b2 << 8) + (b1 << 0));
            }
            return ((b1 << 8) + (b2 << 0));
        }

        public final int readInt() {
            // Only used with header, no need for byte-ordering
            int b1 = readByte();
            int b2 = readByte();
            int b3 = readByte();
            int b4 = readByte();
            return ((b1 << 24) + (b2 << 16) + (b3 << 8) + (b4 << 0));
        }

    }

    public class Bucket {

        private int          bucketId;
        private Vector<Page> pages;
        private Vector<DBT>  dbts;

        public Bucket(int bucketId) {
            this.bucketId     = bucketId;
            this.pages        = new Vector<Page>();
            this.dbts         = new Vector<DBT>();
        }

        public void load() throws IOException {
            Page p = getPage(bucketToPage(bucketId));
            pages.addElement(p);
            while(p.isOverflowed()) {
                p = getPage(oAddrToPage(p.overflowAddress()));
                if (!p.isEmpty() && p.overflowType() != OVFLPAGE)
                    pages.addElement(p);
            }
            scanPages();
        }

        public int numberOfKeys() {
            return dbts.size();
        }

        public DBT getDBT(int n) {
            return dbts.elementAt(n);
        }

        public void printChain() {
            System.out.print("bucket#" + bucketId);
            for(int i = 0; i < pages.size(); i++) {
                Page p = pages.elementAt(i);
                System.out.print("." + p.pageNumber());
            }
            System.out.println("");
        }

        private void scanPages() {
            for(int i = 0; i < pages.size(); i++) {
                Page    p  = pages.elementAt(i);
                boolean of = p.isOverflowed();
                if(of && p.overflowType() < REAL_KEY) {
                    i = processPartial(p, i);
                } else {
                    int n = p.numberOfEntries() / 2 - (of ? 1 : 0);
                    for(int j = 0; j < n; j++) {
                        dbts.addElement(new DBT(p.getKey(j), p.getData(j)));
                    }
                }
            }
        }

        private int processPartial(Page p, int i) {
            ByteArrayOutputStream key  = new ByteArrayOutputStream(1024);
            ByteArrayOutputStream data = new ByteArrayOutputStream(1024);

            int    off;
            byte[] buf;

            if(p.overflowType() == PARTIAL_KEY) {
                while(p.overflowType() == PARTIAL_KEY) {
                    off = p.keyOffset(0);
                    buf = p.getBuf();
                    key.write(buf, off, buf.length - off);
                    p = pages.elementAt(++i);
                }
            } else {
                off = p.keyOffset(0);
                buf = p.getBuf();
                key.write(buf, off, buf.length - off);
                /* Check for initial FULL_KEY_DATA */
                if(p.overflowType() == FULL_KEY_DATA) {
                    int doff = p.dataOffset(1);
                    buf = p.getBuf();
                    data.write(buf, doff, off - doff);
                    p = pages.elementAt(++i);
                }
            }

            while(p.overflowType() == FULL_KEY) {
                off = p.keyOffset(0);
                buf = p.getBuf();
                data.write(buf, off, buf.length - off);
                p = pages.elementAt(++i);
            }

            // TODO handle PARTIAL_KEY + FULL_KEY_DATA + FULL_KEY_DATA ??

            /* last FULL_KEY_DATA */
            off = p.keyOffset(0);
            buf = p.getBuf();
            data.write(buf, off, buf.length - off);

            dbts.addElement(new DBT(key.toByteArray(), data.toByteArray()));

            return i;
        }

    }

    public class DBT {

        public byte[] key;
        public byte[] data;

        public DBT(byte[] key, byte[] data) {
            this.key  = key;
            this.data = data;
        }

        public boolean equals(Object obj) {
            if(obj == null || !(obj instanceof DBT)) {
                return false;
            }
            DBT other = (DBT)obj;
            if(other.key.length != key.length) {
                return false;
            }
            int i;
            for(i = 0; i < key.length; i++) {
                if(other.key[i] != key[i])
                    return false;
            }
            return true;
        }

        public int hashCode() {
            return hashFunc(key);
        }

    }

    /* HASHHDR */
    private int   magic;      /* Magic NO for hash tables */
    private int   version;    /* Version ID */
    private int   lorder;     /* Byte Order */
    private int   bsize;      /* Bucket/Page Size */ 
    private int	  max_bucket; /* ID of Maximum bucket in use */
    private int	  nkeys;      /* Number of keys in hash table */
    private int	  hdrpages;   /* Size of table header */
    private int	  h_charkey;  /* value of hash(CHARKEY) */
    private int[] spares;     /* spare pages for overflow */
    private int[] bitmaps;    /* address of overflow page */

    private Hashtable<DBT, DBT> hashtable;

    /* number of bit maps and spare points */
    private final static int NCACHED = 32;

    private RandomAccessFile file;

    public DBHash() {
        spares    = new int[NCACHED];
        bitmaps   = new int[NCACHED];
        hashtable = new Hashtable<DBT, DBT>();
        bsize     = 1024;
    }

    public void load(String fileName) throws IOException {
        file = new RandomAccessFile(fileName, "r");
        loadHeader();
    }

    public void loadAll(String fileName) throws IOException {
        load(fileName);
        for(int i = 0; i <= max_bucket; i++) {
            Bucket b = getBucket(i);
            for(int j = 0; j < b.numberOfKeys(); j++) {
                DBT dbt = b.getDBT(j);
                hashtable.put(dbt, dbt);
            }
        }
    }

    public long length() throws IOException {
        return file.length();
    }

    private void loadHeader() throws IOException {
        byte[] data = new byte[1024];
        file.readFully(data);
        Page hdr =  new Page(data);

        magic   = hdr.readInt();
        version = hdr.readInt();

        if(magic != HASHMAGIC || version != HASHVERSION) {
            throw new IOException("Type/version not supported: " + magic + "/" +
                                  version);
        }

        lorder     = hdr.readInt();
        bsize      = hdr.readInt();
                     hdr.readInt(); /* Bucket shift */
                     hdr.readInt(); /* Directory Size */
                     hdr.readInt(); /* Segment Size */
                     hdr.readInt(); /* Segment shift */
                     hdr.readInt(); /* Where overflow pages are being allocated */
                     hdr.readInt(); /* Last overflow page freed */
        max_bucket = hdr.readInt();
                     hdr.readInt(); /* Mask to modulo into entire table */
                     hdr.readInt(); /* Mask to modulo into lower half of table */
                     hdr.readInt(); /* Fill factor */
        nkeys      = hdr.readInt();
        hdrpages   = hdr.readInt();
        h_charkey  = hdr.readInt();
        for(int i = 0; i < NCACHED; i++) {
            spares[i] = hdr.readInt();
        }
        for(int i = 0; i < NCACHED; i++) {
            bitmaps[i] = hdr.readShort();
        }
        if(h_charkey != hashFunc(CHARKEY)) {
            // We can still traverse table but do we want to?
            throw new IOException("Incompatible hash-function used");
        }
    }

    public Page getPage(int pageNumber) throws IOException {
        long off = (long)pageNumber * bsize;
        file.seek(off);
        byte[] data = new byte[bsize];
        file.readFully(data);
        return new Page(pageNumber, data);
    }

    public Bucket getBucket(int bucketId) throws IOException {
        Bucket bucket = new Bucket(bucketId);
        bucket.load();
        return bucket;
    }

    public int hashFunc(String str) {
        byte[] key = str.getBytes();
        byte[] tmp = new byte[key.length + 1];
        System.arraycopy(key, 0, tmp, 0, key.length);
        key = tmp;
        return hashFunc(key);
    }

    public int hashFunc(byte[] key) {
        int   h   = 0;
        if(key.length > 0) {
            for(int i = 0; i < key.length; i++) {
                h = (h << 5) + h + (key[i] & 0xff);
            }
        }
        return h;
    }

    public int __log2(int num) {
        int i, limit;
        limit = 1;
        for(i = 0; limit < num; limit = limit << 1, i++) {}
        return i;
    }

    public int bucketToPage(int bucket) {
        return bucket + hdrpages + (bucket != 0 ?
                                    spares[__log2(bucket + 1) - 1] :
                                    0);
    }

    public int oAddrToPage(int addr) {
        return bucketToPage((1 << (addr >>> 11)) - 1) + (addr & 0x7ff);
    }

    public byte[] get
        (String key) {
        return get
                   (key.getBytes());
    }

    public byte[] get
        (byte[] key) {
        DBT dbtKey = new DBT(key, null);
        return get
                   (dbtKey);
    }

    public byte[] get
        (DBT key) {
        DBT dbt = hashtable.get(key);
        byte[] data = null;
        if(dbt != null) {
            data = dbt.data;
        }
        return data;
    }

    public Enumeration<DBT> keys() {
        return hashtable.keys();
    }


    public static void main(String[] argv) {
        try {
            DBHash h = new DBHash();
            h.loadAll(argv[0]);
            System.out.println("magic: " +
                               com.mindbright.util.HexDump.intToString(h.magic));
            System.out.println("version: " + h.version);
            System.out.println("lorder: " + h.lorder);
            System.out.println("nkeys: " + h.nkeys);

            int tot = 0;

            Enumeration<DBT> keys = h.keys();
            while(keys.hasMoreElements()) {
                DBT dbt = keys.nextElement();
                System.out.println("key: ");
                com.mindbright.util.HexDump.print(dbt.key);
                System.out.println("data len: " + dbt.data.length);
                tot++;
            }

            System.out.println("Total entries: " + tot);

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }


}
