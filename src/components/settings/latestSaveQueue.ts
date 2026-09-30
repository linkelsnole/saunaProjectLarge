/** Debounce edits and keep only the latest value while a write is in flight. */
export class LatestSaveQueue {
  private timer: ReturnType<typeof setTimeout> | undefined;
  private pending: number | undefined;
  private running = false;
  private save: (value: number) => Promise<unknown>;
  private settled: () => void;
  constructor(save: (value: number) => Promise<unknown>, settled: () => void) {
    this.save = save;
    this.settled = settled;
  }
  get busy() { return this.running || this.pending !== undefined; }
  schedule(value: number) {
    this.pending = value;
    clearTimeout(this.timer);
    this.timer = setTimeout(() => { this.timer = undefined; void this.flush(); }, 500);
  }
  private async flush() {
    if (this.running || this.pending === undefined) return;
    const value = this.pending;
    this.pending = undefined;
    this.running = true;
    try { await this.save(value); } catch { /* Mutation reports errors and restores server state. */ }
    finally {
      this.running = false;
      if (this.pending !== undefined) {
        if (!this.timer) void this.flush();
      } else this.settled();
    }
  }
  dispose() { clearTimeout(this.timer); this.pending = undefined; }
}
