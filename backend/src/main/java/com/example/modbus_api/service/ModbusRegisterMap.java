package com.example.modbus_api.service;

/**
 * Регистры главной схемы Sauna21Control.xls (лист Air2).
 * Смещения из шаблонов XLS — десятичные. Format=2 — IEEE-754 float:
 * младшее слово по base, старшее по base+1.
 */
public final class ModbusRegisterMap {
    private ModbusRegisterMap() {
    }

    // Controller display units confirmed during October commissioning.
    public static final double FAN_PERCENT_SCALE = 10.0;
    public static final double PRESSURE_DISPLAY_MULTIPLIER = 10.0;
    public static final double RECUPERATOR_SETTING_SCALE = 1000.0;
    public static final int SENSOR_ALARM_MODE_OFFSET = 0;
    public static final int SENSOR_ALARM_TIME_OFFSET = 1;
    public static final int TEMPERATURE_OFFSET = 8;
    public static final int HUMIDITY_OFFSET = 9;
    public static final int PRESSURE_OFFSET = 8;
    public static final int FAN_AIRFLOW_OFFSET = 8;
    public static final int FAN_PERCENT_OFFSET = 10;

    public static final int SENSOR_T1 = 0x1000;
    public static final int SENSOR_T2 = 0x1010;
    public static final int SENSOR_T3 = 0x1020;
    public static final int SENSOR_T4_1 = 0x1030;
    public static final int SENSOR_T4_2 = 0x1040;
    public static final int SENSOR_T5 = 0x1050;
    public static final int SENSOR_TH1 = 0x1060;
    public static final int SENSOR_TC1 = 0x1070;
    public static final int SENSOR_TH2 = 0x1080;
    public static final int SENSOR_TC2 = 0x1090;
    public static final int SENSOR_RECUPERATOR_CARRIER = 0x10C0; // Air2Common: TTR / BT6

    public static final int PRESSURE_FAN_SUPPLY_1 = 0x1200 + PRESSURE_OFFSET;
    public static final int PRESSURE_FAN_SUPPLY_2 = 0x1210 + PRESSURE_OFFSET;
    public static final int PRESSURE_FAN_EXHAUST_1 = 0x1220 + PRESSURE_OFFSET;
    public static final int PRESSURE_FAN_EXHAUST_2 = 0x1230 + PRESSURE_OFFSET;
    public static final int PRESSURE_FILTER_SUPPLY = 0x1240 + PRESSURE_OFFSET;
    public static final int PRESSURE_FILTER_EXHAUST = 0x1250 + PRESSURE_OFFSET;
    public static final int PRESSURE_RECUP_SUPPLY = 0x1260 + PRESSURE_OFFSET;
    public static final int PRESSURE_RECUP_EXHAUST = 0x1270 + PRESSURE_OFFSET;

    public static final int FAN_SUPPLY_1 = 0x1400;
    public static final int FAN_SUPPLY_2 = 0x1410;
    public static final int FAN_EXHAUST_1 = 0x1420;
    public static final int FAN_EXHAUST_2 = 0x1430;

    public static final int HEATER_ZONE_1 = 0x0900;
    public static final int HEATER_ZONE_2 = 0x0940;
    public static final int TEMPERATURE_CONTROL_OFFSET = 3;
    public static final int TEMPERATURE_STATE_OFFSET = 5;
    // Heater valve percentages use tenths in the September 2026 controller update.
    public static final double VALVE_PERCENT_SCALE = 10.0;
    public static final int HEAT_PERCENT_OFFSET = 7;
    public static final int COOL_PERCENT_OFFSET = 8;
    public static final int TARGET_TEMPERATURE_OFFSET = 18;
    public static final int HEAT_VALVE_SETTING_OFFSET = 21;
    public static final int COOL_VALVE_SETTING_OFFSET = 22;
    public static final int EXTRA_HEAT_VALVE_SETTING_OFFSET = 23;
    public static final int INFLOW_TARGET_OFFSET = 25;
    public static final int OUTFLOW_TARGET_OFFSET = 26;
    public static final int ORP_TARGET_T1_OFFSET = 27;
    public static final int ORP_CURRENT_T1_OFFSET = 28;
    public static final int ORP_TARGET_T2_OFFSET = 29;
    public static final int ORP_CURRENT_T2_OFFSET = 30;
    public static final int ORP_TARGET_T3_OFFSET = 31;
    public static final int ORP_CURRENT_T3_OFFSET = 32;

    public static final int TEMP_WATER_WARN = 0x0700;
    public static final int TEMP_WATER_FAIL = 0x0701;
    public static final int TEMP_INFLOW_WARN = 0x0702;
    public static final int TEMP_INFLOW_FAIL = 0x0703;
    public static final int TEMP_RADIATOR_FAIL = 0x0704;
    public static final int TEMP_WATER_IDLE_MIN = 0x0705;
    public static final int TEMP_WATER_RUNNING_MIN = 0x0706;
    public static final int TEMP_VALVE_START_POSITION = 0x0708;
    public static final int TEMP_HOT_VALVE_MIN = 0x0709;
    public static final int TEMP_NEUTRAL_ZONE = 0x0710;

    public static final int AIRFLOW_ECO = 0x0106;
    public static final int AIRFLOW_NORMAL = 0x010A;
    public static final int AIRFLOW_INTENSIVE = 0x010E;
    public static final int AIRFLOW_MANUAL = 0x0116;
    public static final int VENTILATION_MODE = 0x0122;
    public static final int AIRFLOW_TARGET = 0x0124;
    public static final int AIRFLOW_REAL = 0x0126;
    public static final int AIRFLOW_IMBALANCE = 0x0707;
    public static final int AGGREGATE_COMMAND = 0x0300;
    public static final int AGGREGATE_STATE = 0x0301;
    public static final int WORK_STATE = 0x0305;
    public static final int WORK_STATE_2 = 0x0306;
    public static final int RECUPERATOR_TRACKING_INTERVAL = 0x0511;
    public static final int RECUPERATOR_STATE = 0x0800;
    public static final int RECUPERATOR_MIN_RPM_SETTING = 0x0801;
    public static final int RECUPERATOR_MAX_RPM_SETTING = 0x0802;
    public static final int RECUPERATOR_RPM_STEP_SETTING = 0x0803;
    public static final int RECUPERATOR_MAX_RPM = 0x0804;
    public static final int RECUPERATOR_TARGET_PERCENT = 0x0812;
    public static final int RECUPERATOR_TARGET_RPM = 0x0813;
    public static final int RECUPERATOR_CURRENT_RPM = 0x0814;
    public static final int RECUPERATOR_MOTOR_RPM = 0x0815;

    public static final int SCHEDULE_BASE = 0x2000;
    public static final int SCHEDULE_STRIDE = 0x20;
    public static final int SCHEDULE_COUNT = 20;
    public static final int SCHEDULE_SIZE = 0x1FFF;
    public static final int SCHEDULE_USERSET_MODE = 5;
    public static final int SCHEDULE_MODE_OFFSET = 0;
    public static final int SCHEDULE_STATE_OFFSET = 1;
    public static final int SCHEDULE_SELECTED_MASK = 1;
    public static final int SCHEDULE_WEEK_DAYS_OFFSET = 2;
    public static final int SCHEDULE_START_DATE_OFFSET = 3;
    public static final int SCHEDULE_END_DATE_OFFSET = 4;
    public static final int SCHEDULE_START_TIME_OFFSET = 5;
    public static final int SCHEDULE_END_TIME_OFFSET = 6;
    public static final int SCHEDULE_DURATION_OFFSET = 7;
    public static final int SCHEDULE_GROUP_OFFSET = 8;
    public static final int SCHEDULE_VENTILATION_MODE_OFFSET = 9;
    public static final int SCHEDULE_TARGET_AIRFLOW_OFFSET = 10;
    public static final int SCHEDULE_TEMPERATURE_MODE_ZONE_1_OFFSET = 12;
    public static final int SCHEDULE_TEMPERATURE_CONTROL_ZONE_1_OFFSET = 13;
    public static final int SCHEDULE_TARGET_TEMPERATURE_ZONE_1_OFFSET = 14;
    public static final int SCHEDULE_TEMPERATURE_MODE_ZONE_2_OFFSET = 15;
    public static final int SCHEDULE_TEMPERATURE_CONTROL_ZONE_2_OFFSET = 16;
    public static final int SCHEDULE_TARGET_TEMPERATURE_ZONE_2_OFFSET = 17;

    public static final int TEMPERATURE_CONTROL_ORP = 0;
    public static final int TEMPERATURE_CONTROL_INFLOW = 1;
    public static final int TEMPERATURE_CONTROL_OUTFLOW = 2;

    public static int scheduleBase(int index) {
        if (index < 0 || index >= SCHEDULE_COUNT) {
            throw new IllegalArgumentException("Индекс расписания вне диапазона: " + index);
        }
        return SCHEDULE_BASE + index * SCHEDULE_STRIDE;
    }

    public static int scheduleBaseById(int scheduleId) {
        if (scheduleId < 1 || scheduleId > SCHEDULE_COUNT) {
            throw new IllegalArgumentException("ID расписания вне диапазона: " + scheduleId);
        }
        return scheduleBase(scheduleId - 1);
    }

    public static Double decodeFloat(Integer low, Integer high) {
        if (low == null || high == null) {
            return null;
        }
        float value = Float.intBitsToFloat(((high & 0xFFFF) << 16) | (low & 0xFFFF));
        return Float.isNaN(value) || Float.isInfinite(value) ? null : (double) value;
    }

    public static int floatLowWord(float value) {
        return Float.floatToIntBits(value) & 0xFFFF;
    }

    public static int floatHighWord(float value) {
        return (Float.floatToIntBits(value) >>> 16) & 0xFFFF;
    }

    public static String temperatureControlName(Integer code) {
        if (code == null) return null;
        switch (code) {
            case 0: return "ОРП";
            case 1: return "ПВ";
            case 2: return "ОВ";
            default: return null;
        }
    }

    public static String temperatureModeName(Integer code) {
        if (code == null) return null;
        switch (code) {
            case 0: return "Отключен";
            case 1: return "ЭКО";
            case 2: return "АВТО";
            case 3: return "Ручной";
            default: return null;
        }
    }

    public static String ventilationModeName(Integer code) {
        if (code == null) return null;
        switch (code) {
            case 0: return "Отключен";
            case 1: return "ЭКО";
            case 2: return "Норм";
            case 3: return "Интенсив";
            case 4: return "Ручной";
            default: return null;
        }
    }
}
