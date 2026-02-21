interface PressureBlockProps {
	width: number
	height: number
	text: string
	x?: number
	y?: number
	rotation?: number
	labelWidth?: number
	labelOffsetX?: number
	labelOffsetY?: number
	labelFontSize?: number
	labelColor?: string
	labelAlign?: 'left' | 'center' | 'right'
}

export function PressureBlock({
	width,
	height,
	text,
	x = 0,
	y = 0,
	rotation = 0,
	labelWidth = 60,
	labelOffsetX = 0,
	labelOffsetY = 0,
	labelFontSize = 13, // Чуть уменьшил для аккуратности
	labelColor = '#0F172A', // Slate-900
	labelAlign = 'right',
}: PressureBlockProps) {
	
	// Отступ между текстом и блоком
	const gap = 8 

	return (
		<div
			style={{
				position: 'absolute',
				left: x,
				top: y,
				display: 'flex',
				alignItems: 'center',
				// Если нужно, чтобы текст был справа, поменяй на 'row-reverse'
				flexDirection: 'row', 
				zIndex: 10,
			}}
		>
			{/* 1. ТЕКСТ */}
			<div
				style={{
					width: labelWidth,
					marginRight: gap,
					fontSize: labelFontSize,
					lineHeight: '1.2',
					color: labelColor,
					fontFamily: 'ui-monospace, SFMono-Regular, Menlo, sans-serif',
					fontWeight: 400,
					textAlign: labelAlign,
					whiteSpace: 'pre-line',
					userSelect: 'none',
					// Ручная коррекция позиции текста, если нужна
					transform: `translate(${labelOffsetX}px, ${labelOffsetY}px)`,
				}}
			>
				{text}
			</div>

			{/* 2. БЛОК С "W" */}
			<div
				style={{
					width: width,
					height: height,
					backgroundColor: '#FFFFFF',
					border: '1px solid #E2E8F0',
					borderRadius: '14px', 
					boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03)',
					// ----------------------
					display: 'flex',
					alignItems: 'center',
					justifyContent: 'center',
					// Вращаем САМ БЛОК, текст остается на месте
					transform: `rotate(${rotation}deg)`, 
				}}
			>
				<svg
					width={width * 0.8} // Отступы внутри
					height={height * 0.9}
					viewBox="0 0 24 24"
					fill="none"
					stroke="#0F172A" // Темно-синий/черный
					strokeWidth="2"
					strokeLinecap="round" 
					strokeLinejoin="round" // ВАЖНО: Скругляет острые углы зигзага
				>
					{/* Рисуем "W" (или две галочки вниз).
                        Точки: Верх -> Низ -> Верх -> Низ -> Верх
                        Это повторяет геометрию твоего Konva Line
                    */}
					<polyline points="2 2 7 22 12 2 17 22 22 2" />
				</svg>
			</div>
		</div>
	)
}