import { useEffect, useState } from 'react';
import { Calendar, Loader2, MonitorSmartphone } from 'lucide-react';
import { useTranslation } from 'react-i18next';

interface ModeToggleProps {
  screenMode?: boolean | null;
  disabled?: boolean;
  onModeChange: (screenMode: boolean) => Promise<void>;
}

export function ModeToggle({ screenMode, disabled = false, onModeChange }: ModeToggleProps) {
  const { t } = useTranslation();
  const [selectedMode, setSelectedMode] = useState(screenMode ?? false);
  const [pending, setPending] = useState(false);

  useEffect(() => {
    if (!pending && screenMode != null) {
      setSelectedMode(screenMode);
    }
  }, [pending, screenMode]);

  const handleToggle = async (nextScreenMode: boolean) => {
    if (disabled || pending || selectedMode === nextScreenMode) return;

    const previousMode = selectedMode;
    setSelectedMode(nextScreenMode);
    setPending(true);
    try {
      await onModeChange(nextScreenMode);
    } catch {
      setSelectedMode(previousMode);
    } finally {
      setPending(false);
    }
  };

  const commonClass = 'flex h-9 items-center justify-center gap-1.5 rounded-lg px-2 text-[11px] font-semibold transition-all duration-300 disabled:cursor-not-allowed disabled:opacity-60';

  return (
    <div className="flex items-center">
      <div className="grid w-[236px] grid-cols-2 gap-1 rounded-xl border border-white/60 bg-white/85 p-1 shadow-md backdrop-blur-md">
        <button
          type="button"
          disabled={disabled || pending}
          aria-pressed={!selectedMode}
          aria-label={t('schemaView.schedule')}
          onClick={() => void handleToggle(false)}
          className={`${commonClass} ${!selectedMode
            ? 'bg-gradient-to-br from-blue-400 to-blue-600 text-white shadow-sm shadow-blue-500/25'
            : 'text-slate-500 hover:bg-white'
          }`}
        >
          <Calendar size={14} />
          <span>{t('schemaView.schedule')}</span>
        </button>

        <button
          type="button"
          disabled={disabled || pending}
          aria-pressed={selectedMode}
          aria-label={t('schemaView.screen')}
          onClick={() => void handleToggle(true)}
          className={`${commonClass} ${selectedMode
            ? 'bg-gradient-to-br from-emerald-500 to-green-600 text-white shadow-sm shadow-emerald-500/25'
            : 'text-slate-500 hover:bg-white'
          }`}
        >
          {pending ? <Loader2 size={14} className="animate-spin" /> : <MonitorSmartphone size={14} />}
          <span>{t('schemaView.screen')}</span>
        </button>
      </div>
    </div>
  );
}
