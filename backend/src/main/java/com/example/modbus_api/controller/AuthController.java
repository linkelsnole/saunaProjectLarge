package com.example.modbus_api.controller;

import com.example.modbus_api.exception.AuthException;
import com.example.modbus_api.security.dto.AuthRequest;
import com.example.modbus_api.security.dto.AuthResponse;
import com.example.modbus_api.security.dto.UserInfoResponse;
import com.example.modbus_api.security.model.AppUser;
import com.example.modbus_api.security.model.Role;
import com.example.modbus_api.security.repository.UserRepository;
import com.example.modbus_api.security.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth Controller", description = "Авторизация пользователей")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthService authService, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Operation(summary = "Вход в систему (access-токен + refresh-cookie)")
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest, HttpServletResponse response) {
        if (authRequest.getUsername() == null || authRequest.getUsername().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("INVALID_USERNAME", "Имя пользователя не может быть пустым"));
        }

        logger.info("Попытка входа пользователя: {}", authRequest.getUsername());
        try {
            AuthResponse body = authService.login(authRequest.getUsername(), authRequest.getPassword(), response);
            logger.info("Успешный вход пользователя: {} с ролью: {}", authRequest.getUsername(), body.getUser().getRole());
            return ResponseEntity.ok(body);
        } catch (BadCredentialsException e) {
            logger.warn("Неверные учётные данные для пользователя: {}", authRequest.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("INVALID_CREDENTIALS", "Неверный логин или пароль"));
        } catch (AuthenticationException e) {
            logger.warn("Ошибка аутентификации пользователя {}: {}", authRequest.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("AUTH_FAILED", "Неверный логин или пароль"));
        } catch (DataAccessException e) {
            logger.error("Ошибка доступа к базе данных при входе пользователя {}: {}",
                    authRequest.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ErrorResponse("DATABASE_ERROR", "Ошибка соединения с базой данных. Проверьте подключение к серверу."));
        } catch (Exception e) {
            logger.error("Неожиданная ошибка при входе пользователя {}: {}",
                    authRequest.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("SERVER_ERROR", "Произошла ошибка при входе. Попробуйте позже"));
        }
    }

    @Operation(summary = "Обновление access-токена по refresh-cookie")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        return ResponseEntity.ok(authService.refresh(request, response));
    }

    @Operation(summary = "Выход из системы (гасит refresh-cookie и токен в БД)")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Текущий пользователь")
    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> me(Authentication authentication) {
        return ResponseEntity.ok(authService.currentUser(authentication));
    }

    @Operation(summary = "Регистрация (для первичной настройки, можно закрыть позже)")
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody AuthRequest authRequest) {
        if (userRepository.existsByUsername(authRequest.getUsername())) {
            return ResponseEntity.badRequest().body("Error: Username is already taken!");
        }

        Role role = Role.ROLE_USER;
        if ("admin".equalsIgnoreCase(authRequest.getUsername())) {
            role = Role.ROLE_ADMIN;
        }

        AppUser user = AppUser.builder()
                .username(authRequest.getUsername())
                .password(passwordEncoder.encode(authRequest.getPassword()))
                .role(role)
                .build();

        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully!");
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(AuthException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("AUTH_ERROR", exception.getMessage()));
    }
}
