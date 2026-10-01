import { afterEach, describe, expect, it, vi } from 'vitest';
import { MutationObserver, QueryClient } from '@tanstack/react-query';
import { settingsMutationOptions } from './settingsQueue';
afterEach(() => vi.useRealTimers());
const deferred = () => { let resolve!: () => void; let reject!: (error: Error) => void; const promise = new Promise<void>((a,b) => {resolve=a;reject=b;}); return {promise,resolve,reject}; };
describe('shared settings mutations', () => {
  it('queues different fields behind a slow write, including its readback', async () => {
    vi.useFakeTimers();
    const client = new QueryClient();
    const write = deferred(); const read = deferred(); const events: string[] = [];
    vi.spyOn(client, 'invalidateQueries').mockImplementationOnce(() => {events.push('read1');return read.promise;}).mockResolvedValue(undefined);
    const make = (name: string, wait: Promise<void>) => new MutationObserver(client, settingsMutationOptions(client, {
      onMutate: async () => {events.push('snapshot'+name);},
      mutationFn: async (_value: number) => {events.push('write'+name);await wait;},
    }));
    const a = make('volume',write.promise).mutate(15000);
    const b = make('temperature',Promise.resolve()).mutate(22);
    await vi.advanceTimersByTimeAsync(15000);
    expect(events).toEqual(['snapshotvolume','writevolume']);
    write.resolve();await vi.advanceTimersByTimeAsync(0);
    expect(events).toEqual(['snapshotvolume','writevolume','read1']);
    read.resolve();await Promise.all([a,b]);
    expect(events).toEqual(['snapshotvolume','writevolume','read1','snapshottemperature','writetemperature']);
    client.clear();
  });
  it('failure restores only its own snapshot and does not erase or block later edits', async () => {
    const client = new QueryClient();vi.spyOn(client,'invalidateQueries').mockResolvedValue(undefined);
    client.setQueryData(['dashboard'], {eco:3000, normal:5000});
    const save = deferred();
    const make = (field: 'eco' | 'normal', fail: boolean) => new MutationObserver(client, settingsMutationOptions(client, {
      onMutate: async (value: number) => {const before=client.getQueryData(['dashboard']);client.setQueryData(['dashboard'],(old: object) => ({...old,[field]:value}));return before;},
      mutationFn: async (_value: number) => {if(fail) await save.promise;},
      onError: (_error,_value,before) => {client.setQueryData(['dashboard'],before);},
    }));
    const a = make('eco',true).mutate(6000).catch(() => undefined);
    const b = make('normal',false).mutate(15000);
    save.reject(new Error('timeout'));await Promise.all([a,b]);
    expect(client.getQueryData(['dashboard'])).toEqual({eco:3000,normal:15000});client.clear();
  });
  it('preserves two edits to the same field in FIFO order and does not retry failed writes', async () => {
    const client = new QueryClient();vi.spyOn(client,'invalidateQueries').mockResolvedValue(undefined);
    const calls: number[]=[];
    const make = () => new MutationObserver(client,settingsMutationOptions(client,{mutationFn:async (value:number)=>{calls.push(value);if(value===100)throw new Error('offline');}}));
    const first=make().mutate(100).catch(()=>undefined);const second=make().mutate(200);
    await Promise.all([first,second]);expect(calls).toEqual([100,200]);client.clear();
  });
});
