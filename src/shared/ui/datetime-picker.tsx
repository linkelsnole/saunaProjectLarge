import { useState } from 'react'
import { format } from 'date-fns'
import { ru } from 'date-fns/locale'
import { CalendarDays, Clock } from 'lucide-react'
import { cn } from '@/shared/lib/utils'
import { Calendar } from '@/shared/ui/calendar'
import { Popover, PopoverContent, PopoverTrigger } from '@/shared/ui/popover'

const pad = (n: number) => String(n).padStart(2, '0')

type DateTimePickerProps = {
	value: Date
	onChange: (value: Date) => void
	className?: string
}

export const DateTimePicker = ({ value, onChange, className }: DateTimePickerProps) => {
	const [open, setOpen] = useState(false)
	const [timeDraft, setTimeDraft] = useState<string | null>(null)
	const timeValue = `${pad(value.getHours())}:${pad(value.getMinutes())}`

	const applyDay = (day: Date | undefined) => {
		if (!day) return
		const next = new Date(day)
		next.setHours(value.getHours(), value.getMinutes(), 0, 0)
		onChange(next)
	}

	const handleTimeInput = (input: string) => {
		const digits = input.replace(/\D/g, '').slice(0, 4)
		setTimeDraft(digits.length > 2 ? `${digits.slice(0, 2)}:${digits.slice(2)}` : digits)
	}

	const commitTime = () => {
		if (timeDraft !== null) {
			const digits = timeDraft.replace(/\D/g, '')
			if (digits.length >= 3) {
				const hours = Math.min(23, parseInt(digits.slice(0, 2), 10) || 0)
				const minutes = Math.min(59, parseInt(digits.slice(2, 4), 10) || 0)
				const next = new Date(value)
				next.setHours(hours, minutes, 0, 0)
				onChange(next)
			}
		}
		setTimeDraft(null)
	}

	return (
		<Popover open={open} onOpenChange={setOpen} modal>
			<PopoverTrigger asChild>
				<button
					className={cn(
						'flex h-11 w-full items-center gap-2.5 rounded-xl border border-slate-200 bg-white px-3 text-sm text-slate-800 transition-colors duration-300 hover:border-slate-300 focus:border-slate-400 focus:outline-none',
						open && 'border-slate-400 hover:border-slate-400',
						className
					)}
				>
					<CalendarDays className="h-4 w-4 shrink-0 text-slate-400" />
					<span className="tabular-nums">{format(value, 'dd.MM.yyyy HH:mm', { locale: ru })}</span>
				</button>
			</PopoverTrigger>
			<PopoverContent
				align="start"
				sideOffset={6}
				onOpenAutoFocus={(e) => e.preventDefault()}
				className="w-auto rounded-2xl border-slate-200 bg-white p-0 shadow-lg motion-reduce:animate-none"
			>
				<Calendar
					mode="single"
					selected={value}
					onSelect={applyDay}
					locale={ru}
					defaultMonth={value}
					captionLayout="dropdown"
					startMonth={new Date(2020, 0)}
					endMonth={new Date(2035, 11)}
					className="w-[285px] rounded-2xl"
				/>
				<div className="border-t border-slate-100 p-3">
					<div className="relative">
						<Clock className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
						<input
							type="text"
							inputMode="numeric"
							placeholder="ЧЧ:ММ"
							value={timeDraft ?? timeValue}
							onChange={(e) => handleTimeInput(e.target.value)}
							onFocus={(e) => {
								setTimeDraft(timeValue)
								e.currentTarget.select()
							}}
							onBlur={commitTime}
							onKeyDown={(e) => {
								if (e.key === 'Enter') e.currentTarget.blur()
							}}
							className="h-10 w-full rounded-xl border border-slate-200 bg-white pl-9 pr-3 text-sm tabular-nums text-slate-800 transition-colors duration-300 focus:border-slate-400 focus:outline-none"
						/>
					</div>
				</div>
			</PopoverContent>
		</Popover>
	)
}
