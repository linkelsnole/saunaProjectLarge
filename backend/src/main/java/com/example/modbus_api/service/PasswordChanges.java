package com.example.modbus_api.service;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.annotation.PostConstruct;
import java.util.List;
@Service
public class PasswordChanges {
 private final JdbcTemplate jdbc;
 public PasswordChanges(JdbcTemplate jdbc){this.jdbc=jdbc;}
 @PostConstruct public void initialize(){jdbc.execute("CREATE TABLE IF NOT EXISTS web_password_changes (username TEXT PRIMARY KEY, changed_at BIGINT NOT NULL)");}
 public Long changedAt(String username){List<Long> rows=jdbc.queryForList("SELECT changed_at FROM web_password_changes WHERE username=?",Long.class,username);return rows.isEmpty()?null:rows.get(0);}
 public void record(String username){jdbc.update("INSERT OR REPLACE INTO web_password_changes(username,changed_at) VALUES (?,?)",username,System.currentTimeMillis());}
}
