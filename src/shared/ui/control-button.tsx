import { Button } from "@/shared/ui/button"
import { Power, Loader2, AlertTriangle } from "lucide-react"
import { useTranslation } from "react-i18next"
import { cn } from "@/lib/utils"

interface ControlButtonProps {
  isActive?: boolean
  onClick?: () => void
  loading?: boolean
  disabled?: boolean
  readOnly?: boolean
  className?: string
  stateCode?: number | null
  isDataLoading?: boolean
}

export function ControlButton({
  isActive = false,
  onClick,
  loading = false,
  disabled = false,
  readOnly = false,
  className,
  stateCode,
  isDataLoading = false,
}: ControlButtonProps) {
  const { t } = useTranslation()
  let buttonText = t("controlButton.off")
  let buttonColor = "bg-red-500 hover:bg-red-600"
  let showSpinner = false
  let isTransitioning = false
  let Icon = Power


  const isAlarm = stateCode != null && (stateCode & 6) !==0

  if (isDataLoading) {
    buttonText = t("controlButton.loading")
    buttonColor = "bg-gray-400"
    showSpinner = true
  } else if (isAlarm) {
    buttonText = t("controlButton.alarm")
    buttonColor = "bg-red-600 hover:bg-red-700" 
    Icon = AlertTriangle
    isTransitioning = true
  } else if (stateCode === 16) {
    // Начальный запуск
    buttonText = t("controlButton.starting")
    buttonColor = "bg-green-600 hover:bg-green-700"
    showSpinner = true
    isTransitioning = true
  } else if (stateCode === 128) {
    // Останов агрегата
    buttonText = t("controlButton.stopping")
    buttonColor = "bg-red-500 hover:bg-red-600"
    showSpinner = true
    isTransitioning = true
  } else if (isActive) {
    // Включено (работает)
    buttonText = t("controlButton.on")
    buttonColor = "bg-green-600 hover:bg-green-700"
  } else {
    // Выключено
    buttonText = t("controlButton.off")
    buttonColor = "bg-red-500 hover:bg-red-600"
  }

  return (
    <Button
      onClick={readOnly ? undefined : onClick}
      disabled={disabled || loading || isDataLoading || isTransitioning}
      aria-disabled={readOnly ? true : undefined}
      tabIndex={readOnly ? -1 : 0}
      className={cn(
        "relative h-8 text-xs transition-all duration-200 text-white shadow-lg", 
        "min-w-[120px] justify-center",
        buttonColor,
        (loading || isDataLoading || isTransitioning) && "opacity-70 cursor-wait",
        readOnly && "pointer-events-none",
        className
      )}
    >
      {showSpinner || loading ? (
        <Loader2 className="w-4 h-4 animate-spin" />
      ) : (
        <Icon className="w-4 h-4" />
      )}
      {buttonText}
    </Button>
  )
}

