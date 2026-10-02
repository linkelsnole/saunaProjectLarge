import type { SVGProps } from 'react';
type Props = SVGProps<SVGSVGElement>;
const base = { viewBox: '0 0 32 32', fill: 'none', stroke: 'currentColor', strokeWidth: 2, strokeLinecap: 'round' as const, strokeLinejoin: 'round' as const };
export const Wind = (p: Props) => <svg {...base} {...p}><path d="M3 12h15c7 0 5-10 0-7-1 .6-1.5 1.5-1.5 2.5M2 17h23c7 0 5-10 0-7M4 22h14c6 0 5 8 0 6-1-.4-1.5-1-2-2"/></svg>;
export const Target = (p: Props) => <svg {...base} {...p}><path d="M27 15a12 12 0 1 1-10-11M22 15a7 7 0 1 1-7-6M16 16 28 4M24 3v5h5"/><circle cx="16" cy="16" r="2" fill="currentColor" stroke="none"/></svg>;
export const Leaf = (p: Props) => <svg {...base} {...p}><path d="M6 24C-1 6 18 5 28 4c-2 9 2 25-16 23" fill="currentColor" stroke="none"/><path d="M4 29 21 12" strokeWidth="2.5"/><path d="m9 23 12-11" stroke="white" strokeWidth="1.5"/></svg>;
export const Fan = (p: Props) => <svg {...base} {...p}><path d="M15 13c-2-3-6-7-3-10 4-4 9 0 7 4l-2 5M19 15c3-2 7-6 10-3 4 4 0 9-4 7l-5-2M17 19c2 3 6 7 3 10-4 4-9 0-7-4l2-5M13 17c-3 2-7 6-10 3-4-4 0-9 4-7l5 2"/><circle cx="16" cy="16" r="2"/></svg>;
export const SlidersHorizontal = (p: Props) => <svg {...base} {...p}><path d="M3 7h9m6 0h11M3 16h17m6 0h3M3 25h4m6 0h16"/><circle cx="15" cy="7" r="3"/><circle cx="23" cy="16" r="3"/><circle cx="10" cy="25" r="3"/></svg>;

export const Boost = (p: Props) => <svg {...base} {...p}><path d="M3 19h11c5 0 6-3 6-7V3m-6 6 6-6 6 6M3 24h17c7 0 7 7 2 6M3 29h10M5 14h4c4 0 4-5 1-5"/></svg>;
