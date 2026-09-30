import { afterEach, describe, expect, it, vi } from 'vitest';
import { diagnosticFlags, eventMessage, eventSeverity, eventTime, filterEvents, prepareEvents, readSystemEvents, type Diagnostics } from './systemEvents';
import { fetchWithAuth } from './authApi';
vi.mock('./authApi', () => ({ fetchWithAuth: vi.fn() }));
afterEach(() => vi.resetAllMocks());
describe('Air2 event semantics', () => {
    it('uses all six documented levels, with LOG 5 distinct from a crash', () => { expect([0, 1, 2, 3, 4, 5].map(eventSeverity)).toEqual(['INFO', 'WARNING', 'ERROR', 'FAILURE', 'CRASH', 'LOG']); });
    it('never infers severity from words, type, or reaction', () => { const items = prepareEvents([{ level: 0, type: 6, comment: 'авария error fatal', value: 4 }, { level: 3, comment: 'Норма, аварий 0', value: 0 }, { level: 5, comment: 'Error counter = 0' }]); expect(items.map(e => e.severity)).toEqual(['INFO', 'FAILURE', 'LOG']); });
    it('keeps unknown levels unknown instead of normal or critical', () => { for (const level of [null, undefined, 6, -1, 1.5, '5', NaN])
        expect(eventSeverity(level)).toBe('UNKNOWN'); });
    it('preserves diagnostic text including timeout number and multiple lines', () => { const comment = 'AirPLCONCycle warning: тайм-аут 23\nerror: unit=6 byteCount=4'; expect(eventMessage({ comment, title: 'other' })).toBe(comment); });
    it('does not fabricate a timestamp for missing/zero/invalid dates', () => { for (const value of [null, undefined, 0, NaN, Infinity, -1, 8640000000000001])
        expect(eventTime(value)).toBeNull(); expect(eventTime(1790000000000)).toBe(1790000000000); });
    it('sorts by arrival, puts missing times last and does not mutate input', () => { const input = [{ oid: 'a' }, { oid: 'b', arrivalTime: { timeInMS: 100 } }, { oid: 'c', arrivalTime: { timeInMS: 200 } }]; expect(prepareEvents(input).map(e => e.oid)).toEqual(['c', 'b', 'a']); expect(input[0].oid).toBe('a'); });
    it('keeps IDs exact and stable when a newer event is inserted', () => { const e = { oid: '9007199254740993', level: 5 }; expect(prepareEvents([e])[0].key).toBe(prepareEvents([{ oid: 'new', arrivalTime: { timeInMS: 5 } }, e])[1].key); });
    it('preserves repeated events with distinct keys and excludes invalid entries', () => { const e = { level: 1, comment: 'same' }; const list = prepareEvents([e, e, { valid: false, level: 4 }]); expect(list).toHaveLength(2); expect(list[0].key).not.toBe(list[1].key); });
    it('filters level, source and text without silently mixing sources', () => { const events = [{ level: 5, devName: 'HomePLC', comment: 'debug' }, { level: 2, devName: 'HomePLC', equipName: 'T1', regNum: 0x900, comment: 'тайм-аут' }, { level: 3, devName: 'OtherPLC', comment: 'авария' }]; expect(filterEvents(events, 'PROBLEMS', '', '')).toHaveLength(2); expect(filterEvents(events, 'ERROR', 'HomePLC / T1', '0x900')).toEqual([events[1]]); expect(filterEvents(events, 'LOG', '', 'debug')).toEqual([events[0]]); });
    it('decodes work-state bit 15 as crash, not missing data', () => { const data: Diagnostics = { logName: 'HomePLC', readAt: 1, state: 0, workState: 0x8000, modbusErrors: 0, errorRestarts: 0, failureRestarts: 0 }; expect(diagnosticFlags(data)).toEqual(['eventCrash']); expect(diagnosticFlags({ ...data, state: 1 << 9, workState: 1 << 14 })).toEqual(['stateFailure', 'eventModbus']); });
    it('accepts a genuinely empty journal', async () => { vi.mocked(fetchWithAuth).mockResolvedValue(new Response('[]')); expect(await readSystemEvents(60)).toEqual([]); });
    it('does not turn null malformed or failed responses into an empty journal', async () => { for (const body of ['null', '{}', '[null]']) {
        vi.mocked(fetchWithAuth).mockResolvedValue(new Response(body));
        await expect(readSystemEvents(60)).rejects.toThrow();
    } vi.mocked(fetchWithAuth).mockResolvedValue(new Response('[]', { status: 502 })); await expect(readSystemEvents(60)).rejects.toThrow('502'); });
    it('passes cancellation through so old periods cannot replace the current query', async () => { const controller = new AbortController(); vi.mocked(fetchWithAuth).mockResolvedValue(new Response('[]')); await readSystemEvents(1440, controller.signal); expect(fetchWithAuth).toHaveBeenCalledWith('/api/v1/devices/events?minutes=1440', { signal: controller.signal }); });
});
