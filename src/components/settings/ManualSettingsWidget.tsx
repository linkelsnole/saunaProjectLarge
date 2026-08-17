import { useState, useEffect } from 'react';
import { useDashboardData } from '@/api/useDashboard';
import { useUpdateManualSettings } from '@/api/hooks';
import { GlassCard } from '@/shared/ui/GlassCard';
import { NeoInput } from '@/shared/ui/NeoInput';
import { useAuth } from '@/contexts/AuthContext';
import { useTranslation } from 'react-i18next';

interface ManualInputProps {
  label: string;
  value: number | null | undefined;
  onChange: (val: number) => void;
  unit?: string;
  readOnly?: boolean;
}

const ManualInput = ({ label, value, onChange, unit = '%', readOnly = false }: ManualInputProps) => {
  const [localValue, setLocalValue] = useState('');
  const [isEditing, setIsEditing] = useState(false);

  // Синхронизация с сервером (только если не редактируем)
  useEffect(() => {
    if (!isEditing && value !== null && value !== undefined) {
      setLocalValue(value.toFixed(1));
    }
  }, [value, isEditing]);

  const handleBlur = () => {
    setIsEditing(false);
    
    // Заменяем точку на запятую перед парсингом
    const normalizedValue = localValue.replace('.', ',');
    const numVal = parseFloat(normalizedValue.replace(',', '.'));

    if (!isNaN(numVal) && Math.abs(numVal - (value || 0)) > 0.01) {
      onChange(numVal);
      // Уведомление будет в onSuccess хука после ответа сервера
    } else {
      // Если ввели ерунду - возвращаем как было
      if (value !== null && value !== undefined) {
        setLocalValue(value.toFixed(1));
      }
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') e.currentTarget.blur();
  };

  return (
    <GlassCard className="p-5 flex flex-col justify-between h-32 hover:translate-y-[-2px] transition-transform duration-300">
      <div className="flex justify-between items-start">
        <span className="text-[11px] text-slate-500 font-bold uppercase tracking-widest opacity-80">
          {label}
        </span>
        {!readOnly && (
          <div className="w-1.5 h-1.5 rounded-full bg-blue-400/50 shadow-[0_0_8px_rgba(96,165,250,0.8)]" />
        )}
      </div>
      <div className="relative mt-auto">
        <NeoInput
          type="number"
          step="0.1"
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
          className={readOnly ? "text-slate-500 shadow-none bg-transparent" : "text-slate-800"}
        />
        <span className="absolute right-4 top-1/2 -translate-y-1/2 text-slate-400 text-sm font-medium pointer-events-none">
          {unit}
        </span>
      </div>
    </GlassCard>
  );
};

export const ManualSettingsWidget = () => {
  const { t } = useTranslation();
  const { data } = useDashboardData();
  const { mutate } = useUpdateManualSettings();
  const { role } = useAuth();
  const isAdmin = role === 'ROLE_ADMIN';
  const isUser = role === 'ROLE_USER';
  const canEditVentilation = isAdmin || isUser;


  const supplySpeed = data?.manualSettings?.supplySpeed;
  const boostPercent = data?.manualSettings?.boostPercent;

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
        {/* Скорость вентилирования ПВ */}
        <ManualInput
          label={t('manualWidget.supplySpeed')}
          value={supplySpeed}
          onChange={(val) => mutate({ supplySpeed: val })}
          readOnly={!canEditVentilation}
        />

        {/* Процент наддува (ПВ-ОВ) */}
        <ManualInput
          label={t('manualWidget.boost')}
          value={boostPercent}
          onChange={(val) => mutate({ boostPercent: val })}
          readOnly={!canEditVentilation}
        />
      </div>
    </div>
  );
};

