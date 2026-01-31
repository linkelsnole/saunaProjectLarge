package com.example.modbus_api.controller;

import com.example.modbus_api.security.dto.AuthRequest;
import com.example.modbus_api.security.dto.AuthResponse;
import com.example.modbus_api.security.model.AppUser;
import com.example.modbus_api.security.model.Role;
import com.example.modbus_api.security.repository.UserRepository;
import com.example.modbus_api.security.service.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*")
@Tag(name = "Auth Controller", description = "Авторизация и регистрация пользователей")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Operation(summary = "Вход в систему (получение токена)")
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody AuthRequest authRequest) {
        logger.info("Попытка входа пользователя: {}", authRequest.getUsername());
        
        try {
            if (authRequest.getUsername() == null || authRequest.getUsername().trim().isEmpty()) {
                logger.warn("Попытка входа с пустым именем пользователя");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("INVALID_USERNAME", "Имя пользователя не может быть пустым"));
            }


            Boolean userExists;
            try {
                userExists = userRepository.existsByUsername(authRequest.getUsername());
                logger.debug("Проверка существования пользователя {}: {}", authRequest.getUsername(), userExists);
            } catch (DataAccessException e) {
                logger.error("Ошибка доступа к базе данных при проверке пользователя {}: {}", 
                    authRequest.getUsername(), e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ErrorResponse("DATABASE_ERROR", "Ошибка соединения с базой данных. Проверьте подключение к серверу."));
            } catch (Exception e) {
                logger.error("Неожиданная ошибка при проверке пользователя {}: {}", 
                    authRequest.getUsername(), e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ErrorResponse("DATABASE_ERROR", "Ошибка соединения с базой данных. Проверьте подключение к серверу."));
            }

            if (Boolean.FALSE.equals(userExists)) {
                logger.warn("Пользователь {} не найден в базе данных", authRequest.getUsername());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("USER_NOT_FOUND", "Пользователь с таким именем не найден"));
            }

            logger.debug("Попытка аутентификации пользователя {}", authRequest.getUsername());
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String jwt = jwtUtils.generateToken(userDetails);
            
            String role = userDetails.getAuthorities().iterator().next().getAuthority();

            logger.info("Успешный вход пользователя: {} с ролью: {}", authRequest.getUsername(), role);
            return ResponseEntity.ok(AuthResponse.builder()
                    .token(jwt)
                    .username(userDetails.getUsername())
                    .role(role)
                    .build());
        } catch (BadCredentialsException e) {
            logger.warn("Неверный пароль для пользователя: {}", authRequest.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("INVALID_PASSWORD", "Неверный пароль"));
        } catch (UsernameNotFoundException e) {
            logger.warn("Пользователь не найден: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("USER_NOT_FOUND", "Пользователь не найден"));
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
}