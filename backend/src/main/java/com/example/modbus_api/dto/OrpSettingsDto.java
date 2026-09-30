package com.example.modbus_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrpSettingsDto {
    private Double targetT1;
    private Double currentT1;
    private Double targetT2;
    private Double currentT2;
    private Double targetT3;
    private Double currentT3;

    public boolean isEmpty() {
        return targetT1 == null && currentT1 == null
            && targetT2 == null && currentT2 == null
            && targetT3 == null && currentT3 == null;
    }
}
