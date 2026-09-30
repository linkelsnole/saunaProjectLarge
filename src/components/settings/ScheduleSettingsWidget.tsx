import { useEffect, useMemo, useState } from 'react';
import { CalendarDays, ChevronDown, Clock, Save, Thermometer, Wind } from 'lucide-react';
import { format } from 'date-fns';
import { enUS, ru } from 'date-fns/locale';
import { useTranslation } from 'react-i18next';

import { useSchedules, useToggleScheduleGroup, useUpdateScheduleGroup } from '@/api/hooks';
import type {
  ScheduleData,
  ScheduleVentilationMode,
  TemperatureControlType,
  UpdateScheduleGroupRequest,
} from '@/api/typesApi';
import { useAuth } from '@/contexts/AuthContext';
import { cn } from '@/shared/lib/utils';
import { Button } from '@/shared/ui/button';
import { Calendar } from '@/shared/ui/calendar';
import { Popover, PopoverContent, PopoverTrigger } from '@/shared/ui/popover';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/shared/ui/select';
import { Switch } from '@/shared/ui/switch';
import {
  buildScheduleGroups,
  changeScheduleTemperatureMode,
  changeScheduleTemperatureTarget,
  DAY_KEYS,
  encodeScheduleDate,
  encodeScheduleTime,
  formatScheduleTime,
  parseScheduleDate,
  parseWeekDays,
  normalizeScheduleTemperatureSelection,
  type DayKey,
  type ScheduleZone,
} from './scheduleUtils';

interface IntervalState {
  scheduleId: number;
  startTime: number;
  endTime: number;
  airMode: ScheduleVentilationMode;
  zone1Mode: TemperatureControlType;
  zone1Target: number;
  zone2Mode: TemperatureControlType;
  zone2Target: number;
}

interface GroupState {
  startDate: Date | null;
  endDate: Date | null;
  weekDays: DayKey[];
  active: boolean;
  intervals: IntervalState[];
}

const initialGroupState = (schedules: ScheduleData[]): GroupState => {
  const first = schedules[0];
  return {
    startDate: parseScheduleDate(first?.dateRange.startDate ?? 0),
    endDate: parseScheduleDate(first?.dateRange.endDate ?? 0),
    weekDays: parseWeekDays(first?.weekDayMask ?? 0),
    active: schedules.some((schedule) => schedule.status === 'ACTIVE'),
    intervals: schedules.map((schedule) => {
      const temperature = normalizeScheduleTemperatureSelection({
        zone1Mode: schedule.temperatureZone1.controlType,
        zone1Target: schedule.temperatureZone1.targetTemperature ?? 20,
        zone2Mode: schedule.temperatureZone2.controlType,
        zone2Target: schedule.temperatureZone2.targetTemperature ?? 20,
      });
      return {
        scheduleId: schedule.scheduleId,
        startTime: schedule.timeRange.startTime,
        endTime: schedule.timeRange.endTime,
        airMode: schedule.airControl.mode,
        ...temperature,
      };
    }),
  };
};

const DateField = ({
  value,
  onChange,
  label,
  disabled,
}: {
  value: Date | null;
  onChange: (value: Date | null) => void;
  label: string;
  disabled: boolean;
}) => {
  const { i18n, t } = useTranslation();
  const locale = i18n.language === 'en' ? enUS : ru;
  return (
    <div className="space-y-1.5">
      <label className="text-[11px] font-semibold uppercase tracking-wide text-slate-500">{label}</label>
      <Popover>
        <PopoverTrigger asChild>
          <button
            type="button"
            disabled={disabled}
            className="flex h-10 w-full items-center gap-2 rounded-xl border border-slate-200 bg-white px-3 text-left text-sm font-medium text-slate-700 transition-colors hover:border-slate-300 disabled:cursor-not-allowed disabled:opacity-50"
          >
            <CalendarDays className="h-4 w-4 text-slate-400" />
            {value ? format(value, 'd MMM yyyy', { locale }) : t('schedule.selectDate')}
          </button>
        </PopoverTrigger>
        <PopoverContent className="w-auto p-0" align="start">
          <Calendar
            mode="single"
            selected={value ?? undefined}
            onSelect={(date) => onChange(date ?? null)}
            locale={locale}
            captionLayout="dropdown"
            startMonth={new Date(2020, 0)}
            endMonth={new Date(2035, 11)}
          />
        </PopoverContent>
      </Popover>
    </div>
  );
};

const ModeSelect = ({
  value,
  onChange,
  options,
  disabled,
}: {
  value: string;
  onChange: (value: string) => void;
  options: Array<{ value: string; label: string }>;
  disabled: boolean;
}) => (
  <Select value={value} onValueChange={onChange} disabled={disabled}>
    <SelectTrigger className="h-9 w-full rounded-lg border-slate-200 bg-slate-50 text-xs font-medium">
      <SelectValue />
    </SelectTrigger>
    <SelectContent>
      {options.map((option) => (
        <SelectItem key={option.value} value={option.value}>
          {option.label}
        </SelectItem>
      ))}
    </SelectContent>
  </Select>
);

export const ScheduleSettingsWidget = () => {
  const { t, i18n } = useTranslation();
  const { role } = useAuth();
  const canEdit = role === 'ROLE_ADMIN' || role === 'ROLE_USER';
  const schedulesQuery = useSchedules();
  const saveMutation = useUpdateScheduleGroup();
  const toggleMutation = useToggleScheduleGroup();
  const [openGroup, setOpenGroup] = useState<number | null>(null);
  const [forms, setForms] = useState<Record<number, GroupState>>({});

  const groups = useMemo(() => {
    const schedules = schedulesQuery.data?.schedules ?? [];
    return buildScheduleGroups(schedules);
  }, [schedulesQuery.data]);

  useEffect(() => {
    if (groups.length === 0) return;
    const next: Record<number, GroupState> = {};
    groups.forEach((group) => {
      next[group.id] = initialGroupState(group.schedules);
    });
    setForms(next);
  }, [groups]);

  const airOptions = [
    { value: 'ECO', label: t('schedule.modes.eco') },
    { value: 'NORMAL', label: t('schedule.modes.normal') },
    { value: 'INTENSIVE', label: t('schedule.modes.intensive') },
  ];
  const temperatureOptions = [
    { value: 'ORP', label: t('schedule.modes.orp') },
    { value: 'PV', label: t('schedule.modes.pv') },
    { value: 'OV', label: t('schedule.modes.ov') },
  ];
  const locale = i18n.language === 'en' ? enUS : ru;

  const updateGroup = (groupId: number, change: Partial<GroupState>) => {
    setForms((current) => ({
      ...current,
      [groupId]: { ...current[groupId], ...change },
    }));
  };

  const updateInterval = (groupId: number, scheduleId: number, change: Partial<IntervalState>) => {
    const form = forms[groupId];
    if (!form) return;
    updateGroup(groupId, {
      intervals: form.intervals.map((interval) =>
        interval.scheduleId === scheduleId ? { ...interval, ...change } : interval),
    });
  };

  const updateTemperatureMode = (
    groupId: number,
    scheduleId: number,
    zone: ScheduleZone,
    nextMode: TemperatureControlType,
  ) => {
    const interval = forms[groupId]?.intervals.find((item) => item.scheduleId === scheduleId);
    if (!interval) return;
    updateInterval(groupId, scheduleId, changeScheduleTemperatureMode(interval, zone, nextMode));
  };

  const updateTemperatureTarget = (
    groupId: number,
    scheduleId: number,
    zone: ScheduleZone,
    nextTarget: number,
  ) => {
    const interval = forms[groupId]?.intervals.find((item) => item.scheduleId === scheduleId);
    if (!interval) return;
    updateInterval(groupId, scheduleId, changeScheduleTemperatureTarget(interval, zone, nextTarget));
  };

  const toggleDay = (groupId: number, day: DayKey) => {
    const form = forms[groupId];
    if (!form) return;
    updateGroup(groupId, {
      weekDays: form.weekDays.includes(day)
        ? form.weekDays.filter((value) => value !== day)
        : [...form.weekDays, day],
    });
  };

  const save = (groupId: number) => {
    const form = forms[groupId];
    if (!form || form.weekDays.length === 0) return;
    const airCodes: Record<ScheduleVentilationMode, number> = {
      DISABLED: 0,
      ECO: 1,
      NORMAL: 2,
      INTENSIVE: 3,
      MANUAL: 4,
    };
    const temperatureCodes: Record<TemperatureControlType, number> = { ORP: 0, PV: 1, OV: 2 };
    const payload: UpdateScheduleGroupRequest = {
      startDate: encodeScheduleDate(form.startDate),
      endDate: encodeScheduleDate(form.endDate),
      weekDays: form.weekDays,
      isActive: form.active,
      intervals: form.intervals.map((interval) => ({
        scheduleId: interval.scheduleId,
        startTime: interval.startTime,
        endTime: interval.endTime,
        airControlMode: airCodes[interval.airMode],
        zone1ControlMode: temperatureCodes[interval.zone1Mode],
        ...(interval.zone1Mode !== 'ORP' ? { zone1TargetTemperature: interval.zone1Target } : {}),
        zone2ControlMode: temperatureCodes[interval.zone2Mode],
        ...(interval.zone2Mode !== 'ORP' ? { zone2TargetTemperature: interval.zone2Target } : {}),
      })),
    };
    saveMutation.mutate({ groupId, data: payload });
  };

  const toggleActive = (groupId: number, active: boolean) => {
    const previous = forms[groupId]?.active ?? false;
    updateGroup(groupId, { active });
    toggleMutation.mutate(
      { groupId, isActive: active },
      { onError: () => updateGroup(groupId, { active: previous }) },
    );
  };

  if (schedulesQuery.isLoading) {
    return <div className="py-12 text-center text-sm text-slate-500">{t('schedule.loading')}</div>;
  }
  if (schedulesQuery.isError) {
    return (
      <div className="space-y-3 rounded-2xl border border-rose-200 bg-rose-50 p-5 text-center">
        <div className="text-sm font-medium text-rose-700">{t('schedule.loadError')}</div>
        <Button variant="outline" size="sm" onClick={() => schedulesQuery.refetch()}>
          {t('schedule.retry')}
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-3 pb-3">
      {groups.map((group, groupIndex) => {
        const form = forms[group.id];
        if (!form) return null;
        const open = openGroup === group.id;
        return (
          <section
            key={group.id}
            className={cn(
              'overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm transition-all duration-300',
              open && 'border-slate-300 shadow-md',
              'animate-in fade-in fill-mode-backwards duration-500 motion-reduce:animate-none',
            )}
            style={{ animationDelay: `${groupIndex * 80}ms` }}
          >
            <div className="flex w-full items-center justify-between gap-3 p-3.5 transition-colors hover:bg-slate-50">
              <button
                type="button"
                className="flex min-w-0 flex-1 items-center gap-3 text-left"
                onClick={() => setOpenGroup(open ? null : group.id)}
              >
                <div className={cn(
                  'flex h-10 w-10 shrink-0 items-center justify-center rounded-xl text-base font-bold transition-all duration-300',
                  form.active
                    ? 'bg-gradient-to-br from-emerald-500 to-teal-600 text-white shadow-md shadow-emerald-500/25'
                    : 'bg-slate-100 text-slate-400',
                )}>
                  {groupIndex + 1}
                </div>
                <div className="min-w-0">
                  <h3 className="truncate text-base font-semibold text-slate-800">
                    {t('schedule.cardTitle', { number: groupIndex + 1 })}
                  </h3>
                  <p className="mt-1 text-xs text-slate-500">{group.id < 0 ? t("physicalSchedule.ungrouped") : `${t("physicalSchedule.group")} ${group.id}`}</p>
                  <div className="mt-0.5 flex items-center gap-1.5 text-xs text-slate-500">
                    <CalendarDays className="h-3 w-3 shrink-0" />
                    <span>
                      {form.startDate ? format(form.startDate, 'd MMM', { locale }) : '—'} —{' '}
                      {form.endDate ? format(form.endDate, 'd MMM', { locale }) : '—'}
                    </span>
                  </div>
                </div>
              </button>
              <div className="flex shrink-0 items-center gap-2.5">
                {!open && (
                  <div className="flex items-center gap-2">
                    <span className="hidden text-xs font-medium text-slate-600 sm:inline">{t('schedule.active')}</span>
                    <Switch
                      checked={form.active}
                      onCheckedChange={(checked) => toggleActive(group.id, checked)}
                      disabled={!canEdit || toggleMutation.isPending}
                    />
                  </div>
                )}
                <button
                  type="button"
                  aria-label={t('schedule.cardTitle', { number: groupIndex + 1 })}
                  onClick={() => setOpenGroup(open ? null : group.id)}
                  className={cn(
                    'flex h-8 w-8 items-center justify-center rounded-lg transition-all duration-300',
                    open ? 'rotate-180 bg-slate-800 text-white' : 'bg-slate-100 text-slate-400',
                  )}>
                  <ChevronDown className="h-4 w-4" />
                </button>
              </div>
            </div>

            <div className={cn(
              'overflow-hidden transition-all duration-300 ease-in-out',
              open ? 'max-h-[3600px] opacity-100' : 'max-h-0 opacity-0',
            )}>
              <div className="space-y-4 bg-gradient-to-b from-slate-50/60 to-white px-3.5 pb-3.5 pt-1">
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                  <DateField
                    value={form.startDate}
                    onChange={(startDate) => updateGroup(group.id, { startDate })}
                    label={t('schedule.startDate')}
                    disabled={!canEdit}
                  />
                  <DateField
                    value={form.endDate}
                    onChange={(endDate) => updateGroup(group.id, { endDate })}
                    label={t('schedule.endDate')}
                    disabled={!canEdit}
                  />
                </div>

                <div className="space-y-2">
                  <label className="text-[11px] font-semibold uppercase tracking-wide text-slate-500">
                    {t('schedule.weekDays')}
                  </label>
                  <div className="grid grid-cols-7 gap-1.5">
                    {DAY_KEYS.map((day) => (
                      <button
                        key={day}
                        type="button"
                        disabled={!canEdit}
                        onClick={() => toggleDay(group.id, day)}
                        className={cn(
                          'rounded-lg py-2 text-xs font-semibold transition-all disabled:cursor-not-allowed disabled:opacity-50',
                          form.weekDays.includes(day)
                            ? 'bg-slate-800 text-white shadow-sm'
                            : 'border border-slate-200 bg-white text-slate-500 hover:bg-slate-100',
                        )}
                      >
                        {t(`schedule.days.${day}`)}
                      </button>
                    ))}
                  </div>
                  {form.weekDays.length === 0 && (
                    <p className="text-xs text-amber-600">{t('schedule.selectDay')}</p>
                  )}
                </div>

                <div className="space-y-2.5">
                  <div className="text-[11px] font-semibold uppercase tracking-wide text-slate-500">
                    {t('schedule.intervals')}
                  </div>
                  {form.intervals.map((interval) => (
                    <div key={interval.scheduleId} className="space-y-3 rounded-xl border border-slate-200 bg-white p-3">
                      <div className="flex items-center gap-2">
                        <div className="relative">
                          <Clock className="pointer-events-none absolute left-2.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-slate-400" />
                          <input
                            type="time"
                            value={formatScheduleTime(interval.startTime)}
                            disabled={!canEdit}
                            onChange={(event) => updateInterval(group.id, interval.scheduleId, {
                              startTime: encodeScheduleTime(event.target.value),
                            })}
                            className="h-9 w-[118px] rounded-lg border border-slate-200 bg-slate-50 pl-8 pr-2 text-xs font-semibold text-slate-700 outline-none focus:border-slate-400 disabled:opacity-50"
                          />
                        </div>
                        <div className="h-px flex-1 bg-slate-200" />
                        <div className="relative">
                          <Clock className="pointer-events-none absolute left-2.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-slate-400" />
                          <input
                            type="time"
                            value={formatScheduleTime(interval.endTime)}
                            disabled={!canEdit}
                            onChange={(event) => updateInterval(group.id, interval.scheduleId, {
                              endTime: encodeScheduleTime(event.target.value),
                            })}
                            className="h-9 w-[118px] rounded-lg border border-slate-200 bg-slate-50 pl-8 pr-2 text-xs font-semibold text-slate-700 outline-none focus:border-slate-400 disabled:opacity-50"
                          />
                        </div>
                      </div>

                      <div className="grid grid-cols-1 gap-3 lg:grid-cols-3">
                        <div className="space-y-1.5">
                          <label className="flex items-center gap-1.5 text-xs font-medium text-slate-600">
                            <Wind className="h-3.5 w-3.5" />
                            {t('schedule.volumeMode')}
                          </label>
                          <ModeSelect
                            value={interval.airMode}
                            onChange={(value) => updateInterval(group.id, interval.scheduleId, {
                              airMode: value as ScheduleVentilationMode,
                            })}
                            options={airOptions}
                            disabled={!canEdit}
                          />
                        </div>

                        {([1, 2] as const).map((zone) => {
                          const mode = zone === 1 ? interval.zone1Mode : interval.zone2Mode;
                          const target = zone === 1 ? interval.zone1Target : interval.zone2Target;
                          return (
                            <div key={zone} className="space-y-1.5">
                              <label className="flex items-center gap-1.5 text-xs font-medium text-slate-600">
                                <Thermometer className="h-3.5 w-3.5" />
                                {t('schedule.temperatureMode', { zone })}
                              </label>
                              <div className="flex gap-2">
                                <ModeSelect
                                  value={mode}
                                  onChange={(value) => updateTemperatureMode(
                                    group.id,
                                    interval.scheduleId,
                                    zone,
                                    value as TemperatureControlType,
                                  )}
                                  options={temperatureOptions}
                                  disabled={!canEdit}
                                />
                                {mode !== 'ORP' && (
                                  <div className="relative w-20 shrink-0">
                                    <input
                                      type="number"
                                      min={-50}
                                      max={100}
                                      step={0.1}
                                      value={target}
                                      disabled={!canEdit}
                                      aria-label={t('schedule.targetTemperature', { zone })}
                                      onChange={(event) => updateTemperatureTarget(
                                        group.id,
                                        interval.scheduleId,
                                        zone,
                                        Number(event.target.value),
                                      )}
                                      className="h-9 w-full rounded-lg border border-slate-200 bg-slate-50 px-2 pr-6 text-right text-xs font-semibold text-slate-700 outline-none focus:border-slate-400 disabled:opacity-50"
                                    />
                                    <span className="pointer-events-none absolute right-2 top-1/2 -translate-y-1/2 text-[10px] text-slate-400">°C</span>
                                  </div>
                                )}
                              </div>
                            </div>
                          );
                        })}
                      </div>
                    </div>
                  ))}
                </div>

                <Button
                  type="button"
                  className="h-11 w-full rounded-xl bg-slate-800 text-sm font-semibold text-white hover:bg-slate-900"
                  disabled={!canEdit || form.weekDays.length === 0 || saveMutation.isPending}
                  onClick={() => save(group.id)}
                >
                  <Save className="mr-2 h-4 w-4" />
                  {saveMutation.isPending ? t('schedule.saving') : t('schedule.save')}
                </Button>
              </div>
            </div>
          </section>
        );
      })}
    </div>
  );
};
