package com.example.modbus_api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.modbus_api.dto.*;
import com.example.modbus_api.service.*;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;
import com.example.modbus_api.modbus.legacy.ArchESSEvent;

@RestController
@RequestMapping("/api/v1/devices")
@CrossOrigin(origins = "*") 
@Tag(name = "Device Controller", description = "Управление датчиками и устройствами ПЛК")
public class DeviceController {

    private final DeviceService deviceService;
    private final SettingsService settingsService;


    public DeviceController(
		DeviceService deviceService,
		SettingsService settingsService
	) {
        this.deviceService = deviceService;
        this.settingsService = settingsService;
    }

	@Operation(summary = "Получить данные всех датчиков для дашборда")
	@GetMapping("/dashboard")
	@PreAuthorize("hasAnyRole('ADMIN','USER','READONLY')")
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
	@PreAuthorize("hasRole('ADMIN')")
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
	@PreAuthorize("hasAnyRole('ADMIN','USER')")
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
	@PreAuthorize("hasAnyRole('ADMIN','USER')")
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
	@PreAuthorize("hasAnyRole('ADMIN','USER')")
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
	@PreAuthorize("hasAnyRole('ADMIN','USER')")
	public ResponseEntity<?> updateTempSettings(@RequestBody UpdateTempSettingsRequest request) {
		if (isRole("ROLE_USER") && hasNonZoneTempFields(request)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(new ErrorResponse("FORBIDDEN", "Недостаточно прав для изменения этих уставок"));
		}
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

	@Operation(summary = "Обновить настройки рекуператора")
	@PostMapping("/recuperator-settings")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> updateRecupSettings(@RequestBody UpdateRecupSettingsReq request) {
		try {
			settingsService.updateRecuperatorSettings(
				request.getTargetRpmMin(),
				request.getTargetRpmMax(),
				request.getTargetRpmStep()
			);
			return ResponseEntity.ok().build();
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@Operation(summary = "Получить журнал событий")
	@GetMapping("/events")
	@PreAuthorize("hasAnyRole('ADMIN','USER','READONLY')")
	public ResponseEntity<?> getEvents(@RequestParam(defaultValue = "60") int minutes) {
		try {
			List<ArchESSEvent> events = deviceService.getSystemEvents(minutes);
			return ResponseEntity.ok(events);
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ErrorResponse("API_ERROR", e.getMessage()));
		}
	}

	private boolean isRole(String role) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || authentication.getAuthorities() == null) {
			return false;
		}
		return authentication.getAuthorities().stream().anyMatch(a -> role.equals(a.getAuthority()));
	}

	private boolean hasNonZoneTempFields(UpdateTempSettingsRequest request) {
		return request.getWaterWarnThreshold() != null
			|| request.getWaterFailThreshold() != null
			|| request.getInflowWarnThreshold() != null
			|| request.getInflowFailThreshold() != null
			|| request.getNeutralZoneT() != null
			|| request.getHotValveMinProc() != null
			|| request.getWaterMinTNoWork() != null
			|| request.getWaterMinTWork() != null;
	}
	

}