import { useMutation, useQueryClient, type QueryClient } from '@tanstack/react-query';

/** One FIFO per panel query client. A failure must not poison later writes. */
export class SettingsQueue {
  private tail: Promise<unknown> = Promise.resolve();
  run<T>(operation: () => Promise<T>): Promise<T> {
    const result = this.tail.then(operation);
    this.tail = result.catch(() => undefined);
    return result;
  }
}
type Patch = { path: string[]; value: unknown };
const record = (value: unknown): value is Record<string, unknown> =>
  value !== null && typeof value === 'object' && !Array.isArray(value);
function differences(before: unknown, after: unknown, path: string[] = []): Patch[] {
  if (Object.is(before, after)) return [];
  if (record(before) && record(after)) {
    return Object.keys(after).flatMap(key => differences(before[key], after[key], [...path, key]));
  }
  return [{ path, value: after }];
}
function apply(data: unknown, patch: Patch): unknown {
  if (!patch.path.length) return patch.value;
  const [key, ...rest] = patch.path;
  const old = record(data) ? data : {};
  return { ...old, [key]: apply(old[key], { path: rest, value: patch.value }) };
}
interface State {
  writes: SettingsQueue;
  preparations: SettingsQueue;
  raw: unknown;
  pending: Map<symbol, Patch[]>;
}
const states = new WeakMap<QueryClient, State>();
function stateFor(client: QueryClient): State {
  let state = states.get(client);
  if (!state) {
    state = { writes: new SettingsQueue(), preparations: new SettingsQueue(), raw: client.getQueryData(['dashboard']), pending: new Map() };
    states.set(client, state);
  }
  return state;
}
function displayed(state: State): unknown {
  let data = state.raw;
  for (const patches of state.pending.values()) for (const patch of patches) data = apply(data, patch);
  return data;
}
/** Keep live readings, masking only fields with outstanding writes (including queued writes). */
export function mergeSettingsDashboard<T>(client: QueryClient, incoming: T): T {
  const state = stateFor(client);
  state.raw = incoming;
  return displayed(state) as T;
}
interface SettingsMutation<V, C> {
  mutationFn: (value: V) => Promise<void>;
  onMutate?: (value: V) => Promise<C>;
  onSuccess?: () => void;
  onError?: (error: Error, value: V, context: C | undefined) => void;
}
export function settingsMutationOptions<V, C>(client: QueryClient, options: SettingsMutation<V, C>) {
  const state = stateFor(client);
  return {
    mutationKey: ['settings-write'],
    retry: false as const,
    mutationFn: (value: V) => {
      const token = Symbol('setting');
      let context: C | undefined;
      // Prepare queued edits immediately, so their inputs also retain the newest value.
      const prepared = state.preparations.run(async () => {
        const before = client.getQueryData(['dashboard']);
        if (!state.pending.size) state.raw = before;
        const preparation = options.onMutate?.(value);
        state.pending.set(token, differences(before, client.getQueryData(['dashboard'])));
        context = await preparation;
      });
      return state.writes.run(async () => {
        try {
          await prepared;
          await options.mutationFn(value);
          options.onSuccess?.();
        } catch (error) {
          options.onError?.(error as Error, value, context);
          throw error;
        } finally {
          try {
            // Polling remains enabled; this fresh read follows the completed write.
            await client.invalidateQueries({ queryKey: ['dashboard'] });
          } finally {
            state.pending.delete(token);
            client.setQueryData(['dashboard'], displayed(state));
          }
        }
      });
    },
  };
}
export function useQueuedSettingsMutation<V, C>(options: SettingsMutation<V, C>) {
  return useMutation(settingsMutationOptions(useQueryClient(), options));
}
