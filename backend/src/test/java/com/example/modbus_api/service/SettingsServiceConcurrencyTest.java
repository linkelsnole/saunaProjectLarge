package com.example.modbus_api.service;

import com.example.modbus_api.dto.UpdateVolumeSettingsRequest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class SettingsServiceConcurrencyTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void wholeSettingsWritesCannotInterleaveAcrossClientsAndFailureReleasesLock(boolean failFirst) throws Exception {
        ModbusClientAdapter client = mock(ModbusClientAdapter.class);
        SettingsService service = new SettingsService(client);
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1), secondStarted = new CountDownLatch(1);
        AtomicBoolean first = new AtomicBoolean(true);
        doAnswer(call -> {
            if (first.getAndSet(false)) {
                entered.countDown();
                if (!release.await(5, TimeUnit.SECONDS)) throw new IOException("test timeout");
                if (failFirst) throw new IOException("controller unavailable");
            }
            return null;
        }).when(client).writeRegister(anyString(), anyInt(), anyInt());
        ExecutorService threads = Executors.newFixedThreadPool(2);
        try {
            UpdateVolumeSettingsRequest a = new UpdateVolumeSettingsRequest(); a.setEcoAirflow(15000.0);
            Future<?> one = threads.submit(() -> {if (failFirst) assertThrows(IOException.class, () -> service.updateVolumeSettings(a));
                else assertDoesNotThrow(() -> service.updateVolumeSettings(a));});
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            Future<?> two = threads.submit(() -> {secondStarted.countDown();service.updateRecuperatorSettings(null, null, null, 5);return null;});
            assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> two.get(150, TimeUnit.MILLISECONDS));
            verify(client, times(1)).writeRegister(anyString(), anyInt(), anyInt());
            release.countDown();one.get(5, TimeUnit.SECONDS);two.get(5, TimeUnit.SECONDS);
            verify(client, atLeast(3)).writeRegister(anyString(), anyInt(), anyInt());
        } finally {release.countDown();threads.shutdownNow();}
    }
}
