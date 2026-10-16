import { Thermometer, Droplets, Gauge, ChevronDown, AlertCircle } from 'lucide-react';
import { useDashboardData } from '@/api/useDashboard';
import { alarmDuration, deviceDescriptions, deviceStates } from '@/schema/deviceLabels';

const groups = [
  { title: 'Воздух', code: 'T', description: 'Температура и влажность', icon: Thermometer, ids: ['T1', 'T2', 'T3', 'T4_1', 'T4_2', 'T5', 'TTR'] },
  { title: 'Водяной контур', code: 'TH / TC', description: 'Нагрев и охлаждение', icon: Droplets, ids: ['TH1', 'TC1', 'TH2', 'TC2'] },
  { title: 'Давление', code: 'BF / BP', description: 'Вентиляторы, фильтры, рекуператор', icon: Gauge, ids: ['BF11', 'BF12', 'BF21', 'BF22', 'BP1', 'BP2', 'BP10', 'BP5'] },
];
export function DeviceStatePanel() {
  const { data, error, isLoading } = useDashboardData();
  const devices = data?.deviceStates ?? [];
  const mode = (value: number | null | undefined) => !error && value != null ? deviceStates[value] : undefined;
  const normal = devices.filter(d => mode(d.alarmMode) && d.alarmMode === 0).length;
  const abnormal = devices.filter(d => mode(d.alarmMode) && d.alarmMode !== 0);
  const unknown = devices.length - normal - abnormal.length;
  const known = new Set(groups.flatMap(g => g.ids));
  const sections = [...groups, { title: 'Другие датчики', code: '', description: 'Состояние оборудования', icon: Gauge, ids: devices.filter(d => !known.has(d.id)).map(d => d.id) }];
  return <section aria-label="Состояние датчиков" className="@container rounded-[24px] border border-slate-200/80 bg-white p-4 shadow-[0_8px_30px_-22px_rgba(15,23,42,.25)] sm:p-5">
    <header className="mb-5">
      <h3 className="text-xl font-semibold tracking-tight text-slate-950">Состояние датчиков</h3>
      <p className="mt-1 text-xs leading-5 text-slate-500" aria-live="polite">{devices.length ? `${normal} в норме · Отклонений: ${abnormal.length}${unknown ? ` · Нет данных: ${unknown}` : ''}` : isLoading ? 'Загрузка…' : 'Нет данных о состоянии датчиков'}</p>
    </header>
    {error && <p role="alert" className="mb-4 rounded-xl bg-slate-50 p-3 text-xs text-slate-600">Нет связи. Состояния датчиков не подтверждены.</p>}
    <div className="grid min-w-0 grid-cols-1 gap-3 @[660px]:grid-cols-3">
      {sections.filter(group => group.ids.some(id => devices.some(d => d.id === id))).map(group => <section key={group.title} aria-label={group.title} className="min-w-0 rounded-[22px] border border-slate-100 bg-gradient-to-b from-slate-50/80 to-white p-3">
        <div className="mb-4 flex items-center gap-3 px-1 pt-1">
          <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl border border-white bg-white text-slate-500 shadow-sm"><group.icon size={20} strokeWidth={1.6} /></span>
          <div className="min-w-0"><p className="text-[10px] font-semibold tracking-wider text-slate-400">{group.code}</p><h4 className="text-sm font-semibold text-slate-900">{group.title}</h4><p className="mt-0.5 text-[10px] leading-4 text-slate-500">{group.description}</p></div>
        </div>
        <div className="space-y-2">{group.ids.map(id => {
          const device = devices.find(d => d.id === id);
          if (!device) return null;
          const state = mode(device.alarmMode);
          const active = state && device.alarmMode !== 0;
          const duration = active ? alarmDuration(device.alarmSeconds) : '';
          return <details key={id} className={`group rounded-2xl border shadow-[0_2px_8px_-6px_rgba(15,23,42,.2)] transition-colors duration-200 motion-reduce:transition-none ${state?.row ?? 'border-slate-200 bg-slate-50 text-slate-500'}`}>
            <summary className="flex min-h-12 cursor-pointer list-none flex-wrap items-center gap-x-2 gap-y-1 rounded-2xl px-3 py-3 outline-none focus-visible:ring-2 focus-visible:ring-slate-400 [&::-webkit-details-marker]:hidden">
              <span className={`flex h-5 w-5 shrink-0 items-center justify-center rounded-full ${active ? state.dot + ' text-white' : 'bg-slate-50'}`} aria-hidden="true">{active ? <AlertCircle size={15} /> : <span className={`h-2.5 w-2.5 rounded-full ${state?.dot ?? 'bg-slate-300'}`} />}</span>
              <span className="text-xs font-semibold">{id}</span>
              <span className={`ml-auto text-[10px] ${active ? 'font-medium' : 'text-slate-400'}`}>{state?.label ?? 'Нет данных'}{duration && <span className="ml-1 tabular-nums">{duration}</span>}</span>
              <ChevronDown size={12} className="shrink-0 text-slate-400 transition-transform group-open:rotate-180 motion-reduce:transition-none" />
            </summary>
            <div className="border-t border-current/10 px-3 py-3 text-xs leading-5"><p>{deviceDescriptions[id] ?? id}</p>{duration && <p className="mt-1">Продолжительность: {duration} ({device.alarmSeconds} с)</p>}</div>
          </details>;
        })}</div>
      </section>)}
    </div>
    {abnormal.length > 0 && <div className="mt-4 space-y-2" aria-label="Активные отклонения">{abnormal.map(device => {
      const state = mode(device.alarmMode)!;
      return <div key={device.id} className={`flex flex-wrap items-center gap-2 rounded-2xl border px-4 py-3 text-xs ${state.row}`}><AlertCircle size={16} /><strong>{device.id}</strong><span>· {state.label}</span><span className="ml-auto tabular-nums">{alarmDuration(device.alarmSeconds)}</span></div>;
    })}</div>}
  </section>;
}
