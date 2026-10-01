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
const queues = new WeakMap<QueryClient, SettingsQueue>();
interface SettingsMutation<V, C> {
  mutationFn: (value: V) => Promise<void>;
  onMutate?: (value: V) => Promise<C>;
  onSuccess?: () => void;
  onError?: (error: Error, value: V, context: C | undefined) => void;
}
export function settingsMutationOptions<V, C>(client: QueryClient, options: SettingsMutation<V, C>) {
  let queue = queues.get(client);
  if (!queue) { queue = new SettingsQueue(); queues.set(client, queue); }
  const sharedQueue = queue;
  return {
    mutationKey: ['settings-write'],
    retry: false as const,
    mutationFn: (value: V) => sharedQueue.run(async () => {
      let context: C | undefined;
      try {
        // Snapshot and optimism happen only when this write reaches the head.
        context = await options.onMutate?.(value);
        await options.mutationFn(value);
        options.onSuccess?.();
      } catch (error) {
        options.onError?.(error as Error, value, context);
        throw error;
      } finally {
        // Read back before taking the next snapshot, including after partial failures.
        await client.invalidateQueries({ queryKey: ['dashboard'] });
      }
    }),
  };
}
export function useQueuedSettingsMutation<V, C>(options: SettingsMutation<V, C>) {
  return useMutation(settingsMutationOptions(useQueryClient(), options));
}
