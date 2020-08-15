package com.haleywang.putty.view;

import com.haleywang.putty.dto.ProjectInfo;
import com.haleywang.putty.view.constraints.MyGridBagConstraints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;


/**
 * @author haley
 */
public class HelpDialog extends JDialog {

    private static final Logger LOGGER = LoggerFactory.getLogger(HelpDialog.class);

    private static final String TILE = "Help";

    public HelpDialog(SpringRemoteView omegaRemote) {
        super(omegaRemote, TILE, true);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        MyGridBagConstraints cs = new MyGridBagConstraints();
        cs.insets = new Insets(6, 6, 6, 6);

        LOGGER.info("getImplementationVersion: {}", getClass().getPackage().getImplementationVersion());
        ProjectInfo projectInfo = getProperties();

        String helpUrl = "https://github.com/HaleyWang/SpringRemote";

        JLabel linklabel = new JLabel("<html> Help: <a href='" + helpUrl + "'>" + helpUrl + "</a></html>");
        linklabel.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop desktop = Desktop.getDesktop();
                    desktop.browse(new URI(helpUrl));
                } catch (IOException | URISyntaxException ex) {
                    LOGGER.error("open url error", ex);
                }
            }
        });

        cs.ofWeightx(1).ofGridy(0).ofGridx(0);
        panel.add(new JLabel(projectInfo.getArchivesBaseName() + " " + projectInfo.getVersion()), cs);
        cs.ofWeightx(1).ofGridy(1).ofGridx(0);
        panel.add(linklabel, cs);

        panel.setBorder(new LineBorder(Color.GRAY));

        getContentPane().add(panel, BorderLayout.CENTER);

        pack();
        setResizable(false);
        setLocationRelativeTo(omegaRemote);
    }

    private static ProjectInfo getProperties() {
        Properties prop = new Properties();

        try (InputStream in = Object.class.getResourceAsStream("/version.properties")) {
            prop.load(in);
            String version = prop.getProperty("version").trim();
            String archivesBaseName = prop.getProperty("archivesBaseName").trim();
            return new ProjectInfo(version, archivesBaseName);
        } catch (Exception e) {
            LOGGER.error("getPropertiesError", e);
            return new ProjectInfo("0.1", "SpringRemote");
        }
    }

}