import { useEffect, useRef, useState } from 'react';
import { ArrowUpRight, LoaderCircle, TriangleAlert } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useAuth } from '@/contexts/AuthContext';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/shared/ui/dialog';
import { panelUpdateStatus, updateOutcome, type PanelUpdateStatus } from '@/api/panelUpdates';

const action = 'inline-flex min-h-11 items-center justify-center gap-2 rounded-xl px-5 text-sm font-medium transition duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-zinc-400 focus-visible:ring-offset-2 disabled:opacity-40 motion-reduce:transition-none';

export function PanelUpdateNotice() {
  const { role } = useAuth();
  return role === 'ROLE_ADMIN' ? <AdminUpdateNotice /> : null;
}

function AdminUpdateNotice() {
  const { t } = useTranslation();
  const [state, setState] = useState<PanelUpdateStatus>({ enabled: false });
  const [dismissed, setDismissed] = useState('');
  const [requested, setRequested] = useState('');
  const [slow, setSlow] = useState(false);
  const [failed, setFailed] = useState(false);
  const started = useRef(0);
  const installed = useRef<string | null>(null);
  useEffect(()=>{
    const receive=(event:Event)=>{started.current=Date.now();setFailed(false);setRequested((event as CustomEvent<string>).detail);};
    window.addEventListener('panel-update-requested',receive);
    return ()=>window.removeEventListener('panel-update-requested',receive);
  },[]);
  const busy = Boolean(requested) && !failed;
  const tr = (key: string) => t(`panelUpdate.${key}`);

  useEffect(() => {
    let disposed = false;
    let timer: ReturnType<typeof setTimeout>;
    async function poll() {
      try {
        const next = await panelUpdateStatus();
        if (disposed) return;
        setState(next);
        if(next.installedVersion){
          if(installed.current!==null && installed.current!==next.installedVersion){window.location.reload();return;}
          installed.current=next.installedVersion;
        }
        if (!requested && next.version && (next.status === 'queued' || next.status === 'installing')) {
          started.current = Date.now();
          setRequested(next.version);
        }
        const outcome = updateOutcome(next, requested);
        if (outcome === 'reload') { window.location.reload(); return; }
        if (outcome === 'failed') setFailed(true);
      } catch {
        // During restart the server is deliberately unavailable. Keep the page and modal mounted.
      } finally {
        if (!disposed) {
          if (requested && Date.now() - started.current > 180_000) setSlow(true);
          timer = setTimeout(() => { void poll(); }, requested ? 3000 : 5000);
        }
      }
    }
    void poll();
    return () => { disposed = true; clearTimeout(timer); };
  }, [requested, dismissed]);

  function close() {
    if (busy && !slow) return;
    setDismissed(state.version ?? '');
    setRequested('');
    setFailed(false);
    setSlow(false);
  }

  return <Dialog open={Boolean(requested)} onOpenChange={open => { if (!open) close(); }}>
    <DialogContent className={`gap-6 rounded-3xl border-zinc-200 bg-white p-7 text-zinc-950 sm:max-w-[440px] duration-150 motion-reduce:animate-none ${busy && !slow ? '[&>button:last-child]:hidden' : ''}`}
      onEscapeKeyDown={event => { if (busy && !slow) event.preventDefault(); }}
      onInteractOutside={event => { if (busy && !slow) event.preventDefault(); }}>
      <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-zinc-100" aria-hidden="true">
        {failed ? <TriangleAlert size={23} /> : busy ? <LoaderCircle size={23} className="animate-spin motion-reduce:animate-none" /> : <ArrowUpRight size={24} />}
      </div>
      <DialogHeader className="gap-3 text-left">
        <DialogTitle className="text-xl tracking-tight">{tr(failed ? 'failedTitle' : busy ? 'busyTitle' : 'title')}</DialogTitle>
        <DialogDescription className="text-sm leading-6 text-zinc-500" aria-live="polite">
          {tr(failed ? 'failedBody' : busy ? slow ? 'slowBody' : 'busyBody' : 'body')}
        </DialogDescription>
      </DialogHeader>
      {busy ? <div className="text-xs text-zinc-500" role="status">{tr(slow ? 'stillWaiting' : 'waiting')}</div> :
        <div className="flex justify-end gap-2">
          <button className={`${action} border border-zinc-200 bg-white hover:bg-zinc-50`} onClick={close}>{tr('close')}</button>
        </div>}
    </DialogContent>
  </Dialog>;
}
