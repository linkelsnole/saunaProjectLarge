package com.example.modbus_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTemperatureSetpointsRequest {
    private Double outflowTarget;
    private Double inflowTargetZone1;
    private Double inflowTargetZone2;
    private OrpSettingsDto orpZone1;
    private OrpSettingsDto orpZone2;

    public boolean isEmpty() {
        return outflowTarget == null && inflowTargetZone1 == null && inflowTargetZone2 == null
            && (orpZone1 == null || orpZone1.isEmpty())
            && (orpZone2 == null || orpZone2.isEmpty());
    }
}
