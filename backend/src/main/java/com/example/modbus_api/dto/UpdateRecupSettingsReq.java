package com.example.modbus_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRecupSettingsReq {
    private Double targetRpmMin;   // Минимальные обороты (об/мин)
    private Double targetRpmMax;   // Максимальные обороты (об/мин)
    private Double targetRpmStep;  // Шаг калибровки (об/мин)
    /** 0x0511 интервал опроса счётчика оборотов, секунды */
    private Integer trackingIntervalRecup;
}

