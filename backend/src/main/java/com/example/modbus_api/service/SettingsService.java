package com.example.modbus_api.service;

import org.springframework.stereotype.Service;

import com.example.modbus_api.dto.UpdateVolumeSettingsRequest;
import com.example.modbus_api.dto.OrpSettingsDto;
import com.example.modbus_api.dto.UpdateTemperatureControlRequest;
import com.example.modbus_api.dto.UpdateTemperatureSetpointsRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.example.modbus_api.service.ModbusConstants.*;

@Service
public class SettingsService {
	private static final double AIRFLOW_MIN = 0.0;
	private static final double AIRFLOW_MAX = 10000.0;
	private static final double BOOST_MIN = -100.0;
	private static final double BOOST_MAX = 100.0;

	private final ModbusClientAdapter modbusClient;

	public SettingsService(ModbusClientAdapter modbusClient) {
		this.modbusClient = modbusClient;
	}

	public void updateManualSettings(Double supplySpeed, Double boostPercent) throws IOException {
		UpdateVolumeSettingsRequest request = new UpdateVolumeSettingsRequest();
		request.setManualAirflow(supplySpeed);
		request.setBoostPercent(boostPercent);
		updateVolumeSettings(request);
	}

	public void setVentilationMode(int modeCode) throws IOException {
		if (modeCode < 1 || modeCode > 4) {
			throw new IllegalArgumentException("Режим вентиляции должен быть в диапазоне 1–4");
		}

		List<Integer> modeRegisters = new ArrayList<>();
		for (int i = 0; i < ModbusRegisterMap.SCHEDULE_COUNT; i++) {
			modeRegisters.add(ModbusRegisterMap.scheduleBase(i));
		}
		Map<Integer, Integer> modes = modbusClient.readRegisters(SAUNA_PLC, modeRegisters);
		Integer userSetBase = findUserSetScheduleBase(modes);
		if (userSetBase == null) {
			throw new IOException("Расписание UserSet (mode=5) не найдено");
		}

		int stateRegister = userSetBase + ModbusRegisterMap.SCHEDULE_STATE_OFFSET;
		Integer scheduleState = modbusClient.readRegister(SAUNA_PLC, stateRegister);
		if (scheduleState == null || scheduleState == INVALID_VALUE) {
			throw new IOException("Не удалось прочитать состояние UserSet");
		}
		Integer aggregateState = modbusClient.readRegister(SAUNA_PLC, ModbusRegisterMap.AGGREGATE_STATE);

		modbusClient.writeRegister(SAUNA_PLC,
			userSetBase + ModbusRegisterMap.SCHEDULE_VENTILATION_MODE_OFFSET, modeCode);
		int selectedState = scheduleState | ModbusRegisterMap.SCHEDULE_SELECTED_MASK;
		if (selectedState != scheduleState) {
			modbusClient.writeRegister(SAUNA_PLC, stateRegister, selectedState);
		}

		boolean running = aggregateState != null
			&& aggregateState != INVALID_VALUE
			&& (aggregateState & MASK_STATE_VENTILATION) != 0;
		modbusClient.writeRegister(SAUNA_PLC, ModbusRegisterMap.AGGREGATE_COMMAND, running ? 6 : 1);
	}

	public void setTemperatureControlModes(UpdateTemperatureControlRequest request) throws IOException {
		if (request == null || (request.getZone1Mode() == null && request.getZone2Mode() == null)) {
			throw new IllegalArgumentException("Не передан режим регулирования температуры");
		}
		validateTemperatureControlMode(request.getZone1Mode());
		validateTemperatureControlMode(request.getZone2Mode());
		boolean selectsOutflow = Integer.valueOf(ModbusRegisterMap.TEMPERATURE_CONTROL_OUTFLOW)
			.equals(request.getZone1Mode())
			|| Integer.valueOf(ModbusRegisterMap.TEMPERATURE_CONTROL_OUTFLOW).equals(request.getZone2Mode());
		if (selectsOutflow && !(Integer.valueOf(ModbusRegisterMap.TEMPERATURE_CONTROL_OUTFLOW)
			.equals(request.getZone1Mode())
			&& Integer.valueOf(ModbusRegisterMap.TEMPERATURE_CONTROL_OUTFLOW).equals(request.getZone2Mode()))) {
			throw new IllegalArgumentException("Режим ОВ должен одновременно применяться к обеим зонам");
		}

		List<Integer> modeRegisters = new ArrayList<>();
		for (int i = 0; i < ModbusRegisterMap.SCHEDULE_COUNT; i++) {
			modeRegisters.add(ModbusRegisterMap.scheduleBase(i));
		}
		Map<Integer, Integer> modes = modbusClient.readRegisters(SAUNA_PLC, modeRegisters);
		Integer userSetBase = findUserSetScheduleBase(modes);
		if (userSetBase == null) {
			throw new IOException("Расписание UserSet (mode=5) не найдено");
		}

		int stateRegister = userSetBase + ModbusRegisterMap.SCHEDULE_STATE_OFFSET;
		Integer scheduleState = modbusClient.readRegister(SAUNA_PLC, stateRegister);
		if (scheduleState == null || scheduleState == INVALID_VALUE) {
			throw new IOException("Не удалось прочитать состояние UserSet");
		}
		Integer aggregateState = modbusClient.readRegister(SAUNA_PLC, ModbusRegisterMap.AGGREGATE_STATE);

		forceAutoTemperature(userSetBase, request.getZone1Mode() != null, request.getZone2Mode() != null);

		if (request.getZone1Mode() != null) {
			modbusClient.writeRegister(SAUNA_PLC,
				ModbusRegisterMap.HEATER_ZONE_1 + ModbusRegisterMap.TEMPERATURE_CONTROL_OFFSET,
				request.getZone1Mode());
			modbusClient.writeRegister(SAUNA_PLC,
				userSetBase + ModbusRegisterMap.SCHEDULE_TEMPERATURE_CONTROL_ZONE_1_OFFSET,
				request.getZone1Mode());
		}
		if (request.getZone2Mode() != null) {
			modbusClient.writeRegister(SAUNA_PLC,
				ModbusRegisterMap.HEATER_ZONE_2 + ModbusRegisterMap.TEMPERATURE_CONTROL_OFFSET,
				request.getZone2Mode());
			modbusClient.writeRegister(SAUNA_PLC,
				userSetBase + ModbusRegisterMap.SCHEDULE_TEMPERATURE_CONTROL_ZONE_2_OFFSET,
				request.getZone2Mode());
		}

		int selectedState = scheduleState | ModbusRegisterMap.SCHEDULE_SELECTED_MASK;
		if (selectedState != scheduleState) {
			modbusClient.writeRegister(SAUNA_PLC, stateRegister, selectedState);
		}
		boolean running = aggregateState != null
			&& aggregateState != INVALID_VALUE
			&& (aggregateState & MASK_STATE_VENTILATION) != 0;
		modbusClient.writeRegister(SAUNA_PLC, ModbusRegisterMap.AGGREGATE_COMMAND, running ? 6 : 1);
	}

	public void updateTemperatureSetpoints(UpdateTemperatureSetpointsRequest request) throws IOException {
		if (request == null || request.isEmpty()) {
			throw new IllegalArgumentException("Не переданы температурные уставки");
		}
		validateTemperature("ОВ", request.getOutflowTarget());
		validateTemperature("ПВ зоны 1", request.getInflowTargetZone1());
		validateTemperature("ПВ зоны 2", request.getInflowTargetZone2());
		validateOrp("ОРП зоны 1", request.getOrpZone1());
		validateOrp("ОРП зоны 2", request.getOrpZone2());
		forceAutoTemperature(requireTemperatureUserSetBase(),
            request.getOutflowTarget() != null || request.getInflowTargetZone1() != null || request.getOrpZone1() != null,
            request.getOutflowTarget() != null || request.getInflowTargetZone2() != null || request.getOrpZone2() != null);

		if (request.getOutflowTarget() != null) {
			int rawValue = scaledTemperature(request.getOutflowTarget());
			modbusClient.writeRegister(SAUNA_PLC,
				ModbusRegisterMap.HEATER_ZONE_1 + ModbusRegisterMap.OUTFLOW_TARGET_OFFSET, rawValue);
			modbusClient.writeRegister(SAUNA_PLC,
				ModbusRegisterMap.HEATER_ZONE_2 + ModbusRegisterMap.OUTFLOW_TARGET_OFFSET, rawValue);
		}
		writeScaledTemperatureIfPresent(
			ModbusRegisterMap.HEATER_ZONE_1 + ModbusRegisterMap.INFLOW_TARGET_OFFSET,
			request.getInflowTargetZone1());
		writeScaledTemperatureIfPresent(
			ModbusRegisterMap.HEATER_ZONE_2 + ModbusRegisterMap.INFLOW_TARGET_OFFSET,
			request.getInflowTargetZone2());
		writeOrpSettings(ModbusRegisterMap.HEATER_ZONE_1, request.getOrpZone1());
		writeOrpSettings(ModbusRegisterMap.HEATER_ZONE_2, request.getOrpZone2());
		performFastModeChange();
	}

	private void validateTemperatureControlMode(Integer mode) {
		if (mode != null && (mode < ModbusRegisterMap.TEMPERATURE_CONTROL_ORP
			|| mode > ModbusRegisterMap.TEMPERATURE_CONTROL_OUTFLOW)) {
			throw new IllegalArgumentException("Режим регулирования температуры должен быть 0, 1 или 2");
		}
	}

	private void validateTemperature(String name, Double value) {
		if (value != null && (value.isNaN() || value.isInfinite() || value < -50.0 || value > 100.0)) {
			throw new IllegalArgumentException(name + ": температура должна быть в диапазоне -50…100 °C");
		}
	}

	private void validateOrp(String name, OrpSettingsDto settings) {
		if (settings == null) return;
		validateTemperature(name + " T1 (цель)", settings.getTargetT1());
		validateTemperature(name + " T1 (текущая)", settings.getCurrentT1());
		validateTemperature(name + " T2 (цель)", settings.getTargetT2());
		validateTemperature(name + " T2 (текущая)", settings.getCurrentT2());
		validateTemperature(name + " T3 (цель)", settings.getTargetT3());
		validateTemperature(name + " T3 (текущая)", settings.getCurrentT3());
	}

	private int scaledTemperature(Double value) {
		return (int) Math.round(value * 10.0) & 0xFFFF;
	}

	private void writeScaledTemperatureIfPresent(int register, Double value) throws IOException {
		if (value != null) {
			modbusClient.writeRegister(SAUNA_PLC, register, scaledTemperature(value));
		}
	}

	private void writeOrpSettings(int base, OrpSettingsDto settings) throws IOException {
		if (settings == null) return;
		writeWholeTemperatureIfPresent(base + ModbusRegisterMap.ORP_TARGET_T1_OFFSET, settings.getTargetT1());
		writeWholeTemperatureIfPresent(base + ModbusRegisterMap.ORP_CURRENT_T1_OFFSET, settings.getCurrentT1());
		writeWholeTemperatureIfPresent(base + ModbusRegisterMap.ORP_TARGET_T2_OFFSET, settings.getTargetT2());
		writeWholeTemperatureIfPresent(base + ModbusRegisterMap.ORP_CURRENT_T2_OFFSET, settings.getCurrentT2());
		writeWholeTemperatureIfPresent(base + ModbusRegisterMap.ORP_TARGET_T3_OFFSET, settings.getTargetT3());
		writeWholeTemperatureIfPresent(base + ModbusRegisterMap.ORP_CURRENT_T3_OFFSET, settings.getCurrentT3());
	}

	private void writeWholeTemperatureIfPresent(int register, Double value) throws IOException {
		if (value != null) {
			modbusClient.writeRegister(SAUNA_PLC, register, ((int) Math.round(value)) & 0xFFFF);
		}
	}

	public void updateVolumeSettings(UpdateVolumeSettingsRequest request) throws IOException {
		if (request == null || request.isEmpty()) {
			throw new IllegalArgumentException("Не переданы настройки объёма");
		}
		validateAirflow("ЭКО", request.getEcoAirflow());
		validateAirflow("Норм", request.getNormalAirflow());
		validateAirflow("Интенсив", request.getIntensiveAirflow());
		validateAirflow("Ручной", request.getManualAirflow());
		validateBoost(request.getBoostPercent());

		writeFloatIfPresent(ModbusRegisterMap.AIRFLOW_ECO, request.getEcoAirflow());
		writeFloatIfPresent(ModbusRegisterMap.AIRFLOW_NORMAL, request.getNormalAirflow());
		writeFloatIfPresent(ModbusRegisterMap.AIRFLOW_INTENSIVE, request.getIntensiveAirflow());
		writeFloatIfPresent(ModbusRegisterMap.AIRFLOW_MANUAL, request.getManualAirflow());
		if (request.getBoostPercent() != null) {
			int rawBoost = (int) Math.round(request.getBoostPercent() * 10.0);
			modbusClient.writeRegister(SAUNA_PLC, ModbusRegisterMap.AIRFLOW_IMBALANCE, rawBoost & 0xFFFF);
		}
		performFastModeChange();
	}

	private int requireTemperatureUserSetBase() throws IOException {
        List<Integer> registers = new ArrayList<>();
        for (int i = 0; i < ModbusRegisterMap.SCHEDULE_COUNT; i++) {
            registers.add(ModbusRegisterMap.scheduleBase(i));
        }
        Integer base = findUserSetScheduleBase(modbusClient.readRegisters(SAUNA_PLC, registers));
        if (base == null) throw new IOException("Расписание UserSet (mode=5) не найдено");
        return base;
    }

    private void forceAutoTemperature(int base, boolean zone1, boolean zone2) throws IOException {
        if (zone1) modbusClient.writeRegister(SAUNA_PLC, base + ModbusRegisterMap.SCHEDULE_TEMPERATURE_MODE_ZONE_1_OFFSET, 2);
        if (zone2) modbusClient.writeRegister(SAUNA_PLC, base + ModbusRegisterMap.SCHEDULE_TEMPERATURE_MODE_ZONE_2_OFFSET, 2);
    }

	private Integer findUserSetScheduleBase(Map<Integer, Integer> modes) {
		for (int i = 0; i < ModbusRegisterMap.SCHEDULE_COUNT; i++) {
			int base = ModbusRegisterMap.scheduleBase(i);
			if (Integer.valueOf(ModbusRegisterMap.SCHEDULE_USERSET_MODE).equals(modes.get(base))) {
				return base;
			}
		}
		return null;
	}

	private void writeFloatIfPresent(int register, Double value) throws IOException {
		if (value == null) {
			return;
		}
		float floatValue = value.floatValue();
		modbusClient.writeRegister(SAUNA_PLC, register, ModbusRegisterMap.floatLowWord(floatValue));
		modbusClient.writeRegister(SAUNA_PLC, register + 1, ModbusRegisterMap.floatHighWord(floatValue));
	}

	private void validateAirflow(String name, Double value) {
		if (value == null) {
			return;
		}
		if (value.isNaN() || value.isInfinite() || value < AIRFLOW_MIN || value > AIRFLOW_MAX) {
			throw new IllegalArgumentException(name + ": объём должен быть в диапазоне 0–10000 м³/ч");
		}
	}

	private void validateBoost(Double value) {
		if (value == null) {
			return;
		}
		if (value.isNaN() || value.isInfinite() || value < BOOST_MIN || value > BOOST_MAX) {
			throw new IllegalArgumentException("Наддув должен быть в диапазоне от -100 до 100 %");
		}
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
		if (targetZone1 != null || targetZone2 != null) {
            forceAutoTemperature(requireTemperatureUserSetBase(), targetZone1 != null, targetZone2 != null);
        }
		/*
		 * Порядок важен: сначала приоритет 0 по температуре (п.1–3), значения как в Modbus, без REG_POWERPROC:
		 *   0x00E1 (REG_T_VALVE_START_POS) — положение вентилей при запуске,
		 *   0x00DD (REG_T_CTRL_ALGORITHM) — алгоритм управления,
		 *   0x007E (REG_T_TRACK_INTERVAL) — интервал слежения по T.
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
			modbusClient.writeRegister(SAUNA_PLC, REG_T_TARGET_ZONE1, scaledTemperature(targetZone1));
		}
		if (targetZone2 != null) {
			modbusClient.writeRegister(SAUNA_PLC, REG_T_TARGET_ZONE2, scaledTemperature(targetZone2));
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
		// 0x0511 DQualityBack — интервал опроса счётчика оборотов, секунды
		if (trackingIntervalRecup != null) {
			modbusClient.writeRegister(SAUNA_PLC, REG_R_TRACK_INTERVAL, clampToInt16(trackingIntervalRecup));
		}

		if (targetRpmMin != null) {
			int rawValue = (int) Math.round(targetRpmMin * 100);
			modbusClient.writeRegister(SAUNA_PLC, REG_R_TARGET_RPM_MIN, rawValue);
		}

		if (targetRpmMax != null) {
			int rawValue = (int) Math.round(targetRpmMax * 100);
			modbusClient.writeRegister(SAUNA_PLC, REG_R_TARGET_RPM_MAX, rawValue);
		}

		if (targetRpmStep != null) {
			int rawValue = (int) Math.round(targetRpmStep * 100);
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
