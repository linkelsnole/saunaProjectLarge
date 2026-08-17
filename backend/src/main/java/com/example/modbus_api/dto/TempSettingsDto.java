package com.example.modbus_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TempSettingsDto {
    // Целевые температуры зон (в градусах Цельсия)
    private Double targetZone1;
    private Double targetZone2;
    
    // Пороги температуры воды (в градусах Цельсия, делятся на 10)
    private Double waterWarnThreshold;
    private Double waterFailThreshold;
    
    // Пороги температуры ПВ (в градусах Цельсия, делятся на 10)
    private Double inflowWarnThreshold;
    private Double inflowFailThreshold;
    
    // Режимы управления (только чтение):
    // 0-Off, 1-NeutralZone, 2-NZFromHeat, 3-NZFromCool, 4-NeedToCool, 5-Heating, 6-Average,
    // 7-Hood, 8-Alg2Step0, 9-Alg2Driving, 10-Alg2Stabilise, 11-Alg2HeatOff, 12-Alg2HeatInsuff
    private Integer ctrlModeZone1;
    private Integer ctrlModeZone2;

    private Double neutralZoneT;
    private Double hotValveMinProc;
    private Double waterMinTNoWork;
    private Double waterMinTWork;
    private Double radTempFail;

    // п.1: 0x00E1 — положение вентилей при запуске
    private Integer valveStartupPosition;
    // п.2: 0x00DD — алгоритм управления
    private Integer controlAlgorithm;
    // п.3: 0x007E — интервал слежения по T
    private Integer tempTrackingInterval;
}

