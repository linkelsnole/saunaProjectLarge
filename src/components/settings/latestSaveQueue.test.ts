import { afterEach, describe, expect, it, vi } from 'vitest';
import { LatestSaveQueue } from './latestSaveQueue';
afterEach(() => vi.useRealTimers());
describe('latest slider save', () => {
  it('debounces and serializes slow writes, sending only the latest pending value', async () => {
    vi.useFakeTimers();
    let complete!: () => void;
    const save = vi.fn().mockImplementationOnce(() => new Promise<void>(r => { complete = r; })).mockResolvedValue(undefined);
    const settled = vi.fn();
    const queue = new LatestSaveQueue(save, settled);
    queue.schedule(14000); queue.schedule(15000);
    expect(queue.busy).toBe(true);
    await vi.advanceTimersByTimeAsync(500);
    expect(save.mock.calls).toEqual([[15000]]);
    queue.schedule(17000); queue.schedule(21000);
    await vi.advanceTimersByTimeAsync(15000);
    expect(save).toHaveBeenCalledTimes(1);
    expect(settled).not.toHaveBeenCalled();
    complete(); await vi.advanceTimersByTimeAsync(0);
    expect(save.mock.calls).toEqual([[15000], [21000]]);
    expect(queue.busy).toBe(false);
    expect(settled).toHaveBeenCalledTimes(1);
  });
  it('settles after failure and cancels unsent edits when disposed', async () => {
    vi.useFakeTimers();
    const save = vi.fn().mockRejectedValue(new Error('offline'));
    const settled = vi.fn();
    const queue = new LatestSaveQueue(save, settled);
    queue.schedule(15000); await vi.advanceTimersByTimeAsync(500);
    expect(queue.busy).toBe(false); expect(settled).toHaveBeenCalledTimes(1);
    queue.schedule(21000); queue.dispose(); await vi.advanceTimersByTimeAsync(500);
    expect(save).toHaveBeenCalledTimes(1);
  });
});
