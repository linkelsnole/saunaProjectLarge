package com.example.modbus_api.service;

import com.example.modbus_api.dto.DashboardData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.example.modbus_api.service.ModbusConstants.SAUNA_PLC;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DeviceServiceRegisterMapTest {
    @Test void filterCoefficientsUseHundredthsAndPreserveMissingData() throws IOException {
        ModbusClientAdapter client = mock(ModbusClientAdapter.class);
        Map<Integer, Integer> values = new HashMap<>();
        values.put(0x012C, 90); values.put(0x012D, 350);
        when(client.readRegisters(eq(SAUNA_PLC), anyList())).thenAnswer(call -> {
            java.util.List<Integer> requested = call.getArgument(1);
            assertTrue(requested.contains(0x012C)); assertTrue(requested.contains(0x012D));
            return values;
        });
        DeviceService service = new DeviceService(client);
        assertEquals(0.9, service.getDashboardData().getFilterSupplyCoefficient(), 0.0001);
        assertEquals(3.5, service.getDashboardData().getFilterExhaustCoefficient(), 0.0001);
        values.clear();
        assertNull(service.getDashboardData().getFilterSupplyCoefficient());
        assertNull(service.getDashboardData().getFilterExhaustCoefficient());
    }

    @Test
    void scalesAllEightPressureSensorsExactlyOnce() throws IOException {
        ModbusClientAdapter client = mock(ModbusClientAdapter.class);
        Map<Integer, Integer> registers = new HashMap<>();
        int[] addresses = {0x1208, 0x1218, 0x1228, 0x1238, 0x1248, 0x1258, 0x1268, 0x1278};
        for (int i = 0; i < addresses.length; i++) {
            putFloat(registers, addresses[i], 10.5f + i);
        }
        when(client.readRegisters(eq(SAUNA_PLC), anyList())).thenReturn(registers);
        DashboardData data = new DeviceService(client).getDashboardData();
        for (int i = 0; i < 4; i++) {
            assertEquals(105.0 + 10 * i, data.getFans().get(i).getPressure(), 0.001);
        }
        assertEquals(145.0, Double.parseDouble(data.getWSensors().get(0).getValue()), 0.001);
        assertEquals(155.0, Double.parseDouble(data.getWSensors().get(1).getValue()), 0.001);
        assertEquals("BP10", data.getPressureSensors().get(1).getId());
        assertEquals(165.0, Double.parseDouble(data.getPressureSensors().get(1).getValue()), 0.001);
        assertEquals("BP5", data.getPressureSensors().get(0).getId());
        assertEquals(175.0, Double.parseDouble(data.getPressureSensors().get(0).getValue()), 0.001);
    }

    @ParameterizedTest @ValueSource(ints = {8, 9, 65535})
    void normalStartNeverWritesWhileTestingIsEnabled(int mode) throws IOException {
        ModbusClientAdapter client = mock(ModbusClientAdapter.class);
        when(client.readRegister(SAUNA_PLC, 0x0302)).thenReturn(mode);
        assertThrows(IllegalStateException.class, () -> new DeviceService(client).enableRecuperator());
        verify(client, never()).writeRegister(anyString(), anyInt(), anyInt());
        verify(client, never()).writeCommandOnce(anyString(), anyInt(), anyInt());
    }
    @Test void rotorMaximumUsesDocumentedRpmRegister() throws IOException {
        ModbusClientAdapter client = mock(ModbusClientAdapter.class);
        when(client.readRegisters(eq(SAUNA_PLC), anyList())).thenAnswer(invocation -> {
            java.util.List<Integer> requested = invocation.getArgument(1);
            assertTrue(requested.contains(0x0804));
            return Collections.singletonMap(0x0804, 900);
        });
        assertEquals(9.0, new DeviceService(client).getDashboardData().getRecupMaximumRpm());
    }

    @Test void unreadableOrInvalidModeNeverStartsAggregate() throws IOException {
        ModbusClientAdapter client = mock(ModbusClientAdapter.class);
        when(client.readRegister(SAUNA_PLC, 0x0302)).thenThrow(new IOException("offline"));
        assertThrows(IOException.class, () -> new DeviceService(client).enableRecuperator());
        doReturn(-1, 65536).when(client).readRegister(SAUNA_PLC, 0x0302);
        assertThrows(IOException.class, () -> new DeviceService(client).enableRecuperator());
        assertThrows(IOException.class, () -> new DeviceService(client).enableRecuperator());
        verify(client, never()).writeCommandOnce(anyString(), anyInt(), anyInt());
    }
    @Test void normalStartChecksModeBeforeNonRetriedCommand() throws IOException {
        ModbusClientAdapter client = mock(ModbusClientAdapter.class);
        when(client.readRegister(SAUNA_PLC, 0x0302)).thenReturn(0);
        new DeviceService(client).enableRecuperator();
        InOrder order = inOrder(client);
        order.verify(client).readRegister(SAUNA_PLC, 0x0302);
        order.verify(client).writeCommandOnce(SAUNA_PLC, 0x0300, 1);
        verify(client, never()).writeRegister(anyString(), anyInt(), anyInt());
    }

    @Test void pumpCommandUsesCurrentWorkState2AndPreservesUnknown() throws IOException {
        ModbusClientAdapter client = mock(ModbusClientAdapter.class);
        Map<Integer, Integer> values = new HashMap<>();
        when(client.readRegisters(eq(SAUNA_PLC), anyList())).thenAnswer(invocation -> {
            java.util.List<Integer> requested = invocation.getArgument(1);
            assertTrue(requested.contains(0x0306));
            assertFalse(requested.contains(0x009F));
            return new HashMap<>(values);
        });
        DeviceService service = new DeviceService(client);
        assertNull(service.getDashboardData().getPumpActive());
        values.put(0x0306, 0x0040);
        assertEquals(Boolean.TRUE, service.getDashboardData().getPumpActive());
        values.put(0x0306, 0x4000);
        assertEquals(Boolean.FALSE, service.getDashboardData().getPumpActive());
        values.put(0x0306, 0x8000);
        assertEquals(Boolean.FALSE, service.getDashboardData().getPumpActive());
        values.put(0x0306, 0);
        assertEquals(Boolean.FALSE, service.getDashboardData().getPumpActive());
        values.put(0x0306, -1);
        assertNull(service.getDashboardData().getPumpActive());
        verify(client, never()).writeRegister(anyString(), anyInt(), anyInt());
    }

    @Test
    void dashboardNeverPollsUnconfirmedLegacyTemperatureRegisters() throws IOException {
        ModbusClientAdapter client = mock(ModbusClientAdapter.class);
        when(client.readRegisters(eq(SAUNA_PLC), anyList())).thenAnswer(invocation -> {
            java.util.List<Integer> requested = invocation.getArgument(1);
            assertFalse(requested.contains(0x007E), "Legacy temperature tracking address must not be polled");
            assertFalse(requested.contains(0x00DD), "Legacy algorithm address must not be polled");
            assertTrue(requested.contains(ModbusRegisterMap.AGGREGATE_STATE));
            return Collections.emptyMap();
        });
        new DeviceService(client).getDashboardData();
        verify(client).readRegisters(eq(SAUNA_PLC), anyList());
        verify(client, never()).writeRegister(anyString(), anyInt(), anyInt());
    }


    @Test void dashboardDoesNotRequestLegacyRegistersAndScalesStartupPercent() throws IOException {
        ModbusClientAdapter client=mock(ModbusClientAdapter.class);
        when(client.readRegisters(eq(SAUNA_PLC),anyList())).thenAnswer(a->{
            java.util.List<Integer> requested=a.getArgument(1);
            int[] obsolete={0x24,0x47,0x48,0x70,0x7e,0x84,0x9f,0xa3,0xa4,0xa5,0xa6,0xa9,0xab,0xac,0xad,0xae,0xb3,0xb4,0xb5,0xb6,0xb8,0xd5,0xd6,0xdd,0x100,0x110,0x120,0x130,0x140,0x150,0x190,0x1a0,0x1b0,0x1c0,0x210,0x220,0x230};
            for(int address:obsolete)assertFalse(requested.contains(address),"obsolete "+Integer.toHexString(address));
            assertTrue(requested.contains(0x708));
            assertTrue(requested.contains(0x10c8));
            return Collections.singletonMap(0x708,30);
        });
        assertEquals(30.0,new DeviceService(client).getDashboardData().getTempSettings().getValveStartupPosition());
        new SettingsService(client).updateTempSettings(null,null,null,null,null,null,null,null,null,null,30.5,null,null);
        verify(client).writeRegister(SAUNA_PLC,0x708,31);
        assertEquals(30,SettingsService.encodeStartupPercent(30.0));
        assertEquals(31,SettingsService.encodeStartupPercent(30.5));
        assertThrows(IllegalArgumentException.class,()->SettingsService.encodeStartupPercent(300));
        assertThrows(IllegalArgumentException.class,()->SettingsService.encodeStartupPercent(Double.NaN));
    }

    @Test void carrierTemperatureUsesNewBt6RegisterAndPreservesMissingValues() throws IOException {
        ModbusClientAdapter client=mock(ModbusClientAdapter.class);
        Map<Integer,Integer> values=new HashMap<>();
        values.put(0x02C0,999);
        values.put(0x10C8,253);
        when(client.readRegisters(eq(SAUNA_PLC),anyList())).thenAnswer(a->{
            java.util.List<Integer> requested=a.getArgument(1);
            assertTrue(requested.contains(0x10C8));
            assertFalse(requested.contains(0x02C0));
            return new HashMap<>(values);
        });
        assertEquals(25.3,new DeviceService(client).getDashboardData().getRecupTempBt6());
        values.put(0x10C8,(-53)&65535);
        assertEquals(-5.3,new DeviceService(client).getDashboardData().getRecupTempBt6());
        values.put(0x10C8,0x8000);
        assertNull(new DeviceService(client).getDashboardData().getRecupTempBt6());
        values.remove(0x10C8);
        assertNull(new DeviceService(client).getDashboardData().getRecupTempBt6());
        verify(client,never()).writeRegister(anyString(),anyInt(),anyInt());
    }

    @Test void actualSupplyUsesSumAndNeverSubstitutesMissingFanWithZero() throws IOException {
        ModbusClientAdapter client=mock(ModbusClientAdapter.class);
        Map<Integer,Integer> values=new HashMap<>();
        int first=ModbusRegisterMap.FAN_SUPPLY_1+ModbusRegisterMap.FAN_AIRFLOW_OFFSET;
        int second=ModbusRegisterMap.FAN_SUPPLY_2+ModbusRegisterMap.FAN_AIRFLOW_OFFSET;
        int[] addresses={first,second,ModbusRegisterMap.AIRFLOW_REAL};
        float[] flows={3355.4f,3380.4f,123f};
        for(int i=0;i<addresses.length;i++) {
            int bits=Float.floatToIntBits(flows[i]);
            values.put(addresses[i],bits&65535);values.put(addresses[i]+1,bits>>>16);
        }
        when(client.readRegisters(eq(SAUNA_PLC),anyList())).thenAnswer(a->{
            java.util.List<Integer> requested=a.getArgument(1);
            assertFalse(requested.contains(ModbusRegisterMap.AIRFLOW_REAL));
            return new HashMap<>(values);
        });
        DashboardData data=new DeviceService(client).getDashboardData();
        assertEquals(6736.0,data.getAirflowReal());
        assertEquals(data.getVm1().doubleValue(),data.getAirflowReal());
        values.remove(second);
        assertNull(new DeviceService(client).getDashboardData().getAirflowReal());
        verify(client,never()).writeRegister(anyString(),anyInt(),anyInt());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void activatesUserSetWithFullStateOneAndAppliesRunningMode(int currentState) throws IOException {
        ModbusClientAdapter client = mock(ModbusClientAdapter.class);
        int base = ModbusRegisterMap.scheduleBase(3);
        when(client.readRegisters(eq(SAUNA_PLC), anyList()))
            .thenReturn(Collections.singletonMap(base, ModbusRegisterMap.SCHEDULE_USERSET_MODE));
        when(client.readRegister(SAUNA_PLC, base + 1)).thenReturn(currentState, 1);
        when(client.readRegister(SAUNA_PLC, ModbusRegisterMap.AGGREGATE_STATE)).thenReturn(64);

        new DeviceService(client).setScreenMode(true);

        InOrder ordered = inOrder(client);
        ordered.verify(client).writeRegister(SAUNA_PLC, base + 1, 1);
        ordered.verify(client).readRegister(SAUNA_PLC, base + 1);
        ordered.verify(client).writeRegister(SAUNA_PLC, ModbusRegisterMap.AGGREGATE_COMMAND, 6);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void deactivatesUserSetClearingSelectedAndWorkingBeforeModeChange(int currentState) throws IOException {
        ModbusClientAdapter client = mock(ModbusClientAdapter.class);
        int base = ModbusRegisterMap.scheduleBase(19);
        when(client.readRegisters(eq(SAUNA_PLC), anyList()))
            .thenReturn(Collections.singletonMap(base, ModbusRegisterMap.SCHEDULE_USERSET_MODE));
        when(client.readRegister(SAUNA_PLC, base + 1)).thenReturn(currentState, 0);

        new DeviceService(client).setScreenMode(false);

        InOrder ordered = inOrder(client);
        ordered.verify(client).writeRegister(SAUNA_PLC, base + 1, 0);
        ordered.verify(client).readRegister(SAUNA_PLC, base + 1);
        ordered.verify(client).writeRegister(SAUNA_PLC, ModbusRegisterMap.AGGREGATE_COMMAND, 6);
    }

    @Test
    void activatingScreenDoesNotStartStoppedUnit() throws IOException {
        ModbusClientAdapter client = mock(ModbusClientAdapter.class);
        int base = ModbusRegisterMap.scheduleBase(0);
        when(client.readRegisters(eq(SAUNA_PLC), anyList()))
            .thenReturn(Collections.singletonMap(base, ModbusRegisterMap.SCHEDULE_USERSET_MODE));
        when(client.readRegister(SAUNA_PLC, base + 1)).thenReturn(0, 1);
        when(client.readRegister(SAUNA_PLC, ModbusRegisterMap.AGGREGATE_STATE)).thenReturn(1);

        new DeviceService(client).setScreenMode(true);

        verify(client).writeRegister(SAUNA_PLC, base + 1, 1);
        verify(client, never()).writeRegister(eq(SAUNA_PLC),
            eq(ModbusRegisterMap.AGGREGATE_COMMAND), anyInt());
    }

    @Test
    void missingUserSetDoesNotWriteAnyRegister() throws IOException {
        ModbusClientAdapter client = mock(ModbusClientAdapter.class);
        when(client.readRegisters(eq(SAUNA_PLC), anyList())).thenReturn(Collections.emptyMap());

        assertThrows(IOException.class, () -> new DeviceService(client).setScreenMode(true));

        verify(client, never()).writeRegister(anyString(), anyInt(), anyInt());
    }

    @Test
    void dashboardReadsFloatPairsScalesValuesAndUsesNewModes() throws IOException {
        ModbusClientAdapter client = mock(ModbusClientAdapter.class);
        Map<Integer, Integer> registers = new HashMap<>();
        putFloat(registers, ModbusRegisterMap.PRESSURE_FAN_SUPPLY_1, 93.0f);
        putFloat(registers, ModbusRegisterMap.FAN_SUPPLY_1 + 8, 2934.5f);
        putFloat(registers, ModbusRegisterMap.FAN_SUPPLY_2 + 8, 2945.25f);
        putFloat(registers, ModbusRegisterMap.AIRFLOW_ECO, 750.0f);
        putFloat(registers, ModbusRegisterMap.AIRFLOW_NORMAL, 1500.0f);
        putFloat(registers, ModbusRegisterMap.AIRFLOW_INTENSIVE, 2000.0f);
        putFloat(registers, ModbusRegisterMap.AIRFLOW_MANUAL, 1750.0f);
        putFloat(registers, ModbusRegisterMap.AIRFLOW_TARGET, 6000.0f);
        putFloat(registers, ModbusRegisterMap.AIRFLOW_REAL, 5880.0f);
        registers.put(ModbusRegisterMap.AIRFLOW_IMBALANCE, (int) (short) -50);
        registers.put(ModbusRegisterMap.FAN_SUPPLY_1 + 10, 700);
        registers.put(ModbusRegisterMap.SENSOR_T1 + 8, 100);
        registers.put(ModbusRegisterMap.SENSOR_T3 + 8, 200);
        registers.put(ModbusRegisterMap.SENSOR_T5 + 8, 300);
        registers.put(ModbusRegisterMap.SENSOR_T1 + 9, 668);
        registers.put(ModbusRegisterMap.HEATER_ZONE_1 + 18, 190);
        registers.put(ModbusRegisterMap.HEATER_ZONE_1 + 7, 260);
        registers.put(ModbusRegisterMap.HEATER_ZONE_1 + 3, 0);
        registers.put(ModbusRegisterMap.HEATER_ZONE_2 + 3, 1);
        registers.put(ModbusRegisterMap.HEATER_ZONE_1 + 5, 10);
        registers.put(ModbusRegisterMap.HEATER_ZONE_2 + 5, 5);
        registers.put(ModbusRegisterMap.HEATER_ZONE_1 + 25, 215);
        registers.put(ModbusRegisterMap.HEATER_ZONE_2 + 25, 180);
        registers.put(ModbusRegisterMap.HEATER_ZONE_1 + 26, 195);
        registers.put(ModbusRegisterMap.HEATER_ZONE_2 + 26, 196);
        registers.put(ModbusRegisterMap.HEATER_ZONE_1 + 27, 20);
        registers.put(ModbusRegisterMap.HEATER_ZONE_1 + 28, 15);
        registers.put(ModbusRegisterMap.HEATER_ZONE_1 + 29, 21);
        registers.put(ModbusRegisterMap.HEATER_ZONE_1 + 30, 19);
        registers.put(ModbusRegisterMap.HEATER_ZONE_1 + 31, 14);
        registers.put(ModbusRegisterMap.HEATER_ZONE_1 + 32, 22);
        registers.put(ModbusConstants.REG_VALVE_HEAT_POS1, 250);
        registers.put(ModbusConstants.REG_VALVE_COOL_POS1, 1000);
        registers.put(ModbusConstants.REG_VALVE_OPEN_PROC1, 50);
        registers.put(ModbusConstants.REG_T_RAD_FZ_FAIL, 0xFFCE);
        registers.put(ModbusRegisterMap.VENTILATION_MODE, 2);
        registers.put(ModbusRegisterMap.AGGREGATE_STATE, 64);
        registers.put(ModbusRegisterMap.WORK_STATE, 3);
        registers.put(ModbusRegisterMap.RECUPERATOR_TARGET_RPM, 150);
        registers.put(ModbusRegisterMap.RECUPERATOR_CURRENT_RPM, 145);
        registers.put(ModbusRegisterMap.RECUPERATOR_MOTOR_RPM, 123);
        registers.put(ModbusRegisterMap.RECUPERATOR_MAX_RPM, 200);
        registers.put(ModbusRegisterMap.RECUPERATOR_TRACKING_INTERVAL, 120);
        registers.put(ModbusRegisterMap.RECUPERATOR_MIN_RPM_SETTING, 200);
        registers.put(ModbusRegisterMap.RECUPERATOR_MAX_RPM_SETTING, 9000);
        registers.put(ModbusRegisterMap.RECUPERATOR_RPM_STEP_SETTING, 200);
        registers.put(ModbusRegisterMap.scheduleBase(2), ModbusRegisterMap.SCHEDULE_USERSET_MODE);
        registers.put(ModbusRegisterMap.scheduleBase(2) + 1, 3);
        when(client.readRegisters(eq(SAUNA_PLC), anyList())).thenAnswer(invocation -> {
            java.util.List<Integer> requested = invocation.getArgument(1);
            assertTrue(requested.contains(ModbusRegisterMap.AIRFLOW_TARGET + 1));
            assertTrue(requested.contains(ModbusRegisterMap.PRESSURE_FAN_SUPPLY_1 + 1));
            assertTrue(requested.contains(ModbusRegisterMap.HEATER_ZONE_1 + 18));
            return registers;
        });

        DashboardData data = new DeviceService(client).getDashboardData();

        assertEquals(930.0, data.getFans().get(0).getPressure(), 0.001);
        assertEquals(Integer.valueOf(70), data.getFans().get(0).getSpeedPerc());
        assertEquals(Integer.valueOf(5880), data.getVm1());
        assertEquals(6300.0, data.getFans().get(2).getTargetAirFlow(), 0.001);
        assertEquals(10.0, data.getClimateSensors().get(0).getTemperature(), 0.001);
        assertEquals(66.8, data.getClimateSensors().get(0).getHumidity(), 0.001);
        assertEquals(19.0, data.getClimateSensors().get(10).getTemperature(), 0.001);
        assertEquals("26.0", data.getValves().get(0).getValue());
        assertEquals("50.0", data.getRecuperator().get(0).getValue());
        assertEquals("Норм", data.getVentilationModeName());
        assertEquals(6000.0, data.getAirflowTarget(), 0.001);
        assertEquals(5880.0, data.getAirflowReal(), 0.001);
        assertEquals(750.0, data.getVolumeSettings().getEcoAirflow(), 0.001);
        assertEquals(1500.0, data.getVolumeSettings().getNormalAirflow(), 0.001);
        assertEquals(2000.0, data.getVolumeSettings().getIntensiveAirflow(), 0.001);
        assertEquals(1750.0, data.getVolumeSettings().getManualAirflow(), 0.001);
        assertEquals(-5.0, data.getVolumeSettings().getBoostPercent(), 0.001);
        assertEquals("ОРП", data.getTemperatureControlModeZone1Name());
        assertEquals("ПВ", data.getTemperatureControlModeZone2Name());
        assertEquals(19.5, data.getTempSettings().getOutflowTarget(), 0.001);
        assertEquals(21.5, data.getTempSettings().getInflowTargetZone1(), 0.001);
        assertEquals(18.0, data.getTempSettings().getInflowTargetZone2(), 0.001);
        assertEquals(20.0, data.getTempSettings().getOrpZone1().getTargetT1(), 0.001);
        assertEquals(15.0, data.getTempSettings().getOrpZone1().getCurrentT1(), 0.001);
        assertEquals(Integer.valueOf(10), data.getTempSettings().getCtrlModeZone1());
        assertEquals("25.0", data.getDebugValveSensors().get(0).getValue());
        assertEquals("0.5", data.getDebugValveSensors().get(2).getValue());
        assertEquals(-5.0, data.getTempSettings().getRadTempFail(), 0.001);
        assertEquals(Boolean.TRUE, data.getScreenMode());
        assertEquals(1.5, data.getRecupTargetRpm(), 0.001);
        assertEquals(1.45, data.getRecupCurrentRpm(), 0.001);
        assertEquals(12.3, data.getRecupSpeedRpm(), 0.001);
        assertEquals(Integer.valueOf(120), data.getRecuperatorSettings().getTrackingIntervalRecup());
        assertEquals(2.0, data.getRecuperatorSettings().getTargetRpmMin(), 0.001);
        assertEquals(90.0, data.getRecuperatorSettings().getTargetRpmMax(), 0.001);
        assertEquals(2.0, data.getRecuperatorSettings().getTargetRpmStep(), 0.001);
        assertEquals(Boolean.TRUE, data.getDamperSupply());
        assertEquals(Boolean.TRUE, data.getDamperExhaust());
    }

    @Test void sensorStatesFollowRegistersAndClearOnRecovery() throws IOException {
        ModbusClientAdapter client = mock(ModbusClientAdapter.class);
        Map<Integer, Integer> values = new HashMap<>();
        when(client.readRegisters(eq(SAUNA_PLC), anyList())).thenAnswer(call -> {
            java.util.List<Integer> requested = call.getArgument(1);
            assertTrue(requested.contains(0x1000));
            assertTrue(requested.contains(0x1001));
            assertTrue(requested.contains(0x1240));
            assertTrue(requested.contains(0x1241));
            assertTrue(requested.contains(0x10C0));
            assertTrue(requested.contains(0x10C1));
            return values;
        });
        DeviceService service = new DeviceService(client);
        for (int mode = 0; mode <= 3; mode++) {
            values.put(0x1000, mode); values.put(0x1001, 123);
            DashboardData data = service.getDashboardData();
            assertEquals(19, data.getDeviceStates().size());
            assertEquals("TTR / BT6", data.getDeviceStates().get(18).getId());
            assertEquals(Integer.valueOf(mode), data.getDeviceStates().get(0).getAlarmMode());
            assertEquals(mode == 0 ? null : Integer.valueOf(123), data.getDeviceStates().get(0).getAlarmSeconds());
            assertNull(data.getDeviceStates().get(1).getAlarmMode());
        }
        values.put(0x1000, 32768);
        assertNull(service.getDashboardData().getDeviceStates().get(0).getAlarmMode());
        values.put(0x1000, 0);
        assertNull(service.getDashboardData().getDeviceStates().get(0).getAlarmSeconds());
        putFloat(values, ModbusRegisterMap.PRESSURE_FILTER_SUPPLY, 93f);
        assertEquals("930.0", service.getDashboardData().getWSensors().get(0).getValue());
        values.clear();
        assertNull(service.getDashboardData().getFans().get(0).getPressure());
        verify(client, never()).writeRegister(anyString(), anyInt(), anyInt());
    }

    private static void putFloat(Map<Integer, Integer> registers, int base, float value) {
        int bits = Float.floatToIntBits(value);
        registers.put(base, bits & 0xFFFF);
        registers.put(base + 1, (bits >>> 16) & 0xFFFF);
    }

    @ParameterizedTest
    @ValueSource(ints = {0x0042, 0x0044, 0x0046, 0x0244})
    void ventilationStateWinsOverStaleAlarmBits(int state) throws IOException {
        ModbusClientAdapter client = mock(ModbusClientAdapter.class);
        when(client.readRegisters(eq(SAUNA_PLC), anyList()))
            .thenReturn(Collections.singletonMap(ModbusRegisterMap.AGGREGATE_STATE, state));

        DashboardData data = new DeviceService(client).getDashboardData();

        assertEquals("Вентилирование", data.getRecuperatorStatus().getStateName());
        assertEquals(state, data.getRecuperatorStatus().getStateCode().intValue());
        assertTrue(data.getRecuperatorStatus().isEnabled());
        verify(client, never()).writeRegister(anyString(), anyInt(), anyInt());
    }
}
