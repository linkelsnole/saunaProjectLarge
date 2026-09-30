import { useMemo, useState } from 'react';
import { Activity, AlertCircle, AlertTriangle, Clock, Cpu, Download, Info, RefreshCw, Search, ChevronDown } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { cn } from '@/shared/lib/utils';
import { useSystemEvents } from '@/api/useSystemEvents';
import { eventMessage, eventSource, eventTime, filterEvents, prepareEvents, type EventSeverity } from '@/api/systemEvents';
import { ControllerDiagnostics } from './ControllerDiagnostics';
const TIME_OPTIONS = [15, 60, 240, 1440, 10080, 43200, 129600];
const FILTER_OPTIONS = ['ALL', 'PROBLEMS', 'WARNING', 'ERROR', 'FAILURE', 'CRASH', 'INFO', 'LOG', 'UNKNOWN'];
const severityStyle: Record<EventSeverity, {
    tile: string;
    edge: string;
    badge: string;
    surface: string;
    text: string;
    icon: typeof Info;
}> = {
    INFO: { edge: 'border-l-slate-500', badge: 'bg-slate-50 text-slate-700', surface: 'open:bg-slate-50/40', tile: 'bg-slate-700', text: 'text-slate-800', icon: Info }, LOG: { edge: 'border-l-slate-500', badge: 'bg-slate-50 text-slate-700', surface: 'open:bg-slate-50/40', tile: 'bg-slate-500', text: 'text-slate-600', icon: Cpu },
    WARNING: { edge: 'border-l-amber-500', badge: 'bg-amber-50 text-amber-700', surface: 'open:bg-amber-50/40', tile: 'bg-amber-500', text: 'text-amber-800', icon: AlertTriangle }, ERROR: { edge: 'border-l-orange-500', badge: 'bg-orange-50 text-orange-700', surface: 'open:bg-orange-50/40', tile: 'bg-orange-600', text: 'text-orange-800', icon: AlertCircle },
    FAILURE: { edge: 'border-l-rose-500', badge: 'bg-rose-50 text-rose-700', surface: 'open:bg-rose-50/40', tile: 'bg-rose-600', text: 'text-rose-700', icon: AlertCircle }, CRASH: { edge: 'border-l-red-500', badge: 'bg-red-50 text-red-700', surface: 'open:bg-red-50/40', tile: 'bg-red-800', text: 'text-red-800', icon: AlertTriangle },
    UNKNOWN: { edge: 'border-l-slate-500', badge: 'bg-slate-50 text-slate-700', surface: 'open:bg-slate-50/40', tile: 'bg-slate-400', text: 'text-slate-700', icon: Info },
};
const input = 'min-h-10 min-w-0 rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-xs text-slate-700 outline-none transition-colors duration-150 focus:border-slate-400 focus:ring-2 focus:ring-slate-100 motion-reduce:transition-none';
const toolbarButton = 'inline-flex h-10 shrink-0 items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white px-3 text-xs font-medium text-slate-800 transition-colors duration-150 hover:bg-slate-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-slate-400 disabled:opacity-40 motion-reduce:transition-none';
const SystemEventsJournal = ({ mode = 'errors' }: {
    mode?: 'errors' | 'logs';
}) => {
    const { t, i18n } = useTranslation();
    const tr = (key: string) => t(`eventsJournal.${key}`);
    const [minutes, setMinutes] = useState(60);
    const [filter, setFilter] = useState(mode === 'errors' ? 'PROBLEMS' : 'ALL');
    const [source, setSource] = useState('');
    const [search, setSearch] = useState('');
    const [limit, setLimit] = useState(100);
    const query = useSystemEvents(minutes);
    const events = useMemo(() => prepareEvents(query.data ?? []), [query.data]);
    const sources = useMemo(() => [...new Set(events.map(eventSource).filter(Boolean))].sort(), [events]);
    const filtered = useMemo(() => filterEvents(events, filter, source, search), [events, filter, source, search]);
    const counts = useMemo(() => events.reduce<Record<string, number>>((a, e) => { a[e.severity] = (a[e.severity] ?? 0) + 1; return a; }, {}), [events]);
    const time = (ms: unknown) => { const valid = eventTime(ms); return valid ? new Date(valid).toLocaleString(i18n.language, { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit', second: '2-digit' }) : tr('unknownTime'); };
    const windowLabel = (m: number) => m < 60 ? `${m} ${t('dashboardLogs.units.min')}` : m < 1440 ? `${m / 60} ${t('dashboardLogs.units.hour')}` : t('eventsJournal.days', { count: m / 1440 });
    const resetPage = () => setLimit(100);
    function download() {
        const payload = { exportedAt: new Date().toISOString(), readAt: query.dataUpdatedAt, minutes, filter, source, search, scope: 'data-server', events: filtered.map(({ key, severity, ...event }) => ({ ...event, severity })) };
        const blob = new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json;charset=utf-8' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `sauna-events-${new Date().toISOString().replace(/[:.]/g, '-')}.json`;
        link.click();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
    }
    return <div className="space-y-4">
 {mode === 'errors' && <ControllerDiagnostics />}
 <section aria-label={tr(mode === 'errors' ? 'errorTitle' : 'logTitle')} className="@container rounded-[22px] border border-slate-200/90 bg-white p-4 shadow-[0_4px_18px_-12px_rgba(15,23,42,.15)] sm:p-5 animate-in fade-in duration-200 motion-reduce:animate-none">
  <div className="flex flex-wrap items-start justify-between gap-3"><div className="flex min-w-0 items-center gap-3"><div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-slate-50 text-slate-900"><Activity size={23} strokeWidth={1.7}/></div><div className="min-w-0"><h3 className="text-lg font-semibold tracking-tight text-slate-950">{tr(mode === 'errors' ? 'errorTitle' : 'logTitle')}</h3><p className="mt-0.5 text-xs text-slate-500">{tr('historyHint')}</p></div></div>
  <div className="ml-auto flex gap-2"><button className={toolbarButton} disabled={!filtered.length || query.isPending} onClick={download} aria-label={tr('export')} title={tr('export')}><Download size={17}/><span>{tr('exportButton')}</span></button><button className={`${toolbarButton} w-10 px-0`} disabled={query.isFetching} onClick={() => void query.refetch()} aria-label={tr('refresh')}><RefreshCw size={18} className={query.isFetching ? 'animate-spin motion-reduce:animate-none' : ''}/></button></div></div>
  <div className="mt-5 flex flex-wrap gap-1 rounded-xl bg-slate-50 p-1" role="group" aria-label={tr('period')}>{TIME_OPTIONS.map(m => <button key={m} className={cn('min-h-9 flex-1 whitespace-nowrap rounded-lg px-2.5 py-2 text-xs font-medium transition-colors duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-slate-400 motion-reduce:transition-none', minutes === m ? 'bg-slate-900 text-white shadow-sm' : 'text-slate-600 hover:bg-white')} aria-pressed={minutes === m} onClick={() => { setMinutes(m); setSource(''); resetPage(); }}>{windowLabel(m)}</button>)}</div>
  <label className="relative mt-3 block"><Search size={17} className="pointer-events-none absolute left-3 top-3 text-slate-400"/><input className={`${input} w-full pl-10`} value={search} placeholder={tr('search')} aria-label={tr('search')} onChange={e => { setSearch(e.target.value); resetPage(); }}/></label>
  <div className="mt-2.5 grid grid-cols-1 gap-2 @[420px]:grid-cols-2"><select aria-label={tr('level')} className={`${input} bg-slate-50/70`} value={filter} onChange={e => { setFilter(e.target.value); resetPage(); }}>{FILTER_OPTIONS.map(value => <option value={value} key={value}>{tr(`levels.${value}`)}{counts[value] != null ? ` · ${counts[value]}` : ''}</option>)}</select><select aria-label={tr('source')} className={`${input} bg-slate-50/70`} value={source} onChange={e => { setSource(e.target.value); resetPage(); }}><option value="">{tr('allSources')}</option>{source && !sources.includes(source) && <option value={source}>{source}</option>}{sources.map(s => <option key={s} value={s}>{s}</option>)}</select></div>
  <p className="mt-3 text-[11px] leading-relaxed text-slate-400">{tr('scopeHint')}{query.dataUpdatedAt > 0 && <> · {tr('lastRead')} {time(query.dataUpdatedAt)}</>}</p>
  {query.isError && <div role="alert" className="mt-3 rounded-xl border border-amber-200 bg-amber-50 p-3 text-sm text-amber-800">{tr(query.data ? 'staleJournal' : 'loadError')}</div>}
  <div className="mt-4 space-y-3">
   {query.isPending ? <div className="space-y-2">{[0, 1, 2].map(i => <div key={i} className="h-20 animate-pulse rounded-xl bg-slate-100 motion-reduce:animate-none"/>)}</div> : filtered.length === 0 ? (!query.isError && <div className="rounded-2xl border border-dashed border-slate-200 bg-slate-50 p-6 text-center text-sm text-slate-500">{tr(events.length ? 'noMatches' : 'empty')}</div>) : filtered.slice(0, limit).map((event, index) => {
            const config = severityStyle[event.severity];
            const Icon = config.icon;
            const arrival = eventTime(event.arrivalTime?.timeInMS);
            const end = eventTime(event.endTime?.timeInMS);
            return <details key={event.key} className={cn('group overflow-hidden rounded-2xl border border-slate-200 border-l-[3px] bg-white transition-colors duration-150 animate-in fade-in fill-mode-backwards duration-200 motion-reduce:animate-none motion-reduce:transition-none', config.edge, config.surface)} style={{ animationDelay: `${Math.min(index, 6) * 20}ms` }}>
     <summary className="flex cursor-pointer list-none items-start gap-3 p-3.5 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-slate-400 [&::-webkit-details-marker]:hidden">
      <span className={cn('mt-0.5 flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-white shadow-sm', config.tile)}><Icon size={20} strokeWidth={1.8}/></span>
      <span className="min-w-0 flex-1"><span className="flex flex-wrap items-center justify-between gap-x-3 gap-y-2"><span className={cn('inline-flex rounded-lg px-2 py-1 text-[11px] font-medium', config.badge)}>{tr(`levels.${event.severity}`)}{event.severity === 'UNKNOWN' && event.level != null ? ` (${event.level})` : ''}</span><span className="inline-flex items-center gap-1.5 text-[10px] tabular-nums text-slate-500"><Clock size={12} className="shrink-0"/>{time(arrival)}</span></span>
       <span className="mt-2 block whitespace-pre-wrap break-words text-sm font-medium leading-relaxed text-slate-900 [overflow-wrap:anywhere]">{eventMessage(event) || tr('noDescription')}</span>
       <span className="mt-1.5 block break-words text-[11px] leading-relaxed text-slate-500">{eventSource(event) || tr('unknownSource')} · {event.type != null ? t(`eventsJournal.types.t${event.type}`, { defaultValue: `${tr('type')} ${event.type}` }) : tr('unknownType')}</span>
      </span><span className="mt-1 shrink-0 text-slate-500"><ChevronDown size={17} className="transition-transform duration-150 group-open:rotate-180 motion-reduce:transition-none"/><span className="sr-only">{tr('details')}</span></span>
     </summary>
     <div className="mx-3.5 mb-3.5 rounded-xl border border-slate-100 bg-white p-4 text-xs leading-relaxed">
<dl className="grid grid-cols-1 gap-x-5 gap-y-1 @[440px]:grid-cols-[auto_minmax(0,1fr)] @[440px]:gap-y-2 [&_dt]:text-slate-400 [&_dd]:mb-2 @[440px]:[&_dd]:mb-0 [&_dd]:text-slate-700 break-words [overflow-wrap:anywhere]">
       <dt>{tr('id')}</dt><dd>{event.oid ?? '—'}</dd><dt>{tr('start')}</dt><dd>{time(arrival)}</dd><dt>{tr('end')}</dt><dd>{end != null ? time(end) : tr('notProvided')}</dd>
       {arrival != null && end != null && end > arrival && <><dt>{tr('duration')}</dt><dd>{((end - arrival) / 1000).toLocaleString(i18n.language)} {tr('seconds')}</dd></>}
       <dt>{tr('register')}</dt><dd>{event.regNum != null && event.regNum >= 0 ? `0x${event.regNum.toString(16).toUpperCase()} (${event.regNum})` : '—'}{event.bitNum != null && event.bitNum >= 0 ? ` [${event.bitNum}]` : ''}</dd>
       <dt>{tr(event.type === 16 ? 'reactionCode' : 'value')}</dt><dd>{event.value ?? '—'}</dd><dt>{tr(event.type === 16 ? 'sourceCode' : 'previousValue')}</dt><dd>{event.prevValue ?? '—'}</dd>
       <dt>{tr('viewLevel')}</dt><dd>{event.viewLevel ?? '—'}</dd><dt>{tr('secondRegister')}</dt><dd>{event.secondRegNum ?? '—'} / {event.secondRegValue ?? '—'}</dd>
       {event.title && <><dt>{tr('originalTitle')}</dt><dd className="whitespace-pre-wrap">{event.title}</dd></>}{event.packedValues && <><dt>{tr('packedValues')}</dt><dd className="whitespace-pre-wrap font-mono">{event.packedValues}</dd></>}
      </dl><p className="mt-3 text-[11px] leading-relaxed text-slate-400">{tr('reactionHint')}</p></div>
    </details>;
        })}
  </div>
  {!query.isPending && events.length > 0 && <div className="mt-3 flex flex-wrap items-center justify-between gap-2 text-xs text-slate-400"><span>{t('eventsJournal.shown', { count: Math.min(limit, filtered.length), total: filtered.length })}</span>{filtered.length > limit && <button className="rounded-xl bg-slate-100 px-3 py-2 font-semibold text-slate-700 transition-colors hover:bg-slate-200" onClick={() => setLimit(limit + 100)}>{tr('showMore')}</button>}</div>}
 </section></div>;
};
export default SystemEventsJournal;
