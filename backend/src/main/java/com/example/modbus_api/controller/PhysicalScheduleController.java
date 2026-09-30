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
            logs.log(ActionType.SETPOINT,action,result.isVerified(),request.getChanges().toString()+"; apply="+request.isApply());
            return result;
        } catch(IOException|RuntimeException e) {
            logs.log(ActionType.SETPOINT,action,false,e.getMessage());
            throw e;
        }
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
