package com.example.modbus_api.service;

import com.example.modbus_api.dto.UpdateVolumeSettingsRequest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;

import static com.example.modbus_api.service.ModbusConstants.SAUNA_PLC;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SettingsServiceVolumeTest {

    @Test
    void updatesRunningUserSetModeAndPreservesStateBits() throws IOException {
        ModbusClientAdapter client = mock(ModbusClientAdapter.class);
        int base = ModbusRegisterMap.scheduleBase(4);
        when(client.readRegisters(eq(SAUNA_PLC), anyList()))
            .thenReturn(Collections.singletonMap(base, ModbusRegisterMap.SCHEDULE_USERSET_MODE));
        when(client.readRegister(SAUNA_PLC, base + ModbusRegisterMap.SCHEDULE_STATE_OFFSET))
            .thenReturn(2);
        when(client.readRegister(SAUNA_PLC, ModbusRegisterMap.AGGREGATE_STATE))
            .thenReturn(64);

        new SettingsService(client).setVentilationMode(3);

        verify(client).writeRegister(
            SAUNA_PLC,
            base + ModbusRegisterMap.SCHEDULE_VENTILATION_MODE_OFFSET,
            3);
        verify(client).writeRegister(
            SAUNA_PLC,
            base + ModbusRegisterMap.SCHEDULE_STATE_OFFSET,
            3);
        verify(client).writeRegister(
            SAUNA_PLC,
            ModbusRegisterMap.AGGREGATE_COMMAND,
            6);
    }

    @Test
    void selectingModeStartsStoppedUnitWithUserSet() throws IOException {
        ModbusClientAdapter client = mock(ModbusClientAdapter.class);
        int base = ModbusRegisterMap.scheduleBase(7);
        when(client.readRegisters(eq(SAUNA_PLC), anyList()))
            .thenReturn(Collections.singletonMap(base, ModbusRegisterMap.SCHEDULE_USERSET_MODE));
        when(client.readRegister(SAUNA_PLC, base + ModbusRegisterMap.SCHEDULE_STATE_OFFSET))
            .thenReturn(0);
        when(client.readRegister(SAUNA_PLC, ModbusRegisterMap.AGGREGATE_STATE))
            .thenReturn(1);

        new SettingsService(client).setVentilationMode(4);

        verify(client).writeRegister(
            SAUNA_PLC,
            base + ModbusRegisterMap.SCHEDULE_VENTILATION_MODE_OFFSET,
            4);
        verify(client).writeRegister(
            SAUNA_PLC,
            base + ModbusRegisterMap.SCHEDULE_STATE_OFFSET,
            1);
        verify(client).writeRegister(
            SAUNA_PLC,
            ModbusRegisterMap.AGGREGATE_COMMAND,
            1);
    }

    @Test
    void writesFloatSettingsLowWordFirstAndScaledSignedBoost() throws IOException {
        ModbusClientAdapter client = mock(ModbusClientAdapter.class);
        UpdateVolumeSettingsRequest request = new UpdateVolumeSettingsRequest();
        request.setEcoAirflow(750.5);
        request.setNormalAirflow(1500.0);
        request.setIntensiveAirflow(2000.25);
        request.setManualAirflow(1234.5);
        request.setBoostPercent(-5.0);

        new SettingsService(client).updateVolumeSettings(request);

        verifyFloatWrite(client, ModbusRegisterMap.AIRFLOW_ECO, 750.5f);
        verifyFloatWrite(client, ModbusRegisterMap.AIRFLOW_NORMAL, 1500.0f);
        verifyFloatWrite(client, ModbusRegisterMap.AIRFLOW_INTENSIVE, 2000.25f);
        verifyFloatWrite(client, ModbusRegisterMap.AIRFLOW_MANUAL, 1234.5f);
        verify(client).writeRegister(
            SAUNA_PLC,
            ModbusRegisterMap.AIRFLOW_IMBALANCE,
            ((int) (short) -50) & 0xFFFF);
        verify(client).writeRegister(
            SAUNA_PLC,
            ModbusRegisterMap.AGGREGATE_COMMAND,
            6);
    }

    @Test
    void rejectsInvalidSettingsBeforeWritingAnything() throws IOException {
        ModbusClientAdapter client = mock(ModbusClientAdapter.class);
        UpdateVolumeSettingsRequest request = new UpdateVolumeSettingsRequest();
        request.setEcoAirflow(21001.0);
        request.setBoostPercent(5.0);

        assertThrows(
            IllegalArgumentException.class,
            () -> new SettingsService(client).updateVolumeSettings(request));

        verify(client, never()).writeRegister(anyString(), anyInt(), anyInt());
    }

    @Test
    void acceptsFullAirflowRangeForEveryMode() throws IOException {
        for (double value : new double[]{0, 15000, 21000}) {
            ModbusClientAdapter client=mock(ModbusClientAdapter.class);
            UpdateVolumeSettingsRequest request=new UpdateVolumeSettingsRequest();
            request.setEcoAirflow(value); request.setNormalAirflow(value);
            request.setIntensiveAirflow(value); request.setManualAirflow(value);
            new SettingsService(client).updateVolumeSettings(request);
            verifyFloatWrite(client, ModbusRegisterMap.AIRFLOW_ECO,(float)value);
            verifyFloatWrite(client, ModbusRegisterMap.AIRFLOW_NORMAL,(float)value);
            verifyFloatWrite(client, ModbusRegisterMap.AIRFLOW_INTENSIVE,(float)value);
            verifyFloatWrite(client, ModbusRegisterMap.AIRFLOW_MANUAL,(float)value);
        }
    }

    @Test
    void rejectsOutOfRangeForEveryModeBeforeAnyWrite() throws IOException {
        for (double value : new double[]{-1, 21000.1, 21010, Double.NaN, Double.POSITIVE_INFINITY}) {
            for (int field=0;field<4;field++) {
                ModbusClientAdapter client=mock(ModbusClientAdapter.class);
                UpdateVolumeSettingsRequest request=new UpdateVolumeSettingsRequest();
                request.setBoostPercent(5.0);
                if(field==0)request.setEcoAirflow(value);
                if(field==1)request.setNormalAirflow(value);
                if(field==2)request.setIntensiveAirflow(value);
                if(field==3)request.setManualAirflow(value);
                IllegalArgumentException error=assertThrows(IllegalArgumentException.class,
                    ()->new SettingsService(client).updateVolumeSettings(request));
                org.junit.jupiter.api.Assertions.assertTrue(error.getMessage().contains("0–21000"));
                verify(client,never()).writeRegister(anyString(),anyInt(),anyInt());
            }
        }
    }

    private static void verifyFloatWrite(
        ModbusClientAdapter client,
        int register,
        float value
    ) throws IOException {
        verify(client).writeRegister(
            SAUNA_PLC,
            register,
            ModbusRegisterMap.floatLowWord(value));
        verify(client).writeRegister(
            SAUNA_PLC,
            register + 1,
            ModbusRegisterMap.floatHighWord(value));
    }
}
