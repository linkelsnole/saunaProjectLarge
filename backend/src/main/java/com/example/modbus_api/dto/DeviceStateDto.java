package com.example.modbus_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeviceStateDto {
    private String id;
    private int baseAddress;
    private Integer alarmMode;
    private Integer alarmSeconds;
}
