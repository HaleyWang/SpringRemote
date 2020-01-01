package com.haleywang.putty.dto;

import java.io.Serializable;

/**
 * @author haley
 */
public class Status implements Serializable {
    private boolean success;
    private String msg;

    public static Status fail(String msg) {

        Status s = new Status();
        s.setMsg(msg);
        s.setSuccess(false);

        return s;

    }

    public static Status ok() {
        return ok(null);
    }

    public static Status ok(String msg) {

        Status s = new Status();
        s.setMsg(msg);
        s.setSuccess(true);

        return s;

    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
