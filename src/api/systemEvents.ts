import type { SystemEvent } from './typesApi';
import { fetchWithAuth } from './authApi';
export type EventSeverity = 'LOG' | 'INFO' | 'WARNING' | 'ERROR' | 'FAILURE' | 'CRASH' | 'UNKNOWN';
const levels: Record<number, EventSeverity> = { 0: 'INFO', 1: 'WARNING', 2: 'ERROR', 3: 'FAILURE', 4: 'CRASH', 5: 'LOG' };
export function eventSeverity(level: unknown): EventSeverity {
    return typeof level === 'number' && Number.isInteger(level) ? levels[level] ?? 'UNKNOWN' : 'UNKNOWN';
}
export function eventTime(value: unknown): number | null {
    return typeof value === 'number' && Number.isFinite(value) && value > 0 && value <= 8640000000000000 ? value : null;
}
export const eventMessage = (event: SystemEvent) => event.comment?.trim() || event.title?.trim() || '';
export const isProblem = (event: SystemEvent) => ['WARNING', 'ERROR', 'FAILURE', 'CRASH', 'UNKNOWN'].includes(eventSeverity(event.level));
export function eventSource(event: SystemEvent): string {
    return [event.devName, event.equipName, event.logUnit != null ? `#${event.logUnit}` : null].filter(Boolean).join(' / ');
}
export function prepareEvents(events: SystemEvent[]) {
    const occurrences = new Map<string, number>();
    return events.filter(event => event && event.valid !== false).map(event => {
        const identity = event.oid && event.oid !== '0' ? `oid:${event.oid}` : JSON.stringify(event);
        const n = occurrences.get(identity) ?? 0;
        occurrences.set(identity, n + 1);
        return { ...event, key: `${identity}:${n}`, severity: eventSeverity(event.level) };
    }).sort((a, b) => (eventTime(b.arrivalTime?.timeInMS) ?? 0) - (eventTime(a.arrivalTime?.timeInMS) ?? 0));
}
export function filterEvents<T extends SystemEvent>(events: T[], filter: string, source: string, search: string): T[] {
    const needle = search.trim().toLocaleLowerCase();
    return events.filter(e => (filter === 'ALL' || (filter === 'PROBLEMS' ? isProblem(e) : eventSeverity(e.level) === filter))
        && (!source || eventSource(e) === source)
        && (!needle || [e.title, e.comment, e.devName, e.equipName, e.regNum, e.regNum != null ? `0x${e.regNum.toString(16)}` : '', e.packedValues].join(' ').toLocaleLowerCase().includes(needle)));
}
export type Diagnostics = {
    logName: string;
    readAt: number;
    state: number | null;
    workState: number | null;
    modbusErrors: number | null;
    errorRestarts: number | null;
    failureRestarts: number | null;
};
export function diagnosticFlags(data: Diagnostics): string[] {
    const result: string[] = [];
    const add = (word: number | null, mask: number, key: string) => { if (word != null && (word & mask) !== 0)
        result.push(key); };
    add(data.state, 1 << 8, 'stateError');
    add(data.state, 1 << 9, 'stateFailure');
    add(data.state, 1 << 10, 'stateFreeze');
    add(data.state, 1 << 11, 'stateCrash');
    add(data.workState, 1 << 6, 'eventFreeze');
    add(data.workState, 1 << 7, 'eventError');
    add(data.workState, 1 << 8, 'eventFailure');
    add(data.workState, 1 << 14, 'eventModbus');
    add(data.workState, 1 << 15, 'eventCrash');
    return result;
}
export async function readSystemEvents(minutes: number, signal?: AbortSignal): Promise<SystemEvent[]> {
    const response = await fetchWithAuth(`/api/v1/devices/events?minutes=${minutes}`, { signal });
    if (!response.ok)
        throw new Error(`HTTP ${response.status}`);
    const result: unknown = await response.json();
    if (!Array.isArray(result) || result.some(e => e === null || typeof e !== 'object' || Array.isArray(e)))
        throw new Error('Invalid event response');
    return result as SystemEvent[];
}
export async function readDiagnostics(signal?: AbortSignal): Promise<Diagnostics> {
    const response = await fetchWithAuth('/api/v1/devices/events/status', { signal });
    if (!response.ok)
        throw new Error(`HTTP ${response.status}`);
    return response.json();
}
