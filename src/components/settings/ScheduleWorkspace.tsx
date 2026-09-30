import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useAuth } from '@/contexts/AuthContext';
import { ScheduleSettingsWidget } from './ScheduleSettingsWidget';
import { PhysicalScheduleEditor } from './PhysicalScheduleEditor';
export function ScheduleWorkspace() {
  const {role}=useAuth();
  const {t}=useTranslation();
  const [physical,setPhysical]=useState(false);
  if(role!=='ROLE_ADMIN') return <ScheduleSettingsWidget/>;
  return <div className="space-y-4"><div className="flex rounded-2xl bg-slate-100 p-1" role="group" aria-label={t('app.tabs.schedule')}>
    {[false,true].map(value=><button key={String(value)} aria-pressed={physical===value} className={`flex-1 rounded-xl px-3 py-2 text-sm font-medium transition-colors duration-150 motion-reduce:transition-none focus-visible:ring-2 focus-visible:ring-zinc-400 ${physical===value?'bg-zinc-950 text-white shadow-sm':'text-slate-500 hover:text-slate-800'}`} onClick={()=>setPhysical(value)}>{t(`physicalSchedule.${value?'title':'normal'}`)}</button>)}
  </div><div hidden={physical}><ScheduleSettingsWidget/></div>{physical&&<PhysicalScheduleEditor/>}</div>;
}
