package com.example.modbus_api.service;

import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import static com.example.modbus_api.service.ModbusConstants.SAUNA_PLC;

/** Owns the shared test parameter/result registers for the whole HTTP transaction. */
@Service
public class TestingService {
    private static final int MODE_COMMAND = 0x0300;
    private static final int MODE_STATE = 0x0302;
    private static final int MODE_MASK = 1 << 3;
    private static final int COMMAND = 0x0307;
    private static final int PARAMETER_1 = 0x0308;
    private static final int PARAMETER_2 = 0x0309;
    private static final int RESULT = 0x030C;
    private static final int FLOAT_RESULT = 0x030E;
    private final ModbusClientAdapter client;

    public TestingService(ModbusClientAdapter client) { this.client = client; }

    public synchronized Map<String, Object> status() throws IOException {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", enabled());
        return result;
    }

    public synchronized Map<String, Object> setMode(boolean desired) throws IOException {
        // Command 14 toggles; never retry an uncertain write or toggle an already matching state.
        if (enabled() != desired) {
            // Only a confirmed stopped aggregate may switch modes; unknown/fault states fail closed.
            int aggregateState = client.readRegister(SAUNA_PLC, ModbusRegisterMap.AGGREGATE_STATE);
            if (aggregateState != 1) {
                throw new IllegalStateException("Переключение тестового режима доступно только при выключенной установке. Дождитесь полной остановки.");
            }
            client.writeCommandOnce(SAUNA_PLC, MODE_COMMAND, 14);
        }
        return status();
    }

    private boolean enabled() throws IOException {
        return (client.readRegister(SAUNA_PLC, MODE_STATE) & MODE_MASK) != 0;
    }

    public synchronized Map<String, Object> execute(int command, Integer index, Integer value) throws IOException {
        validate(command, index, value);
        if (!enabled()) throw new IllegalStateException("Сначала включите режим тестирования");
        if (command == 5) client.writeRegister(SAUNA_PLC, PARAMETER_1, value);
        else if (command >= 7) client.writeRegister(SAUNA_PLC, PARAMETER_1, index);
        // Hand drawings take precedence: 12 controls dampers, 13 reads their position.
        if (command == 7 || command == 12 || command == 14) {
            client.writeRegister(SAUNA_PLC, PARAMETER_2, value);
        }
        client.writeCommandOnce(SAUNA_PLC, COMMAND, command);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("command", command);
        if (command == 6) {
            Map<Integer, Integer> words = client.readRegisters(SAUNA_PLC, Arrays.asList(FLOAT_RESULT, FLOAT_RESULT + 1));
            Double speed = ModbusRegisterMap.decodeFloat(words.get(FLOAT_RESULT), words.get(FLOAT_RESULT + 1));
            if (speed == null || !Double.isFinite(speed)) throw new IOException("Нет достоверного результата скорости");
            result.put("value", speed);
        } else if (command == 8 || command == 9 || command == 10 || command == 11 || command == 13) {
            int raw = client.readRegister(SAUNA_PLC, RESULT);
            result.put("value", command == 9 ? raw / 100.0 : raw);
        }
        return result;
    }

    private static void validate(int command, Integer index, Integer value) {
        if (command < 1 || command > 14) throw new IllegalArgumentException("Команда недоступна");
        if (command >= 7) range(index, 0, 3, "Выберите устройство");
        else if (index != null) throw new IllegalArgumentException("У этой команды нет индекса");
        if (command == 5) range(value, 0, 10000, "Скорость должна быть от 0 до 100%");
        else if (command == 7 || command == 14) range(value, 0, 100, "Значение должно быть от 0 до 100%");
        else if (command == 12) range(value, 0, 1, "Положение должно быть 0 или 1");
        else if (value != null) throw new IllegalArgumentException("У этой команды нет входного значения");
    }

    private static void range(Integer value, int min, int max, String message) {
        if (value == null || value < min || value > max) throw new IllegalArgumentException(message);
    }
}
