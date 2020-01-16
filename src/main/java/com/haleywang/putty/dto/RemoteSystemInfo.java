package com.haleywang.putty.dto;

import java.io.Serializable;
import java.util.List;

public class RemoteSystemInfo implements Serializable {
    private String infoDate;

    private String diskUsageCmd = "df -hl";
    private String memoryUsageCmd = "free -m";
    private String cpuUsageCmd = "vmstat 1 1";

    private String diskUsageString;
    private String memoryUsageString;
    private String cpuUsageString;

    private int idleCpu;
    private int userCpu;
    private int systemCpu;

    private long totalMemory;
    private long usedMemory;
    private long freeMemory;
    private long sharedMemory;
    private long buffMemory;
    private long availableMemory;

    private List<DiskUsage> diskUsages;

    public String getInfoDate() {
        return infoDate;
    }

    public void setInfoDate(String infoDate) {
        this.infoDate = infoDate;
    }

    public int getIdleCpu() {
        return idleCpu;
    }

    public void setIdleCpu(int idleCpu) {
        this.idleCpu = idleCpu;
    }

    public int getUserCpu() {
        return userCpu;
    }

    public void setUserCpu(int userCpu) {
        this.userCpu = userCpu;
    }

    public int getSystemCpu() {
        return systemCpu;
    }

    public void setSystemCpu(int systemCpu) {
        this.systemCpu = systemCpu;
    }

    public long getTotalMemory() {
        return totalMemory;
    }

    public void setTotalMemory(long totalMemory) {
        this.totalMemory = totalMemory;
    }

    public long getUsedMemory() {
        return usedMemory;
    }

    public void setUsedMemory(long usedMemory) {
        this.usedMemory = usedMemory;
    }

    public long getFreeMemory() {
        return freeMemory;
    }

    public void setFreeMemory(long freeMemory) {
        this.freeMemory = freeMemory;
    }

    public long getSharedMemory() {
        return sharedMemory;
    }

    public void setSharedMemory(long sharedMemory) {
        this.sharedMemory = sharedMemory;
    }

    public long getBuffMemory() {
        return buffMemory;
    }

    public void setBuffMemory(long buffMemory) {
        this.buffMemory = buffMemory;
    }

    public long getAvailableMemory() {
        return availableMemory;
    }

    public void setAvailableMemory(long availableMemory) {
        this.availableMemory = availableMemory;
    }

    public List<DiskUsage> getDiskUsages() {
        return diskUsages;
    }

    public void setDiskUsages(List<DiskUsage> diskUsages) {
        this.diskUsages = diskUsages;
    }

    public String getDiskUsageCmd() {
        return diskUsageCmd;
    }

    public void setDiskUsageCmd(String diskUsageCmd) {
        this.diskUsageCmd = diskUsageCmd;
    }

    public String getMemoryUsageCmd() {
        return memoryUsageCmd;
    }

    public void setMemoryUsageCmd(String memoryUsageCmd) {
        this.memoryUsageCmd = memoryUsageCmd;
    }

    public String getCpuUsageCmd() {
        return cpuUsageCmd;
    }

    public void setCpuUsageCmd(String cpuUsageCmd) {
        this.cpuUsageCmd = cpuUsageCmd;
    }

    public String getDiskUsageString() {
        return diskUsageString;
    }

    public void setDiskUsageString(String diskUsageString) {
        this.diskUsageString = diskUsageString;
    }

    public String getMemoryUsageString() {
        return memoryUsageString;
    }

    public void setMemoryUsageString(String memoryUsageString) {
        this.memoryUsageString = memoryUsageString;
    }

    public String getCpuUsageString() {
        return cpuUsageString;
    }

    public void setCpuUsageString(String cpuUsageString) {
        this.cpuUsageString = cpuUsageString;
    }

    public RemoteSystemInfo ofDiskUsageString(String diskUsageString) {
        setDiskUsageString(diskUsageString);

        return this;
    }

    public RemoteSystemInfo ofCpuUsageString(String cpuUsageString) {
        setCpuUsageString(cpuUsageString);

        return this;
    }

    public RemoteSystemInfo ofMemoryUsageString(String memoryUsageString) {
        setMemoryUsageString(memoryUsageString);

        return this;
    }

    public static class DiskUsage {
        private String size;
        private String used;
        private String avail;
        private int use;

        public String getSize() {
            return size;
        }

        public void setSize(String size) {
            this.size = size;
        }

        public String getUsed() {
            return used;
        }

        public void setUsed(String used) {
            this.used = used;
        }

        public String getAvail() {
            return avail;
        }

        public void setAvail(String avail) {
            this.avail = avail;
        }

        public int getUse() {
            return use;
        }

        public void setUse(int use) {
            this.use = use;
        }
    }

}
