export function rotorPercent(current: number | null | undefined, maximum: number | null | undefined): number | null {
 if (current == null || maximum == null || !Number.isFinite(current) || !Number.isFinite(maximum) || current < 0 || maximum <= 0) return null;
 return Math.round(current / maximum * 1000) / 10;
}
export function pumpPercent(active: boolean | null | undefined): number | null {
 return active === true ? 100 : active === false ? 0 : null;
}
