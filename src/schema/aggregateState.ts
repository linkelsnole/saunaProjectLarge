/** Aggregate State (0x0301): ventilation takes precedence over stale stop/fault bits. */
export function isAggregateAlarm(stateCode: number | null | undefined): boolean {
  if (stateCode == null || !Number.isInteger(stateCode) || stateCode < 0 || stateCode > 0xffff) {
    return false;
  }
  return (stateCode & 0x0006) !== 0 && (stateCode & 0x0040) === 0;
}
