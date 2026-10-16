package com.example.modbus_api.service;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CommissioningModesTest {
    @Test void physicalScheduleLogUsesNamesForBothSides() {
        assertEquals("ЭКО", org.springframework.test.util.ReflectionTestUtils.invokeMethod(
            com.example.modbus_api.controller.PhysicalScheduleController.class, "display", 12, 1));
        assertEquals("Ручной", org.springframework.test.util.ReflectionTestUtils.invokeMethod(
            com.example.modbus_api.controller.PhysicalScheduleController.class, "display", 15, 3));
        assertEquals("Норм", org.springframework.test.util.ReflectionTestUtils.invokeMethod(
            com.example.modbus_api.controller.PhysicalScheduleController.class, "display", 9, 2));
    }
    @Test void temperatureAndVolumeHaveDistinctManualCodes() {
        assertEquals("ЭКО", ModbusRegisterMap.temperatureModeName(1));
        assertEquals("АВТО", ModbusRegisterMap.temperatureModeName(2));
        assertEquals("Ручной", ModbusRegisterMap.temperatureModeName(3));
        assertEquals("Интенсив", ModbusRegisterMap.ventilationModeName(3));
        assertEquals("Ручной", ModbusRegisterMap.ventilationModeName(4));
        assertNull(ModbusRegisterMap.temperatureModeName(99));
    }
}
