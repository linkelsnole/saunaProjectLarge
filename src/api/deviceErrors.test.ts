import { describe, expect, it } from 'vitest';
import type { SystemEvent } from './typesApi';
import { deviceEventPhase, eventMessage, eventSource, filterEvents, prepareEvents, unresolvedDeviceErrors } from './systemEvents';
const event = (mode: number, time: number | null, device = 'EM1', logUnit = 1): SystemEvent => ({
    level: 5, devName: 'PLC', logUnit,
    deviceError: { devName: device, devEvtMode: mode, timeStamp: time, level: 1, mes: 'Connection event', devEvtRegName: '' },
});
describe('Structured device errors', () => {
    it('uses the payload message, device and severity', () => {
        const entry = event(1, 10);
        expect(eventMessage(entry)).toBe('Connection event');
        expect(eventSource(entry)).toBe('EM1 / PLC / #1');
        expect(prepareEvents([entry])[0].severity).toBe('WARNING');
    });
    it('keeps recovery separate from faults even with a severe level', () => {
        const recovered = event(2, 20); recovered.deviceError!.level = 4;
        expect(filterEvents([recovered], 'PROBLEMS', '', '')).toEqual([]);
        expect(filterEvents([recovered], 'RECOVERED', '', '')).toEqual([recovered]);
        expect(filterEvents([recovered], 'ALL', '', '')).toEqual([recovered]);
    });
    it('closes only the matching source and device interval regardless of input order', () => {
        const start = event(1, 10); const recovery = event(2, 20);
        const otherDevice = event(1, 10, 'EM2'); const otherSource = event(1, 10, 'EM1', 2);
        expect(unresolvedDeviceErrors([recovery, otherDevice, start, otherSource])).toEqual([otherDevice, otherSource]);
        expect(unresolvedDeviceErrors([start, recovery])).toEqual([]);
        expect(unresolvedDeviceErrors([recovery, event(1, 30)])).toEqual([event(1, 30)]);
    });
    it('does not treat isolated or brief failures as ongoing alarms or clear an existing interval', () => {
        const start = event(1, 10);
        expect(unresolvedDeviceErrors([event(0, 20), event(3, 30)])).toEqual([]);
        expect(unresolvedDeviceErrors([start, event(3, 30)])).toEqual([start]);
        expect(filterEvents([event(0, 20), event(3, 30)], 'PROBLEMS', '', '')).toHaveLength(2);
    });
    it('does not invent current status from missing dates or invalid events', () => {
        expect(unresolvedDeviceErrors([event(1, null)])).toEqual([]);
        expect(unresolvedDeviceErrors([event(1, 10), event(2, null)])).toEqual([]);
        expect(unresolvedDeviceErrors([{ ...event(1, 10), valid: false }])).toEqual([]);
        expect(unresolvedDeviceErrors([{ ...event(1, 10), endTime: { timeInMS: 20 } }])).toEqual([]);
    });
    it('equal-time recovery wins and other registers remain independent', () => {
        const start = event(1, 10); const recovery = event(2, 10);
        expect(unresolvedDeviceErrors([start, recovery])).toEqual([]);
        expect(unresolvedDeviceErrors([recovery, start])).toEqual([]);
        recovery.deviceError!.devEvtRegName = 'Other';
        expect(unresolvedDeviceErrors([start, recovery])).toEqual([start]);
    });
    it('searches structured register names and values and preserves unknown modes', () => {
        const entry = event(99, 20); entry.deviceError!.devEvtRegName = 'Speed'; entry.deviceError!.devEvtRegValue = '123';
        expect(deviceEventPhase(entry)).toBe('unknown');
        expect(filterEvents([entry], 'ALL', '', '123')).toEqual([entry]);
        expect(filterEvents([entry], 'ALL', '', 'speed')).toEqual([entry]);
    });
});
