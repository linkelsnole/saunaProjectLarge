import { displayDeviceText } from '@/schema/deviceIdentity';
import { formatControlLog } from './modeLabels';
/** Existing plain-text records remain readable; only explicit before/after pairs get colors. */
export function ActionDetails({ details, success, action = '' }: { details: string | null; success: boolean; action?: string }) {
  if (!details) return null
  details = details.replace(/наддув/gi, word => word[0] === 'н' ? 'дисбаланс' : 'Дисбаланс')
  if (!success) return <div className="mt-1 break-words text-xs text-rose-600">{displayDeviceText(details)}</div>
  return <div className="mt-1 space-y-1 break-words text-xs text-slate-600">
    {displayDeviceText(formatControlLog(details, action)).split('; ').map((line, index) => {
      const pair = /^(.*?): (.+?) → (.+)$/.exec(line)
      if (!pair) return <div key={index}>{line}</div>
      return <div key={index}>
        <span>{pair[1]}: </span>
        <span className="text-slate-500">{pair[2]}</span>
        <span className="text-slate-400"> → </span>
        <span className="font-medium text-emerald-700">{pair[3]}</span>
      </div>
    })}
  </div>
}
