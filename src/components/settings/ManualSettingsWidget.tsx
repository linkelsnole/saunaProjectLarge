import { LatestSaveQueue } from './latestSaveQueue';
import { useEffect, useId, useMemo, useRef, useState } from 'react';
import { useDashboardData } from '@/api/useDashboard';
import { useSetVentilationMode, useUpdateVolumeSettings } from '@/api/hooks';
import { Wind, Target, Leaf, Fan, SlidersHorizontal } from 'lucide-react';
import './volumeWidget.css';
import { NeoInput } from '@/shared/ui/NeoInput';
import { useAuth } from '@/contexts/AuthContext';
import { useTranslation } from 'react-i18next';

type PresetKey = 'ecoAirflow' | 'normalAirflow' | 'intensiveAirflow';

const AIRFLOW_MIN = 0;
const AIRFLOW_MAX = 21000;

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

export const ManualSettingsWidget = () => {
  const { t } = useTranslation();
  const gaugeId = useId();
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
    { code: 1, Icon: Leaf, label: t('volumeWidget.modes.eco') },
    { code: 2, Icon: Fan, label: t('volumeWidget.modes.normal') },
    { code: 3, Icon: Wind, label: t('volumeWidget.modes.intensive') },
    { code: 4, Icon: SlidersHorizontal, label: t('volumeWidget.modes.manual') },
  ], [t]);

  const presetRows: Array<{ key: PresetKey; label: string; Icon: typeof Leaf }> = [
    { key: 'ecoAirflow', label: t('volumeWidget.modes.eco'), Icon: Leaf },
    { key: 'normalAirflow', label: t('volumeWidget.modes.normal'), Icon: Fan },
    { key: 'intensiveAirflow', label: t('volumeWidget.modes.intensive'), Icon: Wind },
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

  const angle = (135 + sliderProgress * 2.7) * Math.PI / 180;
  const knobX = 160 + 122 * Math.cos(angle);
  const knobY = 160 + 122 * Math.sin(angle);

  return (
    <div className="volume-panel">
      <div className="volume-metrics">
        {[{ Icon: Wind, label: t('volumeWidget.realAirflow'), value: data?.airflowReal },
          { Icon: Target, label: t('volumeWidget.targetAirflow'), value: data?.airflowTarget }].map(({ Icon, label, value }) => (
          <div className="volume-surface volume-metric" key={label}>
            <Icon aria-hidden="true" />
            <div><div className="volume-metric-label">{label}</div>
              <div className="volume-metric-value">{formatValue(value)} <span>{t('units.m3h')}</span></div>
            </div>
          </div>
        ))}
      </div>
      <div className="volume-modes" aria-label={t('volumeWidget.modeTitle')}>
        {modes.map(({ code, label, Icon }) => (
          <button key={code} type="button" aria-pressed={activeMode === code} disabled={modeDisabled}
            onClick={() => activeMode !== code && selectMode(code)}
            className={`volume-mode volume-mode-${code}`}>
            <Icon aria-hidden="true" /><span>{label}</span>
          </button>
        ))}
      </div>
      <div className="volume-mode-caption">
        <span>{t('volumeWidget.currentMode')}: <strong>{currentModeLabel}</strong></span>
        {!screenMode && <span className="text-amber-700">{t('volumeWidget.screenModeHint')}</span>}
      </div>
      <div className={`volume-controls ${activeMode === 4 ? '' : 'volume-controls-presets-only'}`}>
        {activeMode === 4 && (
          <section className="volume-surface volume-manual">
            <div className="volume-gauge">
              <svg viewBox="0 0 320 290" aria-hidden="true">
                <defs>
                  <linearGradient id={gaugeId} x1="0" y1="1" x2="1" y2="0">
                    <stop offset="0%" stopColor="#237bff" /><stop offset="100%" stopColor="#a8e4fa" />
                  </linearGradient>
                </defs>
                <circle cx="160" cy="160" r="147" fill="none" stroke="white" strokeWidth="2" />
                <path d="M 73.73 246.27 A 122 122 0 1 1 246.27 246.27" fill="none" stroke="#e0eef8" strokeWidth="24" strokeLinecap="round" />
                <path d="M 73.73 246.27 A 122 122 0 1 1 246.27 246.27" fill="none" stroke={`url(#${gaugeId})`} strokeWidth="24" strokeLinecap="round" pathLength="100" strokeDasharray={`${sliderProgress} 100`} />
                <circle cx={knobX} cy={knobY} r="16" fill="#38c8ed" stroke="white" strokeWidth="6" />
              </svg>
              <div className="volume-gauge-label">
                <h3>{t('volumeWidget.manualTarget')}</h3>
                <strong>{formatValue(manualAirflow)}</strong><span>{t('units.m3h')}</span>
              </div>
            </div>
            <div className="volume-slider-row">
              <div className="relative h-7 min-w-0 flex-1 px-1">
                <div className="absolute left-1 right-1 top-2 h-3 overflow-hidden rounded-full bg-sky-100">
                  <div className="h-full rounded-full bg-gradient-to-r from-slate-700 to-blue-500" style={{ width: `${sliderProgress}%` }} />
                </div>
                <input type="range" min={AIRFLOW_MIN} max={AIRFLOW_MAX} step={500}
                  value={manualAirflow} disabled={sliderDisabled} aria-label={t('volumeWidget.manualTarget')}
                  onChange={(event) => changeManualAirflow(Number(event.target.value))}
                  className="volume-range absolute inset-0 z-20 h-full w-full cursor-pointer opacity-0 disabled:cursor-not-allowed" />
                <div className="volume-slider-knob pointer-events-none absolute top-1/2 h-6 w-6 -translate-x-1/2 -translate-y-1/2 rounded-full border-4 border-white bg-cyan-400 shadow-md"
                  style={{ left: `calc(${sliderProgress}% + 4px)` }} />
              </div>
              <div className="volume-manual-input"><NumberEditor value={manualAirflow} onCommit={changeManualAirflow}
                readOnly={sliderDisabled} min={AIRFLOW_MIN} max={AIRFLOW_MAX} unit={t('units.m3h')} ariaLabel={t('volumeWidget.manualTarget')} /></div>
            </div>
          </section>
        )}
        <section className="volume-surface volume-presets">
          <h3>{t('volumeWidget.presetsTitle')}</h3>
          {!isAdmin && <div className="text-xs text-slate-500">{t('volumeWidget.readOnly')}</div>}
          <div className="volume-preset-list">
            {presetRows.map(({ key, label, Icon }, index) => (
              <div key={key} className={`volume-preset volume-preset-${index + 1}`}>
                <div className="volume-preset-label"><Icon aria-hidden="true" /><span>{label}</span></div>
                <NumberEditor value={volume?.[key]} onCommit={(value) => saveVolume({ [key]: value })}
                  readOnly={!isAdmin} min={AIRFLOW_MIN} max={AIRFLOW_MAX} unit={t('units.m3h')} ariaLabel={`${label}, ${t('units.m3h')}`} />
              </div>
            ))}
          </div>
          <div className="volume-boost">
            <h3>{t('volumeWidget.boost')}</h3>
            <NumberEditor value={volume?.boostPercent} onCommit={(value) => saveVolume({ boostPercent: value })}
              readOnly={!canOperate} min={-100} max={100} step={0.1} unit="%" ariaLabel={t('volumeWidget.boost')} />
          </div>
        </section>
      </div>
    </div>
  );
};
