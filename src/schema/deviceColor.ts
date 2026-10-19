import { deviceName } from './deviceIdentity';
export function deviceColor(ids: string[], selected: string | null, states: ReadonlyArray<{id: string; alarmMode?: number | null}>, available: boolean): string | undefined {
 if (!available) return undefined;
 const modes = states.filter(d => ids.includes(deviceName(d.id))).map(d => d.alarmMode);
 if (modes.some(mode => mode === 2 || mode === 3)) return '#dc2626';
 if (modes.some(mode => mode === 1)) return '#d97706';
 if (selected && ids.includes(selected) && modes.length > 0 && modes.every(mode => mode === 0)) return '#059669';
 return undefined;
}
