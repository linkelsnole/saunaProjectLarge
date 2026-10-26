import { describe, it, expect } from 'vitest';
import { filterPercent } from './filterScale';
describe('filter cleanliness', () => {
 it('maps and clamps the agreed inverse scale', () => {
  expect([0.9, 2.2, 3.5, 0.5, 4].map(filterPercent)).toEqual([100,50,0,100,0]);
 });
 it('keeps absent and invalid readings unknown', () => {
  for (const k of [null, undefined, NaN, Infinity, -1]) expect(filterPercent(k)).toBeNull();
 });
});
