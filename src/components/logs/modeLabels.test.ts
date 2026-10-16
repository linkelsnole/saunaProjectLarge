import { expect, it } from 'vitest';
import { formatModeLog } from './modeLabels';
it('decodes old mode pairs without confusing temperature manual with volume intensive', () => {
  expect(formatModeLog('Режим температуры зоны 1: 1 → 3; Режим объёма: 3 → 4')).toBe('Режим температуры зоны 1: ЭКО → Ручной; Режим объёма: Интенсив → Ручной');
  expect(formatModeLog('Режим вентиляции: 1')).toBe('Режим вентиляции: ЭКО');
});
it('preserves setpoints and explicitly identifies unknown mode codes', () => {
  expect(formatModeLog('Цель зоны 1: 0 → 3 °C')).toBe('Цель зоны 1: 0 → 3 °C');
  expect(formatModeLog('Режим объёма: 99')).toContain('Неизвестный режим (99)');
});
