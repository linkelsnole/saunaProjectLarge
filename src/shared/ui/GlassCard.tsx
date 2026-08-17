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
        "relative overflow-hidden rounded-2xl",
        "bg-white shadow-sm",
        "border border-slate-200",
        className
      )}
    >
      <div className="relative h-full">{children}</div>
    </div>
  )
}
