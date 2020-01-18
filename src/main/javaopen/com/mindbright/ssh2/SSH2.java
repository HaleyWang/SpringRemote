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
 * Contains basic protocol constants and some identification strings.
 */
public abstract class SSH2 {
    public final static int    SSH_VER_MAJOR = 2;
    public final static int    SSH_VER_MINOR = 0;
    public final static int    PKG_VER_MAJOR = 3;
    public final static int    PKG_VER_MINOR = 0;
    public final static String PKG_NAME      = "MindTerm";

    public final static String getPackageVersion(String pkgName,
            int pkgMajor, int pkgMinor,
            String comment) {
        return pkgName + "_" + pkgMajor + "." + pkgMinor + " " + comment;
    }

    public final static String getVersionId() {
        return getVersionId(getPackageVersion(PKG_NAME,
                                              PKG_VER_MAJOR, PKG_VER_MINOR,
                                              "(Cryptzone Group AB SSH2)"));
    }

    public final static String getVersionId(String pkgVersion) {
        String idStr;
        if(pkgVersion == null) {
            idStr = getVersionId();
        } else {
            idStr = "SSH-" + SSH_VER_MAJOR + "." + SSH_VER_MINOR + "-" +
                    pkgVersion;
        }
        return idStr;
    }

    public final static String msgTypeString(int msgType) {
        String msgStr = null;
        switch(msgType) {
        case MSG_DISCONNECT:
            msgStr = "MSG_DISCONNECT";
            break;
        case MSG_IGNORE:
            msgStr = "MSG_IGNORE";
            break;
        case MSG_UNIMPLEMENTED:
            msgStr = "MSG_UNIMPLEMENTED";
            break;
        case MSG_DEBUG:
            msgStr = "MSG_DEBUG";
            break;
        case MSG_SERVICE_REQUEST:
            msgStr = "MSG_SERVICE_REQUEST";
            break;
        case MSG_SERVICE_ACCEPT:
            msgStr = "MSG_SERVICE_ACCEPT";
            break;
        case MSG_KEXINIT:
            msgStr = "MSG_KEXINIT";
            break;
        case MSG_NEWKEYS:
            msgStr = "MSG_NEWKEYS";
            break;
        case FIRST_KEX_PACKET:
        case 31:
        case 32:
        case 33:
        case 34:
        case 35:
        case 36:
        case 37:
        case 38:
        case 39:
        case 40:
        case 41:
        case 42:
        case 43:
        case 44:
        case 45:
        case 46:
        case 47:
        case 48:
        case LAST_KEX_PACKET:
            msgStr = "KEX_METHOD_PACKET_" + msgType;
            break;

        case MSG_USERAUTH_REQUEST:
            msgStr = "MSG_USERAUTH_REQUEST";
            break;
        case MSG_USERAUTH_FAILURE:
            msgStr = "MSG_USERAUTH_FAILURE";
            break;
        case MSG_USERAUTH_SUCCESS:
            msgStr = "MSG_USERAUTH_SUCCESS";
            break;
        case MSG_USERAUTH_BANNER:
            msgStr = "MSG_USERAUTH_BANNER";
            break;
        case FIRST_USERAUTH_METHOD_PACKET:
        case 61:
        case 62:
        case 63:
        case 64:
        case 65:
        case 66:
        case 67:
        case 68:
        case 69:
        case 70:
        case 71:
        case 72:
        case 73:
        case 74:
        case 75:
        case 76:
        case 77:
        case 78:
        case LAST_USERAUTH_METHOD_PACKET:
            msgStr = "USERAUTH_METHOD_PACKET_" + msgType;
            break;

        case MSG_GLOBAL_REQUEST:
            msgStr = "MSG_GLOBAL_REQUEST";
            break;
        case MSG_REQUEST_SUCCESS:
            msgStr = "MSG_REQUEST_SUCCESS";
            break;
        case MSG_REQUEST_FAILURE:
            msgStr = "MSG_REQUEST_FAILURE";
            break;
        case MSG_CHANNEL_OPEN:
            msgStr = "MSG_CHANNEL_OPEN";
            break;

        case MSG_CHANNEL_OPEN_CONFIRMATION:
            msgStr = "MSG_CHANNEL_OPEN_CONFIRMATION";
            break;
        case MSG_CHANNEL_OPEN_FAILURE:
            msgStr = "MSG_CHANNEL_OPEN_FAILURE";
            break;
        case MSG_CHANNEL_WINDOW_ADJUST:
            msgStr = "MSG_CHANNEL_WINDOW_ADJUST";
            break;
        case MSG_CHANNEL_DATA:
            msgStr = "MSG_CHANNEL_DATA";
            break;
        case MSG_CHANNEL_EXTENDED_DATA:
            msgStr = "MSG_CHANNEL_EXTENDED_DATA";
            break;
        case MSG_CHANNEL_EOF:
            msgStr = "MSG_CHANNEL_EOF";
            break;
        case MSG_CHANNEL_CLOSE:
            msgStr = "MSG_CHANNEL_CLOSE";
            break;
        case MSG_CHANNEL_REQUEST:
            msgStr = "MSG_CHANNEL_REQUEST";
            break;
        case MSG_CHANNEL_SUCCESS:
            msgStr = "MSG_CHANNEL_SUCCESS";
            break;
        case MSG_CHANNEL_FAILURE:
            msgStr = "MSG_CHANNEL_FAILURE";
            break;
        default:
            msgStr = "<unknown: " + msgType + ">";
            break;
        }
        return msgStr;
    }

    /* Maximum length of packet payload, including packet type. */
    public static final int MAX_PAYLOAD_LENGTH = 32768;

    /* Packet numbers for the SSH transport layer protocol. */
    public static final int MSG_DISCONNECT      = 1;
    public static final int MSG_IGNORE          = 2;
    public static final int MSG_UNIMPLEMENTED   = 3;
    public static final int MSG_DEBUG           = 4;
    public static final int MSG_SERVICE_REQUEST = 5;
    public static final int MSG_SERVICE_ACCEPT  = 6;

    public static final int MSG_KEXINIT         = 20;
    public static final int MSG_NEWKEYS         = 21;


    /* Numbers 30-49 for KEX packets.  Different KEX methods may reuse
       message numbers in this range. */
    public static final int FIRST_KEX_PACKET      = 30;

    /* Diffie-Hellman group1 key exchange */
    public static final int MSG_KEXDH_INIT        = 30;
    public static final int MSG_KEXDH_REPLY       = 31;

    /* Diffie-Hellman group and key exchange */
    public static final int MSG_KEXDH_GEX_REQUEST_OLD = 30;
    public static final int MSG_KEXDH_GEX_REQUEST = 34;
    public static final int MSG_KEXDH_GEX_GROUP   = 31;
    public static final int MSG_KEXDH_GEX_INIT    = 32;
    public static final int MSG_KEXDH_GEX_REPLY   = 33;

    /* GSS-API key exchange */
    public static final int MSG_KEXGSS_INIT     = 30;
    public static final int MSG_KEXGSS_CONTINUE = 31;
    public static final int MSG_KEXGSS_COMPLETE = 32;
    public static final int MSG_KEXGSS_HOSTKEY  = 33;
    public static final int MSG_KEXGSS_ERROR    = 34;
    public static final int MSG_KEXGSS_GROUPREQ = 40;
    public static final int MSG_KEXGSS_GROUP    = 41;

    /* EC DH key exchange */
    public static final int MSG_KEX_ECDH_INIT  = 30;
    public static final int MSG_KEX_ECDH_REPLY = 31;
    
    /* EC MQV key exchange */
    public static final int MSG_KEX_ECMQV_INIT  = 30;
    public static final int MSG_KEX_ECMQV_REPLY = 31;
    
    public static final int LAST_KEX_PACKET       = 49;


    public static final int FIRST_SERVICE_PACKET  = 50;

    /* Packet numbers for the SSH userauth protocol. */
    public static final int MSG_USERAUTH_REQUEST  = 50;
    public static final int MSG_USERAUTH_FAILURE  = 51;
    public static final int MSG_USERAUTH_SUCCESS  = 52;
    public static final int MSG_USERAUTH_BANNER   = 53;

    public static final int FIRST_USERAUTH_METHOD_PACKET = 60;
    public static final int LAST_USERAUTH_METHOD_PACKET  = 79;

    /* Packet numbers for various authentication methods. */
    /* Password authentication */
    public static final int MSG_USERAUTH_PASSWD_CHANGEREQ   = 60;

    /* Challenge-response authentication */
    public static final int MSG_USERAUTH_CHALLENGE          = 60;

    /* SSH Inc SecurID authentication (securid-1@ssh.com) */
    public static final int MSG_USERAUTH_SECURID_CHALLENGE    = 60;
    public static final int MSG_USERAUTH_SECURID_NEW_PIN_REQD = 61;
    /* SecurID new pin modes */
    public static final int SSH_SECURID_CANNOT_CHOOSE_PIN   = 0;
    public static final int SSH_SECURID_MUST_CHOOSE_PIN     = 1;
    public static final int SSH_SECURID_USER_SELECTABLE_PIN = 2;

    /* Public key authentication */
    public static final int MSG_USERAUTH_PK_OK              = 60;

    /* Keyboard interactive authentication */
    public static final int MSG_USERAUTH_INFO_REQUEST       = 60;
    public static final int MSG_USERAUTH_INFO_RESPONSE      = 61;

    /* GSS-API authentication */
    public static final int MSG_USERAUTH_GSSAPI_RESPONSE          = 60;
    public static final int MSG_USERAUTH_GSSAPI_TOKEN             = 61;
    public static final int MSG_USERAUTH_GSSAPI_EXCHANGE_COMPLETE = 63;
    public static final int MSG_USERAUTH_GSSAPI_ERROR             = 64;
    public static final int MSG_USERAUTH_GSSAPI_ERRTOK            = 65;
    public static final int MSG_USERAUTH_GSSAPI_MIC               = 66;


    /* Packet numbers for the SSH connection protocol. */
    public static final int MSG_GLOBAL_REQUEST              = 80;
    public static final int MSG_REQUEST_SUCCESS             = 81;
    public static final int MSG_REQUEST_FAILURE             = 82;
    public static final int MSG_CHANNEL_OPEN                = 90;
    public static final int MSG_CHANNEL_OPEN_CONFIRMATION   = 91;
    public static final int MSG_CHANNEL_OPEN_FAILURE        = 92;
    public static final int MSG_CHANNEL_WINDOW_ADJUST       = 93;
    public static final int MSG_CHANNEL_DATA                = 94;
    public static final int MSG_CHANNEL_EXTENDED_DATA       = 95;
    public static final int MSG_CHANNEL_EOF                 = 96;
    public static final int MSG_CHANNEL_CLOSE               = 97;
    public static final int MSG_CHANNEL_REQUEST             = 98;
    public static final int MSG_CHANNEL_SUCCESS             = 99;
    public static final int MSG_CHANNEL_FAILURE             = 100;

    public static final int MSG_RESERVED                    = 255;


    /* Debug message types */
    public static final int DEBUG_DEBUG   = 0;
    public static final int DEBUG_DISPLAY = 1;

    /* Disconnection reasons */
    public static final int DISCONNECT_HOST_NOT_ALLOWED_TO_CONNECT    = 1;
    public static final int DISCONNECT_PROTOCOL_ERROR                 = 2;
    public static final int DISCONNECT_KEY_EXCHANGE_FAILED            = 3;
    public static final int DISCONNECT_RESERVED                       = 4;
    public static final int DISCONNECT_MAC_ERROR                      = 5;
    public static final int DISCONNECT_COMPRESSION_ERROR              = 6;
    public static final int DISCONNECT_SERVICE_NOT_AVAILABLE          = 7;
    public static final int DISCONNECT_PROTOCOL_VERSION_NOT_SUPPORTED = 8;
    public static final int DISCONNECT_HOST_KEY_NOT_VERIFIABLE        = 9;
    public static final int DISCONNECT_CONNECTION_LOST		      = 10;
    public static final int DISCONNECT_BY_APPLICATION		      = 11;
    public static final int DISCONNECT_TOO_MANY_CONNECTIONS           = 12;
    public static final int DISCONNECT_AUTH_CANCELLED_BY_USER         = 13;
    public static final int DISCONNECT_NO_MORE_AUTH_METHODS_AVAILABLE = 14;
    public static final int DISCONNECT_ILLEGAL_USER_NAME              = 15;

    /* Extended channel data types. */
    public static final int EXTENDED_DATA_STDERR = 1;

    /* Channel open result codes. */
    public static final int OPEN_OK                          = 0;
    public static final int OPEN_ADMINISTRATIVELY_PROHIBITED = 1;
    public static final int OPEN_CONNECT_FAILED              = 2;
    public static final int OPEN_UNKNOWN_CHANNEL_TYPE        = 3;
    public static final int OPEN_RESOURCE_SHORTAGE           = 4;
}
