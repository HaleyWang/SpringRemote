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

public class FieldAttributes {
    /** Use Normal Video as default video attribute */
    public static final char DEFAULT_VIDEO = ' ';
    /** Use unprotected, auto-tab disabled, data type 0, MDT not set
     * as default data attribute */
    public static final char DEFAULT_DATA_ATTRIB = 'P';
    /** Use upshift not set, keyboard and AID as default extended data
     * attribute */
    public static final char DEFAULT_EXT_DATA_ATTRIB = '@';
    private char videoAttrib;
    private boolean mdt;
    private boolean autoTab;
    private boolean protect;
    private int dataType;
    private boolean upshift;
    private boolean keyboard;
    private boolean aid;

    /** Constructor with attributes set to default values.
     */
    public FieldAttributes() {
        try {
            setAttribs(DEFAULT_VIDEO, DEFAULT_DATA_ATTRIB,
                       DEFAULT_EXT_DATA_ATTRIB);
        } catch (ParseException e) {
            // Using default values, can't happend
        }
    }

    /** Constructor with extended attributes set to default values.
     * @param video     the video attribute as defined on page 3-20
     * @param data      the data attributes as defined on page 3-37
     * @throws ParseException on error in the arguments
     */
    public FieldAttributes(char video, char data) throws ParseException {
        setAttribs(video, data, DEFAULT_EXT_DATA_ATTRIB);
    }

    /** Constructor with custom values only.
     * @param video     the video attribute as defined on page 3-20
     * @param data      the data attributes as defined on page 3-37
     * @param extData   the extended data attributes as defined on page
     *                  3-38
     * @throws ParseException on error in the arguments
     */
    public FieldAttributes(char video, char data, char extData)
    throws ParseException {
        setAttribs(video, data, extData);
    }

    protected void setAttribs(char video, char data, char extData)
    throws ParseException {
        if (' ' > video || '?' < video) {
            throw new ParseException("Invalid video attribute");
        }
        videoAttrib = video;
        protect  = (data & 0x20) != 0;
        autoTab  = (data & 0x10) == 0;
        mdt      = (data & 0x01) != 0;
        dataType     = (data >> 1) & 0x07;
        upshift  = (extData & 0x01) != 0;
        keyboard = false;
        aid = false;
        switch ((extData >> 1) & 0x03) {
        case 0x00:
            keyboard = true;
            break;
        case 0x01:
            aid = true;
            break;
        case 0x02:
            keyboard = true;
            aid = true;
            break;
        default:
            /* 0x03 */
            throw new ParseException("Invalid extended data attribute");
        }
    }


    public char getVideoAttrib() {
        return videoAttrib;
    }
    public void setVideoAttrib(char videoAttrib) {
        this.videoAttrib = videoAttrib;
    }
    public boolean getMdt() {
        return mdt;
    }
    public void setMdt(boolean set) {
        mdt = set;
    }
    public boolean getAutoTab() {
        return autoTab;
    }
    public void setAutoTab(boolean set) {
        autoTab = set;
    }
    public boolean getProtect() {
        return protect;
    }
    public void setProtect(boolean set) {
        protect = set;
    }
    public int getDataType() {
        return dataType;
    }
    public void setDataType(int dataType) {
        this.dataType = dataType;
    }
    public boolean getUpShift() {
        return upshift;
    }
    public void setUpShift(boolean set) {
        upshift = set;
    }
    public boolean getKeyboard() {
        return keyboard;
    }
    public boolean getAid() {
        return aid;
    }

    public int hashCode() {
        return getVideoAttrib() +
               getDataType()  *  100       +
               (getMdt() ?       200 : 0) +
               (getAutoTab() ?   400 : 0) +
               (getProtect() ?   800 : 0) +
               (getUpShift() ?  1000 : 0) +
               (getKeyboard() ? 2000 : 0) +
               (getAid() ?      4000 : 0);
    }

    public boolean equals(Object o) {
        if (o == null || !(o instanceof FieldAttributes))
	    return false;
	    
        FieldAttributes other = (FieldAttributes) o;

        return getVideoAttrib() == other.getVideoAttrib() &&
               getAutoTab() == other.getAutoTab() &&
               getMdt() == other.getMdt() &&
               getProtect() == other.getProtect() &&
               getDataType() == other.getDataType() &&
               getUpShift() == other.getUpShift() &&
               getKeyboard() == other.getKeyboard() &&
               getAid() == other.getAid();
    }

    public String toString() {
        return  "vid='"+getVideoAttrib() +"'"+
                " datatype="+getDataType()+
                " autotab="+(getAutoTab() ? "1" : "0") +
                " mdt="+(getMdt() ? "1" : "0") +
                " protect="+(getProtect() ? "1" : "0") +
                " upshift="+(getUpShift() ? "1" : "0") +
                " keyboard="+(getKeyboard() ? "1" : "0") +
                " aid="+(getAid() ? "1" : "0");
    }
}


