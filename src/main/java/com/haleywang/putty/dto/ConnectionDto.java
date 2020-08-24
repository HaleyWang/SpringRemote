package com.haleywang.putty.dto;

import com.haleywang.putty.service.action.ActionCategoryEnum;
import com.haleywang.putty.util.StringUtils;

import java.io.Serializable;

/**
 * @author haley
 */
public class ConnectionDto extends GroupDto<ConnectionDto> implements Action, Serializable {

    private String host;
    private String user;
    private String port;
    private String pem;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    @Override
    public String toString() {

        return StringUtils.ifBlank(getName(), getHost());
    }

    @Override
    public String getAction() {
        return getHost();
    }

    @Override
    public String getKeyMap() {
        return null;
    }

    @Override
    public ActionCategoryEnum getCategory() {
        return ActionCategoryEnum.SSH;
    }

    public String getPem() {
        return pem;
    }

    public void setPem(String pem) {
        this.pem = pem;
    }
}
