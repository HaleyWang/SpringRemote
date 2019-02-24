package com.haleywang.putty.common;

public class AESDecryptException extends SpringRemoteException {
    public AESDecryptException() {
        super();
    }


    public AESDecryptException(String message) {
        super(message);
    }


    public AESDecryptException(String message, Throwable cause) {
        super(message, cause);
    }


    public AESDecryptException(Throwable cause) {
        super(cause);
    }

}
