package com.haleywang.putty.view.puttypanel.connector;

import com.google.common.collect.Lists;
import com.jediterm.terminal.ProcessTtyConnector;
import com.pty4j.PtyProcess;
import com.pty4j.WinSize;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

/**
 * @author haley
 */
public class LocalTerminalConnector extends ProcessTtyConnector {
    private final PtyProcess myProcess;
    private final List<char[]> myDataChunks = Lists.newArrayList();


    public LocalTerminalConnector(PtyProcess process, Charset charset) {
        super(process, charset);
        myProcess = process;

    }

    @Override
    protected void resizeImmediately() {
        if (getPendingTermSize() != null && getPendingPixelSize() != null) {
            myProcess.setWinSize(
                    new WinSize(getPendingTermSize().width, getPendingTermSize().height, getPendingPixelSize().width, getPendingPixelSize().height));
        }
    }

    @Override
    public boolean isConnected() {
        return myProcess.isRunning();
    }

    @Override
    public String getName() {
        return "Local";
    }



    @Override
    public int read(char[] buf, int offset, int length) throws IOException {
        int len = super.read(buf, offset, length);
        if (len > 0) {
            char[] arr = Arrays.copyOfRange(buf, offset, len);
            myDataChunks.add(arr);
        }
        return len;
    }

    public List<char[]> getChunks() {
        return Lists.newArrayList(myDataChunks);
    }


}


