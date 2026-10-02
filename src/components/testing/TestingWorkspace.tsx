import { useRef, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Info, ArrowRightLeft, Check, Fan, FlaskConical, Gauge, LoaderCircle, Power, RefreshCw, ShieldAlert, Square, Wind, Droplets } from 'lucide-react';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/shared/ui/tabs';
import { testingApi, percentParameter, type TestRequest } from './testingApi';
import './testing.css';

type Group = 'recuperator' | 'motors' | 'dampers' | 'valves';
type Reading = { value: number; time: string };
const devices: Record<Exclude<Group, 'recuperator'>, string[]> = {
  motors: ['M1_1', 'M1_2', 'M2_1', 'M2_2'],
  dampers: ['DamperIn', 'DamperOut', 'HoodDoorD3', 'HoodDoorD4'],
  valves: ['Hot1', 'Cool1', 'Hot2', 'Cool2'],
};
const sections = [
  { key: 'recuperator', Icon: RefreshCw }, { key: 'motors', Icon: Fan },
  { key: 'dampers', Icon: Wind }, { key: 'valves', Icon: Droplets },
] as const;

export function TestingWorkspace({ embedded = false }: { embedded?: boolean }) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [busy, setBusy] = useState<string | null>(null);
  const lock = useRef(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [readings, setReadings] = useState<Record<string, Reading>>({});
  const [indices, setIndices] = useState({ motors: 0, dampers: 0, valves: 0 });
  const [percents, setPercents] = useState({ recuperator: '0', motors: '0', valves: '0' });
  const status = useQuery({ queryKey: ['testing-status'], queryFn: testingApi.status,
    refetchInterval: 10000, enabled: !busy, retry: false, refetchOnWindowFocus: false });
  const enabled = status.data?.enabled === true && !status.isError;
  const blocked = !enabled || !!busy || status.isPending;
  const label = (key: string) => t(`testing.${key}`);

  async function run(key: string, operation: () => Promise<void>) {
    if (lock.current) return;
    lock.current = true;
    setBusy(key); setError(''); setNotice('');
    try { await operation(); setNotice(label('sent')); }
    catch (cause) { setError(cause instanceof Error ? cause.message : label('failed')); }
    finally { lock.current = false; setBusy(null); }
  }
  const readingKey = (command: number, index?: number) => `${command}:${index ?? ''}`;
  function execute(payload: TestRequest) {
    const key = readingKey(payload.command, payload.index);
    return run(key, async () => {
      // Remove an old reading before requesting a new result, including failed attempts.
      setReadings(previous => {
        const next = { ...previous }; delete next[key]; return next;
      });
      const result = await testingApi.execute(payload);
      if (result.value !== undefined) {
        setReadings(previous => ({ ...previous, [key]: {
          value: result.value!, time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
        } }));
      }
    });
  }
  function apply(command: number, group: keyof typeof percents, index?: number) {
    try { void execute({ command, index, value: percentParameter(command, percents[group]) }); }
    catch (cause) { setError(label(cause instanceof Error ? cause.message : 'failed')); }
  }
  function commandButton(command: number, text: string, index?: number, value?: number, primary = false) {
    const key = readingKey(command, index);
    return <button type="button" className={`testing-button ${primary ? 'testing-button-primary' : ''}`}
      disabled={blocked} onClick={() => void execute({ command, index, value })}>
      {busy === key ? <LoaderCircle className="testing-spinner" size={17} /> : null}{text}
    </button>;
  }
  function selector(group: keyof typeof indices) {
    return <label className="testing-field"><span>{label('device')}</span>
      <select disabled={!!busy} value={indices[group]} onChange={event => setIndices(previous => ({ ...previous, [group]: Number(event.target.value) }))}>
        {devices[group].map((device, index) => <option key={device} value={index}>{device}</option>)}
      </select></label>;
  }
  function percentControl(command: number, group: keyof typeof percents, index?: number) {
    return <div className="testing-setting">
      <label className="testing-field"><span>{label(group === 'valves' ? 'opening' : 'speed')}</span>
        <div className="testing-number"><input type="number" min="0" max="100" step={command === 5 ? '0.01' : '1'}
          value={percents[group]} disabled={!!busy} onChange={event => setPercents(previous => ({ ...previous, [group]: event.target.value }))} /><span>%</span></div>
      </label>
      <button type="button" className="testing-button testing-button-primary" disabled={blocked} onClick={() => apply(command, group, index)}>
        <Check size={17} />{label('apply')}
      </button>
    </div>;
  }
  function reading(command: number, name: string, unit: string, index?: number, mask = false) {
    const data = readings[readingKey(command, index)];
    const formatted = data ? (mask ? `0x${(data.value >>> 0).toString(16).toUpperCase()}` : new Intl.NumberFormat(undefined, { maximumFractionDigits: 2 }).format(data.value)) : label('noData');
    return <div className="testing-reading">
      <div><span className="testing-reading-label">{label(name)}</span><div className={`testing-reading-value${data ? '' : ' is-empty'}`}>{formatted}<span>{data ? unit : ''}</span></div>
        {data && <small>{data.time}</small>}</div>
      {commandButton(command, label('read'), index)}
    </div>;
  }
  return <main className={`testing-workspace${embedded ? ' testing-workspace-embedded' : ''}`}>
    <section className="testing-mode">
      <div className="testing-mode-info"><span className="testing-icon"><FlaskConical size={23} /></span>
        <div><h2>{label('mode')}</h2><p>{label('modeHint')}</p></div>
      </div>
      <div className="testing-mode-actions">
        <span className={`testing-status ${enabled ? 'is-enabled' : ''}`}>{busy ? <LoaderCircle size={12} className="testing-spinner" /> : <span />}{status.isPending ? label('connecting') : status.isError ? label('offline') : label(enabled ? 'active' : 'inactive')}</span>
      <button type="button" className={`testing-button ${enabled ? '' : 'testing-button-primary'}`} disabled={!!busy || status.isPending || status.isError}
        onClick={() => void run('mode', async () => {
          try {
            const result = await testingApi.mode(!enabled);
            queryClient.setQueryData(['testing-status'], result);
            if (result.enabled === enabled) throw new Error(label('modeUnconfirmed'));
          } finally { void queryClient.invalidateQueries({ queryKey: ['testing-status'] }); }
        })}>{busy === 'mode' ? <LoaderCircle size={17} className="testing-spinner" /> : <Power size={17} />}{label(enabled ? 'disable' : 'enable')}</button>
      </div>
    </section>
    <div className={error || status.isError ? 'testing-feedback' : 'sr-only'} aria-live="polite">
      {error || status.isError ? <div className="testing-error"><ShieldAlert size={18} /><span>{error || label('offlineHint')}</span>
        {status.isError && <button className="testing-button" disabled={!!busy} onClick={() => void status.refetch()}>{label('retry')}</button>}</div>
        : busy ? label('executing') : notice}
    </div>
    <Tabs defaultValue="recuperator" className="testing-tabs">
      <TabsList className="testing-nav">{sections.map(({ key, Icon }) => <TabsTrigger key={key} value={key} disabled={!!busy}><Icon size={18} />{label(key)}</TabsTrigger>)}</TabsList>
      <TabsContent value="recuperator" className="testing-panel">
        <div className="testing-actions">
          <button className="testing-button testing-button-primary" disabled={blocked} onClick={() => void execute({ command: 2 })}><Power size={17}/>{label('start')}</button>
          <button className="testing-button" disabled={blocked} onClick={() => void execute({ command: 3 })}><Square size={17}/>{label('stop')}</button>
          <button className="testing-button" disabled={blocked} onClick={() => void execute({ command: 4 })}><ArrowRightLeft size={17}/>{label('reverse')}</button>
          <button className="testing-button" disabled={blocked} onClick={() => void execute({ command: 1 })}><RefreshCw size={17}/>{label('reset')}</button>
        </div>
        <div className="testing-controls">{percentControl(5, 'recuperator')}{reading(6, 'currentSpeed', label('rpm'))}</div>
        <p className="testing-footnote"><Info size={16}/>{label('recupErrorsUnavailable')}</p>
      </TabsContent>
      <TabsContent value="motors" className="testing-panel">
        <div className="testing-controls">{selector('motors')}{percentControl(7, 'motors', indices.motors)}</div>
        <div className="testing-readings">
        {reading(8, 'currentSpeed', label('rpm'), indices.motors)}
        {reading(9, 'relativeSpeed', '%', indices.motors)}
        {reading(10, 'maxSpeed', label('rpm'), indices.motors)}
        {reading(11, 'errorMask', '', indices.motors, true)}</div>
      </TabsContent>
      <TabsContent value="dampers" className="testing-panel">
        {selector('dampers')}<div className="testing-actions">{commandButton(12, label('open'), indices.dampers, 1, true)}{commandButton(12, label('close'), indices.dampers, 0)}</div>
        {reading(13, 'position', '%', indices.dampers)}
      </TabsContent>
      <TabsContent value="valves" className="testing-panel">
        <div className="testing-controls">{selector('valves')}{percentControl(14, 'valves', indices.valves)}</div>
        <div className="testing-reading"><div><span className="testing-reading-label">{label('position')}</span><div className="testing-reading-value">—</div><small>{label('valveUnavailable')}</small></div><Gauge size={25} className="testing-muted"/></div>
      </TabsContent>
    </Tabs>
  </main>;
}
