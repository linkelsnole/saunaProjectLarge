package com.example.modbus_api.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import static com.example.modbus_api.service.ModbusConstants.SAUNA_PLC;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TestingServiceTest {
    private final ModbusClientAdapter client = mock(ModbusClientAdapter.class);
    private final TestingService service = new TestingService(client);
    private void active() throws IOException { when(client.readRegister(SAUNA_PLC, 0x0302)).thenReturn(8); }

    @Test void parametersAreWrittenBeforeCommandAndScaledResultReadAfterIt() throws IOException {
        active();
        service.execute(7, 2, 45);
        InOrder order = inOrder(client);
        order.verify(client).readRegister(SAUNA_PLC, 0x0302);
        order.verify(client).writeRegister(SAUNA_PLC, 0x0308, 2);
        order.verify(client).writeRegister(SAUNA_PLC, 0x0309, 45);
        order.verify(client).writeCommandOnce(SAUNA_PLC, 0x0307, 7);
        when(client.readRegister(SAUNA_PLC, 0x030C)).thenReturn(4567);
        assertEquals(45.67, service.execute(9, 2, null).get("value"));
        order.verify(client).readRegister(SAUNA_PLC, 0x0302);
        order.verify(client).writeRegister(SAUNA_PLC, 0x0308, 2);
        order.verify(client).writeCommandOnce(SAUNA_PLC, 0x0307, 9);
        order.verify(client).readRegister(SAUNA_PLC, 0x030C);
    }
    @Test void damperCommandNumbersFollowDrawing() throws IOException {
        active(); service.execute(12, 1, 0); service.execute(13, 1, null);
        verify(client).writeRegister(SAUNA_PLC, 0x0309, 0);
        verify(client).writeCommandOnce(SAUNA_PLC, 0x0307, 12);
        verify(client).writeCommandOnce(SAUNA_PLC, 0x0307, 13);
        verify(client).readRegister(SAUNA_PLC, 0x030C);
    }
    @Test void floatSpeedUsesBothWordsAt030e() throws IOException {
        active(); Map<Integer, Integer> words = new HashMap<>();
        words.put(0x030E, ModbusRegisterMap.floatLowWord(123.5f));
        words.put(0x030F, ModbusRegisterMap.floatHighWord(123.5f));
        when(client.readRegisters(eq(SAUNA_PLC), anyList())).thenReturn(words);
        assertEquals(123.5, service.execute(6, null, null).get("value"));
    }
    @ParameterizedTest @ValueSource(ints = {1, 2, 3, 4})
    void noParametersMeansNoParameterWrites(int command) throws IOException {
        active(); service.execute(command, null, null);
        verify(client, never()).writeRegister(anyString(), anyInt(), anyInt());
        verify(client).writeCommandOnce(SAUNA_PLC, 0x0307, command);
    }
    @Test void inactiveModeAndInvalidInputsNeverWrite() throws IOException {
        assertThrows(IllegalStateException.class, () -> service.execute(2, null, null));
        assertThrows(IllegalArgumentException.class, () -> service.execute(15, 0, null));
        assertThrows(IllegalArgumentException.class, () -> service.execute(7, 4, 50));
        assertThrows(IllegalArgumentException.class, () -> service.execute(5, null, 10001));
        assertThrows(IllegalArgumentException.class, () -> service.execute(14, 0, null));
        assertThrows(IllegalArgumentException.class, () -> service.execute(12, 0, 2));
        verify(client, never()).writeCommandOnce(anyString(), anyInt(), anyInt());
        verify(client, never()).writeRegister(anyString(), anyInt(), anyInt());
    }
    @Test void failedParameterWriteDoesNotExecuteCommand() throws IOException {
        active(); doThrow(new IOException("offline")).when(client).writeRegister(SAUNA_PLC, 0x0309, 50);
        assertThrows(IOException.class, () -> service.execute(7, 1, 50));
        verify(client, never()).writeCommandOnce(anyString(), anyInt(), anyInt());
    }
    @Test void toggleOnlyWhenNeededAndUseBitThreeOf0302() throws IOException {
        when(client.readRegister(SAUNA_PLC, 0x0302)).thenReturn(0, 8, 8, 8);
        when(client.readRegister(SAUNA_PLC, 0x0301)).thenReturn(1);
        assertEquals(true, service.setMode(true).get("enabled"));
        service.setMode(true);
        verify(client, times(1)).writeCommandOnce(SAUNA_PLC, 0x0300, 14);
    }
    @ParameterizedTest @ValueSource(ints = {0, 2, 4, 8, 16, 32, 64, 65, 128, 256, 512, 1024, 2048, 65535, -1})
    void toggleIsBlockedUnlessAggregateIsConfirmedOff(int state) throws IOException {
        when(client.readRegister(SAUNA_PLC, 0x0301)).thenReturn(state);
        assertThrows(IllegalStateException.class, () -> service.setMode(true));
        active();
        assertThrows(IllegalStateException.class, () -> service.setMode(false));
        verify(client, never()).writeCommandOnce(anyString(), anyInt(), anyInt());
    }
    @Test void unreadableAggregateNeverSendsToggle() throws IOException {
        when(client.readRegister(SAUNA_PLC, 0x0301)).thenThrow(new IOException("offline"));
        assertThrows(IOException.class, () -> service.setMode(true));
        verify(client, never()).writeCommandOnce(anyString(), anyInt(), anyInt());
    }
    @Test void stoppedAggregateMayLeaveTestingWithStateCheckedBeforeCommand() throws IOException {
        when(client.readRegister(SAUNA_PLC, 0x0302)).thenReturn(8, 0);
        when(client.readRegister(SAUNA_PLC, 0x0301)).thenReturn(1);
        assertEquals(false, service.setMode(false).get("enabled"));
        InOrder order = inOrder(client);
        order.verify(client).readRegister(SAUNA_PLC, 0x0302);
        order.verify(client).readRegister(SAUNA_PLC, 0x0301);
        order.verify(client).writeCommandOnce(SAUNA_PLC, 0x0300, 14);
        order.verify(client).readRegister(SAUNA_PLC, 0x0302);
    }
    @Test void alreadyMatchingModeDoesNotNeedToggleOrAggregateRead() throws IOException {
        active();
        service.setMode(true);
        verify(client, never()).readRegister(SAUNA_PLC, 0x0301);
        verify(client, never()).writeCommandOnce(anyString(), anyInt(), anyInt());
    }
    @Test void uncertainCommandFailureIsNotRepeatedAndResultNotRead() throws IOException {
        active(); doThrow(new IOException("unknown outcome")).when(client).writeCommandOnce(SAUNA_PLC, 0x0307, 8);
        assertThrows(IOException.class, () -> service.execute(8, 0, null));
        verify(client, times(1)).writeCommandOnce(SAUNA_PLC, 0x0307, 8);
        verify(client, never()).readRegister(SAUNA_PLC, 0x030C);
    }
    @Test void secondRequestCannotOverwriteParametersBeforeFirstResultIsRead() throws Exception {
        active();
        java.util.concurrent.CountDownLatch reading = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch secondStarted = new java.util.concurrent.CountDownLatch(1);
        when(client.readRegister(SAUNA_PLC, 0x030C)).thenAnswer(invocation -> {
            reading.countDown();
            if (!release.await(2, java.util.concurrent.TimeUnit.SECONDS)) throw new IOException("test timed out");
            return 1200;
        });
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Future<?> first = executor.submit(() -> {
                try { service.execute(8, 0, null); } catch (IOException e) { throw new RuntimeException(e); }
            });
            assertTrue(reading.await(2, java.util.concurrent.TimeUnit.SECONDS));
            java.util.concurrent.Future<?> second = executor.submit(() -> {
                secondStarted.countDown();
                try { service.execute(7, 1, 50); } catch (IOException e) { throw new RuntimeException(e); }
            });
            assertTrue(secondStarted.await(2, java.util.concurrent.TimeUnit.SECONDS));
            assertThrows(java.util.concurrent.TimeoutException.class, () -> second.get(100, java.util.concurrent.TimeUnit.MILLISECONDS));
            verify(client, never()).writeRegister(SAUNA_PLC, 0x0308, 1);
            release.countDown();
            first.get(2, java.util.concurrent.TimeUnit.SECONDS);
            second.get(2, java.util.concurrent.TimeUnit.SECONDS);
            verify(client).writeRegister(SAUNA_PLC, 0x0308, 1);
        } finally { release.countDown(); executor.shutdownNow(); }
    }
}
