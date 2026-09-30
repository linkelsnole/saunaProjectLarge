import { useEffect, useState } from 'react';
import { RefreshCw, History, Check } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/shared/ui/dialog';
import { panelVersionHistory, restorePanelVersion, type PanelVersion } from '@/api/panelUpdates';

export function PanelVersionHistory({open,onOpenChange}:{open:boolean;onOpenChange:(open:boolean)=>void}) {
  const {t}=useTranslation(); const tr=(key:string)=>t(`panelHistory.${key}`);
  const [versions,setVersions]=useState<PanelVersion[]>([]);
  const [loading,setLoading]=useState(false); const [busy,setBusy]=useState(false);
  const [error,setError]=useState(''); const [selected,setSelected]=useState<PanelVersion|null>(null);
  const button='inline-flex min-h-10 items-center justify-center gap-2 rounded-xl border border-zinc-200 px-4 text-sm transition-colors duration-150 disabled:opacity-40';
  async function load(){
    setLoading(true);setError('');
    try {const data=await panelVersionHistory();setVersions(data.versions);if(!data.enabled)setError(tr('unavailable'));}
    catch {setError(tr('loadError'));} finally{setLoading(false);}
  }
  useEffect(()=>{if(open){setSelected(null);void load();}},[open]);
  async function restore(){
    if(!selected||busy)return;
    setBusy(true);setError('');
    try {
      await restorePanelVersion(selected.version);
      window.dispatchEvent(new CustomEvent('panel-update-requested',{detail:selected.version}));
      onOpenChange(false);
    } catch {setError(tr('restoreError'));} finally {setBusy(false);}
  }
  const date=(value:number)=>new Date(value*1000).toLocaleString();
  return <Dialog open={open} onOpenChange={value=>{if(!busy)onOpenChange(value);}}>
    <DialogContent className="max-h-[85dvh] overflow-y-auto rounded-3xl border-zinc-200 bg-white p-6 text-zinc-950 sm:max-w-xl">
      <DialogHeader><DialogTitle className="flex items-center gap-2"><History size={20}/>{tr('title')}</DialogTitle><DialogDescription>{tr('description')}</DialogDescription></DialogHeader>
      {error&&<p role="alert" className="text-sm text-rose-600">{error}</p>}
      {selected ? <div className="space-y-4">
        <h3 className="font-medium">{tr('confirm')}</h3><p className="text-sm">{selected.description}</p>
        <p className="rounded-xl bg-amber-50 p-4 text-sm leading-relaxed text-amber-950">{tr('databaseWarning')} {selected.databaseSavedAt?date(selected.databaseSavedAt):''}</p>
        <div className="flex flex-wrap justify-end gap-2"><button className={button} disabled={busy} onClick={()=>setSelected(null)}>{tr('cancel')}</button><button className={`${button} bg-zinc-950 text-white`} disabled={busy} onClick={()=>void restore()}>{busy&&<RefreshCw size={16} className="animate-spin"/>}{tr('restore')}</button></div>
      </div> : <>
        <div className="flex justify-end"><button className={button} disabled={loading} onClick={()=>void load()}><RefreshCw size={16} className={loading?'animate-spin':''}/>{tr('refresh')}</button></div>
        {!loading&&!versions.length&&<p className="py-6 text-center text-sm text-zinc-500">{tr('empty')}</p>}
        <div className="divide-y divide-zinc-100">{versions.map(version=><section key={version.version} className="space-y-3 py-4">
          <div className="flex flex-wrap items-center justify-between gap-2"><span className="text-xs text-zinc-500">{date(version.installedAt)}</span>{version.current?<span className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2 py-1 text-xs text-emerald-700"><Check size={13}/>{tr('current')}</span>:<button className={button} disabled={!version.restorable} onClick={()=>setSelected(version)}>{tr('choose')}</button>}</div>
          <p className="whitespace-pre-line break-words text-sm leading-relaxed">{version.description}</p><p className="break-all font-mono text-[11px] text-zinc-400">{version.commit?.slice(0,7)||version.version.slice(0,18)}</p>
        </section>)}</div>
      </>}
    </DialogContent>
  </Dialog>;
}
