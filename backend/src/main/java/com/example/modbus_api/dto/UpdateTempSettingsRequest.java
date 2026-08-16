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

    /**
     * Приоритет 0 (пакет 301): положение вентилей при запуске, регистр 0x00E1; сырое signed 16-bit, без REG_POWERPROC и без ×10.
     */
    private Integer valveStartupPosition;
    /**
     * Приоритет 0 (пакет 301): алгоритм управления температурой, регистр 0x00DD; сырое signed 16-bit, без REG_POWERPROC и без ×10.
     */
    private Integer controlAlgorithm;
    /**
     * Приоритет 0 (пакет 301): интервал слежения по температуре, регистр 0x007E; сырое signed 16-bit, без REG_POWERPROC и без ×10.
     */
    private Integer tempTrackingInterval;
}

