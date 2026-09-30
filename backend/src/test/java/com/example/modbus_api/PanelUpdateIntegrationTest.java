package com.example.modbus_api;

import com.example.modbus_api.security.model.*;
import com.example.modbus_api.security.repository.UserRepository;
import com.example.modbus_api.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.jpa.show-sql=false")
@AutoConfigureMockMvc
class PanelUpdateIntegrationTest {
    static Path root;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @MockBean ModbusClientAdapter modbus;
    @MockBean ReadingsLogService readings;
    static final String VERSION = "large-production-17-1";
    static final String PASSWORD = "synthetic-update-test-password";

    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) throws Exception {
        root = Files.createTempDirectory("sauna-panel-update-test-");
        r.add("spring.datasource.url", () -> "jdbc:sqlite:" + root.resolve("test.db"));
        r.add("panel.updates.directory", () -> root.toString());
    }
    String auth(Role role) throws Exception {
        AppUser user = users.saveAndFlush(AppUser.builder().username("up_" + UUID.randomUUID().toString().substring(0, 16))
                .password(encoder.encode(PASSWORD)).role(role).build());
        Map<String, String> body = new HashMap<>(); body.put("username", user.getUsername()); body.put("password", PASSWORD);
        String response = mvc.perform(post("/api/v1/auth/login").contentType("application/json").content(json.writeValueAsBytes(body)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(response).path("accessToken").asText();
    }
    @BeforeEach void reset() throws Exception {
        Files.deleteIfExists(root.resolve("request.json"));
        Files.deleteIfExists(root.resolve("history.json"));
        Files.deleteIfExists(root.resolve("heartbeat.json"));
        Files.write(root.resolve("ui-enabled"), new byte[0]);
        state(Instant.now().getEpochSecond());
    }
    void state(long time) throws Exception {
        Files.write(root.resolve("status.json"), ("{\"status\":\"available\",\"tag\":\""+VERSION+"\",\"checked_at\":"+time+"}").getBytes("UTF-8"));
    }
    @Test void onlyAdminCanSeeOrRequestUpdates() throws Exception {
        mvc.perform(get("/api/v1/panel-updates")).andExpect(status().isUnauthorized());
        for (Role role : Role.values()) {
            if (role == Role.ROLE_ADMIN) continue;
            String token = auth(role);
            mvc.perform(get("/api/v1/panel-updates").header("Authorization", token)).andExpect(status().isForbidden());
            mvc.perform(post("/api/v1/panel-updates").header("Authorization", token).contentType("application/json")
                    .content("{\"version\":\""+VERSION+"\"}")).andExpect(status().isForbidden());
        }
        assertFalse(Files.exists(root.resolve("request.json")));
    }
    @Test void approvedVersionIsQueuedIdempotentlyWithoutAnyControllerWrites() throws Exception {
        String token = auth(Role.ROLE_ADMIN);
        clearInvocations(modbus);
        for (int i=0; i<2; i++) mvc.perform(post("/api/v1/panel-updates").header("Authorization", token)
                .contentType("application/json").content("{\"version\":\""+VERSION+"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("queued"));
        assertEquals(VERSION, json.readTree(Files.readAllBytes(root.resolve("request.json"))).path("tag").asText());
        verifyNoInteractions(modbus);
    }
    @Test void rejectsStaleWrongAndPathLikeVersions() throws Exception {
        String token = auth(Role.ROLE_ADMIN);
        mvc.perform(post("/api/v1/panel-updates").header("Authorization", token).contentType("application/json")
                .content("{\"version\":\"../../sauna.jar\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/panel-updates").header("Authorization", token).contentType("application/json")
                .content("{\"version\":\"large-production-18-1\"}")).andExpect(status().isConflict());
        state(1);
        mvc.perform(post("/api/v1/panel-updates").header("Authorization", token).contentType("application/json")
                .content("{\"version\":\""+VERSION+"\"}")).andExpect(status().isServiceUnavailable());
        assertFalse(Files.exists(root.resolve("request.json")));
    }
    @Test void productionPanelRejectsLabVersionEvenWhenOffered() throws Exception {
        String body = "{\"version\":\"large-lab-17-1\"}";
        mvc.perform(post("/api/v1/panel-updates").header("Authorization", auth(Role.ROLE_ADMIN))
                .contentType("application/json").content(body)).andExpect(status().isBadRequest());
        assertFalse(Files.exists(root.resolve("request.json")));
    }
    @Test void historyAndRestoreAreAdminOnlyAndIgnoreUnknownVersions() throws Exception {
        Files.write(root.resolve("history.json"),"{\"current\":\"large-production-17-1\",\"versions\":[{\"version\":\"large-production-16-1\",\"description\":\"previous\",\"restorable\":true,\"dbSha256\":\"private-checksum\"}]}".getBytes("UTF-8"));
        mvc.perform(get("/api/v1/panel-updates/history").header("Authorization",auth(Role.ROLE_READONLY))).andExpect(status().isForbidden());
        String admin=auth(Role.ROLE_ADMIN);
        mvc.perform(get("/api/v1/panel-updates/history").header("Authorization",admin)).andExpect(status().isOk()).andExpect(jsonPath("$.versions[0].description").value("previous")).andExpect(jsonPath("$.versions[0].dbSha256").doesNotExist());
        mvc.perform(post("/api/v1/panel-updates/restore").header("Authorization",auth(Role.ROLE_USER)).contentType("application/json").content("{\"version\":\"large-production-16-1\"}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/panel-updates/restore").header("Authorization",admin).contentType("application/json").content("{\"version\":\"../../other\"}")).andExpect(status().isConflict());
        mvc.perform(post("/api/v1/panel-updates/restore").header("Authorization",admin).contentType("application/json").content("{\"version\":\"large-production-16-1\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("queued"));
        assertTrue(json.readTree(Files.readAllBytes(root.resolve("request.json"))).path("restore").asBoolean());
    }

    @Test void absentUpdaterDoesNotOfferUpdates() throws Exception {
        Files.delete(root.resolve("ui-enabled"));
        mvc.perform(get("/api/v1/panel-updates").header("Authorization", auth(Role.ROLE_ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(false));
    }
}
