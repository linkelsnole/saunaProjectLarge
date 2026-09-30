package com.example.modbus_api;

import com.example.modbus_api.config.JwtProperties;
import com.example.modbus_api.service.ModbusClientAdapter;
import com.example.modbus_api.service.ReadingsLogService;
import com.example.modbus_api.security.model.RefreshToken;
import com.example.modbus_api.security.repository.RefreshTokenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import javax.servlet.http.Cookie;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"jwt.access-token-expiration-ms=2000", "spring.jpa.show-sql=false"})
@AutoConfigureMockMvc
class SaunaSessionIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JwtProperties properties;
    @Autowired RefreshTokenRepository sessions;
    @Autowired JdbcTemplate jdbc;
    @MockBean ModbusClientAdapter modbus;
    @MockBean ReadingsLogService readings;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) throws Exception {
        Path dir = Files.createTempDirectory("sauna-session-test-");
        String url = "jdbc:sqlite:" + dir.resolve("auth.db");
        try (Connection c = DriverManager.getConnection(url); Statement s = c.createStatement()) {
            // An upgrade must work even when the old table enforces one token per user.
            s.execute("CREATE TABLE refresh_tokens (id TEXT PRIMARY KEY, token TEXT UNIQUE NOT NULL, expiry_at BIGINT NOT NULL, user_id INTEGER UNIQUE NOT NULL)");
            s.execute("INSERT INTO refresh_tokens VALUES ('legacy', 'legacy-test-only', 0, 999)");
        }
        registry.add("spring.datasource.url", () -> url);
    }

    private String register() throws Exception {
        String username = "session_test_" + UUID.randomUUID().toString().replace("-", "");
        mvc.perform(post("/api/v1/auth/register").contentType("application/json")
            .content("{\"username\":\"" + username + "\",\"password\":\"synthetic-local-test-password\"}"))
            .andExpect(status().isOk());
        return username;
    }

    private MvcResult login(String username) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType("application/json")
            .content("{\"username\":\"" + username + "\",\"password\":\"synthetic-local-test-password\"}"))
            .andExpect(status().isOk()).andReturn();
    }

    private Cookie cookie(MvcResult result) {
        String header = result.getResponse().getHeader("Set-Cookie");
        assertNotNull(header);
        assertTrue(header.contains("HttpOnly"));
        assertTrue(header.startsWith(properties.getRefreshCookieName() + "="));
        return new Cookie(properties.getRefreshCookieName(), header.split(";", 2)[0].split("=", 2)[1]);
    }

    private MvcResult refresh(Cookie cookie) throws Exception {
        return mvc.perform(post("/api/v1/auth/refresh").cookie(cookie))
            .andExpect(status().isOk()).andReturn();
    }

    @Test void multipleBrowsersStayLoggedInAndLogoutIsIndependent() throws Exception {
        String username = register();
        Cookie first = cookie(login(username));
        Cookie second = cookie(login(username));
        assertNotEquals(first.getValue(), second.getValue());
        refresh(first);
        refresh(second);
        mvc.perform(post("/api/v1/auth/logout").cookie(first)).andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/auth/refresh").cookie(first)).andExpect(status().isUnauthorized());
        refresh(second);
    }

    @Test void tabsAndLostResponsesCanReuseTheirSessionCookie() throws Exception {
        Cookie session = cookie(login(register()));
        assertEquals(session.getValue(), cookie(refresh(session)).getValue());
        assertEquals(session.getValue(), cookie(refresh(session)).getValue());
        assertEquals(session.getValue(), cookie(refresh(session)).getValue());
    }

    @Test void accessTokenExpiresButRefreshRestoresAuthenticatedRequests() throws Exception {
        MvcResult loggedIn = login(register());
        Cookie session = cookie(loggedIn);
        String oldAccess = json.readTree(loggedIn.getResponse().getContentAsString()).get("accessToken").asText();
        Thread.sleep(2300);
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + oldAccess))
            .andExpect(status().isUnauthorized());
        MvcResult refreshed = mvc.perform(post("/api/v1/auth/refresh").cookie(session)
            .header("Authorization", "Bearer " + oldAccess)).andExpect(status().isOk()).andReturn();
        String access = json.readTree(refreshed.getResponse().getContentAsString()).get("accessToken").asText();
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + access))
            .andExpect(status().isOk());
    }

    @Test void expiredAndMissingRefreshTokensStillRequireLogin() throws Exception {
        Cookie session = cookie(login(register()));
        RefreshToken saved = sessions.findByToken(session.getValue()).orElseThrow(AssertionError::new);
        saved.setExpiryAt(Instant.now().minusSeconds(1));
        sessions.saveAndFlush(saved);
        mvc.perform(post("/api/v1/auth/refresh").cookie(session)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh")).andExpect(status().isUnauthorized());
    }

    @Test void oldUniqueUserTableIsPreservedAndNewSessionsUseANewTable() throws Exception {
        Cookie session = cookie(login(register()));
        refresh(session);
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM refresh_tokens WHERE id='legacy'", Integer.class));
        assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM web_refresh_sessions", Integer.class) > 0);
        assertEquals("sauna_large_refresh_token", properties.getRefreshCookieName());
    }

    @Test void physicalEditorEnforcesRolesForReadWriteAndApply() throws Exception {
        for (String role : new String[] {"ROLE_USER", "ROLE_READONLY", "ROLE_TESTER", "ROLE_ADMIN"}) {
            String username = register();
            jdbc.update("UPDATE app_users SET role=? WHERE username=?", role, username);
            String token = json.readTree(login(username).getResponse().getContentAsString()).get("accessToken").asText();
            boolean allowed = role.equals("ROLE_ADMIN");
            org.mockito.Mockito.clearInvocations(modbus);
            mvc.perform(get("/api/v1/devices/physical-schedules").header("Authorization", "Bearer " + token))
                .andExpect(status().is(allowed ? 200 : 403));
            mvc.perform(patch("/api/v1/devices/physical-schedules/7").header("Authorization", "Bearer " + token)
                .contentType("application/json").content("{\"changes\":[{\"offset\":8,\"expected\":0,\"value\":42}],\"apply\":true}"))
                .andExpect(status().is(allowed ? 502 : 403));
            mvc.perform(post("/api/v1/devices/physical-schedules/apply").header("Authorization", "Bearer " + token))
                .andExpect(status().is(allowed ? 204 : 403));
            if (!allowed) org.mockito.Mockito.verifyNoInteractions(modbus);
        }
    }
}
