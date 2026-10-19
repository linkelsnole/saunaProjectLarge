import { describe, expect, it } from 'vitest';
import { deviceName, displayDeviceText } from './deviceIdentity';
import { filterEvents, diagnosticFlags } from '@/api/systemEvents';
describe('operator device names', () => {
 it('keeps pressure sensors attached to their diagram equipment', () => {
  expect(deviceName('BP10')).toBe('PR1'); expect(deviceName('BP5')).toBe('PR2');
  expect(displayDeviceText('BP1 BP2 BF11 BF12 BF21 BF22')).toBe('PF1 PF2 M1_1 M1_2 M2_1 M2_2');
  expect(displayDeviceText('BP100 T4_1')).toBe('BP100 T4_1');
 });
 it('finds raw API events by their displayed name without changing raw data', () => {
  const event = { oid: 'one', devName: 'BP10', title: 'BP10 fault', level: 2 };
  expect(filterEvents([event], 'ALL', '', 'PR1')).toEqual([event]);
  expect(event.devName).toBe('BP10');
 });
 it('does not claim no faults for fault shutdown or communication crash', () => {
  const data = { logName: '', readAt: 0, state: 4, workState: 0, modbusErrors: 0, errorRestarts: 0, failureRestarts: 0 };
  expect(diagnosticFlags(data)).toContain('stateError');
  expect(diagnosticFlags({...data, state: 2})).toContain('stateCrash');
 });
});
