package com.example.modbus_api.service;

import com.example.modbus_api.dto.OrpSettingsDto;
import com.example.modbus_api.dto.UpdateTemperatureControlRequest;
import com.example.modbus_api.dto.UpdateTemperatureSetpointsRequest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;

import static com.example.modbus_api.service.ModbusConstants.SAUNA_PLC;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class SettingsServiceTemperatureTest {

    @Test
    void selectingOutflowWritesBothHeatersAndBothUserSetFields() throws IOException {
        ModbusClientAdapter client = mock(ModbusClientAdapter.class);
        int userSetBase = ModbusRegisterMap.scheduleBase(4);
        when(client.readRegisters(eq(SAUNA_PLC), anyList()))
            .thenReturn(Collections.singletonMap(userSetBase, ModbusRegisterMap.SCHEDULE_USERSET_MODE));
        when(client.readRegister(SAUNA_PLC, userSetBase + ModbusRegisterMap.SCHEDULE_STATE_OFFSET)).thenReturn(2);
        when(client.readRegister(SAUNA_PLC, ModbusRegisterMap.AGGREGATE_STATE)).thenReturn(64);

        new SettingsService(client).setTemperatureControlModes(
            new UpdateTemperatureControlRequest(2, 2));

        verify(client).writeRegister(SAUNA_PLC, userSetBase + 12, 2);
        verify(client).writeRegister(SAUNA_PLC, userSetBase + 15, 2);
        verify(client).writeRegister(SAUNA_PLC,
            ModbusRegisterMap.HEATER_ZONE_1 + ModbusRegisterMap.TEMPERATURE_CONTROL_OFFSET, 2);
        verify(client).writeRegister(SAUNA_PLC,
            ModbusRegisterMap.HEATER_ZONE_2 + ModbusRegisterMap.TEMPERATURE_CONTROL_OFFSET, 2);
        verify(client).writeRegister(SAUNA_PLC,
            userSetBase + ModbusRegisterMap.SCHEDULE_TEMPERATURE_CONTROL_ZONE_1_OFFSET, 2);
        verify(client).writeRegister(SAUNA_PLC,
            userSetBase + ModbusRegisterMap.SCHEDULE_TEMPERATURE_CONTROL_ZONE_2_OFFSET, 2);
        verify(client).writeRegister(SAUNA_PLC, ModbusRegisterMap.AGGREGATE_COMMAND, 6);
    }

    @Test
    void selectingZoneModeWritesOnlyThatZoneAndPreservesSource() throws IOException {
        ModbusClientAdapter client = mock(ModbusClientAdapter.class);
        int userSetBase = ModbusRegisterMap.scheduleBase(1);
        when(client.readRegisters(eq(SAUNA_PLC), anyList()))
            .thenReturn(Collections.singletonMap(userSetBase, ModbusRegisterMap.SCHEDULE_USERSET_MODE));
        when(client.readRegister(SAUNA_PLC, userSetBase + ModbusRegisterMap.SCHEDULE_STATE_OFFSET)).thenReturn(0);
        when(client.readRegister(SAUNA_PLC, ModbusRegisterMap.AGGREGATE_STATE)).thenReturn(1);

        new SettingsService(client).setTemperatureControlModes(
            new UpdateTemperatureControlRequest(0, null));

        verify(client).writeRegister(SAUNA_PLC, userSetBase + 12, 2);
        verify(client, never()).writeRegister(eq(SAUNA_PLC), eq(userSetBase + 15), anyInt());
        verify(client).writeRegister(SAUNA_PLC,
            ModbusRegisterMap.HEATER_ZONE_1 + ModbusRegisterMap.TEMPERATURE_CONTROL_OFFSET, 0);
        verify(client).writeRegister(SAUNA_PLC,
            userSetBase + ModbusRegisterMap.SCHEDULE_TEMPERATURE_CONTROL_ZONE_1_OFFSET, 0);
        verify(client, never()).writeRegister(eq(SAUNA_PLC),
            eq(userSetBase + ModbusRegisterMap.SCHEDULE_STATE_OFFSET), anyInt());
        verify(client).writeRegister(SAUNA_PLC, ModbusRegisterMap.AGGREGATE_COMMAND, 6);
        verify(client, never()).writeRegister(eq(SAUNA_PLC),
            eq(ModbusRegisterMap.HEATER_ZONE_2 + ModbusRegisterMap.TEMPERATURE_CONTROL_OFFSET), anyInt());
    }

    @Test
    void writesScaledPvAndOvButWholeDegreeOrpPoints() throws IOException {
        ModbusClientAdapter client = mock(ModbusClientAdapter.class);
        when(client.readRegisters(eq(SAUNA_PLC), anyList())).thenReturn(Collections.singletonMap(ModbusRegisterMap.scheduleBase(3), 5));
        UpdateTemperatureSetpointsRequest request = new UpdateTemperatureSetpointsRequest();
        request.setOutflowTarget(19.5);
        request.setInflowTargetZone1(21.4);
        request.setOrpZone2(OrpSettingsDto.builder()
            .targetT1(20.0)
            .currentT1(-5.0)
            .targetT3(14.0)
            .build());

        new SettingsService(client).updateTemperatureSetpoints(request);
        verify(client).writeRegister(SAUNA_PLC, ModbusRegisterMap.scheduleBase(3) + 12, 2);
        verify(client).writeRegister(SAUNA_PLC, ModbusRegisterMap.scheduleBase(3) + 15, 2);

        verify(client).writeRegister(SAUNA_PLC,
            ModbusRegisterMap.HEATER_ZONE_1 + ModbusRegisterMap.OUTFLOW_TARGET_OFFSET, 195);
        verify(client).writeRegister(SAUNA_PLC,
            ModbusRegisterMap.HEATER_ZONE_2 + ModbusRegisterMap.OUTFLOW_TARGET_OFFSET, 195);
        verify(client).writeRegister(SAUNA_PLC,
            ModbusRegisterMap.HEATER_ZONE_1 + ModbusRegisterMap.INFLOW_TARGET_OFFSET, 214);
        verify(client).writeRegister(SAUNA_PLC,
            ModbusRegisterMap.HEATER_ZONE_2 + ModbusRegisterMap.ORP_TARGET_T1_OFFSET, 20);
        verify(client).writeRegister(SAUNA_PLC,
            ModbusRegisterMap.HEATER_ZONE_2 + ModbusRegisterMap.ORP_CURRENT_T1_OFFSET, 0xFFFB);
        verify(client).writeRegister(SAUNA_PLC,
            ModbusRegisterMap.HEATER_ZONE_2 + ModbusRegisterMap.ORP_TARGET_T3_OFFSET, 14);
        verify(client).writeRegister(SAUNA_PLC, ModbusRegisterMap.AGGREGATE_COMMAND, 6);
    }

    @Test
    void rejectsOutflowForOnlyOneZoneBeforeModbusAccess() {
        ModbusClientAdapter client = mock(ModbusClientAdapter.class);

        assertThrows(IllegalArgumentException.class, () ->
            new SettingsService(client).setTemperatureControlModes(
                new UpdateTemperatureControlRequest(2, null)));

        verifyNoInteractions(client);
    }

    @Test void missingUserSetStopsTemperatureWrites() throws IOException {
        ModbusClientAdapter client=mock(ModbusClientAdapter.class);
        when(client.readRegisters(eq(SAUNA_PLC),anyList())).thenReturn(Collections.emptyMap());
        UpdateTemperatureSetpointsRequest request=new UpdateTemperatureSetpointsRequest();request.setInflowTargetZone1(21.0);
        assertThrows(IOException.class,()->new SettingsService(client).updateTemperatureSetpoints(request));
        verify(client,never()).writeRegister(anyString(),anyInt(),anyInt());
    }
    @Test void legacyTemperatureSettingsForceAffectedZoneBeforeCommand() throws IOException {
        ModbusClientAdapter client=mock(ModbusClientAdapter.class);
        int base=ModbusRegisterMap.scheduleBase(9);
        when(client.readRegisters(eq(SAUNA_PLC),anyList())).thenReturn(Collections.singletonMap(base,5));
        new SettingsService(client).updateTempSettings(21.0,null,null,null,null,null,null,null,null,null,null,null,null);
        org.mockito.InOrder order=inOrder(client);
        order.verify(client).writeRegister(SAUNA_PLC,base+12,2);
        verify(client, never()).writeRegister(eq(SAUNA_PLC), eq(base+15), anyInt());
        order.verify(client).writeRegister(SAUNA_PLC,ModbusConstants.REG_T_TARGET_ZONE1,210);
        order.verify(client).writeRegister(SAUNA_PLC,ModbusRegisterMap.AGGREGATE_COMMAND,6);
    }

    @Test void everyModeEnablesAutoFirstEvenWhenUserSetWasManual() throws IOException {
        for (int mode=0; mode<=2; mode++) {
            ModbusClientAdapter client=mock(ModbusClientAdapter.class);
            int base=ModbusRegisterMap.scheduleBase(7);
            when(client.readRegisters(eq(SAUNA_PLC),anyList())).thenReturn(Collections.singletonMap(base,5));
            when(client.readRegister(SAUNA_PLC,base+1)).thenReturn(1);
            new SettingsService(client).setTemperatureControlModes(new UpdateTemperatureControlRequest(mode==2?2:null,mode));
            org.mockito.InOrder order=inOrder(client);
            order.verify(client).writeRegister(SAUNA_PLC,base+15,2);
            order.verify(client).writeRegister(SAUNA_PLC,ModbusRegisterMap.HEATER_ZONE_2+3,mode);
            order.verify(client).writeRegister(SAUNA_PLC,base+16,mode);
        }
    }
    @Test void zoneTwoTargetEnablesAutoBeforeWritingAndLeavesZoneOneAlone() throws IOException {
        ModbusClientAdapter client=mock(ModbusClientAdapter.class);
        int base=ModbusRegisterMap.scheduleBase(7);
        when(client.readRegisters(eq(SAUNA_PLC),anyList())).thenReturn(Collections.singletonMap(base,5));
        UpdateTemperatureSetpointsRequest request=new UpdateTemperatureSetpointsRequest();request.setInflowTargetZone2(22.5);
        new SettingsService(client).updateTemperatureSetpoints(request);
        org.mockito.InOrder order=inOrder(client);
        order.verify(client).writeRegister(SAUNA_PLC,base+15,2);
        order.verify(client).writeRegister(SAUNA_PLC,base+17,225);
        order.verify(client).writeRegister(SAUNA_PLC,ModbusRegisterMap.HEATER_ZONE_2+25,225);
        verify(client,never()).writeRegister(eq(SAUNA_PLC),eq(base+1),anyInt());
        order.verify(client).writeRegister(SAUNA_PLC,ModbusRegisterMap.AGGREGATE_COMMAND,6);
        verify(client,never()).writeRegister(eq(SAUNA_PLC),eq(base+12),anyInt());
    }
    @Test void failedAutoWritePreventsTargetAndRecalculation() throws IOException {
        ModbusClientAdapter client=mock(ModbusClientAdapter.class);
        int base=ModbusRegisterMap.scheduleBase(7);
        when(client.readRegisters(eq(SAUNA_PLC),anyList())).thenReturn(Collections.singletonMap(base,5));
        doThrow(new IOException("write failed")).when(client).writeRegister(SAUNA_PLC,base+15,2);
        UpdateTemperatureSetpointsRequest request=new UpdateTemperatureSetpointsRequest();request.setInflowTargetZone2(22.5);
        assertThrows(IOException.class,()->new SettingsService(client).updateTemperatureSetpoints(request));
        verify(client,never()).writeRegister(eq(SAUNA_PLC),eq(ModbusRegisterMap.HEATER_ZONE_2+25),anyInt());
        verify(client,never()).writeRegister(eq(SAUNA_PLC),eq(ModbusRegisterMap.AGGREGATE_COMMAND),anyInt());
    }
    @Test void everyTemperatureModePreservesScheduleOrScreenSelection() throws IOException {
        for (int selected : new int[]{0, 1}) for (int mode : new int[]{0, 1, 2}) {
            ModbusClientAdapter client = mock(ModbusClientAdapter.class);
            int base = ModbusRegisterMap.scheduleBase(3);
            java.util.Map<Integer,Integer> registers = new java.util.HashMap<>();
            registers.put(base, 5); registers.put(base+1, selected);
            when(client.readRegisters(eq(SAUNA_PLC), anyList())).thenReturn(registers);
            when(client.readRegister(eq(SAUNA_PLC), anyInt())).thenAnswer(i -> registers.getOrDefault((Integer)i.getArgument(1), 0));
            doAnswer(i -> { registers.put(i.getArgument(1), i.getArgument(2)); return null; })
                .when(client).writeRegister(eq(SAUNA_PLC), anyInt(), anyInt());
            new SettingsService(client).setTemperatureControlModes(new UpdateTemperatureControlRequest(mode, mode));
            org.junit.jupiter.api.Assertions.assertEquals(selected, registers.get(base+1).intValue());
            verify(client, never()).writeRegister(eq(SAUNA_PLC), eq(base+1), anyInt());
        }
    }

    @Test void pvTargetSurvivesRecalculationFromUserSetForBothZones() throws IOException {
        for (int zone : new int[]{1, 2}) {
            ModbusClientAdapter client = mock(ModbusClientAdapter.class);
            int base = ModbusRegisterMap.scheduleBase(3);
            int stored = base + (zone == 1 ? 14 : 17);
            int actual = (zone == 1 ? ModbusRegisterMap.HEATER_ZONE_1 : ModbusRegisterMap.HEATER_ZONE_2) + 25;
            java.util.Map<Integer,Integer> registers = new java.util.HashMap<>();
            registers.put(base, 5); registers.put(base+1, 1); registers.put(stored, 0);
            when(client.readRegisters(eq(SAUNA_PLC), anyList())).thenAnswer(i -> new java.util.HashMap<>(registers));
            doAnswer(i -> {
                int address = i.getArgument(1); int value = i.getArgument(2);
                registers.put(address, value);
                if (address == ModbusRegisterMap.AGGREGATE_COMMAND && value == 6)
                    registers.put(actual, registers.get(stored));
                return null;
            }).when(client).writeRegister(eq(SAUNA_PLC), anyInt(), anyInt());
            UpdateTemperatureSetpointsRequest request = new UpdateTemperatureSetpointsRequest();
            if (zone == 1) request.setInflowTargetZone1(22.5); else request.setInflowTargetZone2(22.5);
            new SettingsService(client).updateTemperatureSetpoints(request);
            org.junit.jupiter.api.Assertions.assertEquals(225, registers.get(actual).intValue());
            org.junit.jupiter.api.Assertions.assertEquals(1, registers.get(base+1).intValue());
            verify(client, never()).writeRegister(eq(SAUNA_PLC), eq(base+1), anyInt());
            verify(client, never()).writeRegister(eq(SAUNA_PLC), eq(base+(zone == 1 ? 17 : 14)), anyInt());
        }
    }
}
