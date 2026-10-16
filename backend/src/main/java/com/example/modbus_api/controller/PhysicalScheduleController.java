package com.example.modbus_api.controller;

import com.example.modbus_api.dto.PhysicalScheduleDto.*;
import com.example.modbus_api.service.PhysicalScheduleService;
import com.example.modbus_api.service.UserActionLogService;
import com.example.modbus_api.logging.ActionType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import javax.validation.Valid;
import java.io.IOException;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import com.example.modbus_api.service.ModbusRegisterMap;
import java.util.ConcurrentModificationException;

@RestController
@RequestMapping("/api/v1/devices/physical-schedules")
@PreAuthorize("hasRole('ADMIN')")
public class PhysicalScheduleController {
    private final PhysicalScheduleService service;
    private final UserActionLogService logs;
    public PhysicalScheduleController(PhysicalScheduleService service,UserActionLogService logs) { this.service=service; this.logs=logs; }
    @GetMapping public Snapshot readAll() throws IOException { return service.readAll(); }
    @PatchMapping("/{id}") public Result update(@PathVariable int id,@Valid @RequestBody Update request) throws IOException {
        String action="Физическое расписание: слот "+id;
        try {
            Result result=service.update(id,request);
            logs.log(ActionType.SETPOINT,action,result.isVerified(),describeChanges(request));
            return result;
        } catch(IOException|RuntimeException e) {
            logs.log(ActionType.SETPOINT,action,false,e.getMessage());
            throw e;
        }
    }
    private static String describeChanges(Update request) {
        String[] labels = {"Тип расписания", "Активность", "Дни недели", "Дата начала", "Дата окончания",
            "Время начала", "Время окончания", "Продолжительность", "Группа", "Режим объёма",
            "Объём", "Объём", "Режим температуры зоны 1", "Регулирование зоны 1", "Цель зоны 1",
            "Режим температуры зоны 2", "Регулирование зоны 2", "Цель зоны 2"};
        Map<Integer, Change> fields = new HashMap<>();
        for (Change c : request.getChanges()) fields.put(c.getOffset(), c);
        List<String> parts = new ArrayList<>();
        for (Change c : request.getChanges()) {
            int offset = c.getOffset();
            if (offset == 11) continue;
            if (offset == 10) {
                Change high = fields.get(11);
                parts.add("Объём: " + airflow(c.getExpected(), high.getExpected())
                    + " → " + airflow(c.getValue(), high.getValue()) + " м³/ч");
            } else {
                parts.add(labels[offset] + ": " + display(offset, c.getExpected()) + " → " + display(offset, c.getValue()));
            }
        }
        parts.add(request.isApply() ? "Команда применения отправлена" : "Сохранено без команды применения");
        return String.join("; ", parts);
    }
    private static String airflow(int low, int high) {
        Double value = ModbusRegisterMap.decodeFloat(low, high);
        return value == null ? "неизвестно" : java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
    private static String display(int offset, int value) {
        if (offset == 1) return (value & 1) != 0 ? "включено" : "выключено";
        if (offset == 2) {
            String[] names = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
            List<String> days = new ArrayList<>();
            for (int i = 0; i < names.length; i++) if ((value & (1 << i)) != 0) days.add(names[i]);
            return days.isEmpty() ? "нет" : String.join(", ", days);
        }
        if (offset == 3 || offset == 4) return String.format(java.util.Locale.ROOT, "%02d.%02d", value & 255, value >> 8);
        if (offset == 5 || offset == 6) return String.format(java.util.Locale.ROOT, "%02d:%02d", value >> 8, value & 255);
        if (offset == 9) {
            String mode = ModbusRegisterMap.ventilationModeName(value);
            return mode == null ? "код " + value : mode;
        }
        if (offset == 12 || offset == 15) {
            String mode = ModbusRegisterMap.temperatureModeName(value);
            return mode == null ? "Неизвестный режим (" + value + ")" : mode;
        }
        if (offset == 13 || offset == 16) return value == 0 ? "ОРП" : value == 1 ? "ПВ" : value == 2 ? "ОВ" : "код " + value;
        if (offset == 14 || offset == 17) return value == 32768 ? "неизвестно" : ((short) value) / 10.0 + " °C";
        return Integer.toString(value);
    }
    @PostMapping("/apply") public ResponseEntity<Void> apply() throws IOException {
        try { service.apply(); logs.log(ActionType.MODE,"Физическое расписание: команда 6",true,null); }
        catch(IOException e) { logs.log(ActionType.MODE,"Физическое расписание: команда 6",false,e.getMessage()); throw e; }
        return ResponseEntity.noContent().build();
    }
    @ExceptionHandler(ConcurrentModificationException.class) public ResponseEntity<?> conflict(Exception e) { return ResponseEntity.status(409).body(Collections.singletonMap("message",e.getMessage())); }
    @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<?> invalid(Exception e) { return ResponseEntity.badRequest().body(Collections.singletonMap("message",e.getMessage())); }
    @ExceptionHandler(IOException.class) public ResponseEntity<?> unavailable(Exception e) { return ResponseEntity.status(502).body(Collections.singletonMap("message",e.getMessage())); }
}
