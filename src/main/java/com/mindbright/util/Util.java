/******************************************************************************
 *
 * Copyright (c) 2005-2011 Cryptzone Group AB. All Rights Reserved.
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

/**
 * Miscellaneous utility functions
 */
public final class Util {

    /**
     * Extracts the host part of an address string.
     * The address string is expected to be in the format:
     * <code>host:port</code>
     * where the ":port" part is optional. The host part is either a
     * name or an IPv4 address. The host may also be an IPv6 literal
     * address enclosed in brackets (<code>[]</code>).
     *
     * @param address the string to extract data from
     * @return the host name
     */
    public static String getHost(String address) {
        int i;

        // IPv6 literal?
        if (address.startsWith("[")) {
            if ((i = address.indexOf(']', 1)) > 0) {
                return address.substring(1, i);
            }
        }

        // Is there a :port ?
        if ((i = address.indexOf(':')) > 0) {
            return address.substring(0, i);
        }

        return address;
    }

    /**
     * Extracts the host part of an address string.
     * The address string is expected to be in the format:
     * <code>host:port</code>
     * where the ":port" part is optional. The host part is either a
     * name or an IPv4 address. The host may also be an IPv6 literal
     * address enclosed in brackets (<code>[]</code>).
     *
     * @param address the string to extract data from
     * @param def default port number to return if the address does not
     *            contain any port definition.
     * @return the port name
     */
    public static int getPort(String address, int def) {
        int start = 0;

        // IPv6 literal?
        if (address.startsWith("[")) {
            start = address.indexOf(']', 1);
        }

        // Is there a :port ?
        if ((start = address.indexOf(':', start)) > 0) {
            return Integer.parseInt(address.substring(start + 1));
        }

        return def;
    }

    /**
     * Parses a port forward spec of the following format:
     * <code>/plugin/local_host:local_port:remote_host:remote_port</code>
     * Where <code>/plugin/</code> and <code>local_host:</code> are
     * optional.
     * <p>
     * local_host and remote_host may be names or literal IPv4
     * addreses. They can also be literal IPv6 addresses enclosed in
     * (<code>[]</code>).
     *
     * @param spec the port forward spec
     * @param local defaukt local listenber address
     * @return an array of five objects 
     */
    public static Object[] parseForwardSpec(String spec, Object local)
    throws IllegalArgumentException {
        int    d1, d2, d3;
        String tmp = spec;
        Object[] components = new Object[5];

        // Plugin
        if(tmp.startsWith("/")) {
            int i = tmp.indexOf('/', 1);
            if(i == -1) {
                throw new IllegalArgumentException("Invalid port forward spec. "
                                                   + spec);
            }
            components[0] = tmp.substring(1, i);
            tmp = tmp.substring(i + 1);
        } else {
            components[0] = "general";
        }

        // local_host
        if (tmp.startsWith("[")
            && -1 != (d1 = tmp.indexOf(']', 1))
            && ':' == tmp.charAt(d1+1)) {
            components[1] = tmp.substring(1, d1);
            tmp = tmp.substring(d1+2); // ]:

        } else if (-1 != (d1 = tmp.indexOf('['))
                   && -1 != (d2 = tmp.indexOf(':'))
                   && -1 != (d3 = tmp.indexOf(':', d2+1))
                   && d2 < d1
                   && d3 < d1) {
            components[1] = tmp.substring(0, d1);
            tmp = tmp.substring(d1+1);

        } else if (-1 == tmp.indexOf('[')
                   && -1 != (d1 = tmp.indexOf(':'))
                   && -1 != (d2 = tmp.indexOf(':', d1+1))
                   && -1 != tmp.indexOf(':', d2+1)) {
            components[1] = tmp.substring(0, d1);
            tmp = tmp.substring(d1+1);
        } else {
            components[1] = local;
        }

        // local_port
        if (0 == (d1 = tmp.indexOf(':'))) {
            throw new IllegalArgumentException("Invalid port forward spec. " +
                                               spec);
        }
        components[2] = Integer.valueOf(tmp.substring(0, d1));
        tmp = tmp.substring(d1+1);

        // remote_host
        if (tmp.startsWith("[")
            && -1 != (d1 = tmp.indexOf(']', 1))
            && ':' == tmp.charAt(d1+1)) {
            components[3] = tmp.substring(1, d1);
            tmp = tmp.substring(d1+2);
        } else if (-1 != (d1 = tmp.indexOf(':'))) {
            components[3] = tmp.substring(0, d1);
            tmp = tmp.substring(d1+1);
        } else {
            throw new IllegalArgumentException("Invalid port forward spec. " +
                                               spec);
        }

        // remote_port
        components[4] = Integer.valueOf(tmp);

        return components;
    }

    /**
     * Convert an hex-string to byte array "0001a0" -> {0x00, 0x01, 0xa0}
     *
     * @param in a hexadecimal string
     * @return a byte array
     */
    public static byte[] byteArrayFromHexString(String in) {
        byte out[] = new byte[in.length()/2];
        for(int i=0; i < in.length(); i += 2) {
            out[i/2] = (byte)Integer.parseInt(in.substring(i, i+2), 16);
        }
        return out;
    }

	public static boolean isLink(java.io.File f) {
		// if last part of absolute and canonical path differ, this is a link
		try {
			String ap[] = f.getAbsolutePath().split(java.io.File.pathSeparator);
			String cp[] = f.getCanonicalPath().split(java.io.File.pathSeparator);
			return !ap[ap.length-1].equals(cp[cp.length-1]);
		} catch (Throwable t) {
		}
		return false;
	}

    /////////////////////////////////////////////////////////////////////////
    // The code below is for unit testing only.
    // Uncomment and run this class to test
    /////////////////////////////////////////////////////////////////////////
/*
    // Test an host:port expression
    private static void testHostPort(String address, int defport,
                                     String host, int port) {
        System.out.print(" " + address + " -> ");
        if (!host.equals(getHost(address))) {
            System.out.println("FAILED host <" + getHost(address) + ">");
        } else if (port != getPort(address, defport)) {
            System.out.println("FAILED port " + getPort(address, defport));
        } else {
            System.out.println("ok");
        }
    }

    // Test a bad host:port specification
    private static void testBadHostPort(String address) {
        System.out.print(" " + address + " -> ");
        try {
            getHost(address);
        } catch (Exception e) {
            System.out.println("ok");
            return;
        }
            
        try {
            getPort(address, 1);
        } catch (Exception e) {
            System.out.println("ok");
            return;
        }
        System.err.println("FAILED");
    }

    // Test an portforward spec
    private static void testFwdSpec(String spec, Object local,
                                    String plugin,
                                    String local_host,
                                    int    local_port,
                                    String remote_host,
                                    int    remote_port) {
        System.out.print(" " + spec + " -> ");
        Object comps[] = parseForwardSpec(spec, local);

        if (plugin != null && comps[0] == null) {
            System.out.println(" FAILED plugin is null");
        } else if (plugin != null && !plugin.equals((String)comps[0])) {
            System.out.println(" FAILED plugin is <" + comps[0] + ">");
        } else if (!local_host.equals(comps[1])) {
            System.out.println(" FAILED local_host is <" + comps[1] + ">");
        } else if (local_port != ((Integer)comps[2]).intValue()) {
            System.out.println(" FAILED local_port is " + comps[2]);
        } else if (!remote_host.equals(comps[3])) {
            System.out.println(" FAILED remote_host is <" + comps[3] + ">");
        } else if (remote_port != ((Integer)comps[4]).intValue()) {
            System.out.println(" FAILED remote_port is " + comps[4]);
        } else {
            System.out.println("ok");
        }
    }

    // Test a bad portforward specification
    private static void testBadFwdSpec(String spec) {
        System.out.print(" " + spec + " -> ");
        try {
            parseForwardSpec(spec, "::1");
            System.out.println("FAILED");
        } catch (Exception e) {
            System.out.println("ok");
        }
    }

    // Run unit tests
    public static void main(String argv[]) {
        System.out.println("Testing host:port parsing");
        testHostPort("host",           23, "host",      23);
        testHostPort("host:22",        23, "host",      22);
        testHostPort("127.0.0.1",      23, "127.0.0.1", 23);
        testHostPort("127.0.0.1:22",   23, "127.0.0.1", 22);
        testHostPort("[::1]",          23, "::1",       23);
        testHostPort("[::1]:22",       23, "::1",       22);
        testHostPort("[a:b:c:d:e]",    23, "a:b:c:d:e", 23);
        testHostPort("[a:b:c:d:e]:22", 23, "a:b:c:d:e", 22);

        System.out.println("Testing bad host:port parsing");
        testBadHostPort("host::1");
        testBadHostPort("host:a");
        testBadHostPort("host:1a");
        testBadHostPort("host:1:a");

        System.out.println("Testing portfwd parsing");
        testFwdSpec("22:localhost:23", "::1",
                    null, "::1", 22, "localhost", 23);
        testFwdSpec("22:[a:b:c:d:e]:23", "::1",
                    null, "::1", 22, "a:b:c:d:e", 23);
        testFwdSpec("/general/22:localhost:23", "::1",
                    "general", "::1", 22, "localhost", 23);
        testFwdSpec("/general/127.0.0.2:22:localhost:23", "::1",
                    "general", "127.0.0.2", 22, "localhost", 23);
        testFwdSpec("127.0.0.2:22:localhost:23", "::1",
                    null, "127.0.0.2", 22, "localhost", 23);
        testFwdSpec("[::2]:22:localhost:23", "::1",
                    null, "::2", 22, "localhost", 23);
        testFwdSpec("[::2]:22:[a:b:c:d:e]:23", "::1",
                    null, "::2", 22, "a:b:c:d:e", 23);
        testFwdSpec("/foo/[::2]:22:[a:b:c:d:e]:23", "::1",
                    "foo", "::2", 22, "a:b:c:d:e", 23);

        System.out.println("Testing bad portfwd parsing");
        testBadFwdSpec("a:localhost:1");
        testBadFwdSpec("1:localhost:a");
        testBadFwdSpec(":1:localhost:a");
        testBadFwdSpec(":1:a:b:c:d:a");
        testBadFwdSpec("/plugin:1:localhost:1");
        testBadFwdSpec("/plugin:1:localhost:1/");
    }
*/
}

