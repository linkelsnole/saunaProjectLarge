import { expect, it } from 'vitest';
import { alarmDuration, climateDeviceNames, deviceDescriptions } from './deviceLabels';
it('keeps outside air T1 and exhaust outlet T2 tied to their existing API sensor IDs', () => {
  expect(climateDeviceNames.BT3).toBe('T1');
  expect(climateDeviceNames.BT11).toBe('T2');
  expect(deviceDescriptions.T1).toContain('Наружный воздух');
  expect(deviceDescriptions.T2).toContain('выход вытяжки');
});
it('formats alarm duration without inventing missing time or wrapping after an hour', () => {
  expect(alarmDuration(154)).toBe('02:34');
  expect(alarmDuration(3601)).toBe('60:01');
  expect(alarmDuration(0)).toBe('00:00');
  expect(alarmDuration(null)).toBe('');
  expect(alarmDuration(-1)).toBe('');
});
