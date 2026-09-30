package com.example.modbus_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateScheduleIntervalRequest {
    @Min(1) @Max(20) private int scheduleId;
    @NotNull private Integer startTime;
    @NotNull private Integer endTime;
    @Min(1) @Max(3) private int airControlMode;
    @Min(0) @Max(2) private int zone1ControlMode;
    @DecimalMin("-50.0") @DecimalMax("100.0") private Double zone1TargetTemperature;
    @Min(0) @Max(2) private int zone2ControlMode;
    @DecimalMin("-50.0") @DecimalMax("100.0") private Double zone2TargetTemperature;
}
