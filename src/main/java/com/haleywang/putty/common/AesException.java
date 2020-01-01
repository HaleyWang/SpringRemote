package com.haleywang.putty.common;

/**
 * @author haley
 */
public class AesException extends SpringRemoteException {
    public AesException() {
        super();
    }


    public AesException(String message) {
        super(message);
    }


    public AesException(String message, Throwable cause) {
        super(message, cause);
    }


    public AesException(Throwable cause) {
        super(cause);
    }

}
