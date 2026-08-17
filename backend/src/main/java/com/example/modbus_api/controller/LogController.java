package com.example.modbus_api.controller;

import com.example.modbus_api.logging.ActionType;
import com.example.modbus_api.service.ReadingsLogService;
import com.example.modbus_api.service.UserActionLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/logs")
public class LogController {

    private final UserActionLogService userActionLogService;
    private final ReadingsLogService readingsLogService;

    public LogController(UserActionLogService userActionLogService, ReadingsLogService readingsLogService) {
        this.userActionLogService = userActionLogService;
        this.readingsLogService = readingsLogService;
    }

    @GetMapping("/settings")
    public ResponseEntity<?> getSettings() {
        return ResponseEntity.ok(Collections.singletonMap("intervalMinutes", readingsLogService.getIntervalMinutes()));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/settings")
    public ResponseEntity<?> updateSettings(@RequestBody Map<String, Integer> request) {
        String action = "Изменение интервала записи показаний";
        Integer minutes = request.get("intervalMinutes");
        if (minutes == null) {
            userActionLogService.log(ActionType.SETPOINT, action, false, "Не указан интервал записи");
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("error", "Не указан интервал записи"));
        }
        try {
            int oldMinutes = readingsLogService.getIntervalMinutes();
            readingsLogService.setIntervalMinutes(minutes);
            userActionLogService.log(ActionType.SETPOINT, action, true,
                    oldMinutes == minutes ? null : oldMinutes + " → " + minutes + " мин");
            return ResponseEntity.ok(Collections.singletonMap("intervalMinutes", minutes));
        } catch (IllegalArgumentException e) {
            userActionLogService.log(ActionType.SETPOINT, action, false, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @GetMapping("/actions")
    public ResponseEntity<List<Map<String, Object>>> getActions(
            @RequestParam(defaultValue = "60") int minutes,
            @RequestParam(defaultValue = "ALL") String type) {
        return ResponseEntity.ok(userActionLogService.findRecent(minutes, type));
    }

    @GetMapping("/readings/export")
    public void exportReadings(
            @RequestParam long from,
            @RequestParam long to,
            HttpServletResponse response) throws IOException {
        if (to <= from) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Конец периода должен быть позже начала");
            return;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
        String filename = "readings_" + format.format(new Date(from)) + "_" + format.format(new Date(to)) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        readingsLogService.writeXlsx(from, to, response.getOutputStream());
    }
}
