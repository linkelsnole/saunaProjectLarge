package com.example.modbus_api.controller;

import com.example.modbus_api.service.TestingService;
import com.example.modbus_api.service.UserActionLogService;
import com.example.modbus_api.logging.ActionType;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/testing")
@PreAuthorize("hasRole('TESTER')")
public class TestingController {
    private final TestingService service;
    private final UserActionLogService log;
    public TestingController(TestingService service, UserActionLogService log) {
        this.service = service;
        this.log = log;
    }
    @GetMapping("/status")
    public Map<String, Object> status() throws IOException { return service.status(); }

    @PostMapping("/mode")
    public Map<String, Object> mode(@RequestBody ModeRequest request) throws IOException {
        if (request.getEnabled() == null) throw new IllegalArgumentException("Не указан режим");
        return audited("Режим тестирования: " + request.getEnabled(), () -> service.setMode(request.getEnabled()));
    }
    @PostMapping("/commands")
    public Map<String, Object> execute(@RequestBody CommandRequest request) throws IOException {
        if (request.getCommand() == null) throw new IllegalArgumentException("Не указана команда");
        return audited("Тестовая команда " + request.getCommand() + ", индекс " + request.getIndex() + ", значение " + request.getValue(),
            () -> service.execute(request.getCommand(), request.getIndex(), request.getValue()));
    }
    private Map<String, Object> audited(String action, Operation operation) throws IOException {
        try {
            Map<String, Object> result = operation.run();
            log.log(ActionType.OTHER, action, true, null);
            return result;
        } catch (IOException | IllegalArgumentException | IllegalStateException exception) {
            log.log(ActionType.OTHER, action, false, exception.getMessage());
            throw exception;
        }
    }
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, String>> invalid(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", exception.getMessage()));
    }
    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, String>> unavailable(IOException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Collections.singletonMap("error", "Связь с контроллером нарушена. Проверьте состояние перед повторной командой."));
    }
    private interface Operation { Map<String, Object> run() throws IOException; }
    @Data public static class ModeRequest { private Boolean enabled; }
    @Data public static class CommandRequest { private Integer command; private Integer index; private Integer value; }
}
