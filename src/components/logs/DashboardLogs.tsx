import SystemEventsJournal from './SystemEventsJournal'
import { useState } from 'react'
import { Download, FileSpreadsheet, History, Loader2 } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { cn } from '@/shared/lib/utils'
import { DateTimePicker } from '@/shared/ui/datetime-picker'
import {
	useDownloadReadings,
	useLogSettings,
	useUpdateLogSettings,
	useUserActionLogs,
} from '@/api/hooks'
import type { UserActionEntry } from '@/api/typesApi'

const WINDOW_OPTIONS = [1, 5, 30, 60, 1440, 43200, 129600]
const TYPE_OPTIONS = ['ALL', 'POWER', 'MODE', 'SETPOINT', 'AUTH', 'OTHER']
const INTERVAL_OPTIONS = [1, 5, 10, 30, 60]

const cardClassName =
	'rounded-[22px] border border-slate-200/90 bg-white p-4 sm:p-5 shadow-[0_4px_18px_-12px_rgba(15,23,42,.15)] animate-in fade-in fill-mode-backwards duration-200 motion-reduce:animate-none'

const readStoredNumber = (key: string, fallback: number, allowed: number[]) => {
	const raw = Number(localStorage.getItem(key))
	return allowed.includes(raw) ? raw : fallback
}

type SegmentedOption = { value: string | number; label: string }

const Segmented = ({
	options,
	value,
	onChange,
	disabled,
	className,
}: {
	options: SegmentedOption[]
	value: string | number
	onChange: (value: string | number) => void
	disabled?: boolean
	className?: string
}) => (
	<div className={cn('inline-flex flex-wrap gap-1 rounded-xl bg-slate-100 p-1', disabled && 'opacity-60', className)}>
		{options.map((option) => (
			<button
				key={option.value}
				onClick={() => onChange(option.value)}
				disabled={disabled}
				className={cn(
					'rounded-lg px-3 py-1.5 text-xs font-semibold transition-colors duration-150 motion-reduce:transition-none',
					value === option.value ? 'bg-slate-800 text-white' : 'text-slate-600 hover:bg-white'
				)}
			>
				{option.label}
			</button>
		))}
	</div>
)

const DashboardLogs = () => {
	const { t, i18n } = useTranslation()
	const [windowMinutes, setWindowMinutes] = useState(() => readStoredNumber('logs.window', 60, WINDOW_OPTIONS))
	const [actionType, setActionType] = useState(() => {
		const raw = localStorage.getItem('logs.type')
		return raw && TYPE_OPTIONS.includes(raw) ? raw : 'ALL'
	})

	const [fromDate, setFromDate] = useState(() => {
		const start = new Date()
		start.setHours(0, 0, 0, 0)
		return start
	})
	const [toDate, setToDate] = useState(() => {
		const end = new Date()
		end.setSeconds(0, 0)
		return end
	})

	const { data: actions, isLoading: actionsLoading, isError: actionsError } = useUserActionLogs(windowMinutes, actionType)
	const { data: settings } = useLogSettings()
	const updateSettings = useUpdateLogSettings()
	const download = useDownloadReadings()

	const changeWindow = (value: string | number) => {
		setWindowMinutes(Number(value))
		localStorage.setItem('logs.window', String(value))
	}

	const changeType = (value: string | number) => {
		setActionType(String(value))
		localStorage.setItem('logs.type', String(value))
	}

	const windowLabel = (minutes: number) =>
		minutes < 60 ? `${minutes} ${t('dashboardLogs.units.min')}` : minutes < 1440 ? `${minutes / 60} ${t('dashboardLogs.units.hour')}` : t('eventsJournal.days', { count: minutes / 1440 })

	const fromMs = fromDate.getTime()
	const toMs = toDate.getTime()
	const rangeValid = toMs > fromMs

	const formatTime = (ms: number) =>
		new Date(ms).toLocaleTimeString(i18n.language, { hour: '2-digit', minute: '2-digit', second: '2-digit' })

	const formatDate = (ms: number) =>
		new Date(ms).toLocaleDateString(i18n.language, { day: '2-digit', month: '2-digit', year: 'numeric' })

	const renderEntry = (entry: UserActionEntry, index: number) => (
		<div
			key={`${entry.timestamp}-${index}`}
			className="flex items-start gap-4 border-b border-slate-100 py-3 last:border-0 last:pb-0"
		>
			<div className="w-20 shrink-0 text-right">
				<div className="text-sm font-semibold tabular-nums text-slate-800">{formatTime(entry.timestamp)}</div>
				<div className="text-xs tabular-nums text-slate-400">{formatDate(entry.timestamp)}</div>
			</div>
			<div className="min-w-0 flex-1">
				<div className="text-sm font-medium text-slate-800">{entry.action}</div>
				<div className="mt-0.5 flex flex-wrap items-center gap-2 text-xs text-slate-500">
					<span className="font-medium text-slate-600">{entry.username}</span>
					<span className="text-slate-300">•</span>
					<span>{t(`dashboardLogs.actions.types.${entry.actionType.toLowerCase()}`, { defaultValue: entry.actionType })}</span>
				</div>
				{entry.details && (
					<div className={cn('mt-1 text-xs', entry.success ? 'text-slate-500' : 'text-rose-600')}>
						{entry.details}
					</div>
				)}
			</div>
			<span
				className={cn(
					'shrink-0 rounded-full px-2.5 py-1 text-[11px] font-semibold whitespace-nowrap',
					entry.success ? 'bg-emerald-50 text-emerald-700' : 'bg-rose-50 text-rose-600'
				)}
			>
				{entry.success ? t('dashboardLogs.actions.success') : t('dashboardLogs.actions.error')}
			</span>
		</div>
	)

	return (
		<div className="space-y-4 p-1">
			<SystemEventsJournal mode="logs" />
			<div className={cardClassName}>
				<div className="flex flex-wrap items-start justify-between gap-4">
					<div className="flex min-w-0 items-center gap-4">
						<div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-slate-50 text-slate-900">
							<History className="h-5 w-5" />
						</div>
						<div className="min-w-0">
							<h3 className="text-lg font-semibold text-slate-900">{t('dashboardLogs.actions.title')}</h3>
							<p className="text-xs text-slate-500">{t('dashboardLogs.actions.subtitle')}</p>
						</div>
					</div>
					<Segmented
						options={WINDOW_OPTIONS.map((m) => ({ value: m, label: windowLabel(m) }))}
						value={windowMinutes}
						onChange={changeWindow}
						className="max-w-full justify-start"
					/>
				</div>

				<div className="mt-4">
					<Segmented
						options={TYPE_OPTIONS.map((type) => ({
							value: type,
							label: t(`dashboardLogs.actions.filters.${type.toLowerCase()}`, { defaultValue: type }),
						}))}
						value={actionType}
						onChange={changeType}
					/>
				</div>

				<div className="mt-2">
					{actionsLoading ? (
						<div className="space-y-3 py-3">
							{[0, 1, 2].map((i) => (
								<div key={i} className="h-10 rounded-xl bg-slate-100 animate-pulse motion-reduce:animate-none" />
							))}
						</div>
					) : actionsError ? (
						<div className="rounded-2xl border border-dashed border-slate-200 bg-slate-50 p-6 text-center text-sm text-slate-400">
							{t('dashboardLogs.actions.loadError')}
						</div>
					) : !actions || actions.length === 0 ? (
						<div className="rounded-2xl border border-dashed border-slate-200 bg-slate-50 p-6 text-center text-sm text-slate-400">
							{t('dashboardLogs.actions.empty')}
						</div>
					) : (
						<div className="max-h-[440px] overflow-y-auto pr-1 [scrollbar-gutter:stable]">{actions.map(renderEntry)}</div>
					)}
				</div>
			</div>

			<div className={cn(cardClassName, '[animation-delay:80ms]')}>
				<div className="flex items-center gap-4">
					<div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-slate-50 text-slate-900">
						<FileSpreadsheet className="h-5 w-5" />
					</div>
					<div>
						<h3 className="text-lg font-semibold text-slate-900">{t('dashboardLogs.readings.title')}</h3>
						<p className="text-xs text-slate-500">{t('dashboardLogs.readings.subtitle')}</p>
					</div>
				</div>

				<div className="mt-5 flex flex-wrap items-center justify-between gap-3 rounded-xl bg-slate-50 p-4">
					<span className="text-sm font-medium text-slate-700">{t('dashboardLogs.readings.interval')}</span>
					<Segmented
						options={INTERVAL_OPTIONS.map((m) => ({ value: m, label: windowLabel(m) }))}
						value={settings?.intervalMinutes ?? 5}
						onChange={(value) => updateSettings.mutate(Number(value))}
						disabled={updateSettings.isPending}
						className="ml-auto justify-end"
					/>
				</div>

				<div className="mt-5 grid gap-4 sm:grid-cols-2">
					<div>
						<span className="mb-1.5 block text-xs font-medium uppercase tracking-wide text-slate-500">
							{t('dashboardLogs.readings.from')}
						</span>
						<DateTimePicker value={fromDate} onChange={setFromDate} />
					</div>
					<div>
						<span className="mb-1.5 block text-xs font-medium uppercase tracking-wide text-slate-500">
							{t('dashboardLogs.readings.to')}
						</span>
						<DateTimePicker value={toDate} onChange={setToDate} />
					</div>
				</div>

				<button
					onClick={() => rangeValid && download.mutate({ from: fromMs, to: toMs })}
					disabled={!rangeValid || download.isPending}
					className="mt-5 flex h-12 w-full items-center justify-center gap-2 rounded-xl bg-slate-900 text-sm font-semibold text-white transition-colors duration-150 motion-reduce:transition-none hover:bg-slate-800 disabled:pointer-events-none disabled:opacity-50"
				>
					{download.isPending ? (
						<Loader2 className="h-4 w-4 animate-spin motion-reduce:animate-none" />
					) : (
						<Download className="h-4 w-4" />
					)}
					{download.isPending ? t('dashboardLogs.readings.downloading') : t('dashboardLogs.readings.download')}
				</button>

				<p className="mt-2 text-center text-xs text-slate-400">
					{rangeValid ? t('dashboardLogs.readings.hint') : t('dashboardLogs.readings.invalidRange')}
				</p>
			</div>
		</div>
	)
}

export default DashboardLogs
