package com.jediterm.ssh;

import com.jediterm.ssh.jsch.JSchShellTtyConnector;
import com.jediterm.terminal.TtyConnector;
import com.jediterm.terminal.ui.AbstractTerminalFrame;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * @author traff
 */
public class SshMain2 extends AbstractTerminalFrame {

  public static void main(final String[] arg) {
    BasicConfigurator.configure();
    Logger.getRootLogger().setLevel(Level.INFO);
    new SshMain2();
  }

  @Override
  public TtyConnector createTtyConnector() {
    return new JSchShellTtyConnector("47.88.13.53", "root" , "");
  }

}
