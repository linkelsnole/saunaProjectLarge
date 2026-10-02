package com.example.modbus_api;
import com.example.modbus_api.security.model.*;
import com.example.modbus_api.security.repository.UserRepository;
import com.example.modbus_api.service.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.file.Files;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static com.example.modbus_api.service.ModbusConstants.SAUNA_PLC;

@SpringBootTest(properties={"spring.jpa.show-sql=false"})
@AutoConfigureMockMvc
class UserManagementIntegrationTest {
    @Autowired javax.sql.DataSource dataSource;
    @Autowired com.example.modbus_api.config.DatabaseInitializer initializer;
    @Autowired com.example.modbus_api.security.repository.RefreshTokenRepository refreshSessions;
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired UserRepository users; @Autowired PasswordEncoder encoder;
    @MockBean ModbusClientAdapter modbus; @MockBean ReadingsLogService readings;
    String actor,token;AppUser admin;
    static final String PASSWORD="synthetic-test-only-password";
    @DynamicPropertySource static void db(DynamicPropertyRegistry r) throws Exception {
        String url="jdbc:sqlite:"+Files.createTempDirectory("sauna-admin-test-").resolve("users.db");r.add("spring.datasource.url",()->url);
    }
    AppUser account(Role role) {return users.saveAndFlush(AppUser.builder().username("test_"+UUID.randomUUID().toString().replace("-","").substring(0,20)).password(encoder.encode(PASSWORD)).role(role).build());}
    String login(AppUser user) throws Exception {
        String result=mvc.perform(post("/api/v1/auth/login").contentType("application/json").content(json.writeValueAsString(new HashMap<String,String>(){{put("username",user.getUsername());put("password",PASSWORD);}}))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(result).get("accessToken").asText();
    }
    @BeforeEach void before() throws Exception {admin=account(Role.ROLE_ADMIN);actor=admin.getUsername();token=login(admin);}
    String auth(){return "Bearer "+token;}
    String createBody(String username,String password,String role) throws Exception {Map<String,String> m=new HashMap<>();m.put("username",username);m.put("password",password);m.put("role",role);return json.writeValueAsString(m);}
    @Test void deleteRevokesSessionsAndOldTokensEvenWhenLoginIsRecreated() throws Exception {
        AppUser target=account(Role.ROLE_ADMIN);
        String oldToken=login(target);
        String name=target.getUsername();long id=target.getId();
        mvc.perform(delete("/api/v1/admin/users/"+id).header("Authorization",auth())).andExpect(status().isNoContent());
        assertFalse(users.findById(id).isPresent());
        assertTrue(refreshSessions.findAll().stream().noneMatch(s->s.getUser().getId().equals(id)));
        mvc.perform(get("/api/v1/admin/users").header("Authorization","Bearer "+oldToken)).andExpect(status().isUnauthorized());
        AppUser recreated=users.saveAndFlush(AppUser.builder().username(name).password(encoder.encode(PASSWORD)).role(Role.ROLE_ADMIN).build());
        mvc.perform(get("/api/v1/admin/users").header("Authorization","Bearer "+oldToken)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/users").header("Authorization","Bearer "+login(recreated))).andExpect(status().isOk());
    }
    @Test void deletionRequiresAdminAndCannotDeleteSelf() throws Exception {
        AppUser reader=account(Role.ROLE_READONLY);
        mvc.perform(delete("/api/v1/admin/users/"+admin.getId()).header("Authorization","Bearer "+login(reader))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/admin/users/"+admin.getId()).header("Authorization",auth())).andExpect(status().isBadRequest());
        assertTrue(users.findById(admin.getId()).isPresent());
    }
    @Test void deletedBuiltinAccountDoesNotReturnAtStartup() throws Exception {
        String login=users.findByUsername("READER").isPresent()?"READER":"readonly";
        AppUser target=users.findByUsername(login).get();
        mvc.perform(delete("/api/v1/admin/users/"+target.getId()).header("Authorization",auth())).andExpect(status().isNoContent());
        initializer.run();
        assertFalse(users.findByUsername(login).isPresent());
    }

    @Test void adminCreatesTesterAndReaderWithHashedPasswordAndNoSecretsInResponse() throws Exception {
        for(String role:new String[]{"ROLE_TESTER","ROLE_READONLY"}) {
            String name="new_"+UUID.randomUUID().toString().substring(0,12);
            String response=mvc.perform(post("/api/v1/admin/users").header("Authorization",auth()).contentType("application/json").content(createBody(name,PASSWORD,role))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
            assertEquals(role,json.readTree(response).get("role").asText());assertFalse(response.contains("password"));
            AppUser saved=users.findByUsername(name).get();assertNotEquals(PASSWORD,saved.getPassword());assertTrue(encoder.matches(PASSWORD,saved.getPassword()));
            String newToken=login(saved);
            mvc.perform(get("/api/v1/devices/physical-schedules").header("Authorization","Bearer "+newToken)).andExpect(status().isForbidden());
        }
        String list=mvc.perform(get("/api/v1/admin/users").header("Authorization",auth())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        for(JsonNode user:json.readTree(list)){assertEquals(3,user.size());assertFalse(user.has("password"));}
    }
    @Test void physicalSchedulesDenyAllNonAdminsAndAnonymousWithoutControllerAccess() throws Exception {
        String url="/api/v1/devices/physical-schedules";
        String body="{\"changes\":[{\"offset\":8,\"expected\":0,\"value\":42}],\"apply\":true}";
        for(Role role:Role.values()) {
            if(role==Role.ROLE_ADMIN) continue;
            String header="Bearer "+login(account(role));
            clearInvocations(modbus);
            mvc.perform(get(url).header("Authorization",header)).andExpect(status().isForbidden());
            mvc.perform(patch(url+"/7").header("Authorization",header).contentType("application/json").content(body)).andExpect(status().isForbidden());
            mvc.perform(post(url+"/apply").header("Authorization",header)).andExpect(status().isForbidden());
            verifyNoInteractions(modbus);
        }
        clearInvocations(modbus);
        mvc.perform(get(url)).andExpect(status().isUnauthorized());
        mvc.perform(patch(url+"/7").contentType("application/json").content(body)).andExpect(status().isUnauthorized());
        mvc.perform(post(url+"/apply")).andExpect(status().isUnauthorized());
        verifyNoInteractions(modbus);
    }
    @Test void testingEndpointsOnlyAllowTesterAndNeverTouchControllerForOtherRoles() throws Exception {
        String url = "/api/v1/testing";
        for (Role role : Role.values()) {
            if (role == Role.ROLE_TESTER) continue;
            String header = "Bearer " + login(account(role));
            clearInvocations(modbus);
            mvc.perform(get(url + "/status").header("Authorization", header)).andExpect(status().isForbidden());
            mvc.perform(post(url + "/mode").header("Authorization", header).contentType("application/json").content("{\"enabled\":true}")).andExpect(status().isForbidden());
            mvc.perform(post(url + "/commands").header("Authorization", header).contentType("application/json").content("{\"command\":2}")).andExpect(status().isForbidden());
            verifyNoInteractions(modbus);
        }
        String header = "Bearer " + login(account(Role.ROLE_TESTER));
        when(modbus.readRegister(SAUNA_PLC, 0x0302)).thenReturn(8);
        mvc.perform(get(url + "/status").header("Authorization", header)).andExpect(status().isOk());
        mvc.perform(post(url + "/commands").header("Authorization", header).contentType("application/json").content("{\"command\":2}")).andExpect(status().isOk());
        verify(modbus).writeCommandOnce(SAUNA_PLC, 0x0307, 2);
        mvc.perform(post(url + "/commands").header("Authorization", header).contentType("application/json").content("{\"command\":15,\"index\":0}")).andExpect(status().isBadRequest());
        mvc.perform(get(url + "/status")).andExpect(status().isUnauthorized());
    }

    @Test void adminCanReadPhysicalMapAndApplySchedules() throws Exception {
        when(modbus.readRegisters(eq(SAUNA_PLC),anyList())).thenReturn(Collections.emptyMap());
        mvc.perform(get("/api/v1/devices/physical-schedules").header("Authorization",auth())).andExpect(status().isOk());
        verify(modbus).readRegisters(eq(SAUNA_PLC),anyList());
        mvc.perform(post("/api/v1/devices/physical-schedules/apply").header("Authorization",auth())).andExpect(status().isNoContent());
        verify(modbus).writeRegister(SAUNA_PLC,ModbusRegisterMap.AGGREGATE_COMMAND,6);
    }
    @Test void nonAdminsCannotListCreateOrChangeRoles() throws Exception {
        for(Role role:Role.values()) {if(role==Role.ROLE_ADMIN)continue;AppUser user=account(role);String header="Bearer "+login(user);
            mvc.perform(get("/api/v1/admin/users").header("Authorization",header)).andExpect(status().isForbidden());
            mvc.perform(post("/api/v1/admin/users").header("Authorization",header).contentType("application/json").content(createBody("blocked_user",PASSWORD,"ROLE_ADMIN"))).andExpect(status().isForbidden());
            mvc.perform(patch("/api/v1/admin/users/"+user.getId()+"/role").header("Authorization",header).contentType("application/json").content("{\"role\":\"ROLE_ADMIN\",\"expectedRole\":\""+role+"\"}")).andExpect(status().isForbidden());
            assertEquals(role,users.findById(user.getId()).get().getRole());
        }
    }
    @Test void demotionTakesEffectForAlreadyIssuedAccessToken() throws Exception {
        AppUser second=account(Role.ROLE_ADMIN);String oldToken=login(second);
        mvc.perform(patch("/api/v1/admin/users/"+second.getId()+"/role").header("Authorization",auth()).contentType("application/json").content("{\"role\":\"ROLE_READONLY\",\"expectedRole\":\"ROLE_ADMIN\"}")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/users").header("Authorization","Bearer "+oldToken)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/devices/physical-schedules").header("Authorization","Bearer "+oldToken)).andExpect(status().isForbidden());
    }
    @Test void rejectsSelfDemotionConcurrentRoleEditAndUnknownUser() throws Exception {
        mvc.perform(patch("/api/v1/admin/users/"+admin.getId()+"/role").header("Authorization",auth()).contentType("application/json").content("{\"role\":\"ROLE_USER\",\"expectedRole\":\"ROLE_ADMIN\"}")).andExpect(status().isBadRequest());
        AppUser reader=account(Role.ROLE_READONLY);
        mvc.perform(patch("/api/v1/admin/users/"+reader.getId()+"/role").header("Authorization",auth()).contentType("application/json").content("{\"role\":\"ROLE_ADMIN\",\"expectedRole\":\"ROLE_USER\"}")).andExpect(status().isConflict());
        mvc.perform(patch("/api/v1/admin/users/99999999/role").header("Authorization",auth()).contentType("application/json").content("{\"role\":\"ROLE_USER\",\"expectedRole\":\"ROLE_ADMIN\"}")).andExpect(status().isNotFound());
        assertEquals(Role.ROLE_ADMIN,users.findById(admin.getId()).get().getRole());
    }
    @Test void rejectsDuplicateInvalidRoleAndInvalidPassword() throws Exception {
        mvc.perform(post("/api/v1/admin/users").header("Authorization",auth()).contentType("application/json").content(createBody(actor.toUpperCase(Locale.ROOT),PASSWORD,"ROLE_USER"))).andExpect(status().isConflict());
        for(String body:Arrays.asList(createBody("bad name",PASSWORD,"ROLE_USER"),createBody("valid_name","short","ROLE_USER"),createBody("valid_name",PASSWORD,"ROLE_ROOT"),createBody("valid_name","яяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяяя","ROLE_USER")))
            mvc.perform(post("/api/v1/admin/users").header("Authorization",auth()).contentType("application/json").content(body)).andExpect(status().isBadRequest());
    }
    @Test void anonymousRequestsCannotManageUsers() throws Exception {
        mvc.perform(get("/api/v1/admin/users")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/admin/users").contentType("application/json").content(createBody("anonymous",PASSWORD,"ROLE_ADMIN"))).andExpect(status().isUnauthorized());
    }
    @Test void ordinaryRegistrationCannotGrantAdminByNameOrPayload() throws Exception {
        String name="AdMiN";
        users.findByUsername(name).ifPresent(u->users.delete(u));
        mvc.perform(post("/api/v1/auth/register").contentType("application/json").content(createBody(name,PASSWORD,"ROLE_ADMIN"))).andExpect(status().isOk());
        assertEquals(Role.ROLE_USER,users.findByUsername(name).get().getRole());
    }
    @Test void scheduleEndpointsUsePhysicalGroupsAndRejectStaleMembership() throws Exception {
        Map<Integer,Integer> map=new HashMap<>();
        for(int id=1;id<=20;id++){int base=0x2000+(id-1)*32;map.put(base,0);map.put(base+8,0);}
        map.put(0x20C0,4);map.put(0x20C8,42);map.put(0x20C1,2);
        map.put(0x2240,3);map.put(0x2248,42);map.put(0x2241,0);
        when(modbus.readRegisters(eq(SAUNA_PLC),anyList())).thenReturn(map);
        mvc.perform(patch("/api/v1/devices/schedules/group/42/active").header("Authorization",auth())
            .contentType("application/json").content("{\"isActive\":true}")).andExpect(status().isOk());
        verify(modbus).writeRegister(SAUNA_PLC,0x20C1,3);
        verify(modbus).writeRegister(SAUNA_PLC,0x2241,1);
        clearInvocations(modbus);
        mvc.perform(put("/api/v1/devices/schedules/group/42").header("Authorization",auth()).contentType("application/json")
            .content("{\"startDate\":257,\"endDate\":3103,\"weekDays\":[\"mon\"],\"active\":true,\"intervals\":[{\"scheduleId\":7,\"startTime\":2048,\"endTime\":4608,\"airControlMode\":1,\"zone1ControlMode\":0,\"zone2ControlMode\":0}]}")).andExpect(status().isBadRequest());
        verify(modbus,never()).writeRegister(anyString(),anyInt(),anyInt());
    }

    @Test void dashboardUsesTenthsForCurrentAndSetValvePercentagesInBothZones() throws Exception {
        Map<Integer,Integer> map=new HashMap<>();
        // Literal addresses protect against a mistaken mapping change.
        map.put(0x0907,0);map.put(0x0908,519);map.put(0x0947,125);map.put(0x0948,1000);
        map.put(0x0915,0);map.put(0x0916,518);map.put(0x0917,15);map.put(0x0955,120);map.put(0x0956,999);map.put(0x0957,10);
        when(modbus.readRegisters(eq(SAUNA_PLC),anyList())).thenReturn(map);
        String body=mvc.perform(get("/api/v1/devices/dashboard").header("Authorization",auth())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode data=json.readTree(body);assertEquals("0.0",data.at("/valves/0/value").asText());assertEquals("51.9",data.at("/valves/1/value").asText());assertEquals("12.5",data.at("/valves/2/value").asText());assertEquals("100.0",data.at("/valves/3/value").asText());assertEquals("51.8",data.at("/debugValveSensors/1/value").asText());assertEquals("99.9",data.at("/debugValveSensors/4/value").asText());
        verify(modbus,never()).writeRegister(anyString(),anyInt(),anyInt());
    }
    @Test void screenTemperatureEndpointForcesAutoInAffectedUserSetZone() throws Exception {
        when(modbus.readRegisters(eq(SAUNA_PLC),anyList())).thenReturn(Collections.singletonMap(0x20C0,5));
        when(modbus.readRegister(SAUNA_PLC,0x20C1)).thenReturn(2);
        when(modbus.readRegister(SAUNA_PLC,ModbusRegisterMap.AGGREGATE_STATE)).thenReturn(64);
        mvc.perform(post("/api/v1/devices/temperature-control").header("Authorization",auth()).contentType("application/json").content("{\"zone1Mode\":1}")).andExpect(status().isOk());
        org.mockito.InOrder order=inOrder(modbus);
        order.verify(modbus).writeRegister(SAUNA_PLC,0x20CC,2);
        order.verify(modbus).writeRegister(SAUNA_PLC,0x20CD,1);
        verify(modbus,never()).writeRegister(eq(SAUNA_PLC),eq(0x20CF),anyInt());
        verify(modbus).writeRegister(SAUNA_PLC,0x20CD,1);
    }

    @Test void temperatureTargetEndpointEnablesAutoBeforeZoneTwoTarget() throws Exception {
        when(modbus.readRegisters(eq(SAUNA_PLC),anyList())).thenReturn(Collections.singletonMap(0x20C0,5));
        mvc.perform(post("/api/v1/devices/temperature-setpoints").header("Authorization",auth())
            .contentType("application/json").content("{\"inflowTargetZone2\":22.5}"))
            .andExpect(status().isOk());
        org.mockito.InOrder order=inOrder(modbus);
        order.verify(modbus).writeRegister(SAUNA_PLC,0x20CF,2);
        order.verify(modbus).writeRegister(SAUNA_PLC,ModbusRegisterMap.HEATER_ZONE_2+25,225);
        order.verify(modbus).writeRegister(SAUNA_PLC,ModbusRegisterMap.AGGREGATE_COMMAND,6);
        verify(modbus,never()).writeRegister(eq(SAUNA_PLC),eq(0x20CC),anyInt());
    }

    @Test void physicalPatchKeepsManualModesAndWorkingAndConfirmsReadback() throws Exception {
        Map<Integer,Integer> registers=new HashMap<>();for(int offset=0;offset<18;offset++) registers.put(0x20C0+offset,0);registers.put(0x20C1,2);
        when(modbus.readRegisters(eq(SAUNA_PLC),anyList())).thenAnswer(a->new HashMap<>(registers));
        when(modbus.readRegister(SAUNA_PLC,0x20C1)).thenAnswer(a->registers.get(0x20C1));
        doAnswer(a->{registers.put(a.getArgument(1),a.getArgument(2));return null;}).when(modbus).writeRegister(eq(SAUNA_PLC),anyInt(),anyInt());
        String body=mvc.perform(patch("/api/v1/devices/physical-schedules/7").header("Authorization",auth()).contentType("application/json")
            .content("{\"changes\":[{\"offset\":8,\"expected\":0,\"value\":42},{\"offset\":12,\"expected\":0,\"value\":3},{\"offset\":15,\"expected\":0,\"value\":1},{\"offset\":1,\"expected\":0,\"value\":1}],\"apply\":true}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(json.readTree(body).get("verified").asBoolean());
        assertEquals(Integer.valueOf(3),registers.get(0x20C1));assertEquals(Integer.valueOf(3),registers.get(0x20CC));assertEquals(Integer.valueOf(1),registers.get(0x20CF));assertEquals(Integer.valueOf(42),registers.get(0x20C8));
        verify(modbus).writeRegister(SAUNA_PLC,ModbusRegisterMap.AGGREGATE_COMMAND,6);
    }

    @Test void eventApiKeepsEveryDocumentedLevelAndUnknownFields() throws Exception {
        java.util.ArrayList<com.example.modbus_api.dto.SystemEventDto> events=new java.util.ArrayList<>();
        for(int level=0;level<=6;level++) {com.example.modbus_api.dto.SystemEventDto e=new com.example.modbus_api.dto.SystemEventDto();e.setLevel(level);e.setType(16);e.setValue(0.0);e.setOid("900719925474099"+level);events.add(e);}
        when(modbus.getEvents(60)).thenReturn(events);
        String body=mvc.perform(get("/api/v1/devices/events").header("Authorization",auth())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode result=json.readTree(body);assertEquals(7,result.size());assertEquals(5,result.get(5).get("level").asInt());assertTrue(result.get(5).get("arrivalTime").isNull());assertTrue(result.get(5).get("regNum").isNull());assertEquals("9007199254740995",result.get(5).get("oid").asText());
    }
    @Test void monthlyAndQuarterlyHistoryWindowsReachDataServerUnchanged() throws Exception {
        for(int minutes:new int[]{43200,129600}) {
            when(modbus.getEvents(minutes)).thenReturn(new ArrayList<com.example.modbus_api.dto.SystemEventDto>());
            mvc.perform(get("/api/v1/devices/events").param("minutes",String.valueOf(minutes)).header("Authorization",auth())).andExpect(status().isOk());
            verify(modbus).getEvents(minutes);
        }
    }
    @Test void actionHistoryIncludesOlderEntriesOnlyWithinSelectedWindow() throws Exception {
        org.springframework.jdbc.core.JdbcTemplate jdbc=new org.springframework.jdbc.core.JdbcTemplate(dataSource);
        String marker="period_"+UUID.randomUUID();
        for(int days:new int[]{20,60,100}) jdbc.update("INSERT INTO user_action_log (ts,username,action_type,action,success) VALUES (?,?,?,?,?)",System.currentTimeMillis()-days*86400000L,marker,"OTHER",marker+"_"+days,1);
        for(int days:new int[]{30,90}) {
            String body=mvc.perform(get("/api/v1/logs/actions").param("minutes",String.valueOf(days*1440)).header("Authorization",auth())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertTrue(body.contains(marker+"_20"));assertEquals(days==90,body.contains(marker+"_60"));assertFalse(body.contains(marker+"_100"));
        }
    }
    @Test void invalidHistoryWindowDoesNotReachController() throws Exception {
        clearInvocations(modbus);
        for(int minutes:new int[]{-1,0,129601,Integer.MAX_VALUE})mvc.perform(get("/api/v1/devices/events").param("minutes",String.valueOf(minutes)).header("Authorization",auth())).andExpect(status().isBadRequest());
        verify(modbus,never()).getEvents(anyInt());
    }
    @Test void journalAccessIsAdminOnlyIncludingTechnicalLogs() throws Exception {
        for(Role role:Role.values()){if(role==Role.ROLE_ADMIN)continue;String header="Bearer "+login(account(role));
            mvc.perform(get("/api/v1/devices/events").header("Authorization",header)).andExpect(status().isForbidden());
            mvc.perform(get("/api/v1/devices/events/status").header("Authorization",header)).andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/v1/devices/events")).andExpect(status().isUnauthorized());
        verify(modbus,never()).getEvents(anyInt());
    }
    @Test void unavailableJournalReturns502WithoutLeakingInternalErrorDetails() throws Exception {
        when(modbus.getEvents(60)).thenThrow(new java.io.IOException("private-upstream-detail"));
        String body=mvc.perform(get("/api/v1/devices/events").header("Authorization",auth())).andExpect(status().isBadGateway()).andReturn().getResponse().getContentAsString();
        assertFalse(body.contains("private-upstream-detail"));
    }
    @Test void currentDiagnosticsUsesRegistersAndDoesNotInferStateFromHistory() throws Exception {
        Map<Integer,Integer> words=new HashMap<>();words.put(0x0301,0);words.put(0x0305,0x8000);words.put(0x0600,7);words.put(0x0604,0);
        when(modbus.readRegisters(eq(SAUNA_PLC),anyList())).thenReturn(words);
        String body=mvc.perform(get("/api/v1/devices/events/status").header("Authorization",auth())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode result=json.readTree(body);assertEquals(32768,result.get("workState").asInt());assertEquals(7,result.get("modbusErrors").asInt());assertTrue(result.get("failureRestarts").isNull());
        verify(modbus,never()).getEvents(anyInt());verify(modbus,never()).writeRegister(anyString(),anyInt(),anyInt());
    }
    @Test void passwordChangeRevokesSessionsAndOldPasswordAndSurvivesInitialization() throws Exception {
        AppUser target=account(Role.ROLE_ADMIN);String oldToken=login(target);
        String next="new-synthetic-password-42";
        mvc.perform(patch("/api/v1/admin/users/"+target.getId()+"/password").header("Authorization",auth())
            .contentType("application/json").content(json.writeValueAsString(Collections.singletonMap("password",next))))
            .andExpect(status().isNoContent());
        assertTrue(encoder.matches(next,users.findById(target.getId()).get().getPassword()));
        assertTrue(refreshSessions.findAll().stream().noneMatch(s->s.getUser().getId().equals(target.getId())));
        mvc.perform(get("/api/v1/admin/users").header("Authorization","Bearer "+oldToken)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").contentType("application/json").content(json.writeValueAsString(new HashMap<String,String>(){{put("username",target.getUsername());put("password",PASSWORD);}}))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/login").contentType("application/json").content(json.writeValueAsString(new HashMap<String,String>(){{put("username",target.getUsername());put("password",next);}}))).andExpect(status().isOk());
        initializer.run();assertTrue(encoder.matches(next,users.findById(target.getId()).get().getPassword()));
    }
    @Test void passwordChangeRequiresAdminAndValidPassword() throws Exception {
        AppUser reader=account(Role.ROLE_READONLY);String path="/api/v1/admin/users/"+reader.getId()+"/password";
        mvc.perform(patch(path).header("Authorization","Bearer "+login(reader)).contentType("application/json").content("{\"password\":\"long-enough-test-password\"}")).andExpect(status().isForbidden());
        for(String invalid:Arrays.asList("short",String.join("",Collections.nCopies(37,"я"))))
            mvc.perform(patch(path).header("Authorization",auth()).contentType("application/json").content(json.writeValueAsString(Collections.singletonMap("password",invalid)))).andExpect(status().isBadRequest());
        assertTrue(encoder.matches(PASSWORD,users.findById(reader.getId()).get().getPassword()));
    }
    @Test void administratorCanChangeOwnPasswordAndMustSignInAgain() throws Exception {
        mvc.perform(patch("/api/v1/admin/users/"+admin.getId()+"/password").header("Authorization",auth()).contentType("application/json").content("{\"password\":\"new-own-password-42\"}")).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/admin/users").header("Authorization",auth())).andExpect(status().isUnauthorized());
    }
    @Test void builtinReaderPasswordIsNotResetOnRestart() throws Exception {
        AppUser target=users.findByUsername("READER").orElseGet(()->users.findByUsername("readonly").get());
        mvc.perform(patch("/api/v1/admin/users/"+target.getId()+"/password").header("Authorization",auth()).contentType("application/json").content("{\"password\":\"new-reader-password-42\"}")).andExpect(status().isNoContent());
        initializer.run();assertTrue(encoder.matches("new-reader-password-42",users.findById(target.getId()).get().getPassword()));
    }
    @Test void sixCharacterPasswordsAreAcceptedAndHashedForCreationAndChanges() throws Exception {
        String name = "six_" + UUID.randomUUID().toString().substring(0, 12);
        mvc.perform(post("/api/v1/admin/users").header("Authorization", auth()).contentType("application/json")
            .content(createBody(name, "plain6", "ROLE_TESTER"))).andExpect(status().isCreated());
        AppUser created = users.findByUsername(name).get();
        assertNotEquals("plain6", created.getPassword());
        assertTrue(encoder.matches("plain6", created.getPassword()));
        mvc.perform(post("/api/v1/auth/login").contentType("application/json")
            .content("{\"username\":\"" + name + "\",\"password\":\"plain6\"}")).andExpect(status().isOk());
        mvc.perform(patch("/api/v1/admin/users/" + created.getId() + "/password").header("Authorization", auth()).contentType("application/json")
            .content("{\"password\":\"other6\"}")).andExpect(status().isNoContent());
        assertTrue(encoder.matches("other6", users.findById(created.getId()).get().getPassword()));
        mvc.perform(post("/api/v1/admin/users").header("Authorization", auth()).contentType("application/json")
            .content(createBody(name + "_short", "abcde", "ROLE_TESTER"))).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/admin/users/" + created.getId() + "/password").header("Authorization", auth()).contentType("application/json")
            .content("{\"password\":\"abcde\"}")).andExpect(status().isBadRequest());
    }
    @Test void testerCanReadRegularDashboardAndSchedulesButNotAdminTools() throws Exception {
        String header = "Bearer " + login(account(Role.ROLE_TESTER));
        when(modbus.readRegisters(eq(SAUNA_PLC), anyList())).thenReturn(Collections.emptyMap());
        mvc.perform(get("/api/v1/devices/dashboard").header("Authorization", header)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/devices/schedules").header("Authorization", header)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/users").header("Authorization", header)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/devices/physical-schedules").header("Authorization", header)).andExpect(status().isForbidden());
    }
}
