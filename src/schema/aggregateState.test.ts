import { describe, expect, it } from 'vitest';
import { isAggregateAlarm } from './aggregateState';

describe('aggregate alarm indication', () => {
  it.each([0x0040, 0x0042, 0x0044, 0x0046, 0x0244])(
    'ignores stale alarm bits while ventilation is set: %i',
    state => expect(isAggregateAlarm(state)).toBe(false),
  );
  it.each([0x0002, 0x0004, 0x0006, 0x0204])(
    'shows the alarm when ventilation is not set: %i',
    state => expect(isAggregateAlarm(state)).toBe(true),
  );
  it.each([null, undefined, NaN, -1, 65536, 1.5, 0, 1, 16, 128])(
    'does not invent an alarm for unknown or non-alarm state: %s',
    state => expect(isAggregateAlarm(state)).toBe(false),
  );
});
