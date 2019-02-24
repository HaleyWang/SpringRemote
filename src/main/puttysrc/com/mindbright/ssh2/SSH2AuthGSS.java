/******************************************************************************
 *
 * Copyright (c) 2007-2011 Cryptzone Group AB. All Rights Reserved.
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

import com.mindbright.util.Log;

import java.net.InetAddress;

import java.util.Hashtable;

import javax.naming.*;
import javax.naming.directory.*;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import org.ietf.jgss.*;

/**
 * This class implements a module for GSS API user authentication as defined in 
 * RFC 4462. It only works with cached tickets.
 *
 * See also: http://java.sun.com/j2se/1.4.2/docs/guide/security/jgss/single-signon.html
 *
 * Depends on java properties:
 *
 *   java.security.krb5.conf
 *   java.security.krb5.realm
 *   java.security.krb5.kdc
 *
 * @see SSH2AuthModule
 */
public class SSH2AuthGSS implements SSH2AuthModule, java.security.PrivilegedAction<SSH2TransportPDU> {

    public final static String STANDARD_NAME = "gssapi-with-mic";

    private static Oid OID_KRBv5  = null;

    private String realm, kdc, host;
    private boolean isinit = false;
    private byte[] token = null;

    private SSH2UserAuth userauth;    
    private SSH2FatalException saved_exc;

    private GSSContext gssctx;
    private LoginContext loginctx;
    private boolean dodispose = false;

    private String dnsfail;

    private Log log;

    {
        try {
            OID_KRBv5 = new Oid("1.2.840.113554.1.2.2");    
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
        
    public SSH2AuthGSS() {
        this(null, null);
    }

    public SSH2AuthGSS(String realm, String kdc) {
        super();

        this.realm = realm;
        this.kdc = kdc;
    }

    private static String getHostPortFromAttr(Attributes attrs) throws NamingException {
        if (attrs == null)
            return null;

        for (NamingEnumeration<?> ae = attrs.getAll(); ae.hasMore(); ) {
            final Attribute attr = (Attribute)ae.next();
            
            for (NamingEnumeration<?> e = attr.getAll(); e.hasMore(); ) {
                Object a = e.next();

                if (a instanceof String) {
                    String[] rec = com.mindbright.ssh2.SSH2ListUtil.arrayFromList((String)a, " ");
                    if (rec != null && rec.length == 4) 
                        return rec[3]+":"+rec[2];
                }
            }
        }

        return null;
    }

    private static String lookupsrv(String name) throws NamingException {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY,
                "com.sun.jndi.dns.DnsContextFactory");
        env.put(Context.AUTHORITATIVE, "true");

        DirContext ictx = new InitialDirContext(env);
        
        final String[] srv = { "SRV" };
        
        return getHostPortFromAttr(ictx.getAttributes(name, srv));
    }
    
    private String getRemoteName() {
        String remote = userauth.getTransport().getRemoteHostName();
        if (-1 != remote.indexOf('.')) {
            return remote;
        }
        try {
            InetAddress addr = InetAddress.getByAddress(
                userauth.getTransport().getRemoteAddress());
            return addr.getHostName();
        } catch (Exception e) {
            return userauth.getTransport().getRemoteHostName();
        }
    }

    private void init() {

        if (isinit) return;
        isinit = true;

        log = userauth.getTransport().getLog();

        if (realm == null) 
            realm = System.getProperty("java.security.krb5.realm");

        host = getRemoteName();
        if (realm == null) {
            int idx = host.indexOf('.');
            if (idx != -1)
                realm = host.substring(idx+1);
        }

        if (realm != null && kdc == null) {
            // Try to look up KDC from DNS SRV records
            String name = "_kerberos._udp."+realm;
            try {
                kdc = lookupsrv(name);
            } catch (Throwable t) {
                dnsfail = name;
                t.printStackTrace();
            }
        }
        
        if (realm != null && kdc != null) {
            try {
                System.setProperty("java.security.krb5.realm", realm.toUpperCase());
                System.setProperty("java.security.krb5.kdc", kdc.toUpperCase());
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        try { 
            java.net.URL url = SSH2AuthGSS.class.getClassLoader().getResource("defaults/jaas.conf");
            System.setProperty("java.security.auth.login.config", url.toExternalForm());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public String getStandardName() {
        return STANDARD_NAME;
    }

    // PrivilegedAction 
    public SSH2TransportPDU run() {
        try {            
            if (dodispose) {
                gssctx.dispose();
                dodispose = false;
                return null;
            }

            if (gssctx == null) {
                GSSManager gssmgr = GSSManager.getInstance();

                String realm = System.getProperty("java.security.krb5.realm");
                if (realm != null && !realm.equals(""))
                    realm = "@" + realm.toUpperCase();
                
                GSSName userName = gssmgr.createName
                    (userauth.user + (realm != null ? realm : ""), null); 
                GSSName serverName = gssmgr.createName
                    ("host/" + host + (realm != null ? realm : ""), null);

                log.debug2("SSH2AuthGSS", "run", "  userName: " + userName);
                log.debug2("SSH2AuthGSS", "run", "serverName: " + serverName);

                // create credentials and a context
                GSSCredential cred = gssmgr.createCredential
                    (userName, GSSCredential.DEFAULT_LIFETIME, OID_KRBv5, GSSCredential.INITIATE_ONLY);
                
                gssctx = gssmgr.createContext
                    (serverName, OID_KRBv5, cred, GSSContext.DEFAULT_LIFETIME);
                
                // set flags based on RFC 
                gssctx.requestInteg(true);         // MUST
                gssctx.requestCredDeleg(true);     // MAY
                gssctx.requestMutualAuth(true);    // RFC says SHOULD NOT, but that doesn't work with OpenSSH
                gssctx.requestReplayDet(false);    // SHOULD NOT
                gssctx.requestSequenceDet(false);  // SHOULD NOT
            }

            SSH2TransportPDU pdu = null;

            if (!gssctx.isEstablished()) {
                if (token == null) 
                    token = new byte[0];

                token = gssctx.initSecContext(token, 0, token.length);
                if (token != null) {
                    pdu = SSH2TransportPDU.createOutgoingPacket(SSH2.MSG_USERAUTH_GSSAPI_TOKEN);
                    pdu.writeString(token);
                    if (!gssctx.isEstablished()) return pdu;
                    userauth.getTransport().transmit(pdu);
                } else if (!gssctx.isEstablished()) {
                    gssctx.dispose();
                    gssctx = null;
                    loginctx = null;
                    saved_exc = new SSH2FatalException("GSS API authentication - failed to initialize token");
                    return null;                    
                }
            }

            if (!gssctx.getIntegState()) {
                pdu = SSH2TransportPDU.createOutgoingPacket(SSH2.MSG_USERAUTH_GSSAPI_EXCHANGE_COMPLETE);
            } else {
                SSH2DataBuffer data = new SSH2DataBuffer(128);
                data.writeRaw(userauth.getTransport().getSessionId());
                data.writeByte((byte)SSH2.MSG_USERAUTH_REQUEST);
                data.writeString(userauth.user);
                data.writeString("ssh-connection");
                data.writeString(STANDARD_NAME);
                
                pdu = SSH2TransportPDU.createOutgoingPacket(SSH2.MSG_USERAUTH_GSSAPI_MIC);
                pdu.writeString(gssctx.getMIC(data.getData(), 0, data.getWPos(), new MessageProp(0, true))); 
            }
            
            gssctx.dispose();
            gssctx = null;
            loginctx = null;
            return pdu;
            
        } catch (GSSException gse) {
            gse.printStackTrace();
            String msg = "failed to initialize GSS context";
            if (dnsfail != null) {
                msg += "\nThis could be because DNS lookup failed for " +
                    dnsfail;
            }
            saved_exc = new SSH2FatalException(msg);
            if (gssctx != null)
                try { gssctx.dispose(); } catch (Throwable t) {}
            gssctx = null;
            loginctx = null;
        }
        return null;
    }

    public SSH2TransportPDU processMethodMessage(SSH2UserAuth userAuth,
                                                 SSH2TransportPDU pdu)
    throws SSH2Exception {
        saved_exc = null;
        
        log.debug2("SSH2AuthGSS", "processMethodMessage",
                   "processing " + pdu.getType());
        switch(pdu.getType()) {
        case SSH2.MSG_USERAUTH_GSSAPI_ERRTOK:
        case SSH2.MSG_USERAUTH_GSSAPI_TOKEN:
            token = pdu.readString();
            userauth = userAuth;
            pdu = Subject.doAs(loginctx.getSubject(), this);
            break;

        case SSH2.MSG_USERAUTH_GSSAPI_ERROR:
            dodispose = true;
            Subject.doAs(loginctx.getSubject(), this);
            gssctx = null;
            loginctx = null;
            throw new SSH2FatalException("GSS API authentication failed");

        case SSH2.MSG_USERAUTH_GSSAPI_RESPONSE:
            // byte     SSH_MSG_USERAUTH_GSSAPI_RESPONSE
            // string   selected mechanism OID

            try {
                byte[] oid = pdu.readString();
                if (oid == null || !java.util.Arrays.equals(oid, OID_KRBv5.getDER()))
                    throw new SSH2FatalException("GSS API authentication - received unexpected OID");

                // Do the JAAS login, which in turn uses it's cfg file to specify
                // how this is done - in our case it should the Krb5LoginModule
                // with 'useTicketCache=true'
                try {
                    loginctx = new LoginContext("MindTerm");
                    loginctx.login();
                } catch (Exception e){
                    e.printStackTrace();
                }

                userauth = userAuth;
                pdu = Subject.doAs(loginctx.getSubject(), this);
            } catch (SSH2FatalException fe) {
                loginctx = null;
                gssctx = null;
                saved_exc = fe;            
            } catch (Throwable t) {
                loginctx = null;
                gssctx = null;
                t.printStackTrace();
                throw new SSH2FatalException("GSS API authentication - failed to initialize GSS context");
            }
            
            break;

        default:
        	String msg = "received unexpected packet of type: " +pdu.getType();
            log.warning("SSH2AuthGSS", msg);
            pdu = null;
            throw new SSH2FatalException("SSH2AuthGSS: got unexpected " + msg);
        }
        
        if (saved_exc != null)
            throw saved_exc;
        
        return pdu;
    }

    public SSH2TransportPDU startAuthentication(SSH2UserAuth userAuth)
        throws SSH2Exception {
        try {
            userauth = userAuth;
            init();
            log.debug("SSH2AuthGSS", "Starting Kerberos authentication");
            log.debug("SSH2AuthGSS", "  realm: " + realm);
            log.debug("SSH2AuthGSS", "    kdc: " + kdc);
            return createRequest(userAuth);
        } catch (Throwable t) {
            t.printStackTrace();
            throw new SSH2FatalException("SSH2AuthGSS: got exception: " + t);
        }
    }

    private SSH2TransportPDU createRequest(SSH2UserAuth userAuth) throws GSSException {
        SSH2TransportPDU pdu = userAuth.createUserAuthRequest(STANDARD_NAME);
        pdu.writeInt(1);
        pdu.writeString(OID_KRBv5.getDER());
        return pdu;
    }

    public void clearSensitiveData() {
        gssctx   = null;
        loginctx = null;
    }

    public boolean retryPointless() {
        return true;
    }
}
