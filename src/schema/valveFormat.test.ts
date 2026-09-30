import {expect,it} from 'vitest';
import {formatValvePercent} from './valveFormat';
it('preserves tenths instead of rounding 51.9 to 52',()=>{expect(formatValvePercent('51.9')).toBe('51.9 %');expect(formatValvePercent('0.0')).toBe('0 %');expect(formatValvePercent('100.0')).toBe('100 %');});
it('shows unavailable for missing or invalid sensor values',()=>{for(const value of [null,undefined,'','NaN','Infinity']) expect(formatValvePercent(value)).toBe('-- %');});
