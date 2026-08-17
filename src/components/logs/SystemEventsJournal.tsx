import { useEffect, useMemo, useState } from 'react'
import {
	Activity,
	AlertCircle,
	AlertTriangle,
	CheckCircle2,
	Clock,
	Cpu,
	Info,
	RefreshCw,
	Server,
} from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { cn } from '@/shared/lib/utils'
import { useSystemEvents } from '@/api/useSystemEvents'
import type { SystemEvent } from '@/api/typesApi'

const TIME_OPTIONS = [15, 60, 240, 1440]
const FILTER_OPTIONS = ['ALL', 'CRITICAL', 'WARNING', 'INFO'] as const

type Severity = 'CRITICAL' | 'WARNING' | 'INFO' | 'SUCCESS'

const cleanMessage = (msg: string | undefined): string => {
	if (!msg) return 'Нет описания'

	const clean = msg
		.replace(/^Ошибка исполнения:\s*/i, '')
		.replace(/^AirPLCONCycle\s*/i, '')
		.replace(/byteCount=\d+/g, '')
		.replace(/unit=\d+/g, '')
		.replace(/тайм-аут \d+/g, 'тайм-аут')
		.replace(/read=\d+:\d+\[\d+\]/g, '')

	if (msg.includes('AirPLCONCycle')) {
		if (msg.includes('warning:')) return msg.split('warning:')[1].trim()
		if (msg.includes('error:')) return msg.split('error:')[1].trim()
		if (msg.includes('fatal:')) return msg.split('fatal:')[1].trim()
		if (clean.includes('аварий 0') && clean.includes('сбоев 0') && clean.includes('предупреждений 0')) {
			return 'Системный цикл: Норма'
		}
	}
	return clean.trim()
}

const determineSeverity = (level: number, rawText: string): Severity => {
	const lowerText = rawText.toLowerCase()

	if (
		lowerText.includes('системный цикл: норма') ||
		(lowerText.includes('аварий 0') && lowerText.includes('сбоев 0') && lowerText.includes('предупреждений 0'))
	) {
		return 'SUCCESS'
	}

	if (level >= 2) return 'CRITICAL'
	if (level === 1) return 'WARNING'

	if (
		lowerText.includes('fatal') ||
		lowerText.includes('fail') ||
		lowerText.includes('error') ||
		lowerText.includes('авария') ||
		lowerText.includes('сбой') ||
		lowerText.includes('тайм-аут')
	) {
		return 'CRITICAL'
	}
	if (
		lowerText.includes('warning') ||
		lowerText.includes('warn') ||
		lowerText.includes('предупреждение') ||
		lowerText.includes('внимание')
	) {
		return 'WARNING'
	}
	return 'INFO'
}

const SEVERITY_CONFIG: Record<Severity, { tile: string; title: string; icon: typeof AlertCircle }> = {
	CRITICAL: { tile: 'bg-rose-600', title: 'text-rose-700', icon: AlertCircle },
	WARNING: { tile: 'bg-amber-500', title: 'text-amber-700', icon: AlertTriangle },
	INFO: { tile: 'bg-slate-700', title: 'text-slate-800', icon: Info },
	SUCCESS: { tile: 'bg-emerald-600', title: 'text-emerald-700', icon: CheckCircle2 },
}

const Segmented = ({
	options,
	value,
	onChange,
	className,
}: {
	options: { value: string | number; label: string }[]
	value: string | number
	onChange: (value: string | number) => void
	className?: string
}) => (
	<div className={cn('inline-flex flex-wrap gap-1 rounded-xl bg-slate-100 p-1', className)}>
		{options.map((option) => (
			<button
				key={option.value}
				onClick={() => onChange(option.value)}
				className={cn(
					'rounded-lg px-3 py-1.5 text-xs font-semibold transition-colors duration-300',
					value === option.value ? 'bg-slate-800 text-white' : 'text-slate-600 hover:bg-white'
				)}
			>
				{option.label}
			</button>
		))}
	</div>
)

const SystemEventsJournal = () => {
	const { t, i18n } = useTranslation()
	const [minutes, setMinutes] = useState(60)
	const [filterType, setFilterType] = useState<string>('ALL')
	const [now, setNow] = useState(Date.now())
	const { data, isLoading, isError, refetch, isFetching } = useSystemEvents(minutes)

	useEffect(() => {
		const timer = setInterval(() => setNow(Date.now()), 30000)
		return () => clearInterval(timer)
	}, [])

	const processedEvents = useMemo(() => {
		if (!data) return []
		return data
			.map((event) => {
				const rawMsg = event.comment || event.title || ''
				const cleanedMsg = cleanMessage(rawMsg)
				const severity = determineSeverity(event.level, cleanedMsg + ' ' + rawMsg)
				return { ...event, severity, cleanedMsg }
			})
			.sort((a, b) => (b.arrivalTime?.timeInMS ?? 0) - (a.arrivalTime?.timeInMS ?? 0))
	}, [data])

	const stats = useMemo(
		() =>
			processedEvents.reduce(
				(acc, e) => {
					if (e.severity === 'CRITICAL') acc.critical++
					if (e.severity === 'WARNING') acc.warning++
					return acc
				},
				{ critical: 0, warning: 0 }
			),
		[processedEvents]
	)

	const filteredEvents = useMemo(() => {
		if (filterType === 'ALL') return processedEvents
		return processedEvents.filter((e) => {
			if (filterType === 'CRITICAL') return e.severity === 'CRITICAL'
			if (filterType === 'WARNING') return e.severity === 'WARNING'
			if (filterType === 'INFO') return e.severity === 'INFO' || e.severity === 'SUCCESS'
			return true
		})
	}, [processedEvents, filterType])

	const windowLabel = (m: number) =>
		m < 60 ? `${m} ${t('dashboardLogs.units.min')}` : `${m / 60} ${t('dashboardLogs.units.hour')}`

	const typeLabel = (type?: number) => {
		if (type != null && [1, 3, 4, 6, 16].includes(type)) return t(`eventsJournal.types.t${type}`)
		return type != null ? `${t('eventsJournal.types.unknown')} ${type}` : t('eventsJournal.types.t4')
	}

	const formatTimestamp = (ms?: number) => {
		if (!ms) return '-'
		return new Date(ms).toLocaleString(i18n.language, {
			day: '2-digit',
			month: '2-digit',
			hour: '2-digit',
			minute: '2-digit',
			second: '2-digit',
		})
	}

	const formatRelativeTime = (ms?: number) => {
		if (!ms) return ''
		const diffMinutes = Math.floor((now - ms) / 60000)
		if (diffMinutes < 1) return t('eventsJournal.justNow')
		if (diffMinutes < 60) return t('eventsJournal.minutesAgo', { value: diffMinutes })
		const hours = Math.floor(diffMinutes / 60)
		if (hours < 24) return t('eventsJournal.hoursAgo', { value: hours })
		return new Date(ms).toLocaleDateString(i18n.language, {
			day: '2-digit',
			month: '2-digit',
			hour: '2-digit',
			minute: '2-digit',
		})
	}

	const renderEvent = (event: SystemEvent & { severity: Severity; cleanedMsg: string }, index: number) => {
		const config = SEVERITY_CONFIG[event.severity]
		const IconComponent = config.icon

		return (
			<div
				key={`${event.type}-${event.arrivalTime?.timeInMS ?? index}-${index}`}
				className="rounded-xl border border-slate-200 bg-white p-3 transition-colors hover:border-slate-300 animate-in fade-in fill-mode-backwards duration-500 motion-reduce:animate-none"
				style={{ animationDelay: `${Math.min(index, 10) * 40}ms` }}
			>
				<div className="flex gap-3">
					<div className={cn('mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-xl text-white', config.tile)}>
						<IconComponent className="h-4 w-4" />
					</div>
					<div className="min-w-0 flex-1">
						<div className="flex items-start justify-between gap-3">
							<p className={cn('min-w-0 break-words text-sm font-semibold leading-snug', config.title)}>
								{event.cleanedMsg}
							</p>
							<span
								className="flex shrink-0 items-center gap-1.5 text-xs tabular-nums text-slate-400"
								title={formatTimestamp(event.arrivalTime?.timeInMS)}
							>
								<Clock className="h-3.5 w-3.5" />
								{formatRelativeTime(event.arrivalTime?.timeInMS)}
							</span>
						</div>
						<div className="mt-1 flex min-w-0 items-center gap-2 text-xs text-slate-500">
							{event.type === 16 ? (
								<Cpu className="h-3 w-3 shrink-0 text-slate-400" />
							) : (
								<Server className="h-3 w-3 shrink-0 text-slate-400" />
							)}
							<span className="truncate">{typeLabel(event.type)}</span>
							{event.equipName && event.equipName !== 'AirPLC' && (
								<>
									<span className="text-slate-300">/</span>
									<span className="max-w-[140px] truncate text-slate-400">{event.equipName}</span>
								</>
							)}
						</div>
					</div>
				</div>
			</div>
		)
	}

	return (
		<div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm animate-in fade-in fill-mode-backwards duration-500 motion-reduce:animate-none">
			<div className="flex flex-wrap items-start justify-between gap-4">
				<div className="flex items-center gap-4">
					<div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-slate-100 text-slate-700">
						<Activity className="h-5 w-5" />
					</div>
					<div>
						<div className="flex items-center gap-2">
							<h3 className="text-lg font-semibold text-slate-900">{t('eventsJournal.title')}</h3>
							{stats.critical > 0 && (
								<span className="rounded-full bg-rose-600 px-2 py-0.5 text-[11px] font-semibold text-white">
									{stats.critical}
								</span>
							)}
							{stats.warning > 0 && (
								<span className="rounded-full bg-amber-500 px-2 py-0.5 text-[11px] font-semibold text-white">
									{stats.warning}
								</span>
							)}
							{stats.critical === 0 && stats.warning === 0 && (
								<span className="rounded-full bg-slate-100 px-2 py-0.5 text-[11px] font-semibold text-slate-500">
									{processedEvents.length}
								</span>
							)}
						</div>
						<p className="text-xs text-slate-500">{t('eventsJournal.subtitle')}</p>
					</div>
				</div>
				<button
					onClick={() => refetch()}
					disabled={isFetching}
					className="flex h-9 w-9 items-center justify-center rounded-xl border border-slate-200 text-slate-500 transition-colors hover:bg-slate-50 disabled:opacity-50"
				>
					<RefreshCw className={cn('h-4 w-4', isFetching && 'animate-spin motion-reduce:animate-none')} />
				</button>
			</div>

			<div className="mt-4 flex flex-wrap items-center justify-between gap-2">
				<Segmented
					options={FILTER_OPTIONS.map((f) => ({ value: f, label: t(`eventsJournal.filters.${f.toLowerCase()}`) }))}
					value={filterType}
					onChange={(value) => setFilterType(String(value))}
				/>
				<Segmented
					options={TIME_OPTIONS.map((m) => ({ value: m, label: windowLabel(m) }))}
					value={minutes}
					onChange={(value) => setMinutes(Number(value))}
					className="ml-auto justify-end"
				/>
			</div>

			<div className="mt-4 max-h-[540px] space-y-2 overflow-y-auto pr-1 [scrollbar-gutter:stable]">
				{isLoading ? (
					<div className="space-y-2">
						{[0, 1, 2].map((i) => (
							<div key={i} className="h-16 rounded-xl bg-slate-100 animate-pulse motion-reduce:animate-none" />
						))}
					</div>
				) : isError ? (
					<div className="rounded-2xl border border-dashed border-slate-200 bg-slate-50 p-6 text-center text-sm text-slate-400">
						{t('eventsJournal.loadError')}
					</div>
				) : filteredEvents.length === 0 ? (
					<div className="rounded-2xl border border-dashed border-slate-200 bg-slate-50 p-6 text-center text-sm text-slate-400">
						{t('eventsJournal.empty')}
					</div>
				) : (
					filteredEvents.map(renderEvent)
				)}
			</div>
		</div>
	)
}

export default SystemEventsJournal
