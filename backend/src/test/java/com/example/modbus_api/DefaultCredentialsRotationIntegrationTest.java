package com.example.modbus_api;

import com.example.modbus_api.config.DatabaseInitializer;
import com.example.modbus_api.config.JwtProperties;
import com.example.modbus_api.security.model.*;
import com.example.modbus_api.security.repository.UserRepository;
import com.example.modbus_api.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import javax.servlet.http.Cookie;
import java.nio.file.Files;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties={"spring.jpa.show-sql=false", "sauna.credentials.password-hash=$2a$10$f88S2Jl.DeSwA1qG8ey93uhNynCyCzN1smZpLbHJnQKzM9iG8v3iy"})
@AutoConfigureMockMvc
class DefaultCredentialsRotationIntegrationTest {
    static final String PASSWORD="Test26";
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users; @Autowired PasswordEncoder encoder; @Autowired JwtProperties jwt;
    @Autowired DefaultCredentialsRotation rotation; @Autowired DatabaseInitializer initializer;
    @MockBean ModbusClientAdapter modbus; @MockBean ReadingsLogService readings;
    @DynamicPropertySource static void db(DynamicPropertyRegistry r) throws Exception {
        String url="jdbc:sqlite:"+Files.createTempDirectory("sauna-password-test-").resolve("users.db");r.add("spring.datasource.url",()->url);
    }
    @BeforeEach void legacyDatabase() {
        jdbc.update("DELETE FROM web_credential_migrations WHERE version=?",DefaultCredentialsRotation.VERSION);
        for(String username:new String[]{"admin","user"}) {
            AppUser user=users.findByUsername(username).get();user.setPassword(encoder.encode(username.equals("admin")?"RA285Q7n":"RU804f*"));users.saveAndFlush(user);
        }
    }
    MvcResult login(String username,String password,int expected) throws Exception {
        Map<String,String> body=new HashMap<>();body.put("username",username);body.put("password",password);
        return mvc.perform(post("/api/v1/auth/login").contentType("application/json").content(json.writeValueAsString(body))).andExpect(status().is(expected)).andReturn();
    }
    Cookie cookie(MvcResult result) {String value=result.getResponse().getHeader("Set-Cookie").split(";",2)[0].split("=",2)[1];return new Cookie(jwt.getRefreshCookieName(),value);}
    String legacyAccess(String username) {
        Date now=new Date();return Jwts.builder().subject(username).issuedAt(now).expiration(new Date(now.getTime()+600000)).signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwt.getSecret())),Jwts.SIG.HS256).compact();
    }
    @Test void rotationRejectsOldPasswordsAndSessionsButAcceptsNewLogins() throws Exception {
        Cookie oldSession=cookie(login("admin","RA285Q7n",200));String oldAccess=legacyAccess("admin");
        rotation.applyOnce();
        login("admin","RA285Q7n",401);login("user","RU804f*",401);
        mvc.perform(post("/api/v1/auth/refresh").cookie(oldSession)).andExpect(status().isUnauthorized());
        for(String username:new String[]{"admin","user"}) {
            mvc.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+(username.equals("admin")?oldAccess:legacyAccess(username)))).andExpect(status().isUnauthorized());
            MvcResult result=login(username,PASSWORD,200);
            mvc.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+json.readTree(result.getResponse().getContentAsString()).get("accessToken").asText())).andExpect(status().isOk());
            mvc.perform(post("/api/v1/auth/refresh").cookie(cookie(result))).andExpect(status().isOk());
        }
    }
    @Test void restartDoesNotResetLaterPasswordOrRevokeNewSession() throws Exception {
        rotation.applyOnce();AppUser user=users.findByUsername("admin").get();String later="changed-again-test-password";
        user.setPassword(encoder.encode(later));users.saveAndFlush(user);Cookie session=cookie(login("admin",later,200));String hash=users.findByUsername("admin").get().getPassword();
        initializer.run();
        assertEquals(hash,users.findByUsername("admin").get().getPassword());login("admin",later,200);login("admin",PASSWORD,401);
        mvc.perform(post("/api/v1/auth/refresh").cookie(session)).andExpect(status().isOk());
        assertEquals(Integer.valueOf(1),jdbc.queryForObject("SELECT COUNT(*) FROM web_credential_migrations WHERE version=?",Integer.class,DefaultCredentialsRotation.VERSION));
    }
    @Test void reupgradeRepairsPasswordsRestoredByOldJarWithoutRepeatingMigration() throws Exception {
        rotation.applyOnce();
        for(String username:new String[]{"admin","user"}) {
            AppUser user=users.findByUsername(username).get();user.setPassword(encoder.encode(username.equals("admin")?"RA285Q7n":"RU804f*"));users.saveAndFlush(user);
        }
        Cookie oldSession=cookie(login("admin","RA285Q7n",200));
        initializer.run();
        login("admin","RA285Q7n",401);login("user","RU804f*",401);login("admin",PASSWORD,200);login("user",PASSWORD,200);
        mvc.perform(post("/api/v1/auth/refresh").cookie(oldSession)).andExpect(status().isUnauthorized());
        assertEquals(Integer.valueOf(1),jdbc.queryForObject("SELECT COUNT(*) FROM web_credential_migrations WHERE version=?",Integer.class,DefaultCredentialsRotation.VERSION));
    }
    @Test void previousReleaseUpgradeAndRollbackBothUseNewSixCharacterPassword() throws Exception {
        String previousVersion="2026-09-26-admin-user-v1";
        jdbc.update("INSERT OR IGNORE INTO web_credential_migrations (version,applied_at) VALUES (?,?)",previousVersion,System.currentTimeMillis());
        String oldAccess=Jwts.builder().subject("admin").claim("credentialsVersion",previousVersion).issuedAt(new Date()).expiration(new Date(System.currentTimeMillis()+600000)).signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwt.getSecret())),Jwts.SIG.HS256).compact();
        for(int attempt=0;attempt<2;attempt++) {
            for(String username:new String[]{"admin","user"}) {
                AppUser user=users.findByUsername(username).get();user.setPassword(DefaultCredentialsRotation.PREVIOUS_PASSWORD_HASH);users.saveAndFlush(user);
            }
            initializer.run();
            for(String username:new String[]{"admin","user"}) login(username,PASSWORD,200);
            mvc.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+oldAccess)).andExpect(status().isUnauthorized());
        }
        assertEquals(6,PASSWORD.length());
    }
    @Test void rotationLeavesOtherAccountsPasswordsRolesAndSessionsUntouched() throws Exception {
        AppUser reader=users.saveAndFlush(AppUser.builder().username("reader_migration_test").password(encoder.encode("reader-test-password")).role(Role.ROLE_READONLY).build());
        Cookie session=cookie(login(reader.getUsername(),"reader-test-password",200));String access=legacyAccess(reader.getUsername());
        rotation.applyOnce();AppUser after=users.findById(reader.getId()).get();assertEquals(reader.getPassword(),after.getPassword());assertEquals(Role.ROLE_READONLY,after.getRole());
        mvc.perform(post("/api/v1/auth/refresh").cookie(session)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+access)).andExpect(status().isOk());
    }
}
