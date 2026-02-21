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
import { ControlButton } from '@/shared/ui/control-button';
import { toggleRecuperator } from '@/api/deviceApi';
import { toast } from 'sonner';
import { Loader2 } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';

const LoadingOverlay = () => (
	<div className="absolute inset-0 z-50 flex items-center justify-center bg-white/50 backdrop-blur-[10px]">
		<div className="flex flex-col items-center gap-3 p-6 bg-white/80 rounded-2xl shadow-lg">
			<Loader2 className="w-10 h-10 text-blue-500 animate-spin" />
			<span className="text-sm text-gray-600">Получение данных...</span>
		</div>
	</div>
);

const GlassBackground = ({ scale }: { scale: number }) => {
	const style: CSSProperties = {
		top: 220 * scale,
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

const Schema = () => {
	const { data, isLoading, refetch } = useDashboardData();
	const { role } = useAuth();
	const containerRef = useRef<HTMLDivElement>(null);
	const stageRef = useRef(null);
	const [stageSize, setStageSize] = useState({width: 800, height: 550});
	const [scale, setScale] = useState(1);
	const [loading, setLoading] = useState(false);
	const [optimisticState, setOptimisticState] = useState<{ enabled: boolean; stateCode: number } | null>(null);
	const defaultColor = '#000';
	const canToggleRecuperator = role === 'ROLE_ADMIN' || role === 'ROLE_USER';

	const lastNotifiedErrorCode = useRef<number | null>(null);

	useEffect(() => {
        const status = data?.recuperatorStatus;
        if (!status || status.stateCode == null) return;

        const currentCode = status.stateCode;
        const stateName = status.stateName;
        const isAlarm = (currentCode & 6) !== 0;

        if (isAlarm) {
            if (lastNotifiedErrorCode.current !== currentCode) {
                
                toast.error("Внимание! Авария агрегата", {
                    description: `Статус: ${stateName || 'Ошибка'} (Код ${currentCode})`,
                    duration: 10000, 
                });
                lastNotifiedErrorCode.current = currentCode;
            }
        } else {
            if (lastNotifiedErrorCode.current !== null) {
                toast.success("Система в норме", {
                    description: "Аварийные статусы сняты",
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
			
			toast.success(isCurrentlyEnabled ? 'Останов агрегата' : 'Запуск агрегата');
		} catch (error) {
			setOptimisticState(null);
			toast.error('Ошибка при переключении рекуператора');
			console.error(error);
		} finally {
			setLoading(false);
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
		const sensor = getClimate(id);
		const temp = sensor?.temperature;
		return temp != null ? `${temp.toFixed(1)} °C` : '-- °C';
	};

	const formatHumidity = (id: string): string => {
		const sensor = getClimate(id);
		const humid = sensor?.humidity;
		return humid != null ? `${humid.toFixed(1)} %` : '-- %';
	};

	const formatPressure = (id: string): string => {
		const sensor = getWSensor(id);
		const value = sensor?.value;
		return value != null ? `${value} Па` : '-- Па';
	};

	const formatPressureSensor = (id: string): string => {
		const sensor = getPressureSensor(id);
		const value = sensor?.value;
		if (value != null) {
			const intValue = Math.round(parseFloat(value));
			return `${intValue} Па`;
		}
		return '-- Па';
	};

	const formatValveLabel = (valveId: string, tempId: string): string => {
		const valve = getValve(valveId);
		const temp = getClimate(tempId);
		
		const valveValue = valve?.value;
		const tempValue = temp?.temperature;
		const humidValue = temp?.humidity;
		
		const valveInt = valveValue != null ? Math.round(parseFloat(valveValue)) : null;
		const valveStr = valveInt != null ? `${valveInt} %` : '-- %';
		const tempStr = tempValue != null ? `${tempValue.toFixed(1)} °C` : '-- °C';
		const humidStr = humidValue != null ? `${humidValue.toFixed(1)} %` : '-- %';
		
		return `${valveStr}\n${tempStr}\n${humidStr}`;
	};

	const formatFanLabel = (id: string, bottomValue: 'vm1' | 'vm2' | 'formula' | 'vmanual' = 'vm2'): string => {
		const fan = getFan(id);
		const pressure = fan?.pressure;
		const speed = fan?.speedPerc;
		const vm = fan?.vm;
		
		const pressureStr = pressure != null ? `${Math.round(pressure)} Па` : '-- Па';
		const speedStr = speed != null ? `${speed} %` : '-- %';
		const vmStr = vm != null ? `${vm} м³/ч` : '-- м³/ч';
		
		let bottomStr = '';
		if (bottomValue === 'vm1') {
			const vm1Value = data?.vm1;
			bottomStr = vm1Value != null ? `${vm1Value} м³/ч` : '-- м³/ч';
		} else if (bottomValue === 'vm2') {
			const vm2Value = data?.vm2;
			bottomStr = vm2Value != null ? `${vm2Value} м³/ч` : '-- м³/ч';
		} else if (bottomValue === 'vmanual') {
			const vManualProcIn = data?.vManualProcIn;
			const targetStr = vManualProcIn != null ? `${vManualProcIn.toFixed(0)} %` : '-- %';
			bottomStr = `<span style="color: #22c55e;">${targetStr}</span>`;
		} else if (bottomValue === 'formula') {
			const vManualProcIn = data?.vManualProcIn;
			const vManualDProcOut = data?.vManualDProcOut;
			let targetValue = null;
			if (vManualProcIn != null && vManualDProcOut != null) {
				targetValue = Math.max(0,vManualProcIn - vManualDProcOut);
			}
			const targetStr = targetValue != null ? `${targetValue.toFixed(0)} %` : '-- %';
			bottomStr = `<span style="color: #22c55e;">${targetStr}</span>`;
		}
		
		return `${pressureStr}\n${speedStr}\n${vmStr}\n<span style="display: block; margin-top: 8px;">${bottomStr}</span>`;
	};

	const formatZoneTemperature = (id: string): string => {
		const sensor = getClimate(id);
		const temp = sensor?.temperature;
		return temp != null ? `${temp.toFixed(1)} °C` : '-- °C';
	};


	const getRecuperator = (id: string) => {
		return data?.recuperator?.find(s => s.id === id);
	};

	const formatRecuperator = (id: string): string => {
		if (id === 'R_SPEED') {
			const speed = data?.recupSpeed;
			return speed != null ? `${speed} об/мин` : '-- об/мин';
		}
		if (id === 'R_CURRENT') {
			const current = data?.recupTargetRpm; // было recupCurrent(0x2C0), заменил пока что 22.01.26
			return current != null ? `${Number(current).toFixed(1)} об/мин` : '-- об/мин';
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
		const baseHeight = 740;
		
		const updateDimensions = () => {
			if (!container) return;
			const containerWidth = container.clientWidth;
			const containerHeight = window.innerHeight ; 
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
				top: -30,
				left: 0,
				zIndex: 20
			  }}
			>
			  <Layer scale={{x: 1, y: 1}}>
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
				  text={`${data?.recuperatorStatus?.stateName ?? '—'}`}
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
			label={`${formatPressureSensor('BP5')}\n${formatPressureSensor('BP10')}`}
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
					x={330}
					y={292}
					rotation={-90}
					leftLabel={formatFanLabel('Вентилятор 1', 'vm1')}
					labelWidth={150}
					labelGap={5}
				/>
				<TriangleBlock 
					size={45}
					x={455}
					y={292}
					rotation={-90}
					rightLabel={formatFanLabel('Вентилятор 2', 'vmanual')}
					labelWidth={150}
					labelGap={5}
				/>
			</Html>

			{/*BF21 и BF22 - объединенный компонент*/}
			<Html>
				<TriangleBlock 
					size={45}
					x={100}
					y={543}
					rotation={90}
					leftLabel={formatFanLabel('Вентилятор 3', 'vm2')}
					labelWidth={150}
					labelGap={5}
				/>

				<TriangleBlock 
					size={45}
					x={220}
					y={543}
					rotation={90}
					rightLabel={formatFanLabel('Вентилятор 4', 'formula')}
					labelWidth={150}
					labelGap={5}
				/>
			</Html>


			<Group x={120} y={410}>
				<Html>
					<SquareBlock 
						size={50}
						sign="+"
						x={250}
						y={0}
					/>
				</Html>
				<Html>
					<SquareBlock 
						size={50}
						sign="-"
						x={10}
						y={0}
					/>
				</Html>
				<Line points={[50, 15, 270, 15]} stroke={defaultColor} strokeWidth={2} />
				<Line points={[50, 37.7, 270, 37.7]} stroke={defaultColor} strokeWidth={2} />
				<Line points={[190, 37.7, 190, 20]} stroke={defaultColor} strokeWidth={2} />

				{/* <Text
					text={formatRecuperator('R_CURRENT')}
					x={70}
					y={20}
					fontSize={10}
					fill="#000"
				/> */}
				<Text
					text={formatRecuperator('R_EFFICIENCY')}
					x={225}
					y={42}
					fontSize={11}
					fill="#000"
				/>
				<Html>
					<TriangleBlockIcon size={20} x={120} y={37} />
				</Html>
				<Html>
					<TemperatureBlockIcon width={40} height={24} x={120} y={0} rotation={90} />
				</Html>
				<Html>
					<CanvasIcon src="/iconc.png" x={190} y={18} size={50} />
				</Html>
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



			

          </Layer>
        </Stage>
		</div>
      </div>
    </div>
  );
};

export default Schema;
