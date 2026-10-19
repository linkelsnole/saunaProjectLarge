import { displayDeviceText, selectDevice } from '@/schema/deviceIdentity';
import { AlertTriangle } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import type { SystemEvent } from '@/api/typesApi';
import { eventMessage, eventSource, unresolvedDeviceErrors } from '@/api/systemEvents';

export function DeviceErrorSummary({ events, stale = false }: { events: SystemEvent[]; stale?: boolean }) {
    const { t } = useTranslation();
    const unresolved = unresolvedDeviceErrors(events);
    if (!unresolved.length) return null;
    return <section aria-label={t('eventsJournal.devicePending')} className="min-w-0 rounded-xl border border-rose-200 bg-rose-50 p-3 text-rose-900">
        <div className="flex items-start gap-2"><AlertTriangle size={17} className="mt-0.5 shrink-0" /><div className="min-w-0">
            <h4 className="text-sm font-semibold">{t('eventsJournal.devicePending')}</h4>
            <p className="mt-1 text-xs leading-relaxed">{t(stale ? 'eventsJournal.deviceStale' : 'eventsJournal.deviceWindowHint')}</p>
        </div></div>
        <ul className="mt-2 max-h-36 space-y-2 overflow-y-auto text-xs">
            {unresolved.map((event, index) => <li key={`${event.oid ?? ''}:${index}`} className="break-words [overflow-wrap:anywhere]">
                <button className="text-left font-semibold underline underline-offset-2" onClick={() => selectDevice(event.deviceError?.devName || event.devName || null)}>{displayDeviceText(eventSource(event))}</button>
                {event.deviceError?.devEvtRegName && <> · {event.deviceError.devEvtRegName}</>}
                <p className="mt-0.5 whitespace-pre-wrap">{eventMessage(event)}</p>
            </li>)}
        </ul>
    </section>;
}
