import { useEffect, useRef, useState } from 'react';
import { useDashboardData } from '@/api/useDashboard';
import { useSetTemperatureControl, useUpdateTemperatureSetpoints, useUpdateTempSettings } from '@/api/hooks';
import type { OrpSettingsDto } from '@/api/typesApi';
import { Badge } from '@/shared/ui/badge';
import { useAuth } from '@/contexts/AuthContext';
import { GlassCard } from '@/shared/ui/GlassCard';
import { NeoInput } from '@/shared/ui/NeoInput';
import { useTranslation } from 'react-i18next';

type ZoneMode = 0 | 1;
type OrpField = keyof OrpSettingsDto;

interface TempInputProps {
  label?: string;
  value: number | null | undefined;
  onChange: (value: number) => void;
  unit?: string;
  decimals?: number;
  readOnly?: boolean;
  inputClassName?: string;
}

const TempInput = ({ label, value, onChange, unit = '°C', decimals = 1, readOnly = false, inputClassName }: TempInputProps) => {
  const [localValue, setLocalValue] = useState('');
  const [isEditing, setIsEditing] = useState(false);

  useEffect(() => {
    if (!isEditing) setLocalValue(value == null ? '' : value.toFixed(decimals));
  }, [value, isEditing, decimals]);

  const finishEditing = () => {
    setIsEditing(false);
    const nextValue = Number.parseFloat(localValue.replace(',', '.'));
    if (Number.isFinite(nextValue) && (value == null || Math.abs(nextValue - value) > 0.001)) onChange(nextValue);
    else setLocalValue(value == null ? '' : value.toFixed(decimals));
  };

  return (
    <div className="flex w-full min-w-0 flex-col gap-1.5">
      {label ? <span className="flex min-h-8 items-end px-1 text-[10px] font-medium uppercase leading-tight tracking-wide text-slate-500 sm:text-[11px]">{label}</span> : null}
      <div className="relative">
        <NeoInput
          type="number"
          step={decimals === 0 ? '1' : '0.1'}
          value={localValue}
          onChange={(event) => {
            if (readOnly) return;
            setIsEditing(true);
            setLocalValue(event.target.value);
          }}
          onBlur={finishEditing}
          onKeyDown={(event) => { if (event.key === 'Enter') event.currentTarget.blur(); }}
          readOnly={readOnly}
          tabIndex={readOnly ? -1 : 0}
          className={`${readOnly ? 'bg-slate-50 text-slate-500 shadow-none' : 'text-slate-800'} h-10 pr-9 text-sm sm:text-base ${inputClassName ?? ''}`}
        />
        <span className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-xs font-medium text-slate-400">{unit}</span>
      </div>
    </div>
  );
};

const ValveMonitorRow = ({ label, value }: { label: string; value: string | null | undefined }) => (
  <div className="mb-1 flex items-center justify-between rounded-lg border border-slate-200 bg-white px-3 py-2 last:mb-0">
    <span className="text-[10px] font-semibold uppercase tracking-wide text-slate-500">{label}</span>
    <span className="font-mono text-sm font-bold text-slate-700">{value ?? '-'} <span className="text-[10px] text-slate-400">%</span></span>
  </div>
);

const getModeLabelKey = (mode: number | null | undefined) => {
  const keys: Record<number, string> = {
    0: 'tempWidget.modes.off', 1: 'tempWidget.modes.neutralZone', 2: 'tempWidget.modes.nzAfterHeating',
    3: 'tempWidget.modes.nzAfterCooling', 4: 'tempWidget.modes.cooling', 5: 'tempWidget.modes.heating',
    6: 'tempWidget.modes.tAveraging', 7: 'tempWidget.modes.hood', 8: 'tempWidget.modes.alg2Step0',
    9: 'tempWidget.modes.alg2Tuning', 10: 'tempWidget.modes.alg2Stabilization', 11: 'tempWidget.modes.alg2HeatOff',
    12: 'tempWidget.modes.alg2HeatInsuff',
  };
  return mode == null ? 'tempWidget.modes.na' : (keys[mode] ?? 'tempWidget.modes.unknown');
};

const getModeColor = (mode: number | null | undefined) => {
  if (mode === 5) return 'bg-red-100 text-red-700';
  if (mode === 10) return 'bg-emerald-100 text-emerald-700';
  if (mode === 4) return 'bg-blue-100 text-blue-700';
  if (mode === 9) return 'bg-amber-100 text-amber-700';
  return 'bg-slate-100 text-slate-600';
};

const ZoneModeSelector = ({ value, disabled, onChange }: { value: number | null | undefined; disabled: boolean; onChange: (mode: ZoneMode) => void }) => (
  <div className="grid grid-cols-2 gap-1 rounded-xl border border-slate-200 bg-slate-50 p-1">
    {([{ code: 1 as ZoneMode, label: 'ПВ' }, { code: 0 as ZoneMode, label: 'ОРП' }]).map((option) => (
      <button key={option.code} type="button" disabled={disabled} onClick={() => onChange(option.code)} className={`rounded-lg px-3 py-2 text-xs font-semibold transition-all duration-200 disabled:cursor-not-allowed disabled:opacity-60 ${value === option.code ? 'bg-slate-800 text-white shadow-sm' : 'text-slate-600 hover:bg-white'}`}>
        {option.label}
      </button>
    ))}
  </div>
);

const OrpTable = ({ value, readOnly, onChange }: { value: OrpSettingsDto | null | undefined; readOnly: boolean; onChange: (field: OrpField, value: number) => void }) => {
  const rows: Array<{ label: string; current: OrpField; target: OrpField }> = [
    { label: 'T1', current: 'currentT1', target: 'targetT1' },
    { label: 'T2', current: 'currentT2', target: 'targetT2' },
    { label: 'T3', current: 'currentT3', target: 'targetT3' },
  ];
  return (
    <div className="overflow-hidden rounded-xl border border-slate-200 bg-slate-50">
      <div className="grid grid-cols-[36px_1fr_1fr] gap-2 border-b border-slate-200 px-3 py-2 text-center text-[10px] font-semibold uppercase tracking-wide text-slate-500"><span /><span>Текущая T</span><span>Целевая T</span></div>
      <div className="space-y-2 p-3">
        {rows.map((row) => (
          <div key={row.label} className="grid grid-cols-[36px_1fr_1fr] items-center gap-2">
            <span className="text-xs font-bold text-slate-500">{row.label}</span>
            <TempInput value={value?.[row.current]} onChange={(next) => onChange(row.current, next)} decimals={0} readOnly={readOnly} />
            <TempInput value={value?.[row.target]} onChange={(next) => onChange(row.target, next)} decimals={0} readOnly={readOnly} />
          </div>
        ))}
      </div>
    </div>
  );
};

export const TempSettingsWidget = () => {
  const { t } = useTranslation();
  const { data } = useDashboardData();
  const { mutate: updateCommon } = useUpdateTempSettings();
  const { mutate: changeControl, isPending: controlPending } = useSetTemperatureControl();
  const { mutate: updateSetpoints } = useUpdateTemperatureSetpoints();
  const { role } = useAuth();
  const canEditZones = role === 'ROLE_ADMIN' || role === 'ROLE_USER';
  const canEditAll = role === 'ROLE_ADMIN';
  const tempSettings = data?.tempSettings;
  const sensors = data?.debugValveSensors ?? [];
  const getSensorValue = (id: string) => sensors.find((sensor) => sensor.id === id)?.value;
  const zone1Mode = data?.temperatureControlModeZone1Code;
  const zone2Mode = data?.temperatureControlModeZone2Code;
  const outflowSelected = zone1Mode === 2 && zone2Mode === 2;
  const previousZoneModes = useRef<[ZoneMode, ZoneMode]>([1, 1]);

  useEffect(() => {
    if (zone1Mode === 0 || zone1Mode === 1) previousZoneModes.current[0] = zone1Mode;
    if (zone2Mode === 0 || zone2Mode === 1) previousZoneModes.current[1] = zone2Mode;
  }, [zone1Mode, zone2Mode]);

  const setOutflowSelected = (selected: boolean) => {
    if (!canEditZones || controlPending || selected === outflowSelected) return;
    changeControl(selected ? { zone1Mode: 2, zone2Mode: 2 } : { zone1Mode: previousZoneModes.current[0], zone2Mode: previousZoneModes.current[1] });
  };

  const renderValves = (zone: 1 | 2) => (
    <div className="rounded-xl border border-slate-200 bg-slate-50 p-3">
      <ValveMonitorRow label={t('tempWidget.hotValve')} value={getSensorValue(`VHP${zone}`)} />
      <ValveMonitorRow label={t('tempWidget.coldValve')} value={getSensorValue(`VCP${zone}`)} />
      <ValveMonitorRow label={t('tempWidget.hotValveExtra')} value={getSensorValue(`VOP${zone}`)} />
    </div>
  );

  const renderZone = (zone: 1 | 2) => {
    const controlMode = zone === 1 ? zone1Mode : zone2Mode;
    const statusMode = zone === 1 ? tempSettings?.ctrlModeZone1 : tempSettings?.ctrlModeZone2;
    const orp = zone === 1 ? tempSettings?.orpZone1 : tempSettings?.orpZone2;
    return (
      <GlassCard className="flex h-full flex-col animate-in fade-in fill-mode-backwards duration-500 motion-reduce:animate-none">
        <div className="flex items-center justify-between border-b border-slate-200 p-4">
          <h4 className="flex items-center gap-2 text-sm font-semibold tracking-wider text-slate-800"><span className="h-2 w-2 rounded-full bg-slate-800" />{t(`tempWidget.zone${zone}`)}</h4>
          <Badge className={`${getModeColor(statusMode)} rounded-full border border-slate-200 px-3 py-1 text-[10px] font-bold uppercase tracking-wide`}>{t(getModeLabelKey(statusMode))}</Badge>
        </div>
        <div className="flex-1 space-y-4 p-5">
          <ZoneModeSelector value={controlMode} disabled={!canEditZones || controlPending} onChange={(mode) => changeControl(zone === 1 ? { zone1Mode: mode } : { zone2Mode: mode })} />
          {controlMode === 0 ? (
            <OrpTable value={orp} readOnly={!canEditZones} onChange={(field, value) => updateSetpoints(zone === 1 ? { orpZone1: { [field]: value } } : { orpZone2: { [field]: value } })} />
          ) : (
            <TempInput label={t('tempWidget.inflowTarget')} value={zone === 1 ? tempSettings?.inflowTargetZone1 : tempSettings?.inflowTargetZone2} onChange={(value) => updateSetpoints(zone === 1 ? { inflowTargetZone1: value } : { inflowTargetZone2: value })} readOnly={!canEditZones} inputClassName="h-12 text-center text-2xl tracking-tight" />
          )}
          {renderValves(zone)}
        </div>
      </GlassCard>
    );
  };

  const renderOutflowZone = (zone: 1 | 2) => {
    const statusMode = zone === 1 ? tempSettings?.ctrlModeZone1 : tempSettings?.ctrlModeZone2;
    return (
      <GlassCard className="overflow-hidden animate-in fade-in duration-500 motion-reduce:animate-none">
        <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3">
          <h4 className="flex items-center gap-2 text-sm font-semibold text-slate-800"><span className="h-2 w-2 rounded-full bg-slate-800" />{t(`tempWidget.zone${zone}`)}</h4>
          <Badge className={`${getModeColor(statusMode)} rounded-full border border-slate-200 px-3 py-1 text-[10px] font-bold uppercase tracking-wide`}>{t(getModeLabelKey(statusMode))}</Badge>
        </div>
        <div className="p-3">{renderValves(zone)}</div>
      </GlassCard>
    );
  };

  return (
    <div className="space-y-4 pb-4">
      <GlassCard className="p-4 animate-in fade-in fill-mode-backwards duration-500 motion-reduce:animate-none">
        <div className="mb-3 flex items-center justify-between gap-3">
          <div><h4 className="text-xs font-semibold uppercase tracking-wide text-slate-700">{t('tempWidget.controlType')}</h4><p className="mt-1 text-[11px] text-slate-400">{t('tempWidget.outflowHint')}</p></div>
          <Badge className="border border-slate-200 bg-slate-100 text-slate-600">{outflowSelected ? 'ОВ' : 'ПВ / ОРП'}</Badge>
        </div>
        <div className="grid grid-cols-2 gap-1 rounded-xl border border-slate-200 bg-slate-50 p-1">
          <button type="button" disabled={!canEditZones || controlPending} onClick={() => setOutflowSelected(true)} className={`rounded-lg px-3 py-2.5 text-sm font-semibold transition-all duration-200 disabled:opacity-60 ${outflowSelected ? 'bg-slate-800 text-white shadow-sm' : 'text-slate-600 hover:bg-white'}`}>{t('tempWidget.outflowMode')}</button>
          <button type="button" disabled={!canEditZones || controlPending} onClick={() => setOutflowSelected(false)} className={`rounded-lg px-3 py-2.5 text-sm font-semibold transition-all duration-200 disabled:opacity-60 ${!outflowSelected ? 'bg-slate-800 text-white shadow-sm' : 'text-slate-600 hover:bg-white'}`}>{t('tempWidget.zoneModes')}</button>
        </div>
        {outflowSelected ? <div className="mx-auto mt-4 max-w-md animate-in fade-in duration-500 motion-reduce:animate-none"><TempInput label={t('tempWidget.outflowTarget')} value={tempSettings?.outflowTarget} onChange={(value) => updateSetpoints({ outflowTarget: value })} readOnly={!canEditZones} inputClassName="h-12 text-center text-2xl tracking-tight" /></div> : null}
      </GlassCard>

      {!outflowSelected ? <div className="grid grid-cols-1 gap-4 md:grid-cols-2">{renderZone(1)}{renderZone(2)}</div> : <div className="grid grid-cols-1 gap-4 md:grid-cols-2">{renderOutflowZone(1)}{renderOutflowZone(2)}</div>}

      <GlassCard className="p-4 animate-in fade-in fill-mode-backwards duration-500 [animation-delay:160ms] motion-reduce:animate-none">
        <h4 className="mb-3 flex items-center gap-2 text-xs font-semibold uppercase tracking-wide text-slate-500"><span className="h-1.5 w-1.5 rounded-full bg-amber-400" />{t('tempWidget.temperatureSection')}</h4>
        <TempInput label={t('tempWidget.valveStartupPosition')} value={tempSettings?.valveStartupPosition} onChange={(value) => updateCommon({ valveStartupPosition: value })} unit="%" decimals={1} readOnly={!canEditAll} />
      </GlassCard>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <GlassCard className="p-4 animate-in fade-in duration-500 motion-reduce:animate-none"><h4 className="mb-3 flex items-center gap-2 text-xs font-semibold uppercase tracking-wide text-slate-500"><span className="h-1.5 w-1.5 rounded-full bg-blue-400" />{t('tempWidget.waterThresholds')}</h4><div className="grid grid-cols-2 gap-3"><TempInput label={t('tempWidget.rpmReduction')} value={tempSettings?.waterWarnThreshold} onChange={(value) => updateCommon({ waterWarnThreshold: value })} readOnly={!canEditAll} /><TempInput label={t('tempWidget.failure')} value={tempSettings?.waterFailThreshold} onChange={(value) => updateCommon({ waterFailThreshold: value })} readOnly={!canEditAll} /></div></GlassCard>
        <GlassCard className="p-4 animate-in fade-in duration-500 motion-reduce:animate-none"><h4 className="mb-3 flex items-center gap-2 text-xs font-semibold uppercase tracking-wide text-slate-500"><span className="h-1.5 w-1.5 rounded-full bg-emerald-400" />{t('tempWidget.supplyThresholds')}</h4><div className="grid grid-cols-2 gap-3"><TempInput label={t('tempWidget.rpmReduction')} value={tempSettings?.inflowWarnThreshold} onChange={(value) => updateCommon({ inflowWarnThreshold: value })} readOnly={!canEditAll} /><TempInput label={t('tempWidget.failure')} value={tempSettings?.inflowFailThreshold} onChange={(value) => updateCommon({ inflowFailThreshold: value })} readOnly={!canEditAll} /></div></GlassCard>
      </div>

      <GlassCard className="p-4 animate-in fade-in duration-500 motion-reduce:animate-none">
        <h4 className="mb-4 flex items-center gap-2 text-xs font-semibold uppercase tracking-wide text-slate-700"><span className="h-4 w-1 rounded-full bg-amber-400" />{t('tempWidget.setpoints')}</h4>
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-5"><TempInput label={t('tempWidget.neutralZoneT')} value={tempSettings?.neutralZoneT} onChange={(value) => updateCommon({ neutralZoneT: value })} readOnly={!canEditAll} /><TempInput label={t('tempWidget.hotValveMin')} value={tempSettings?.hotValveMinProc} onChange={(value) => updateCommon({ hotValveMinProc: value })} unit="%" readOnly={!canEditAll} /><TempInput label={t('tempWidget.waterMinTNoWork')} value={tempSettings?.waterMinTNoWork} onChange={(value) => updateCommon({ waterMinTNoWork: value })} readOnly={!canEditAll} /><TempInput label={t('tempWidget.waterMinTWork')} value={tempSettings?.waterMinTWork} onChange={(value) => updateCommon({ waterMinTWork: value })} readOnly={!canEditAll} /><TempInput label={t('tempWidget.radiatorFail')} value={tempSettings?.radTempFail} onChange={() => undefined} readOnly /></div>
      </GlassCard>
    </div>
  );
};
