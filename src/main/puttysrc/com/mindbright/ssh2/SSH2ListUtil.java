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

import java.util.StringTokenizer;

// !!! TODO, rewrite this mess to a real list handling class instead !!!
//
/**
 * Util class for manipulating commaseparated lists.
 */
public final class SSH2ListUtil {
    /**
     * Select the first entry in the clientList which also exists in
     * the server List.
     *
     * @param clientList List of keys to look for.
     * @param serverList List to look in.
     *
     * @return The new list.
     */
    public static String chooseFromList(String clientList, String serverList) {
        String[] cliL = arrayFromList(clientList);
        String[] srvL = arrayFromList(serverList);
        for(int i = 0; i < cliL.length; i++) {
            for(int j = 0; j < srvL.length; j++) {
                if(cliL[i].equals(srvL[j])) {
                    return cliL[i];
                }
            }
        }
        return null;
    }

    /**
     * Remove all instances of a specific element from list.
     *
     * @param list List to remove elements from.
     * @param element Element to remove.
     *
     * @return The new list.
     */
    public static String removeAllFromList(String list, String element) {
        StringBuilder buf = new StringBuilder();
        String[]     arr = arrayFromList(list);
        for(int i = 0; i < arr.length; i++) {
            if(arr[i].equals(element))
                continue;
            if(buf.length() > 0)
                buf.append(",");
            buf.append(arr[i]);
        }
        return buf.toString();
    }

    /**
     * Remove all elements which starts with the given string from list.
     *
     * @param list List to remove elements from.
     * @param prefix Prefix of elements to remove.
     *
     * @return The new list.
     */
    public static String removeAllPrefixFromList(String list, String prefix) {
        StringBuilder buf = new StringBuilder();
        String[]     arr = arrayFromList(list);
        for(int i = 0; i < arr.length; i++) {
            if(arr[i].startsWith(prefix))
                continue;
            if(buf.length() > 0)
                buf.append(",");
            buf.append(arr[i]);
        }
        return buf.toString();
    }

    /**
     * Remove the first instance of the given element from the list.
     *
     * @param list List to remove element from.
     * @param element Element to remove.
     *
     * @return The new list.
     */
    public static String removeFirstFromList(String list, String element) {
        StringBuilder buf = new StringBuilder();
        String[]     arr = arrayFromList(list);
        for(int i = 0; i < arr.length; i++) {
            if(arr[i].equals(element)) {
                element = "";
                continue;
            }
            if(buf.length() > 0)
                buf.append(",");
            buf.append(arr[i]);
        }
        return buf.toString();
    }

    /**
     * Get the first element of the list.
     *
     * @param list The list.
     *
     * @return The first element or <code>null</code> if the list is empty.
     */
    public static String getFirstInList(String list) {
        String[] arr = arrayFromList(list);
        if(arr.length == 0) {
            return null;
        }
        return arr[0];
    }

    /**
     * Checks if the given element is mentioned in the list.
     *
     * @param list List to look in.
     * @param element Element to look for.
     *
     * @return True if the element is found.
     */
    public static boolean isInList(String list, String element) {
        String[] arr = arrayFromList(list);
        for(int i = 0; i < arr.length; i++) {
            if(arr[i].equals(element)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the any element in the list starts with the given prefix.
     *
     * @param list List to look in.
     * @param prefix Prefix to look for.
     *
     * @return True if any matching element is found.
     */
    public static boolean isPrefixInList(String list, String prefix) {
        String[] arr = arrayFromList(list);
        for(int i = 0; i < arr.length; i++) {
            if(arr[i].startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sort the list alphabetically.
     *
     * @param list List to sort.
     *
     * @return A sorted list.
     */
    public static String sortList(String list) {
        String[] arr = arrayFromList(list);
        for(int i = 0; i < arr.length; i++) {
            for(int j = i; j < arr.length; j++) {
                if(!(arr[i].compareTo(arr[j])< 0)) {
                    String tmp = arr[j];
                    arr[j] = arr[i];
                    arr[i] = tmp;
                }
            }
        }
        return listFromArray(arr);
    }

    /**
     * Convert a list expressed as a commaseparated string to an array.
     *
     * @param list List to split.
     *
     * @return Resulting array.
     */
    public static String[] arrayFromList(String list) {
        return arrayFromList(list, ",");
    }
    
    /**
     * Convert a list expressed as a delimited string to an array.
     *
     * @param list List to split.
     * @param delim Delimiter.
     *
     * @return Resulting array.
     */
    public static String[] arrayFromList(String list, String delim) {
        if(list == null) {
            return new String[0];
        }
        StringTokenizer st = new StringTokenizer(list, delim);
        int cnt = 0;
        String[] sa = new String[st.countTokens()];
        while(st.hasMoreTokens()) {
            sa[cnt++] = st.nextToken().trim();
        }
        return sa;
    }

    /**
     * Convert an array of strings into a commaseparated string.
     *
     * @param arr Array to convert.
     *
     * @return The resulting string.
     */
    public static String listFromArray(String[] arr) {
        StringBuilder buf = new StringBuilder();
        for(int i = 0; i < arr.length; i++) {
            if(arr[i] == null || arr[i].equals("")) {
                continue;
            }
            if(buf.length() > 0)
                buf.append(",");
            buf.append(arr[i]);
        }
        return buf.toString();
    }

}
