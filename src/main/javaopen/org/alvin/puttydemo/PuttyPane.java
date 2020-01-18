package org.alvin.puttydemo;

public interface PuttyPane {
    void init();

    void typedString(String command);

    void setTermFocus();

    void close();
}
