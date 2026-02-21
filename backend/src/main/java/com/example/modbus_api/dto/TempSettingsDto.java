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
    // 0-Off, 1-NeutralZone, 2-NZFromHeat, 3-NZFromCool, 4-NeedToCool, 5-Heating, 6-Average
    private Integer ctrlModeZone1;
    private Integer ctrlModeZone2;

    private Double neutralZoneT;
    private Double hotValveMinProc;
    private Double waterMinTNoWork;
    private Double waterMinTWork;
    private Double radTempFail;
}

