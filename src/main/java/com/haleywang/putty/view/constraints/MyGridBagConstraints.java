package com.haleywang.putty.view.constraints;

import java.awt.GridBagConstraints;

public class MyGridBagConstraints extends GridBagConstraints {

    public MyGridBagConstraints() {
        this.fill = GridBagConstraints.HORIZONTAL;
    }

    public MyGridBagConstraints ofGridx(int x) {
        this.gridx = x;
        return this;
    }

    public MyGridBagConstraints ofGridy(int y) {
        this.gridy = y;
        return this;
    }

    public MyGridBagConstraints ofGridwidth(int w) {
        this.gridwidth = w;
        return this;
    }

    public MyGridBagConstraints ofWeightx(int weightx) {
        this.weightx = weightx;
        return this;
    }

    public MyGridBagConstraints ofWeighty(int weighty) {
        this.weighty = weighty;
        return this;
    }

    public MyGridBagConstraints of(GridConstraints gc) {
        return ofGridx(gc.getGridx()).ofGridy(gc.getGridy())
                .ofWeightx(gc.getWeightx())
                .ofWeighty(gc.getWeighty())
                .ofGridwidth(gc.getGridwidth());
    }
}
