package com.haleywang.putty.service.action.strategy;


import com.haleywang.putty.dto.Action;
import com.haleywang.putty.service.action.ActionStrategy;
import com.haleywang.putty.service.action.ActionsData;
import com.haleywang.putty.view.SpringRemoteView;

public class TermPanelActionStrategy implements ActionStrategy {
    @Override
    public boolean execute(Action action) {

        int index = ActionsData.getIndex(ActionsData.getTermViewActionsData(), action.getAction());

        SpringRemoteView.getInstance().changeCurrentTabPanel(index);

        return true;
    }
}
