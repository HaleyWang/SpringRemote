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

package com.mindbright.util;

import java.io.IOException;
import java.io.BufferedInputStream;

import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.StringTokenizer;

/**
 * A <code>ClassLoader</code> which can load classes from a jar-file.
 */
public class JarLoader extends ClassLoader {
    String jarName;
    ZipFile jarFile;

    /**
     * Construct an instance which looks for a jar-file with the given
     * name somewhere in the given path.
     *
     * @param path a list of directories, separated by ':' or ';', to
     *             search in.
     * @param name name, including suffix, of jarfile to look for
     * @throws IOException if no jarfile with that name was found
     */
    public JarLoader(String path, String name) throws IOException {
        jarName = name;
        if (path == null) {
            path = "";
        }
        StringTokenizer st = new StringTokenizer(path, ":;");
        while (st.hasMoreTokens()) {
            try {
                jarFile = new ZipFile(st.nextToken() + "/" + name);
                return;
            } catch (IOException e) {
                // Ignore errors here
            }
        }
        throw new IOException("Failed to locate '" + name +
                              "' with jar-path: " + path);
    }

    /**
     * Finds the class with the specified name.
     *
     * @param name the name of the class to find
     *
     * @return the found class
     * @throws ClassNotFoundException if no matching class is found
     */
    @SuppressWarnings("unchecked")
	public Class findClass(String name) throws ClassNotFoundException {
        byte[] b = loadClassData(name.replace('.', '/') + ".class");
        return defineClass(name, b, 0, b.length);
    }

    /**
     * Loads the given class.
     *
     * @param name the name of the class to load
     * @param resolve true of the class should be resolved as well
     *
     * @return The resulting Class object
     * @throws ClassNotFoundException if no matching class is found
     */
    @SuppressWarnings("unchecked")
	public synchronized Class loadClass(String name, boolean resolve)
    throws ClassNotFoundException {
        Class c = null;

        c = findLoadedClass(name);
        if (c != null) {
            return c;
        }

        try {
            c = findSystemClass(name);
            if (c != null) {
                return c;
            }
        } catch (ClassNotFoundException e) {
            // noop
        }

        byte[] data = loadClassData(name.replace('.', '/') + ".class");
        if (data != null) {
        	c = defineClass(name, data, 0, data.length);
        }

        if ((c != null) && resolve) {
            resolveClass(c);
        }

        return c;
    }

    private byte[] loadClassData(String name) throws ClassNotFoundException {
        ZipEntry je = jarFile.getEntry(name);
        BufferedInputStream is = null;
        try {
            int sz = (int)je.getSize();
            byte[] buffer = new byte[sz];
            is = new BufferedInputStream(jarFile.getInputStream(je));
            int pos = 0;
            while (sz > 0) {
                int l = is.read(buffer, pos, sz);
                if (l < 0)
                    break;
                sz -= l;
                pos += l;
            }
            return buffer;
        } catch (IOException e) {
            throw new ClassNotFoundException("Can't find " + name + " in " +
                                             jarName);
        } finally {
            try {
                is.close();
            } catch (Throwable t) {}
        }
    }
}
