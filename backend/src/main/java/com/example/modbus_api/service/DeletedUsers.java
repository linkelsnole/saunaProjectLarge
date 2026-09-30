package com.example.modbus_api.service;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.annotation.PostConstruct;
import java.util.List;
@Service
public class DeletedUsers {
    private final JdbcTemplate jdbc;
    public DeletedUsers(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @PostConstruct public void initialize() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS web_deleted_users (username TEXT PRIMARY KEY, deleted_at BIGINT NOT NULL)");
    }
    public Long deletedAt(String username) {
        List<Long> times=jdbc.queryForList("SELECT deleted_at FROM web_deleted_users WHERE username=?",Long.class,username);
        return times.isEmpty()?null:times.get(0);
    }
    public boolean contains(String username) {return deletedAt(username)!=null;}
    public void record(String username) {
        jdbc.update("INSERT OR REPLACE INTO web_deleted_users(username,deleted_at) VALUES (?,?)",username,System.currentTimeMillis());
    }
}
