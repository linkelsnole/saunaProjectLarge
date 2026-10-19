import { useSyncExternalStore } from 'react';
export const deviceAliases: Record<string, string> = { BP1: 'PF1', BP2: 'PF2', BP10: 'PR1', BP5: 'PR2', BF11: 'M1_1', BF12: 'M1_2', BF21: 'M2_1', BF22: 'M2_2' };
export const deviceName = (id: string) => deviceAliases[id] ?? id;
export function displayDeviceText(text: string): string {
 return text.replace(/\b(?:BP10|BP5|BP1|BP2|BF11|BF12|BF21|BF22)\b/g, id => deviceName(id));
}
let selected: string | null = null;
const listeners = new Set<() => void>();
export function selectDevice(id: string | null) { selected = id ? deviceName(id) : null; listeners.forEach(fn => fn()); }
export function useSelectedDevice() {
 return useSyncExternalStore(fn => { listeners.add(fn); return () => { listeners.delete(fn); }; }, () => selected, () => null);
}
