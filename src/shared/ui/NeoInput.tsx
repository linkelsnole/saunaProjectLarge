import { cn } from "@/lib/utils"

export const NeoInput = ({
  className,
  ...props
}: React.InputHTMLAttributes<HTMLInputElement>) => {
  return (
    <input
      {...props}
      className={cn(
        "no-spinner h-10 w-full rounded-lg px-3",
        "bg-slate-50 text-slate-800 font-semibold tabular-nums shadow-xs",
        "border border-slate-200 outline-none",
        "transition-[border-color,box-shadow,background-color] duration-200",
        "hover:border-slate-300 focus-visible:border-slate-400 focus-visible:bg-white",
        "focus-visible:ring-[3px] focus-visible:ring-slate-200/70",
        className
      )}
    />
  )
}
