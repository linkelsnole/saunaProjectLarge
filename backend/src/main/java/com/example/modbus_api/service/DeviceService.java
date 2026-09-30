package com.example.modbus_api.service;

import org.springframework.stereotype.Service;
import com.example.modbus_api.dto.SensorData;
import com.example.modbus_api.dto.CalibrationData;
import com.example.modbus_api.dto.DashboardData;
import com.example.modbus_api.dto.ClimateSensorDto;
import com.example.modbus_api.dto.FanFlowData;
import com.example.modbus_api.dto.RecuperatorStatusDto;
import com.example.modbus_api.dto.ManualSettingsDto;
import com.example.modbus_api.dto.TempSettingsDto;
import com.example.modbus_api.dto.OrpSettingsDto;
import com.example.modbus_api.dto.RecuperatorSettingsDto;
import com.example.modbus_api.dto.VolumeSettingsDto;

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.io.IOException;

import static com.example.modbus_api.service.ModbusConstants.*;


@Service
public class DeviceService {
	private final ModbusClientAdapter modbusClient;

	public DeviceService(ModbusClientAdapter modbusClient) {
		this.modbusClient = modbusClient;
	}

	public ModbusClientAdapter getModbusClient() {
		return modbusClient;
	}

	public void enableRecuperator() throws IOException {
		modbusClient.writeRegister(SAUNA_PLC, REG_AGGREGATE_CMD, 1);
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		
		Integer currentState = modbusClient.readRegister(SAUNA_PLC, REG_R_STATE);
		String stateName = resolveRecuperatorStateName(currentState);
		System.out.println("DEBUG: Команда включения отправлена, текущее состояние: " + currentState + " - " + stateName);
	}

	public void disableRecuperator() throws IOException {
		modbusClient.writeRegister(SAUNA_PLC, REG_AGGREGATE_CMD, 2);
		
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		
		Integer currentState = modbusClient.readRegister(SAUNA_PLC, REG_R_STATE);
		String stateName = resolveRecuperatorStateName(currentState);
		System.out.println("DEBUG: Команда выключения отправлена, текущее состояние: " + currentState + " - " + stateName);
	}

	public boolean isCalibratingState() throws IOException {
		Map<Integer, Integer> results = modbusClient.readRegisters(SAUNA_PLC, Collections.singletonList(REG_R_STATE));
		int stateCode = results.getOrDefault(REG_R_STATE, 0);
		return (stateCode & 32) != 0;
	}

	public void setScreenMode(boolean screenMode) throws IOException {
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
		Integer currentState = modbusClient.readRegister(SAUNA_PLC, stateRegister);
		if (currentState == null || currentState == INVALID_VALUE) {
			throw new IOException("Не удалось прочитать состояние UserSet");
		}
		// Как в основной схеме 301: передаём полное состояние UserSet,
		// чтобы при выходе с экрана сбросить и Selected, и прежний Working.
		int requestedState = screenMode ? 1 : 0;
		modbusClient.writeRegister(SAUNA_PLC, stateRegister, requestedState);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Прервано подтверждение состояния UserSet", e);
		}
		int confirmedState = modbusClient.readRegister(SAUNA_PLC, stateRegister);
		System.out.printf("UserSet %s state[0x%04X]: before=%d, requested=%d, readback=%d (before command 6)%n",
			SAUNA_PLC, stateRegister, currentState, requestedState, confirmedState);
		if (confirmedState != requestedState) {
			System.err.println("UserSet: прочитанное состояние отличается от отправленного; команда применяется как в 301");
		}
		// Как в 301: возврат к расписанию всегда применяет режим,
		// активация экрана — только когда агрегат уже вентилирует.
		if (!screenMode) {
			modbusClient.writeRegister(SAUNA_PLC, REG_AGGREGATE_CMD, CMD_FAST_MODE_CHANGE);
		} else {
			Integer aggregateState = modbusClient.readRegister(SAUNA_PLC, REG_R_STATE);
			if (isValid(aggregateState) && (aggregateState & MASK_STATE_VENTILATION) != 0) {
				modbusClient.writeRegister(SAUNA_PLC, REG_AGGREGATE_CMD, CMD_FAST_MODE_CHANGE);
			}
		}
	}

	public DashboardData getDashboardData() throws IOException {
		List<Integer> tempRegs = Arrays.asList(
			// --- ТЕМПЕРАТУРА (тип 1) ---
			REG_T_BT3,    // T1
			REG_T_BT11,    // T2
			REG_T_BT10, // T3
			REG_T_BT1,  // T4-1
			REG_T_BT2,    // T5
			REG_T_BT21,  // T4-2

			// --- ТЕМПЕРАТУРА (тип 2) ---
			REG_T_BT8,
			REG_T_BT9,
			REG_T_BT24,
			REG_T_BT27,

			// --- ТЕМПЕРАТУРЫ ЗОН ---
			REG_T_ZONE1,
			REG_T_ZONE2
		);

		List<Integer> allRegs = new ArrayList<>(tempRegs);
		for (Integer reg : tempRegs) {
			allRegs.add(reg + OFFSET_HUMID);
		}

		allRegs.addAll(Arrays.asList(
			// Приоритет 0 (Сауна): 0xE1, 0xDD, 0x7E (темп.) + 0x64 (рекуп) — см. ModbusConstants
			REG_T_VALVE_START_POS,
			REG_T_CTRL_ALGORITHM,
			REG_T_TRACK_INTERVAL,
			REG_R_TRACK_INTERVAL,

			// --- ДАВЛЕНИЕ (Вентиляторы) ---
			REG_P_BF11,  // вентилятор 1
			REG_P_BF12,  // внетилятор 2
			REG_P_BF21,  // вентилятор 3
			REG_P_BF22,  // внетилятор 4

			// --- ДАВЛЕНИЕ (W)  ---
			REG_P_BP1,
			REG_P_BP2,
			REG_P_BP5,  // Давление "слева от рекупа"
			REG_P_BP10,   // Давление "справа от рекупа"

			// --- ОБОРОТЫ ВЕНТИЛЯТОРОВ ---
			REG_P_VM11, //обороты вентилятора 1
			REG_P_VM12, //обороты вентилятора 2
			REG_P_VM21, //обороты вентилятора 3
			REG_P_VM22, //обороты вентилятора 4

			// --- ЦЕЛЬ ВЕНТИЛЯТОРОВ ---
			REG_F_TARGET, // цель всех вентиляторов
			REG_POWERPROC, // powerproc для расчета цели справа

			// --- ОБЪЕМ ПРОКАЧКИ ---
			REG_F_VM1, // объем прокачки 1
			REG_F_VM2, // объем прокачки 2

			// --- КЛАПАНЫ ---
			REG_F_VH1, //клапан + слева
			REG_F_VC1, //клапан - справа
			REG_F_VH2, //клапан + справа
			REG_F_VC2, //клапан - справа

			// --- СКОРОСТЬ (Вентиляторы) ---
			REG_F_G11,
			REG_F_G12,
			REG_F_G21,
			REG_F_G22,

			// --- РЕКУПЕРАТОР ---
			REG_R_RECUP_SPEED,
			SPEED_R_REG,   // 0x0082 скорость вращения
			REG_R_TARGET,
			REG_R_STATE,
			TARGET_R_REG,  // Обороты вала рекуператора (целевые) - делить на 100
			CURRENT_R_REG, // Обороты вала - делить на 100

			// --- MANUAL PROC ---
			REG_V_VManualProcIn,
			REG_V_VManualDProcOut,

			// --- ТЕМПЕРАТУРНЫЕ УСТАВКИ ---
			REG_T_WATER_WARN,
			REG_T_WATER_FAIL,
			REG_T_INFLOW_WARN,
			REG_T_INFLOW_FAIL,
			REG_T_CTRL_MODE_ZONE1,
			REG_T_CTRL_MODE_ZONE2,

			//Задвижки
			REG_Z_WORKSTATE,
			REG_Z_WORKSTATE2,

			// --- НОВЫЕ РЕГИСТРЫ (22.01.2026) ---
			REG_T_NEUTRAL_ZONE,
			REG_HOT_VALVE_MIN_PROC,
			REG_T_WATER_NO_WORK,
			REG_T_WATER_WORK,
			REG_T_RAD_FZ_FAIL,
			REG_VALVE_HEAT_POS1,
			REG_VALVE_COOL_POS1,
			REG_VALVE_OPEN_PROC1,
			REG_VALVE_HEAT_POS2,
			REG_VALVE_COOL_POS2,
			REG_VALVE_OPEN_PROC2,


			REG_R_TARGET_RPM_MIN,
			REG_R_TARGET_RPM_MAX,
			REG_R_TARGET_RPM_STEP,

			REG_R_TEMP,   // 0x02C0 температура рекуператора (BT6)
			REG_RECUP_STATE  // 0x00A4 RecupState — текущее состояние рекуператора
		));

		// Новая карта общей схемы. Чтения специфичных для большой сауны
		// REG_Z_WORKSTATE2 и REG_R_TEMP выше сохраняются до уточнения их адресов.
		int[] sensorBases = {
			ModbusRegisterMap.SENSOR_T1, ModbusRegisterMap.SENSOR_T2,
			ModbusRegisterMap.SENSOR_T3, ModbusRegisterMap.SENSOR_T4_1,
			ModbusRegisterMap.SENSOR_T4_2, ModbusRegisterMap.SENSOR_T5,
			ModbusRegisterMap.SENSOR_TH1, ModbusRegisterMap.SENSOR_TC1,
			ModbusRegisterMap.SENSOR_TH2, ModbusRegisterMap.SENSOR_TC2
		};
		for (int base : sensorBases) {
			allRegs.add(base + ModbusRegisterMap.TEMPERATURE_OFFSET);
			allRegs.add(base + ModbusRegisterMap.HUMIDITY_OFFSET);
		}
		int[] floatRegisters = {
			ModbusRegisterMap.PRESSURE_FAN_SUPPLY_1,
			ModbusRegisterMap.PRESSURE_FAN_SUPPLY_2,
			ModbusRegisterMap.PRESSURE_FAN_EXHAUST_1,
			ModbusRegisterMap.PRESSURE_FAN_EXHAUST_2,
			ModbusRegisterMap.PRESSURE_FILTER_SUPPLY,
			ModbusRegisterMap.PRESSURE_FILTER_EXHAUST,
			ModbusRegisterMap.PRESSURE_RECUP_SUPPLY,
			ModbusRegisterMap.PRESSURE_RECUP_EXHAUST,
			ModbusRegisterMap.FAN_SUPPLY_1 + ModbusRegisterMap.FAN_AIRFLOW_OFFSET,
			ModbusRegisterMap.FAN_SUPPLY_2 + ModbusRegisterMap.FAN_AIRFLOW_OFFSET,
			ModbusRegisterMap.FAN_EXHAUST_1 + ModbusRegisterMap.FAN_AIRFLOW_OFFSET,
			ModbusRegisterMap.FAN_EXHAUST_2 + ModbusRegisterMap.FAN_AIRFLOW_OFFSET,
			ModbusRegisterMap.AIRFLOW_ECO,
			ModbusRegisterMap.AIRFLOW_NORMAL,
			ModbusRegisterMap.AIRFLOW_INTENSIVE,
			ModbusRegisterMap.AIRFLOW_MANUAL,
			ModbusRegisterMap.AIRFLOW_TARGET,
			ModbusRegisterMap.AIRFLOW_REAL
		};
		for (int register : floatRegisters) {
			addFloatRegister(allRegs, register);
		}
		allRegs.addAll(Arrays.asList(
			ModbusRegisterMap.FAN_SUPPLY_1 + ModbusRegisterMap.FAN_PERCENT_OFFSET,
			ModbusRegisterMap.FAN_SUPPLY_2 + ModbusRegisterMap.FAN_PERCENT_OFFSET,
			ModbusRegisterMap.FAN_EXHAUST_1 + ModbusRegisterMap.FAN_PERCENT_OFFSET,
			ModbusRegisterMap.FAN_EXHAUST_2 + ModbusRegisterMap.FAN_PERCENT_OFFSET,
			ModbusRegisterMap.HEATER_ZONE_1 + ModbusRegisterMap.HEAT_PERCENT_OFFSET,
			ModbusRegisterMap.HEATER_ZONE_1 + ModbusRegisterMap.COOL_PERCENT_OFFSET,
			ModbusRegisterMap.HEATER_ZONE_2 + ModbusRegisterMap.HEAT_PERCENT_OFFSET,
			ModbusRegisterMap.HEATER_ZONE_2 + ModbusRegisterMap.COOL_PERCENT_OFFSET,
			ModbusRegisterMap.HEATER_ZONE_1 + ModbusRegisterMap.TARGET_TEMPERATURE_OFFSET,
			ModbusRegisterMap.HEATER_ZONE_2 + ModbusRegisterMap.TARGET_TEMPERATURE_OFFSET,
			ModbusRegisterMap.HEATER_ZONE_1 + ModbusRegisterMap.TEMPERATURE_CONTROL_OFFSET,
			ModbusRegisterMap.HEATER_ZONE_2 + ModbusRegisterMap.TEMPERATURE_CONTROL_OFFSET,
			ModbusRegisterMap.HEATER_ZONE_1 + ModbusRegisterMap.INFLOW_TARGET_OFFSET,
			ModbusRegisterMap.HEATER_ZONE_2 + ModbusRegisterMap.INFLOW_TARGET_OFFSET,
			ModbusRegisterMap.HEATER_ZONE_1 + ModbusRegisterMap.OUTFLOW_TARGET_OFFSET,
			ModbusRegisterMap.HEATER_ZONE_2 + ModbusRegisterMap.OUTFLOW_TARGET_OFFSET,
			ModbusRegisterMap.HEATER_ZONE_1 + ModbusRegisterMap.ORP_TARGET_T1_OFFSET,
			ModbusRegisterMap.HEATER_ZONE_1 + ModbusRegisterMap.ORP_CURRENT_T1_OFFSET,
			ModbusRegisterMap.HEATER_ZONE_1 + ModbusRegisterMap.ORP_TARGET_T2_OFFSET,
			ModbusRegisterMap.HEATER_ZONE_1 + ModbusRegisterMap.ORP_CURRENT_T2_OFFSET,
			ModbusRegisterMap.HEATER_ZONE_1 + ModbusRegisterMap.ORP_TARGET_T3_OFFSET,
			ModbusRegisterMap.HEATER_ZONE_1 + ModbusRegisterMap.ORP_CURRENT_T3_OFFSET,
			ModbusRegisterMap.HEATER_ZONE_2 + ModbusRegisterMap.ORP_TARGET_T1_OFFSET,
			ModbusRegisterMap.HEATER_ZONE_2 + ModbusRegisterMap.ORP_CURRENT_T1_OFFSET,
			ModbusRegisterMap.HEATER_ZONE_2 + ModbusRegisterMap.ORP_TARGET_T2_OFFSET,
			ModbusRegisterMap.HEATER_ZONE_2 + ModbusRegisterMap.ORP_CURRENT_T2_OFFSET,
			ModbusRegisterMap.HEATER_ZONE_2 + ModbusRegisterMap.ORP_TARGET_T3_OFFSET,
			ModbusRegisterMap.HEATER_ZONE_2 + ModbusRegisterMap.ORP_CURRENT_T3_OFFSET,
			ModbusRegisterMap.VENTILATION_MODE,
			ModbusRegisterMap.AIRFLOW_IMBALANCE,
			ModbusRegisterMap.RECUPERATOR_MAX_RPM
		));
		for (int i = 0; i < ModbusRegisterMap.SCHEDULE_COUNT; i++) {
			int base = ModbusRegisterMap.scheduleBase(i);
			allRegs.add(base);
			allRegs.add(base + ModbusRegisterMap.SCHEDULE_STATE_OFFSET);
		}

		Map<Integer, Integer> data =  modbusClient.readRegisters(SAUNA_PLC, allRegs);

		List<ClimateSensorDto> climate = Arrays.asList(
			mapClimate("BT3", "T1", ModbusRegisterMap.SENSOR_T1, data),
			mapClimate("BT11", "T2", ModbusRegisterMap.SENSOR_T2, data),
			mapClimate("BT10", "T3", ModbusRegisterMap.SENSOR_T3, data),
			mapClimate("BT1", "T4-1", ModbusRegisterMap.SENSOR_T4_1, data),
			mapClimate("BT2", "T5", ModbusRegisterMap.SENSOR_T5, data),
			mapClimate("BT21", "T4-2", ModbusRegisterMap.SENSOR_T4_2, data),
			mapClimate("BT8", "BT8", ModbusRegisterMap.SENSOR_TH1, data),
			mapClimate("BT9", "BT9", ModbusRegisterMap.SENSOR_TC1, data),
			mapClimate("BT24", "BT24", ModbusRegisterMap.SENSOR_TH2, data),
			mapClimate("BT27", "BT27", ModbusRegisterMap.SENSOR_TC2, data),
			mapZoneTemperature("T_ZONE1", "T_ZONE1",
				ModbusRegisterMap.HEATER_ZONE_1 + ModbusRegisterMap.TARGET_TEMPERATURE_OFFSET, data),
			mapZoneTemperature("T_ZONE2", "T_ZONE2",
				ModbusRegisterMap.HEATER_ZONE_2 + ModbusRegisterMap.TARGET_TEMPERATURE_OFFSET, data)
		);

		Double targetRight = decodeFloat(data, ModbusRegisterMap.AIRFLOW_TARGET);
		Double imbalance = getValidScaledSigned(data.get(ModbusRegisterMap.AIRFLOW_IMBALANCE), 10.0);
		Double targetLeft = targetRight;
		if (targetRight != null && imbalance != null) {
			targetLeft = targetRight * (1.0 - imbalance / 100.0);
		}
		
		List<FanFlowData> fans = Arrays.asList(
			mapFan("BF11", "Давление/Вентилятор 1", ModbusRegisterMap.PRESSURE_FAN_SUPPLY_1, ModbusRegisterMap.FAN_SUPPLY_1, targetRight, data),
			mapFan("BF12", "Давление/Вентилятор 2", ModbusRegisterMap.PRESSURE_FAN_SUPPLY_2, ModbusRegisterMap.FAN_SUPPLY_2, targetRight, data),
			mapFan("BF21", "Давление/Вентилятор 3", ModbusRegisterMap.PRESSURE_FAN_EXHAUST_1, ModbusRegisterMap.FAN_EXHAUST_1, targetLeft, data),
			mapFan("BF22", "Давление/Вентилятор 4", ModbusRegisterMap.PRESSURE_FAN_EXHAUST_2, ModbusRegisterMap.FAN_EXHAUST_2, targetLeft, data)
		);
		 

		// --- ДАВЛЕНИЕ (W)  ---
		List<SensorData> wSensors = Arrays.asList(
			createSensor("BP1", "Давление фильтра НВ", decodeFloat(data, ModbusRegisterMap.PRESSURE_FILTER_SUPPLY), "Pa"),
			createSensor("BP2", "Давление фильтра ОВ", decodeFloat(data, ModbusRegisterMap.PRESSURE_FILTER_EXHAUST), "Pa")
		);

		

		// --- ДАВЛЕНИЕ (Рекуп)  ---
		List<SensorData> pressureSensors = Arrays.asList(
			createSensor("BP5", "Давление рекуператора ОВ-ВВ", decodeFloat(data, ModbusRegisterMap.PRESSURE_RECUP_EXHAUST), "Pa"),
			createSensor("BP10", "Давление рекуператора НВ-ОВ", decodeFloat(data, ModbusRegisterMap.PRESSURE_RECUP_SUPPLY), "Pa")
		);

		// --- КЛАПАНЫ ---
		List<SensorData> valves = Arrays.asList(
			createSensor("VH1", "Нагрев зона 1", getValidScaledSigned(data.get(ModbusRegisterMap.HEATER_ZONE_1 + ModbusRegisterMap.HEAT_PERCENT_OFFSET), ModbusRegisterMap.VALVE_PERCENT_SCALE), "%"),
			createSensor("VC1", "Охлаждение зона 1", getValidScaledSigned(data.get(ModbusRegisterMap.HEATER_ZONE_1 + ModbusRegisterMap.COOL_PERCENT_OFFSET), ModbusRegisterMap.VALVE_PERCENT_SCALE), "%"),
			createSensor("VH2", "Нагрев зона 2", getValidScaledSigned(data.get(ModbusRegisterMap.HEATER_ZONE_2 + ModbusRegisterMap.HEAT_PERCENT_OFFSET), ModbusRegisterMap.VALVE_PERCENT_SCALE), "%"),
			createSensor("VC2", "Охлаждение зона 2", getValidScaledSigned(data.get(ModbusRegisterMap.HEATER_ZONE_2 + ModbusRegisterMap.COOL_PERCENT_OFFSET), ModbusRegisterMap.VALVE_PERCENT_SCALE), "%")
		);

		List<SensorData> debugValveSensors = Arrays.asList(
			createSensor("VHP1", "Положение вентиля ГВ зона 1", getValidScaledSigned(data.get(REG_VALVE_HEAT_POS1), ModbusRegisterMap.VALVE_PERCENT_SCALE), "%"),
			createSensor("VCP1", "Положение вентиля ХВ зона 1", getValidScaledSigned(data.get(REG_VALVE_COOL_POS1), ModbusRegisterMap.VALVE_PERCENT_SCALE), "%"),
			createSensor("VOP1", "% доп. открытия вентиля ГВ зона 1", getValidScaledSigned(data.get(REG_VALVE_OPEN_PROC1), ModbusRegisterMap.VALVE_PERCENT_SCALE), "%"),
			createSensor("VHP2", "Положение вентиля ГВ зона 2", getValidScaledSigned(data.get(REG_VALVE_HEAT_POS2), ModbusRegisterMap.VALVE_PERCENT_SCALE), "%"),
			createSensor("VCP2", "Положение вентиля ХВ зона 2", getValidScaledSigned(data.get(REG_VALVE_COOL_POS2), ModbusRegisterMap.VALVE_PERCENT_SCALE), "%"),
			createSensor("VOP2", "% доп. открытия вентиля ГВ зона 2", getValidScaledSigned(data.get(REG_VALVE_OPEN_PROC2), ModbusRegisterMap.VALVE_PERCENT_SCALE), "%")
		);

		

		// --- РЕКУПЕРАТОР ---
		Integer recupSpeed = roundToInteger(getValidScaledSigned(
			data.get(ModbusRegisterMap.RECUPERATOR_CURRENT_RPM), 100.0));
		Double efficiency = calcEfficiency(data);
		List<SensorData> recuperator = Arrays.asList(
			createSensor("R_EFFICIENCY", "КПД", efficiency, "%")
		);

		//  -- РЕКУПЕРАТОР ШАГ и БЫСТРАЯ СМЕНА РЕЖИМА --
		

		Integer stateCode = data.get(ModbusRegisterMap.AGGREGATE_STATE);
		String stateName = resolveRecuperatorStateName(stateCode);
		boolean isEffectivelyOff = stateCode == null || stateCode == 0 || (stateCode & 1) != 0 || stateCode == 128;
		
		RecuperatorStatusDto recuperatorStatus = RecuperatorStatusDto.builder()
			.enabled(!isEffectivelyOff)
			.stateCode(stateCode)
			.stateName(stateName)
			.build();

	
		Integer vm1 = sumFloatRegisters(data,
			ModbusRegisterMap.FAN_SUPPLY_1 + ModbusRegisterMap.FAN_AIRFLOW_OFFSET,
			ModbusRegisterMap.FAN_SUPPLY_2 + ModbusRegisterMap.FAN_AIRFLOW_OFFSET);
		Integer vm2 = sumFloatRegisters(data,
			ModbusRegisterMap.FAN_EXHAUST_1 + ModbusRegisterMap.FAN_AIRFLOW_OFFSET,
			ModbusRegisterMap.FAN_EXHAUST_2 + ModbusRegisterMap.FAN_AIRFLOW_OFFSET);
		
		Double manualAirflow = decodeFloat(data, ModbusRegisterMap.AIRFLOW_MANUAL);
		Double boostPercent = getValidScaledSigned(data.get(ModbusRegisterMap.AIRFLOW_IMBALANCE), 10.0);

		
		// --- MANUAL SETTINGS DTO ---
		ManualSettingsDto manualSettings = ManualSettingsDto.builder()
			.supplySpeed(manualAirflow)
			.boostPercent(boostPercent)
			.build();

		VolumeSettingsDto volumeSettings = VolumeSettingsDto.builder()
			.ecoAirflow(decodeFloat(data, ModbusRegisterMap.AIRFLOW_ECO))
			.normalAirflow(decodeFloat(data, ModbusRegisterMap.AIRFLOW_NORMAL))
			.intensiveAirflow(decodeFloat(data, ModbusRegisterMap.AIRFLOW_INTENSIVE))
			.manualAirflow(manualAirflow)
			.boostPercent(boostPercent)
			.build();
		
		// --- TEMP SETTINGS DTO ---
		Double waterWarn = getValidScaledSigned(data.get(REG_T_WATER_WARN), 10.0);
		Double waterFail = getValidScaledSigned(data.get(REG_T_WATER_FAIL), 10.0);
		Double inflowWarn = getValidScaledSigned(data.get(REG_T_INFLOW_WARN), 10.0);
		Double inflowFail = getValidScaledSigned(data.get(REG_T_INFLOW_FAIL), 10.0);
		
		Double inflowTargetZone1 = getValidScaledSigned(data.get(
			ModbusRegisterMap.HEATER_ZONE_1 + ModbusRegisterMap.INFLOW_TARGET_OFFSET), 10.0);
		Double inflowTargetZone2 = getValidScaledSigned(data.get(
			ModbusRegisterMap.HEATER_ZONE_2 + ModbusRegisterMap.INFLOW_TARGET_OFFSET), 10.0);
		Double outflowTargetZone1 = getValidScaledSigned(data.get(
			ModbusRegisterMap.HEATER_ZONE_1 + ModbusRegisterMap.OUTFLOW_TARGET_OFFSET), 10.0);
		Double outflowTargetZone2 = getValidScaledSigned(data.get(
			ModbusRegisterMap.HEATER_ZONE_2 + ModbusRegisterMap.OUTFLOW_TARGET_OFFSET), 10.0);
		Double outflowTarget = outflowTargetZone1 != null ? outflowTargetZone1 : outflowTargetZone2;
		Double targetZone1 = inflowTargetZone1;
		Double targetZone2 = inflowTargetZone2;
		OrpSettingsDto orpZone1 = readOrpSettings(data, ModbusRegisterMap.HEATER_ZONE_1);
		OrpSettingsDto orpZone2 = readOrpSettings(data, ModbusRegisterMap.HEATER_ZONE_2);
		
		Integer ctrlModeZone1 = data.get(REG_T_CTRL_MODE_ZONE1);
		Integer ctrlModeZone2 = data.get(REG_T_CTRL_MODE_ZONE2);

		//Задвижки
		Integer workStateRaw = data.get(ModbusRegisterMap.WORK_STATE);
		Boolean damperSupply = null;
		Boolean damperExhaust = null;
		if (workStateRaw != null) {
			damperSupply = (workStateRaw & MASK_DAMPER_SUPPLY) != 0;
			damperExhaust = (workStateRaw & MASK_DAMPER_EXHAUST) != 0;
			System.out.println("State = " + workStateRaw);
			System.out.println("Задвижка НВ (MG1) = " + (((workStateRaw & MASK_DAMPER_SUPPLY) > 0) ? 1 : 0));
			System.out.println("Задвижка ОВ (MG2) = " + (((workStateRaw & MASK_DAMPER_EXHAUST) > 0) ? 1 : 0));
		}

		// Workstate2: бит 14 — насос рекуператора (команда)
		Integer workState2Raw = data.get(REG_Z_WORKSTATE2);
		Boolean pumpActive = workState2Raw != null && (workState2Raw & MASK_PUMP_WORKSTATE) != 0;

		// целые значения 0, 1, 2, 3, 4
		Integer recupStateRaw = data.get(ModbusRegisterMap.RECUPERATOR_STATE);
		Integer recupStateCode = (recupStateRaw != null && recupStateRaw >= 0 && recupStateRaw <= 6) ? recupStateRaw : null;
		String recupStateName = recupStateCode != null ? RECUP_STATES.get(recupStateCode) : (recupStateRaw != null ? "Неизвестно (" + recupStateRaw + ")" : null);

		//Логи
		System.out.println("DEBUG: текущее состояние агрегата: " + stateCode + " - " + stateName);
		
		// --- НОВЫЕ РЕГИСТРЫ РЕКУПЕРАТОРА ---
		Double recupTargetRpm = getValidScaledSigned(data.get(ModbusRegisterMap.RECUPERATOR_TARGET_RPM), 100.0);
		Double recupCurrentRpm = getValidScaledSigned(data.get(ModbusRegisterMap.RECUPERATOR_CURRENT_RPM), 100.0);
		Double recupSpeedRpm = getValidScaledSigned(data.get(ModbusRegisterMap.RECUPERATOR_MOTOR_RPM), 10.0);
		Integer rawTempBt6 = data.get(REG_R_TEMP);
		Double recupTempBt6 = isValid(rawTempBt6) ? ((short) rawTempBt6.intValue()) / 10.0 : null;

		Double recupTargetRpmMax = getValidScaledSigned(data.get(ModbusRegisterMap.RECUPERATOR_MAX_RPM), 100.0);
		Integer recupTargetPercent = null;
		if (recupTargetRpm != null && recupTargetRpmMax != null && recupTargetRpmMax > 0) {
			recupTargetPercent = (int) Math.round((recupTargetRpm / recupTargetRpmMax) * 100);
		}
		Integer ventilationModeCode = getValidOrNull(data.get(ModbusRegisterMap.VENTILATION_MODE));
		Integer temperatureControlModeZone1Code = getValidOrNull(data.get(
			ModbusRegisterMap.HEATER_ZONE_1 + ModbusRegisterMap.TEMPERATURE_CONTROL_OFFSET));
		Integer temperatureControlModeZone2Code = getValidOrNull(data.get(
			ModbusRegisterMap.HEATER_ZONE_2 + ModbusRegisterMap.TEMPERATURE_CONTROL_OFFSET));
		Boolean screenMode = readScreenMode(data);

		// --- УСТАВКИ РЕКУПЕРАТОРА ---
		RecuperatorSettingsDto recuperatorSettings = RecuperatorSettingsDto.builder()
			.targetRpmMin(getValidScaled(data.get(REG_R_TARGET_RPM_MIN), 100.0))
			.targetRpmMax(getValidScaled(data.get(REG_R_TARGET_RPM_MAX), 100.0))
			.targetRpmStep(getValidScaled(data.get(REG_R_TARGET_RPM_STEP), 100.0))
			.trackingIntervalRecup(getValidOrNull(data.get(REG_R_TRACK_INTERVAL)))
			.build();
		
		TempSettingsDto tempSettings = TempSettingsDto.builder()
			.outflowTarget(outflowTarget)
			.inflowTargetZone1(inflowTargetZone1)
			.inflowTargetZone2(inflowTargetZone2)
			.orpZone1(orpZone1)
			.orpZone2(orpZone2)
			.valveStartupPosition(getValidOrNull(data.get(REG_T_VALVE_START_POS)))       // п.1: 0x00E1 положение вентилей при запуске
			.controlAlgorithm(getValidOrNull(data.get(REG_T_CTRL_ALGORITHM)))           // п.2: 0x00DD алгоритм управления
			.tempTrackingInterval(getValidOrNull(data.get(REG_T_TRACK_INTERVAL)))       // п.3: 0x007E интервал слежения по T
			.targetZone1(targetZone1)
			.targetZone2(targetZone2)
			.waterWarnThreshold(waterWarn)
			.waterFailThreshold(waterFail)
			.inflowWarnThreshold(inflowWarn)
			.inflowFailThreshold(inflowFail)
			.ctrlModeZone1(ctrlModeZone1)
			.ctrlModeZone2(ctrlModeZone2)
			.neutralZoneT(getValidScaledSigned(data.get(REG_T_NEUTRAL_ZONE), 10.0))
			.hotValveMinProc(getValidScaledSigned(data.get(REG_HOT_VALVE_MIN_PROC), 10.0))
			.waterMinTNoWork(getValidScaledSigned(data.get(REG_T_WATER_NO_WORK), 10.0))
			.waterMinTWork(getValidScaledSigned(data.get(REG_T_WATER_WORK), 10.0))
			.radTempFail(getValidScaledSigned(data.get(REG_T_RAD_FZ_FAIL), 10.0))
			.build();
		
		return DashboardData.builder()
			.climateSensors(climate)
			.wSensors(wSensors)
			.pressureSensors(pressureSensors)
			.fans(fans)
			.valves(valves)
			.recuperator(recuperator)
			.recuperatorStatus(recuperatorStatus)
			.vm2(vm2)
			.vm1(vm1)
			.recupSpeed(recupSpeed)
			.vManualProcIn(manualAirflow)
			.vManualDProcOut(boostPercent)
			.manualSettings(manualSettings)
			.volumeSettings(volumeSettings)
			.airflowTarget(targetRight)
			.airflowReal(vm1 == null ? null : vm1.doubleValue())
			.tempSettings(tempSettings)
			.recuperatorSettings(recuperatorSettings)
			.debugValveSensors(debugValveSensors) //22.01.2026
			.damperSupply(damperSupply)
			.damperExhaust(damperExhaust)
			.pumpActive(pumpActive)
			.recupStateCode(recupStateCode)
			.recupStateName(recupStateName)
			.recupTargetRpm(recupTargetRpm)
			.recupCurrentRpm(recupCurrentRpm)
			.recupSpeedRpm(recupSpeedRpm)
			.recupTempBt6(recupTempBt6)
			.recupTargetPercent(recupTargetPercent)
			.ventilationModeCode(ventilationModeCode)
			.ventilationModeName(ModbusRegisterMap.ventilationModeName(ventilationModeCode))
			.temperatureControlModeZone1Code(temperatureControlModeZone1Code)
			.temperatureControlModeZone1Name(ModbusRegisterMap.temperatureControlName(temperatureControlModeZone1Code))
			.temperatureControlModeZone2Code(temperatureControlModeZone2Code)
			.temperatureControlModeZone2Name(ModbusRegisterMap.temperatureControlName(temperatureControlModeZone2Code))
			.screenMode(screenMode)
			.calibration(CalibrationData.builder()
				.calibrating(stateCode != null && (stateCode & 32) != 0)
				.build())
			.build();
	}

	private OrpSettingsDto readOrpSettings(Map<Integer, Integer> data, int base) {
		return OrpSettingsDto.builder()
			.targetT1(getValidScaledSigned(data.get(base + ModbusRegisterMap.ORP_TARGET_T1_OFFSET), 1.0))
			.currentT1(getValidScaledSigned(data.get(base + ModbusRegisterMap.ORP_CURRENT_T1_OFFSET), 1.0))
			.targetT2(getValidScaledSigned(data.get(base + ModbusRegisterMap.ORP_TARGET_T2_OFFSET), 1.0))
			.currentT2(getValidScaledSigned(data.get(base + ModbusRegisterMap.ORP_CURRENT_T2_OFFSET), 1.0))
			.targetT3(getValidScaledSigned(data.get(base + ModbusRegisterMap.ORP_TARGET_T3_OFFSET), 1.0))
			.currentT3(getValidScaledSigned(data.get(base + ModbusRegisterMap.ORP_CURRENT_T3_OFFSET), 1.0))
			.build();
	}




	private ClimateSensorDto mapClimate(String id, String label, int register, Map<Integer, Integer> data) {
		Integer temp = data.get(register + ModbusRegisterMap.TEMPERATURE_OFFSET);
		Integer humid = data.get(register + ModbusRegisterMap.HUMIDITY_OFFSET);
		return ClimateSensorDto.builder()
			.id(id)
			.label(label)
			.temperature(isValid(temp) ? ((short) temp.intValue()) / 10.0 : null)
			.humidity(getValidScaled(humid, 10.0))
			.build();
	}

	private ClimateSensorDto mapZoneTemperature(String id, String label, int register, Map<Integer, Integer> data) {
		Integer temp = data.get(register);
		return ClimateSensorDto.builder()
			.id(id)
			.label(label)
			.temperature(getValidScaledSigned(temp, 10.0))
			.humidity(null)
			.build();
	}

	private FanFlowData mapFan(String id, String type, int pressureRegister, int fanBase, Double targetFlow, Map<Integer, Integer> data) {
		Double pressure = decodeFloat(data, pressureRegister);
		Double airflow = decodeFloat(data, fanBase + ModbusRegisterMap.FAN_AIRFLOW_OFFSET);
		Integer speedPercRaw = data.get(fanBase + ModbusRegisterMap.FAN_PERCENT_OFFSET);
		
		return FanFlowData.builder()
			.type(type)
			.pressure(pressure)
			.speedPerc(roundToInteger(getValidScaledSigned(speedPercRaw, 100.0)))
			.vm(roundToInteger(airflow))
			.targetAirFlow(targetFlow)
			.build();
	}

	private SensorData createSensor(String id, String name, Integer rawValue, String unit, double divisor) {
		Double value = getValidScaled(rawValue, divisor);
		return SensorData.builder()
			.id(id)
			.name(name)
			.value(value != null ? String.valueOf(value) : null)
			.unit(unit)
			.build();
	}

	private SensorData createSensor(String id, String name, Integer rawValue, String unit) {
		return createSensor(id, name, rawValue, unit, 10.0);
	}

	private void addFloatRegister(List<Integer> registers, int lowRegister) {
		registers.add(lowRegister);
		registers.add(lowRegister + 1);
	}

	private Double decodeFloat(Map<Integer, Integer> data, int lowRegister) {
		return ModbusRegisterMap.decodeFloat(data.get(lowRegister), data.get(lowRegister + 1));
	}

	private Integer roundToInteger(Double value) {
		return value == null ? null : (int) Math.round(value);
	}

	private Integer sumFloatRegisters(Map<Integer, Integer> data, int first, int second) {
		Double firstValue = decodeFloat(data, first);
		Double secondValue = decodeFloat(data, second);
		return firstValue == null || secondValue == null ? null : (int) Math.round(firstValue + secondValue);
	}

	private Integer findUserSetScheduleBase(Map<Integer, Integer> data) {
		for (int i = 0; i < ModbusRegisterMap.SCHEDULE_COUNT; i++) {
			int base = ModbusRegisterMap.scheduleBase(i);
			if (Integer.valueOf(ModbusRegisterMap.SCHEDULE_USERSET_MODE).equals(data.get(base))) {
				return base;
			}
		}
		return null;
	}

	private Boolean readScreenMode(Map<Integer, Integer> data) {
		Integer userSetBase = findUserSetScheduleBase(data);
		if (userSetBase == null) {
			return null;
		}
		Integer state = data.get(userSetBase + ModbusRegisterMap.SCHEDULE_STATE_OFFSET);
		return state == null || state == INVALID_VALUE
			? null
			: (state & ModbusRegisterMap.SCHEDULE_SELECTED_MASK) != 0;
	}

	// == HELPER METHODS
	private boolean isValid(Integer value) {
		return value != null && value != INVALID_VALUE;
	}
	
	private Integer getValidOrNull(Integer value) {
		return isValid(value) ? value : null;
	}

	private Double getValidScaled(Integer value, double divisor) {
		return isValid(value) ? value / divisor : null;
	}

	private Integer getValidScaled(Integer value, int divisor) {
		return isValid(value) ? value / divisor : null;
	}

	/** Для 16-битных знаковых значений (температура, скорость и т.п.) — каст к short перед делением */
	private Double getValidScaledSigned(Integer value, double divisor) {
		return isValid(value) ? ((short) value.intValue()) / divisor : null;
	}

	/** Signed 16-bit → Integer (без деления) */
	private Integer getValidSignedOrNull(Integer value) {
		return isValid(value) ? (int) (short) value.intValue() : null;
	}

	/** Signed 16-bit → Integer (с целочисленным делением) */
	private Integer getValidScaledSignedInt(Integer value, int divisor) {
		return isValid(value) ? ((short) value.intValue()) / divisor : null;
	}

	private String resolveRecuperatorStateName(Integer stateCode) {
		if (stateCode == null) {
			return "Неизвестно";
		}
		
		String direct = SYSTEM_STATES.get(stateCode);
		if (direct != null) {
			return direct;
		}
		
		if ((stateCode & MASK_STATE_VENTILATION) != 0) {
			return SYSTEM_STATES.getOrDefault(MASK_STATE_VENTILATION, "Вентилирование");
		}
		
		if ((stateCode & MASK_STATE_ALARM) != 0) {
			return SYSTEM_STATES.getOrDefault(MASK_STATE_ALARM, "Отключение по сбою");
		}
		
		return "Неизвестно (" + stateCode + ")";
	}

	private SensorData createSensor(String id, String name, Double value, String unit) {
		return SensorData.builder()
			.id(id)
			.name(name)
			.value(value != null ? String.valueOf(value) : null)
			.unit(unit)
			.build();
	}

	private Double calcEfficiency(Map<Integer, Integer> data) {
		Double bt3 = getTempFromReg(data, ModbusRegisterMap.SENSOR_T1);
		Double bt10 = getTempFromReg(data, ModbusRegisterMap.SENSOR_T3);
		Double bt2 = getTempFromReg(data, ModbusRegisterMap.SENSOR_T5);
		if (bt3 == null || bt10 == null || bt2 == null) return null;
		double denom = Math.abs(bt3 - bt2);
		if (denom < 1e-6) return null;
		double efficiency = 100.0 * Math.abs(bt3 - bt10) / denom;
		return Math.min(efficiency, 100.0); // не может быть больше 100%
	}

	private Double getTempFromReg(Map<Integer, Integer> data, int reg) {
		Integer raw = data.get(reg + ModbusRegisterMap.TEMPERATURE_OFFSET);
		return isValid(raw) ? ((short) raw.intValue()) / 10.0 : null;
	}
}
