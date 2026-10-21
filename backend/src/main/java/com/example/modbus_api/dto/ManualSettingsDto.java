package com.example.modbus_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualSettingsDto {
    // Скорость вентилирования ПВ
    private Double supplySpeed;
    // Процент дисбаланса ПВ-ОВ
    private Double boostPercent;
}

