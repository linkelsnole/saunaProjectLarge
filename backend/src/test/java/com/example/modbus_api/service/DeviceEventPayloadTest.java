package com.example.modbus_api.service;

import com.example.modbus_api.dto.SystemEventDto;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DeviceEventPayloadTest {
    private SystemEventDto decode(String payload) {
        SystemEventDto event = new SystemEventDto();
        event.setPackedValues(payload);
        event.setDevName("Controller");
        DeviceEventPayload.decode(event);
        return event;
    }
    @Test void decodesActualWireEnvelopeWithoutReplacingSourceOrRawPayload() {
        String payload = "JSDeviceError|{\"devName\":\"EM1\",\"devEvtMode\":2,\"devEvtRegName\":\"Status\",\"devEvtRegValue\":\"0\",\"level\":1,\"mes\":\"restored\\nnow\",\"timeStamp\":1791010008473}";
        SystemEventDto event = decode(payload);
        assertEquals("Controller", event.getDevName());
        assertEquals(payload, event.getPackedValues());
        assertEquals("EM1", event.getDeviceError().getDevName());
        assertEquals(Integer.valueOf(2), event.getDeviceError().getDevEvtMode());
        assertEquals("0", event.getDeviceError().getDevEvtRegValue());
        assertEquals("restored\nnow", event.getDeviceError().getMes());
        assertEquals(Long.valueOf(1791010008473L), event.getDeviceError().getTimeStamp());
    }
    @Test void malformedOrOtherPayloadsDoNotBreakJournal() {
        for (String value : new String[]{null, "", "JSBase|{}", "JSDeviceError|broken", "JSDeviceError|null", "JSDeviceError|[]"}) {
            assertNull(decode(value).getDeviceError());
        }
    }
    @Test void wrongTypesAndFractionalModesNeverBecomeRecovery() {
        SystemEventDto.DeviceError error = decode("JSDeviceError|{\"devName\":{},\"devEvtMode\":2.5,\"timeStamp\":-1,\"level\":\"3\"}").getDeviceError();
        assertNull(error.getDevName()); assertNull(error.getDevEvtMode());
        assertNull(error.getTimeStamp()); assertNull(error.getLevel());
        assertEquals(Integer.valueOf(99), decode("JSDeviceError|{\"devEvtMode\":99}").getDeviceError().getDevEvtMode());
    }
}
