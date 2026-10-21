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
    private List<DeviceStateDto> deviceStates;
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
    
    @JsonProperty("vManualProcIn")
    private Double vManualProcIn;
    
    @JsonProperty("vManualDProcOut")
    private Double vManualDProcOut;
    
    private ManualSettingsDto manualSettings;
    private TempSettingsDto tempSettings;
    private RecuperatorSettingsDto recuperatorSettings;

    private Boolean damperSupply;
    private Boolean damperExhaust;
    private Boolean pumpActive;  // Workstate2 бит 14 — насос рекуператора (команда)
    private Integer recupStateCode;  // RecupState 0x00A4
    private String recupStateName;   // Отключен, Макс. обороты, Настройка, Стабилизация, Врем. остановка

    // Новые регистры рекуператора
    private Double recupTargetRpm;   // Обороты вала рекуператора (целевые), об/мин
    private Double recupMaximumRpm; // Air2Common RecupRealMaxVal, 0x0804 / 100
    private Double recupCurrentRpm;   // Обороты вала, об/мин
    private Double recupSpeedRpm;     // Скорость вращения, об/мин

    @JsonProperty("recupTempBt6")
    private Double recupTempBt6;     // Температура рекуператора BT6 (0x10C8), °C

    @JsonProperty("recupTargetPercent")
    private Integer recupTargetPercent; // recupTargetRpm / targetRpmMax * 100, %

    private Integer ventilationModeCode;
    private String ventilationModeName;
    private Double airflowTarget;
    private Double airflowReal;
    private VolumeSettingsDto volumeSettings;
    private Integer temperatureControlModeZone1Code;
    private String temperatureControlModeZone1Name;
    private Integer temperatureControlModeZone2Code;
    private String temperatureControlModeZone2Name;
    private Boolean screenMode;

    private CalibrationData calibration; // бит 32 REG_R_STATE — идёт калибровка агрегата
}
