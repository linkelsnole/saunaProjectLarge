package com.example.modbus_api.service;

import com.example.modbus_api.dto.ScheduleData;
import com.example.modbus_api.dto.ScheduleListData;
import com.example.modbus_api.dto.UpdateScheduleGroupRequest;
import com.example.modbus_api.dto.UpdateScheduleIntervalRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.example.modbus_api.service.ModbusConstants.CMD_FAST_MODE_CHANGE;
import static com.example.modbus_api.service.ModbusConstants.INVALID_VALUE;
import static com.example.modbus_api.service.ModbusConstants.SAUNA_PLC;

@Service
public class ScheduleService {
    private static final double TEMPERATURE_MIN = -50.0;
    private static final double TEMPERATURE_MAX = 100.0;

    private static final Map<String, Integer> WEEK_DAY_BITS;

    static {
        Map<String, Integer> dayBits = new HashMap<>();
        dayBits.put("mon", 0);
        dayBits.put("пн", 0);
        dayBits.put("tue", 1);
        dayBits.put("вт", 1);
        dayBits.put("wed", 2);
        dayBits.put("ср", 2);
        dayBits.put("thu", 3);
        dayBits.put("чт", 3);
        dayBits.put("fri", 4);
        dayBits.put("пт", 4);
        dayBits.put("sat", 5);
        dayBits.put("сб", 5);
        dayBits.put("sun", 6);
        dayBits.put("вс", 6);
        WEEK_DAY_BITS = Collections.unmodifiableMap(dayBits);
    }

    private final ModbusClientAdapter modbusClient;

    public ScheduleService(ModbusClientAdapter modbusClient) {
        this.modbusClient = modbusClient;
    }

    public ScheduleListData getAllSchedules() throws IOException {
        List<Integer> registers = new ArrayList<>();
        registers.add(ModbusRegisterMap.SCHEDULE_SIZE);
        for (int index = 0; index < ModbusRegisterMap.SCHEDULE_COUNT; index++) {
            int base = ModbusRegisterMap.scheduleBase(index);
            for (int offset = 0; offset <= ModbusRegisterMap.SCHEDULE_TARGET_TEMPERATURE_ZONE_2_OFFSET; offset++) {
                registers.add(base + offset);
            }
        }

        Map<Integer, Integer> values = modbusClient.readRegisters(SAUNA_PLC, registers);
        List<ScheduleData> schedules = new ArrayList<>();
        for (int scheduleId = 1; scheduleId <= ModbusRegisterMap.SCHEDULE_COUNT; scheduleId++) {
            schedules.add(parseScheduleFromMap(scheduleId, values));
        }

        Integer totalSlots = validValue(values.get(ModbusRegisterMap.SCHEDULE_SIZE));
        if (totalSlots == null || totalSlots <= 0 || totalSlots > ModbusRegisterMap.SCHEDULE_COUNT) {
            totalSlots = ModbusRegisterMap.SCHEDULE_COUNT;
        }
        return new ScheduleListData(schedules, "success", "Расписания прочитаны", totalSlots);
    }

    ScheduleData parseScheduleFromMap(int scheduleId, Map<Integer, Integer> values) {
        int base = ModbusRegisterMap.scheduleBaseById(scheduleId);
        int rawState = valueOrDefault(values.get(base + ModbusRegisterMap.SCHEDULE_STATE_OFFSET), 0);

        ScheduleData schedule = new ScheduleData(scheduleId);
        schedule.setMode(ScheduleData.ScheduleMode.fromCode(
            valueOrDefault(values.get(base + ModbusRegisterMap.SCHEDULE_MODE_OFFSET), 0)));
        schedule.setStatusRawCode(rawState);
        schedule.setStatus((rawState & ModbusRegisterMap.SCHEDULE_SELECTED_MASK) != 0
            ? ScheduleData.ScheduleStatus.ACTIVE
            : ScheduleData.ScheduleStatus.INACTIVE);
        schedule.setWeekDayMask(valueOrDefault(values.get(base + ModbusRegisterMap.SCHEDULE_WEEK_DAYS_OFFSET), 0));
        schedule.setGroup(validValue(values.get(base + ModbusRegisterMap.SCHEDULE_GROUP_OFFSET)));
        schedule.setDateRange(new ScheduleData.DateRange(
            valueOrDefault(values.get(base + ModbusRegisterMap.SCHEDULE_START_DATE_OFFSET), 0),
            valueOrDefault(values.get(base + ModbusRegisterMap.SCHEDULE_END_DATE_OFFSET), 0)));
        schedule.setTimeRange(new ScheduleData.TimeRange(
            valueOrDefault(values.get(base + ModbusRegisterMap.SCHEDULE_START_TIME_OFFSET), 0),
            valueOrDefault(values.get(base + ModbusRegisterMap.SCHEDULE_END_TIME_OFFSET), 0)));
        schedule.setDuration(valueOrDefault(values.get(base + ModbusRegisterMap.SCHEDULE_DURATION_OFFSET), 0));

        Double targetAirflow = ModbusRegisterMap.decodeFloat(
            validValue(values.get(base + ModbusRegisterMap.SCHEDULE_TARGET_AIRFLOW_OFFSET)),
            validValue(values.get(base + ModbusRegisterMap.SCHEDULE_TARGET_AIRFLOW_OFFSET + 1)));
        schedule.setAirControl(new ScheduleData.AirControl(
            ScheduleData.VentilationMode.fromCode(
                valueOrDefault(values.get(base + ModbusRegisterMap.SCHEDULE_VENTILATION_MODE_OFFSET), 0)),
            targetAirflow));

        schedule.setTemperatureModeZone1(ScheduleData.TemperatureMode.fromCode(
            valueOrDefault(values.get(base + ModbusRegisterMap.SCHEDULE_TEMPERATURE_MODE_ZONE_1_OFFSET), 0)));
        schedule.setTemperatureZone1(new ScheduleData.TemperatureZoneControl(
            ScheduleData.TemperatureControlType.fromCode(
                valueOrDefault(values.get(base + ModbusRegisterMap.SCHEDULE_TEMPERATURE_CONTROL_ZONE_1_OFFSET), 0)),
            scaledTemperature(values.get(base + ModbusRegisterMap.SCHEDULE_TARGET_TEMPERATURE_ZONE_1_OFFSET))));

        schedule.setTemperatureModeZone2(ScheduleData.TemperatureMode.fromCode(
            valueOrDefault(values.get(base + ModbusRegisterMap.SCHEDULE_TEMPERATURE_MODE_ZONE_2_OFFSET), 0)));
        schedule.setTemperatureZone2(new ScheduleData.TemperatureZoneControl(
            ScheduleData.TemperatureControlType.fromCode(
                valueOrDefault(values.get(base + ModbusRegisterMap.SCHEDULE_TEMPERATURE_CONTROL_ZONE_2_OFFSET), 0)),
            scaledTemperature(values.get(base + ModbusRegisterMap.SCHEDULE_TARGET_TEMPERATURE_ZONE_2_OFFSET))));
        return schedule;
    }

    public String describeGroupChanges(UpdateScheduleGroupRequest request) {
        if (request == null || request.getIntervals() == null) return null;
        SettingChanges changes = new SettingChanges(modbusClient);
        int weekDays = encodeWeekDays(request.getWeekDays());
        for (UpdateScheduleIntervalRequest interval : request.getIntervals()) {
            if (interval == null) continue;
            int base = ModbusRegisterMap.scheduleBaseById(interval.getScheduleId());
            String label = "Интервал " + interval.getScheduleId() + ", ";
            changes.formatted(base + ModbusRegisterMap.SCHEDULE_WEEK_DAYS_OFFSET, label + "дни", weekDays, value -> {
                String[] names = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
                List<String> days = new ArrayList<>();
                for (int i = 0; i < names.length; i++) if ((value.intValue() & (1 << i)) != 0) days.add(names[i]);
                return days.isEmpty() ? "нет" : String.join(", ", days);
            });
            changes.formatted(base + ModbusRegisterMap.SCHEDULE_STATE_OFFSET, label + "активность",
                request.isActive() ? 1 : 0, value -> (value.intValue() & ModbusRegisterMap.SCHEDULE_SELECTED_MASK) != 0 ? "включено" : "выключено");
            changes.formatted(base + ModbusRegisterMap.SCHEDULE_START_DATE_OFFSET, label + "дата начала", request.getStartDate(), ScheduleService::auditDate);
            changes.formatted(base + ModbusRegisterMap.SCHEDULE_END_DATE_OFFSET, label + "дата окончания", request.getEndDate(), ScheduleService::auditDate);
            changes.formatted(base + ModbusRegisterMap.SCHEDULE_START_TIME_OFFSET, label + "начало", interval.getStartTime(), ScheduleService::auditTime);
            changes.formatted(base + ModbusRegisterMap.SCHEDULE_END_TIME_OFFSET, label + "окончание", interval.getEndTime(), ScheduleService::auditTime);
            changes.formatted(base + ModbusRegisterMap.SCHEDULE_VENTILATION_MODE_OFFSET, label + "объём", interval.getAirControlMode(), value -> {
                String mode = ModbusRegisterMap.ventilationModeName(value.intValue());
                return mode == null ? "код " + value.intValue() : mode;
            });
            changes.formatted(base + ModbusRegisterMap.SCHEDULE_TEMPERATURE_CONTROL_ZONE_1_OFFSET, label + "регулирование зоны 1", interval.getZone1ControlMode(), ScheduleService::auditControl);
            changes.formatted(base + ModbusRegisterMap.SCHEDULE_TEMPERATURE_CONTROL_ZONE_2_OFFSET, label + "регулирование зоны 2", interval.getZone2ControlMode(), ScheduleService::auditControl);
            if (interval.getZone1ControlMode() != 0) changes.number(base + ModbusRegisterMap.SCHEDULE_TARGET_TEMPERATURE_ZONE_1_OFFSET, 10, label + "цель зоны 1", "°C", interval.getZone1TargetTemperature());
            if (interval.getZone2ControlMode() != 0) changes.number(base + ModbusRegisterMap.SCHEDULE_TARGET_TEMPERATURE_ZONE_2_OFFSET, 10, label + "цель зоны 2", "°C", interval.getZone2TargetTemperature());
        }
        return changes.describe();
    }

    private static String auditDate(Double value) {
        int raw = value.intValue();
        return raw == 0 ? "без ограничения" : String.format(java.util.Locale.ROOT, "%02d.%02d", raw & 255, (raw >> 8) & 255);
    }
    private static String auditTime(Double value) {
        int raw = value.intValue();
        return String.format(java.util.Locale.ROOT, "%02d:%02d", (raw >> 8) & 255, raw & 255);
    }
    private static String auditControl(Double value) {
        switch (value.intValue()) {
            case 0: return "ОРП";
            case 1: return "ПВ";
            case 2: return "ОВ";
            default: return "код " + value.intValue();
        }
    }

    public synchronized void updateScheduleGroup(int groupId, UpdateScheduleGroupRequest request) throws IOException {
        if (request == null || request.getIntervals() == null) throw new IllegalArgumentException("Нет интервалов");
        List<Integer> submitted = new ArrayList<>();
        for (UpdateScheduleIntervalRequest interval : request.getIntervals()) {
            if (interval == null) throw new IllegalArgumentException("Нет интервала");
            submitted.add(interval.getScheduleId());
        }
        validateGroupRequest(submitted, request);
        for (UpdateScheduleIntervalRequest interval : request.getIntervals()) {
            validateInterval(interval);
        }

        int weekDayMask = encodeWeekDays(request.getWeekDays());
        validateDate(request.getStartDate(), "Дата начала");
        validateDate(request.getEndDate(), "Дата окончания");

        List<Integer> scheduleIds = scheduleIdsForGroup(groupId);
        if (!new HashSet<>(scheduleIds).equals(new HashSet<>(submitted)))
            throw new IllegalArgumentException("Состав группы изменился. Обновите список перед сохранением");
        Map<Integer, Integer> states = readGroupStates(scheduleIds);
        for (int scheduleId : scheduleIds) {
            int base = ModbusRegisterMap.scheduleBaseById(scheduleId);
            modbusClient.writeRegister(SAUNA_PLC, base + ModbusRegisterMap.SCHEDULE_TEMPERATURE_MODE_ZONE_1_OFFSET, 2);
            modbusClient.writeRegister(SAUNA_PLC, base + ModbusRegisterMap.SCHEDULE_TEMPERATURE_MODE_ZONE_2_OFFSET, 2);
            modbusClient.writeRegister(SAUNA_PLC, base + ModbusRegisterMap.SCHEDULE_WEEK_DAYS_OFFSET, weekDayMask);
            modbusClient.writeRegister(SAUNA_PLC, base + ModbusRegisterMap.SCHEDULE_START_DATE_OFFSET, request.getStartDate());
            modbusClient.writeRegister(SAUNA_PLC, base + ModbusRegisterMap.SCHEDULE_END_DATE_OFFSET, request.getEndDate());
            writeSelectedState(base, states, request.isActive());
        }

        for (UpdateScheduleIntervalRequest interval : request.getIntervals()) {
            writeInterval(interval);
        }
        modbusClient.writeRegister(SAUNA_PLC, ModbusRegisterMap.AGGREGATE_COMMAND, CMD_FAST_MODE_CHANGE);
    }

    public synchronized void toggleScheduleGroupActive(int groupId, boolean active) throws IOException {
        List<Integer> scheduleIds = scheduleIdsForGroup(groupId);
        Map<Integer, Integer> states = readGroupStates(scheduleIds);
        for (int scheduleId : scheduleIds) {
            writeSelectedState(ModbusRegisterMap.scheduleBaseById(scheduleId), states, active);
        }
        modbusClient.writeRegister(SAUNA_PLC, ModbusRegisterMap.AGGREGATE_COMMAND, CMD_FAST_MODE_CHANGE);
    }

    private void writeInterval(UpdateScheduleIntervalRequest interval) throws IOException {
        int base = ModbusRegisterMap.scheduleBaseById(interval.getScheduleId());

        modbusClient.writeRegister(SAUNA_PLC, base + ModbusRegisterMap.SCHEDULE_START_TIME_OFFSET, interval.getStartTime());
        modbusClient.writeRegister(SAUNA_PLC, base + ModbusRegisterMap.SCHEDULE_END_TIME_OFFSET, interval.getEndTime());
        modbusClient.writeRegister(SAUNA_PLC, base + ModbusRegisterMap.SCHEDULE_VENTILATION_MODE_OFFSET,
            interval.getAirControlMode());
        modbusClient.writeRegister(SAUNA_PLC, base + ModbusRegisterMap.SCHEDULE_TEMPERATURE_CONTROL_ZONE_1_OFFSET,
            interval.getZone1ControlMode());
        if (interval.getZone1ControlMode() != 0 && interval.getZone1TargetTemperature() != null) {
            modbusClient.writeRegister(SAUNA_PLC,
                base + ModbusRegisterMap.SCHEDULE_TARGET_TEMPERATURE_ZONE_1_OFFSET,
                encodeTemperature(interval.getZone1TargetTemperature()));
        }
        modbusClient.writeRegister(SAUNA_PLC, base + ModbusRegisterMap.SCHEDULE_TEMPERATURE_CONTROL_ZONE_2_OFFSET,
            interval.getZone2ControlMode());
        if (interval.getZone2ControlMode() != 0 && interval.getZone2TargetTemperature() != null) {
            modbusClient.writeRegister(SAUNA_PLC,
                base + ModbusRegisterMap.SCHEDULE_TARGET_TEMPERATURE_ZONE_2_OFFSET,
                encodeTemperature(interval.getZone2TargetTemperature()));
        }
    }

    private Map<Integer, Integer> readGroupStates(List<Integer> scheduleIds) throws IOException {
        List<Integer> stateRegisters = new ArrayList<>();
        for (int scheduleId : scheduleIds) {
            stateRegisters.add(ModbusRegisterMap.scheduleBaseById(scheduleId)
                + ModbusRegisterMap.SCHEDULE_STATE_OFFSET);
        }
        Map<Integer, Integer> states = modbusClient.readRegisters(SAUNA_PLC, stateRegisters);
        for (Integer register : stateRegisters) {
            if (validValue(states.get(register)) == null) {
                throw new IOException("Не удалось прочитать состояние расписания из регистра 0x"
                    + Integer.toHexString(register).toUpperCase());
            }
        }
        return states;
    }

    private void writeSelectedState(int base, Map<Integer, Integer> states, boolean active) throws IOException {
        int stateRegister = base + ModbusRegisterMap.SCHEDULE_STATE_OFFSET;
        int currentState = states.get(stateRegister);
        int updatedState = active
            ? currentState | ModbusRegisterMap.SCHEDULE_SELECTED_MASK
            : currentState & ~ModbusRegisterMap.SCHEDULE_SELECTED_MASK;
        if (updatedState != currentState) {
            modbusClient.writeRegister(SAUNA_PLC, stateRegister, updatedState);
        }
    }

    // Group zero is independent: the API uses -slotId to address a single such slot.
    private List<Integer> scheduleIdsForGroup(int groupId) throws IOException {
        if (groupId == 0 || groupId < -ModbusRegisterMap.SCHEDULE_COUNT || groupId > 65534)
            throw new IllegalArgumentException("Некорректная группа расписания");
        List<Integer> addresses = new ArrayList<>();
        for (int id = 1; id <= ModbusRegisterMap.SCHEDULE_COUNT; id++) {
            int base = ModbusRegisterMap.scheduleBaseById(id);
            addresses.add(base);
            addresses.add(base + ModbusRegisterMap.SCHEDULE_GROUP_OFFSET);
        }
        Map<Integer, Integer> values = modbusClient.readRegisters(SAUNA_PLC, addresses);
        List<Integer> ids = new ArrayList<>();
        for (int id = 1; id <= ModbusRegisterMap.SCHEDULE_COUNT; id++) {
            int base = ModbusRegisterMap.scheduleBaseById(id);
            Integer mode = validValue(values.get(base));
            if (mode == null) throw new IOException("Не удалось прочитать типы расписаний");
            if (mode != 3 && mode != 4) continue;
            Integer group = validValue(values.get(base + ModbusRegisterMap.SCHEDULE_GROUP_OFFSET));
            if (group == null) throw new IOException("Не удалось прочитать группы расписаний");
            if ((groupId > 0 && group == groupId) || (groupId == -id && group == 0)) ids.add(id);
        }
        if (ids.isEmpty()) throw new IllegalArgumentException("Состав расписаний изменился. Обновите список");
        return ids;
    }

    private static void validateGroupRequest(List<Integer> scheduleIds, UpdateScheduleGroupRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Данные группы расписания не переданы");
        }
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("Даты начала и окончания обязательны");
        }
        if (request.getWeekDays() == null || request.getWeekDays().isEmpty()) {
            throw new IllegalArgumentException("Выберите хотя бы один день недели");
        }
        if (request.getIntervals() == null || request.getIntervals().isEmpty()) {
            throw new IllegalArgumentException("Интервалы расписания не переданы");
        }

        Set<Integer> seen = new HashSet<>();
        for (UpdateScheduleIntervalRequest interval : request.getIntervals()) {
            if (interval == null || !scheduleIds.contains(interval.getScheduleId())) {
                throw new IllegalArgumentException("Интервал не относится к выбранной группе расписания");
            }
            if (!seen.add(interval.getScheduleId())) {
                throw new IllegalArgumentException("Интервал расписания передан повторно");
            }
        }
    }

    private static void validateInterval(UpdateScheduleIntervalRequest interval) {
        validateTime(interval.getStartTime(), "Время начала");
        validateTime(interval.getEndTime(), "Время окончания");
        if (interval.getAirControlMode() < 1 || interval.getAirControlMode() > 3) {
            throw new IllegalArgumentException("Режим объёма должен быть ЭКО, Норм или Интенсив");
        }
        validateControlType(interval.getZone1ControlMode(), "зоны 1");
        validateControlType(interval.getZone2ControlMode(), "зоны 2");
        validateTemperature(interval.getZone1TargetTemperature(), "Целевая температура зоны 1");
        validateTemperature(interval.getZone2TargetTemperature(), "Целевая температура зоны 2");
        validateTemperatureTarget(interval.getZone1ControlMode(), interval.getZone1TargetTemperature(), "зоны 1");
        validateTemperatureTarget(interval.getZone2ControlMode(), interval.getZone2TargetTemperature(), "зоны 2");

        boolean zone1Ov = interval.getZone1ControlMode() == 2;
        boolean zone2Ov = interval.getZone2ControlMode() == 2;
        if (zone1Ov != zone2Ov) {
            throw new IllegalArgumentException("Режим ОВ должен быть выбран одновременно для обеих зон");
        }
        if (zone1Ov && encodeTemperature(interval.getZone1TargetTemperature())
            != encodeTemperature(interval.getZone2TargetTemperature())) {
            throw new IllegalArgumentException("Целевая температура ОВ должна совпадать для обеих зон");
        }
    }

    private static int encodeWeekDays(List<String> weekDays) {
        int mask = 0;
        for (String weekDay : weekDays) {
            if (weekDay == null) {
                throw new IllegalArgumentException("День недели не может быть пустым");
            }
            Integer bit = WEEK_DAY_BITS.get(weekDay.trim().toLowerCase());
            if (bit == null) {
                throw new IllegalArgumentException("Неизвестный день недели: " + weekDay);
            }
            mask |= 1 << bit;
        }
        return mask;
    }

    private static void validateDate(int encodedDate, String fieldName) {
        if (encodedDate == 0) return;
        int month = (encodedDate >> 8) & 0xFF;
        int day = encodedDate & 0xFF;
        if (month < 1 || month > 12 || day < 1 || day > daysInMonth(month)) {
            throw new IllegalArgumentException(fieldName + " имеет неверный формат");
        }
    }

    private static int daysInMonth(int month) {
        switch (month) {
            case 2: return 29;
            case 4:
            case 6:
            case 9:
            case 11: return 30;
            default: return 31;
        }
    }

    private static void validateTime(Integer encodedTime, String fieldName) {
        if (encodedTime == null) {
            throw new IllegalArgumentException(fieldName + " обязательно");
        }
        int hour = (encodedTime >> 8) & 0xFF;
        int minute = encodedTime & 0xFF;
        if (hour > 23 || minute > 59) {
            throw new IllegalArgumentException(fieldName + " имеет неверный формат");
        }
    }

    private static void validateControlType(int value, String zoneName) {
        if (value < 0 || value > 2) {
            throw new IllegalArgumentException("Режим температуры " + zoneName + " должен быть ОРП, ПВ или ОВ");
        }
    }

    private static void validateTemperature(Double value, String fieldName) {
        if (value == null) return;
        if (value.isNaN() || value.isInfinite() || value < TEMPERATURE_MIN || value > TEMPERATURE_MAX) {
            throw new IllegalArgumentException(fieldName + " должна быть в диапазоне от -50 до 100 °C");
        }
    }

    private static void validateTemperatureTarget(int controlMode, Double value, String zoneName) {
        if (controlMode != 0 && value == null) {
            throw new IllegalArgumentException("Целевая температура " + zoneName + " обязательна для ПВ и ОВ");
        }
    }

    private static int encodeTemperature(double value) {
        return ((int) Math.round(value * 10.0)) & 0xFFFF;
    }

    private static Double scaledTemperature(Integer raw) {
        Integer valid = validValue(raw);
        return valid == null ? null : ((double) (short) (valid & 0xFFFF)) / 10.0;
    }

    private static int valueOrDefault(Integer value, int defaultValue) {
        Integer valid = validValue(value);
        return valid == null ? defaultValue : valid;
    }

    private static Integer validValue(Integer value) {
        return value == null || value == INVALID_VALUE ? null : value;
    }
}
