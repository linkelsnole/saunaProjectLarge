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
    void selectingZoneModeWritesOnlyThatZoneAndActivatesUserSet() throws IOException {
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
        verify(client).writeRegister(SAUNA_PLC,
            userSetBase + ModbusRegisterMap.SCHEDULE_STATE_OFFSET, 1);
        verify(client).writeRegister(SAUNA_PLC, ModbusRegisterMap.AGGREGATE_COMMAND, 1);
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
        order.verify(client).writeRegister(SAUNA_PLC,ModbusRegisterMap.HEATER_ZONE_2+25,225);
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
}
