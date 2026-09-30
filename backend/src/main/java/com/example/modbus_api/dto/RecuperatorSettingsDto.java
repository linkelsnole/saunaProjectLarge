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
    // Уставки рекуператора: Power=-2, значение регистра / 100
    private Double targetRpmMin;   // Минимальные обороты (об/мин) - сейчас 0 или 0.5 оборотов
    private Double targetRpmMax;   // Максимальные обороты (об/мин) - сейчас от 1 до 7 оборотов может стоять
    private Double targetRpmStep;  // Шаг калибровки (об/мин) - сейчас 0.1 или 0.3 оборота
    private Integer trackingIntervalRecup; // 0x0511, секунды
}
