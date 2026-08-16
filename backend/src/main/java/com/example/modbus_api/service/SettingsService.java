package com.example.modbus_api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static com.example.modbus_api.service.ModbusConstants.*;

@Service
public class SettingsService {
	private final ModbusClientAdapter modbusClient;

	@Autowired
	public SettingsService(ModbusClientAdapter modbusClient) {
		this.modbusClient = modbusClient;
	}

	public void updateManualSettings(Double supplySpeed, Double boostPercent) throws IOException {
		// При записи умножаем на 10 (90.1 -> 901)
		if (supplySpeed != null) {
			int rawValue = (int) Math.round(supplySpeed * 10);
			modbusClient.writeRegister(SAUNA_PLC, REG_V_VManualProcIn, rawValue);
			System.out.println("DEBUG: Записано supplySpeed: " + supplySpeed + " -> raw: " + rawValue);
		}
		
		if (boostPercent != null) {
			int rawValue = (int) Math.round(boostPercent * 10);
			modbusClient.writeRegister(SAUNA_PLC, REG_V_VManualDProcOut, rawValue);
			System.out.println("DEBUG: Записано boostPercent: " + boostPercent + " -> raw: " + rawValue);
		}
		
		// После изменения уставок выполняем быструю смену режима
		System.out.println("DEBUG: Выполняется быстрая смена режима...");
		performFastModeChange();
		System.out.println("DEBUG: Быстрая смена режима выполнена");
	}

	public void updateTempSettings(
		Double targetZone1, 
		Double targetZone2,
		Double waterWarn, 
		Double waterFail,
		Double inflowWarn, 
		Double inflowFail,
		Double neutralZoneT,
		Double hotValveMinProc,
		Double waterMinTNoWork,
		Double waterMinTWork,
		Integer valveStartupPosition,
		Integer controlAlgorithm,
		Integer tempTrackingInterval
	) throws IOException {
		/*
		 * Порядок важен: сначала приоритет 0 по температуре (п.1–3), значения как в Modbus, без REG_POWERPROC:
		 *   0x00E1 (REG_T_VALVE_START_POS) — положение вентилей при запуске,
		 *   0x00DD (REG_T_CTRL_ALGORITHM) — алгоритм управления,
		 *   0x007E (REG_T_TRACK_INTERVAL) — интервал слежения по T;
		 * четвёртый регистр приоритета 0 — 0x0064 (интервал слежения по рекуператору) — в updateRecuperatorSettings.
		 * Затем целевые температуры зон, пороги воды и ПВ, новые температурные уставки; в конце — performFastModeChange() → REG_AGGREGATE_CMD.
		 */
		if (valveStartupPosition != null) {
			modbusClient.writeRegister(SAUNA_PLC, REG_T_VALVE_START_POS, clampToInt16(valveStartupPosition));
		}
		if (controlAlgorithm != null) {
			modbusClient.writeRegister(SAUNA_PLC, REG_T_CTRL_ALGORITHM, clampToInt16(controlAlgorithm));
		}
		if (tempTrackingInterval != null) {
			modbusClient.writeRegister(SAUNA_PLC, REG_T_TRACK_INTERVAL, clampToInt16(tempTrackingInterval));
		}

		// Целевые температуры зон (не делятся на 10)
		if (targetZone1 != null) {
			modbusClient.writeRegister(SAUNA_PLC, REG_T_TARGET_ZONE1, targetZone1.intValue());
		}
		if (targetZone2 != null) {
			modbusClient.writeRegister(SAUNA_PLC, REG_T_TARGET_ZONE2, targetZone2.intValue());
		}
		
		// Пороги температуры воды (умножаем на 10)
		if (waterWarn != null) {
			int rawValue = (int) Math.round(waterWarn * 10);
			modbusClient.writeRegister(SAUNA_PLC, REG_T_WATER_WARN, rawValue);
		}
		if (waterFail != null) {
			int rawValue = (int) Math.round(waterFail * 10);
			modbusClient.writeRegister(SAUNA_PLC, REG_T_WATER_FAIL, rawValue);
		}
		
		// Пороги температуры ПВ 
		if (inflowWarn != null) {
			int rawValue = (int) Math.round(inflowWarn * 10);
			modbusClient.writeRegister(SAUNA_PLC, REG_T_INFLOW_WARN, rawValue);
		}
		if (inflowFail != null) {
			int rawValue = (int) Math.round(inflowFail * 10);
			modbusClient.writeRegister(SAUNA_PLC, REG_T_INFLOW_FAIL, rawValue);
		}

		// Новые температурные уставки (умножаем на 10)
		if (neutralZoneT != null) {
			int rawValue = (int) Math.round(neutralZoneT * 10);
			modbusClient.writeRegister(SAUNA_PLC, REG_T_NEUTRAL_ZONE, rawValue);
		}
		if (hotValveMinProc != null) {
			int rawValue = (int) Math.round(hotValveMinProc * 10);
			modbusClient.writeRegister(SAUNA_PLC, REG_HOT_VALVE_MIN_PROC, rawValue);
		}
		if (waterMinTNoWork != null) {
			int rawValue = (int) Math.round(waterMinTNoWork * 10);
			modbusClient.writeRegister(SAUNA_PLC, REG_T_WATER_NO_WORK, rawValue);
		}
		if (waterMinTWork != null) {
			int rawValue = (int) Math.round(waterMinTWork * 10);
			modbusClient.writeRegister(SAUNA_PLC, REG_T_WATER_WORK, rawValue);
		}
		
		performFastModeChange();
	}

	public void updateRecuperatorSettings(
		Double targetRpmMin,
		Double targetRpmMax,
		Double targetRpmStep,
		Integer trackingIntervalRecup
	) throws IOException {
		// п.4 приоритета 0: 0x0064 (REG_R_TRACK_INTERVAL) — интервал слежения по рекуператору; запись до min/max/step и до команды смены режима
		if (trackingIntervalRecup != null) {
			modbusClient.writeRegister(SAUNA_PLC, REG_R_TRACK_INTERVAL, clampToInt16(trackingIntervalRecup));
		}

		if (targetRpmMin != null) {
			int rawValue = (int) Math.round(targetRpmMin * 10);
			modbusClient.writeRegister(SAUNA_PLC, REG_R_TARGET_RPM_MIN, rawValue);
		}

		if (targetRpmMax != null) {
			int rawValue = (int) Math.round(targetRpmMax * 10);
			modbusClient.writeRegister(SAUNA_PLC, REG_R_TARGET_RPM_MAX, rawValue);
		}

		if (targetRpmStep != null) {
			int rawValue = (int) Math.round(targetRpmStep * 10);
			modbusClient.writeRegister(SAUNA_PLC, REG_R_TARGET_RPM_STEP, rawValue);
		}

		performFastModeChange();
	}

	public void performFastModeChange() throws IOException {
		System.out.println("DEBUG: performFastModeChange вызван, отправка команды " + CMD_FAST_MODE_CHANGE);
		sendAggregateCommand(CMD_FAST_MODE_CHANGE);
		System.out.println("DEBUG: Команда " + CMD_FAST_MODE_CHANGE + " отправлена в регистр 0x" + Integer.toHexString(REG_AGGREGATE_CMD).toUpperCase());
	}

	private void sendAggregateCommand(int command) throws IOException {
		modbusClient.writeRegister(SAUNA_PLC, REG_AGGREGATE_CMD, command);
	}

	private static int clampToInt16(int v) {
		return Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, v));
	}
}

