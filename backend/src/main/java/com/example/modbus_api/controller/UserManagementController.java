package com.example.modbus_api.controller;
import com.example.modbus_api.dto.UserManagementDto.*;
import com.example.modbus_api.service.*;
import com.example.modbus_api.logging.ActionType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import javax.validation.Valid;
import java.util.*;
@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserManagementController {
    private final UserManagementService users;
    private final UserActionLogService logs;
    public UserManagementController(UserManagementService users,UserActionLogService logs) {this.users=users;this.logs=logs;}
    @GetMapping public List<UserView> list() {return users.list();}
    @PostMapping public ResponseEntity<UserView> create(Authentication actor,@Valid @RequestBody Create request) {
        UserView result=users.create(actor.getName(),request);
        logs.log(ActionType.OTHER,"Создание пользователя",true,"id="+result.getId()+"; role="+result.getRole());
        return ResponseEntity.status(201).body(result);
    }
    @PatchMapping("/{id}/role") public UserView changeRole(Authentication actor,@PathVariable long id,@Valid @RequestBody ChangeRole request) {
        UserView result=users.changeRole(actor.getName(),id,request);
        logs.log(ActionType.OTHER,"Изменение роли пользователя",true,"id="+id+"; role="+result.getRole());
        return result;
    }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(Authentication actor,@PathVariable long id) {
        users.delete(actor.getName(),id);
        logs.log(ActionType.OTHER,"Удаление пользователя",true,"id="+id);
        return ResponseEntity.noContent().build();
    }
    @ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<?> invalid(Exception e) {return ResponseEntity.badRequest().body(Collections.singletonMap("message",e.getMessage()));}
    @ExceptionHandler(ConcurrentModificationException.class) public ResponseEntity<?> conflict(Exception e) {return ResponseEntity.status(409).body(Collections.singletonMap("message",e.getMessage()));}
    @ExceptionHandler(DataIntegrityViolationException.class) public ResponseEntity<?> duplicate() {return ResponseEntity.status(409).body(Collections.singletonMap("message","Такой логин уже занят"));}
    @ExceptionHandler(NoSuchElementException.class) public ResponseEntity<?> missing(Exception e) {return ResponseEntity.status(404).body(Collections.singletonMap("message",e.getMessage()));}
}
