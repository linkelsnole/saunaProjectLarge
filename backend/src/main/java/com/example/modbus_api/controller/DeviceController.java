package com.example.modbus_api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.modbus_api.dto.*;
import com.example.modbus_api.service.*;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/devices")
@CrossOrigin(origins = "*") 
@Tag(name = "Device Controller", description = "Управление датчиками и устройствами ПЛК")
public class DeviceController {

    private final DeviceService deviceService;
    private final SettingsService settingsService;


    public DeviceController(DeviceService deviceService, SettingsService settingsService) {
        this.deviceService = deviceService;
        this.settingsService = settingsService;
    }

	@Operation(summary = "Получить данные всех датчиков для дашборда")
	@GetMapping("/dashboard")
	public ResponseEntity<?> getDashboardData() {
		try {
			DashboardData data = deviceService.getDashboardData();
			return ResponseEntity.ok(data);
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@Operation(summary = "Переподключение к Modbus устройству")
	@PostMapping("/reconnect")
	public ResponseEntity<?> reconnect() {
		try {
			deviceService.getModbusClient().reconnectButton();
			return ResponseEntity.ok().build();
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@Operation(summary = "Включить рекуператор")
	@PostMapping("/recuperator/enable")
	public ResponseEntity<?> enableRecuperator() {
		try {
			deviceService.enableRecuperator();
			return ResponseEntity.ok().build();
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@Operation(summary = "Выключить рекуператор")
	@PostMapping("/recuperator/disable")
	public ResponseEntity<?> disableRecuperator() {
		try {
			deviceService.disableRecuperator();
			return ResponseEntity.ok().build();
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@Operation(summary = "Обновить ручные настройки")
	@PostMapping("/manual-settings")
	public ResponseEntity<?> updateManualSettings(@RequestBody UpdateManualSettingsRequest request) {
		try {
			settingsService.updateManualSettings(request.getSupplySpeed(), request.getBoostPercent());
			return ResponseEntity.ok().build();
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@Operation(summary = "Обновить температурные уставки")
	@PostMapping("/temp-settings")
	public ResponseEntity<?> updateTempSettings(@RequestBody UpdateTempSettingsRequest request) {
		try {
			settingsService.updateTempSettings(
				request.getTargetZone1(),
				request.getTargetZone2(),
				request.getWaterWarnThreshold(),
				request.getWaterFailThreshold(),
				request.getInflowWarnThreshold(),
				request.getInflowFailThreshold(),
				request.getNeutralZoneT(),
				request.getHotValveMinProc(),
				request.getWaterMinTNoWork(),
				request.getWaterMinTWork()
			);
			return ResponseEntity.ok().build();
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

}