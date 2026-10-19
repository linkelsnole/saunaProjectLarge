import { displayDeviceText } from '@/schema/deviceIdentity';
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
export type DeviceEventPhase = 'single' | 'started' | 'recovered' | 'brief' | 'unknown';
export function deviceEventPhase(event: SystemEvent): DeviceEventPhase | null {
    if (!event.deviceError) return null;
    return ({ 0: 'single', 1: 'started', 2: 'recovered', 3: 'brief' } as const)[event.deviceError.devEvtMode as 0 | 1 | 2 | 3] ?? 'unknown';
}
export const severityOfEvent = (event: SystemEvent) => eventSeverity(event.deviceError?.level ?? event.level);
export const eventTimestamp = (event: SystemEvent) => eventTime(event.deviceError?.timeStamp) ?? eventTime(event.arrivalTime?.timeInMS);
export const eventMessage = (event: SystemEvent) => displayDeviceText(event.deviceError?.mes?.trim() || event.deviceError?.comment?.trim() || event.comment?.trim() || event.deviceError?.title?.trim() || event.title?.trim() || '');
export const isProblem = (event: SystemEvent) => deviceEventPhase(event) !== 'recovered' && (event.deviceError != null || ['WARNING', 'ERROR', 'FAILURE', 'CRASH', 'UNKNOWN'].includes(severityOfEvent(event)));
export function eventSource(event: SystemEvent): string {
    return [...new Set([event.deviceError?.devName, event.devName, event.equipName, event.logUnit != null ? `#${event.logUnit}` : null].filter(Boolean))].join(' / ');
}
/** Only paired start/recovery events establish an interval; isolated faults cannot establish current state. */
export function unresolvedDeviceErrors(events: SystemEvent[]): SystemEvent[] {
    const latest = new Map<string, SystemEvent>();
    const ambiguous = new Set<string>();
    for (const event of events) {
        const error = event.deviceError;
        if (event.valid === false || !error?.devName?.trim()) continue;
        const phase = deviceEventPhase(event);
        if (phase !== 'started' && phase !== 'recovered') continue;
        const key = JSON.stringify([event.logUnit ?? null, event.devName ?? null, error.devName, error.devEvtRegName || '']);
        const time = eventTimestamp(event);
        if (time == null) { ambiguous.add(key); continue; }
        const previous = latest.get(key);
        const previousTime = previous ? eventTimestamp(previous)! : -1;
        // An equal-time recovery wins regardless of server list ordering.
        if (time > previousTime || (time === previousTime && phase === 'recovered')) latest.set(key, event);
    }
    return [...latest.entries()].filter(([key, event]) => !ambiguous.has(key) && deviceEventPhase(event) === 'started'
        && eventTime(event.endTime?.timeInMS) == null).map(([, event]) => event)
        .sort((a, b) => eventTimestamp(b)! - eventTimestamp(a)!);
}
export function prepareEvents(events: SystemEvent[]) {
    const occurrences = new Map<string, number>();
    return events.filter(event => event && event.valid !== false).map(event => {
        const identity = event.oid && event.oid !== '0' ? `oid:${event.oid}` : JSON.stringify(event);
        const n = occurrences.get(identity) ?? 0;
        occurrences.set(identity, n + 1);
        return { ...event, key: `${identity}:${n}`, severity: severityOfEvent(event) };
    }).sort((a, b) => (eventTimestamp(b) ?? 0) - (eventTimestamp(a) ?? 0));
}
export function filterEvents<T extends SystemEvent>(events: T[], filter: string, source: string, search: string): T[] {
    const needle = search.trim().toLocaleLowerCase();
    return events.filter(e => (filter === 'ALL' || (filter === 'PROBLEMS' ? isProblem(e) : filter === 'RECOVERED' ? deviceEventPhase(e) === 'recovered' : severityOfEvent(e) === filter))
        && (!source || eventSource(e) === source)
        && (!needle || [e.title, e.comment, e.deviceError?.mes, e.deviceError?.devName, e.deviceError?.devEvtRegName, e.deviceError?.devEvtRegValue, e.devName, e.equipName, e.regNum, e.regNum != null ? `0x${e.regNum.toString(16)}` : '', e.packedValues].map(value => displayDeviceText(String(value ?? ''))).join(' ').toLocaleLowerCase().includes(needle)));
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
    add(data.state, (1 << 8) | (1 << 2), 'stateError');
    add(data.state, 1 << 9, 'stateFailure');
    add(data.state, 1 << 10, 'stateFreeze');
    add(data.state, (1 << 11) | (1 << 1), 'stateCrash');
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
