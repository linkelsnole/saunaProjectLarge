import { DeviceHighlights } from './DeviceHighlights';
import { climateDeviceNames } from './deviceLabels';
import { zoneTarget } from "./zoneTarget";
import { isAggregateAlarm } from "./aggregateState";
import { formatValvePercent } from "./valveFormat";
import { useState, useEffect, useRef, type CSSProperties } from 'react';
import { Layer, Line, Stage, Group, Text } from 'react-konva';
import { Html } from 'react-konva-utils';
import { useDashboardData } from '../api/useDashboard';
import { PressureBlock } from './PressureBlock';
import { SquareBlock } from './SquareBlock';
import { TemperatureBlock } from './TemperatureBlock';
import { TemperatureBlockIcon } from './TemperatureBlockIcon';
import { DiagonalBlock } from './DiagonalBlock';
import { TriangleBlock } from './TriangleBlock';
import { TriangleBlockIcon } from './TriangleBlockIcon';
import { CanvasIcon } from './CanvasIcon';
import { DeltaPBlock } from './DeltaPBlock';
import { Pipe } from './Pipe';
import { StatusLine } from './StatusLine';
import { StatusBarBlock } from './StatusBarBlock';
import { ModeToggle } from './ModeToggle';
import { ControlButton } from '@/shared/ui/control-button';
import { setScreenMode, toggleRecuperator } from '@/api/deviceApi';
import { toast } from 'sonner';
import { Loader2 } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';
import { useRecuperatorType } from '@/hooks/useRecuperatorType';
import { useTranslation } from 'react-i18next';
import i18n from '@/shared/config/i18n/i18n';

const SCHEMA_TOP_INSET = 64;

const LoadingOverlay = () => {
	const { t } = useTranslation();

	return (
		<div className="absolute inset-0 z-50 flex items-center justify-center bg-white/50 backdrop-blur-[10px]">
			<div className="flex flex-col items-center gap-3 p-6 bg-white/80 rounded-2xl shadow-lg">
				<Loader2 className="w-10 h-10 text-blue-500 animate-spin" />
				<span className="text-sm text-gray-600">{t('schemaView.loading')}</span>
			</div>
		</div>
	);
};

const FanBottomLabel = ({
	x,
	y,
	text,
	green = false,
}: {
	x: number;
	y: number;
	text: React.ReactNode;
	green?: boolean;
}) => (
	<div
		style={{
			position: 'absolute',
			left: x,
			top: y,
			transform: green ? 'translateX(-50%)' : undefined,
			fontSize: 13,
			lineHeight: 1.2,
			fontFamily: 'ui-monospace, SFMono-Regular, Menlo, sans-serif',
			color: green ? '#22c55e' : '#0F172A',
			whiteSpace: 'nowrap',
			zIndex: 25,
		}}
	>
		{text}
	</div>
);

const TemperatureModeIndicators = ({
	zone1,
	zone2,
}: {
	zone1?: string | null;
	zone2?: string | null;
}) => {
	const { t } = useTranslation();
	const items = [
		{ label: t('schemaView.zone1'), value: zone1 },
		{ label: t('schemaView.zone2'), value: zone2 },
	];

	return (
		<div className="flex w-full justify-between gap-3 pointer-events-none select-none" aria-label={t('schemaView.temperatureModes')}>
			{items.map(item => (
				<div
					key={item.label}
						className="w-[118px] shrink-0 text-xs font-semibold text-slate-600"
					aria-label={`${item.label}: ${item.value ?? '—'}`}
				>
					<div className="mb-1 text-center text-slate-500">{item.label}</div>
						<div className="grid grid-cols-3 rounded-xl border border-white/60 bg-white/85 p-1 shadow-md backdrop-blur-md">
						{['ПВ', 'ОВ', 'ОРП'].map(mode => (
								<span key={mode} className={`rounded-lg px-2 py-1 text-center whitespace-nowrap ${item.value === mode ? 'bg-emerald-500 text-white shadow-sm' : 'text-slate-400'}`}>
								{mode}
							</span>
						))}
					</div>
				</div>
			))}
		</div>
	);
};

const GlassBackground = ({ scale }: { scale: number }) => {
	const style: CSSProperties = {
		top: (220 + SCHEMA_TOP_INSET) * scale,
		left: -10,
		width: 610 * scale,
		height: 400 * scale,
		borderRadius: 30 * scale,
	};

	return (
		<div
			className="absolute backdrop-blur-xl bg-white/60 border border-white/50 shadow-xl z-0 pointer-events-none transition-all duration-100 ease-linear"
			style={style}
		>
			<div className="absolute inset-0 rounded-[inherit] bg-gradient-to-br from-white/40 to-transparent opacity-50" />
		</div>
	);
};

const Schema = ({ diagnostics = false }: { diagnostics?: boolean }) => {
	const { t } = useTranslation();
	const { data, isLoading, refetch } = useDashboardData();
	const { role } = useAuth();
	const { type: recuperatorType } = useRecuperatorType();
	const containerRef = useRef<HTMLDivElement>(null);
	const stageRef = useRef(null);
	const [stageSize, setStageSize] = useState({width: 800, height: 550});
	const [scale, setScale] = useState(1);
	const [loading, setLoading] = useState(false);
	const [optimisticState, setOptimisticState] = useState<{ enabled: boolean; stateCode: number } | null>(null);
	const defaultColor = '#000';
	const canToggleRecuperator = role === 'ROLE_ADMIN' || role === 'ROLE_USER';
	const canChangeScreenMode = role === 'ROLE_ADMIN' || role === 'ROLE_USER';

	const lastNotifiedErrorCode = useRef<number | null>(null);

	useEffect(() => {
        const status = data?.recuperatorStatus;
        if (!status || status.stateCode == null) return;

        const currentCode = status.stateCode;
        const stateName = status.stateName;
        const isAlarm = isAggregateAlarm(currentCode);

        if (isAlarm) {
            if (lastNotifiedErrorCode.current !== currentCode) {
                
                toast.error(i18n.t('schemaView.alarmTitle'), {
                    description: i18n.t('schemaView.alarmDescription', { status: stateName || i18n.t('schemaView.statusFallback'), code: currentCode }),
                    duration: 10000,
                });
                lastNotifiedErrorCode.current = currentCode;
            }
        } else {
            if (lastNotifiedErrorCode.current !== null) {
                toast.success(i18n.t('schemaView.normalTitle'), {
                    description: i18n.t('schemaView.normalDescription'),
                });
                lastNotifiedErrorCode.current = null;
            }
        }
    }, [data?.recuperatorStatus?.stateCode]);

	const handleToggleRecuperator = async () => {
		try {
			setLoading(true);
			const isCurrentlyEnabled = data?.recuperatorStatus?.enabled || false;
			
			if (isCurrentlyEnabled) {
				setOptimisticState({ enabled: false, stateCode: 128 });
			} else {
				setOptimisticState({ enabled: false, stateCode: 16 });
			}
			
			await toggleRecuperator(!isCurrentlyEnabled);
			await refetch();
			
			setOptimisticState(null);
			
			toast.success(isCurrentlyEnabled ? t('schemaView.unitStopped') : t('schemaView.unitStarted'));
		} catch (error) {
			setOptimisticState(null);
			toast.error(error instanceof Error && error.message !== 'Failed to toggle recuperator' ? error.message : t('schemaView.toggleError'));
			console.error(error);
		} finally {
			setLoading(false);
		}
	};

	const handleScreenModeChange = async (screenMode: boolean) => {
		try {
			await setScreenMode(screenMode);
			await refetch();
		} catch (error) {
			toast.error(t('schemaView.modeSwitchError'));
			console.error(error);
			throw error;
		}
	};

	const getClimate = (id: string) => {
		return data?.climateSensors?.find(s => s.id === id);
	};

	const getWSensor = (id: string) => {
		return data?.wSensors?.find(s => s.id === id);
	};

	const getPressureSensor = (id: string) => {
		return data?.pressureSensors?.find(s => s.id === id);
	};

	const getValve = (id: string) => {
		return data?.valves?.find(s => s.id === id);
	};

	const getFan = (id: string) => {
		return data?.fans?.find(f => f.type.includes(id));
	};

	const formatTemperature = (id: string): string => {
		if (diagnostics) return climateDeviceNames[id] ?? id;
		const sensor = getClimate(id);
		const temp = sensor?.temperature;
		return temp != null ? `${temp.toFixed(1)} °C` : '-- °C';
	};

	const formatHumidity = (id: string): string => {
		if (diagnostics) return '';
		const sensor = getClimate(id);
		const humid = sensor?.humidity;
		return humid != null ? `${humid.toFixed(1)} %` : '-- %';
	};

	const formatPressure = (id: string): string => {
		if (diagnostics) return id === 'BP1' ? 'PF1' : id === 'BP2' ? 'PF2' : id;
		const sensor = getWSensor(id);
		const value = sensor?.value;
		return value != null ? `${Math.round(parseFloat(value))} ${t('units.pa')}` : `-- ${t('units.pa')}`;
	};

	const formatPressureSensor = (id: string): string => {
		if (diagnostics) return id;
		const sensor = getPressureSensor(id);
		const value = sensor?.value;
		if (value != null) {
			const intValue = Math.round(parseFloat(value));
			return `${intValue} ${t('units.pa')}`;
		}
		return `-- ${t('units.pa')}`;
	};

	const formatValveLabel = (valveId: string, tempId: string): string => {
		if (diagnostics) return climateDeviceNames[tempId] ?? tempId;
		const valve = getValve(valveId);
		const temp = getClimate(tempId);
		
		const valveValue = valve?.value;
		const tempValue = temp?.temperature;
		// const humidValue = temp?.humidity;
		
		const valveStr = formatValvePercent(valveValue);
		const tempStr = tempValue != null ? `${tempValue.toFixed(1)} °C` : '-- °C';
		// const humidStr = humidValue != null ? `${humidValue.toFixed(1)} %` : '-- %';
		
		return `${valveStr}\n${tempStr}`;
	};

	const formatFanLabel = (id: string): string => {
		if (diagnostics) return ({ 'Вентилятор 1': 'M1_1', 'Вентилятор 2': 'M1_2', 'Вентилятор 3': 'M2_1', 'Вентилятор 4': 'M2_2' } as Record<string, string>)[id] ?? id;
		const fan = getFan(id);
		const pressure = fan?.pressure;
		const speed = fan?.speedPerc;
		const vm = fan?.vm;
		const pressureStr = pressure != null ? `${Math.round(pressure)} ${t('units.pa')}` : `-- ${t('units.pa')}`;
		const speedStr = speed != null ? `${speed} %` : '-- %';
		const vmStr = vm != null ? `${vm} ${t('units.m3h')}` : `-- ${t('units.m3h')}`;
		return `${pressureStr}\n${speedStr}\n${vmStr}`;
	};

	const formatFanBottomLabel = (type: 'vm1' | 'vm2' | 'formula' | 'vmanual'): string => {
		if (diagnostics) return '';
		if (type === 'vm1') {
			const v = data?.vm1;
			return v != null ? `${v} ${t('units.m3h')}` : `-- ${t('units.m3h')}`;
		}
		if (type === 'vm2') {
			const v = data?.vm2;
			return v != null ? `${v} ${t('units.m3h')}` : `-- ${t('units.m3h')}`;
		}
		if (type === 'vmanual') {
			const v = getFan('Вентилятор 1')?.targetAirFlow;
			return v != null ? `${Math.round(v)} ${t('units.m3h')}` : `-- ${t('units.m3h')}`;
		}
		if (type === 'formula') {
			const v = getFan('Вентилятор 3')?.targetAirFlow;
			return v != null ? `${Math.round(v)} ${t('units.m3h')}` : `-- ${t('units.m3h')}`;
		}
		return '--';
	};

	const formatZoneTemperature = (id: string): string => {
		if (diagnostics) return '';
		const temp = zoneTarget(data, id);
		return temp != null ? `${temp.toFixed(1)} °C` : '-- °C';
	};


	const getRecuperator = (id: string) => {
		return data?.recuperator?.find(s => s.id === id);
	};

	const formatRecupTempBt6 = () => {
		const t = data?.recupTempBt6;
		const val = t != null ? t.toFixed(1) : '--';
		return <>{val}<span style={{ marginLeft: 2 }}>°C</span></>;
	};

	const formatRecupFormula = () => {
		const pct = data?.recupTargetPercent;
		const val = pct != null ? String(pct) : '--';
		return <>{val}<span style={{ marginLeft: 3 }}>%</span></>;
	};

	const formatRecuperator = (id: string): string => {
		if (diagnostics) return id === 'R_TARGET' ? 'M3' : '';
		if (id === 'R_SPEED') {
			const speed = data?.recupSpeed;
			return speed != null ? `${speed} ${t('units.rpm')}` : `-- ${t('units.rpm')}`;
		}
		if (id === 'R_CURRENT') {
			const current = data?.recupTargetRpm; // было recupCurrent(0x2C0), заменил пока что 22.01.26
			return current != null ? `${Number(current).toFixed(1)} ${t('units.rpm')}` : `-- ${t('units.rpm')}`;
		}
		const sensor = getRecuperator(id);
		const value = sensor?.value;
		const unit = sensor?.unit || '';
		let label = '';
		if (id === 'R_EFFICIENCY') {
			label = '';
			const numValue = value != null ? parseFloat(value) : null;
			const intValue = numValue != null ? Math.round(numValue) : null;
			return intValue != null ? `${intValue} ${unit}${label}` : `-- ${unit}${label}`;
		}
		return value != null ? `${value} ${unit}${label}` : `-- ${unit}${label}`;
	};


	useEffect(() => {
		const container = containerRef.current;
		if (!container) return;
		
		const baseWidth = 600;
		const baseHeight = 740 + SCHEMA_TOP_INSET;
		
		const updateDimensions = () => {
			if (!container) return;
			const containerWidth = container.clientWidth;
			const containerHeight = container.clientHeight || window.innerHeight;
			const scaleX = containerWidth / baseWidth;
			const scaleY = containerHeight / baseHeight;
			const newScale = Math.min(scaleX, scaleY, 2); 
			const newWidth = baseWidth * newScale;
			const newHeight = baseHeight * newScale;
			setStageSize({width: newWidth, height: newHeight});
			setScale(newScale);
		};

		updateDimensions();
		
		const timer = setTimeout(updateDimensions, 100);
		window.addEventListener('resize', updateDimensions);
		const resizeObserver = new ResizeObserver(updateDimensions);
		resizeObserver.observe(container);
		
		return () => {
			window.removeEventListener('resize', updateDimensions);
			if (container) {
				resizeObserver.unobserve(container);
			}
			resizeObserver.disconnect();
			clearTimeout(timer);
		};
	}, []);

	// Показываем overlay только при первой загрузке (когда data === undefined)
	const showLoadingOverlay = isLoading && !data;

  return (
    <div
		className='relative w-full h-full flex flex-col'
		ref={containerRef}
		style={{ backgroundColor: 'transparent' }}
	>
		{/* Loading overlay */}
		{showLoadingOverlay && <LoadingOverlay />}
      <div className='flex-1 flex items-center justify-center'>
	  <div className="absolute inset-0 z-0">
					<div 
						className="absolute inset-0 w-full h-full"
						style={{
							background: `
								radial-gradient(circle at 20% 20%, rgba(168, 85, 247, 0.12) 0%, transparent 50%),
								radial-gradient(circle at 80% 80%, rgba(59, 130, 246, 0.15) 0%, transparent 50%),
								radial-gradient(circle at 40% 60%, rgba(14, 165, 233, 0.1) 0%, transparent 50%),
								linear-gradient(135deg, #e0f2fe 0%, #dbeafe 50%, #e0e7ff 100%)
							`
						}}
					/>
        </div>
		<div
			style={{
				position: 'relative',
				width: stageSize.width,
				height: stageSize.height,
			}}
		>
			<GlassBackground scale={scale} />
				<Stage
			  width={stageSize.width} 
			  height={stageSize.height} 
			  ref={stageRef}
			  scaleX={scale}
			  scaleY={scale}
			  style={{
				position: 'absolute',
				top: 0,
				left: 0,
				zIndex: 20
			  }}
				>
				  <Layer scale={{x: 1, y: 1}}>
			  <Html>
				  <div style={{ position: 'absolute', left: -8, top: 8 }}>
					  <ModeToggle
						  screenMode={data?.screenMode}
						  disabled={!canChangeScreenMode || !data}
						  onModeChange={handleScreenModeChange}
					  />
				  </div>
			  </Html>
			  <Html>
				  <div style={{ position: 'absolute', left: 238, top: 8, width: 348 }}>
					  <TemperatureModeIndicators
						  zone1={data?.temperatureControlModeZone1Name}
						  zone2={data?.temperatureControlModeZone2Name}
					  />
				  </div>
			  </Html>
			  <Group y={SCHEMA_TOP_INSET}>
			  <Html>
					<StatusBarBlock
					x={165}
					y={400}
					width={500}
					height={300}
				/>
		</Html>
		  <Html>
			  <div style={{ position: 'absolute', left: 218, top: 647 }}>
				  <ControlButton
					  isActive={optimisticState?.enabled ?? data?.recuperatorStatus?.enabled ?? false}
					  onClick={handleToggleRecuperator}
					  loading={loading}
					  stateCode={optimisticState?.stateCode ?? data?.recuperatorStatus?.stateCode}
					  isDataLoading={!data}
					  readOnly={!canToggleRecuperator}
				  />
			  </div>
		  </Html>
		  <Html>
			  <StatusLine
				  x={165}
				  y={600}
					  text={diagnostics ? 'S1' : [data?.recuperatorStatus?.stateName, data?.ventilationModeName].filter(Boolean).join(' · ') || '—'}
			  />
		  </Html>
		  <Pipe points={[160, 500, 160, 50]} type="hot" />
		  <Pipe points={[160, 590, 160, 695]} type="hot" />
		  <Pipe points={[395, 695, 395, 340]} type="cold" />
		  <Pipe points={[395, 250, 395, 230]} type="cold" />
		  <Pipe points={[310, 230, 400, 230]} type="cold" />
		  <Pipe points={[350, 230, 480, 230]} type="cold" />
		  <Pipe points={[310, 50, 310, 230]} type="cold" />
		  <Pipe points={[480, 50, 480, 230]} type="cold" />
		  



		  {/* Линии до ΔP2*/}
		  <Line points={[90, 391, 158, 391]} stroke={defaultColor} strokeWidth={2.5} />
		  <Line points={[90, 480, 158, 480]} stroke={defaultColor} strokeWidth={2.5} />
		  <Line points={[91, 391, 91, 481]} stroke={defaultColor} strokeWidth={2.5} />

		   {/*BPCalib BP5 и BP10*/}
		  <DeltaPBlock
			x={90}
			y={435}
			text="ΔP"
			label={diagnostics ? 'PR1\nPR2' : `${formatPressureSensor('BP5')}`}
			labelWidth={70}
			labelOffsetX={-15}
			labelOffsetY={-5}
		  />
		<Html>
			{/* BP2 */}
			<PressureBlock 
					width={45}
					height={45}
					text={formatPressure('BP2')}
					x={70}
					y={270}
				/>
				{/* BP1 */}
				<PressureBlock 
					width={45}
					height={45}
					text={formatPressure('BP1')}
					x={295}
					y={510}
					rotation={180}
					labelWidth = {70}
					labelOffsetX = {120}
				/>
			</Html>

			{/* Группа слева + и -*/}
			<Group x={285} y={150}>
				<Html>
					{/*REG_F_VH1 + BT8*/}
					<SquareBlock 
						size={50}
						sign="+"
						x={0}
						y={0}
						label={formatValveLabel('VH1', 'BT8')}
					/>
				</Html>
				<Html>
					{/*REG_F_VC1 + BT9*/}
					<SquareBlock 
						size={50}
						sign="-"
						x={0}
						y={-70}
						label={formatValveLabel('VC1', 'BT9')}
					/>
				</Html>
			</Group>

			<Group x={455} y={150}>
				<Html>
					{/*REG_F_VH2 + BT24*/}
					<SquareBlock 
						size={50}
						sign="+"
						x={0}
						y={0}
						label={formatValveLabel('VH2', 'BT24')}
					/>
				</Html>
				<Html>
					{/*REG_F_VC2 + BT27*/}
					<SquareBlock 
						size={50}
						sign="-"
						x={0}
						y={-70}
						label={formatValveLabel('VC2', 'BT27')}
					/>
				</Html>
			</Group>

			<Html>
				<TemperatureBlock 
					width={100}
					height={25}
					text={formatTemperature('BT2')}
					subText={formatHumidity('BT2')}
					x={2}
					y={350}
					labelWidth={40}
					labelOffsetX={35}
					labelOffsetY={-3}
				/>
			</Html>
			<Html>
				<TemperatureBlock
					width={100}
					height={25}
					text={formatTemperature('BT3')}
					subText={formatHumidity('BT3')}
					x={291}
					y={590}
					rotation={-180}
					labelWidth={70}
					labelOffsetX={160}
					labelOffsetY={0}
				/>
			</Html>
			<Html>
				<TemperatureBlock
					width={100}
					height={25}
					text={formatTemperature('BT10')}
					subText={formatHumidity('BT10')}
					x={291}
					y={350}
					rotation={-180}
					labelWidth={70}
					labelOffsetX={160}
					labelOffsetY={10}
				/>
			</Html>
			{/* Слева под вентилятором */}
			<Html>
				<TemperatureBlock 
					width={100}
					height={25}
					text={formatTemperature('BT11')}
					subText={formatHumidity('BT11')}
					x={3}
					y={600}
					labelWidth={40}
					labelOffsetX={35}
					labelOffsetY={10}
				/>
			</Html>
			{/* Справа над - */}
			<Html>
				<TemperatureBlock
					width={100}
					height={25}
					text={formatTemperature('BT21')}
					subText={formatHumidity('BT21')}
					x={376}
					y={42}
					rotation={-180}
					labelWidth={70}
					labelOffsetX={160}
					labelOffsetY={0}
				/>
			</Html>
			{/* Слева над - */}
			<Html>
				<TemperatureBlock
					width={100}
					height={25}
					text={formatTemperature('BT1')}
					subText={formatHumidity('BT1')}
					x={152}
					y={42}
					labelWidth={70}
					labelOffsetX={35}
					labelOffsetY={0}
				/>
			</Html>
			
			{/*MG2 MG1 - задвижки притока и вытяжки*/}
			<Group y={640}>
                {diagnostics && <><Text x={425} y={14} text="D1" fontSize={13} fill="#334155" /><Text x={112} y={14} text="D2" fontSize={13} fill="#334155" /><Text x={196} y={20} text="B1" fontSize={13} fill="#334155" /></>}
				<Html>
					<DiagonalBlock 
						size={45}
						x={373}
						isOpen={data?.damperSupply}
					/>
				</Html>
				<Html>
					<DiagonalBlock 
						size={45}
						x={136}
						isOpen={data?.damperExhaust}
					/>
				</Html>
			</Group>


			<Pipe points={[100, 590, 220, 590]} type="hot" />
			<Pipe points={[100, 590, 100, 500]} type="hot" />
			<Pipe points={[220, 590, 220, 500]} type="hot" />
			<Pipe points={[100, 500, 220, 500]} type="hot" />

			<Pipe points={[335, 250, 455, 250]} type="cold" />
			<Pipe points={[335, 250, 335, 340]} type="cold" />
			<Pipe points={[335, 340, 455, 340]} type="cold" />
			<Pipe points={[455, 340, 455, 250]} type="cold" />
			
			{/*BF12 и BF11 - объединенный компонент 2 вентиля сверху*/}
			<Html>
				<TriangleBlock 
					size={45}
					x={335}
					y={292}
					rotation={-90}
					leftLabel={formatFanLabel('Вентилятор 1')}
					labelWidth={150}
					labelGap={5}
				/>
				<TriangleBlock 
					size={45}
					x={455}
					y={292}
					rotation={-90}
					rightLabel={formatFanLabel('Вентилятор 2')}
					labelWidth={150}
					labelGap={5}
				/>
				<FanBottomLabel x={360} y={315} text={formatFanBottomLabel('vm1')} />
				<FanBottomLabel x={395} y={290} text={formatFanBottomLabel('vmanual')} green />
			</Html>

			{/*BF21 и BF22 - объединенный компонент*/}
			<Html>
				<TriangleBlock 
					size={45}
					x={100}
					y={543}
					rotation={90}
					leftLabel={formatFanLabel('Вентилятор 3')}
					labelWidth={150}
					labelGap={5}
				/>

				<TriangleBlock 
					size={45}
					x={220}
					y={543}
					rotation={90}
					rightLabel={formatFanLabel('Вентилятор 4')}
					labelWidth={150}
					labelGap={5}
				/>
				<FanBottomLabel x={125} y={568} text={formatFanBottomLabel('vm2')} />
				<FanBottomLabel x={160} y={543} text={formatFanBottomLabel('formula')} green />
			</Html>


			<Group x={120} y={410}>
                {diagnostics && recuperatorType === 1 && <Text x={100} y={59} text="Насос" fontSize={11} fill="#334155" />}
				{recuperatorType === 1 ? (
					<>
						<Html>
							<SquareBlock size={50} sign="+" x={250} y={0} />
						</Html>
						<Html>
							<SquareBlock size={50} sign="-" x={10} y={0} />
						</Html>
						<Line points={[50, 15, 270, 15]} stroke={defaultColor} strokeWidth={2} />
						<Line points={[50, 37.7, 270, 37.7]} stroke={defaultColor} strokeWidth={2} />
						<Line points={[190, 37.7, 190, 20]} stroke={defaultColor} strokeWidth={2} />
						<Text
							text={formatRecuperator('R_EFFICIENCY')}
							x={225}
							y={42}
							fontSize={11}
							fill="#000"
						/>
						<Html>
							<TriangleBlockIcon
								size={20}
								x={120}
								y={37}
								rotation={180}
								isActive={data?.pumpActive ?? null}
                                label={t(data?.pumpActive == null ? 'schemaView.pumpUnknown' : data.pumpActive ? 'schemaView.pumpOn' : 'schemaView.pumpOff')}
							/>
						</Html>
						<Html>
							<FanBottomLabel x={90} y={-32} text={diagnostics ? 'TTR' : formatRecupTempBt6()} />
							<TemperatureBlockIcon width={40} height={24} x={120} y={0} rotation={90} />
						</Html>
						<Html>
							<FanBottomLabel x={173} y={-15} text={diagnostics ? 'Вентиль' : formatRecupFormula()} />
							<CanvasIcon src="/iconc.png" x={190} y={18} size={50} />
						</Html>
					</>
				) : (
					<>
						<Html>
							<SquareBlock size={50} sign="+" x={250} y={0} />
						</Html>
						<Html>
							<SquareBlock size={50} sign="-" x={10} y={0} />
						</Html>
						<Line points={[50, 5, 270, 5]} stroke={defaultColor} strokeWidth={2} />
						<Line points={[50, 47.7, 270, 47.7]} stroke={defaultColor} strokeWidth={2} />
						<Text text={formatRecuperator('R_TARGET')} x={70} y={20} fontSize={10} fill="#000" />
						<Text text={formatRecuperator('R_EFFICIENCY')} x={70} y={33} fontSize={10} fill="#000" />
					</>
				)}
			</Group>

			<Group y={210} x={320}>
				<Text
					text={`${formatZoneTemperature('T_ZONE1')}`}
					x={0}
					y={0}
					fontSize={13}
					fill="green"
					fontFamily="Inter, Arial, sans-serif"
				/>
				<Text
					text={`${formatZoneTemperature('T_ZONE2')}`}
					x={110}
					y={0}
					fontSize={13}
					fill="green"
					fontFamily="Inter, Arial, sans-serif"
				/>
			</Group>
		    {/*РЕКУПЕРАТОР*/}
			<Group x={195} y={385}>
				{/* <Text
					text={formatRecuperator('R_SPEED')}
					x={0}
					y={0}
					fontSize={10}
					fill="#000"
				/> */}

			</Group>



			

			  </Group>
	          {diagnostics && <Group y={SCHEMA_TOP_INSET}><DeviceHighlights /></Group>}
                  </Layer>
        </Stage>
		</div>
      </div>
    </div>
  );
};

export default Schema;
