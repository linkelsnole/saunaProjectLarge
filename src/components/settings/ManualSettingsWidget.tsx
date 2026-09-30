import { LatestSaveQueue } from './latestSaveQueue';
import { useEffect, useMemo, useRef, useState } from 'react';
import { useDashboardData } from '@/api/useDashboard';
import { useSetVentilationMode, useUpdateVolumeSettings } from '@/api/hooks';
import { GlassCard } from '@/shared/ui/GlassCard';
import { NeoInput } from '@/shared/ui/NeoInput';
import { useAuth } from '@/contexts/AuthContext';
import { useTranslation } from 'react-i18next';

type PresetKey = 'ecoAirflow' | 'normalAirflow' | 'intensiveAirflow';

const AIRFLOW_MIN = 0;
const AIRFLOW_MAX = 21000;

const MODE_STYLES: Record<number, string> = {
  1: 'from-emerald-500 to-green-600 ring-emerald-400/50',
  2: 'from-blue-400 to-blue-600 ring-blue-400/50',
  3: 'from-orange-400 to-orange-600 ring-orange-400/50',
  4: 'from-slate-700 to-slate-950 ring-slate-400/50',
};

const formatValue = (value: number | null | undefined) => {
  if (value === null || value === undefined || !Number.isFinite(value)) return '—';
  return Math.abs(value - Math.round(value)) < 0.05 ? String(Math.round(value)) : value.toFixed(1);
};

interface NumberEditorProps {
  value: number | null | undefined;
  onCommit: (value: number) => void;
  readOnly?: boolean;
  min?: number;
  max?: number;
  step?: number;
  unit: string;
  ariaLabel: string;
}

const NumberEditor = ({
  value,
  onCommit,
  readOnly = false,
  min,
  max,
  step = 1,
  unit,
  ariaLabel,
}: NumberEditorProps) => {
  const [localValue, setLocalValue] = useState('');
  const [editing, setEditing] = useState(false);

  useEffect(() => {
    if (!editing) setLocalValue(value === null || value === undefined ? '' : formatValue(value));
  }, [editing, value]);

  const commit = () => {
    setEditing(false);
    const parsed = Number(localValue.replace(',', '.'));
    if (Number.isFinite(parsed) && parsed !== value) {
      onCommit(parsed);
      return;
    }
    setLocalValue(value === null || value === undefined ? '' : formatValue(value));
  };

  return (
    <div className="relative min-w-0">
      <NeoInput
        type="number"
        value={localValue}
        min={min}
        max={max}
        step={step}
        aria-label={ariaLabel}
        readOnly={readOnly}
        tabIndex={readOnly ? -1 : 0}
        onFocus={() => !readOnly && setEditing(true)}
        onChange={(event) => !readOnly && setLocalValue(event.target.value)}
        onBlur={commit}
        onKeyDown={(event) => event.key === 'Enter' && event.currentTarget.blur()}
        className={readOnly ? 'bg-slate-50 text-slate-500 shadow-none' : 'pr-16 text-slate-800'}
      />
      <span className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-xs font-medium text-slate-400">
        {unit}
      </span>
    </div>
  );
};

const MetricCard = ({ label, value, unit }: { label: string; value: string; unit?: string }) => (
  <GlassCard className="min-h-20 p-3 sm:p-4">
    <div className="text-[10px] font-semibold uppercase tracking-wider text-slate-400">{label}</div>
    <div className="mt-2 break-words text-lg font-black leading-tight text-slate-800 sm:text-xl">
      {value}{unit ? <span className="mt-1 block whitespace-nowrap text-[10px] font-semibold text-slate-400 sm:ml-1 sm:mt-0 sm:inline sm:text-xs">{unit}</span> : null}
    </div>
  </GlassCard>
);

export const ManualSettingsWidget = () => {
  const { t } = useTranslation();
  const { data } = useDashboardData();
  const { role } = useAuth();
  const { mutate: selectMode, isPending: modePending } = useSetVentilationMode();
  const { mutate: saveVolume, mutateAsync: saveVolumeAsync } = useUpdateVolumeSettings();

  const [manualAirflow, setManualAirflow] = useState(data?.volumeSettings?.manualAirflow ?? 0);

  const isAdmin = role === 'ROLE_ADMIN';
  const canOperate = isAdmin || role === 'ROLE_USER';
  const unitRunning = data?.recuperatorStatus?.enabled === true;
  const screenMode = data?.screenMode === true;
  const activeMode = data?.ventilationModeCode ?? null;
  const volume = data?.volumeSettings;
  const latestVolume = useRef(volume);
  latestVolume.current = volume;
  const manualQueue = useMemo(() => new LatestSaveQueue(
    (value: number) => saveVolumeAsync({ manualAirflow: value }),
    () => {
      const actual = latestVolume.current?.manualAirflow;
      if (actual != null) setManualAirflow(actual);
    },
  ), [saveVolumeAsync]);

  useEffect(() => {
    if (!manualQueue.busy && volume?.manualAirflow !== null && volume?.manualAirflow !== undefined) {
      setManualAirflow(volume.manualAirflow);
    }
  }, [volume?.manualAirflow, manualQueue]);

  useEffect(() => () => manualQueue.dispose(), [manualQueue]);

  const modes = useMemo(() => [
    { code: 1, label: t('volumeWidget.modes.eco') },
    { code: 2, label: t('volumeWidget.modes.normal') },
    { code: 3, label: t('volumeWidget.modes.intensive') },
    { code: 4, label: t('volumeWidget.modes.manual') },
  ], [t]);

  const presetRows: Array<{ key: PresetKey; label: string; color: string }> = [
    { key: 'ecoAirflow', label: t('volumeWidget.modes.eco'), color: 'bg-emerald-500' },
    { key: 'normalAirflow', label: t('volumeWidget.modes.normal'), color: 'bg-blue-500' },
    { key: 'intensiveAirflow', label: t('volumeWidget.modes.intensive'), color: 'bg-orange-500' },
  ];

  const currentModeLabel = activeMode === null
    ? '—'
    : t(`volumeWidget.currentModes.${activeMode}`, { defaultValue: data?.ventilationModeName ?? '—' });
  const modeDisabled = !canOperate || !screenMode || !unitRunning || modePending;
  const sliderDisabled = !canOperate || !screenMode || !unitRunning;
  const sliderProgress = Math.max(0, Math.min(100, (manualAirflow / AIRFLOW_MAX) * 100));

  const changeManualAirflow = (value: number) => {
    const nextValue = Math.max(AIRFLOW_MIN, Math.min(AIRFLOW_MAX, value));
    setManualAirflow(nextValue);
    manualQueue.schedule(nextValue);
  };

  return (
    <div className="space-y-4 pb-4">
      <GlassCard className="p-3 sm:p-4">
        <div className="mb-3 text-xs font-semibold uppercase tracking-widest text-slate-500">
          {t('volumeWidget.modeTitle')}
        </div>
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
          {modes.map((mode) => {
            const active = activeMode === mode.code;
            return (
              <button
                key={mode.code}
                type="button"
                aria-pressed={active}
                disabled={modeDisabled}
                onClick={() => !active && selectMode(mode.code)}
                className={`min-h-14 rounded-xl bg-gradient-to-br px-2 py-3 text-sm font-bold text-white shadow-sm transition-all ease-out hover:scale-[1.03] disabled:cursor-not-allowed disabled:opacity-45 ${MODE_STYLES[mode.code]} ${mode.code === 1 ? 'duration-500' : 'duration-200'} ${active ? 'ring-4 ring-offset-2 shadow-lg' : ''}`}
              >
                {mode.label}
              </button>
            );
          })}
        </div>
        {!screenMode && (
          <div className="mt-3 text-xs text-amber-600">{t('volumeWidget.screenModeHint')}</div>
        )}
      </GlassCard>

      <div className="grid grid-cols-3 gap-2 sm:gap-3">
        <MetricCard label={t('volumeWidget.currentMode')} value={currentModeLabel} />
        <MetricCard label={t('volumeWidget.targetAirflow')} value={formatValue(data?.airflowTarget)} unit={t('units.m3h')} />
        <MetricCard label={t('volumeWidget.realAirflow')} value={formatValue(data?.airflowReal)} unit={t('units.m3h')} />
      </div>

      <div className="grid grid-cols-1 items-start gap-4 lg:grid-cols-2">
        <GlassCard className="p-4">
          <div className="mb-3 flex items-center justify-between gap-3">
            <div className="text-xs font-semibold uppercase tracking-widest text-slate-500">
              {t('volumeWidget.presetsTitle')}
            </div>
            {!isAdmin && <span className="text-[10px] text-slate-400">{t('volumeWidget.readOnly')}</span>}
          </div>
          <div className="space-y-2">
            {presetRows.map((row) => (
              <div key={row.key} className="grid grid-cols-[minmax(76px,0.72fr)_minmax(0,1.28fr)] items-center gap-3 rounded-xl border border-slate-100 bg-slate-50/80 p-2.5">
                <div className="flex min-w-0 items-center gap-2.5">
                  <span className={`h-2.5 w-2.5 shrink-0 rounded-full shadow-sm ${row.color}`} />
                  <span className="truncate text-sm font-bold text-slate-700">{row.label}</span>
                </div>
                <NumberEditor
                  value={volume?.[row.key]}
                  onCommit={(value) => saveVolume({ [row.key]: value })}
                  readOnly={!isAdmin}
                  min={AIRFLOW_MIN}
                  max={AIRFLOW_MAX}
                  unit={t('units.m3h')}
                  ariaLabel={`${row.label}, ${t('units.m3h')}`}
                />
              </div>
            ))}
          </div>
        </GlassCard>

        <div className="grid gap-4">
          {activeMode === 4 && (
            <GlassCard className="p-4">
              <div className="text-xs font-semibold uppercase tracking-widest text-slate-500">
                {t('volumeWidget.manualTarget')}
              </div>
              <div className="mt-2 text-3xl font-black text-slate-800">
                {formatValue(manualAirflow)} <span className="text-sm font-semibold text-slate-400">{t('units.m3h')}</span>
              </div>
              <div className="relative mt-5 h-7 px-1" style={{ touchAction: 'pan-x' }}>
                <div className="absolute left-1 right-1 top-2 h-3 overflow-hidden rounded-full bg-slate-200">
                  <div className="h-full rounded-full bg-slate-800 transition-[width] duration-200" style={{ width: `${sliderProgress}%` }} />
                </div>
                <input
                  type="range"
                  min={AIRFLOW_MIN}
                  max={AIRFLOW_MAX}
                  step={10}
                  value={manualAirflow}
                  disabled={sliderDisabled}
                  aria-label={t('volumeWidget.manualTarget')}
                  onChange={(event) => changeManualAirflow(Number(event.target.value))}
                  className="absolute inset-0 z-20 h-full w-full cursor-pointer opacity-0 disabled:cursor-not-allowed"
                />
                <div
                  className="pointer-events-none absolute top-1/2 z-10 h-6 w-6 -translate-x-1/2 -translate-y-1/2 rounded-full border-2 border-slate-800 bg-white shadow-lg transition-[left] duration-200"
                  style={{ left: `calc(${sliderProgress}% + 4px)` }}
                />
              </div>
            </GlassCard>
          )}

          <GlassCard className="p-4">
            <div className="mb-3 text-xs font-semibold uppercase tracking-widest text-slate-500">
              {t('volumeWidget.boost')}
            </div>
            <NumberEditor
              value={volume?.boostPercent}
              onCommit={(value) => saveVolume({ boostPercent: value })}
              readOnly={!canOperate}
              min={-100}
              max={100}
              step={0.1}
              unit="%"
              ariaLabel={t('volumeWidget.boost')}
            />
          </GlassCard>
        </div>
      </div>
    </div>
  );
};
