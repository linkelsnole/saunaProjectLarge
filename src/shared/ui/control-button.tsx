import { Button } from "@/shared/ui/button"
import { Power, Loader2, AlertTriangle } from "lucide-react"
import { cn } from "@/lib/utils"

interface ControlButtonProps {
  isActive?: boolean
  onClick?: () => void
  loading?: boolean
  disabled?: boolean
  className?: string
  stateCode?: number | null
  isDataLoading?: boolean
}

export function ControlButton({
  isActive = false,
  onClick,
  loading = false,
  disabled = false,
  className,
  stateCode,
  isDataLoading = false,
}: ControlButtonProps) {
  let buttonText = "Выключено"
  let buttonColor = "bg-red-500 hover:bg-red-600"
  let showSpinner = false
  let isTransitioning = false
  let Icon = Power


  const isAlarm = stateCode != null && (stateCode & 6) !==0

  if (isDataLoading) {
    buttonText = "Загрузка"
    buttonColor = "bg-gray-400"
    showSpinner = true
  } else if (isAlarm) {
    buttonText = "Авария"
    buttonColor = "bg-red-600 hover:bg-red-700" 
    Icon = AlertTriangle
    isTransitioning = true
  } else if (stateCode === 16) {
    // Начальный запуск
    buttonText = "Запуск"
    buttonColor = "bg-green-600 hover:bg-green-700"
    showSpinner = true
    isTransitioning = true
  } else if (stateCode === 128) {
    // Останов агрегата
    buttonText = "Останов"
    buttonColor = "bg-red-500 hover:bg-red-600"
    showSpinner = true
    isTransitioning = true
  } else if (isActive) {
    // Включено (работает)
    buttonText = "Включено"
    buttonColor = "bg-green-600 hover:bg-green-700"
  } else {
    // Выключено
    buttonText = "Выключено"
    buttonColor = "bg-red-500 hover:bg-red-600"
  }

  return (
    <Button
      onClick={onClick}
      disabled={disabled || loading || isDataLoading || isTransitioning}
      className={cn(
        "relative h-8 text-xs transition-all duration-200 text-white shadow-lg", 
        "min-w-[120px] justify-center",
        buttonColor,
        (loading || isDataLoading || isTransitioning) && "opacity-70 cursor-wait",
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

