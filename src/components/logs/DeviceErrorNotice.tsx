import { useAuth } from '@/contexts/AuthContext';
import { useSystemEvents } from '@/api/useSystemEvents';
import { DeviceErrorSummary } from './DeviceErrorSummary';
import { useTranslation } from 'react-i18next';

function Notice() {
    const query = useSystemEvents(1440);
    const { t } = useTranslation();
    return <div className="shrink-0 px-3 pt-3 empty:hidden">
        {query.isError && <p role="status" className="mb-2 text-xs text-amber-800">{t('eventsJournal.deviceStale')}</p>}
        <DeviceErrorSummary events={query.data ?? []} stale={query.isError} />
    </div>;
}

export function DeviceErrorNotice() {
    const { role } = useAuth();
    // Keep the existing journal permissions: other roles never issue this request.
    return role === 'ROLE_ADMIN' ? <Notice /> : null;
}
