package com.example.modbus_api.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModbusRegisterMapTest {

    @Test
    void decodesLowWordFirstFloat() {
        float expected = 1234.5f;
        int bits = Float.floatToIntBits(expected);

        Double actual = ModbusRegisterMap.decodeFloat(bits & 0xFFFF, (bits >>> 16) & 0xFFFF);

        assertEquals((double) expected, actual, 0.0001);
    }

    @Test
    void rejectsIncompleteAndNonFiniteFloat() {
        assertNull(ModbusRegisterMap.decodeFloat(null, 0));
        assertNull(ModbusRegisterMap.decodeFloat(0, 0x7F80));
    }

    @Test
    void usesTwentySchedulesWithHexTwentyStride() {
        assertEquals(0x2000, ModbusRegisterMap.scheduleBase(0));
        assertEquals(0x2020, ModbusRegisterMap.scheduleBase(1));
        assertEquals(0x2260, ModbusRegisterMap.scheduleBase(19));
        assertEquals(0x2000, ModbusRegisterMap.scheduleBaseById(1));
        assertEquals(0x2260, ModbusRegisterMap.scheduleBaseById(20));
        assertThrows(IllegalArgumentException.class, () -> ModbusRegisterMap.scheduleBase(20));
        assertThrows(IllegalArgumentException.class, () -> ModbusRegisterMap.scheduleBaseById(0));
    }

    @Test
    void mapsUiModes() {
        assertEquals("Норм", ModbusRegisterMap.ventilationModeName(2));
        assertEquals("ОРП", ModbusRegisterMap.temperatureControlName(0));
        assertNull(ModbusRegisterMap.temperatureControlName(99));
    }

    @Test
    void mapsRecuperatorWidgetRegisters() {
        assertEquals(0x0511, ModbusRegisterMap.RECUPERATOR_TRACKING_INTERVAL);
        assertEquals(0x0800, ModbusRegisterMap.RECUPERATOR_STATE);
        assertEquals(0x0801, ModbusRegisterMap.RECUPERATOR_MIN_RPM_SETTING);
        assertEquals(0x0802, ModbusRegisterMap.RECUPERATOR_MAX_RPM_SETTING);
        assertEquals(0x0803, ModbusRegisterMap.RECUPERATOR_RPM_STEP_SETTING);
        assertEquals(0x0813, ModbusRegisterMap.RECUPERATOR_TARGET_RPM);
        assertEquals(0x0814, ModbusRegisterMap.RECUPERATOR_CURRENT_RPM);
        assertEquals(0x0815, ModbusRegisterMap.RECUPERATOR_MOTOR_RPM);
    }
}
