import { RegisterStates } from './RegisterStates';
import { useEffect, useId, useRef, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { RefreshCw, ChevronDown } from 'lucide-react';
import { diagnosticFlags, readDiagnostics } from '@/api/systemEvents';

const counters = ['modbusErrors', 'errorRestarts', 'failureRestarts'] as const;
function CounterValue({ value }: { value: number | null }) {
 const element = useRef<HTMLSpanElement>(null);
 const previous = useRef(value);
 useEffect(() => {
  const changed = previous.current != null && value != null && previous.current !== value;
  previous.current = value;
  if (changed && !window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
   const animation = element.current?.animate([{ backgroundColor: '#e4e4e7' }, { backgroundColor: 'transparent' }], { duration: 700, easing: 'ease-out' });
   return () => animation?.cancel();
  }
 }, [value]);
 return <span ref={element} className="inline-block rounded-md px-1 -ml-1 text-3xl font-semibold tracking-tight tabular-nums text-zinc-950">{value ?? '—'}</span>;
}
export function ControllerDiagnostics() {
 const { t, i18n } = useTranslation(); const tr = (key: string) => t(`eventsJournal.${key}`);
 const [expanded, setExpanded] = useState(false); const detailsId = useId();
 const query = useQuery({ queryKey: ['controllerDiagnostics'], queryFn: ({ signal }) => readDiagnostics(signal), refetchInterval: 15000, retry: 1 });
 const data = query.data; const flags = data ? diagnosticFlags(data) : []; const known = data?.state != null && data?.workState != null;
 const word = (value: number | null) => value == null ? '—' : `0x${value.toString(16).toUpperCase().padStart(4, '0')}`;
 return <section aria-label={tr('currentFlags')} className="@container rounded-2xl border border-zinc-200 bg-white p-4 sm:p-5 animate-in fade-in duration-150 motion-reduce:animate-none">
  <div className="flex flex-wrap items-center justify-between gap-3"><h3 className="min-w-0 break-all text-base font-semibold tracking-tight text-zinc-950">{data?.logName ?? tr('currentFlags')}</h3><div className="ml-auto flex items-center gap-3">{data && <p className="text-right text-[11px] leading-relaxed text-zinc-500">{tr('lastRead')}<br/><span className="tabular-nums">{new Date(data.readAt).toLocaleString(i18n.language)}</span></p>}<button aria-label={tr('refresh')} className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg border border-zinc-200 text-zinc-800 transition-colors duration-150 hover:bg-zinc-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400 disabled:opacity-40 motion-reduce:transition-none" disabled={query.isFetching} onClick={() => void query.refetch()}><RefreshCw size={16} className={query.isFetching ? 'animate-spin motion-reduce:animate-none' : ''}/></button></div></div>
  {query.isError && <p role="alert" className="mt-3 rounded-lg bg-amber-50 p-3 text-xs leading-relaxed text-amber-800">{tr(data ? 'staleStatus' : 'statusError')}</p>}
  {query.isPending ? <div className="mt-4 h-20 animate-pulse rounded-lg bg-zinc-50 motion-reduce:animate-none"/> : data && <>
   <div className="mt-3 flex flex-wrap items-center gap-x-5 gap-y-2 text-xs">{flags.map(flag => <span key={flag} className="inline-flex items-center gap-1.5 font-medium text-rose-700"><span className="h-1.5 w-1.5 rounded-full bg-rose-500"/>{tr(`flags.${flag}`)}</span>)}{!known && <span className="text-amber-700">{tr('incompleteStatus')}</span>}{known && !query.isError && !flags.length && <span className="leading-relaxed text-zinc-600">{tr('noFlags')}</span>}</div>
   <RegisterStates state={data.state} workState={data.workState} stale={query.isError} />
   <dl className="my-5 grid grid-cols-3 divide-x divide-zinc-100">{counters.map(key => <div key={key} className="min-w-0 px-3 first:pl-0 last:pr-0 @[480px]:px-5"><dt className="mb-2 text-[11px] leading-relaxed text-zinc-600">{tr(key)}</dt><dd><CounterValue value={data[key]}/></dd></div>)}</dl>
   <div className="border-t border-zinc-100 pt-3"><button className="flex w-full items-center justify-between gap-3 rounded-md py-1 text-xs font-medium text-zinc-600 transition-colors hover:text-zinc-950 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400" aria-expanded={expanded} aria-controls={detailsId} onClick={() => setExpanded(!expanded)}>{tr('registers')}<ChevronDown size={15} className={`transition-transform duration-150 motion-reduce:transition-none ${expanded ? 'rotate-180' : ''}`}/></button><div id={detailsId} aria-hidden={!expanded} className={`grid transition-[grid-template-rows,opacity] duration-200 motion-reduce:transition-none ${expanded ? 'grid-rows-[1fr] opacity-100' : 'grid-rows-[0fr] opacity-0'}`}><div className="min-h-0 overflow-hidden"><div className="grid gap-2 pt-3 text-xs text-zinc-600 @[500px]:grid-cols-2"><p>State <span className="font-mono text-zinc-400">[0x0301]</span><strong className="ml-2 font-mono text-zinc-900">{word(data.state)}</strong></p><p>WorkState <span className="font-mono text-zinc-400">[0x0305]</span><strong className="ml-2 font-mono text-zinc-900">{word(data.workState)}</strong></p></div></div></div></div>
  </>}
 </section>;
}
