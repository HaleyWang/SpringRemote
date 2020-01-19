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

package com.mindbright.ssh;

import java.io.*;

import java.security.MessageDigest;

import java.security.interfaces.RSAPublicKey;

import com.mindbright.util.Crypto;
import com.mindbright.util.SecureRandomAndPad;

public abstract class SSH {

    public static boolean DEBUG     = false;
    public static boolean DEBUGMORE = false;
    public static boolean DEBUGPKG  = false;

    public final static int    SSH_VER_MAJOR = 1;
    public final static int    SSH_VER_MINOR = 5;
    public final static String VER_MINDTERM  = "MindTerm_" + Version.version;
    public final static String VER_MINDTUNL  = "MindTunnel_" + Version.version;

    public final static int    DEFAULTPORT        = 22;
    public final static int    SESSION_KEY_LENGTH = 256; // !!! Must be multiple of 8
    public final static int    SERVER_KEY_LENGTH  = 768;
    public final static int    HOST_KEY_LENGTH    = 1024;

    public final static int    PROTOFLAG_SCREEN_NUMBER	= 1;
    public final static int    PROTOFLAG_HOST_IN_FWD_OPEN	= 2;

    public final static int    MSG_ANY                        = -1; // !!! Not part of protocol
    public final static int    MSG_NONE                       = 0;
    public final static int    MSG_DISCONNECT                 = 1;
    public final static int    SMSG_PUBLIC_KEY                = 2;
    public final static int    CMSG_SESSION_KEY               = 3;
    public final static int    CMSG_USER                      = 4;
    public final static int    CMSG_AUTH_RHOSTS               = 5;
    public final static int    CMSG_AUTH_RSA                  = 6;
    public final static int    SMSG_AUTH_RSA_CHALLENGE        = 7;
    public final static int    CMSG_AUTH_RSA_RESPONSE         = 8;
    public final static int    CMSG_AUTH_PASSWORD             = 9;
    public final static int    CMSG_REQUEST_PTY               = 10;
    public final static int    CMSG_WINDOW_SIZE               = 11;
    public final static int    CMSG_EXEC_SHELL                = 12;
    public final static int    CMSG_EXEC_CMD                  = 13;
    public final static int    SMSG_SUCCESS                   = 14;
    public final static int    SMSG_FAILURE                   = 15;
    public final static int    CMSG_STDIN_DATA                = 16;
    public final static int    SMSG_STDOUT_DATA               = 17;
    public final static int    SMSG_STDERR_DATA               = 18;
    public final static int    CMSG_EOF                       = 19;
    public final static int    SMSG_EXITSTATUS                = 20;
    public final static int    MSG_CHANNEL_OPEN_CONFIRMATION  = 21;
    public final static int    MSG_CHANNEL_OPEN_FAILURE       = 22;
    public final static int    MSG_CHANNEL_DATA               = 23;
    public final static int    MSG_CHANNEL_CLOSE              = 24;
    public final static int    MSG_CHANNEL_CLOSE_CONFIRMATION = 25;
    public final static int    MSG_CHANNEL_INPUT_EOF          = 24;
    public final static int    MSG_CHANNEL_OUTPUT_CLOSED      = 25;
    // OBSOLETE                CMSG_X11_REQUEST_FORWARDING    = 26;
    public final static int    SMSG_X11_OPEN                  = 27;
    public final static int    CMSG_PORT_FORWARD_REQUEST      = 28;
    public final static int    MSG_PORT_OPEN                  = 29;
    public final static int    CMSG_AGENT_REQUEST_FORWARDING  = 30;
    public final static int    SMSG_AGENT_OPEN                = 31;
    public final static int    MSG_IGNORE                     = 32;
    public final static int    CMSG_EXIT_CONFIRMATION         = 33;
    public final static int    CMSG_X11_REQUEST_FORWARDING    = 34;
    public final static int    CMSG_AUTH_RHOSTS_RSA           = 35;
    public final static int    MSG_DEBUG                      = 36;
    public final static int    CMSG_REQUEST_COMPRESSION       = 37;
    public final static int    CMSG_MAX_PACKET_SIZE           = 38;
    public final static int    CMSG_AUTH_TIS                  = 39;
    public final static int    SMSG_AUTH_TIS_CHALLENGE        = 40;
    public final static int    CMSG_AUTH_TIS_RESPONSE         = 41;

    public final static int    CMSG_AUTH_SDI                  = 16; // !!! OUCH
    public final static int    CMSG_ACM_OK                    = 64;
    public final static int    CMSG_ACM_ACCESS_DENIED         = 65;
    public final static int    CMSG_ACM_NEXT_CODE_REQUIRED    = 66;
    public final static int    CMSG_ACM_NEXT_CODE             = 67;
    public final static int    CMSG_ACM_NEW_PIN_REQUIRED      = 68;
    public final static int    CMSG_ACM_NEW_PIN_ACCEPTED      = 69;
    public final static int    CMSG_ACM_NEW_PIN_REJECTED      = 70;
    public final static int    CMSG_ACM_NEW_PIN               = 71;

    public final static int    IDX_CIPHER_CLASS = 0;
    public final static int    IDX_CIPHER_NAME  = 1;

    public final static String[][] cipherClasses = {
                { "SSHNoEncrypt", "none"         }, // No encryption
                { "SSHIDEA",      "idea-cbc"     }, // IDEA in CFB mode
                { "SSHDES",       "des-cbc"      }, // DES in CBC mode
                { "SSHDES3",      "3des-cbc"     }, // Triple-DES in CBC mode
                { null,           "tss"          }, // An experimental stream cipher
                { null,           "arcfour"      }, // RC4
                { "SSHBlowfish",  "blowfish-cbc" }, // Bruce Schneier's Blowfish
                { null,           "reserved"     }  // reserved
            };
    public final static int    CIPHER_INVALID  = -2;
    public final static int    CIPHER_ANY      = -1;
    public final static int    CIPHER_NONE     = 0; // No encryption
    public final static int    CIPHER_IDEA     = 1; // IDEA in CFB mode
    public final static int    CIPHER_DES      = 2; // DES in CBC mode
    public final static int    CIPHER_3DES     = 3; // Triple-DES in CBC mode
    public final static int    CIPHER_TSS      = 4; // An experimental stream cipher
    public final static int    CIPHER_RC4      = 5; // RC4
    public final static int    CIPHER_BLOWFISH = 6; // Bruce Schneier's Blowfish */
    public final static int    CIPHER_RESERVED = 7; // Reserved for 40 bit crippled encryption,
    // Bernard Perrot <perrot@lal.in2p3.fr>
    public final static int    CIPHER_NOTSUPPORTED = 8; // Indicates an unsupported cipher
    public final static int    CIPHER_DEFAULT  = CIPHER_BLOWFISH;

    public final static String[] authTypeDesc = {
        "_N/A_",
        "rhosts",
        "publickey",
        "password",
        "rhostsrsa",
        "tis",
        "kerberos",
        "kerbtgt",
        "securid",
        "cryptocard",
        "keyboard-interactive",
        "hostbased",
        "gssapi-with-mic"
    };

    public final static int    AUTH_RHOSTS       = 1;
    public final static int    AUTH_PUBLICKEY    = 2;
    public final static int    AUTH_PASSWORD     = 3;
    public final static int    AUTH_RHOSTS_RSA   = 4;
    public final static int    AUTH_TIS          = 5;
    public final static int    AUTH_KERBEROS     = 6;
    public final static int    PASS_KERBEROS_TGT = 7;

    public final static int    AUTH_SDI          = 8;
    public final static int    AUTH_CRYPTOCARD   = 9;
    public final static int    AUTH_KBDINTERACT  = 10;
    public final static int    AUTH_HOSTBASED    = 11;
    public final static int    AUTH_GSSAPI       = 12;

    public final static int    AUTH_NOTSUPPORTED = authTypeDesc.length;
    public final static int    AUTH_DEFAULT      = AUTH_PASSWORD;


    final static String[] proxyTypes = { "none", "http", "socks4",
                                         "socks5",
                                         "socks5-local-dns" };

    final static int[] defaultProxyPorts  = { 0, 8080, 1080, 1080, 1080 };


    public final static int    PROXY_NONE         = 0;
    public final static int    PROXY_HTTP         = 1;
    public final static int    PROXY_SOCKS4       = 2;
    public final static int    PROXY_SOCKS5_DNS   = 3;
    public final static int    PROXY_SOCKS5_IP    = 4;
    public final static int    PROXY_NOTSUPPORTED = proxyTypes.length;


    public final static int    TTY_OP_END	     = 0;
    public final static int    TTY_OP_ISPEED   = 192;
    public final static int    TTY_OP_OSPEED   = 193;

    // These are special "channels" not associated with a channel-number
    // in "SSH-sense".
    //
    public final static int    MAIN_CHAN_NUM    = -1;
    public final static int    CONNECT_CHAN_NUM = -2;
    public final static int    LISTEN_CHAN_NUM  = -3;
    public final static int    UNKNOWN_CHAN_NUM = -4;

    // Default name of file containing set of known hosts
    //
    public final static String KNOWN_HOSTS_FILE = "known_hosts";

    // When verifying the server's host-key to the set of known hosts, the
    // possible outcome is one of these.
    //
    public final static int SRV_HOSTKEY_KNOWN   = 0;
    public final static int SRV_HOSTKEY_NEW     = 1;
    public final static int SRV_HOSTKEY_CHANGED = 2;

    public SecureRandomAndPad secureRandom;

    //
    //
    protected byte[] sessionKey;
    protected byte[] sessionId;

    //
    //
    protected SSHCipher     sndCipher;
    protected SSHCipher     rcvCipher;
    protected SSHCompressor sndComp;
    protected SSHCompressor rcvComp;
    protected int           cipherType;

    // Server data fields
    //
    protected byte[]  srvCookie;
    protected RSAPublicKey srvServerKey;
    protected RSAPublicKey srvHostKey;
    protected int     protocolFlags;
    protected int     supportedCiphers;
    protected int     supportedAuthTypes;

    protected boolean isAnSSHClient = true;

    public final static String msgTypeString(int msgType) {
        switch (msgType) {
        case MSG_NONE:                       return "MSG_NONE";
        case MSG_DISCONNECT:                 return "MSG_DISCONNECT";
        case SMSG_PUBLIC_KEY:                return "SMSG_PUBLIC_KEY";
        case CMSG_SESSION_KEY:               return "CMSG_SESSION_KEY";
        case CMSG_USER:                      return "CMSG_USER";
        case CMSG_AUTH_RHOSTS:               return "CMSG_AUTH_RHOSTS";
        case CMSG_AUTH_RSA:                  return "CMSG_AUTH_RSA";
        case SMSG_AUTH_RSA_CHALLENGE:        return "SMSG_AUTH_RSA_CHALLENGE";
        case CMSG_AUTH_RSA_RESPONSE:         return "CMSG_AUTH_RSA_RESPONSE";
        case CMSG_AUTH_PASSWORD:             return "CMSG_AUTH_PASSWORD";
        case CMSG_REQUEST_PTY:               return "CMSG_REQUEST_PTY";
        case CMSG_WINDOW_SIZE:               return "CMSG_WINDOW_SIZE";
        case CMSG_EXEC_SHELL:                return "CMSG_EXEC_SHELL";
        case CMSG_EXEC_CMD:                  return "CMSG_EXEC_CMD";
        case SMSG_SUCCESS:                   return "SMSG_SUCCESS";
        case SMSG_FAILURE:                   return "SMSG_FAILURE";
        case CMSG_STDIN_DATA:                return "CMSG_STDIN_DATA";
        case SMSG_STDOUT_DATA:               return "SMSG_STDOUT_DATA";
        case SMSG_STDERR_DATA:               return "SMSG_STDERR_DATA";
        case CMSG_EOF:                       return "CMSG_EOF";
        case SMSG_EXITSTATUS:                return "SMSG_EXITSTATUS";
        case MSG_CHANNEL_OPEN_CONFIRMATION:  return "MSG_CHANNEL_OPEN_CONFIRMATION";
        case MSG_CHANNEL_OPEN_FAILURE:       return "MSG_CHANNEL_OPEN_FAILURE";
        case MSG_CHANNEL_DATA:               return "MSG_CHANNEL_DATA";
        case MSG_CHANNEL_CLOSE:              return "MSG_CHANNEL_CLOSE";
        case MSG_CHANNEL_CLOSE_CONFIRMATION: return "MSG_CHANNEL_CLOSE_CONFIRMATION";
        case SMSG_X11_OPEN:                  return "SMSG_X11_OPEN";
        case CMSG_PORT_FORWARD_REQUEST:      return "CMSG_PORT_FORWARD_REQUEST";
        case MSG_PORT_OPEN:                  return "MSG_PORT_OPEN";
        case CMSG_AGENT_REQUEST_FORWARDING:  return "CMSG_AGENT_REQUEST_FORWARDING";
        case SMSG_AGENT_OPEN:                return "SMSG_AGENT_OPEN";
        case MSG_IGNORE:                     return "MSG_IGNORE";
        case CMSG_EXIT_CONFIRMATION:         return "CMSG_EXIT_CONFIRMATION";
        case CMSG_X11_REQUEST_FORWARDING:    return "CMSG_X11_REQUEST_FORWARDING";
        case CMSG_AUTH_RHOSTS_RSA:           return "CMSG_AUTH_RHOSTS_RSA";
        case MSG_DEBUG:                      return "MSG_DEBUG";
        case CMSG_REQUEST_COMPRESSION:       return "CMSG_REQUEST_COMPRESSION";
        case CMSG_MAX_PACKET_SIZE:           return "CMSG_MAX_PACKET_SIZE";
        case CMSG_AUTH_TIS:                  return "CMSG_AUTH_TIS";
        case SMSG_AUTH_TIS_CHALLENGE:        return "SMSG_AUTH_TIS_CHALLENGE";
        case CMSG_AUTH_TIS_RESPONSE:         return "CMSG_AUTH_TIS_RESPONSE";
        case CMSG_ACM_OK:                    return "CMSG_ACM_OK";
        case CMSG_ACM_ACCESS_DENIED:         return "CMSG_ACM_ACCESS_DENIED";
        case CMSG_ACM_NEXT_CODE_REQUIRED:    return "CMSG_ACM_NEXT_CODE_REQUIRED";
        case CMSG_ACM_NEXT_CODE:             return "CMSG_ACM_NEXT_CODE";
        case CMSG_ACM_NEW_PIN_REQUIRED:      return "CMSG_ACM_NEW_PIN_REQUIRED";
        case CMSG_ACM_NEW_PIN_ACCEPTED:      return "CMSG_ACM_NEW_PIN_ACCEPTED";
        case CMSG_ACM_NEW_PIN_REJECTED:      return "CMSG_ACM_NEW_PIN_REJECTED";
        case CMSG_ACM_NEW_PIN:               return "CMSG_ACM_NEW_PIN";
        default:
            return "Unknown packet type " + msgType;
        }
    }

    public String getVersionId(boolean client) {
        String idStr = "SSH-" + SSH_VER_MAJOR + "." + SSH_VER_MINOR + "-";
        idStr += (client ? VER_MINDTERM : VER_MINDTUNL);
        if (com.mindbright.util.Crypto.isFipsMode())
            idStr += "_FIPS";
        return idStr;
    }

    public static String[] getProxyTypes() {
        return new String[] { "none", "http", "socks4", "socks5" };
    }

    public static int getProxyType(String typeName) throws IllegalArgumentException {
        int i;
        if("socks5-proxy-dns".equals(typeName)) {
            typeName = "socks5";
        }
        for(i = 0; i < proxyTypes.length; i++) {
            if(proxyTypes[i].equalsIgnoreCase(typeName))
                break;
        }
        if(i == PROXY_NOTSUPPORTED)
            throw new IllegalArgumentException("Proxytype " + typeName + " not supported");

        return i;
    }

    public static String getCipherName(int cipherType) {
        if (cipherType == CIPHER_ANY) return "any";
        return cipherClasses[cipherType][IDX_CIPHER_NAME];
    }

    public static int getCipherType(String cipherName) {
        if (cipherName.equals("any")) return CIPHER_ANY; 
        int i;
        if("blowfish".equals(cipherName)) {
            cipherName = "blowfish-cbc";
        } else if("rc4".equals(cipherName)) {
            cipherName = "arcfour";
        } else if("des".equals(cipherName)) {
            cipherName = "des-cbc";
        } else if("3des".equals(cipherName)) {
            cipherName = "3des-cbc";
        } else if("idea".equals(cipherName)) {
            cipherName = "idea-cbc";
        }
        for(i = 0; i < cipherClasses.length; i++) {
            String ciN = cipherClasses[i][IDX_CIPHER_NAME];
            if(ciN.equalsIgnoreCase(cipherName)) {
                if(cipherClasses[i][0] == null)
                    i = cipherClasses.length;
                break;
            }
        }
        return i;
    }

    public static String getAuthName(int authType) {
        return authTypeDesc[authType];
    }

    public static String getAltAuthName(int authType) {
        if(authType == AUTH_TIS || authType == AUTH_SDI ||
                authType == AUTH_CRYPTOCARD) {
            return "keyboard-interactive";
        }
        return getAuthName(authType);
    }

    public static int getAuthType(String authName) throws IllegalArgumentException {
        int i;
        if("sdi-token".equals(authName) || "secureid".equals(authName)) {
            authName = "securid";
        } else if("kbd-interact".equals(authName)) {
            authName = "keyboard-interactive";
        } else if("rsa".equals(authName)) {
            authName = "publickey";
        } else if("passwd".equals(authName)) {
            authName = "password";
        }
        for(i = 1; i < SSH.authTypeDesc.length; i++) {
            if(authTypeDesc[i].equalsIgnoreCase(authName))
                break;
        }
        if(i == AUTH_NOTSUPPORTED)
            throw new IllegalArgumentException("Authtype " + authName + " not supported");

        return i;
    }

    static int cntListSize(String authList) {
        int cnt = 1;
        int i   = 0, n;
        while(i < authList.length() && (n = authList.indexOf(',', i)) != -1) {
            i = n + 1;
            cnt++;
        }
        return cnt;
    }

    public static int[] getAuthTypes(String authList) throws IllegalArgumentException {
        int len = cntListSize(authList);
        int[] authTypes = new int[len];
        int r, l = 0;
        String type;

        for(int i = 0; i < len; i++) {
            r = authList.indexOf(',', l);
            if(r == -1)
                r = authList.length();
            type = authList.substring(l, r).trim();
            authTypes[i] = getAuthType(type);
            l = r + 1;
        }

        return authTypes;
    }

    protected int getSupportedCipher(int cipherType) {
        if (cipherType == CIPHER_ANY) {
            if (com.mindbright.util.Crypto.hasUnlimitedStrengthJCE() && 
		getSupportedCipher(CIPHER_BLOWFISH) == CIPHER_BLOWFISH)
                return CIPHER_BLOWFISH;
            if (getSupportedCipher(CIPHER_3DES) == CIPHER_3DES)
                return CIPHER_3DES;
            if (getSupportedCipher(CIPHER_DES) == CIPHER_DES)
                return CIPHER_DES;
            if (getSupportedCipher(CIPHER_RC4) == CIPHER_RC4)
                return CIPHER_RC4;
            return CIPHER_INVALID;
        }
        int cipherMask = (0x01 << cipherType);
        if((cipherMask & supportedCiphers) != 0)
            return cipherType;
        return CIPHER_INVALID;
    }

    protected boolean isAuthTypeSupported(int authType) {
        int authTypeMask = (0x01 << authType);
        if((authTypeMask & supportedAuthTypes) != 0)
            return true;
        return false;
    }

    protected boolean isProtocolFlagSet(int protFlag) {
        int protFlagMask = (0x01 << protFlag);
        if((protFlagMask & protocolFlags) != 0)
            return true;
        return false;
    }

    public synchronized SecureRandomAndPad secureRandom() {
		if (secureRandom == null)
			secureRandom = Crypto.getSecureRandomAndPad();
		return secureRandom;
	}

    public static void log(String msg) {
        if(DEBUG)
            System.out.println(msg);
    }

    public static void logExtra(String msg) {
        if(DEBUGMORE)
            System.out.println(msg);
    }

    public static void logDebug(String msg) {
        if(DEBUG)
            System.out.println(msg);
    }

    public static void logIgnore(SSHPduInputStream pdu) {
        if(DEBUG)
            System.out.println("MSG_IGNORE received...(len = " + pdu.length + ")");
    }

    void generateSessionId() throws IOException {
        byte[]        message;
        byte[]        srvKey = srvServerKey.getModulus().toByteArray();
        byte[]        hstKey = srvHostKey.getModulus().toByteArray();
        int           len = srvKey.length + hstKey.length + srvCookie.length;

        if(srvKey[0] == 0)
            len -= 1;
        if(hstKey[0] == 0)
            len -= 1;

        message = new byte[len];

        if(hstKey[0] == 0) {
            System.arraycopy(hstKey, 1, message, 0, hstKey.length - 1);
            len = hstKey.length - 1;
        } else {
            System.arraycopy(hstKey, 0, message, 0, hstKey.length);
            len = hstKey.length;
        }

        if(srvKey[0] == 0) {
            System.arraycopy(srvKey, 1, message, len, srvKey.length - 1);
            len += srvKey.length - 1;
        } else {
            System.arraycopy(srvKey, 0, message, len, srvKey.length);
            len += srvKey.length;
        }

        System.arraycopy(srvCookie, 0, message, len, srvCookie.length);

        try {
            MessageDigest md5;
            md5 = com.mindbright.util.Crypto.getMessageDigest("MD5");
            md5.update(message);
            sessionId = md5.digest();
        } catch(Exception e) {
            throw new IOException("MD5 not implemented, can't generate session-id");
        }
    }

    protected void initClientCipher() throws IOException {
        initCipher(false);
    }

    protected void initServerCipher() throws IOException {
        initCipher(true);
    }

    protected void initCipher(boolean server) throws IOException {
        sndCipher = SSHCipher.getInstance(cipherClasses[cipherType][0]);
        rcvCipher = SSHCipher.getInstance(cipherClasses[cipherType][0]);

        if(sndCipher == null) {
            throw new IOException("SSHCipher " + cipherClasses[cipherType][1] +
                                  " not found, can't use it");
        }
	try {
	    sndCipher.setKey(true, sessionKey);
	    rcvCipher.setKey(false, sessionKey);
	} catch (Throwable t) {
            throw new IOException("Failed to set cipher keys: " + t.getMessage());
	}
    }

    /* !!! USED FOR DEBUG !!!
    void printSrvKeys() {
      BigInteger big;
      byte[] theId = new byte[sessionId.length + 1];
      theId[0] = 0;
      System.arraycopy(sessionId, 0, theId, 1, sessionId.length);
      big = new BigInteger(theId);
      System.out.println("sessionId: " + big.toString(16));
      byte[] theKey = new byte[sessionKey.length + 1];
      theKey[0] = 0;
      System.arraycopy(sessionKey, 0, theKey, 1, sessionKey.length);
      big = new BigInteger(theKey);
      System.out.println("sessionkey: " + big.toString(16));

      System.out.println("srvkey n: " + ((RSAPublicKey)srvServerKey.getPublic()).getN().toString(16));
      System.out.println("srvkey e: " + ((RSAPublicKey)srvServerKey.getPublic()).getE().toString(16));
      System.out.println("srvkey bits: " +  ((RSAPublicKey)srvServerKey.getPublic()).bitLength());
      System.out.println("hstkey n: " + ((RSAPublicKey)srvHostKey.getPublic()).getN().toString(16));
      System.out.println("hstkey e: " + ((RSAPublicKey)srvHostKey.getPublic()).getE().toString(16));
      System.out.println("hstkey bits: " +  ((RSAPublicKey)srvHostKey.getPublic()).bitLength());
    }
    */

}

