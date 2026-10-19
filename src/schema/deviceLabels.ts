export const climateDeviceNames: Record<string, string> = {
  BT3: 'T1', BT11: 'T2', BT10: 'T3', BT1: 'T4_1', BT21: 'T4_2', BT2: 'T5',
  BT8: 'TH1', BT9: 'TC1', BT24: 'TH2', BT27: 'TC2', BT6: 'TTR',
};
export const deviceDescriptions: Record<string, string> = {
  T1: 'Наружный воздух · вход притока', T2: 'Отработанный воздух · выход вытяжки',
  T3: 'Приточный воздух после рекуператора', T4_1: 'Приточный воздух · зона 1',
  T4_2: 'Приточный воздух · зона 2', T5: 'Отработанный воздух перед рекуператором',
  TH1: 'Водяной нагрев · зона 1', TC1: 'Водяное охлаждение · зона 1',
  TH2: 'Водяной нагрев · зона 2', TC2: 'Водяное охлаждение · зона 2',
  BF11: 'Давление · вентилятор M1_1', BF12: 'Давление · вентилятор M1_2',
  BF21: 'Давление · вентилятор M2_1', BF22: 'Давление · вентилятор M2_2',
  BP1: 'Давление · фильтр PF1', BP2: 'Давление · фильтр PF2',
  BP5: 'Давление рекуператора · ОВ–ВВ', BP10: 'Давление рекуператора · НВ–ОВ',
};
export const deviceStates = [
  { label: 'Норма', dot: 'bg-emerald-500', row: 'border-slate-100 bg-white text-slate-800', ink: '#059669', background: '#ffffff' },
  { label: 'Предупреждение', dot: 'bg-amber-400', row: 'border-amber-200 bg-amber-50 text-amber-900', ink: '#b45309', background: '#fffbeb' },
  { label: 'Неисправность', dot: 'bg-orange-500', row: 'border-orange-200 bg-orange-50 text-orange-900', ink: '#c2410c', background: '#fff7ed' },
  { label: 'Авария', dot: 'bg-red-500', row: 'border-red-200 bg-red-50 text-red-900', ink: '#b91c1c', background: '#fef2f2' },
];
export function alarmDuration(seconds: number | null | undefined): string {
  if (seconds == null || !Number.isFinite(seconds) || seconds < 0) return '';
  const total = Math.floor(seconds);
  const minutes = Math.floor(total / 60);
  return `${minutes.toString().padStart(2, '0')}:${(total % 60).toString().padStart(2, '0')}`;
}
