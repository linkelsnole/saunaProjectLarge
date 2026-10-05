package com.example.modbus_api.service;

import java.io.IOException;
import java.util.*;
import java.math.BigDecimal;
import java.util.function.Function;
import static com.example.modbus_api.service.ModbusConstants.*;

/** Describes requested fields using one pre-write snapshot. No controller writes. */
public final class SettingChanges {
    private final ModbusClientAdapter client;
    private final List<Field> fields = new ArrayList<>();

    public SettingChanges(ModbusClientAdapter client) { this.client = client; }

    public void number(int register, double scale, String label, String unit, Number value) {
        if (value != null) fields.add(new Field(register, scale, label, unit, value, false));
    }

    public void floating(int register, String label, String unit, Number value) {
        if (value != null) fields.add(new Field(register, 1, label, unit, value, true));
    }

    public void formatted(int register, String label, Number value, Function<Double, String> formatter) {
        if (value != null) {
            Field field = new Field(register, 1, label, "", value, false);
            field.formatter = formatter;
            fields.add(field);
        }
    }

    public String describe() {
        if (fields.isEmpty()) return null;
        Set<Integer> registers = new LinkedHashSet<>();
        for (Field field : fields) {
            registers.add(field.register);
            if (field.floating) registers.add(field.register + 1);
        }
        Map<Integer, Integer> old;
        try {
            old = client.readRegisters(SAUNA_PLC, new ArrayList<>(registers));
        } catch (IOException e) {
            old = Collections.emptyMap();
        }
        if (old == null) old = Collections.emptyMap();
        List<String> parts = new ArrayList<>();
        for (Field field : fields) {
            Double before = field.decode(old);
            parts.add(field.label + ": " + (before == null ? "неизвестно" : field.formatter.apply(before))
                + " → " + field.formatter.apply(field.value.doubleValue()) + " " + field.unit);
        }
        return String.join("; ", parts);
    }

    private static String format(double value) {
        return Double.isFinite(value) ? BigDecimal.valueOf(value).stripTrailingZeros().toPlainString() : "неизвестно";
    }

    private static boolean word(Integer value) { return value != null && value >= 0 && value <= 65535; }

    private static final class Field {
        final int register;
        final double scale;
        final String label, unit;
        final Number value;
        final boolean floating;
        Function<Double, String> formatter = SettingChanges::format;
        Field(int register, double scale, String label, String unit, Number value, boolean floating) {
            this.register = register; this.scale = scale; this.label = label;
            this.unit = unit; this.value = value; this.floating = floating;
        }
        Double decode(Map<Integer, Integer> values) {
            Integer raw = values.get(register);
            if (!word(raw)) return null;
            if (floating) {
                Integer high = values.get(register + 1);
                if (!word(high)) return null;
                float result = Float.intBitsToFloat((high << 16) | raw);
                return Float.isFinite(result) ? Double.valueOf(Float.toString(result)) : null;
            }
            if (raw == INVALID_VALUE) return null;
            return ((short) raw.intValue()) / scale;
        }
    }
}
