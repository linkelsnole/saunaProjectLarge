import { useState, useEffect, useRef } from 'react';
import { Layer, Line, Stage, Group, Text } from 'react-konva';
import { Html } from 'react-konva-utils';
import { useDashboardData } from '../api/useDashboard';
import { PressureBlock } from './PressureBlock';
import { SquareBlock } from './SquareBlock';
import { TemperatureBlock } from './TemperatureBlock';
import { DiagonalBlock } from './DiagonalBlock';
import { TriangleBlock } from './TriangleBlock';

import { ControlButton } from '@/shared/ui/control-button';
import { toggleRecuperator } from '@/api/deviceApi';
import { toast } from 'sonner';
import { Loader2 } from 'lucide-react';
import { useAuth } from '@/contexts/AuthContext';

const LoadingOverlay = () => (
	<div className="absolute inset-0 z-50 flex items-center justify-center bg-white/50 backdrop-blur-[10px]">
		<div className="flex flex-col items-center gap-3 p-6 bg-white/80 rounded-2xl shadow-lg">
			<Loader2 className="w-10 h-10 text-blue-500 animate-spin" />
			<span className="text-sm font-medium text-gray-600">Получение данных...</span>
		</div>
	</div>
);

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

	const formatPressure = (id: string): string => {
		const sensor = getWSensor(id);
		const value = sensor?.value;
		return value != null ? `${value} Па` : '-- Па';
	};


	const formatValveLabel = (valveId: string, tempId: string): string => {
		const valve = getValve(valveId);
		const temp = getClimate(tempId);
		
		const valveValue = valve?.value;
		const tempValue = temp?.temperature;
		
		const valveInt = valveValue != null ? Math.round(parseFloat(valveValue)) : null;
		const valveStr = valveInt != null ? `${valveInt} % (клапан)` : '-- % (клапан)';
		const tempStr = tempValue != null ? `${tempValue.toFixed(1)} °C` : '-- °C';
		
		return `${valveStr}\n${tempStr}`;
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
			const current = data?.recupCurrent;
			return current != null ? `${current} об/мин` : '-- об/мин';
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
		style={{ backgroundImage: `url(/bg2.jpg)` }}
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

        <Stage 
          width={stageSize.width} 
          height={stageSize.height} 
          ref={stageRef}
          scaleX={scale}
          scaleY={scale}
          style={{
            position: 'absolute',
            zIndex: 20
          }}
        >
          <Layer scale={{x: 1, y: 1}}>
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
		  <Line points={[160, 695, 160, 50]}  strokeWidth={2.5} stroke="red" />
		  <Line points={[395, 695, 395, 230]}  strokeWidth={2.5} stroke="#006AD2"/>
		  <Line points={[310, 230, 400, 230]} strokeWidth={2.5} stroke="#006AD2"/>
		  <Line points={[350, 230, 480, 230]} strokeWidth={2.5} stroke="#006AD2"/>
		  <Line points={[310, 50, 310, 231]} strokeWidth={2.5} stroke="#006AD2"/>
		  <Line points={[480, 50, 480, 231]} strokeWidth={2.5} stroke="#006AD2"/>

		  {/* Линии до ΔP2*/}
		  {/* <Line points={[90, 350, 160, 350]} stroke={defaultColor} strokeWidth={2.5} />
		  <Line points={[90, 450, 160, 450]} stroke={defaultColor} strokeWidth={2.5} />
		  <Line points={[90, 349, 90, 451]} stroke={defaultColor} strokeWidth={2.5} /> */}

		   {/* BPCalib BP5 и BP10
		  <DeltaPBlock
			x={90}
			y={400}
			text="ΔP"
			label={`${formatPressureSensor('BP5')}\n${formatPressureSensor('BP10')}`}
			labelWidth={70}
			labelOffsetX={-15}
			labelOffsetY={-5}
		  /> */}

		{/* BP2 */}
		  	<PressureBlock 
				width={45}
				height={45}
				text={formatPressure('BP2')}
				x={70}
				y={220}
			/>
			{/* BP1 */}
			<PressureBlock 
				width={45}
				height={45}
				text={formatPressure('BP1')}
				x={295}
				y={460}
				rotation={180}
				labelWidth = {70}
				labelOffsetX = {120}
			/>
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

			<TemperatureBlock 
				width={100}
				height={25}
				text={formatTemperature('BT2')}
				x={36}
				y={300}
			/>
			<TemperatureBlock
				width={100}
				height={25}
				text={formatTemperature('BT3')}
				x={372}
				y={540}
				rotation={-180}
				labelWidth={70}
				labelOffsetX={62}
				labelOffsetY={0}
			/>
			<TemperatureBlock
				width={100}
				height={25}
				text={formatTemperature('BT10')}
				x={372}
				y={340}
				rotation={-180}
				labelWidth={70}
				labelOffsetX={62}
				labelOffsetY={0}
			/>
			{/* Слева под вентилятором */}
			<TemperatureBlock 
				width={100}
				height={25}
				text={formatTemperature('BT11')}
				x={36}
				y={610}
			/>
			{/* Справа над - */}
			<TemperatureBlock
				width={100}
				height={25}
				text={formatTemperature('BT21')}
				x={456}
				y={50}
				rotation={-180}
				labelWidth={70}
				labelOffsetX={62}
				labelOffsetY={0}
			/>
			{/* Слева над - */}
			<TemperatureBlock
				width={100}
				height={25}
				text={formatTemperature('BT1')}
				x={186}
				y={50}
				labelWidth={70}
				labelOffsetX={0}
				labelOffsetY={0}
			/>
			
			{/*MG2 MG1 - задвижки притока и вытяжки*/}
			<Group y={640}>
			<DiagonalBlock 
				size={45}
				x={373}
				isOpen={data?.damperSupply}
			/>
			<DiagonalBlock 
				size={45}
				x={136}
				isOpen={data?.damperExhaust}
			/>
			</Group>
			
			{/*BF12 и BF11 - объединенный компонент*/}
			<TriangleBlock 
				size={45}
				x={395}
				y={285}
				rotation={-90}
				leftLabel={formatFanLabel('Вентилятор 1', 'vm1')}
				rightLabel={formatFanLabel('Вентилятор 2', 'vmanual')}
				labelWidth={150}
				labelGap={30}
				rightLabelOffsetX={-43}
			/>

			{/*BF21 и BF22 - объединенный компонент*/}
			<TriangleBlock 
				size={45}
				x={160}
				y={528}
				rotation={90}
				leftLabel={formatFanLabel('Вентилятор 3', 'vm2')}
				rightLabel={formatFanLabel('Вентилятор 4', 'formula')}
				labelWidth={150}
				labelGap={30}
				rightLabelOffsetX={-43}
			/>



			<Group x={120} y={380}>
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
				<Line points={[60, 1, 250, 1]} stroke={defaultColor} strokeWidth={2} />
				<Line points={[59, 48.7, 250, 48.7]} stroke={defaultColor} strokeWidth={2} />
			</Group>

			<Group y={210} x={320}>
				<Text
					text={`${formatZoneTemperature('T_ZONE1')}`}
					x={0}
					y={0}
					fontSize={13}
					fill="green"
				/>
				<Text
					text={`${formatZoneTemperature('T_ZONE2')}`}
					x={110}
					y={0}
					fontSize={13}
					fill="green"
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
				{/* <Text
					text={formatRecuperator('R_CURRENT')}
					x={0}
					y={15}
					fontSize={10}
					fill="#000"
				/> */}
				<Text
					text={formatRecuperator('R_EFFICIENCY')}
					x={0}
					y={30}
					fontSize={10}
					fill="#000"
				/>
			</Group>

          </Layer>
        </Stage>
      </div>
    </div>
  );
};

export default Schema;
