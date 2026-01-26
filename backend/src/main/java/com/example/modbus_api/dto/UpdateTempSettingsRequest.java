package com.example.modbus_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTempSettingsRequest {
    private Double targetZone1;
    private Double targetZone2;
    private Double waterWarnThreshold;
    private Double waterFailThreshold;
    private Double inflowWarnThreshold;
    private Double inflowFailThreshold;
    private Double neutralZoneT;
    private Double hotValveMinProc;
    private Double waterMinTNoWork;
    private Double waterMinTWork;
}

