import { afterEach, describe, expect, it, vi } from 'vitest';
import { MutationObserver, QueryClient } from '@tanstack/react-query';
import { mergeSettingsDashboard, settingsMutationOptions } from './settingsQueue';
afterEach(() => vi.useRealTimers());
const deferred = () => { let resolve!: () => void; let reject!: (error: Error) => void; const promise = new Promise<void>((a,b) => {resolve=a;reject=b;}); return {promise,resolve,reject}; };
type Dashboard = { settings: { volume: number; temperature: number }; sensor: number };
function setup() {
  const client = new QueryClient();
  let server: Dashboard = {settings:{volume:3000,temperature:20},sensor:10};
  const poll = (next: Dashboard) => {server=next;client.setQueryData(['dashboard'],mergeSettingsDashboard(client,next));};
  poll(server);
  vi.spyOn(client,'invalidateQueries').mockImplementation(async () => {poll(server);});
  const data = () => client.getQueryData<Dashboard>(['dashboard'])!;
  const make = (field: 'volume'|'temperature', wait: Promise<void>, calls: number[]) => new MutationObserver(client,settingsMutationOptions(client,{
    onMutate: async (value:number) => {client.setQueryData<Dashboard>(['dashboard'],old => old && ({...old,settings:{...old.settings,[field]:value}}));},
    mutationFn: async (value:number) => {calls.push(value);await wait;server={...server,settings:{...server.settings,[field]:value}};},
  }));
  return {client,poll,data,make};
}
describe('per-setting protection with a shared write queue',()=>{
  it('continues updating sensors and untouched settings during a 15-second write',async()=>{
    vi.useFakeTimers();const {client,poll,data,make}=setup();const write=deferred();
    const saved=make('volume',write.promise,[]).mutate(15000);
    await vi.advanceTimersByTimeAsync(0);
    for (let tick=1;tick<=3;tick++) {
      await vi.advanceTimersByTimeAsync(5000);
      poll({settings:{volume:3000,temperature:20+tick},sensor:10+tick});
      expect(data()).toEqual({settings:{volume:15000,temperature:20+tick},sensor:10+tick});
    }
    write.resolve();await saved;
    poll({settings:{volume:16000,temperature:24},sensor:14});
    expect(data().settings.volume).toBe(16000);client.clear();
  });
  it('protects queued fields immediately and sends writes in order',async()=>{
    vi.useFakeTimers();const {client,poll,data,make}=setup();const a=deferred();const b=deferred();const calls:number[]=[];
    const first=make('volume',a.promise,calls).mutate(15000);
    const second=make('temperature',b.promise,calls).mutate(22);
    await vi.advanceTimersByTimeAsync(0);
    poll({settings:{volume:3000,temperature:20},sensor:50});
    expect(data()).toEqual({settings:{volume:15000,temperature:22},sensor:50});expect(calls).toEqual([15000]);
    a.resolve();await first;await vi.advanceTimersByTimeAsync(0);
    expect(calls).toEqual([15000,22]);expect(data().settings.temperature).toBe(22);
    b.resolve();await second;expect(data().settings).toEqual({volume:15000,temperature:22});client.clear();
  });
  it('a failed write does not roll sensors or another pending setting back',async()=>{
    vi.useFakeTimers();const {client,poll,data,make}=setup();const a=deferred();const b=deferred();
    const first=make('volume',a.promise,[]).mutate(15000).catch(()=>undefined);
    const second=make('temperature',b.promise,[]).mutate(22);
    await vi.advanceTimersByTimeAsync(0);poll({settings:{volume:3500,temperature:20},sensor:99});
    a.reject(new Error('timeout'));await first;
    expect(data()).toEqual({settings:{volume:3500,temperature:22},sensor:99});
    b.resolve();await second;expect(data().sensor).toBe(99);client.clear();
  });
  it('the first completion cannot erase a newer edit to the same field',async()=>{
    vi.useFakeTimers();const {client,poll,data,make}=setup();const a=deferred();const b=deferred();const calls:number[]=[];
    const first=make('volume',a.promise,calls).mutate(15000);
    const second=make('volume',b.promise,calls).mutate(21000);
    await vi.advanceTimersByTimeAsync(0);a.resolve();await first;
    poll({settings:{volume:15000,temperature:20},sensor:11});
    expect(data().settings.volume).toBe(21000);
    b.resolve();await second;expect(calls).toEqual([15000,21000]);expect(data().settings.volume).toBe(21000);client.clear();
  });
  it('releases protection even when readback fails, without retrying a write',async()=>{
    const {client,poll,data,make}=setup();const calls:number[]=[];
    vi.mocked(client.invalidateQueries).mockRejectedValueOnce(new Error('offline'));
    await expect(make('volume',Promise.resolve(),calls).mutate(15000)).rejects.toThrow('offline');
    poll({settings:{volume:16000,temperature:20},sensor:12});
    expect(data().settings.volume).toBe(16000);expect(calls).toEqual([15000]);client.clear();
  });
  it('does not start the next write until the readback finishes', async()=>{
    vi.useFakeTimers();const {client,make}=setup();const read=deferred();const calls:number[]=[];
    vi.mocked(client.invalidateQueries).mockImplementationOnce(()=>read.promise);
    const first=make('volume',Promise.resolve(),calls).mutate(15000);
    const second=make('temperature',Promise.resolve(),calls).mutate(22);
    await vi.advanceTimersByTimeAsync(15000);expect(calls).toEqual([15000]);
    read.resolve();await Promise.all([first,second]);expect(calls).toEqual([15000,22]);client.clear();
  });
  it('a failed earlier edit cannot erase a later edit of the same field',async()=>{
    vi.useFakeTimers();const {client,data,make}=setup();const firstWrite=deferred();const secondWrite=deferred();
    const first=make('volume',firstWrite.promise,[]).mutate(15000).catch(()=>undefined);
    const second=make('volume',secondWrite.promise,[]).mutate(21000);
    await vi.advanceTimersByTimeAsync(0);firstWrite.reject(new Error('timeout'));await first;
    expect(data().settings.volume).toBe(21000);secondWrite.resolve();await second;
    expect(data().settings.volume).toBe(21000);client.clear();
  });
});
