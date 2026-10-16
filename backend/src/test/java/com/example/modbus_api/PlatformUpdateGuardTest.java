package com.example.modbus_api;

import com.example.modbus_api.config.PlatformUpdateGuard;
import com.example.modbus_api.controller.PlatformStateController;
import com.example.modbus_api.service.PlatformStateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PlatformUpdateGuardTest {
    @Test void rejectsStaleMutationBeforeHandler() throws Exception {
        PlatformStateService service = mock(PlatformStateService.class);
        when(service.buildId()).thenReturn("current");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/devices/start");
        request.addHeader("X-Panel-Build", "previous");
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertFalse(new PlatformUpdateGuard(service).preHandle(request, response, new Object()));
        assertEquals(503, response.getStatus());
        assertEquals("required", response.getHeader("X-Panel-Update"));
    }
    @Test void blocksAllMutationsDuringInstallIncludingOlderClientsWithoutHeader() throws Exception {
        PlatformStateService service = mock(PlatformStateService.class);
        when(service.updating()).thenReturn(true);
        PlatformUpdateGuard guard = new PlatformUpdateGuard(service);
        for (String method : new String[]{"POST", "PUT", "PATCH", "DELETE"})
            assertFalse(guard.preHandle(new MockHttpServletRequest(method, "/api/v1/devices/start"), new MockHttpServletResponse(), new Object()));
        assertTrue(guard.preHandle(new MockHttpServletRequest("GET", "/api/v1/devices/dashboard"), new MockHttpServletResponse(), new Object()));
    }
    @Test void acceptsMatchingBuildAfterCompletionOrRollback() throws Exception {
        PlatformStateService service = mock(PlatformStateService.class);
        when(service.buildId()).thenReturn("current");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/devices/start");
        request.addHeader("X-Panel-Build", "current");
        assertTrue(new PlatformUpdateGuard(service).preHandle(request, new MockHttpServletResponse(), new Object()));
    }
    @Test void publicStateContainsOnlyBuildAndMaintenanceFlagAndCannotBeCached() throws Exception {
        PlatformStateService service = mock(PlatformStateService.class);
        when(service.state()).thenReturn(new ObjectMapper().createObjectNode().put("buildId", "current").put("updating", false));
        MockMvcBuilders.standaloneSetup(new PlatformStateController(service)).build().perform(get("/api/v1/platform-state"))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.buildId").value("current")).andExpect(jsonPath("$.updating").value(false));
    }
}
