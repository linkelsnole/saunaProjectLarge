import { cn } from "@/lib/utils"

export const NeoInput = ({
  className,
  ...props
}: React.InputHTMLAttributes<HTMLInputElement>) => {
  return (
    <input
      {...props}
      className={cn(
        "w-full h-12 px-4 rounded-xl",
        "bg-slate-100/50 text-slate-700 font-mono font-bold",
        "outline-none transition-all",
        "shadow-[inset_2px_2px_5px_rgba(0,0,0,0.05),inset_-2px_-2px_5px_rgba(255,255,255,0.8)]",
        "border border-transparent",
        "focus:border-blue-400/50 focus:shadow-[inset_2px_2px_5px_rgba(0,0,0,0.02)]",
        "focus:bg-slate-50/80",
        className
      )}
    />
  )
}
