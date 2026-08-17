package com.example.modbus_api.controller;

import com.example.modbus_api.service.ModbusClientAdapter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Collections;

@RestController
@RequestMapping("/api/v1/devices")
public class SystemEventsController {

    private final ModbusClientAdapter modbusClient;

    public SystemEventsController(ModbusClientAdapter modbusClient) {
        this.modbusClient = modbusClient;
    }

    @GetMapping("/events")
    public ResponseEntity<?> getEvents(@RequestParam(defaultValue = "60") int minutes) {
        try {
            int window = Math.max(1, Math.min(minutes, 7 * 24 * 60));
            return ResponseEntity.ok(modbusClient.getEvents(window));
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Collections.singletonMap("error", "Ошибка получения журнала событий: " + e.getMessage()));
        }
    }
}
