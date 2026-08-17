import { cn } from "@/lib/utils"

export const NeoInput = ({
  className,
  ...props
}: React.InputHTMLAttributes<HTMLInputElement>) => {
  return (
    <input
      {...props}
      className={cn(
        "w-full h-12 px-4 rounded-xl no-spinner",
        "bg-slate-50 text-slate-800 font-semibold tabular-nums",
        "outline-none transition-colors",
        "border border-slate-200",
        "focus:border-slate-300 focus:bg-white",
        className
      )}
    />
  )
}
