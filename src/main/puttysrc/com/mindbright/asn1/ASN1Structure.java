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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public class ASN1Structure extends ASN1Object {

    private static class StructComponent {

        protected ASN1Object value;
        protected ASN1Object defaultValue;
        protected boolean    isOptional;

        protected StructComponent(ASN1Object value, ASN1Object defaultValue,
                                  boolean isOptional) {
            this.value        = value;
            this.defaultValue = defaultValue;
            this.isOptional   = isOptional;
        }

    }

    protected StructComponent[] components;
    protected int               count;
    protected Class<?>          ofType;

    protected ASN1Structure(int tag) {
        this(tag, 4);
    }

    protected ASN1Structure(int tag, int initialSize) {
        super(tag | ASN1.TYPE_CONSTRUCTED);
        this.components = new StructComponent[initialSize];
        this.count      = 0;
        this.ofType     = null;
    }

    public int getCount() {
        return count;
    }

    public ASN1Object getComponent(int index) {
        ASN1Object component = null;
        if(count > 0 && index < count) {
            component = components[index].value;
        }
        return component;
    }

    public ASN1Object getDistinctComponent(int tag) {
        int i;
        for(i = 0; i < count; i++) {
            if(tag == components[i].value.getTag()) {
                break;
            }
        }
        return getComponent(i);
    }

    public boolean isOptional(int index) {
        boolean isOptional = false;
        if(count > 0 && index < count) {
            isOptional = components[index].isOptional;
        }
        return isOptional;
    }

    public ASN1Object getDefault(int index) {
        ASN1Object defaultValue = null;
        if(count > 0 && index < count) {
            defaultValue = components[index].defaultValue;
        }
        return defaultValue;
    }

    public ASN1Object getDecodeComponent(int index, int tag)
    throws IOException {
        Class<?>   ofType    = ofType();
        ASN1Object component = null;

        if(ofType == null) {
            // !!! TODO check count / max count constraint here

            if(this instanceof ASN1Set) {
                component = getDistinctComponent(tag);
            } else {
                component = getComponent(index);
                if(component != null) {
                    int ctag  = component.getTag();
                    //
                    // !!! OUCH not very clean, want to be able to handle OPTIONAL
                    // but we also have ANY and Constructed Strings...
                    // !!! TODO clean this out
                    //
                    boolean tagMatch = (ctag == ASN1.TAG_ANY) ||
                        (ctag == tag) ||
                        (ctag == (tag & ~ASN1.TYPE_CONSTRUCTED));
                    if(component instanceof ASN1Choice) {
                        tagMatch = (((ASN1Choice)component).getMember(tag) !=
                                    null);
                    }
                    if(!tagMatch) {
                        component = null;
                    }
                }
            }
            if(component == null && !isOptional(index)) {
                throw new IOException("Error when decoding structure " +
                                      getType() + ", component not found (t=" +
                                      tag + ", i=" + index + ")");
            }
        } else {
            try {
                component = (ASN1Object)ofType().newInstance();
                addComponent(component);
            } catch (Exception e) {
                throw new IOException("Error when decoding structure " +
                                      getType() + ": " + e.getMessage());
            }
        }

        return component;
    }

    public void addComponent(ASN1Object component) {
        addComponent(component, null, false);
    }

    public void addComponent(ASN1Object component, ASN1Object defaultValue,
                             boolean isOptional) {
        if(count >= components.length) {
            StructComponent[] tmp = components;
            components = new StructComponent[(tmp.length + 1) * 2];
            System.arraycopy(tmp, 0, components, 0, tmp.length);
        }
        components[count++] = new StructComponent(component, defaultValue,
                              isOptional);
    }

    public void addOptional(ASN1Object component) {
        addComponent(component, null, true);
    }

    public void addOptional(ASN1Object component, int defInteger) {
        ASN1Integer asn1Int = new ASN1Integer();
        asn1Int.setValue(defInteger);
        addComponent(component, asn1Int, true);
    }

    public void addOptional(ASN1Object component, boolean defBoolean) {
        ASN1Boolean asn1Bool = new ASN1Boolean();
        asn1Bool.setValue(defBoolean);
        addComponent(component, asn1Bool, true);
    }

    public int encodeValue(ASN1Encoder encoder, OutputStream out)
    throws IOException {
        return encoder.encodeStructure(out, this);
    }

    public void decodeValue(ASN1Decoder decoder, InputStream in, int len)
    throws IOException {
        decoder.decodeStructure(in, len, this);
        setValue();
        // !!! TODO Check that all non-optional/default values are set
    }

    protected Class<?> ofType() {
        return ofType;
    }

    public boolean equals(Object obj) {
        if(obj == null || !(obj instanceof ASN1Structure)) {
            return false;
        }
        ASN1Structure other = (ASN1Structure)obj;
        if(count != other.getCount()) {
            return false;
        }
        for(int i = 0; i < count; i++) {
            if(!getComponent(i).equals(other.getComponent(i))) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int hash = 0;
        for(int i = 0; i < count; i++) {
            hash += getComponent(i).hashCode();
        }
        return hash;
    }

}
