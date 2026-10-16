import { useDashboardData } from '@/api/useDashboard';

const states = [
  { label: 'Норма', color: 'border-emerald-200 bg-emerald-50 text-emerald-800' },
  { label: 'Предупреждение', color: 'border-amber-300 bg-amber-50 text-amber-900' },
  { label: 'Неисправность', color: 'border-orange-300 bg-orange-50 text-orange-900' },
  { label: 'Авария', color: 'border-red-300 bg-red-50 text-red-800' },
];
export function DeviceStatePanel() {
  const { data, error, isLoading } = useDashboardData();
  return <section aria-label="Состояние датчиков" className="rounded-2xl border border-slate-200 bg-white p-4">
    <h3 className="text-base font-semibold">Состояние датчиков</h3>
    <p className="mt-1 text-xs text-slate-500">Текущее состояние и продолжительность ненормальной работы</p>
    {error && <p role="alert" className="mt-2 text-sm text-amber-800">Нет связи. Состояния датчиков не подтверждены.</p>}
    {!data?.deviceStates?.length && <p className="mt-3 text-sm text-slate-500">{isLoading ? 'Загрузка…' : 'Нет данных о состоянии датчиков'}</p>}
    <div className="mt-3 grid grid-cols-2 gap-2 sm:grid-cols-3">
      {data?.deviceStates?.map(device => {
        const state = !error && device.alarmMode != null ? states[device.alarmMode] : undefined;
        return <div key={device.id} className={`rounded-xl border p-3 transition-colors motion-reduce:transition-none ${state?.color ?? 'border-slate-200 bg-slate-50 text-slate-500'}`}>
          <strong className="text-sm">{device.id}</strong>
          <p className="mt-1 text-xs">{state?.label ?? 'Нет данных'}</p>
          {state && device.alarmMode !== 0 && device.alarmSeconds != null && <p className="mt-1 text-xs tabular-nums">{device.alarmSeconds} с</p>}
        </div>;
      })}
    </div>
  </section>;
}
