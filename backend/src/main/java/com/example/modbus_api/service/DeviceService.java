package com.example.modbus_api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.modbus_api.dto.SensorData;
import com.example.modbus_api.dto.DashboardData;
import com.example.modbus_api.dto.ClimateSensorDto;
import com.example.modbus_api.dto.FanFlowData;
import com.example.modbus_api.dto.RecuperatorStatusDto;
import com.example.modbus_api.dto.ManualSettingsDto;
import com.example.modbus_api.dto.TempSettingsDto;
import com.example.modbus_api.dto.RecuperatorSettingsDto;

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.io.IOException;

import static com.example.modbus_api.service.ModbusConstants.*;
import com.example.modbus_api.modbus.ModbusAPIClient;
import com.example.modbus_api.modbus.legacy.ArchESSEvent;
import com.example.modbus_api.modbus.legacy.IntegerList;


@Service
public class DeviceService {
	private final ModbusClientAdapter modbusClient;

	@Autowired
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

	public List<ArchESSEvent> getSystemEvents(int lastMinutes) throws IOException {
		modbusClient.ensureConnection();

		IntegerList types = new IntegerList();
		types.add(16); // EventPLC - Сообщения модуля управления
		types.add(3);  // EventExternal
		types.add(4);  // EventSystem
		types.add(6);  // EventFailure (Аварии)

		ArrayList<ArchESSEvent> events = new ModbusAPIClient.APICall<ArrayList<ArchESSEvent>>() {
			@Override
			public retrofit2.Call<ArrayList<ArchESSEvent>> apiFun() {
				return modbusClient.getApi().getEventsList(
					modbusClient.getSessionToken(),
					lastMinutes,
					types
				);
			}
		}.call();

		return events != null ? events : new ArrayList<>();
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

		Map<Integer, Integer> data =  modbusClient.readRegisters(SAUNA_PLC, allRegs);

		List<ClimateSensorDto> climate = Arrays.asList(
			// --- ТЕМПЕРАТУРА (тип 1) ---
			mapClimate("BT3", "T1", REG_T_BT3, data),
			mapClimate("BT11", "T2", REG_T_BT11, data),
			mapClimate("BT10", "T3", REG_T_BT10, data),
			mapClimate("BT1", "T4-1", REG_T_BT1, data),
			mapClimate("BT2", "T5", REG_T_BT2, data),
			mapClimate("BT21", "T4-2", REG_T_BT21, data),

			// --- ТЕМПЕРАТУРА (тип 2) ---
			mapClimate("BT8", "BT8", REG_T_BT8, data),
			mapClimate("BT9", "BT9", REG_T_BT9, data),
			mapClimate("BT24", "BT24", REG_T_BT24, data),
			mapClimate("BT27", "BT27", REG_T_BT27, data),

			// --- ТЕМПЕРАТУРЫ ЗОН ---
			mapZoneTemperature("T_ZONE1", "T_ZONE1", REG_T_ZONE1, data),
			mapZoneTemperature("T_ZONE2", "T_ZONE2", REG_T_ZONE2, data)
		);

		// --- ДАВЛЕНИЕ (Вентиляторы) ---
		Integer targetFlow = data.get(REG_F_TARGET);
		Integer powerProc = data.get(REG_POWERPROC);
		
		Double targetRight = null;
		if (targetFlow != null && powerProc != null) {
			targetRight = (100.0 + powerProc / 10.0) / 100.0 * targetFlow;
		}
		
		Double targetLeft = targetFlow != null ? targetFlow.doubleValue() : null;
		
		List<FanFlowData> fans = Arrays.asList(
			mapFan("BF11", "Давление/Вентилятор 1", REG_P_BF11, REG_P_VM11, targetRight, data, REG_F_G11),
			mapFan("BF12", "Давление/Вентилятор 2", REG_P_BF12,  REG_P_VM12, targetRight, data, REG_F_G12),
			mapFan("BF21", "Давление/Вентилятор 3", REG_P_BF21, REG_P_VM21, targetLeft, data, REG_F_G21),
			mapFan("BF22", "Давление/Вентилятор 4", REG_P_BF22, REG_P_VM22, targetLeft, data, REG_F_G22)
		);
		 

		// --- ДАВЛЕНИЕ (W)  ---
		List<SensorData> wSensors = Arrays.asList(
			createSensor("BP1", "Давление \"W right\"", data.get(REG_P_BP1), "Pa", 1.0),
			createSensor("BP2", "Давление \"W left\"", data.get(REG_P_BP2), "Pa", 1.0)
		);

		

		// --- ДАВЛЕНИЕ (Рекуп)  ---
		List<SensorData> pressureSensors = Arrays.asList(
			createSensor("BP5", "Давление \"слева от рекупа\"", data.get(REG_P_BP5), "Pa", 1.0),
			createSensor("BP10", "Давление \"справа от рекупа\"", data.get(REG_P_BP10), "Pa", 1.0)
		);

		// --- КЛАПАНЫ ---
		List<SensorData> valves = Arrays.asList(
			createSensor("VH1", "Клапан + слева", data.get(REG_F_VH1), "%"),
			createSensor("VC1", "Клапан - справа", data.get(REG_F_VC1), "%"),
			createSensor("VH2", "Клапан + справа", data.get(REG_F_VH2), "%"),
			createSensor("VC2", "Клапан - справа", data.get(REG_F_VC2), "%")
		);

		List<SensorData> debugValveSensors = Arrays.asList(
			createSensor("VHP1", "Положение вентиля ГВ зона 1", data.get(REG_VALVE_HEAT_POS1), "%"),
			createSensor("VCP1", "Положение вентиля ХВ зона 1", data.get(REG_VALVE_COOL_POS1), "%"),
			createSensor("VOP1", "% доп. открытия вентиля ГВ зона 1", data.get(REG_VALVE_OPEN_PROC1), "%"),
			createSensor("VHP2", "Положение вентиля ГВ зона 2", data.get(REG_VALVE_HEAT_POS2), "%"),
			createSensor("VCP2", "Положение вентиля ХВ зона 2", data.get(REG_VALVE_COOL_POS2), "%"),
			createSensor("VOP2", "% доп. открытия вентиля ГВ зона 2", data.get(REG_VALVE_OPEN_PROC2), "%")
		);

		

		// --- РЕКУПЕРАТОР ---
		Integer recupSpeed = getValidSignedOrNull(data.get(REG_R_RECUP_SPEED));
		Double efficiency = calcEfficiency(data);
		List<SensorData> recuperator = Arrays.asList(
			createSensor("R_EFFICIENCY", "КПД", efficiency, "%")
		);

		//  -- РЕКУПЕРАТОР ШАГ и БЫСТРАЯ СМЕНА РЕЖИМА --
		

		Integer stateCode = data.get(REG_R_STATE);
		String stateName = resolveRecuperatorStateName(stateCode);
		boolean isEffectivelyOff = stateCode == null || stateCode == 0 || (stateCode & 1) != 0 || stateCode == 128;
		
		RecuperatorStatusDto recuperatorStatus = RecuperatorStatusDto.builder()
			.enabled(!isEffectivelyOff)
			.stateCode(stateCode)
			.stateName(stateName)
			.build();

	
		Integer vm1 = getValidOrNull(data.get(REG_F_VM1));
		Integer vm2 = getValidOrNull(data.get(REG_F_VM2));
		
		// --- MANUAL PROC ---
		Double vManualProcIn = getValidScaled(data.get(REG_V_VManualProcIn), 10.0);
		Double vManualDProcOut = getValidScaled(data.get(REG_V_VManualDProcOut), 10.0);

		
		// --- MANUAL SETTINGS DTO ---
		ManualSettingsDto manualSettings = ManualSettingsDto.builder()
			.supplySpeed(vManualProcIn)
			.boostPercent(vManualDProcOut)
			.build();
		
		// --- TEMP SETTINGS DTO ---
		Double waterWarn = getValidScaled(data.get(REG_T_WATER_WARN), 10.0);
		Double waterFail = getValidScaled(data.get(REG_T_WATER_FAIL), 10.0);
		Double inflowWarn = getValidScaled(data.get(REG_T_INFLOW_WARN), 10.0);
		Double inflowFail = getValidScaled(data.get(REG_T_INFLOW_FAIL), 10.0);
		
		// Целевые температуры зон берутся напрямую из REG_T_ZONE1/ZONE2 (уже прочитаны выше)
		Double targetZone1 = data.get(REG_T_ZONE1) != null ? data.get(REG_T_ZONE1).doubleValue() : null;
		Double targetZone2 = data.get(REG_T_ZONE2) != null ? data.get(REG_T_ZONE2).doubleValue() : null;
		
		Integer ctrlModeZone1 = data.get(REG_T_CTRL_MODE_ZONE1);
		Integer ctrlModeZone2 = data.get(REG_T_CTRL_MODE_ZONE2);

		//Задвижки
		Integer workStateRaw = data.get(REG_Z_WORKSTATE);
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
		Integer recupStateRaw = data.get(REG_RECUP_STATE);
		Integer recupStateCode = (recupStateRaw != null && recupStateRaw >= 0 && recupStateRaw <= 4) ? recupStateRaw : null;
		String recupStateName = recupStateCode != null ? RECUP_STATES.get(recupStateCode) : (recupStateRaw != null ? "Неизвестно (" + recupStateRaw + ")" : null);

		//Логи
		System.out.println("DEBUG: текущее состояние агрегата: " + stateCode + " - " + stateName);
		
		// --- НОВЫЕ РЕГИСТРЫ РЕКУПЕРАТОРА ---
		Double recupTargetRpm = getValidScaled(data.get(TARGET_R_REG), 100.0);  // Обороты вала рекуператора (целевые)
		Double recupCurrentRpm = getValidScaled(data.get(CURRENT_R_REG), 100.0); // Обороты вала
		Double recupSpeedRpm = getValidScaledSigned(data.get(SPEED_R_REG), 10.0); // Скорость вращения 0x0082 (signed 16-bit)
		Integer rawTempBt6 = data.get(REG_R_TEMP);
		Double recupTempBt6 = isValid(rawTempBt6) ? ((short) rawTempBt6.intValue()) / 10.0 : null;

		Double recupTargetRpmMax = getValidScaled(data.get(REG_R_TARGET_RPM_MAX), 10.0);
		Integer recupTargetPercent = null;
		if (recupTargetRpm != null && recupTargetRpmMax != null && recupTargetRpmMax > 0) {
			recupTargetPercent = (int) Math.round((recupTargetRpm / recupTargetRpmMax) * 100);
		}

		// --- УСТАВКИ РЕКУПЕРАТОРА (25.01.2026) ---
		RecuperatorSettingsDto recuperatorSettings = RecuperatorSettingsDto.builder()
			.targetRpmMin(getValidScaled(data.get(REG_R_TARGET_RPM_MIN), 10.0))   // Минимальные целевые обороты вала рекуператора в оборотах в минуту (масштаб ×10 в регистре)
			.targetRpmMax(getValidScaled(data.get(REG_R_TARGET_RPM_MAX), 10.0))   // Максимальные целевые обороты вала рекуператора в оборотах в минуту (масштаб ×10 в регистре)
			.targetRpmStep(getValidScaled(data.get(REG_R_TARGET_RPM_STEP), 10.0))  // Шаг калибровки целевых оборотов рекуператора в оборотах в минуту (масштаб ×10 в регистре)
			.trackingIntervalRecup(getValidOrNull(data.get(REG_R_TRACK_INTERVAL))) // п.4 приоритета 0: 0x0064 интервал слежения по рекуператору, сырое
			.build();
		
		TempSettingsDto tempSettings = TempSettingsDto.builder()
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
			.neutralZoneT(getValidScaled(data.get(REG_T_NEUTRAL_ZONE), 10.0))
			.hotValveMinProc(getValidScaled(data.get(REG_HOT_VALVE_MIN_PROC), 10.0))
			.waterMinTNoWork(getValidScaled(data.get(REG_T_WATER_NO_WORK), 10.0))
			.waterMinTWork(getValidScaled(data.get(REG_T_WATER_WORK), 10.0))
			.radTempFail(getValidScaled(data.get(REG_T_RAD_FZ_FAIL), 10.0))
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
			.vManualProcIn(vManualProcIn)
			.vManualDProcOut(vManualDProcOut)
			.manualSettings(manualSettings)
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
			.build();
	}




	private ClimateSensorDto mapClimate(String id, String label, int register, Map<Integer, Integer> data) {
		Integer temp = data.get(register + OFFSET_TEMP);
		Integer humid = data.get(register + OFFSET_HUMID);
		return ClimateSensorDto.builder()
			.id(id)
			.label(label)
			.temperature(isValid(temp) ? ((short) temp.intValue()) / 10.0 : null)
			.humidity(getValidScaled(humid, 10.0))
			.build();
	}

	private ClimateSensorDto mapZoneTemperature(String id, String label, int register, Map<Integer, Integer> data) {
		Integer temp = data.get(register + OFFSET_TEMP);
		Integer humid = data.get(register + OFFSET_HUMID);
		return ClimateSensorDto.builder()
			.id(id)
			.label(label)
			.temperature(temp != null ? temp.doubleValue() : null)
			.humidity(getValidScaled(humid, 10.0))
			.build();
	}

	private FanFlowData mapFan(String id, String type, Integer pressureRegister, Integer vm, Double targetFlow, Map<Integer, Integer> data, Integer speedPerc) {
		Integer pressureRaw = data.get(pressureRegister);
		Integer vmRaw = data.get(vm);
		Integer speedPercRaw = data.get(speedPerc);
		
		return FanFlowData.builder()
			.type(type)
			.pressure(getValidScaled(pressureRaw, 1.0))
			.speedPerc(isValid(speedPercRaw) ? (int)(speedPercRaw / 100.0) : null)
			.vm(getValidOrNull(vmRaw))
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
		Double bt3 = getTempFromReg(data, REG_T_BT3);
		Double bt10 = getTempFromReg(data, REG_T_BT10);
		Double bt2 = getTempFromReg(data, REG_T_BT2);
		if (bt3 == null || bt10 == null || bt2 == null) return null;
		double denom = Math.abs(bt3 - bt2);
		if (denom < 1e-6) return null;
		double efficiency = 100.0 * Math.abs(bt3 - bt10) / denom;
		return Math.min(efficiency, 100.0); // не может быть больше 100%
	}

	private Double getTempFromReg(Map<Integer, Integer> data, int reg) {
		Integer raw = data.get(reg + OFFSET_TEMP);
		return isValid(raw) ? ((short) raw.intValue()) / 10.0 : null;
	}
}
