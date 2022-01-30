package com.haleywang.putty.view;

import com.haleywang.putty.dto.TmpCommandsDto;
import com.haleywang.putty.storage.FileStorage;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.Color;


/**
 * @author haley
 */
public class CommandHistoryDialog extends JDialog {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandHistoryDialog.class);

    private static final String TILE = "Temp commands history";
    private static final long serialVersionUID = 6490851820619369412L;

    public CommandHistoryDialog(SpringRemoteView omegaRemote) {
        super(omegaRemote, TILE, true);
        this.setSize(600, 500);
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        LOGGER.info("getImplementationVersion: {}", getClass().getPackage().getImplementationVersion());

        StringBuilder sb = new StringBuilder(1024);

        TmpCommandsDto tmpCommandsDto = FileStorage.INSTANCE.getTmpCommandsJson();
        int size = tmpCommandsDto.getCommands().size();
        for(int i = size-1; i >=0; i--) {
            sb.append("\n").append(tmpCommandsDto.getCommands().get(i))
                    .append("\n\n#--------------------------#\n");

        }

        CommandEditor commandTextArea = new CommandEditor();
        RTextScrollPane sp = new RTextScrollPane(commandTextArea);
        panel.add(sp, BorderLayout.CENTER);
        commandTextArea.setText(sb.toString());

        panel.setBorder(new LineBorder(Color.GRAY));
        getContentPane().add(panel, BorderLayout.CENTER);

        //pack
        setResizable(true);
        setLocationRelativeTo(omegaRemote);
    }
}