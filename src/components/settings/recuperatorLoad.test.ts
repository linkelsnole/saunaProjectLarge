import { expect, it } from 'vitest';
import { rotorPercent, pumpPercent } from './recuperatorPercent';
it('uses current RPM divided by max, preserving values above max',()=>{
 expect(rotorPercent(6.3,9)).toBe(70); expect(rotorPercent(0,9)).toBe(0); expect(rotorPercent(10,9)).toBe(111.1);
});
it('does not invent RPM or pump readings',()=>{
 expect(rotorPercent(null,9)).toBeNull(); expect(rotorPercent(1,0)).toBeNull(); expect(rotorPercent(NaN,9)).toBeNull();
 expect(pumpPercent(true)).toBe(100); expect(pumpPercent(false)).toBe(0); expect(pumpPercent(null)).toBeNull();
});
