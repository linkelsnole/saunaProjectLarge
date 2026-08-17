package com.example.modbus_api.service;

import com.example.modbus_api.logging.ActionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserActionLogService {

    private static final Logger log = LoggerFactory.getLogger(UserActionLogService.class);
    private static final int MAX_ROWS = 500;

    private final JdbcTemplate jdbc;

    public UserActionLogService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    public void initSchema() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS user_action_log (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "ts INTEGER NOT NULL, " +
                "username TEXT NOT NULL, " +
                "action_type TEXT NOT NULL, " +
                "action TEXT NOT NULL, " +
                "success INTEGER NOT NULL, " +
                "details TEXT)");
        jdbc.execute("CREATE INDEX IF NOT EXISTS idx_user_action_log_ts ON user_action_log(ts)");
    }

    public void log(ActionType type, String action, boolean success, String details) {
        logAs(currentUsername(), type, action, success, details);
    }

    public void logAs(String username, ActionType type, String action, boolean success, String details) {
        try {
            jdbc.update("INSERT INTO user_action_log (ts, username, action_type, action, success, details) VALUES (?, ?, ?, ?, ?, ?)",
                    System.currentTimeMillis(),
                    username,
                    type.name(),
                    action,
                    success ? 1 : 0,
                    details != null && details.length() > 500 ? details.substring(0, 500) : details);
        } catch (Exception e) {
            log.warn("Не удалось записать лог действия '{}': {}", action, e.getMessage());
        }
    }

    public List<Map<String, Object>> findRecent(int minutes, String type) {
        int window = Math.max(1, Math.min(minutes, 7 * 24 * 60));
        long since = System.currentTimeMillis() - window * 60_000L;

        StringBuilder sql = new StringBuilder(
                "SELECT ts, username, action_type, action, success, details FROM user_action_log WHERE ts >= ?");
        List<Object> args = new ArrayList<>();
        args.add(since);
        if (type != null && !"ALL".equalsIgnoreCase(type)) {
            sql.append(" AND action_type = ?");
            args.add(type.toUpperCase());
        }
        sql.append(" ORDER BY ts DESC LIMIT ").append(MAX_ROWS);

        return jdbc.query(sql.toString(), args.toArray(), (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("timestamp", rs.getLong("ts"));
            row.put("username", rs.getString("username"));
            row.put("actionType", rs.getString("action_type"));
            row.put("action", rs.getString("action"));
            row.put("success", rs.getInt("success") == 1);
            row.put("details", rs.getString("details"));
            return row;
        });
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return "система";
        }
        return authentication.getName();
    }
}
