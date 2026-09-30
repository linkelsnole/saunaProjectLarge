package com.example.modbus_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVolumeSettingsRequest {
    private Double ecoAirflow;
    private Double normalAirflow;
    private Double intensiveAirflow;
    private Double manualAirflow;
    private Double boostPercent;

    public boolean hasPresetValues() {
        return ecoAirflow != null || normalAirflow != null || intensiveAirflow != null;
    }

    public boolean isEmpty() {
        return !hasPresetValues() && manualAirflow == null && boostPercent == null;
    }
}
