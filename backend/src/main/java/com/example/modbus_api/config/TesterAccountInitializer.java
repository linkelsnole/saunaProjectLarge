package com.example.modbus_api.config;

import com.example.modbus_api.security.model.AppUser;
import com.example.modbus_api.security.model.Role;
import com.example.modbus_api.security.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Optional one-time provisioning; existing accounts and passwords are never replaced.
@Component
public class TesterAccountInitializer implements CommandLineRunner {
    private final com.example.modbus_api.service.DeletedUsers deleted;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    @Value("${sauna.tester.username:tester}") private String username;
    @Value("${sauna.tester.password:}") private String password;
    public TesterAccountInitializer(UserRepository users,PasswordEncoder encoder,com.example.modbus_api.service.DeletedUsers deleted) { this.deleted=deleted; this.users=users; this.encoder=encoder; }
    @Override public void run(String... args) {
        if(password==null || password.isEmpty()) return;
        if(deleted.contains(username)) return;
        if(users.existsByUsername(username)) return;
        if(!username.matches("[A-Za-z0-9_.-]{3,32}") || password.length()<6 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)
            throw new IllegalArgumentException("Для тестировщика нужен логин 3–32 символа и пароль от 6 символов, до 72 байт UTF-8");
        users.save(AppUser.builder().username(username).password(encoder.encode(password)).role(Role.ROLE_TESTER).build());
    }
}
