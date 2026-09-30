package com.example.modbus_api.controller;

import com.example.modbus_api.dto.SystemEventDto;
import com.example.modbus_api.service.ModbusClientAdapter;
import com.example.modbus_api.service.ControllerDiagnosticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/devices")
@PreAuthorize("hasRole('ADMIN')")
public class SystemEventsController {
    private static final Logger log=LoggerFactory.getLogger(SystemEventsController.class);
    private final ModbusClientAdapter modbusClient;
    private final ControllerDiagnosticsService diagnostics;
    public SystemEventsController(ModbusClientAdapter modbusClient,ControllerDiagnosticsService diagnostics) {
        this.modbusClient=modbusClient;this.diagnostics=diagnostics;
    }
    @GetMapping("/events")
    public ResponseEntity<?> getEvents(@RequestParam(defaultValue="60") int minutes) throws IOException {
        if(minutes<1 || minutes>129600) return ResponseEntity.badRequest().body(Collections.singletonMap("error","Период должен быть от 1 до 129600 минут"));
        List<SystemEventDto> events=modbusClient.getEvents(minutes);
        if(events==null) throw new IOException("Empty event response");
        return ResponseEntity.ok(events.stream().filter(e->e!=null && !Boolean.FALSE.equals(e.getValid())).collect(Collectors.toList()));
    }
    @GetMapping("/events/status")
    public ControllerDiagnosticsService.Snapshot status() throws IOException { return diagnostics.read(); }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<?> unavailable(IOException e) {
        log.warn("Controller journal/diagnostics unavailable: {}",e.getClass().getSimpleName());
        return ResponseEntity.status(502).body(Collections.singletonMap("error","Не удалось получить данные от сервера. Повторите запрос."));
    }
}
