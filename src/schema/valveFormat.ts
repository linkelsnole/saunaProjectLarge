export function formatValvePercent(raw: string | null | undefined): string {
  if (raw == null || raw.trim() === '') return '-- %';
  const value = Number(raw);
  return Number.isFinite(value) ? `${Number(value.toFixed(1))} %` : '-- %';
}
