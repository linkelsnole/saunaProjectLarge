package com.example.modbus_api.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ModbusConstants {
	public static final String SAUNA_PLC = "SaunaPLC"; 

	public static final Map<Integer, String> SYSTEM_STATES;
	static {
			Map<Integer, String> states = new HashMap<>();
			states.put(1, "Выключено");                                    
			states.put(2, "Авария фатальная");                             
			states.put(4, "Отключение по сбою");                          
			states.put(8, "Быстрый запуск");                              
			states.put(16, "Начальный запуск");                           
			states.put(32, "Калибровка");                                 
			states.put(64, "Вентилирование");                           
			states.put(128, "Остановка агрегата, выбег");                 
			states.put(256, "Продувка датчика давления");            
			states.put(512, "Охлаждение калорифера");                
			states.put(1024, "Остаточная работа теплообменника");         
			states.put(2048, "Чистительный продув теплообменника");    
			states.put(4096, "Вытяжка");                          
			states.put(8192, "Калибровка вращения теплообменника");       
			states.put(16384, "Размораживание теплообменника");          
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
	public static final int REG_T_ZONE1 = 0x0025;
	public static final int REG_T_ZONE2 = 0x00CE;

	public static final int REG_R_RECUP_SPEED = 0x00B8; // обороты вала
	public static final int SPEED_R_REG = 0x0082; // скорость вращения

	
	// public static final int REG_R_EFFICIENCY = 0x008A;  // КПД теперь считаем: abs(BT3-BT10)/abs(BT3-BT2) навсегда
	
	public static final int REG_R_TARGET = 0x0084;
	public static final int REG_R_STATE = 0x0010;
	public static final int MASK_STATE_ALARM = 0x0004;
	public static final int MASK_STATE_VENTILATION = 0x0040;
	
	// Объем прокачки
	public static final int REG_F_VM1 = 0x0047;
	public static final int REG_F_VM2 = 0x0048;

	public static final int REG_V_VManualProcIn = 0x00D5;
	public static final int REG_V_VManualDProcOut = 0x00D6;
	
	// --- ТЕМПЕРАТУРНЫЕ УСТАВКИ ---
	// Целевые температуры зон
	public static final int REG_T_TARGET_ZONE1 = 0x0025; // T manual
	public static final int REG_T_TARGET_ZONE2 = 0x00CE; // T manual 2
	
	// Пороги температуры воды
	public static final int REG_T_WATER_WARN = 0x00C3;  // снижение оборотов
	public static final int REG_T_WATER_FAIL = 0x00C4;  // авария
	
	// Пороги температуры ПВ 
	public static final int REG_T_INFLOW_WARN = 0x00C5; // снижение оборотов
	public static final int REG_T_INFLOW_FAIL = 0x00C6; // авария
	
	// Режимы управления (только чтение)
	// 0-Off, 1-NeutralZone, 2-NZFromHeat, 3-NZFromCool, 4-NeedToCool, 5-Heating, 6-Average
	public static final int REG_T_CTRL_MODE_ZONE1 = 0x0032;
	public static final int REG_T_CTRL_MODE_ZONE2 = 0x0033;
	
	// Команда быстрой смены режима
	public static final int REG_AGGREGATE_CMD = 0x0012;
	public static final int CMD_FAST_MODE_CHANGE = 6;

	// Задвижки
	public static final int REG_Z_WORKSTATE = 0x008C;
	public static final int MASK_DAMPER_SUPPLY = 0x0001;  // бит 0 - задвижка притока
	public static final int MASK_DAMPER_EXHAUST = 0x0002; // бит 1 - задвижка вытяжки

	public static final int REG_Z_WORKSTATE2 = 0x009F;    // Workstate2
	public static final int MASK_PUMP_WORKSTATE = 0x4000; // бит 14 - насос рекуператора (команда)


	// Проверка на 32768
	public static final int INVALID_VALUE = 0x8000;


	//Целевые обороты (Все в оборотах в минуту измеряются)
	public static final int TARGET_R_REG = 0x0081; // делить на 100
	public static final int CURRENT_R_REG = 0x0080;// Обороты вала (делить на 100)

	
	

	// --- НОВЫЕ ТЕМПЕРАТУРНЫЕ УСТАВКИ (MESetting) 22.01.2026 ---
    public static final int REG_T_NEUTRAL_ZONE = 0x006E;    // DTNeutral
    public static final int REG_HOT_VALVE_MIN_PROC = 0x00D2; // HotValveMinProc
    public static final int REG_T_WATER_NO_WORK = 0x00D3;   // TWaterNoWork
    public static final int REG_T_WATER_WORK = 0x00D4;      // TWaterWork
    public static final int REG_T_RAD_FZ_FAIL = 0x00C7;     // TRadFzFail

    // --- ДАННЫЕ ВЕНТИЛЕЙ (MEDataRegister) 22.01.2026 ---
    public static final int REG_VALVE_HEAT_POS1 = 0x00AB;   // ValveHeatPos1
    public static final int REG_VALVE_COOL_POS1 = 0x00AC;   // ValveCoolPos1
    public static final int REG_VALVE_OPEN_PROC1 = 0x00B9;  // ValveOpenProc
    public static final int REG_VALVE_HEAT_POS2 = 0x00AD;   // ValveHeatPos2
    public static final int REG_VALVE_COOL_POS2 = 0x00AE;   // ValveCoolPos2
    public static final int REG_VALVE_OPEN_PROC2 = 0x00BA;  // ValveOpenProc2

	//Рекуператор 25.01.2026 (плавающая точка, об/мин, быстрая смена режима)
    public static final int REG_R_TARGET_RPM_MIN = 0x005D;  // (мин) (сейчас 0 или 0.5 оборотов)
    public static final int REG_R_TARGET_RPM_MAX = 0x005E;  // (макс) (сейчас от 1 до 7 оборотов может стоять)
    public static final int REG_R_TARGET_RPM_STEP = 0x0065;  // 0x0065 - шаг калибровки (сейчас 0.1 или 0.3 оборота)


	public static final int REG_R_TEMP = 0x02C0; //температура рекуператора (BT6)

	// RecupState (MEStateRegister) 0x00A4 — текущее состояние рекуператора
	public static final int REG_RECUP_STATE = 0x00BB;

	public static final Map<Integer, String> RECUP_STATES;
	static {
		Map<Integer, String> m = new HashMap<>();
		m.put(0, "Отключен");
		m.put(1, "Макс. обороты");
		m.put(2, "Настройка");
		m.put(3, "Стабилизация");
		m.put(4, "Врем. остановка");
		RECUP_STATES = Collections.unmodifiableMap(m);
	}
}

