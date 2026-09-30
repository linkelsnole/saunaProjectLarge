import { useEffect, useRef, useState } from 'react';
import { Switch } from '@/shared/ui/switch';
import { useAuth } from '@/contexts/AuthContext';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { RefreshCw, Save, LayoutGrid, ArrowLeft, Check, Pencil, X, Info } from 'lucide-react';
import { toast } from 'sonner';
import { applyPhysicalSchedules, changesFor, displayValue, enumKeys, hex, names, offsets, readPhysicalSchedules, savePhysicalSlot, slotDraft, type PhysicalSlot } from './physicalSchedule';

const button='inline-flex items-center justify-center gap-2 rounded-xl px-4 py-2.5 text-sm font-medium transition-colors duration-150 motion-reduce:transition-none disabled:opacity-40 disabled:cursor-not-allowed focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400';
const input='w-full min-w-0 rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm text-slate-800 outline-none focus:border-zinc-400 focus:ring-2 focus:ring-zinc-100 disabled:opacity-50 transition-colors duration-150 motion-reduce:transition-none';
export function PhysicalScheduleEditor() {
  const {role}=useAuth();
  return role==='ROLE_ADMIN' ? <AdminPhysicalScheduleEditor/> : null;
}

function AdminPhysicalScheduleEditor() {
  const queryClient=useQueryClient();
  const {t}=useTranslation();
  const tr=(key:string)=>t(`physicalSchedule.${key}`);
  const query=useQuery({queryKey:['physical-schedules'],queryFn:readPhysicalSchedules,staleTime:Infinity,refetchInterval:false,refetchOnWindowFocus:false,refetchOnReconnect:false,refetchOnMount:false,retry:false});
  const [baseline,setBaseline]=useState<PhysicalSlot|null>(null);
  const [editing,setEditing]=useState(false);
  const selectionRef=useRef<HTMLDivElement>(null);
  useEffect(()=>{ if(baseline) selectionRef.current?.scrollIntoView({behavior:window.matchMedia('(prefers-reduced-motion: reduce)').matches?'instant':'smooth',block:'nearest'}); },[baseline?.slotId]);
  const [draft,setDraft]=useState<Record<number,string>>({});
  const [busy,setBusy]=useState(false);
  const [failure,setFailure]=useState('');
  const [group,setGroup]=useState('all');
  const [workingOnly,setWorkingOnly]=useState(false);
  const slots=query.data?.slots??[];
  const live=slots.find(s=>s.slotId===baseline?.slotId);
  const groups=[...new Set(slots.map(s=>s.values[8]).filter((v):v is number=>v!=null))].sort((a,b)=>a-b);
  let validation='';
  let changes: ReturnType<typeof changesFor>=[];
  if (baseline) { try {changes=changesFor(baseline,draft);} catch(e) {validation=e instanceof Error?e.message:String(e);} }
  const dirty=baseline!=null && offsets.some(o=>draft[o]!==displayValue(baseline.values,o));
  const working=slots.filter(s=>s.values[1]!=null && (s.values[1]&2)!==0);
  const select=(slot:PhysicalSlot)=>{setBaseline(slot);setDraft(slotDraft(slot));setFailure('');};
  async function save() {
    if (!baseline || validation || !changes.length) return;
    setBusy(true);setFailure('');
    try {
      const result=await savePhysicalSlot(baseline.slotId,changes);
      if(result.verified) {select(result.slot);toast.success(tr('saved'));}
      else {setFailure(tr('unverified'));toast.warning(tr('unverified'));}
    } catch(e) {setFailure(e instanceof Error?e.message:String(e));}
    finally {await query.refetch();await queryClient.invalidateQueries({queryKey:['schedules']});setBusy(false);}
  }
  async function apply() {
    setBusy(true);setFailure('');
    try {await applyPhysicalSchedules();toast.success(tr('applied'));}
    catch(e) {setFailure(e instanceof Error?e.message:String(e));}
    finally {await query.refetch();await queryClient.invalidateQueries({queryKey:['schedules']});setBusy(false);}
  }
  const raw=(slot:PhysicalSlot,o:number)=>slot.values[o]==null?'—':hex(slot.values[o]);
  const state=(slot?:PhysicalSlot)=>slot?.values[1];
  const slotType=(slot:PhysicalSlot)=>slot.values[0]!=null && slot.values[0]<=5?tr(`types.${slot.values[0]}`):`Mode ${slot.values[0]??'—'}`;
  const isWorking=(slot:PhysicalSlot)=>state(slot)!=null && (state(slot)!&2)!==0;
  const isSelected=(slot:PhysicalSlot)=>state(slot)!=null && (state(slot)!&1)!==0;
  const shown=slots.filter(s=>(group==='all'||String(s.values[8])===group)&&(!workingOnly||isWorking(s)));
  const current=live??baseline;
  const statusBadge=(slot:PhysicalSlot)=>state(slot)==null?<span className="text-[10px] text-zinc-400">{tr('missing')}</span>:isWorking(slot)?<span className="rounded-md bg-emerald-100 px-2 py-1 text-[10px] font-medium text-emerald-800">{tr('workingOnly')}</span>:isSelected(slot)?<span className="rounded-md bg-zinc-100 px-2 py-1 text-[10px] font-medium text-zinc-800">{tr('selected')}</span>:null;
  const badge=(slot:PhysicalSlot)=> <span className="flex flex-wrap gap-1.5 text-[11px]">
    {state(slot)==null ? <span className="text-slate-400">{tr('missing')}</span> : <>
    <span className={(state(slot)!&1)?'text-zinc-900':'text-slate-400'}>Selected {(state(slot)!&1)?'1':'0'}</span>
    <span className={(state(slot)!&2)?'font-semibold text-emerald-600':'text-slate-400'}>Working {(state(slot)!&2)?'1':'0'}</span></>}
  </span>;
  return <section className="@container space-y-4 text-zinc-900 animate-in fade-in duration-150 motion-reduce:animate-none">
    <div className="rounded-2xl border border-zinc-200/80 bg-white p-4 @[600px]:p-5">
      <div className="flex flex-wrap items-start justify-between gap-4 border-b border-zinc-100 pb-4">
        <div className="flex items-center gap-3"><LayoutGrid size={22} strokeWidth={1.7} className="shrink-0"/><div><h3 className="text-lg font-semibold tracking-tight">{tr('title')}</h3><p className="mt-1 text-xs text-zinc-500">{tr('map')} · {query.data?.reportedSlots??'—'} {tr('slotCount')}</p></div></div>
        <div className="ml-auto flex items-center gap-3"><p className="text-right text-[11px] leading-relaxed text-zinc-500">{tr('manualRefresh')}<br/>{query.data?`${tr('updated')} ${new Date(query.data.readAt).toLocaleString()}`:tr('loading')}</p><button className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border border-zinc-200 transition-colors hover:bg-zinc-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400 disabled:opacity-40" disabled={busy||query.isFetching} onClick={()=>void query.refetch()} aria-label={tr('refresh')}><RefreshCw size={17} className={query.isFetching?'animate-spin motion-reduce:animate-none':''}/></button></div>
      </div>
      {(query.error||failure) && <div role="alert" className="mt-4 rounded-xl border border-rose-200 bg-rose-50 p-3 text-sm text-rose-700">{failure||`${tr('readError')}: ${query.error?.message}`}</div>}
      {query.data && <div className="mt-4 flex flex-wrap items-center gap-2 text-sm"><span className={`h-2 w-2 rounded-full ${working.length?'bg-emerald-500':'bg-zinc-300'}`}/><span className="font-medium">{tr('workingNow')}</span><span className={working.length?'font-medium text-emerald-700':'text-zinc-500'}>{working.length?working.map(s=>`#${String(s.slotId).padStart(2,'0')}`).join(', '):tr('noWorking')}</span></div>}
      <div className="my-4 flex flex-wrap items-center justify-between gap-3"><select aria-label={tr('group')} className={`${input} max-w-56 flex-1`} value={group} onChange={e=>setGroup(e.target.value)}><option value="all">{tr('allGroups')}</option>{groups.map(g=><option key={g} value={g}>{g===0?tr('standalone'):`${tr('group')} ${g}`}</option>)}</select><label className="flex cursor-pointer items-center gap-2 text-xs text-zinc-700"><Switch checked={workingOnly} onCheckedChange={setWorkingOnly} aria-label={tr('workingOnly')} className="data-[state=checked]:bg-zinc-950 data-[state=unchecked]:bg-zinc-200 motion-reduce:transition-none [&_span]:bg-white"/>{tr('workingOnly')}</label></div>
      {dirty && <p className="mb-3 text-xs text-zinc-600">{tr('finishEditing')}</p>}
      <div className="grid grid-cols-2 gap-2.5 @[520px]:grid-cols-3 @[700px]:grid-cols-5">{shown.map(slot=><button key={slot.slotId} disabled={busy||dirty} aria-pressed={baseline?.slotId===slot.slotId} onClick={()=>{select(slot);setEditing(false);}} className={`min-w-0 rounded-xl border p-3 text-left transition duration-150 hover:-translate-y-0.5 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400 disabled:cursor-not-allowed disabled:hover:translate-y-0 motion-reduce:transform-none motion-reduce:transition-none ${baseline?.slotId===slot.slotId?'border-zinc-950 ring-1 ring-zinc-950':isWorking(slot)?'border-emerald-500 bg-emerald-50/30 hover:bg-emerald-50/60':'border-zinc-200 bg-zinc-50/30 hover:border-zinc-400 hover:bg-zinc-50'}`}>
        <span className="flex flex-wrap items-center justify-between gap-1.5"><span className="flex items-center gap-2"><span className="text-lg font-semibold tracking-tight tabular-nums"><span className="sr-only">{tr('slot')} </span>{String(slot.slotId).padStart(2,'0')}</span><span className={`h-2 w-2 rounded-full ${state(slot)==null?'border border-dashed border-zinc-400':isWorking(slot)?'bg-emerald-500':isSelected(slot)?'bg-zinc-900':'bg-zinc-300'}`}/></span>{statusBadge(slot)}</span>
        <span className="mt-2 block text-xs text-zinc-700">{tr('group')} {slot.values[8]??'—'}</span><span className="mt-1 block text-[11px] leading-relaxed text-zinc-500">{slotType(slot)}</span><code className="mt-2 block text-[11px] text-zinc-400">{hex(slot.baseAddress)}</code>
      </button>)}</div>
      {query.data&&!shown.length&&<p className="py-6 text-center text-sm text-zinc-500">{tr('noMatches')}</p>}
      {query.isPending&&<div className="grid grid-cols-2 gap-2.5 @[520px]:grid-cols-3 @[700px]:grid-cols-5">{Array.from({length:10},(_,i)=><div key={i} className="h-28 animate-pulse rounded-xl bg-zinc-50 motion-reduce:animate-none"/>)}</div>}
      {query.data && <div className="mt-4 flex justify-end border-t border-zinc-100 pt-4"><button className={`${button} border border-zinc-200 hover:bg-zinc-50`} disabled={busy||query.isError||dirty} onClick={()=>void apply()}><RefreshCw size={15}/>{tr('apply')}</button></div>}
    </div>
    {baseline&&current&&<div ref={selectionRef} className="scroll-mt-4 overflow-hidden rounded-2xl border border-zinc-200 bg-white animate-in fade-in slide-in-from-bottom-1 duration-150 motion-reduce:animate-none">
      <div className="flex items-start justify-between gap-3 p-4 @[600px]:p-5"><div className="flex min-w-0 items-center gap-3"><span className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-full text-lg font-semibold ${isWorking(current)?'bg-emerald-50 text-emerald-900':'bg-zinc-100 text-zinc-900'}`}>{String(baseline.slotId).padStart(2,'0')}</span><div className="min-w-0"><div className="flex flex-wrap items-center gap-2"><h4 className="text-lg font-semibold">{tr('slot')} {String(baseline.slotId).padStart(2,'0')}</h4>{statusBadge(current)}</div><p className="mt-1 text-xs leading-relaxed text-zinc-500">{tr('group')} {current.values[8]??'—'} · {slotType(current)} · <code>{hex(baseline.baseAddress)}</code></p></div></div><button className="rounded-lg p-2 text-zinc-500 hover:bg-zinc-100 disabled:opacity-30 focus-visible:ring-2 focus-visible:ring-zinc-400" disabled={busy||dirty} aria-label={tr('allSlots')} onClick={()=>{setBaseline(null);setEditing(false);}}><X size={17}/></button></div>
      {!editing?<div className="grid gap-5 px-4 pb-4 @[560px]:grid-cols-2 @[600px]:px-5 @[600px]:pb-5"><dl className="grid grid-cols-[auto_minmax(0,1fr)] gap-x-4 gap-y-2 text-xs leading-relaxed [&_dt]:text-zinc-500"><dt>{tr('fields.0')}</dt><dd>{slotType(current)}</dd><dt>{tr('group')}</dt><dd>{current.values[8]??'—'}</dd><dt>{tr('address')}</dt><dd className="font-mono">{hex(current.baseAddress)}</dd><dt>Selected</dt><dd>{state(current)==null?'—':isSelected(current)?'1':'0'}</dd><dt>Working</dt><dd>{state(current)==null?'—':isWorking(current)?'1':'0'}</dd></dl><div className="space-y-3"><p className="flex items-start gap-2 rounded-xl bg-zinc-50 p-3 text-xs leading-relaxed text-zinc-600"><Info size={16} className="mt-0.5 shrink-0"/>{tr('workingHint')}</p><button className={`${button} w-full bg-zinc-950 text-white hover:bg-zinc-800`} disabled={busy||query.isError} onClick={()=>setEditing(true)}><Pencil size={16}/>{tr('edit')}</button></div></div>:<>
      <div className="space-y-2 border-y border-zinc-100 bg-zinc-50/70 px-4 py-3"><button className={`${button} !px-0 text-zinc-700`} disabled={busy||dirty} onClick={()=>setEditing(false)}><ArrowLeft size={15}/>{tr('overview')}</button>{badge(current)}<p className="text-xs leading-relaxed text-zinc-500">{tr('editHint')}</p></div>
      <div className="divide-y divide-slate-100">{offsets.map(o=>{
        const missing=baseline.values[o]==null||(o===10&&baseline.values[11]==null);
        const options=enumKeys[o] ? t(`physicalSchedule.${enumKeys[o]}`,{returnObjects:true}) as unknown as Record<string,string>:null;
        const changed=draft[o]!==displayValue(baseline.values,o);
        return <div key={o} className={`grid grid-cols-1 @[440px]:grid-cols-[minmax(0,1fr)_minmax(0,1.15fr)] items-center gap-2 px-4 py-3 ${changed?'bg-zinc-50':''}`}>
          <label htmlFor={`physical-${o}`} className="min-w-0"><span className="text-sm font-medium">{tr(`fields.${o}`)}</span><code className="mt-0.5 block text-[10px] text-slate-400">{names[o]} · +{o}{o===10?' / +11':''} · {hex(baseline.baseAddress+o)}</code></label>
          <div className="min-w-0">{o===1?<label className="flex items-center gap-2 py-2 text-sm"><input id={`physical-${o}`} type="checkbox" className="h-4 w-4 accent-zinc-950" disabled={missing||busy} checked={draft[o]==='1'} onChange={e=>setDraft({...draft,[o]:e.target.checked?'1':'0'})}/>Selected</label>:options?<select id={`physical-${o}`} className={input} value={draft[o]} disabled={missing||busy} onChange={e=>setDraft({...draft,[o]:e.target.value})}>
            {!Object.prototype.hasOwnProperty.call(options,draft[o])&&<option value={draft[o]}>{draft[o]||'—'}</option>}{Object.entries(options).map(([v,label])=><option key={v} value={v}>{v} · {label}</option>)}</select>:
          <input id={`physical-${o}`} className={input} type={o>=3&&o<=6?'text':'number'} step={o===10?'any':o===14||o===17?'0.1':'1'} placeholder={missing?'—':o===3||o===4?'MM-DD':o===5||o===6?'HH:mm':undefined} disabled={missing||busy} value={draft[o]??''} onChange={e=>setDraft({...draft,[o]:e.target.value})}/>}
          {o===2&&<div className="mt-2 flex flex-wrap gap-1">{(t('physicalSchedule.days',{returnObjects:true}) as unknown as string[]).map((day,i)=><button key={i} type="button" disabled={missing||busy} aria-pressed={(Number(draft[o])&(1<<i))!==0} className={`rounded-md px-1.5 py-1 text-[10px] transition-colors duration-150 motion-reduce:transition-none ${(Number(draft[o])&(1<<i))?'bg-slate-800 text-white':'bg-slate-100 text-slate-500'}`} onClick={()=>setDraft({...draft,[o]:String(Number(draft[o])^(1<<i))})}>{day}</button>)}</div>}
          <p className="mt-1 text-[10px] leading-relaxed text-slate-400">{tr('live')} {raw(live??baseline,o)}{o===10?` / ${raw(live??baseline,11)}`:''}{o===2?` · ${tr('dailyHint')}`:o===8?` · ${tr('groupHint')}`:o===3||o===4?` · MM-DD`:''}</p>
          </div>
        </div>;
      })}</div>
      <div className="space-y-3 border-t border-slate-100 bg-slate-50/80 p-4">
        {validation&&<p role="alert" className="text-sm text-rose-600">{tr('invalid')} {validation}</p>}
        <div className="flex flex-wrap gap-2"><button className={`${button} flex-1 bg-zinc-950 text-white hover:bg-zinc-800`} disabled={busy||!changes.length||!!validation||query.isError} onClick={()=>void save()}>{busy?<RefreshCw size={16} className="animate-spin motion-reduce:animate-none"/>:<Save size={16}/>} {tr('save')}</button>
        <button className={`${button} border border-slate-200 bg-white hover:bg-slate-100`} disabled={busy||!live} onClick={()=>live&&select(live)}>{dirty?tr('discard'):tr('reread')}</button></div>
        {!dirty&&<p className="flex items-center gap-1 text-xs text-slate-400"><Check size={12}/>{tr('noChanges')}</p>}
      </div>
    </>}</div>}
  </section>;
}
