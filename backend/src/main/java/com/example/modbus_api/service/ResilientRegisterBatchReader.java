package com.example.modbus_api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Keeps batch reads usable when a controller firmware does not expose one of
 * the registers from the current map. Missing registers are skipped until the
 * application restarts; connection and authentication failures are never
 * swallowed.
 */
final class ResilientRegisterBatchReader {

    private static final Pattern REGISTER_IN_ERROR = Pattern.compile(
        "(?:регистр|register)\\s+0x([0-9a-f]+)", Pattern.CASE_INSENSITIVE);

    private final ConcurrentMap<String, Set<Integer>> unavailableByLogUnit =
        new ConcurrentHashMap<>();

    Map<Integer, Integer> read(
        String logUnit,
        List<Integer> registers,
        BatchReadOperation operation
    ) throws IOException {
        List<Integer> remaining = availableRegisters(logUnit, registers);
        if (remaining.isEmpty()) {
            return Collections.emptyMap();
        }

        while (!remaining.isEmpty()) {
            try {
                return operation.read(Collections.unmodifiableList(remaining));
            } catch (IOException exception) {
                if (!isMissingRegisterError(exception)) {
                    throw exception;
                }

                Integer missingRegister = extractMissingRegister(exception);
                if (missingRegister != null && remaining.remove(missingRegister)) {
                    markUnavailable(logUnit, missingRegister);
                    continue;
                }

                return readByHalves(logUnit, remaining, operation);
            }
        }

        return Collections.emptyMap();
    }

    private Map<Integer, Integer> readByHalves(
        String logUnit,
        List<Integer> registers,
        BatchReadOperation operation
    ) throws IOException {
        if (registers.size() == 1) {
            int missingRegister = registers.get(0);
            markUnavailable(logUnit, missingRegister);
            return Collections.emptyMap();
        }

        int middle = registers.size() / 2;
        Map<Integer, Integer> result = new HashMap<>();
        result.putAll(read(logUnit, new ArrayList<>(registers.subList(0, middle)), operation));
        result.putAll(read(logUnit, new ArrayList<>(registers.subList(middle, registers.size())), operation));
        return result;
    }

    private List<Integer> availableRegisters(String logUnit, List<Integer> registers) {
        Set<Integer> unavailable = unavailableByLogUnit.get(logUnit);
        LinkedHashSet<Integer> unique = new LinkedHashSet<>();
        for (Integer register : registers) {
            if (register != null && (unavailable == null || !unavailable.contains(register))) {
                unique.add(register);
            }
        }
        return new ArrayList<>(unique);
    }

    private void markUnavailable(String logUnit, int register) {
        Set<Integer> unavailable = unavailableByLogUnit.computeIfAbsent(
            logUnit, ignored -> ConcurrentHashMap.newKeySet());
        if (unavailable.add(register)) {
            System.err.printf(
                "Modbus API: регистр 0x%04X отсутствует в %s и исключён из пакетного чтения до перезапуска.%n",
                register,
                logUnit);
        }
    }

    private boolean isMissingRegisterError(Throwable throwable) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            String message = current.getMessage();
            if (message == null) {
                continue;
            }
            String normalized = message.toLowerCase(Locale.ROOT);
            if (normalized.contains("не найден регистр")
                || (normalized.contains("register") && normalized.contains("not found"))) {
                return true;
            }
        }
        return false;
    }

    private Integer extractMissingRegister(Throwable throwable) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            String message = current.getMessage();
            if (message == null) {
                continue;
            }
            Matcher matcher = REGISTER_IN_ERROR.matcher(message);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1), 16);
            }
        }
        return null;
    }

    @FunctionalInterface
    interface BatchReadOperation {
        Map<Integer, Integer> read(List<Integer> registers) throws IOException;
    }
}
