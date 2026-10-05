package com.example.modbus_api.service;
import com.example.modbus_api.controller.DeviceController;
import com.example.modbus_api.dto.*;
import com.example.modbus_api.logging.ActionType;
import java.io.IOException;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.example.modbus_api.service.ModbusConstants.*;

class SettingChangesTest {
    final ModbusClientAdapter client = mock(ModbusClientAdapter.class);
    final DeviceService device = mock(DeviceService.class);
    final SettingsService settings = mock(SettingsService.class);
    final UserActionLogService logs = mock(UserActionLogService.class);
    DeviceController controller() {
        when(device.getModbusClient()).thenReturn(client);
        return new DeviceController(device, settings, mock(ScheduleService.class), mock(UpgradeService.class), logs);
    }
    @Test void temperatureSnapshotPrecedesWriteAndIncludesEveryRequestedField() throws Exception {
        Map<Integer,Integer> before = new HashMap<>();
        before.put(ModbusRegisterMap.HEATER_ZONE_1 + ModbusRegisterMap.INFLOW_TARGET_OFFSET, 600);
        before.put(ModbusRegisterMap.HEATER_ZONE_2 + ModbusRegisterMap.ORP_CURRENT_T3_OFFSET, 65531);
        when(client.readRegisters(eq(SAUNA_PLC), anyList())).thenReturn(before);
        UpdateTemperatureSetpointsRequest request = new UpdateTemperatureSetpointsRequest();
        request.setInflowTargetZone1(65.0);
        OrpSettingsDto orp = new OrpSettingsDto(); orp.setCurrentT3(-4.0); request.setOrpZone2(orp);
        assertEquals(200, controller().updateTemperatureSetpoints(request).getStatusCodeValue());
        org.mockito.InOrder order = inOrder(client, settings, logs);
        order.verify(client).readRegisters(eq(SAUNA_PLC), anyList());
        order.verify(settings).updateTemperatureSetpoints(request);
        order.verify(logs).log(ActionType.SETPOINT, "Запись температурных уставок зоны", true,
            "ПВ, зона 1: 60 → 65 °C; ОРП, зона 2, T3 текущая: -5 → -4 °C");
    }
    @Test void unknownOldValueDoesNotBlockWriteOrInventZero() throws Exception {
        when(client.readRegisters(eq(SAUNA_PLC), anyList())).thenThrow(new IOException("offline"));
        UpdateVolumeSettingsRequest request = new UpdateVolumeSettingsRequest(); request.setEcoAirflow(3500.0);
        assertEquals(200, controller().updateVolumeSettings(request).getStatusCodeValue());
        verify(settings).updateVolumeSettings(request);
        verify(logs).log(ActionType.SETPOINT, "Запись настроек объёма", true, "ЭКО: неизвестно → 3500 м³/ч");
    }
    @Test void failedWriteNeverProducesSuccess() throws Exception {
        UpdateTemperatureSetpointsRequest request = new UpdateTemperatureSetpointsRequest(); request.setOutflowTarget(40.0);
        doThrow(new IOException("write failed")).when(settings).updateTemperatureSetpoints(request);
        assertEquals(500, controller().updateTemperatureSetpoints(request).getStatusCodeValue());
        verify(logs, never()).log(any(), anyString(), eq(true), any());
    }
    @Test void floatWordsSignedNumbersMissingAndUnchangedValues() throws Exception {
        Map<Integer,Integer> before = new HashMap<>();
        before.put(100, ModbusRegisterMap.floatLowWord(3500f)); before.put(101, ModbusRegisterMap.floatHighWord(3500f));
        before.put(200, 65436); before.put(201, INVALID_VALUE);
        when(client.readRegisters(eq(SAUNA_PLC), anyList())).thenReturn(before);
        SettingChanges changes = new SettingChanges(client);
        changes.floating(100, "ЭКО", "м³/ч", 3500);
        changes.number(200, 10, "Наддув", "%", -5);
        changes.number(201, 10, "ПВ", "°C", 60);
        changes.number(202, 10, "ОВ", "°C", null);
        assertEquals("ЭКО: 3500 → 3500 м³/ч; Наддув: -10 → -5 %; ПВ: неизвестно → 60 °C", changes.describe());
        verify(client).readRegisters(SAUNA_PLC, Arrays.asList(100,101,200,201));
        verifyNoMoreInteractions(client);
    }
    @Test void emptyFieldsDoNotReadController() {
        assertNull(new SettingChanges(client).describe()); verifyNoInteractions(client);
    }
    @Test void detailedScheduleUsesReadableDatesTimesAndModes() throws Exception {
        when(client.readRegisters(eq(SAUNA_PLC), anyList())).thenReturn(Collections.emptyMap());
        UpdateScheduleIntervalRequest interval = new UpdateScheduleIntervalRequest();
        interval.setScheduleId(1); interval.setStartTime((9 << 8) | 30); interval.setEndTime(18 << 8);
        interval.setAirControlMode(1); interval.setZone1ControlMode(1); interval.setZone1TargetTemperature(65.0);
        UpdateScheduleGroupRequest request = new UpdateScheduleGroupRequest();
        request.setStartDate((10 << 8) | 5); request.setEndDate(0); request.setWeekDays(Arrays.asList("mon", "fri"));
        request.setIntervals(Collections.singletonList(interval));
        String details = new ScheduleService(client).describeGroupChanges(request);
        assertTrue(details.contains("неизвестно → 09:30"));
        assertTrue(details.contains("неизвестно → 05.10"));
        assertTrue(details.contains("неизвестно → Пн, Пт"));
        assertTrue(details.contains("неизвестно → ЭКО"));
        assertTrue(details.contains("цель зоны 1: неизвестно → 65 °C"));
        assertFalse(details.contains("цель зоны 2"));
    }
    @Test void longLogSurvivesDatabaseRoundTrip() {
        org.springframework.jdbc.datasource.SingleConnectionDataSource source =
            new org.springframework.jdbc.datasource.SingleConnectionDataSource("jdbc:sqlite::memory:", true);
        try {
            UserActionLogService service = new UserActionLogService(new org.springframework.jdbc.core.JdbcTemplate(source));
            service.initSchema();
            String details = String.join("; ", Collections.nCopies(60, "ОРП, зона 2, T3 текущая: -5 → -4 °C"));
            assertTrue(details.length() > 500);
            service.logAs("tester", ActionType.SETPOINT, "Уставки", true, details);
            assertEquals(details, service.findRecent(60, "ALL").get(0).get("details"));
        } finally { source.destroy(); }
    }
}
