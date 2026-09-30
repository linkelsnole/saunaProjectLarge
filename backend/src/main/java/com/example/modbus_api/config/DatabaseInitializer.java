package com.example.modbus_api.config;

import com.example.modbus_api.security.model.AppUser;
import com.example.modbus_api.security.model.Role;
import com.example.modbus_api.security.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final com.example.modbus_api.service.DeletedUsers deleted;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.example.modbus_api.service.DefaultCredentialsRotation credentials;

    public DatabaseInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder, com.example.modbus_api.service.DefaultCredentialsRotation credentials, com.example.modbus_api.service.DeletedUsers deleted) {
        this.deleted=deleted;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.credentials = credentials;
    }

    @Override
    public void run(String... args) throws Exception {
        credentials.applyOnce();

        // Readonly user
        if (!deleted.contains("readonly")) {
        AppUser readOnlyUser = userRepository.findByUsername("readonly").orElse(null);
        if (readOnlyUser == null) {
            readOnlyUser = AppUser.builder()
                    .username("readonly")
                    .password(passwordEncoder.encode("RR104k$"))
                    .role(Role.ROLE_READONLY)
                    .build();
            userRepository.save(readOnlyUser);
            logger.info("Read-only user created");
        } else {
            readOnlyUser.setPassword(passwordEncoder.encode("RR104k$"));
            readOnlyUser.setRole(Role.ROLE_READONLY);
            userRepository.save(readOnlyUser);
            logger.info("Read-only user credentials updated");
        }

        }

        if (!deleted.contains("config")) {
        AppUser configUser = userRepository.findByUsername("config").orElse(null);
        if (configUser == null) {
            configUser = AppUser.builder()
                    .username("config")
                    .password(passwordEncoder.encode("config123"))
                    .role(Role.ROLE_CONFIG)
                    .build();
            userRepository.save(configUser);
            logger.info("Config user created");
        } else {
            configUser.setPassword(passwordEncoder.encode("config123"));
            configUser.setRole(Role.ROLE_CONFIG);
            userRepository.save(configUser);
            logger.info("Config user credentials updated");
        }
        }

    }
}
