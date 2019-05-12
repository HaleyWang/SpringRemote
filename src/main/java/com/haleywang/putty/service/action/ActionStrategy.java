package com.haleywang.putty.service.action;

import com.haleywang.putty.dto.Action;
import com.haleywang.putty.dto.ActionDto;

public interface ActionStrategy {
    boolean execute(Action action);
}
