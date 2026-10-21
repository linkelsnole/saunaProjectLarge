package com.example.modbus_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRecupSettingsReq {
    private Double targetRpmMin;   // Air2Common: проценты; имя поля сохранено для совместимости API.
    private Double targetRpmMax;   // Air2Common: проценты; имя поля сохранено для совместимости API.
    private Double targetRpmStep;  // Air2Common: проценты; имя поля сохранено для совместимости API.
    /** 0x0511 интервал опроса счётчика оборотов, секунды */
    private Integer trackingIntervalRecup;
}

