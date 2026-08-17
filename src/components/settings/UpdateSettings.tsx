import { useCallback, useEffect, useRef, useState } from 'react'
import {
	AlertTriangle,
	Check,
	CloudDownload,
	Download,
	Info,
	Loader2,
	Save,
	Search,
	Upload,
} from 'lucide-react'
import { toast } from 'sonner'
import { Button } from '@/shared/ui/button'
import { Input } from '@/shared/ui/input'
import { cn } from '@/shared/lib/utils'
import {
	useSaveUpgradeStoredUrl,
	useStartUpgradeDownload,
	useStartUpgradeInstall,
	useUpgradeInstallStatus,
	useUpgradeLoadStatus,
	useUpgradeStoredUrl,
	useServerState,
} from '@/api/hooks'
import { useDashboardData } from '@/api/useDashboard'
import { getUpgradeInstallStatus, getUpgradeLoadStatus } from '@/api/deviceApi'
import type { UpgradeStatusData } from '@/api/typesApi'
import { useAuth } from '@/contexts/AuthContext'

type StatusVariant = 'success' | 'warning' | 'error' | 'neutral'

type FlowStep = 'IDLE' | 'CHECKING' | 'AVAILABLE' | 'DOWNLOADING' | 'READY_TO_INSTALL' | 'INSTALLING'

const POLL_MS = 3000

const INSTALL_BLOCKED_BY_CALIBRATION_MESSAGE =
	'Идёт калибровка агрегата: установку обновления (force) запускать нельзя. Проверка статуса без force доступна.'

const formatTimestamp = (value: Date | null) => {
	if (!value) return ''
	return value.toLocaleString()
}

const normalizePayload = (payload?: UpgradeStatusData | null): UpgradeStatusData => ({
	errors: payload?.errors ?? [],
	info: payload?.info ?? [],
})

const isWarningLikeMessage = (message: string) => {
	const normalized = message.toLowerCase()
	return normalized.startsWith('нет обновления') || normalized.startsWith('предупреждение')
}

const getStatusGroups = (payload?: UpgradeStatusData | null) => {
	const normalized = normalizePayload(payload)
	const warnings = normalized.errors.filter(isWarningLikeMessage)
	const errors = normalized.errors.filter((message) => !isWarningLikeMessage(message))

	return {
		info: normalized.info,
		warnings,
		errors,
		hasMessages: normalized.info.length > 0 || warnings.length > 0 || errors.length > 0,
	}
}

const isReadyToInstall = (payload: UpgradeStatusData | null): boolean => {
	if (!payload) return false
	const groups = getStatusGroups(payload)
	if (groups.errors.length > 0) return false
	const all = [...(payload.info ?? []), ...(payload.errors ?? [])].join(' ').toLowerCase()
	const downloaded = all.includes('скачан') // ловит "скачано" / "скачана" / "скачан"
	const notInstalled =
		all.includes('не установлен') || all.includes('готово к установк')
	return downloaded && notInstalled
}

const isDownloadAvailable = (payload: UpgradeStatusData | null): boolean => {
	if (!payload) return false
	if (isReadyToInstall(payload)) return false
	const groups = getStatusGroups(payload)
	if (groups.errors.length > 0) return false
	const all = [...groups.info, ...groups.warnings].join(' ').toLowerCase()
	if (all.includes('нет обновления')) return false
	if (all.includes('нет обновлений')) return false
	return groups.info.length > 0 || groups.warnings.length > 0
}

const getSummaryVariant = (payload?: UpgradeStatusData | null): StatusVariant => {
	const groups = getStatusGroups(payload)
	if (groups.errors.length > 0) return 'error'
	if (groups.warnings.length > 0) return 'warning'
	if (groups.info.length > 0) return 'success'
	return 'neutral'
}

const badgeClassNameByVariant: Record<StatusVariant, string> = {
	success: 'bg-emerald-100 text-emerald-700',
	warning: 'bg-amber-100 text-amber-700',
	error: 'bg-red-100 text-red-700',
	neutral: 'bg-slate-100 text-slate-500',
}

const dotClassNameByVariant: Record<StatusVariant, string> = {
	success: 'bg-emerald-500',
	warning: 'bg-amber-500',
	error: 'bg-red-500',
	neutral: 'bg-slate-300',
}

// карточки появляются каскадом, поэтому анимация вынесена в общий класс
const cardClassName =
	'rounded-2xl border border-slate-200 bg-white p-5 shadow-sm animate-in fade-in fill-mode-backwards duration-500 motion-reduce:animate-none'

const StatusMessages = ({ payload }: { payload: UpgradeStatusData | null }) => {
	const groups = getStatusGroups(payload)
	if (!groups.hasMessages) return null

	// плашки нейтральные, семантика цвета только в заголовке группы —
	// иначе большие цветные пятна спорят с общей холодной гаммой
	const blocks: Array<{ key: string; icon: typeof Info; heading: string; items: string[]; headingClassName: string }> = [
		{
			key: 'info',
			icon: Info,
			heading: 'Инфо',
			items: groups.info,
			headingClassName: 'text-sky-700',
		},
		{
			key: 'warning',
			icon: AlertTriangle,
			heading: 'Предупреждение',
			items: groups.warnings,
			headingClassName: 'text-amber-700',
		},
		{
			key: 'error',
			icon: AlertTriangle,
			heading: 'Ошибки',
			items: groups.errors,
			headingClassName: 'text-red-600',
		},
	]

	return (
		<div className="mt-3 space-y-3">
			{blocks
				.filter((block) => block.items.length > 0)
				.map((block) => (
					<div key={block.key} className="space-y-2 animate-in fade-in duration-300 motion-reduce:animate-none">
						<div
							className={cn(
								'flex items-center gap-2 text-xs font-semibold uppercase tracking-wide',
								block.headingClassName
							)}
						>
							<block.icon className="h-3.5 w-3.5" />
							{block.heading}
						</div>
						<div className="space-y-2">
							{block.items.map((message, index) => (
								<div
									key={`${block.key}-${index}`}
									className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-700"
								>
									{message}
								</div>
							))}
						</div>
					</div>
				))}
		</div>
	)
}

const TimelineItem = ({
	title,
	subtitle,
	payload,
	timestamp,
	emptyLabel,
	inProgress,
	last,
}: {
	title: string
	subtitle: string
	payload: UpgradeStatusData | null
	timestamp: Date | null
	emptyLabel: string
	inProgress?: boolean
	last?: boolean
}) => {
	const variant = getSummaryVariant(payload)
	const badgeLabel = inProgress
		? 'В процессе'
		: variant === 'success'
			? 'OK'
			: variant === 'warning'
				? 'Предупреждение'
				: 'Ошибка'

	return (
		<li className={cn('relative pl-6', !last && 'pb-5')}>
			{!last && <span className="absolute bottom-0 left-[5px] top-5 w-px bg-slate-200" />}
			{/* key заставляет точку и бейдж перемонтироваться при смене статуса,
			    чтобы пульс сыграл два раза и остановился */}
			<span
				key={inProgress ? 'progress' : variant}
				className={cn(
					'absolute left-0 top-1.5 h-[11px] w-[11px] rounded-full ring-4 ring-white',
					inProgress ? 'bg-amber-500' : dotClassNameByVariant[variant],
					(inProgress || variant !== 'neutral') &&
						'animate-[status-pulse_0.8s_ease-in-out_2] motion-reduce:animate-none'
				)}
			/>
			<div className="flex flex-col gap-1 sm:flex-row sm:items-start sm:justify-between sm:gap-4">
				<div className="min-w-0">
					<p className="text-sm font-medium text-slate-800">{title}</p>
					<p className="mt-0.5 text-xs text-slate-500">{subtitle}</p>
				</div>
				<div className="flex shrink-0 items-center gap-2">
					{timestamp && (
						<span className="text-xs tabular-nums text-slate-400">{formatTimestamp(timestamp)}</span>
					)}
					{(inProgress || variant !== 'neutral') && (
						<span
							key={badgeLabel}
							className={cn(
								'rounded-full px-2.5 py-1 text-[11px] font-semibold',
								inProgress ? 'bg-amber-100 text-amber-700' : badgeClassNameByVariant[variant],
								'animate-[status-pulse_0.8s_ease-in-out_2] motion-reduce:animate-none'
							)}
						>
							{badgeLabel}
						</span>
					)}
				</div>
			</div>
			{payload ? (
				<StatusMessages payload={payload} />
			) : (
				<p className="mt-2 text-xs text-slate-400">{emptyLabel}</p>
			)}
		</li>
	)
}

const STEP_LABELS = ['Проверка', 'Загрузка', 'Готово к установке', 'Установлено']

const UpdateSettings = () => {
	const [loadResult, setLoadResult] = useState<UpgradeStatusData | null>(null)
	const [installResult, setInstallResult] = useState<UpgradeStatusData | null>(null)
	const [loadCheckedAt, setLoadCheckedAt] = useState<Date | null>(null)
	const [installCheckedAt, setInstallCheckedAt] = useState<Date | null>(null)

	const [controllerUrl, setControllerUrl] = useState('')

	const [step, setStep] = useState<FlowStep>('IDLE')
	const [installTimer, setInstallTimer] = useState<number | null>(null)

	const pollingRef = useRef<ReturnType<typeof setInterval> | null>(null)
	const installCountdownRef = useRef<ReturnType<typeof setInterval> | null>(null)

	const { logout } = useAuth()

	const { data: dashboardData } = useDashboardData()
	const installBlockedByCalibration = dashboardData?.calibration?.calibrating === true

	const { data: serverState, isFetching: serverStateLoading } = useServerState(true)
	const { data: storedUrlQuery, isFetching: storedUrlLoading } = useUpgradeStoredUrl(true)
	const saveStoredUrlMutation = useSaveUpgradeStoredUrl()

	const checkLoadStatusMutation = useUpgradeLoadStatus()
	const startDownloadMutation = useStartUpgradeDownload()
	const checkInstallStatusMutation = useUpgradeInstallStatus()
	const startInstallMutation = useStartUpgradeInstall()

	// проверка и загрузка идут по тому же url, что хранится на контроллере,
	// чтобы не было двух разных путей обновления
	const trimmedUrl = controllerUrl.trim()

	const stopPolling = useCallback(() => {
		if (pollingRef.current) {
			clearInterval(pollingRef.current)
			pollingRef.current = null
		}
	}, [])

	useEffect(() => {
		if (storedUrlQuery?.url !== undefined) {
			setControllerUrl(storedUrlQuery.url)
		}
	}, [storedUrlQuery])

	useEffect(() => {
		return () => {
			stopPolling()
			if (installCountdownRef.current) {
				clearInterval(installCountdownRef.current)
			}
		}
	}, [stopPolling])

	const startPollingAfterDownload = useCallback(
		(urlParam: string) => {
			stopPolling()
			const tick = async () => {
				try {
					const u = urlParam || undefined
					const load = await getUpgradeLoadStatus(u)
					setLoadResult(load)
					setLoadCheckedAt(new Date())
					const install = await getUpgradeInstallStatus()
					setInstallResult(install)
					setInstallCheckedAt(new Date())
					if (isReadyToInstall(install)) {
						stopPolling()
						setStep('READY_TO_INSTALL')
					}
				} catch {
					// polling continues
				}
			}
			void tick()
			pollingRef.current = setInterval(() => void tick(), POLL_MS)
		},
		[stopPolling]
	)

	const handleCheckUpdates = () => {
		setStep('CHECKING')
		checkLoadStatusMutation.mutate(trimmedUrl || undefined, {
			onSuccess: (data) => {
				setLoadResult(data)
				setLoadCheckedAt(new Date())
				checkInstallStatusMutation.mutate(undefined, {
					onSuccess: (install) => {
						setInstallResult(install)
						setInstallCheckedAt(new Date())
						if (isReadyToInstall(install)) {
							setStep('READY_TO_INSTALL')
						} else if (isDownloadAvailable(data)) {
							setStep('AVAILABLE')
						} else {
							setStep('IDLE')
						}
					},
					onError: () => {
						if (isDownloadAvailable(data)) {
							setStep('AVAILABLE')
						} else {
							setStep('IDLE')
						}
					},
				})
			},
			onError: () => setStep('IDLE'),
		})
	}

	const handleStartDownload = () => {
		setStep('DOWNLOADING')
		startDownloadMutation.mutate(trimmedUrl || undefined, {
			onSuccess: (data) => {
				setLoadResult(data)
				setLoadCheckedAt(new Date())
				startPollingAfterDownload(trimmedUrl)
			},
			onError: () => {
				stopPolling()
				setStep('AVAILABLE')
			},
		})
	}

	const handleInstall = () => {
		if (installBlockedByCalibration) {
			toast.error(INSTALL_BLOCKED_BY_CALIBRATION_MESSAGE)
			return
		}
		setStep('INSTALLING')
		setInstallTimer(30)
		if (installCountdownRef.current) {
			clearInterval(installCountdownRef.current)
		}
		startInstallMutation.mutate(undefined, {
			onSuccess: () => {
				let left = 30
				installCountdownRef.current = setInterval(() => {
					left -= 1
					setInstallTimer(left)
					if (left <= 0) {
						if (installCountdownRef.current) {
							clearInterval(installCountdownRef.current)
							installCountdownRef.current = null
						}
						// Гасим сессию и чистим токен, иначе /login отбросит ещё авторизованного
						// юзера обратно на дашборд и релогина не произойдёт. Редиректим в любом случае.
						logout()
						window.location.href = '/login'
					}
				}, 1000)
			},
			onError: () => {
				setStep('READY_TO_INSTALL')
				setInstallTimer(null)
			},
		})
	}

	const versionLabel = serverState?.version?.trim() || 'неизвестно'
	const releaseLabel =
		typeof serverState?.releaseNumber === 'number' ? String(serverState.releaseNumber) : '—'

	const stepIndex =
		step === 'IDLE' || step === 'CHECKING'
			? 0
			: step === 'AVAILABLE' || step === 'DOWNLOADING'
				? 1
				: step === 'READY_TO_INSTALL'
					? 2
					: 3

	return (
		<div className="relative p-1">
			<div className="space-y-4">
				{/* Версии */}
				<div className={cn(cardClassName, 'flex items-center gap-4')}>
					<div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-slate-100 text-slate-700">
						<CloudDownload className="h-6 w-6" />
					</div>
					<div className="min-w-0">
						<p className="text-xs font-medium uppercase tracking-wide text-slate-500">
							Текущая версия
						</p>
						{serverStateLoading ? (
							<Loader2 className="mt-1 h-5 w-5 animate-spin text-slate-400" />
						) : (
							<p className="truncate text-lg font-semibold text-slate-900">{versionLabel}</p>
						)}
						<p className="mt-0.5 text-xs text-slate-500">
							Сборка {releaseLabel}
							<span className="mx-2 text-slate-300">•</span>
							Версия веб-панели {__APP_VERSION__}{' '}
							<span className="text-slate-400">
								(сборка от {__BUILD_DATE__})
							</span>
						</p>
					</div>
				</div>

				{/* Шаги */}
				<div className={cn(cardClassName, '[animation-delay:80ms]')}>
					<ol className="flex w-full">
						{STEP_LABELS.map((label, index) => {
							const isCompleted = index < stepIndex
							const isCurrent = index === stepIndex
							return (
								<li key={label} className="relative flex flex-1 flex-col items-center gap-2">
									{index > 0 && (
										<span
											className={cn(
												'absolute left-[calc(-50%+1.25rem)] right-[calc(50%+1.25rem)] top-4 h-0.5 rounded-full transition-colors duration-500',
												index <= stepIndex ? 'bg-slate-800' : 'bg-slate-200'
											)}
										/>
									)}
									<span
										className={cn(
											'flex h-8 w-8 items-center justify-center rounded-full text-sm font-semibold transition-all duration-500',
											isCompleted && 'bg-slate-800 text-white',
											isCurrent && 'bg-slate-800 text-white ring-4 ring-slate-800/15',
											!isCompleted && !isCurrent && 'bg-slate-100 text-slate-400'
										)}
									>
										{isCompleted ? <Check className="h-4 w-4" /> : index + 1}
									</span>
									<span
										className={cn(
											'px-1 text-center text-[11px] font-medium leading-tight transition-colors duration-300 sm:text-xs',
											isCurrent ? 'text-slate-900' : 'text-slate-500'
										)}
									>
										{label}
									</span>
								</li>
							)
						})}
					</ol>
				</div>

				{/* URL сервера обновлений */}
				<div className={cn(cardClassName, '[animation-delay:160ms]')}>
					<label className="mb-2 block text-xs font-semibold uppercase tracking-wide text-slate-500">
						URL сервера обновлений
					</label>
					<div className="flex flex-col gap-2 sm:flex-row sm:items-center">
						<Input
							value={controllerUrl}
							onChange={(event) => setControllerUrl(event.target.value)}
							placeholder="http://172.16.7.109/Upgrade.smb"
							disabled={storedUrlLoading || saveStoredUrlMutation.isPending}
							className="h-11 rounded-xl border-slate-200 bg-slate-50 sm:flex-1"
						/>
						<Button
							type="button"
							variant="secondary"
							className="h-11 shrink-0 rounded-xl bg-slate-700 px-5 text-white transition-all duration-200 hover:bg-slate-800 active:scale-[0.98]"
							disabled={storedUrlLoading || saveStoredUrlMutation.isPending}
							onClick={() => {
								saveStoredUrlMutation.mutate(controllerUrl.trim(), {
									onSuccess: () => toast.success('URL записан на контроллер'),
									onError: () => toast.error('Не удалось записать URL'),
								})
							}}
						>
							{saveStoredUrlMutation.isPending ? (
								<Loader2 className="animate-spin" />
							) : (
								<Save className="h-4 w-4" />
							)}
							Изменить путь к серверу обновлений
						</Button>
					</div>
					<p className="mt-2 text-xs text-slate-500">
						{storedUrlLoading
							? 'Загрузка с контроллера…'
							: 'Хранится на контроллере. Проверка и загрузка обновления идут по этому же адресу.'}
					</p>
				</div>

				{/* Статус обновления */}
				<div className={cn(cardClassName, '[animation-delay:240ms]')}>
					<h3 className="text-sm font-semibold text-slate-800">Статус обновления</h3>
					<ol className="mt-4">
						<TimelineItem
							title="Ответ по загрузке"
							subtitle="Сообщения от метода `/upgrade/load`."
							payload={loadResult}
							timestamp={loadCheckedAt}
							emptyLabel="Пока нет ответа по загрузке обновления."
							inProgress={step === 'CHECKING' || step === 'DOWNLOADING'}
						/>
						<TimelineItem
							title="Ответ по установке"
							subtitle="Сообщения от метода `/upgrade/execute`."
							payload={installResult}
							timestamp={installCheckedAt}
							emptyLabel="Пока нет ответа по установке обновления."
							inProgress={step === 'INSTALLING'}
							last
						/>
					</ol>
				</div>

				{installBlockedByCalibration && (
					<div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800 animate-in fade-in duration-300 motion-reduce:animate-none">
						{INSTALL_BLOCKED_BY_CALIBRATION_MESSAGE}
					</div>
				)}

				{/* Действие по текущему шагу */}
				{step === 'INSTALLING' ? (
					<div className={cn(cardClassName, 'p-8 text-center')}>
						<Loader2 className="mx-auto mb-4 h-10 w-10 animate-spin text-slate-800" />
						<h3 className="text-lg font-semibold text-slate-900">Идёт установка</h3>
						<p className="mt-2 text-sm text-slate-600">
							Подождите. Через {installTimer ?? 0} с откроется страница входа.
						</p>
					</div>
				) : (
					<div className="flex flex-col gap-2 sm:flex-row sm:items-center animate-in fade-in fill-mode-backwards duration-500 [animation-delay:320ms] motion-reduce:animate-none">
						{(step === 'IDLE' || step === 'CHECKING') && (
							<Button
								type="button"
								className="h-12 w-full justify-center rounded-xl bg-slate-800 text-sm font-semibold text-white transition-all duration-200 hover:bg-slate-900 active:scale-[0.99]"
								onClick={handleCheckUpdates}
								disabled={step === 'CHECKING'}
							>
								{step === 'CHECKING' ? <Loader2 className="animate-spin" /> : <Search className="h-4 w-4" />}
								Проверить обновления
							</Button>
						)}
						{(step === 'AVAILABLE' || step === 'DOWNLOADING') && (
							<>
								<Button
									type="button"
									className="h-12 w-full justify-center rounded-xl bg-sky-600 text-sm font-semibold text-white transition-all duration-200 hover:bg-sky-700 active:scale-[0.99] sm:flex-1"
									onClick={handleStartDownload}
									disabled={step === 'DOWNLOADING'}
								>
									{step === 'DOWNLOADING' ? (
										<Loader2 className="animate-spin" />
									) : (
										<Download className="h-4 w-4" />
									)}
									{step === 'DOWNLOADING' ? 'Загрузка обновления…' : 'Загрузить обновление'}
								</Button>
								{step === 'AVAILABLE' && (
									<Button
										type="button"
										variant="ghost"
										className="h-12 rounded-xl px-5"
										onClick={() => setStep('IDLE')}
									>
										Отмена
									</Button>
								)}
							</>
						)}
						{step === 'READY_TO_INSTALL' && (
							<>
								<Button
									type="button"
									className="h-12 w-full justify-center rounded-xl bg-emerald-600 text-sm font-semibold text-white transition-all duration-200 hover:bg-emerald-700 active:scale-[0.99] sm:flex-1"
									onClick={handleInstall}
									disabled={installBlockedByCalibration}
								>
									<Upload className="h-4 w-4" />
									Установить обновление
								</Button>
								<Button
									type="button"
									variant="ghost"
									className="h-12 rounded-xl px-5"
									onClick={() => setStep('IDLE')}
								>
									Отмена
								</Button>
							</>
						)}
					</div>
				)}
			</div>
		</div>
	)
}

export default UpdateSettings
