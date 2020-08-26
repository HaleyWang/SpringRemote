package com.haleywang.putty.view.side.subview;

import com.haleywang.putty.dto.CommandDto;
import com.haleywang.putty.dto.ConnectionDto;
import com.haleywang.putty.util.StringUtils;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.Component;


/**
 * @author haley
 */
public class MyTreeCellRenderer extends DefaultTreeCellRenderer {

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

        if (value instanceof DefaultMutableTreeNode) {

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            Object userValue = node.getUserObject();

            if (userValue instanceof CommandDto) {
                CommandDto commandDto = (CommandDto) userValue;
                if (commandDto.getChildrenCount() == 0) {
                    setText(commandDto.getName(), commandDto.getCommand());

                } else {
                    setText(value.toString());

                }
            } else if (userValue instanceof ConnectionDto) {
                ConnectionDto connectionDto = (ConnectionDto) userValue;
                if (connectionDto.getChildrenCount() == 0) {
                    setText(connectionDto.getName(), connectionDto.getHost());

                } else {
                    setText(value.toString());

                }
            } else {
                setText(value.toString());

            }
        }
        return this;
    }

    private void setText(String name, String host) {
        if (StringUtils.isAnyBlank(name, host)) {
            setText("<html><div style=\"min-height:20px;padding:4px 0\">" + StringUtils.ifBlank(name, host) + "</div></html>");

        } else {
            setText("<html><div style=\"min-height:22px;padding:1px 0\"><span>" + name + "</span><br/> <span style=\"color:#888888;\">" + host + "</span></div></html>");

        }
    }
}