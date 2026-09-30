package com.example.modbus_api.service;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.io.IOException;

import static com.example.modbus_api.service.ModbusConstants.SAUNA_PLC;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

class SettingsServiceRecuperatorTest {

    @Test
    void writesUpdatedRecuperatorRegistersWithPowerMinusTwoScale() throws IOException {
        ModbusClientAdapter client = mock(ModbusClientAdapter.class);

        new SettingsService(client).updateRecuperatorSettings(0.5, 10.0, 0.1, 120);

        InOrder writes = inOrder(client);
        writes.verify(client).writeRegister(
            SAUNA_PLC, ModbusRegisterMap.RECUPERATOR_TRACKING_INTERVAL, 120);
        writes.verify(client).writeRegister(
            SAUNA_PLC, ModbusRegisterMap.RECUPERATOR_MIN_RPM_SETTING, 50);
        writes.verify(client).writeRegister(
            SAUNA_PLC, ModbusRegisterMap.RECUPERATOR_MAX_RPM_SETTING, 1000);
        writes.verify(client).writeRegister(
            SAUNA_PLC, ModbusRegisterMap.RECUPERATOR_RPM_STEP_SETTING, 10);
        writes.verify(client).writeRegister(
            SAUNA_PLC, ModbusRegisterMap.AGGREGATE_COMMAND, 6);
    }
}
