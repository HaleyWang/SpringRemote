package com.haleywang.putty.dto;

import java.io.Serializable;

/**
 * @author haley
 */
public class AccountDto implements Serializable {
    private String name;
    private String password;
    private String pem;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPem() {
        return pem;
    }

    public void setPem(String pem) {
        this.pem = pem;
    }
}
