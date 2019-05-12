package com.haleywang.putty.service.action;

import com.haleywang.putty.dto.Action;

public interface ActionStrategy {
    boolean execute(Action action);
}
