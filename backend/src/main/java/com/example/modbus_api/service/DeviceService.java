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

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.io.IOException;

import static com.example.modbus_api.service.ModbusConstants.*;


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
		String stateName = currentState != null ? SYSTEM_STATES.getOrDefault(currentState, "Неизвестно (" + currentState + ")") : "Неизвестно";
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
		String stateName = currentState != null ? SYSTEM_STATES.getOrDefault(currentState, "Неизвестно (" + currentState + ")") : "Неизвестно";
		System.out.println("DEBUG: Команда выключения отправлена, текущее состояние: " + currentState + " - " + stateName);
	}

	public DashboardData getDashboardData() throws IOException {

		List<Integer> allRegs = new ArrayList<>(Arrays.asList(
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

			// --- ТЕМПЕРАТУРЫ ЗОН ---
			REG_T_ZONE1,
			REG_T_ZONE2,

			// --- РЕКУПЕРАТОР ---
			REG_R_RECUP_SPEED,
			REG_R_CURRENT,
			REG_R_TARGET,
			REG_R_EFFICIENCY,
			REG_R_STATE,
			TARGET_R_REG,  // Обороты вала рекуператора (целевые) - делить на 100
			CURRENT_R_REG, // Обороты вала - делить на 100
			SPEED_R_REG,   // Скорость вращения - делить на 10

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
			REG_VALVE_OPEN_PROC2
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
			createSensor("BP1", "Давление \"W right\"", data.get(REG_P_BP1), "Pa"),
			createSensor("BP2", "Давление \"W left\"", data.get(REG_P_BP2), "Pa")
		);

		

		// --- ДАВЛЕНИЕ (Рекуп)  ---
		List<SensorData> pressureSensors = Arrays.asList(
			createSensor("BP5", "Давление \"слева от рекупа\"", data.get(REG_P_BP5), "Pa"),
			createSensor("BP10", "Давление \"справа от рекупа\"", data.get(REG_P_BP10), "Pa")
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
		Integer recupSpeed = getValidOrNull(data.get(REG_R_RECUP_SPEED));
		// Integer recupCurrentRaw = data.get(REG_R_CURRENT);
		// Integer recupCurrent = (recupCurrentRaw != null && recupCurrentRaw != INVALID_VALUE) ? recupCurrentRaw / 10 : null; 
		Integer recupCurrent = getValidScaled(data.get(REG_R_CURRENT), 10); 
		System.out.println("DEBUG: recupCurrent: " + recupCurrent);
		List<SensorData> recuperator = Arrays.asList(
			createSensor("R_EFFICIENCY", "КПД", data.get(REG_R_EFFICIENCY), "%", 1.0)
		);

		Integer stateCode = data.get(REG_R_STATE);
		String stateName = stateCode != null ? SYSTEM_STATES.getOrDefault(stateCode, "Неизвестно") : "Неизвестно";
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

		//Логи
		System.out.println("DEBUG: текущее состояние агрегата: " + stateCode + " - " + stateName);
		
		// --- НОВЫЕ РЕГИСТРЫ РЕКУПЕРАТОРА ---
		Double recupTargetRpm = getValidScaled(data.get(TARGET_R_REG), 100.0);  // Обороты вала рекуператора (целевые)
		Double recupCurrentRpm = getValidScaled(data.get(CURRENT_R_REG), 100.0); // Обороты вала
		Double recupSpeedRpm = getValidScaled(data.get(SPEED_R_REG), 10.0);      // Скорость вращения
		
		TempSettingsDto tempSettings = TempSettingsDto.builder()
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
			.recupCurrent(recupCurrent)
			.vManualProcIn(vManualProcIn)
			.vManualDProcOut(vManualDProcOut)
			.manualSettings(manualSettings)
			.tempSettings(tempSettings)
			.debugValveSensors(debugValveSensors) //22.01.2026
			.damperSupply(damperSupply)
			.damperExhaust(damperExhaust)
			.recupTargetRpm(recupTargetRpm)
			.recupCurrentRpm(recupCurrentRpm)
			.recupSpeedRpm(recupSpeedRpm)
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
			.humidity(humid != null ? humid.doubleValue() : null)
			.build();
	}

	private FanFlowData mapFan(String id, String type, Integer pressureRegister, Integer vm, Double targetFlow, Map<Integer, Integer> data, Integer speedPerc) {
		Integer pressureRaw = data.get(pressureRegister);
		Integer vmRaw = data.get(vm);
		Integer speedPercRaw = data.get(speedPerc);
		
		return FanFlowData.builder()
			.type(type)
			.pressure(getValidScaled(pressureRaw, 10.0))
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
}
