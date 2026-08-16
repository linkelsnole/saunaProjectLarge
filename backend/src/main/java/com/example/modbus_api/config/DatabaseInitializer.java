package com.example.modbus_api.config;

import com.example.modbus_api.security.model.AppUser;
import com.example.modbus_api.security.model.Role;
import com.example.modbus_api.security.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Admin user
        AppUser admin = userRepository.findByUsername("admin").orElse(null);
        if (admin == null) {
            admin = AppUser.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("RA285Q7n"))
                    .role(Role.ROLE_ADMIN)
                    .build();
            userRepository.save(admin);
            logger.info("Admin user created: admin / RA285Q7n");
        } else {
            // Обновляем пароль если пользователь уже существует
            admin.setPassword(passwordEncoder.encode("RA285Q7n"));
            admin.setRole(Role.ROLE_ADMIN);
            userRepository.save(admin);
            logger.info("Admin user password updated: admin / RA285Q7n");
        }

        // User
        AppUser user = userRepository.findByUsername("user").orElse(null);
        if (user == null) {
            user = AppUser.builder()
                    .username("user")
                    .password(passwordEncoder.encode("RU804f*"))
                    .role(Role.ROLE_USER)
                    .build();
            userRepository.save(user);
            logger.info("Default user created: user / RU804f*");
        } else {
            user.setPassword(passwordEncoder.encode("RU804f*"));
            user.setRole(Role.ROLE_USER);
            userRepository.save(user);
            logger.info("User password updated: user / RU804f*");
        }

        // Readonly user
        AppUser readOnlyUser = userRepository.findByUsername("readonly").orElse(null);
        if (readOnlyUser == null) {
            readOnlyUser = AppUser.builder()
                    .username("readonly")
                    .password(passwordEncoder.encode("RR104k$"))
                    .role(Role.ROLE_READONLY)
                    .build();
            userRepository.save(readOnlyUser);
            logger.info("Read-only user created: readonly / RR104k$");
        } else {
            readOnlyUser.setPassword(passwordEncoder.encode("RR104k$"));
            readOnlyUser.setRole(Role.ROLE_READONLY);
            userRepository.save(readOnlyUser);
            logger.info("Read-only user password updated: readonly / RR104k$");
        }

        AppUser configUser = userRepository.findByUsername("config").orElse(null);
        if (configUser == null) {
            configUser = AppUser.builder()
                    .username("config")
                    .password(passwordEncoder.encode("config123"))
                    .role(Role.ROLE_CONFIG)
                    .build();
            userRepository.save(configUser);
            logger.info("Config user created: config / config123");
        } else {
            configUser.setPassword(passwordEncoder.encode("config123"));
            configUser.setRole(Role.ROLE_CONFIG);
            userRepository.save(configUser);
            logger.info("Config user password updated: config / config123");
        }
    }
}