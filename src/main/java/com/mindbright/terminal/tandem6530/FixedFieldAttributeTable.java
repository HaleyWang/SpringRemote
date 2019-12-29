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

package com.mindbright.terminal.tandem6530;

/** Fixed Field Definition Table (table 3-9).
 * This table is supposed to be 64 entries, but only the first 24 is
 * listen in table 3-9.
 * From another customer we got the entire table:
 *<code>
 *    Defined Field Attributes Assignments
 * 
 *     The Defined Field Attributes Assignments (ESC -s and FS) sequences
 *     act upon two new field attribute tables.  One table is named the Fixed
 *     Field Definitions table and the other table is the Variable Field
 *     Definitions table.
 * 
 *     The Fixed Field Definitions table is defined by Pathway and EM3270
 *     (Tandem Product release TBD ). The values in this table cannot be
 *     programmatically altered.  The definition of this table is:
 * 
 *      +------------------------------------------+---------------------+
 *      |     FIXED FIELD DEFINITIONS TABLE        | Pathway table (cont)|
 *      +--------------------+---------------------+---------------------+
 *      | Assigned by EM3270 | Assigned by PATHWAY | Assigned by PATHWAY |
 *      +--------------------+---------------------+---------------------+
 *      |   ix   va d1 d2    |   ix   va d1 d2     |   ix   va d1 d2     |
 *      +--------------------+---------------------+---------------------+
 *      |   00 = 20 40 44    |   24 = 20 50 44     |   48 = 20 41 44     |
 *      |   01 = 20 41 44    |   25 = 21 50 44     |   49 = 21 41 44     |
 *      |   02 = 20 48 44    |   26 = 22 50 44     |   50 = 22 41 44     |
 *      |   03 = 20 49 44    |   27 = 23 50 44     |   51 = 23 41 44     |
 *      |   04 = 20 60 44    |   28 = 30 50 44     |   52 = 30 41 44     |
 *      |   05 = 20 61 44    |   29 = 31 50 44     |   53 = 31 41 44     |
 *      |   06 = 20 68 44    |   30 = 32 50 44     |   54 = 32 41 44     |
 *      |   07 = 20 69 44    |   31 = 33 50 44     |   55 = 33 41 44     |
 *      |   08 = 21 40 44    |   32 = 20 60 44     |   56 = 20 51 44     |
 *      |   09 = 21 41 44    |   33 = 21 60 44     |   57 = 21 51 44     |
 *      |   10 = 21 48 44    |   34 = 22 60 44     |   58 = 22 51 44     |
 *      |   11 = 21 49 44    |   35 = 23 60 44     |   59 = 23 51 44     |
 *      |   12 = 21 60 44    |   36 = 30 60 44     |   60 = 30 51 44     |
 *      |   13 = 21 61 44    |   37 = 31 60 44     |   61 = 31 51 44     |
 *      |   14 = 21 68 44    |   38 = 32 60 44     |   62 = 32 51 44     |
 *      |   15 = 21 69 44    |   39 = 33 60 44     |   63 = 33 51 44     |
 *      |   16 = 28 40 44    |   40 = 20 40 44     |---------------------+
 *      |   17 = 28 41 44    |   41 = 21 40 44     |
 *      |   18 = 28 48 44    |   42 = 22 40 44     |
 *      |   19 = 28 49 44    |   43 = 23 40 44     |
 *      |   20 = 28 60 44    |   44 = 30 40 44     |
 *      |   21 = 28 61 44    |   45 = 31 40 44     |
 *      |   22 = 28 68 44    |   46 = 32 40 44     |
 *      |   23 = 28 69 44    |   47 = 33 40 44     |
 *      +--------------------+---------------------+
 * 
 *          ix = the 0-base index into the field definition table
 *          va = video attribute         (format in appendix)
 *          d1 = normal data attribute   (format in appendix)
 *          d2 = extended data attribute (format in appendix)
 *</code>
 */

public class FixedFieldAttributeTable extends VariableFieldAttributeTable {
    static {
        try {
            FieldAttributes tmp[] = {
                new FieldAttributes((char)0x20, (char)0x40, (char)0x44), // 00
                new FieldAttributes((char)0x20, (char)0x41, (char)0x44), // 01
                new FieldAttributes((char)0x20, (char)0x48, (char)0x44), // 02
                new FieldAttributes((char)0x20, (char)0x49, (char)0x44), // 03
                new FieldAttributes((char)0x20, (char)0x60, (char)0x44), // 04
                new FieldAttributes((char)0x20, (char)0x61, (char)0x44), // 05
                new FieldAttributes((char)0x20, (char)0x68, (char)0x44), // 06
                new FieldAttributes((char)0x20, (char)0x69, (char)0x44), // 07
                new FieldAttributes((char)0x21, (char)0x40, (char)0x44), // 08
                new FieldAttributes((char)0x21, (char)0x41, (char)0x44), // 09
                new FieldAttributes((char)0x21, (char)0x48, (char)0x44), // 10
                new FieldAttributes((char)0x21, (char)0x49, (char)0x44), // 11
                new FieldAttributes((char)0x21, (char)0x60, (char)0x44), // 12
                new FieldAttributes((char)0x21, (char)0x61, (char)0x44), // 13
                new FieldAttributes((char)0x21, (char)0x68, (char)0x44), // 14
                new FieldAttributes((char)0x21, (char)0x69, (char)0x44), // 15
                new FieldAttributes((char)0x28, (char)0x40, (char)0x44), // 16
                new FieldAttributes((char)0x28, (char)0x41, (char)0x44), // 17
                new FieldAttributes((char)0x28, (char)0x48, (char)0x44), // 18
                new FieldAttributes((char)0x28, (char)0x49, (char)0x44), // 19
                new FieldAttributes((char)0x28, (char)0x60, (char)0x44), // 20
                new FieldAttributes((char)0x28, (char)0x61, (char)0x44), // 21
                new FieldAttributes((char)0x28, (char)0x68, (char)0x44), // 22
                new FieldAttributes((char)0x28, (char)0x69, (char)0x44), // 23
                new FieldAttributes((char)0x20, (char)0x50, (char)0x44), // 24
                new FieldAttributes((char)0x21, (char)0x50, (char)0x44), // 25
                new FieldAttributes((char)0x22, (char)0x50, (char)0x44), // 26
                new FieldAttributes((char)0x23, (char)0x50, (char)0x44), // 27
                new FieldAttributes((char)0x30, (char)0x50, (char)0x44), // 28
                new FieldAttributes((char)0x31, (char)0x50, (char)0x44), // 29
                new FieldAttributes((char)0x32, (char)0x50, (char)0x44), // 30
                new FieldAttributes((char)0x33, (char)0x50, (char)0x44), // 31
                new FieldAttributes((char)0x20, (char)0x60, (char)0x44), // 32
                new FieldAttributes((char)0x21, (char)0x60, (char)0x44), // 33
                new FieldAttributes((char)0x22, (char)0x60, (char)0x44), // 34
                new FieldAttributes((char)0x23, (char)0x60, (char)0x44), // 35
                new FieldAttributes((char)0x30, (char)0x60, (char)0x44), // 36
                new FieldAttributes((char)0x31, (char)0x60, (char)0x44), // 37
                new FieldAttributes((char)0x32, (char)0x60, (char)0x44), // 38
                new FieldAttributes((char)0x33, (char)0x60, (char)0x44), // 39
                new FieldAttributes((char)0x20, (char)0x40, (char)0x44), // 40
                new FieldAttributes((char)0x21, (char)0x40, (char)0x44), // 41
                new FieldAttributes((char)0x22, (char)0x40, (char)0x44), // 42
                new FieldAttributes((char)0x23, (char)0x40, (char)0x44), // 43
                new FieldAttributes((char)0x30, (char)0x40, (char)0x44), // 44
                new FieldAttributes((char)0x31, (char)0x40, (char)0x44), // 45
                new FieldAttributes((char)0x32, (char)0x40, (char)0x44), // 46
                new FieldAttributes((char)0x33, (char)0x40, (char)0x44), // 47
                new FieldAttributes((char)0x20, (char)0x41, (char)0x44), // 48
                new FieldAttributes((char)0x21, (char)0x41, (char)0x44), // 49
                new FieldAttributes((char)0x22, (char)0x41, (char)0x44), // 50
                new FieldAttributes((char)0x23, (char)0x41, (char)0x44), // 51
                new FieldAttributes((char)0x30, (char)0x41, (char)0x44), // 52
                new FieldAttributes((char)0x31, (char)0x41, (char)0x44), // 53
                new FieldAttributes((char)0x32, (char)0x41, (char)0x44), // 54
                new FieldAttributes((char)0x33, (char)0x41, (char)0x44), // 55
                new FieldAttributes((char)0x20, (char)0x51, (char)0x44), // 56
                new FieldAttributes((char)0x21, (char)0x51, (char)0x44), // 57
                new FieldAttributes((char)0x22, (char)0x51, (char)0x44), // 58
                new FieldAttributes((char)0x23, (char)0x51, (char)0x44), // 59
                new FieldAttributes((char)0x30, (char)0x51, (char)0x44), // 60
                new FieldAttributes((char)0x31, (char)0x51, (char)0x44), // 61
                new FieldAttributes((char)0x32, (char)0x51, (char)0x44), // 62
                new FieldAttributes((char)0x33, (char)0x51, (char)0x44), // 63
            };
            defaultTable = tmp;
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public FixedFieldAttributeTable() {
        reset();
    }

    public void set
    (int startIndex, FieldAttributes entries[]) {
        // This table is fixed
    }
}



