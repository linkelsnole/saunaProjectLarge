package com.example.modbus_api.service;

import com.example.modbus_api.dto.SystemEventDto;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Decodes the documented payload without replacing the outer journal metadata. */
final class DeviceEventPayload {
    private static final String PREFIX = "JSDeviceError|";
    private DeviceEventPayload() { }

    static void decode(SystemEventDto event) {
        if (event == null || event.getPackedValues() == null || !event.getPackedValues().startsWith(PREFIX)) return;
        try {
            JsonElement parsed = JsonParser.parseString(event.getPackedValues().substring(PREFIX.length()));
            if (!parsed.isJsonObject()) return;
            JsonObject object = parsed.getAsJsonObject();
            SystemEventDto.DeviceError error = new SystemEventDto.DeviceError();
            error.setDevName(text(object, "devName"));
            error.setDevEvtMode(integer(object, "devEvtMode"));
            error.setDevEvtRegName(text(object, "devEvtRegName"));
            error.setDevEvtRegValue(text(object, "devEvtRegValue"));
            error.setLevel(integer(object, "level"));
            error.setTitle(text(object, "title"));
            error.setMes(text(object, "mes"));
            error.setComment(text(object, "comment"));
            Long time = number(object, "timeStamp");
            error.setTimeStamp(time != null && time > 0 && time <= 8640000000000000L ? time : null);
            event.setDeviceError(error);
        } catch (RuntimeException ignored) {
            // A malformed payload must not hide other events or become a healthy device state.
        }
    }

    private static String text(JsonObject object, String name) {
        JsonElement value = object.get(name);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                ? value.getAsString() : null;
    }

    private static Long number(JsonObject object, String name) {
        JsonElement value = object.get(name);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) return null;
        try { return value.getAsBigDecimal().longValueExact(); }
        catch (ArithmeticException | NumberFormatException e) { return null; }
    }

    private static Integer integer(JsonObject object, String name) {
        Long value = number(object, name);
        return value != null && value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE ? value.intValue() : null;
    }
}
