package com.haleywang.putty.view;

import com.haleywang.putty.dto.ConnectionDto;

public interface SideViewListener {


    void onTypedString(String command);

    void onCreateConnectionsTab(ConnectionDto connectionDto, String connectionPassword);
}
