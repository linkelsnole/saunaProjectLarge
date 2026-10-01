import { useState, useEffect } from 'react';
import { useDashboardData } from '@/api/useDashboard';
import { useUpdateRecuperatorSettings } from '@/api/hooks';
import { Input } from '@/shared/ui/input';
import { useAuth } from '@/contexts/AuthContext';
import { useTranslation } from 'react-i18next';

const RecuperatorRow = ({ label, value, unit }: { label: string; value: number | null | undefined; unit?: string }) => {
  const { t } = useTranslation();
  const unitLabel = unit ?? t('units.rpm');

  return (
    <div className="flex justify-between items-center py-3 px-4 rounded-xl border border-slate-200 bg-slate-50 hover:bg-slate-100 transition-colors">
      <span className="text-sm text-slate-600 font-medium">{label}</span>
      <span className="text-lg font-semibold tabular-nums text-slate-800">
        {value != null ? value.toFixed(1) : '-'} <span className="text-xs text-slate-400 font-normal ml-1">{unitLabel}</span>
      </span>
    </div>
  );
};

const RecuperatorTextRow = ({ label, value }: { label: string; value: string }) => (
  <div className="flex justify-between items-center py-3 px-4 rounded-xl border border-slate-200 bg-slate-50 hover:bg-slate-100 transition-colors">
    <span className="text-sm text-slate-600 font-medium">{label}</span>
    <span className="text-lg font-semibold tabular-nums text-slate-800">{value}</span>
  </div>
);

interface RecuperatorInputProps {
  label: string;
  value: number | null | undefined;
  onChange: (val: number) => void;
  unit?: string;
  decimals?: number;
  readOnly?: boolean;
}

const RecuperatorInput = ({ label, value, onChange, unit, decimals = 1, readOnly = false }: RecuperatorInputProps) => {
  const { t } = useTranslation();
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
      <span className="text-[10px] sm:text-[11px] text-slate-500 font-semibold uppercase tracking-wide px-1 truncate" title={label}>
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
          className="no-spinner pr-8 text-base sm:text-lg font-semibold tabular-nums rounded-xl border-slate-200 hover:border-slate-300 h-9 sm:h-10 text-slate-800 bg-slate-50 focus:bg-white"
        />
        <span className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 text-xs font-medium">
          {unit ?? t('units.rpm')}
        </span>
      </div>
    </div>
  );
};

export const RecuperatorWidget = () => {
  const { t } = useTranslation();
  const { data } = useDashboardData();
  const { mutate } = useUpdateRecuperatorSettings();
  const { role } = useAuth();
  const isAdmin = role === 'ROLE_ADMIN';

  const recuperatorSettings = data?.recuperatorSettings;

  const getRecupStateColor = (code: number | null | undefined) => {
    if (code == null) return 'bg-slate-100 text-slate-500';
    switch (code) {
      case 0: return 'bg-slate-100 text-slate-600';
      case 1: return 'bg-emerald-100 text-emerald-700';
      case 2: return 'bg-amber-100 text-amber-700';
      case 3: return 'bg-blue-100 text-blue-700';
      case 4: return 'bg-orange-100 text-orange-700';
      default: return 'bg-slate-100 text-slate-500';
    }
  };

  return (
    <div className="space-y-6 pb-6">
      {/* === СЕКЦИЯ 1: МОНИТОРИНГ РЕКУПЕРАТОРА === */}
      <div className="space-y-3 animate-in fade-in fill-mode-backwards duration-500 motion-reduce:animate-none">
        <div className="flex justify-between items-center py-3 px-4 rounded-xl border border-slate-200 bg-slate-50">
          <span className="text-sm text-slate-600 font-medium">{t('recuperatorWidget.state')}</span>
          <span
            className={`px-3 py-1 rounded-full text-xs font-semibold uppercase tracking-wide ${getRecupStateColor(data?.recupStateCode)}`}
          >
            {data?.recupStateCode != null && data.recupStateCode >= 0 && data.recupStateCode <= 4
            ? t(`recuperatorStates.${data.recupStateCode}`)
            : (data?.recupStateName ?? '—')}
          </span>
        </div>
        <RecuperatorRow
          label={t('recuperatorWidget.valvePercent')}
          value={data?.recupTargetPercent ?? undefined}
          unit="%"
        />
        <RecuperatorTextRow
          label={t('recuperatorWidget.pump')}
          value={data?.pumpActive ? t('recuperatorWidget.pumpOn') : t('recuperatorWidget.pumpOff')}
        />
        <RecuperatorRow
          label={t('recuperatorWidget.bt6')}
          value={data?.recupTempBt6 ?? undefined}
          unit="°C"
        />
      </div>


      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-5 animate-in fade-in fill-mode-backwards duration-500 [animation-delay:80ms] motion-reduce:animate-none">
        <h4 className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-4 flex items-center gap-2">
          <span className="w-1.5 h-1.5 rounded-full bg-blue-400"></span>
          {t('recuperatorWidget.setpoints')}
        </h4>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <RecuperatorInput
            label={t('recuperatorWidget.trackingIntervalRecup')}
            value={recuperatorSettings?.trackingIntervalRecup ?? undefined}
            onChange={(val) => mutate({ trackingIntervalRecup: Math.round(val) })}
            unit=""
            decimals={0}
            readOnly={!isAdmin}
          />
          <RecuperatorInput
            label={t('recuperatorWidget.minRpm')}
            value={recuperatorSettings?.targetRpmMin}
            onChange={(val) => mutate({ targetRpmMin: val })}
            decimals={1}
            readOnly={!isAdmin}
          />
          <RecuperatorInput
            label={t('recuperatorWidget.maxRpm')}
            value={recuperatorSettings?.targetRpmMax}
            onChange={(val) => mutate({ targetRpmMax: val })}
            decimals={1}
            readOnly={!isAdmin}
          />
          <RecuperatorInput
            label={t('recuperatorWidget.calibrationStep')}
            value={recuperatorSettings?.targetRpmStep}
            onChange={(val) => mutate({ targetRpmStep: val })}
            decimals={1}
            readOnly={!isAdmin}
          />
        </div>
      </div>
    </div>
  );
};
