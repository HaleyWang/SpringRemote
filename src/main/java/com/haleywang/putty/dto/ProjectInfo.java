package com.haleywang.putty.dto;

import java.io.Serializable;


/**
 * @author haley
 * archivesBaseName=SpringRemote
 * version=0.1
 */
public class ProjectInfo implements Serializable {
    private String archivesBaseName;
    private String version;

    public ProjectInfo() {
    }

    public ProjectInfo(String version, String archivesBaseName) {
        this.version = version;
        this.archivesBaseName = archivesBaseName;
    }

    public String getArchivesBaseName() {
        return archivesBaseName;
    }

    public void setArchivesBaseName(String archivesBaseName) {
        this.archivesBaseName = archivesBaseName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
