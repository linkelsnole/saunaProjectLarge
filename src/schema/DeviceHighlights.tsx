import { Html } from 'react-konva-utils';
import { useDashboardData } from '@/api/useDashboard';
import { deviceName, useSelectedDevice } from './deviceIdentity';
import { deviceStates } from './deviceLabels';
// Coordinates are in the same unscaled equipment group as Schema.
const boxes: Record<string, number[]> = {
 T1: [447,580,59,40], T2: [32,601,59,40], T3: [447,350,59,40], T5: [30,341,59,40],
 T4_1: [181,32,59,40], T4_2: [532,32,59,40],
 TH1: [279,144,62,62], TC1: [279,74,62,62], TH2: [449,144,62,62], TC2: [449,74,62,62],
 M1_1: [307,264,56,56], M1_2: [427,264,56,56], M2_1: [72,514,56,56], M2_2: [192,514,56,56],
 PF1: [367,503,58,58], PF2: [132,263,58,58], PR1: [65,410,50,50], PR2: [65,410,50,50],
 M3: [125,404,300,62], D1: [367,634,57,57], D2: [130,634,57,57], TTR: [222,370,55,45],
};
export function DeviceHighlights() {
 const { data, error } = useDashboardData(); const selected = useSelectedDevice();
 const visible = new Map<string, number>();
 if (!error) for (const d of data?.deviceStates ?? []) if (d.alarmMode != null && d.alarmMode > 0 && deviceStates[d.alarmMode]) visible.set(deviceName(d.id), d.alarmMode);
 if (selected && !visible.has(selected)) visible.set(selected, 0);
 return <Html divProps={{style:{pointerEvents:'none'}}}><div style={{pointerEvents:'none'}}>
 {Array.from(visible, ([id, mode]) => {
  const box = boxes[id]; if (!box) return null;
  // PR1 and PR2 share one gauge; keep its most severe active state visible.
  const sharedMode = id === 'PR1' || id === 'PR2' ? Math.max(mode, visible.get('PR1') ?? 0, visible.get('PR2') ?? 0) : mode;
  const color = sharedMode ? deviceStates[sharedMode].ink : '#2563eb'; const chosen = selected === id || ((id === 'PR1' || id === 'PR2') && (selected === 'PR1' || selected === 'PR2'));
  return <div key={id} data-device-highlight={id} aria-label={`${id} · ${mode ? deviceStates[mode].label : 'Выбрано'}`} style={{position:'absolute',left:box[0],top:box[1],width:box[2],height:box[3],border:`${chosen ? 3 : 2}px solid ${color}`,borderRadius:14,boxShadow:chosen ? `0 0 0 4px ${color}30` : `0 0 10px ${color}45`,pointerEvents:'none',zIndex:40}} />;
 })}</div></Html>;
}
