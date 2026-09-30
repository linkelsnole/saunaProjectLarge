package com.example.modbus_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VolumeSettingsDto {
    private Double ecoAirflow;
    private Double normalAirflow;
    private Double intensiveAirflow;
    private Double manualAirflow;
    private Double boostPercent;
}
