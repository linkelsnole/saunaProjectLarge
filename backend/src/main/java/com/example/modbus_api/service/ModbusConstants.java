package com.example.modbus_api.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ModbusConstants {
	public static final String SAUNA_PLC = "Sauna7000PLC";

	public static final Map<Integer, String> SYSTEM_STATES;
	static {
			Map<Integer, String> states = new HashMap<>();
			states.put(1, "Выключено");
			states.put(2, "Крах: коммуникации (modbus)");
			states.put(4, "Отключение по сбою");
			states.put(8, "Быстрый запуск");
			states.put(16, "Начальный запуск");
			states.put(32, "Калибровка");
			states.put(64, "Вентилирование");
			states.put(128, "Остановка агрегата, выбег");
			states.put(256, "Сбой");
			states.put(512, "Авария");
			states.put(1024, "Крах: заморозка");
			states.put(2048, "Крах");
			SYSTEM_STATES = Collections.unmodifiableMap(states);
	}

	// Смещение T
	public static final int OFFSET_TEMP = 0; // Температура (*0+0)
	public static final int OFFSET_HUMID = 1; // Влажность (*0+1)

	// --- ТЕМПЕРАТУРА (тип 1) ---
    public static final int REG_T_BT3 = 0x0100;    // T1
    public static final int REG_T_BT11 = 0x0110;    // T2
    public static final int REG_T_BT10 = 0x0120; // T3
    public static final int REG_T_BT1 = 0x0130;  // T4-1
    public static final int REG_T_BT2 = 0x0140;    // T5
    public static final int REG_T_BT21 = 0x0150;  // T4-2

	// --- ТЕМПЕРАТУРА (тип 2) ---
	public static final int REG_T_BT8  = 0x0190;
    public static final int REG_T_BT9  = 0x01A0;
    public static final int REG_T_BT24 = 0x01B0;
    public static final int REG_T_BT27 = 0x01C0;

	// --- ДАВЛЕНИЕ (Вентиляторы) ---
	public static final int REG_P_BF11 = 0x0160;  // вентилятор 1
	public static final int REG_P_BF12 = 0x01F0;  // внетилятор 2
	public static final int REG_P_BF21 = 0x0170;  // вентилятор 3
	public static final int REG_P_BF22 = 0x0200;  // внетилятор 4

	// --- СКОРОСТЬ (Вентиляторы) ---
	public static final int REG_F_G11 = 0x00B3;
	public static final int REG_F_G12 = 0x00B4;
	public static final int REG_F_G21 = 0x00B5;
	public static final int REG_F_G22 = 0x00B6;

	// --- ДАВЛЕНИЕ (W)  ---
	public static final int REG_P_BP1 = 0x0210;
	public static final int REG_P_BP2 = 0x0220;

	public static final int REG_P_BP5 = 0x0230;  // Давление "слева от рекупа"
	public static final int REG_P_BP10 = 0x00A9;   // Давление "справа от рекупа"

	public static final int REG_P_VM11 = 0x00A3; //обороты вентилятора 1
	public static final int REG_P_VM12 = 0x00A4; //обороты вентилятора 2
	public static final int REG_P_VM21 = 0x00A5; //обороты вентилятора 3
	public static final int REG_P_VM22 = 0x00A6; //обороты вентилятора 4

	public static final int REG_F_TARGET = 0x0024; // цель всех вентиляторов
	public static final int REG_POWERPROC = 0x0070; // powerproc для расчета цели справа

	public static final int REG_F_VH1 = 0x00AB; //клапан + слева
	public static final int REG_F_VC1 = 0x00AC; //клапан - справа
	public static final int REG_F_VH2 = 0x00AD; //клапан + справа
	public static final int REG_F_VC2 = 0x00AE; //клапан - справа

	// ТЕМПЕРАТУРЫ ЗОН
	public static final int REG_T_ZONE1 = ModbusRegisterMap.HEATER_ZONE_1 + ModbusRegisterMap.INFLOW_TARGET_OFFSET;
	public static final int REG_T_ZONE2 = ModbusRegisterMap.HEATER_ZONE_2 + ModbusRegisterMap.INFLOW_TARGET_OFFSET;

	public static final int REG_R_RECUP_SPEED = 0x00B8; // обороты вала
	public static final int SPEED_R_REG = ModbusRegisterMap.RECUPERATOR_MOTOR_RPM; // x10


	// public static final int REG_R_EFFICIENCY = 0x008A;  // КПД теперь считаем: abs(BT3-BT10)/abs(BT3-BT2) навсегда

	public static final int REG_R_TARGET = 0x0084;
	public static final int REG_R_STATE = ModbusRegisterMap.AGGREGATE_STATE;
	public static final int MASK_STATE_ALARM = 0x0004;
	public static final int MASK_STATE_VENTILATION = 0x0040;

	// Объем прокачки
	public static final int REG_F_VM1 = 0x0047;
	public static final int REG_F_VM2 = 0x0048;

	public static final int REG_V_VManualProcIn = 0x00D5;
	public static final int REG_V_VManualDProcOut = 0x00D6;

	/*
	 * Приоритет 0 (Сауна ПЛК): температурные регистры первыми идут в логике записи.
	 * В API отдаём сырое значение из Modbus (без REG_POWERPROC и без масштабирования x10).
	 *
	 * Температура (updateTempSettings, порядок записи 0xE1 -> 0xDD -> 0x7E):
	 *   0x00E1 — положение вентилей при запуске
	 *   0x00DD — алгоритм управления
	 *   0x007E — интервал слежения по T
	 * Рекуператор отдельно использует 0x0511 (DQualityBack), затем min/max/step.
	 */
	public static final int REG_T_VALVE_START_POS = ModbusRegisterMap.TEMP_VALVE_START_POSITION;
	public static final int REG_T_CTRL_ALGORITHM = 0x00DD;   // п.2: алгоритм управления
	public static final int REG_T_TRACK_INTERVAL = 0x007E;   // п.3: интервал слежения по T
	public static final int REG_R_TRACK_INTERVAL = ModbusRegisterMap.RECUPERATOR_TRACKING_INTERVAL; // DQualityBack, секунды

	// --- ТЕМПЕРАТУРНЫЕ УСТАВКИ ---
	// Целевые температуры зон
	public static final int REG_T_TARGET_ZONE1 = REG_T_ZONE1;
	public static final int REG_T_TARGET_ZONE2 = REG_T_ZONE2;

	// Пороги температуры воды
	public static final int REG_T_WATER_WARN = ModbusRegisterMap.TEMP_WATER_WARN;
	public static final int REG_T_WATER_FAIL = ModbusRegisterMap.TEMP_WATER_FAIL;

	// Пороги температуры ПВ
	public static final int REG_T_INFLOW_WARN = ModbusRegisterMap.TEMP_INFLOW_WARN;
	public static final int REG_T_INFLOW_FAIL = ModbusRegisterMap.TEMP_INFLOW_FAIL;

	// Режимы управления (только чтение)
	// 0-Off, 1-NeutralZone, 2-NZFromHeat, 3-NZFromCool, 4-NeedToCool, 5-Heating, 6-Average,
	// 7-Hood, 8-Alg2Step0, 9-Alg2Driving, 10-Alg2Stabilise, 11-Alg2HeatOff, 12-Alg2HeatInsuff
	public static final int REG_T_CTRL_MODE_ZONE1 = ModbusRegisterMap.HEATER_ZONE_1 + ModbusRegisterMap.TEMPERATURE_STATE_OFFSET;
	public static final int REG_T_CTRL_MODE_ZONE2 = ModbusRegisterMap.HEATER_ZONE_2 + ModbusRegisterMap.TEMPERATURE_STATE_OFFSET;

	// Команда быстрой смены режима
	public static final int REG_AGGREGATE_CMD = ModbusRegisterMap.AGGREGATE_COMMAND;
	public static final int CMD_FAST_MODE_CHANGE = 6;

	// Задвижки
	public static final int REG_Z_WORKSTATE = ModbusRegisterMap.WORK_STATE;
	public static final int MASK_DAMPER_SUPPLY = 0x0001;  // бит 0 - задвижка притока
	public static final int MASK_DAMPER_EXHAUST = 0x0002; // бит 1 - задвижка вытяжки

	public static final int REG_Z_WORKSTATE2 = ModbusRegisterMap.WORK_STATE_2;    // Workstate2
	public static final int MASK_PUMP_WORKSTATE = 0x4000; // бит 14 - насос рекуператора (команда)


	// Проверка на 32768
	public static final int INVALID_VALUE = 0x8000;


	//Целевые обороты (Все в оборотах в минуту измеряются)
	public static final int TARGET_R_REG = ModbusRegisterMap.RECUPERATOR_TARGET_RPM; // x100
	public static final int CURRENT_R_REG = ModbusRegisterMap.RECUPERATOR_CURRENT_RPM; // x100




	// --- НОВЫЕ ТЕМПЕРАТУРНЫЕ УСТАВКИ (MESetting) 22.01.2026 ---
    public static final int REG_T_NEUTRAL_ZONE = ModbusRegisterMap.TEMP_NEUTRAL_ZONE;
    public static final int REG_HOT_VALVE_MIN_PROC = ModbusRegisterMap.TEMP_HOT_VALVE_MIN;
    public static final int REG_T_WATER_NO_WORK = ModbusRegisterMap.TEMP_WATER_IDLE_MIN;
    public static final int REG_T_WATER_WORK = ModbusRegisterMap.TEMP_WATER_RUNNING_MIN;
    public static final int REG_T_RAD_FZ_FAIL = ModbusRegisterMap.TEMP_RADIATOR_FAIL;

    // --- ДАННЫЕ ВЕНТИЛЕЙ (MEDataRegister) 22.01.2026 ---
    public static final int REG_VALVE_HEAT_POS1 = ModbusRegisterMap.HEATER_ZONE_1 + ModbusRegisterMap.HEAT_VALVE_SETTING_OFFSET;
    public static final int REG_VALVE_COOL_POS1 = ModbusRegisterMap.HEATER_ZONE_1 + ModbusRegisterMap.COOL_VALVE_SETTING_OFFSET;
    public static final int REG_VALVE_OPEN_PROC1 = ModbusRegisterMap.HEATER_ZONE_1 + ModbusRegisterMap.EXTRA_HEAT_VALVE_SETTING_OFFSET;
    public static final int REG_VALVE_HEAT_POS2 = ModbusRegisterMap.HEATER_ZONE_2 + ModbusRegisterMap.HEAT_VALVE_SETTING_OFFSET;
    public static final int REG_VALVE_COOL_POS2 = ModbusRegisterMap.HEATER_ZONE_2 + ModbusRegisterMap.COOL_VALVE_SETTING_OFFSET;
    public static final int REG_VALVE_OPEN_PROC2 = ModbusRegisterMap.HEATER_ZONE_2 + ModbusRegisterMap.EXTRA_HEAT_VALVE_SETTING_OFFSET;

	// Уставки рекуператора: Power=-2, значение регистра / 100
    public static final int REG_R_TARGET_RPM_MIN = ModbusRegisterMap.RECUPERATOR_MIN_RPM_SETTING;
    public static final int REG_R_TARGET_RPM_MAX = ModbusRegisterMap.RECUPERATOR_MAX_RPM_SETTING;
    public static final int REG_R_TARGET_RPM_STEP = ModbusRegisterMap.RECUPERATOR_RPM_STEP_SETTING;


	public static final int REG_R_TEMP = ModbusRegisterMap.SENSOR_RECUPERATOR_CARRIER + ModbusRegisterMap.TEMPERATURE_OFFSET; // TTR / BT6

	// RecupState (MEStateRegister) 0x00A4 — текущее состояние рекуператора
	public static final int REG_RECUP_STATE = ModbusRegisterMap.RECUPERATOR_STATE;

	public static final Map<Integer, String> RECUP_STATES;
	static {
		Map<Integer, String> m = new HashMap<>();
		m.put(0, "Отключен");
		m.put(1, "Макс. обороты");
		m.put(2, "Сужение");
		m.put(3, "Стабилизация");
		m.put(4, "Врем. остановка");
		m.put(5, "Расширение");
		m.put(6, "Откл. по условию");
		RECUP_STATES = Collections.unmodifiableMap(m);
	}
}
