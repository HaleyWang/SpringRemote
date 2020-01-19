package com.haleywang.putty.view.constraints;

import java.io.Serializable;

/**
 * @author haley
 */
public class GridConstraints implements Serializable {
    private int gridwidth;
    private int gridx;
    private int gridy;
    private int weightx;
    private int weighty;

    public GridConstraints ofGridwidth(int gridwidth) {
        setGridwidth(gridwidth);
        return this;
    }

    public GridConstraints ofGridx(int gridx) {
        setGridx(gridx);
        return this;
    }

    public GridConstraints ofGridy(int gridy) {
        setGridy(gridy);
        return this;
    }


    public int getGridwidth() {
        return gridwidth;
    }

    public void setGridwidth(int gridwidth) {
        this.gridwidth = gridwidth;
    }


    public int getGridx() {
        return gridx;
    }

    public void setGridx(int gridx) {
        this.gridx = gridx;
    }

    public int getGridy() {
        return gridy;
    }

    public void setGridy(int gridy) {
        this.gridy = gridy;
    }

    public int getWeightx() {
        return weightx;
    }

    public void setWeightx(int weightx) {
        this.weightx = weightx;
    }

    public int getWeighty() {
        return weighty;
    }

    public void setWeighty(int weighty) {
        this.weighty = weighty;
    }
}
