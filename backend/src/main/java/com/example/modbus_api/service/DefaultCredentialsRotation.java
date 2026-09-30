package com.example.modbus_api.service;

import com.example.modbus_api.security.model.AppUser;
import com.example.modbus_api.security.model.Role;
import com.example.modbus_api.security.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/** Release migration: rotate the two shared logins once, without storing plaintext credentials. */
@Service
public class DefaultCredentialsRotation {
    public static final String VERSION = "2026-09-26-admin-user-v2";
    public static final String PREVIOUS_PASSWORD_HASH = "$2a$10$YUOOon3Yv0FzfqempipSvuagi/LUbHXqHDxvl0znBK/OQT8LirQc6";
    public static final String DEFAULT_PASSWORD_HASH = "$2a$10$bh1HemRdaRzHz/tD2S5DOulayEGa3KdIcFPhPsXgUmrE354C2ZaEW";
    private final UserRepository users;
    private final JdbcTemplate jdbc;
    private final String passwordHash;
    private final PasswordEncoder encoder;

    public DefaultCredentialsRotation(UserRepository users, JdbcTemplate jdbc, PasswordEncoder encoder,
            @Value("${sauna.credentials.password-hash:" + DEFAULT_PASSWORD_HASH + "}") String passwordHash) {
        this.users = users; this.jdbc = jdbc; this.passwordHash = passwordHash; this.encoder = encoder;
    }
    public static boolean appliesTo(String username) {
        return "admin".equals(username) || "user".equals(username);
    }
    @Transactional
    public void applyOnce() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS web_credential_migrations (version TEXT PRIMARY KEY, applied_at BIGINT NOT NULL)");
        boolean applied = jdbc.queryForObject("SELECT COUNT(*) FROM web_credential_migrations WHERE version=?", Integer.class, VERSION) != 0;
        if (!passwordHash.matches("\\$2[aby]\\$[0-9]{2}\\$[./A-Za-z0-9]{53}")) throw new IllegalArgumentException("Invalid initial password hash");
        if (applied) {
            // Older JARs reset these credentials at every start. Repair that after a rollback,
            // while preserving any later administrator-chosen password and valid sessions.
            repairLegacyPassword("admin", "RA285Q7n", Role.ROLE_ADMIN);
            repairLegacyPassword("user", "RU804f*", Role.ROLE_USER);
            return;
        }
        rotate("admin", Role.ROLE_ADMIN);
        rotate("user", Role.ROLE_USER);
        jdbc.update("INSERT INTO web_credential_migrations (version,applied_at) VALUES (?,?)", VERSION, System.currentTimeMillis());
    }
    private void repairLegacyPassword(String username, String legacyPassword, Role role) {
        AppUser user = users.findByUsername(username).orElse(null);
        if (user != null && (PREVIOUS_PASSWORD_HASH.equals(user.getPassword()) || encoder.matches(legacyPassword, user.getPassword()))) rotate(username, role);
    }
    private void rotate(String username, Role initialRole) {
        AppUser user = users.findByUsername(username).orElse(null);
        if (user == null) user = AppUser.builder().username(username).role(initialRole).build();
        else jdbc.update("DELETE FROM web_refresh_sessions WHERE user_id=?", user.getId());
        user.setPassword(passwordHash);
        users.saveAndFlush(user);
    }
}
