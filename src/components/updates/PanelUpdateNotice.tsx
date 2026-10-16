import { useEffect, useState } from 'react';
import { LoaderCircle } from 'lucide-react';
import { checkPlatform, platformBuildId, platformIsBlocked } from '@/api/platformUpdate';

/** Runs for every role. Deployment itself belongs to the server updater. */
export function PanelUpdateNotice() {
  const [busy, setBusy] = useState(platformIsBlocked);
  const [slow, setSlow] = useState(false);
  useEffect(() => {
    let disposed = false;
    let timer: ReturnType<typeof setTimeout>;
    let checking = false;
    let reloading = false;
    let started = 0;
    let lastCheck = 0;
    const show = () => {
      if (!started) started = Date.now();
      setBusy(true);
    };
    async function poll() {
      if (checking || disposed || reloading) return;
      checking = true;
      lastCheck = Date.now();
      clearTimeout(timer);
      try {
        const state = await checkPlatform();
        if (disposed) return;
        if (!state.updating && platformIsBlocked()) {
          // Success, rollback, or a completed update missed while this tab slept.
          // The endpoint is served by the running application, not a timer or a release label.
          if (state.buildId && platformBuildId) {
            reloading = true;
            window.location.reload();
          }
        }
      } catch {
        // Connection loss alone is not evidence of an update. Keep an existing overlay.
      } finally {
        checking = false;
        if (!disposed && !reloading) {
          if (started && Date.now() - started > 180_000) setSlow(true);
          timer = setTimeout(() => { void poll(); }, platformIsBlocked() ? 2000 : 5000);
        }
      }
    }
    const interaction = (event: Event) => {
      if (platformIsBlocked()) { event.preventDefault(); event.stopImmediatePropagation(); show(); }
      if (Date.now() - lastCheck > 1000) void poll();
    };
    const wake = () => { if (!document.hidden) void poll(); };
    window.addEventListener('platform-updating', show);
    window.addEventListener('focus', wake);
    document.addEventListener('visibilitychange', wake);
    for (const type of ['pointerdown', 'click', 'keydown', 'submit']) window.addEventListener(type, interaction, true);
    void poll();
    return () => {
      disposed = true;
      clearTimeout(timer);
      window.removeEventListener('platform-updating', show);
      window.removeEventListener('focus', wake);
      document.removeEventListener('visibilitychange', wake);
      for (const type of ['pointerdown', 'click', 'keydown', 'submit']) window.removeEventListener(type, interaction, true);
    };
  }, []);
  if (!busy) return null;
  return <div className="fixed inset-0 z-[1000] flex items-center justify-center bg-slate-950/35 p-5 backdrop-blur-sm" role="alertdialog" aria-modal="true" aria-labelledby="platform-update-title" aria-describedby="platform-update-description">
    <div className="w-full max-w-sm rounded-3xl border border-slate-200 bg-white p-8 text-center shadow-xl">
      <LoaderCircle className="mx-auto mb-5 h-8 w-8 animate-spin text-slate-700 motion-reduce:animate-none" aria-hidden="true" />
      <h2 id="platform-update-title" className="text-xl font-semibold text-slate-900">Обновляется платформа</h2>
      <p id="platform-update-description" className="mt-3 text-sm leading-6 text-slate-500" aria-live="polite">{slow ? 'Ожидаем восстановления связи с сервером. Страница обновится автоматически.' : 'Подождите немного. Страница обновится автоматически.'}</p>
    </div>
  </div>;
}
