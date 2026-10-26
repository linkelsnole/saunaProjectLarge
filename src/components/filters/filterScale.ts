// Controller coefficients use Power=-2. UI shows cleanliness, not clogging.
export function filterPercent(k: number | null | undefined): number | null {
  if (k == null || !Number.isFinite(k) || k < 0) return null;
  return Math.round(Math.max(0, Math.min(100, (3.5 - k) / (3.5 - 0.9) * 100)));
}
export function filterColor(percent: number | null): string {
  if (percent === null) return '#94a3b8';
  const stops = [[220,38,38],[217,151,6],[0,171,115]];
  const i = percent <= 50 ? 0 : 1;
  const t = i === 0 ? percent / 50 : (percent - 50) / 50;
  return `rgb(${stops[i].map((v,j) => Math.round(v+(stops[i+1][j]-v)*t)).join(',')})`;
}
