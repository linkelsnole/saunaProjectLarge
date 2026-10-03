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
import javax.validation.Valid;

import static com.example.modbus_api.service.ModbusConstants.*;

@RestController
@RequestMapping("/api/v1/devices")
@CrossOrigin(origins = "*")
@Tag(name = "Device Controller", description = "Управление датчиками и устройствами ПЛК")
public class DeviceController {

    private final DeviceService deviceService;
    private final SettingsService settingsService;
    private final ScheduleService scheduleService;
    private final UpgradeService upgradeService;
    private final UserActionLogService userActionLogService;


    public DeviceController(
		DeviceService deviceService,
		SettingsService settingsService,
		ScheduleService scheduleService,
		UpgradeService upgradeService,
		UserActionLogService userActionLogService
	) {
        this.deviceService = deviceService;
        this.settingsService = settingsService;
        this.scheduleService = scheduleService;
        this.upgradeService = upgradeService;
        this.userActionLogService = userActionLogService;
    }

	@Operation(summary = "Получить данные всех датчиков для дашборда")
	@GetMapping("/dashboard")
	@PreAuthorize("hasAnyRole('ADMIN','USER','READONLY','TESTER')")
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
        } catch (IllegalStateException e) {
            userActionLogService.log(ActionType.POWER, action, false, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(java.util.Collections.singletonMap("error", e.getMessage()));
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

	@Operation(summary = "Переключить управление на экран")
	@PostMapping("/screen-mode/activate")
	@PreAuthorize("hasAnyRole('ADMIN','USER')")
	public ResponseEntity<?> activateScreenMode() {
		return updateScreenMode(true);
	}

	@Operation(summary = "Переключить управление на расписание")
	@PostMapping("/screen-mode/deactivate")
	@PreAuthorize("hasAnyRole('ADMIN','USER')")
	public ResponseEntity<?> deactivateScreenMode() {
		return updateScreenMode(false);
	}

	private ResponseEntity<?> updateScreenMode(boolean screenMode) {
		String action = screenMode ? "Режим управления: экран" : "Режим управления: расписание";
		try {
			deviceService.setScreenMode(screenMode);
			userActionLogService.log(ActionType.MODE, action, true, null);
			return ResponseEntity.ok().build();
		} catch (IOException e) {
			userActionLogService.log(ActionType.MODE, action, false, "Ошибка обмена с контроллером");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ErrorResponse("MODBUS_ERROR", "Не удалось переключить режим управления"));
		}
	}

	@Operation(summary = "Получить все слоты расписания")
	@GetMapping("/schedules")
	@PreAuthorize("hasAnyRole('ADMIN','USER','READONLY','TESTER','MGM')")
	public ResponseEntity<?> getSchedules() {
		try {
			return ResponseEntity.ok(scheduleService.getAllSchedules());
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ErrorResponse("MODBUS_ERROR", "Не удалось прочитать расписания"));
		}
	}

	@Operation(summary = "Сохранить группу расписания")
	@PutMapping("/schedules/group/{groupId}")
	@PreAuthorize("hasAnyRole('ADMIN','USER')")
	public ResponseEntity<?> updateScheduleGroup(
		@PathVariable int groupId,
		@Valid @RequestBody UpdateScheduleGroupRequest request
	) {
		String action = "Изменение группы расписаний " + groupId;
		try {
			scheduleService.updateScheduleGroup(groupId, request);
			userActionLogService.log(ActionType.SETPOINT, action, true, null);
			return ResponseEntity.ok().build();
		} catch (IllegalArgumentException e) {
			userActionLogService.log(ActionType.SETPOINT, action, false, e.getMessage());
			return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION_ERROR", e.getMessage()));
		} catch (IOException e) {
			userActionLogService.log(ActionType.SETPOINT, action, false, "Ошибка обмена с контроллером");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ErrorResponse("MODBUS_ERROR", "Не удалось сохранить расписание"));
		}
	}

	@Operation(summary = "Изменить активность группы расписания")
	@PatchMapping("/schedules/group/{groupId}/active")
	@PreAuthorize("hasAnyRole('ADMIN','USER')")
	public ResponseEntity<?> toggleScheduleGroup(
		@PathVariable int groupId,
		@Valid @RequestBody ToggleScheduleGroupActiveRequest request
	) {
		String action = (request.isActive() ? "Включение" : "Выключение")
			+ " группы расписаний " + groupId;
		try {
			scheduleService.toggleScheduleGroupActive(groupId, request.isActive());
			userActionLogService.log(ActionType.MODE, action, true, null);
			return ResponseEntity.ok().build();
		} catch (IllegalArgumentException e) {
			userActionLogService.log(ActionType.MODE, action, false, e.getMessage());
			return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION_ERROR", e.getMessage()));
		} catch (IOException e) {
			userActionLogService.log(ActionType.MODE, action, false, "Ошибка обмена с контроллером");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ErrorResponse("MODBUS_ERROR", "Не удалось изменить активность расписания"));
		}
	}

	@Operation(summary = "Выбрать режим вентиляции через UserSet")
	@PostMapping("/ventilation-mode/{modeCode}")
	@PreAuthorize("hasAnyRole('ADMIN','USER')")
	public ResponseEntity<?> setVentilationMode(@PathVariable int modeCode) {
		String modeName = ModbusRegisterMap.ventilationModeName(modeCode);
		String action = "Режим вентиляции: " + (modeName == null ? modeCode : modeName);
		try {
			settingsService.setVentilationMode(modeCode);
			userActionLogService.log(ActionType.MODE, action, true, null);
			return ResponseEntity.ok().build();
		} catch (IllegalArgumentException e) {
			userActionLogService.log(ActionType.MODE, action, false, e.getMessage());
			return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION_ERROR", e.getMessage()));
		} catch (IOException e) {
			userActionLogService.log(ActionType.MODE, action, false, "Ошибка обмена с контроллером");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ErrorResponse("MODBUS_ERROR", "Не удалось изменить режим вентиляции"));
		}
	}

	@Operation(summary = "Выбрать тип регулирования температуры для зон через UserSet")
	@PostMapping("/temperature-control")
	@PreAuthorize("hasAnyRole('ADMIN','USER')")
	public ResponseEntity<?> setTemperatureControl(@RequestBody UpdateTemperatureControlRequest request) {
		String action = "Смена типа регулирования температуры";
		try {
			settingsService.setTemperatureControlModes(request);
			userActionLogService.log(ActionType.MODE, action, true,
				"Зона 1: " + request.getZone1Mode() + "; зона 2: " + request.getZone2Mode());
			return ResponseEntity.ok().build();
		} catch (IllegalArgumentException e) {
			userActionLogService.log(ActionType.MODE, action, false, e.getMessage());
			return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION_ERROR", e.getMessage()));
		} catch (IOException e) {
			userActionLogService.log(ActionType.MODE, action, false, "Ошибка обмена с контроллером");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ErrorResponse("MODBUS_ERROR", "Не удалось изменить тип регулирования температуры"));
		}
	}

	@Operation(summary = "Обновить уставки ОВ, ПВ или ОРП")
	@PostMapping("/temperature-setpoints")
	@PreAuthorize("hasAnyRole('ADMIN','USER')")
	public ResponseEntity<?> updateTemperatureSetpoints(@RequestBody UpdateTemperatureSetpointsRequest request) {
		String action = "Запись температурных уставок зоны";
		try {
			settingsService.updateTemperatureSetpoints(request);
			userActionLogService.log(ActionType.SETPOINT, action, true, null);
			return ResponseEntity.ok().build();
		} catch (IllegalArgumentException e) {
			userActionLogService.log(ActionType.SETPOINT, action, false, e.getMessage());
			return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION_ERROR", e.getMessage()));
		} catch (IOException e) {
			userActionLogService.log(ActionType.SETPOINT, action, false, "Ошибка обмена с контроллером");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ErrorResponse("MODBUS_ERROR", "Не удалось записать температурные уставки"));
		}
	}

	@Operation(summary = "Обновить настройки объёма вентиляции")
	@PostMapping("/volume-settings")
	@PreAuthorize("hasAnyRole('ADMIN','USER')")
	public ResponseEntity<?> updateVolumeSettings(@RequestBody UpdateVolumeSettingsRequest request) {
		String action = "Запись настроек объёма";
		if (isRole("ROLE_USER") && request != null && request.hasPresetValues()) {
			userActionLogService.log(ActionType.SETPOINT, action, false, "Недостаточно прав для уставок режимов");
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(new ErrorResponse("FORBIDDEN", "Уставки ЭКО/Норм/Интенсив доступны только администратору"));
		}
		try {
			settingsService.updateVolumeSettings(request);
			userActionLogService.log(ActionType.SETPOINT, action, true, describeVolumeSettings(request));
			return ResponseEntity.ok().build();
		} catch (IllegalArgumentException e) {
			userActionLogService.log(ActionType.SETPOINT, action, false, e.getMessage());
			return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION_ERROR", e.getMessage()));
		} catch (IOException e) {
			userActionLogService.log(ActionType.SETPOINT, action, false, "Ошибка обмена с контроллером");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ErrorResponse("MODBUS_ERROR", "Не удалось сохранить настройки объёма"));
		}
	}

	@Operation(summary = "Обновить ручные настройки")
	@PostMapping("/manual-settings")
	@PreAuthorize("hasAnyRole('ADMIN','USER')")
	public ResponseEntity<?> updateManualSettings(@RequestBody UpdateManualSettingsRequest request) {
		String action = "Запись ручных уставок";
		try {
			List<String> parts = new ArrayList<>();
			if (request.getSupplySpeed() != null) {
				parts.add("Ручной объём → " + request.getSupplySpeed() + " м³/ч");
			}
			if (request.getBoostPercent() != null) {
				parts.add("Наддув → " + request.getBoostPercent() + " %");
			}

			settingsService.updateManualSettings(request.getSupplySpeed(), request.getBoostPercent());
			userActionLogService.log(ActionType.SETPOINT, action, true,
					parts.isEmpty() ? null : String.join("; ", parts));
			return ResponseEntity.ok().build();
		} catch (IOException e) {
			userActionLogService.log(ActionType.SETPOINT, action, false, e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	private static String describeVolumeSettings(UpdateVolumeSettingsRequest request) {
		if (request == null) {
			return null;
		}
		List<String> parts = new ArrayList<>();
		if (request.getEcoAirflow() != null) parts.add("ЭКО → " + request.getEcoAirflow() + " м³/ч");
		if (request.getNormalAirflow() != null) parts.add("Норм → " + request.getNormalAirflow() + " м³/ч");
		if (request.getIntensiveAirflow() != null) parts.add("Интенсив → " + request.getIntensiveAirflow() + " м³/ч");
		if (request.getManualAirflow() != null) parts.add("Ручной → " + request.getManualAirflow() + " м³/ч");
		if (request.getBoostPercent() != null) parts.add("Наддув → " + request.getBoostPercent() + " %");
		return parts.isEmpty() ? null : String.join("; ", parts);
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
			appendChange(parts, old, REG_R_TARGET_RPM_MIN, 100.0, "Мин. обороты", "об/мин", request.getTargetRpmMin());
			appendChange(parts, old, REG_R_TARGET_RPM_MAX, 100.0, "Макс. обороты", "об/мин", request.getTargetRpmMax());
			appendChange(parts, old, REG_R_TARGET_RPM_STEP, 100.0, "Шаг калибровки", "об/мин", request.getTargetRpmStep());
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
		appendChange(parts, old, REG_T_VALVE_START_POS, 10.0, "Положение вентилей при запуске", "%", request.getValveStartupPosition());
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
