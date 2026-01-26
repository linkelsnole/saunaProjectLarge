import { useDashboardData } from '@/api/useDashboard';


const RecuperatorRow = ({ label, value, unit = 'об/мин' }: { label: string; value: number | null | undefined; unit?: string }) => (
  <div className="flex justify-between items-center py-3 px-4 rounded-lg border border-slate-200 bg-white hover:bg-slate-50 transition-colors">
    <span className="text-sm text-slate-600 font-medium">{label}</span>
    <span className="font-mono text-lg font-bold text-slate-800">
      {value != null ? value.toFixed(1) : '-'} <span className="text-xs text-slate-400 font-normal ml-1">{unit}</span>
    </span>
  </div>
);

export const RecuperatorWidget = () => {
  const { data } = useDashboardData();

  return (
    <div className="space-y-6 pb-6">        
        <div className=" space-y-3">
          <RecuperatorRow 
            label="Обороты вала рекуператора (целевые)" 
            value={data?.recupTargetRpm} 
          />
          <RecuperatorRow 
            label="Обороты вала" 
            value={data?.recupCurrentRpm} 
          />
          <RecuperatorRow 
            label="Скорость вращения" 
            value={data?.recupSpeedRpm} 
          />
        </div>
    </div>
  );
};
