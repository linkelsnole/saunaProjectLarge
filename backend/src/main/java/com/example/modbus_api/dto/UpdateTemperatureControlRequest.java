package com.example.modbus_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTemperatureControlRequest {
    private Integer zone1Mode;
    private Integer zone2Mode;
}
