import { useState, useEffect } from 'react';
import { useDashboardData } from '@/api/useDashboard';
import { useUpdateTempSettings } from '@/api/hooks';
import { Badge } from '@/shared/ui/badge'; 
import { useAuth } from '@/contexts/AuthContext';
import { GlassCard } from '@/shared/ui/GlassCard';
import { NeoInput } from '@/shared/ui/NeoInput';
import { useTranslation } from 'react-i18next';


interface TempInputProps {
  label: string;
  value: number | null | undefined;
  onChange: (val: number) => void;
  unit?: string;
  decimals?: number;
  readOnly?: boolean;
  inputClassName?: string;
}

const TempInput = ({
  label,
  value,
  onChange,
  unit = '°C',
  decimals = 1,
  readOnly = false,
  inputClassName,
}: TempInputProps) => {
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
      <span className="text-[10px] sm:text-[11px] text-slate-500 font-semibold uppercase tracking-wide px-1 whitespace-normal break-words" title={label}>
        {label}
      </span>
      <div className="relative">
        <NeoInput
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
          className={`${readOnly ? 'text-slate-500 border-transparent bg-transparent' : 'text-slate-800'} pr-9 text-base sm:text-lg h-10 ${inputClassName ?? ''}`}
        />
        <span className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 text-xs font-medium pointer-events-none">
          {unit}
        </span>
      </div>
    </div>
  );
};

// Компонент для отображения Read-Only данных (вентили)
const ValveMonitorRow = ({ label, value, unit = '%' }: { label: string; value: string | null | undefined; unit?: string }) => (
  <div className="flex justify-between items-center py-2 px-3 rounded-lg bg-white border border-slate-200 mb-1 last:mb-0">
    <span className="text-[10px] text-slate-500 font-semibold uppercase tracking-wide">{label}</span>
    <span className="text-sm font-semibold tabular-nums text-slate-800">
      {value ?? '-'} <span className="text-[10px] text-slate-400">{unit}</span>
    </span>
  </div>
);

const getModeLabelKey = (mode: number | null | undefined): string => {
  if (mode === null || mode === undefined) return 'tempWidget.modes.na';
  switch (mode) {
    case 0: return 'tempWidget.modes.off';
    case 1: return 'tempWidget.modes.neutralZone';
    case 2: return 'tempWidget.modes.nzAfterHeating';
    case 3: return 'tempWidget.modes.nzAfterCooling';
    case 4: return 'tempWidget.modes.cooling';
    case 5: return 'tempWidget.modes.heating';
    case 6: return 'tempWidget.modes.tAveraging';
    case 7: return 'tempWidget.modes.hood';
    case 8: return 'tempWidget.modes.alg2Step0';
    case 9: return 'tempWidget.modes.alg2Tuning';
    case 10: return 'tempWidget.modes.alg2Stabilization';
    case 11: return 'tempWidget.modes.alg2HeatOff';
    case 12: return 'tempWidget.modes.alg2HeatInsuff';
    default: return 'tempWidget.modes.unknown';
  }
};

const getModeColor = (mode: number | null | undefined): string => {
  if (mode === null || mode === undefined) return 'bg-slate-100 text-slate-500';
  switch (mode) {
    case 0: return 'bg-slate-100 text-slate-600';
    case 1: return 'bg-slate-100 text-slate-600';
    case 2: return 'bg-orange-100 text-orange-700';
    case 3: return 'bg-cyan-100 text-cyan-700';
    case 4: return 'bg-blue-100 text-blue-700';
    case 5: return 'bg-red-100 text-red-700';
    case 6: return 'bg-violet-100 text-violet-700';
    case 7: return 'bg-teal-100 text-teal-700';
    case 8: return 'bg-slate-100 text-slate-600';
    case 9: return 'bg-amber-100 text-amber-700';
    case 10: return 'bg-emerald-100 text-emerald-700';
    case 11: return 'bg-slate-100 text-slate-600';
    case 12: return 'bg-orange-100 text-orange-700';
    default: return 'bg-slate-100 text-slate-500';
  }
};


export const TempSettingsWidget = () => {
  const { t } = useTranslation();
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
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 animate-in fade-in fill-mode-backwards duration-500 motion-reduce:animate-none">
        {/* ЗОНА 1 */}
        <GlassCard className="flex flex-col h-full">
          <div className="p-4 border-b border-slate-200 flex justify-between items-center bg-slate-50">
            <h4 className="text-sm font-semibold text-slate-700 flex items-center gap-2">
              <div className="w-2 h-2 bg-slate-800 rounded-full" />
              {t('tempWidget.zone1')}
            </h4>
            <Badge
              className={`${getModeColor(tempSettings?.ctrlModeZone1)} px-3 py-1 rounded-full text-[10px] font-semibold uppercase tracking-wide border border-slate-200`}
            >
              {t(getModeLabelKey(tempSettings?.ctrlModeZone1))}
            </Badge>
          </div>
          <div className="p-5 flex-1 space-y-6">
            <TempInput
              label={t('tempWidget.target')}
              value={tempSettings?.targetZone1}
              onChange={(val) => mutate({ targetZone1: val })}
              decimals={0}
              readOnly={!canEditZones}
              inputClassName="h-16 text-3xl tracking-tight text-center"
            />
            <div className="bg-slate-50 rounded-xl p-3 border border-slate-200">
              <ValveMonitorRow label={t('tempWidget.hotValve')} value={getSensorVal('VHP1')} />
              <ValveMonitorRow label={t('tempWidget.coldValve')} value={getSensorVal('VCP1')} />
              <ValveMonitorRow label={t('tempWidget.hotValveExtra')} value={getSensorVal('VOP1')} />
            </div>
          </div>
        </GlassCard>

        {/* ЗОНА 2 */}
        <GlassCard className="flex flex-col h-full">
          <div className="p-4 border-b border-slate-200 flex justify-between items-center bg-slate-50">
            <h4 className="text-sm font-semibold text-slate-700 flex items-center gap-2">
              <div className="w-2 h-2 bg-slate-800 rounded-full" />
              {t('tempWidget.zone2')}
            </h4>
            <Badge
              className={`${getModeColor(tempSettings?.ctrlModeZone2)} px-3 py-1 rounded-full text-[10px] font-semibold uppercase tracking-wide border border-slate-200`}
            >
              {t(getModeLabelKey(tempSettings?.ctrlModeZone2))}
            </Badge>
          </div>
          <div className="p-5 flex-1 space-y-6">
            <TempInput
              label={t('tempWidget.target')}
              value={tempSettings?.targetZone2}
              onChange={(val) => mutate({ targetZone2: val })}
              decimals={0}
              readOnly={!canEditZones}
              inputClassName="h-16 text-3xl tracking-tight text-center"
            />
            <div className="bg-slate-50 rounded-xl p-3 border border-slate-200">
              <ValveMonitorRow label={t('tempWidget.hotValve')} value={getSensorVal('VHP2')} />
              <ValveMonitorRow label={t('tempWidget.coldValve')} value={getSensorVal('VCP2')} />
              <ValveMonitorRow label={t('tempWidget.hotValveExtra')} value={getSensorVal('VOP2')} />
            </div>
          </div>
        </GlassCard>
      </div>

      <GlassCard className="p-4 w-full animate-in fade-in fill-mode-backwards duration-500 [animation-delay:80ms] motion-reduce:animate-none">
        <h4 className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-3 flex items-center gap-2">
          <span className="w-1.5 h-1.5 rounded-full bg-violet-400" />
          {t('tempWidget.temperatureSection')}
        </h4>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <TempInput
            label={t('tempWidget.valveStartupPosition')}
            value={tempSettings?.valveStartupPosition ?? undefined}
            onChange={(val) => mutate({ valveStartupPosition: Math.round(val) })}
            unit=""
            decimals={0}
            readOnly={!canEditAll}
          />
          {/* <TempInput
            label="Алгоритм управления (0x00DD)"
            value={tempSettings?.controlAlgorithm ?? undefined}
            onChange={(val) => mutate({ controlAlgorithm: Math.round(val) })}
            unit=""
            decimals={0}
            readOnly={!canEditAll}
          />
          <TempInput
            label="Интервал слежения по T (0x007E)"
            value={tempSettings?.tempTrackingInterval ?? undefined}
            onChange={(val) => mutate({ tempTrackingInterval: Math.round(val) })}
            unit=""
            decimals={0}
            readOnly={!canEditAll}
          /> */}
        </div>
      </GlassCard>

      {/* === СЕКЦИЯ 2: ПОРОГИ БЕЗОПАСНОСТИ (Вода и Приток) === */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 animate-in fade-in fill-mode-backwards duration-500 [animation-delay:160ms] motion-reduce:animate-none">
        {/* Пороги Воды */}
        <GlassCard className="p-4">
          <h4 className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-3 flex items-center gap-2">
            <span className="w-1.5 h-1.5 rounded-full bg-blue-400"></span> {t('tempWidget.waterThresholds')}
          </h4>
          <div className="grid grid-cols-2 gap-3">
            <TempInput
              label={t('tempWidget.rpmReduction')}
              value={tempSettings?.waterWarnThreshold}
              onChange={(val) => mutate({ waterWarnThreshold: val })}
              readOnly={!canEditAll}
            />
            <TempInput
              label={t('tempWidget.failure')}
              value={tempSettings?.waterFailThreshold}
              onChange={(val) => mutate({ waterFailThreshold: val })}
              readOnly={!canEditAll}
            />
          </div>
        </GlassCard>

        {/* Пороги Притока */}
        <GlassCard className="p-4">
          <h4 className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-3 flex items-center gap-2">
             <span className="w-1.5 h-1.5 rounded-full bg-emerald-400"></span> {t('tempWidget.supplyThresholds')}
          </h4>
          <div className="grid grid-cols-2 gap-3">
            <TempInput
              label={t('tempWidget.rpmReduction')}
              value={tempSettings?.inflowWarnThreshold}
              onChange={(val) => mutate({ inflowWarnThreshold: val })}
              readOnly={!canEditAll}
            />
            <TempInput
              label={t('tempWidget.failure')}
              value={tempSettings?.inflowFailThreshold}
              onChange={(val) => mutate({ inflowFailThreshold: val })}
              readOnly={!canEditAll}
            />
          </div>
        </GlassCard>
      </div>

      {/* === СЕКЦИЯ 3: ОТЛАДКА И НОВЫЕ ПАРАМЕТРЫ === */}
      <GlassCard className="p-4 animate-in fade-in fill-mode-backwards duration-500 [animation-delay:240ms] motion-reduce:animate-none">
        <div className="flex items-center gap-2 mb-4">
           <div className="bg-amber-400 w-1 h-4 rounded-full"></div>
           <h4 className="text-xs font-semibold text-slate-700 uppercase tracking-wide">
             {t('tempWidget.setpoints')}
           </h4>
        </div>
        
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-4">
           <TempInput
             label={t('tempWidget.neutralZoneT')}
             value={tempSettings?.neutralZoneT}
             onChange={(val) => mutate({ neutralZoneT: val })}
             readOnly={!canEditAll}
           />
           <TempInput
             label={t('tempWidget.hotValveMin')}
             value={tempSettings?.hotValveMinProc}
             onChange={(val) => mutate({ hotValveMinProc: val })}
             unit="%"
             readOnly={!canEditAll}
           />
           <TempInput
             label={t('tempWidget.waterMinTNoWork')}
             value={tempSettings?.waterMinTNoWork}
             onChange={(val) => mutate({ waterMinTNoWork: val })}
             readOnly={!canEditAll}
           />
           <TempInput
             label={t('tempWidget.waterMinTWork')}
             value={tempSettings?.waterMinTWork}
             onChange={(val) => mutate({ waterMinTWork: val })}
             readOnly={!canEditAll}
           />
        </div>

        {/* Аварийный мониторинг радиатора */}
        <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 flex justify-between items-center">
            <span className="text-xs font-semibold text-slate-700 uppercase">
              {t('tempWidget.radiatorFail')}
            </span>
            <span className="text-base font-semibold tabular-nums text-slate-800 bg-white px-3 py-1 rounded-lg border border-slate-200">
               {tempSettings?.radTempFail ?? '-'} °C
            </span>
        </div>
      </GlassCard>

      
    </div>
  );
};