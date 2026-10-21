package com.example.modbus_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecuperatorSettingsDto {
    // Уставки рекуператора: значение регистра / 100, проценты
    private Double targetRpmMin;   // Air2Common: проценты; имя поля сохранено для совместимости API.
    private Double targetRpmMax;   // Air2Common: проценты; имя поля сохранено для совместимости API.
    private Double targetRpmStep;  // Air2Common: проценты; имя поля сохранено для совместимости API.
    private Integer trackingIntervalRecup; // 0x0511, секунды
}
