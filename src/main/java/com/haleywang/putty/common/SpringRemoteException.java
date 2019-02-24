package com.haleywang.putty.common;

public class SpringRemoteException extends RuntimeException {

    public SpringRemoteException() {
        super();
    }


    public SpringRemoteException(String message) {
        super(message);
    }


    public SpringRemoteException(String message, Throwable cause) {
        super(message, cause);
    }


    public SpringRemoteException(Throwable cause) {
        super(cause);
    }

}
