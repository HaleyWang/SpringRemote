/*
 * TimeoutException.java
 *
 * Created on April 3, 2006, 11:43 AM
 */

package com.mindbright.sshcommon;

/**
 * Exception thrown when a command does not finish in the given period of time.
 * @author Constantino
 */
public class TimeoutException extends Exception {
	private static final long serialVersionUID = 1L;
    
    /**
     * Creates a new instance of TimeoutException
     */
    public TimeoutException() {
        super();
    }

    public TimeoutException(String message) {
        super(message);
    }
    
    public TimeoutException(String message, Throwable cause) {
        super(message,cause);
    }

    public TimeoutException(Throwable cause) {
        super(cause);
    }

}
