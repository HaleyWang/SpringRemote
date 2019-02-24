package com.haleywang.putty.dto;

import java.util.List;

public class GroupDto<T> implements GroupItem {

    private String name;

    private List<T> children;



    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<T> getChildren() {
        return children;
    }

    public void setChildren(List<T> children) {
        this.children = children;
    }

    @Override
    public String toString() {
        return getName();
    }
}
