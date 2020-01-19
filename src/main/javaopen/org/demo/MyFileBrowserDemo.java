package org.demo;

import com.haleywang.putty.view.MyFileBrowser;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import javax.swing.JButton;
import javax.swing.JFrame;
import java.awt.BorderLayout;

public class MyFileBrowserDemo {


    public static void main(String[] args) throws SftpException, JSchException {
        JFrame jf = new JFrame();

        MyFileBrowser myFileBrowser = new MyFileBrowser("Remote file browser", "/", p -> {
            System.out.println(p);
        });

        jf.getContentPane().setLayout(new BorderLayout());

        jf.setSize(640, 480);
        JButton a = new JButton("ls");
        a.addActionListener(e -> myFileBrowser.showOpenDialog());
        jf.getContentPane().add(a);

        JSch jsch = new JSch();
        Session session = null;


        String username = "haley";
        String hostname = "127.0.0.1";
        byte[] password = "".getBytes();

        session = jsch.getSession(username, hostname, 22);
        session.setConfig("StrictHostKeyChecking", "no");


        session.setPassword(password);
        session.connect();
        Channel channel = session.openChannel("sftp");
        channel.connect();
        ChannelSftp sftpChannel1 = (ChannelSftp) channel;

        myFileBrowser.setSftpChannel(sftpChannel1);

        jf.pack();
        jf.setVisible(true);

    }

}
