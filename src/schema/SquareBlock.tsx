interface SquareBlockProps {
	size: number
	sign: '+' | '-'
	x?: number
	y?: number
	label?: string
	labelOffsetX?: number
	labelOffsetY?: number
	labelGap?: number
	labelWidth?: number
	labelFontSize?: number
	labelLineHeight?: number
	labelColor?: string
}

export function SquareBlock({
	size,
	sign,
	x = 0,
	y = 0,
	label,
	labelOffsetX = 0,
	labelOffsetY = 0,
	labelGap = 5,
	labelWidth = 140,
	labelFontSize = 12,
	labelLineHeight = 16,
	labelColor = '#0F172A', // Slate-900 (Почти черный, для контраста)
}: SquareBlockProps) {
	const circleSize = size * 0.65

	return (
		<div
			style={{
				position: 'absolute',
				left: x,
				top: y,
				width: size,
				height: size,
				backgroundColor: '#FFFFFF', 
				border: '1.9px solid #cfd5e1', 
				borderRadius: '14px', 
				boxShadow: '0 4px 12px -2px rgba(0, 0, 0, 0.08), 0 2px 6px -1px rgba(0, 0, 0, 0.04)',
				display: 'flex',
				alignItems: 'center',
				justifyContent: 'center',
				overflow: 'visible',
				zIndex: 10,
			}}
		>
			{label ? (
				<div
					style={{
						position: 'absolute',
						left: size + labelGap + labelOffsetX,
						top: labelOffsetY,
						width: labelWidth,
						fontSize: labelFontSize,
						lineHeight: `${labelLineHeight}px`,
						fontFamily: 'ui-monospace, SFMono-Regular, Menlo, sans-serif',
						fontWeight: 400, 
						color: labelColor,
						whiteSpace: 'pre-line',
						userSelect: 'none',
					}}
				>
					{label}
				</div>
			) : null}

			{/* Внутренний круг (Эффект углубления) */}
			<div
				style={{
					width: circleSize,
					height: circleSize,
					borderRadius: '50%',
					backgroundColor: '#F1F5F9', 
					display: 'flex',
					alignItems: 'center',
					justifyContent: 'center',
					position: 'relative',
					boxShadow: 'inset 0 2px 4px 0 rgba(0, 0, 0, 0.12)',
					border: '1px solid rgba(0,0,0,0.02)' 
					
				}}
			>

				<svg
					width={circleSize * 0.55}
					height={circleSize * 0.55}
					viewBox="0 0 24 24"
					style={{ display: 'block', opacity: 0.8 }} // Чуть прозрачности для мягкости
				>
					{sign === '+' ? (
						<>
							<line
								x1="12.5" y1="5" x2="12.5" y2="19"
								stroke="#0F172A"
								strokeWidth="3" // Жирнее (было 2.5)
								strokeLinecap="round"
							/>
							<line
								x1="5.5" y1="12" x2="19.5" y2="12"
								stroke="#0F172A"
								strokeWidth="3"
								strokeLinecap="round"
							/>
						</>
					) : (
						<line
							x1="5" y1="12" x2="19" y2="12"
							stroke="#0F172A"
							strokeWidth="3"
							strokeLinecap="round"
						/>
					)}
				</svg>
			</div>
		</div>
	)
}