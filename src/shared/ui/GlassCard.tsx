import { cn } from "@/lib/utils"

export const GlassCard = ({
  children,
  className,
}: {
  children: React.ReactNode
  className?: string
}) => {
  return (
    <div
      className={cn(
        "relative overflow-hidden rounded-3xl",
        "bg-white/60 backdrop-blur-xl",
        "border border-slate-200",
        className
      )}
    >
      <div className="absolute top-0 left-0 right-0 h-1/3 bg-gradient-to-b from-white/40 to-transparent pointer-events-none" />
      <div className="relative z-10 h-full">{children}</div>
    </div>
  )
}
