package com.haleywang.putty.common;

public class AESException extends SpringRemoteException {
    public AESException() {
        super();
    }


    public AESException(String message) {
        super(message);
    }


    public AESException(String message, Throwable cause) {
        super(message, cause);
    }


    public AESException(Throwable cause) {
        super(cause);
    }

}
