import { useState, useEffect } from 'react';
import { useDashboardData } from '@/api/useDashboard';
import { useUpdateTempSettings } from '@/api/hooks';
import { Input } from '@/shared/ui/input';
import { Badge } from '@/shared/ui/badge'; 
import { useAuth } from '@/contexts/AuthContext';



interface TempInputProps {
  label: string;
  value: number | null | undefined;
  onChange: (val: number) => void;
  unit?: string;
  decimals?: number;
  readOnly?: boolean;
}

const TempInput = ({ label, value, onChange, unit = '°C', decimals = 1, readOnly = false }: TempInputProps) => {
  const [localValue, setLocalValue] = useState('');
  const [isEditing, setIsEditing] = useState(false);

  useEffect(() => {
    if (!isEditing && value !== null && value !== undefined) {
      setLocalValue(value.toFixed(decimals));
    }
  }, [value, isEditing, decimals]);

  const handleBlur = () => {
    setIsEditing(false);
    const normalizedValue = localValue.replace(',', '.');
    const numVal = parseFloat(normalizedValue);

    if (!isNaN(numVal) && Math.abs(numVal - (value || 0)) > 0.01) {
      onChange(numVal);
    } else {
      if (value !== null && value !== undefined) {
        setLocalValue(value.toFixed(decimals));
      }
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') e.currentTarget.blur();
  };

  return (
    <div className="flex flex-col gap-1 w-full">
      <span className="text-[10px] sm:text-[11px] text-slate-500 font-medium uppercase tracking-wide px-1 truncate" title={label}>
        {label}
      </span>
      <div className="relative">
        <Input
          type="number"
          step={decimals === 0 ? '1' : '0.1'}
          value={localValue}
          onChange={(e) => {
            if (readOnly) return;
            setIsEditing(true);
            setLocalValue(e.target.value);
          }}
          onBlur={handleBlur}
          onKeyDown={handleKeyDown}
          readOnly={readOnly}
          onFocus={(e) => {
            if (readOnly) e.currentTarget.blur();
          }}
          tabIndex={readOnly ? -1 : 0}
          className="pr-8 font-mono text-base sm:text-lg font-bold border-slate-200 hover:border-slate-300 focus:border-blue-500 h-9 sm:h-10 text-slate-800 bg-white"
        />
        <span className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 text-xs font-medium">
          {unit}
        </span>
      </div>
    </div>
  );
};

// Компонент для отображения Read-Only данных (вентили)
const ValveMonitorRow = ({ label, value, unit = '%' }: { label: string; value: string | null | undefined; unit?: string }) => (
  <div className="flex justify-between items-center py-1.5 px-2 rounded hover:bg-slate-100/50 transition-colors">
    <span className="text-xs text-slate-500 font-medium">{label}</span>
    <span className="font-mono text-sm font-bold text-slate-700">
      {value ?? '-'} <span className="text-[10px] text-slate-400 font-normal">{unit}</span>
    </span>
  </div>
);

const getModeLabel = (mode: number | null | undefined): string => {
  if (mode === null || mode === undefined) return 'N/A';
  switch (mode) {
    case 0: return 'Нейтральная';
    case 1: return 'Нагрев';
    case 2: return 'Охлаждение';
    default: return 'Неизвестно';
  }
};

const getModeColor = (mode: number | null | undefined): string => {
  if (mode === null || mode === undefined) return 'bg-slate-100 text-slate-500';
  switch (mode) {
    case 0: return 'bg-slate-100 text-slate-600';
    case 1: return 'bg-red-100 text-red-700';
    case 2: return 'bg-blue-100 text-blue-700';
    default: return 'bg-slate-100 text-slate-500';
  }
};


export const TempSettingsWidget = () => {
  const { data } = useDashboardData();
  const { mutate } = useUpdateTempSettings();
  const { role } = useAuth();
  const isAdmin = role === 'ROLE_ADMIN';
  const isUser = role === 'ROLE_USER';
  const canEditZones = isAdmin || isUser;
  const canEditAll = isAdmin;

  const tempSettings = data?.tempSettings;
  const sensors = data?.debugValveSensors || [];

  const getSensorVal = (id: string) => sensors.find(s => s.id === id)?.value;

  return (
    <div className="space-y-6 pb-6">
      
      {/* === СЕКЦИЯ 1: ЗОНЫ (Управление + Мониторинг) === */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        
        {/* ЗОНА 1 */}
        <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden flex flex-col">
          <div className="p-3 bg-slate-50 border-b border-slate-100 flex justify-between items-center">
             <h4 className="text-xs font-bold text-slate-700 uppercase tracking-wider">Зона 1</h4>
             <Badge className={`${getModeColor(tempSettings?.ctrlModeZone1)} text-[10px] px-2 py-0.5 h-5`}>
                {getModeLabel(tempSettings?.ctrlModeZone1)}
             </Badge>
          </div>
          
          <div className="p-4 space-y-4 flex-1">
            {/* Input */}
            <TempInput
              label="Целевая температура"
              value={tempSettings?.targetZone1}
              onChange={(val) => mutate({ targetZone1: val })}
              decimals={0}
              readOnly={!canEditZones}
            />
            
            {/* Monitoring */}
            <div className="bg-slate-50 rounded-lg p-2 border border-slate-100">
               <div className="text-[10px] text-slate-400 uppercase font-bold mb-1 px-2">Мониторинг вентилей</div>
               <ValveMonitorRow label="Вентиль ГВ" value={getSensorVal('VHP1')} />
               <ValveMonitorRow label="Вентиль ХВ" value={getSensorVal('VCP1')} />
               <ValveMonitorRow label="Доп. открытие ГВ" value={getSensorVal('VOP1')} />
            </div>
          </div>
        </div>

        {/* ЗОНА 2 */}
        <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden flex flex-col">
          <div className="p-3 bg-slate-50 border-b border-slate-100 flex justify-between items-center">
             <h4 className="text-xs font-bold text-slate-700 uppercase tracking-wider">Зона 2</h4>
             <Badge className={`${getModeColor(tempSettings?.ctrlModeZone2)} text-[10px] px-2 py-0.5 h-5`}>
                {getModeLabel(tempSettings?.ctrlModeZone2)}
             </Badge>
          </div>
          
          <div className="p-4 space-y-4 flex-1">
            {/* Input */}
            <TempInput
              label="Целевая температура"
              value={tempSettings?.targetZone2}
              onChange={(val) => mutate({ targetZone2: val })}
              decimals={0}
              readOnly={!canEditZones}
            />

             {/* Monitoring */}
             <div className="bg-slate-50 rounded-lg p-2 border border-slate-100">
               <div className="text-[10px] text-slate-400 uppercase font-bold mb-1 px-2">Мониторинг вентилей</div>
               <ValveMonitorRow label="Вентиль ГВ" value={getSensorVal('VHP2')} />
               <ValveMonitorRow label="Вентиль ХВ" value={getSensorVal('VCP2')} />
               <ValveMonitorRow label="Доп. открытие ГВ" value={getSensorVal('VOP2')} />
            </div>
          </div>
        </div>
      </div>

      {/* === СЕКЦИЯ 2: ПОРОГИ БЕЗОПАСНОСТИ (Вода и Приток) === */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {/* Пороги Воды */}
        <div className="bg-white rounded-xl p-4 border border-slate-200 shadow-sm">
          <h4 className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-3 flex items-center gap-2">
            <span className="w-1.5 h-1.5 rounded-full bg-blue-400"></span> Пороги температуры воды
          </h4>
          <div className="grid grid-cols-2 gap-3">
            <TempInput
              label="Снижение оборотов"
              value={tempSettings?.waterWarnThreshold}
              onChange={(val) => mutate({ waterWarnThreshold: val })}
              readOnly={!canEditAll}
            />
            <TempInput
              label="Авария"
              value={tempSettings?.waterFailThreshold}
              onChange={(val) => mutate({ waterFailThreshold: val })}
              readOnly={!canEditAll}
            />
          </div>
        </div>

        {/* Пороги Притока */}
        <div className="bg-white rounded-xl p-4 border border-slate-200 shadow-sm">
          <h4 className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-3 flex items-center gap-2">
             <span className="w-1.5 h-1.5 rounded-full bg-emerald-400"></span> Пороги температуры ПВ
          </h4>
          <div className="grid grid-cols-2 gap-3">
            <TempInput
              label="Снижение оборотов"
              value={tempSettings?.inflowWarnThreshold}
              onChange={(val) => mutate({ inflowWarnThreshold: val })}
              readOnly={!canEditAll}
            />
            <TempInput
              label="Авария"
              value={tempSettings?.inflowFailThreshold}
              onChange={(val) => mutate({ inflowFailThreshold: val })}
              readOnly={!canEditAll}
            />
          </div>
        </div>
      </div>

      {/* === СЕКЦИЯ 3: ОТЛАДКА И НОВЫЕ ПАРАМЕТРЫ === */}
      <div className="bg-slate-50/80 rounded-xl border border-dashed border-slate-300 p-4">
        <div className="flex items-center gap-2 mb-4">
           <div className="bg-yellow-400 w-1 h-4 rounded-full"></div>
           <h4 className="text-xs font-bold text-slate-700 uppercase tracking-wide">
             Уставки
           </h4>
        </div>
        
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-4">
           <TempInput
             label="Нейтральная зона T"
             value={tempSettings?.neutralZoneT}
             onChange={(val) => mutate({ neutralZoneT: val })}
             readOnly={!canEditAll}
           />
           <TempInput
             label="Мин. % вентилей ГВ"
             value={tempSettings?.hotValveMinProc}
             onChange={(val) => mutate({ hotValveMinProc: val })}
             unit="%"
             readOnly={!canEditAll}
           />
           <TempInput
             label="Мин. Т ГВ при неработающих вент."
             value={tempSettings?.waterMinTNoWork}
             onChange={(val) => mutate({ waterMinTNoWork: val })}
             readOnly={!canEditAll}
           />
           <TempInput
             label="Мин. Т ГВ при работающих вент."
             value={tempSettings?.waterMinTWork}
             onChange={(val) => mutate({ waterMinTWork: val })}
             readOnly={!canEditAll}
           />
        </div>

        {/* Аварийный мониторинг радиатора */}
        <div className="rounded-lg border border-slate-200 p-3 flex justify-between items-center">
            <span className="text-xs font-bold text-slate-700 uppercase">
              Т радиатора - авария (E2,E8)
            </span>
            <span className="font-mono text-base font-bold text-slate-700 bg-white px-3 py-1 rounded shadow-sm border border-slate-200">
               {tempSettings?.radTempFail ?? '-'} °C
            </span>
        </div>
      </div>

    </div>
  );
};