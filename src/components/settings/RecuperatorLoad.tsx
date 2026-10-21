import { Fan, Droplets } from 'lucide-react';
export function RecuperatorLoad({ type, percent }: { type: 'rotor' | 'glycol'; percent: number | null }) {
 const Icon = type === 'rotor' ? Fan : Droplets;
 return <section aria-label="Тип и работа рекуператора" className="rounded-2xl border border-slate-200 bg-white p-4">
  <div className="flex flex-wrap items-center gap-3">
   <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-slate-50 text-slate-600"><Icon size={21}/></span>
   <div className="min-w-0 flex-1"><h3 className="text-base font-semibold text-slate-900">{type === 'rotor' ? 'Роторный' : 'Гликолевый'}</h3><p className="mt-0.5 text-xs text-slate-500">{type === 'rotor' ? 'Текущие обороты от максимальных' : 'Работа насоса'}</p></div>
   <strong className="whitespace-nowrap text-2xl font-semibold tabular-nums text-slate-900">{percent == null ? '—' : `${percent.toLocaleString('ru-RU')} %`}</strong>
  </div>
  {percent == null ? <p className="mt-3 text-xs text-slate-500">Нет данных для расчёта</p> : <div aria-hidden="true" className="mt-3 h-1.5 overflow-hidden rounded-full bg-slate-100"><div className="h-full rounded-full bg-emerald-500 transition-[width] duration-300 motion-reduce:transition-none" style={{width:`${Math.min(100, Math.max(0,percent))}%`}}/></div>}
 </section>;
}
