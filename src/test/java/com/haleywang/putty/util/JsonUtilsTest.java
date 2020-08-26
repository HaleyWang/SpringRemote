package com.haleywang.putty.util;

import com.haleywang.putty.dto.ConnectionDto;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

public class JsonUtilsTest {

    @Test
    public void fromJson() {

        ConnectionDto dto = new ConnectionDto();
        dto.setName("1");
        dto.setPem("/home/a.pem");

        ConnectionDto dto2 = JsonUtils.fromJson(JsonUtils.toJson(dto), ConnectionDto.class);

        Assert.assertEquals(dto.getPem(), dto2.getPem());
        Assert.assertEquals(dto.getName(), dto2.getName());
    }
}