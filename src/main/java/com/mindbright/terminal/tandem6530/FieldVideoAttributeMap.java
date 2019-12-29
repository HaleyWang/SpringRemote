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

public class FieldVideoAttributeMap {
    protected int defaultAttrib;
    protected int numAttribs;
    protected Attrib attribs[];

    protected static class Attrib {
        private int pos;
        private int attrib;

        Attrib(int pos, int attrib) {
            this.pos = pos;
            this.attrib = attrib;
        }

        int getPos() {
            return pos;
        }
        void inc() {
            pos++;
        }
        void dec() {
            pos--;
        }
        int getAttrib() {
            return attrib;
        }
        void setAttrib(int attrib) {
            this.attrib = attrib;
        }
        public String toString() {
            return String.valueOf(pos) + ": " + attrib;
        }
    }

    public FieldVideoAttributeMap(int defaultAttrib) {
        this.defaultAttrib = defaultAttrib;
        attribs = new Attrib[1];
    }

    public void setDefaultAttrib(int defaultAttrib) {
        this.defaultAttrib = defaultAttrib;
    }

    public boolean isAttrib(int pos) {
        int index = getAttrib(pos);
        if (index < 0) {
            return false;
        }
        Attrib attrib = attribs[index];

        return attrib.getPos() == pos;
    }

    public int get
        (int pos) {
        int index = getAttrib(pos);
        if (index < 0) {
            return defaultAttrib;
        }

        return attribs[index].getAttrib();
    }

    public void set
        (int pos, int attrib) {
        if (numAttribs + 1 > attribs.length) {
            Attrib tmp[] = new Attrib[attribs.length * 2 + 1];
            System.arraycopy(attribs, 0, tmp, 0, attribs.length);
            attribs = tmp;
        }

        int idx = getAttrib(pos);
        if (idx >= 0 && attribs[idx].getPos() == pos) {
            // We are replacing an existing attribute
            attribs[idx].setAttrib(attrib);
        } else {
            Attrib newAttrib = new Attrib(pos, attrib);
            if (numAttribs == 0) {
                idx = 0;
            } else {
                idx = find(pos);
                System.arraycopy(attribs, idx, attribs, idx + 1,
                                 attribs.length - 1 - idx);
            }
            attribs[idx] = newAttrib;
            numAttribs++;
        }
        return;
    }

    public void clearAt(int pos) {
        if (!isAttrib(pos)) {
            return;
        }
        int index = getAttrib(pos);
        delete(index);
    }


    public void clearFrom(int pos) {
        int i = getAttrib(pos);
        if (i < 0) {
            if (numAttribs > 0) {
                // pos is before the first attribute, clear from the
                // first attribute
                i = 0;
            } else {
                return;
            }
        } else if (attribs[i].getPos() < pos) {
            // The attribute for pos begins before pos,
            // but perhaps the next attribute is affected?
            i++;
        }

        for (int j = numAttribs - 1; j >= i; j--) {
            delete(i);
        }
    }

    protected void delete(int index) {
        if (numAttribs == 1 || (index + 1 == numAttribs)) {
            // Remove the only or the last attribute
            numAttribs--;
        } else {
            System.arraycopy(attribs, index + 1, attribs, index,
                             numAttribs - index - 1);
            numAttribs--;
        }
    }


    public void insertAt(int pos) {
        int i = getAttrib(pos);
        if (i < 0) {
            if (numAttribs > 0) {
                // pos is before the first attribute, increment from the
                // first attribute
                i = 0;
            } else {
                return;
            }
        } else if (attribs[i].getPos() < pos) {
            // The attribute for pos begins before pos,
            // but perhaps the next attribute is affected?
            i++;
        }

        for (; i < numAttribs; i++) {
            attribs[i].inc();
        }
    }

    public void deleteAt(int pos) {
        int i = getAttrib(pos);
        if (i < 0) {
            if (numAttribs > 0) {
                // pos is before the first attribute, decrement from the
                // first attribute
                i = 0;
            } else {
                return;
            }
        } else if (attribs[i].getPos() < pos) {
            // The attribute for pos begins before pos,
            // but perhaps the next attribute is affected?
            i++;
        }

        for (; i < numAttribs; i++) {
            if (attribs[i].getPos() == pos) {
                delete(i);
                for (int j = i; j < numAttribs; j++) {
                    attribs[j].dec();
                }
            } else {
                attribs[i].dec();
            }
        }
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        if (numAttribs == 0) {
            buf.append("Empty");
        } else {
            for (int i = 0; i < numAttribs; i++) {
                if (i != 0) {
                    buf.append(' ');
                }
                buf.append(attribs[i]);
            }
        }
        return buf.toString();
    }



    protected int getAttrib(int pos) {
        if (numAttribs == 0) {
            return -1;
        }

        int ret = find(pos);
        if (ret == numAttribs) {
            // pos larger than the last attributes position
            ret--;
        }
        if (attribs[ret].getPos() > pos) {
            // The selected attributes started after pos,
            // select the previous
            ret--;
        }
        return ret;
    }

    protected int find(int pos) {
        int left = 0;
        int right = numAttribs - 1;
        int middle = 0;

        while (left <= right) {
            middle = (left + right) >>> 1;
            int cmp = attribs[middle].getPos() - pos;
            if (cmp < 0) {
                left = middle + 1;
            } else if (cmp > 0) {
                right = middle - 1;
            } else {
                break;
            }
        }
        int ret = middle;
        if (left > middle) {
            ret = left;
        }
        return ret;
    }

}
