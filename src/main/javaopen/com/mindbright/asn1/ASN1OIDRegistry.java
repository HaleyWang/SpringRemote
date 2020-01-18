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

import java.util.Properties;
import java.util.Hashtable;
import java.util.Enumeration;

public class ASN1OIDRegistry extends Properties {

	private static final long serialVersionUID = 1L;
	private static ASN1OIDRegistry registry = new ASN1OIDRegistry();
    private static Hashtable<String, Properties> modules  = new Hashtable<String, Properties>();

    public void putName(String oid, String name, String shortName) {
        putName(oid, name);
        putShortName(oid, shortName);
    }

    public void putShortName(String oid, String shortName) {
        put("shortname." + oid, shortName);
        put("shortname." + shortName, oid);
    }

    public void putName(String oid, String name) {
        put("name." + oid, name);
        put("name." + name, oid);
    }

    public void putNameAndType(String o, String n, String t) {
        putName(o, n);
        put(o, t);
    }
    
    public static synchronized Class<?> lookupType(String oid) {
        String   type     = registry.getProperty(oid);
        Class<?> typeImpl = null;
        if(type != null) {
            if(type.indexOf('.') == -1) {
                type = "com.mindbright.asn1." + type;
            }
            try {
                typeImpl = Class.forName(type);
            } catch (ClassNotFoundException e) {
                throw new Error("Can't find class '" + type +
                                "' registered in ASN1OIDRegistry with '" +
                                oid + "'");
            }
        }
        return typeImpl;
    }

    public static Class<?> lookupType(ASN1OID oid) {
        return lookupType(oid.getString());
    }

    public static synchronized String lookupName(String id) {
        id = "name." + id;
        return registry.getProperty(id);
    }

    public static synchronized String lookupShortName(String id) {
        id = "shortname." + id;
        return registry.getProperty(id);
    }

    public static void register(ASN1OID oid, String type) {
        register(oid.getString(), type);
    }

    public static synchronized void register(String oid, String type) {
        registry.put(oid, type);
    }

    public static void addModule(String moduleName) {
        // !!! TODO circular references will loop !!!
        try {
            ASN1OIDRegistry registeredTypes = (ASN1OIDRegistry)
                                              Class.forName(moduleName + ".RegisteredTypes").newInstance();
            addModule(moduleName, registeredTypes);
        } catch (Exception e) {
            throw new Error("In ASN1OIDRegistry: " + e.getMessage());
        }
    }

    public static void addModule(String moduleName, Properties moduleTypes) {
        // !!! OUCH synchronization
        if(modules.get(moduleName) != null) {
            /* already added... */
            return;
        }
        modules.put(moduleName, moduleTypes);
        Enumeration<?> e = moduleTypes.propertyNames();
        while(e.hasMoreElements()) {
            String key = (String)e.nextElement();
            register(key, moduleTypes.getProperty(key));
        }
    }

}
