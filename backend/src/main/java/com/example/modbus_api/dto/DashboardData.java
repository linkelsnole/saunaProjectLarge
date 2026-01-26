package com.example.modbus_api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardData {
    private List<ClimateSensorDto> climateSensors; 
    private List<SensorData> debugValveSensors; // Для отображения ValveHeatPos1 и тд 22.01.2026
    
    @JsonProperty("wSensors")
    private List<SensorData> wSensors;        
    
    private List<SensorData> pressureSensors;      
    private List<FanFlowData> fans;
    private List<SensorData> valves;
    private List<SensorData> recuperator;
    private RecuperatorStatusDto recuperatorStatus;
    private Integer vm1;
    private Integer vm2;
    private Integer recupSpeed;
    private Integer recupCurrent; 
    
    @JsonProperty("vManualProcIn")
    private Double vManualProcIn;
    
    @JsonProperty("vManualDProcOut")
    private Double vManualDProcOut;
    
    private ManualSettingsDto manualSettings;
    private TempSettingsDto tempSettings;

    private Boolean damperSupply;
    private Boolean damperExhaust;
    
    // Новые регистры рекуператора
    private Double recupTargetRpm;   // Обороты вала рекуператора (целевые), об/мин
    private Double recupCurrentRpm;   // Обороты вала, об/мин
    private Double recupSpeedRpm;     // Скорость вращения, об/мин
}
