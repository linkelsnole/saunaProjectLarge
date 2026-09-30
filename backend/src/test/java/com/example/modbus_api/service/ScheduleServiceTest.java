package com.example.modbus_api.service;

import com.example.modbus_api.dto.ScheduleData;
import com.example.modbus_api.dto.ScheduleListData;
import com.example.modbus_api.dto.UpdateScheduleGroupRequest;
import com.example.modbus_api.dto.UpdateScheduleIntervalRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.modbus_api.service.ModbusConstants.CMD_FAST_MODE_CHANGE;
import static com.example.modbus_api.service.ModbusConstants.SAUNA_PLC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ScheduleServiceTest {

    @Test
    void parsesNewMapIncludingBothTemperatureZonesAndLowWordFirstFloat() {
        ModbusClientAdapter client = mock(ModbusClientAdapter.class);
        ScheduleService service = new ScheduleService(client);
        int base = ModbusRegisterMap.scheduleBaseById(1);
        Map<Integer, Integer> values = new HashMap<>();

        values.put(base + ModbusRegisterMap.SCHEDULE_MODE_OFFSET, 5);
        values.put(base + ModbusRegisterMap.SCHEDULE_STATE_OFFSET, 3);
        values.put(base + ModbusRegisterMap.SCHEDULE_WEEK_DAYS_OFFSET, 0x55);
        values.put(base + ModbusRegisterMap.SCHEDULE_GROUP_OFFSET, 7);
        values.put(base + ModbusRegisterMap.SCHEDULE_START_DATE_OFFSET, 0x0101);
        values.put(base + ModbusRegisterMap.SCHEDULE_END_DATE_OFFSET, 0x0C1F);
        values.put(base + ModbusRegisterMap.SCHEDULE_START_TIME_OFFSET, 0x081E);
        values.put(base + ModbusRegisterMap.SCHEDULE_END_TIME_OFFSET, 0x173B);
        values.put(base + ModbusRegisterMap.SCHEDULE_DURATION_OFFSET, 120);
        values.put(base + ModbusRegisterMap.SCHEDULE_VENTILATION_MODE_OFFSET, 3);
        putFloat(values, base + ModbusRegisterMap.SCHEDULE_TARGET_AIRFLOW_OFFSET, 750.5f);
        values.put(base + ModbusRegisterMap.SCHEDULE_TEMPERATURE_MODE_ZONE_1_OFFSET, 2);
        values.put(base + ModbusRegisterMap.SCHEDULE_TEMPERATURE_CONTROL_ZONE_1_OFFSET, 1);
        values.put(base + ModbusRegisterMap.SCHEDULE_TARGET_TEMPERATURE_ZONE_1_OFFSET, 215);
        values.put(base + ModbusRegisterMap.SCHEDULE_TEMPERATURE_MODE_ZONE_2_OFFSET, 1);
        values.put(base + ModbusRegisterMap.SCHEDULE_TEMPERATURE_CONTROL_ZONE_2_OFFSET, 2);
        values.put(base + ModbusRegisterMap.SCHEDULE_TARGET_TEMPERATURE_ZONE_2_OFFSET, 0xFFCE);

        ScheduleData result = service.parseScheduleFromMap(1, values);

        assertEquals(ScheduleData.ScheduleMode.USERSET, result.getMode());
        assertEquals(ScheduleData.ScheduleStatus.ACTIVE, result.getStatus());
        assertEquals(3, result.getStatusRawCode());
        assertEquals(0x55, result.getWeekDayMask());
        assertEquals(7, result.getGroup());
        assertEquals(750.5, result.getAirControl().getTargetAirflow(), 0.001);
        assertEquals(ScheduleData.VentilationMode.INTENSIVE, result.getAirControl().getMode());
        assertEquals(ScheduleData.TemperatureMode.AUTO, result.getTemperatureModeZone1());
        assertEquals(ScheduleData.TemperatureControlType.PV, result.getTemperatureZone1().getControlType());
        assertEquals(21.5, result.getTemperatureZone1().getTargetTemperature(), 0.001);
        assertEquals(ScheduleData.TemperatureControlType.OV, result.getTemperatureZone2().getControlType());
        assertEquals(-5.0, result.getTemperatureZone2().getTargetTemperature(), 0.001);
    }

    @Test
    void readsAllTwentySchedulesAcrossFullHexTwentyStride() throws IOException {
        ModbusClientAdapter client = mock(ModbusClientAdapter.class);
        when(client.readRegisters(eq(SAUNA_PLC), anyList())).thenReturn(Collections.emptyMap());

        ScheduleListData result = new ScheduleService(client).getAllSchedules();

        assertEquals(20, result.getSchedules().size());
        assertEquals(Integer.valueOf(20), result.getTotalSlots());
        ArgumentCaptor<List> registersCaptor = ArgumentCaptor.forClass(List.class);
        verify(client).readRegisters(eq(SAUNA_PLC), registersCaptor.capture());
        List<Integer> registers = registersCaptor.getValue();
        assertTrue(registers.contains(ModbusRegisterMap.SCHEDULE_SIZE));
        assertTrue(registers.contains(ModbusRegisterMap.scheduleBase(0)));
        assertTrue(registers.contains(ModbusRegisterMap.scheduleBase(19)
            + ModbusRegisterMap.SCHEDULE_TARGET_TEMPERATURE_ZONE_2_OFFSET));
    }

    @Test
    void savesGroupWithBothZonesAndPreservesWorkingStateBit() throws IOException {
        ModbusClientAdapter client = mock(ModbusClientAdapter.class);
        Map<Integer, Integer> states = topology();
        int base4 = ModbusRegisterMap.scheduleBaseById(4);
        int base5 = ModbusRegisterMap.scheduleBaseById(5);
        int base6 = ModbusRegisterMap.scheduleBaseById(6);
        states.put(base4 + ModbusRegisterMap.SCHEDULE_STATE_OFFSET, 2);
        states.put(base5 + ModbusRegisterMap.SCHEDULE_STATE_OFFSET, 3);
        states.put(base6 + ModbusRegisterMap.SCHEDULE_STATE_OFFSET, 0);
        when(client.readRegisters(eq(SAUNA_PLC), anyList())).thenReturn(states);

        UpdateScheduleGroupRequest request = new UpdateScheduleGroupRequest(
            0x0101,
            0x031F,
            Arrays.asList("mon", "wed", "sun"),
            Arrays.asList(
                interval(4, 0x0800, 0x0C00, 1, 2, 21.5, 2, 21.5),
                interval(5, 0x0C00, 0x1200, 2, 0, 33.0, 1, 19.0),
                interval(6, 0x1200, 0x1700, 3, 1, 20.0, 0, null)
            ),
            true
        );

        new ScheduleService(client).updateScheduleGroup(1, request);

        verify(client).writeRegister(SAUNA_PLC, base4 + ModbusRegisterMap.SCHEDULE_STATE_OFFSET, 3);
        verify(client, never()).writeRegister(SAUNA_PLC, base5 + ModbusRegisterMap.SCHEDULE_STATE_OFFSET, 3);
        verify(client).writeRegister(SAUNA_PLC, base6 + ModbusRegisterMap.SCHEDULE_STATE_OFFSET, 1);
        verify(client, never()).writeRegister(eq(SAUNA_PLC), eq(base4 + ModbusRegisterMap.SCHEDULE_MODE_OFFSET), anyInt());
        verify(client).writeRegister(SAUNA_PLC, base4 + ModbusRegisterMap.SCHEDULE_WEEK_DAYS_OFFSET, 0b1000101);
        verify(client).writeRegister(SAUNA_PLC, base4 + ModbusRegisterMap.SCHEDULE_START_TIME_OFFSET, 0x0800);
        verify(client).writeRegister(SAUNA_PLC, base4 + ModbusRegisterMap.SCHEDULE_END_TIME_OFFSET, 0x0C00);
        verify(client).writeRegister(SAUNA_PLC, base4 + ModbusRegisterMap.SCHEDULE_VENTILATION_MODE_OFFSET, 1);
        verify(client).writeRegister(SAUNA_PLC,
            base4 + ModbusRegisterMap.SCHEDULE_TEMPERATURE_CONTROL_ZONE_1_OFFSET, 2);
        verify(client).writeRegister(SAUNA_PLC,
            base4 + ModbusRegisterMap.SCHEDULE_TARGET_TEMPERATURE_ZONE_1_OFFSET, 215);
        verify(client).writeRegister(SAUNA_PLC,
            base4 + ModbusRegisterMap.SCHEDULE_TEMPERATURE_CONTROL_ZONE_2_OFFSET, 2);
        verify(client).writeRegister(SAUNA_PLC,
            base4 + ModbusRegisterMap.SCHEDULE_TARGET_TEMPERATURE_ZONE_2_OFFSET, 215);
        verify(client, never()).writeRegister(eq(SAUNA_PLC),
            eq(base5 + ModbusRegisterMap.SCHEDULE_TARGET_TEMPERATURE_ZONE_1_OFFSET), anyInt());
        for (int base : new int[] {base4, base5, base6}) verify(client).writeRegister(SAUNA_PLC,
            base + ModbusRegisterMap.SCHEDULE_TEMPERATURE_MODE_ZONE_1_OFFSET, 2);
        for (int base : new int[] {base4, base5, base6}) verify(client).writeRegister(SAUNA_PLC,
            base + ModbusRegisterMap.SCHEDULE_TEMPERATURE_MODE_ZONE_2_OFFSET, 2);
        verify(client).writeRegister(SAUNA_PLC, ModbusRegisterMap.AGGREGATE_COMMAND, CMD_FAST_MODE_CHANGE);
    }

    @Test
    void deactivatesGroupWithoutClearingWorkingBit() throws IOException {
        ModbusClientAdapter client = mock(ModbusClientAdapter.class);
        int base7 = ModbusRegisterMap.scheduleBaseById(7);
        int base8 = ModbusRegisterMap.scheduleBaseById(8);
        Map<Integer, Integer> states = topology();
        states.put(base7 + ModbusRegisterMap.SCHEDULE_STATE_OFFSET, 3);
        states.put(base8 + ModbusRegisterMap.SCHEDULE_STATE_OFFSET, 2);
        when(client.readRegisters(eq(SAUNA_PLC), anyList())).thenReturn(states);

        new ScheduleService(client).toggleScheduleGroupActive(2, false);

        verify(client).writeRegister(SAUNA_PLC, base7 + ModbusRegisterMap.SCHEDULE_STATE_OFFSET, 2);
        verify(client, never()).writeRegister(SAUNA_PLC, base8 + ModbusRegisterMap.SCHEDULE_STATE_OFFSET, 2);
        verify(client).writeRegister(SAUNA_PLC, ModbusRegisterMap.AGGREGATE_COMMAND, CMD_FAST_MODE_CHANGE);
    }

    @Test
    void rejectsIntervalFromAnotherGroupBeforeAnyWrite() throws IOException {
        ModbusClientAdapter client = mock(ModbusClientAdapter.class);
        when(client.readRegisters(eq(SAUNA_PLC), anyList())).thenReturn(topology());
        UpdateScheduleGroupRequest request = new UpdateScheduleGroupRequest(
            0x0101,
            0x031F,
            Collections.singletonList("mon"),
            Collections.singletonList(interval(7, 0x0800, 0x1200, 1, 0, null, 0, null)),
            true
        );

        assertThrows(IllegalArgumentException.class,
            () -> new ScheduleService(client).updateScheduleGroup(1, request));
        verify(client, never()).writeRegister(anyString(), anyInt(), anyInt());
    }

    @Test
    void rejectsInvalidTimeBeforeAnyRegisterWrite() {
        ModbusClientAdapter client = mock(ModbusClientAdapter.class);
        UpdateScheduleGroupRequest request = new UpdateScheduleGroupRequest(
            0x0101,
            0x031F,
            Collections.singletonList("mon"),
            Collections.singletonList(interval(4, 0x1860, 0x1200, 1, 0, null, 0, null)),
            true
        );

        assertThrows(IllegalArgumentException.class,
            () -> new ScheduleService(client).updateScheduleGroup(1, request));
        verifyNoInteractions(client);
    }

    @Test
    void rejectsOvSelectedForOnlyOneZoneBeforeModbusAccess() {
        ModbusClientAdapter client = mock(ModbusClientAdapter.class);
        UpdateScheduleGroupRequest request = new UpdateScheduleGroupRequest(
            0x0101,
            0x031F,
            Collections.singletonList("mon"),
            Collections.singletonList(interval(4, 0x0800, 0x1200, 1, 2, 20.0, 1, 20.0)),
            true
        );

        assertThrows(IllegalArgumentException.class,
            () -> new ScheduleService(client).updateScheduleGroup(1, request));
        verifyNoInteractions(client);
    }

    @Test
    void rejectsDifferentOvTargetsBeforeModbusAccess() {
        ModbusClientAdapter client = mock(ModbusClientAdapter.class);
        UpdateScheduleGroupRequest request = new UpdateScheduleGroupRequest(
            0x0101,
            0x031F,
            Collections.singletonList("mon"),
            Collections.singletonList(interval(4, 0x0800, 0x1200, 1, 2, 20.0, 2, 21.0)),
            true
        );

        assertThrows(IllegalArgumentException.class,
            () -> new ScheduleService(client).updateScheduleGroup(1, request));
        verifyNoInteractions(client);
    }

    @Test
    void rejectsMissingPvTargetBeforeModbusAccess() {
        ModbusClientAdapter client = mock(ModbusClientAdapter.class);
        UpdateScheduleGroupRequest request = new UpdateScheduleGroupRequest(
            0x0101,
            0x031F,
            Collections.singletonList("mon"),
            Collections.singletonList(interval(4, 0x0800, 0x1200, 1, 1, null, 0, null)),
            true
        );

        assertThrows(IllegalArgumentException.class,
            () -> new ScheduleService(client).updateScheduleGroup(1, request));
        verifyNoInteractions(client);
    }

    private static Map<Integer, Integer> topology() {
        Map<Integer, Integer> values = new HashMap<>();
        for (int id=1; id<=20; id++) {
            int base=ModbusRegisterMap.scheduleBaseById(id);
            values.put(base, id>=4 && id<=8 ? 4 : 0);
            values.put(base+8, id>=4 && id<=6 ? 1 : id>=7 && id<=8 ? 2 : 0);
        }
        return values;
    }

    @Test void activatesActualMembersRatherThanHistoricalPairs() throws IOException {
        ModbusClientAdapter client=mock(ModbusClientAdapter.class);
        Map<Integer,Integer> values=topology();
        values.put(0x20C8,42); values.put(0x2240,4); values.put(0x2248,42);
        values.put(0x20C1,2); values.put(0x2241,0);
        when(client.readRegisters(eq(SAUNA_PLC),anyList())).thenReturn(values);
        new ScheduleService(client).toggleScheduleGroupActive(42,true);
        verify(client).writeRegister(SAUNA_PLC,0x20C1,3);
        verify(client).writeRegister(SAUNA_PLC,0x2241,1);
        verify(client,never()).writeRegister(eq(SAUNA_PLC),eq(0x20E1),anyInt());
    }

    @Test void zeroGroupActivationOnlyTouchesRequestedSlot() throws IOException {
        ModbusClientAdapter client=mock(ModbusClientAdapter.class);
        Map<Integer,Integer> values=topology();
        values.put(0x20C8,0); values.put(0x20E8,0); values.put(0x20C1,0);
        when(client.readRegisters(eq(SAUNA_PLC),anyList())).thenReturn(values);
        new ScheduleService(client).toggleScheduleGroupActive(-7,true);
        verify(client).writeRegister(SAUNA_PLC,0x20C1,1);
        verify(client,never()).writeRegister(eq(SAUNA_PLC),eq(0x20E1),anyInt());
    }

    @Test void incompleteTopologyNeverWritesRegisters() throws IOException {
        ModbusClientAdapter client=mock(ModbusClientAdapter.class);
        when(client.readRegisters(eq(SAUNA_PLC),anyList())).thenReturn(Collections.emptyMap());
        assertThrows(IOException.class,()->new ScheduleService(client).toggleScheduleGroupActive(1,true));
        verify(client,never()).writeRegister(anyString(),anyInt(),anyInt());
    }

    private static UpdateScheduleIntervalRequest interval(
        int id,
        int start,
        int end,
        int airMode,
        int zone1Mode,
        Double zone1Target,
        int zone2Mode,
        Double zone2Target
    ) {
        return new UpdateScheduleIntervalRequest(
            id, start, end, airMode, zone1Mode, zone1Target, zone2Mode, zone2Target);
    }

    private static void putFloat(Map<Integer, Integer> values, int base, float value) {
        int bits = Float.floatToIntBits(value);
        values.put(base, bits & 0xFFFF);
        values.put(base + 1, (bits >>> 16) & 0xFFFF);
    }
}
