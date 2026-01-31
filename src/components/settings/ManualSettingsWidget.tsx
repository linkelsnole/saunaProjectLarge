import { useState, useEffect } from 'react';
import { useDashboardData } from '@/api/useDashboard';
import { useUpdateManualSettings } from '@/api/hooks';
import { Input } from '@/shared/ui/input';
import { useAuth } from '@/contexts/AuthContext';

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
    <div className="flex flex-col gap-2 p-4 bg-white rounded-xl border border-slate-200 shadow-sm hover:shadow-md transition-all">
      <span className="text-xs text-slate-500 font-semibold uppercase tracking-wider">
        {label}
      </span>
      <div className="relative">
        <Input
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
          className="pr-10 font-mono text-2xl font-bold border-transparent hover:border-slate-300 focus:border-blue-500 bg-transparent h-12 text-slate-800"
        />
        <span className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 text-sm font-medium">
          {unit}
        </span>
      </div>
    </div>
  );
};

export const ManualSettingsWidget = () => {
  const { data } = useDashboardData();
  const { mutate } = useUpdateManualSettings();
  const { role } = useAuth();
  const isAdmin = role === 'ROLE_ADMIN';
  const isUser = role === 'ROLE_USER';
  const canEditVentilation = isAdmin || isUser;


  const supplySpeed = data?.manualSettings?.supplySpeed;
  const boostPercent = data?.manualSettings?.boostPercent;

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        {/* Скорость вентилирования ПВ */}
        <ManualInput
          label="Скорость вентилирования ПВ"
          value={supplySpeed}
          onChange={(val) => mutate({ supplySpeed: val })}
          readOnly={!canEditVentilation}
        />

        {/* Процент наддува (ПВ-ОВ) */}
        <ManualInput
          label="Наддув (ПВ-ОВ)"
          value={boostPercent}
          onChange={(val) => mutate({ boostPercent: val })}
          readOnly={!canEditVentilation}
        />
      </div>
    </div>
  );
};

