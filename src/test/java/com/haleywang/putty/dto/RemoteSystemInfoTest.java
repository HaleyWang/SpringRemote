package com.haleywang.putty.dto;

import com.haleywang.putty.util.IoTool;
import org.junit.Assert;
import org.junit.Test;

public class RemoteSystemInfoTest {

    @Test
    public void ofDiskUsageString() {

        RemoteSystemInfo remoteSystemInfo = new RemoteSystemInfo();
        remoteSystemInfo.ofDiskUsageString(IoTool.read(RemoteSystemInfoTest.class, "/dto/diskUsage.txt"));
        Assert.assertNotNull(remoteSystemInfo.getDiskUsageString());

    }

    @Test
    public void ofCpuUsageString() {
    }

    @Test
    public void ofMemoryUsageString() {
    }
}