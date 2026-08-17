package com.example.modbus_api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.modbus_api.dto.*;
import com.example.modbus_api.logging.ActionType;
import com.example.modbus_api.logging.LogAction;
import com.example.modbus_api.modbus.model.ErrorList;
import com.example.modbus_api.service.*;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.example.modbus_api.service.ModbusConstants.*;

@RestController
@RequestMapping("/api/v1/devices")
@CrossOrigin(origins = "*")
@Tag(name = "Device Controller", description = "Управление датчиками и устройствами ПЛК")
public class DeviceController {

    private final DeviceService deviceService;
    private final SettingsService settingsService;
    private final UpgradeService upgradeService;
    private final UserActionLogService userActionLogService;


    public DeviceController(
		DeviceService deviceService,
		SettingsService settingsService,
		UpgradeService upgradeService,
		UserActionLogService userActionLogService
	) {
        this.deviceService = deviceService;
        this.settingsService = settingsService;
        this.upgradeService = upgradeService;
        this.userActionLogService = userActionLogService;
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
	@LogAction(type = ActionType.OTHER, value = "Реконнект с контроллером")
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
		String action = "Включение рекуператора";
		try {
			deviceService.enableRecuperator();
			userActionLogService.log(ActionType.POWER, action, true, null);
			return ResponseEntity.ok().build();
		} catch (IOException e) {
			userActionLogService.log(ActionType.POWER, action, false, e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@Operation(summary = "Выключить рекуператор")
	@PostMapping("/recuperator/disable")
	@PreAuthorize("hasAnyRole('ADMIN','USER')")
	public ResponseEntity<?> disableRecuperator() {
		String action = "Выключение рекуператора";
		try {
			deviceService.disableRecuperator();
			userActionLogService.log(ActionType.POWER, action, true, null);
			return ResponseEntity.ok().build();
		} catch (IOException e) {
			userActionLogService.log(ActionType.POWER, action, false, e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@Operation(summary = "Обновить ручные настройки")
	@PostMapping("/manual-settings")
	@PreAuthorize("hasAnyRole('ADMIN','USER')")
	public ResponseEntity<?> updateManualSettings(@RequestBody UpdateManualSettingsRequest request) {
		String action = "Запись ручных уставок";
		try {
			List<Integer> regs = new ArrayList<>();
			if (request.getSupplySpeed() != null) {
				regs.add(REG_V_VManualProcIn);
			}
			if (request.getBoostPercent() != null) {
				regs.add(REG_V_VManualDProcOut);
			}
			Map<Integer, Integer> old = readOldValues(regs);

			List<String> parts = new ArrayList<>();
			appendChange(parts, old, REG_V_VManualProcIn, 10.0, "Скорость притока", "%", request.getSupplySpeed());
			appendChange(parts, old, REG_V_VManualDProcOut, 10.0, "Буст вытяжки", "%", request.getBoostPercent());

			settingsService.updateManualSettings(request.getSupplySpeed(), request.getBoostPercent());
			userActionLogService.log(ActionType.SETPOINT, action, true,
					parts.isEmpty() ? null : String.join("; ", parts));
			return ResponseEntity.ok().build();
		} catch (IOException e) {
			userActionLogService.log(ActionType.SETPOINT, action, false, e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@Operation(summary = "Обновить температурные уставки")
	@PostMapping("/temp-settings")
	@PreAuthorize("hasAnyRole('ADMIN','USER')")
	public ResponseEntity<?> updateTempSettings(@RequestBody UpdateTempSettingsRequest request) {
		String action = "Запись уставок температуры";
		if (isRole("ROLE_USER") && hasNonZoneTempFields(request)) {
			userActionLogService.log(ActionType.SETPOINT, action, false, "Недостаточно прав для изменения этих уставок");
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(new ErrorResponse("FORBIDDEN", "Недостаточно прав для изменения этих уставок"));
		}
		try {
			String details = describeTempSettingsChanges(request);
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
				request.getWaterMinTWork(),
				request.getValveStartupPosition(),
				request.getControlAlgorithm(),
				request.getTempTrackingInterval()
			);
			userActionLogService.log(ActionType.SETPOINT, action, true, details);
			return ResponseEntity.ok().build();
		} catch (IOException e) {
			userActionLogService.log(ActionType.SETPOINT, action, false, e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@Operation(summary = "Обновить настройки рекуператора")
	@PostMapping("/recuperator-settings")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> updateRecupSettings(@RequestBody UpdateRecupSettingsReq request) {
		String action = "Запись уставок рекуператора";
		try {
			List<Integer> regs = new ArrayList<>();
			if (request.getTargetRpmMin() != null) {
				regs.add(REG_R_TARGET_RPM_MIN);
			}
			if (request.getTargetRpmMax() != null) {
				regs.add(REG_R_TARGET_RPM_MAX);
			}
			if (request.getTargetRpmStep() != null) {
				regs.add(REG_R_TARGET_RPM_STEP);
			}
			if (request.getTrackingIntervalRecup() != null) {
				regs.add(REG_R_TRACK_INTERVAL);
			}
			Map<Integer, Integer> old = readOldValues(regs);

			List<String> parts = new ArrayList<>();
			appendChange(parts, old, REG_R_TARGET_RPM_MIN, 10.0, "Мин. обороты", "об/мин", request.getTargetRpmMin());
			appendChange(parts, old, REG_R_TARGET_RPM_MAX, 10.0, "Макс. обороты", "об/мин", request.getTargetRpmMax());
			appendChange(parts, old, REG_R_TARGET_RPM_STEP, 10.0, "Шаг калибровки", "об/мин", request.getTargetRpmStep());
			appendChange(parts, old, REG_R_TRACK_INTERVAL, 1.0, "Интервал слежения", "с", request.getTrackingIntervalRecup());

			settingsService.updateRecuperatorSettings(
				request.getTargetRpmMin(),
				request.getTargetRpmMax(),
				request.getTargetRpmStep(),
				request.getTrackingIntervalRecup()
			);
			userActionLogService.log(ActionType.SETPOINT, action, true,
					parts.isEmpty() ? null : String.join("; ", parts));
			return ResponseEntity.ok().build();
		} catch (IOException e) {
			userActionLogService.log(ActionType.SETPOINT, action, false, e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	// ==================== Upgrade ====================

	@GetMapping("/server-state")
	public ResponseEntity<?> getServerState() {
		try {
			return ResponseEntity.ok(upgradeService.getServerState());
		} catch (IOException e) {
			return ResponseEntity.internalServerError()
					.body(Collections.singletonMap("error", "Ошибка получения состояния сервера: " + e.getMessage()));
		}
	}

	@GetMapping("/upgrade/url")
	public ResponseEntity<?> getUpgradeStoredUrl() {
		try {
			String url = upgradeService.readStoredUpgradeUrl();
			return ResponseEntity.ok(Collections.singletonMap("url", url != null ? url : ""));
		} catch (IOException e) {
			return ResponseEntity.internalServerError()
					.body(Collections.singletonMap("error", "Ошибка чтения URL обновления с контроллера: " + e.getMessage()));
		}
	}

	@PreAuthorize("hasRole('ADMIN')")
	@LogAction(type = ActionType.OTHER, value = "Изменение URL обновления")
	@PostMapping("/upgrade/url")
	public ResponseEntity<?> setUpgradeStoredUrl(@RequestParam(value = "url", required = false) String url) {
		try {
			upgradeService.writeStoredUpgradeUrl(url != null ? url : "");
			return ResponseEntity.ok(Collections.singletonMap("url", url != null ? url : ""));
		} catch (IOException e) {
			return ResponseEntity.internalServerError()
					.body(Collections.singletonMap("error", "Ошибка записи URL обновления на контроллер: " + e.getMessage()));
		}
	}

	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/upgrade/load")
	public ResponseEntity<?> upgradeLoad(
			@RequestParam(required = false) String url,
			@RequestParam(defaultValue = "false") boolean force) {
		try {
			ErrorList result = upgradeService.loadUpgrade(url, force);
			if (force) {
				userActionLogService.log(ActionType.OTHER, "Загрузка обновления", true, null);
			}
			return ResponseEntity.ok(result);
		} catch (IOException e) {
			if (force) {
				userActionLogService.log(ActionType.OTHER, "Загрузка обновления", false, e.getMessage());
			}
			return ResponseEntity.internalServerError()
					.body(Collections.singletonMap("error", "Ошибка проверки/загрузки обновления: " + e.getMessage()));
		}
	}

	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("/upgrade/execute")
	public ResponseEntity<?> upgradeExecute(
		@RequestParam(defaultValue = "false") boolean force,
		@RequestParam(defaultValue = "false") boolean self
	) {
		try {
			if (force && deviceService.isCalibratingState()) {
				userActionLogService.log(ActionType.OTHER, "Установка обновления", false,
						"Отклонено: идет калибровка агрегата");
				return ResponseEntity.status(HttpStatus.CONFLICT)
						.body(Collections.singletonMap("error",
								"Нельзя запускать установку обновления во время калибровки агрегата"));
			}
			ErrorList result = upgradeService.executeUpgrade(force, self);
			if (force) {
				userActionLogService.log(ActionType.OTHER, "Установка обновления", true, null);
			}
			return ResponseEntity.ok(result);
		} catch (IOException e) {
			if (force) {
				userActionLogService.log(ActionType.OTHER, "Установка обновления", false, e.getMessage());
			}
			return ResponseEntity.internalServerError()
					.body(Collections.singletonMap("error", "Ошибка проверки/установки обновления: " + e.getMessage()));
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
			|| request.getWaterMinTWork() != null
			|| request.getValveStartupPosition() != null
			|| request.getControlAlgorithm() != null
			|| request.getTempTrackingInterval() != null;
	}

	private Map<Integer, Integer> readOldValues(List<Integer> regs) throws IOException {
		if (regs.isEmpty()) {
			return Collections.emptyMap();
		}
		return deviceService.getModbusClient().readRegisters(SAUNA_PLC, regs);
	}

	private String describeTempSettingsChanges(UpdateTempSettingsRequest request) throws IOException {
		List<Integer> regs = new ArrayList<>();
		if (request.getTargetZone1() != null) regs.add(REG_T_TARGET_ZONE1);
		if (request.getTargetZone2() != null) regs.add(REG_T_TARGET_ZONE2);
		if (request.getWaterWarnThreshold() != null) regs.add(REG_T_WATER_WARN);
		if (request.getWaterFailThreshold() != null) regs.add(REG_T_WATER_FAIL);
		if (request.getInflowWarnThreshold() != null) regs.add(REG_T_INFLOW_WARN);
		if (request.getInflowFailThreshold() != null) regs.add(REG_T_INFLOW_FAIL);
		if (request.getNeutralZoneT() != null) regs.add(REG_T_NEUTRAL_ZONE);
		if (request.getHotValveMinProc() != null) regs.add(REG_HOT_VALVE_MIN_PROC);
		if (request.getWaterMinTNoWork() != null) regs.add(REG_T_WATER_NO_WORK);
		if (request.getWaterMinTWork() != null) regs.add(REG_T_WATER_WORK);
		if (request.getValveStartupPosition() != null) regs.add(REG_T_VALVE_START_POS);
		if (request.getControlAlgorithm() != null) regs.add(REG_T_CTRL_ALGORITHM);
		if (request.getTempTrackingInterval() != null) regs.add(REG_T_TRACK_INTERVAL);
		Map<Integer, Integer> old = readOldValues(regs);

		List<String> parts = new ArrayList<>();
		appendChange(parts, old, REG_T_TARGET_ZONE1, 1.0, "Целевая T зоны 1", "°C", request.getTargetZone1());
		appendChange(parts, old, REG_T_TARGET_ZONE2, 1.0, "Целевая T зоны 2", "°C", request.getTargetZone2());
		appendChange(parts, old, REG_T_WATER_WARN, 10.0, "Порог T воды (снижение)", "°C", request.getWaterWarnThreshold());
		appendChange(parts, old, REG_T_WATER_FAIL, 10.0, "Порог T воды (авария)", "°C", request.getWaterFailThreshold());
		appendChange(parts, old, REG_T_INFLOW_WARN, 10.0, "Порог T притока (снижение)", "°C", request.getInflowWarnThreshold());
		appendChange(parts, old, REG_T_INFLOW_FAIL, 10.0, "Порог T притока (авария)", "°C", request.getInflowFailThreshold());
		appendChange(parts, old, REG_T_NEUTRAL_ZONE, 10.0, "Нейтральная зона T", "°C", request.getNeutralZoneT());
		appendChange(parts, old, REG_HOT_VALVE_MIN_PROC, 10.0, "Мин. открытие вентиля ГВ", "%", request.getHotValveMinProc());
		appendChange(parts, old, REG_T_WATER_NO_WORK, 10.0, "Мин. T воды без работы", "°C", request.getWaterMinTNoWork());
		appendChange(parts, old, REG_T_WATER_WORK, 10.0, "Мин. T воды в работе", "°C", request.getWaterMinTWork());
		appendChange(parts, old, REG_T_VALVE_START_POS, 1.0, "Положение вентилей при запуске", "%", request.getValveStartupPosition());
		appendChange(parts, old, REG_T_CTRL_ALGORITHM, 1.0, "Алгоритм управления по T", "", request.getControlAlgorithm());
		appendChange(parts, old, REG_T_TRACK_INTERVAL, 1.0, "Интервал слежения по T", "с", request.getTempTrackingInterval());
		return parts.isEmpty() ? null : String.join("; ", parts);
	}

	private static void appendChange(List<String> parts, Map<Integer, Integer> old, int register,
									 double divisor, String label, String unit, Number newValue) {
		if (newValue == null) {
			return;
		}
		Integer raw = old.get(register);
		String oldText = raw == null || raw == INVALID_VALUE ? "—" : formatNumber(raw / divisor);
		String newText = formatNumber(newValue.doubleValue());
		if (oldText.equals(newText)) {
			return;
		}
		parts.add(label + ": " + oldText + " → " + newText + (unit.isEmpty() ? "" : " " + unit));
	}

	private static String formatNumber(double value) {
		return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
	}
}
