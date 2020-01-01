package com.haleywang.putty.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author haley
 */
public class GroupDto<T> implements GroupItem, Serializable {

    private String name;

    private ArrayList<T> children;


    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<T> getChildren() {
        return children;
    }

    public void setChildren(ArrayList<T> children) {
        this.children = children;
    }

    @Override
    public String toString() {
        return getName();
    }

    public int getChildrenCount() {
        return children == null ? 0 : children.size();
    }
}
