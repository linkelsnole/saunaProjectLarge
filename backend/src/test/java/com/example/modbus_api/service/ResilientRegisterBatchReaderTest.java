package com.example.modbus_api.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResilientRegisterBatchReaderTest {

    @Test
    void excludesMissingRegisterAndKeepsAllAvailableValues() throws IOException {
        ResilientRegisterBatchReader reader = new ResilientRegisterBatchReader();
        AtomicInteger calls = new AtomicInteger();

        ResilientRegisterBatchReader.BatchReadOperation operation = registers -> {
            calls.incrementAndGet();
            if (registers.contains(0x0106)) {
                throw new IOException("Ошибка Bad Request (400): не найден регистр 0x106");
            }
            Map<Integer, Integer> values = new HashMap<>();
            for (Integer register : registers) {
                values.put(register, register + 1);
            }
            return values;
        };

        List<Integer> requested = Arrays.asList(0x0105, 0x0106, 0x0107);
        Map<Integer, Integer> first = reader.read("Sauna7000PLC", requested, operation);
        int callsAfterIsolation = calls.get();
        Map<Integer, Integer> second = reader.read("Sauna7000PLC", requested, operation);

        assertEquals(Integer.valueOf(0x0106), first.get(0x0105));
        assertEquals(Integer.valueOf(0x0108), first.get(0x0107));
        assertFalse(first.containsKey(0x0106));
        assertEquals(first, second);
        assertEquals(callsAfterIsolation + 1, calls.get());
    }

    @Test
    void isolatesMissingRegisterWhenErrorDoesNotContainItsAddress() throws IOException {
        ResilientRegisterBatchReader reader = new ResilientRegisterBatchReader();

        Map<Integer, Integer> values = reader.read(
            "Sauna7000PLC",
            Arrays.asList(10, 11, 12),
            registers -> {
                if (registers.contains(11)) {
                    throw new IOException("Программная ошибка: не найден регистр");
                }
                Map<Integer, Integer> result = new HashMap<>();
                for (Integer register : registers) {
                    result.put(register, register);
                }
                return result;
            });

        assertEquals(2, values.size());
        assertFalse(values.containsKey(11));
    }

    @Test
    void doesNotHideConnectionOrOtherBadRequestErrors() {
        ResilientRegisterBatchReader reader = new ResilientRegisterBatchReader();

        IOException exception = assertThrows(IOException.class, () -> reader.read(
            "Sauna7000PLC",
            Arrays.asList(1, 2),
            registers -> {
                throw new IOException("Connection refused");
            }));

        assertEquals("Connection refused", exception.getMessage());
    }
}
