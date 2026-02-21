interface TriangleBlockProps {
	size: number
	x?: number
	y?: number
	rotation?: number
	label?: string
	leftLabel?: string
	rightLabel?: string
	labelSide?: 'left' | 'right'
	labelGap?: number
	labelWidth?: number
	labelFontSize?: number
	labelLineHeight?: number
	labelColor?: string
	labelOffsetY?: number 
}

export function TriangleBlock({
	size,
	x = 0,
	y = 0,
	rotation = 0,
	label,
	leftLabel,
	rightLabel,
	labelSide = 'left',
	labelGap = 16,
	labelWidth = 200,
	labelFontSize = 13,
	labelLineHeight = 16,
	labelColor = '#0F172A',
	labelOffsetY = 0,
}: TriangleBlockProps) {

	const commonLabelStyle: React.CSSProperties = {
		position: 'absolute',
		width: labelWidth,
		fontSize: labelFontSize,
		lineHeight: `${labelLineHeight}px`,
		color: labelColor,
		fontFamily: 'ui-monospace, SFMono-Regular, Menlo, sans-serif',
		fontWeight: 400,
		whiteSpace: 'pre-line',
		userSelect: 'none',
		top: '50%', 
		transform: `translateY(calc(-50% + ${labelOffsetY}px))`,
		display: 'flex',
		flexDirection: 'column',
		justifyContent: 'center',
		zIndex: 20,
	}

	const renderLabel = (text: string, side: 'left' | 'right' | 'center') => {
		const style: React.CSSProperties = {
			...commonLabelStyle,
			textAlign: side === 'center' ? 'center' : side === 'left' ? 'right' : 'left',
		}

		if (side === 'left') {
			style.right = '100%';
			style.marginRight = `${labelGap}px`;
		} else if (side === 'right') {
			style.left = '100%';
			style.marginLeft = `${labelGap}px`;
		} else {
             if (labelSide === 'left') {
                style.right = '100%';
                style.marginRight = `${labelGap}px`;
                style.textAlign = 'right';
             } else {
                style.left = '100%';
                style.marginLeft = `${labelGap}px`;
                style.textAlign = 'left';
             }
        }

		return (
			<div
				style={style}
				dangerouslySetInnerHTML={{ __html: text.replace(/\n/g, '<br />') }}
			/>
		)
	}

	return (
		<div
			style={{
				position: 'absolute',
				left: x, 
				top: y,
				width: size,
				height: size,
				transform: 'translate(-50%, -50%)',
				display: 'flex',
				alignItems: 'center',
				justifyContent: 'center',
				zIndex: 10,
			}}
		>
			<div
				style={{
					width: size,
					height: size,
					borderRadius: '50%',
					backgroundColor: '#FFFFFF',
					border: '1px solid #E2E8F0',
					boxShadow: '0 4px 12px -2px rgba(0, 0, 0, 0.08), 0 2px 6px -1px rgba(0, 0, 0, 0.04)',
					display: 'flex',
					alignItems: 'center',
					justifyContent: 'center',
					transform: `rotate(${rotation}deg)`, 
				}}
			>

				<svg
					width={size * 0.88} 
					height={size * 0.88}
					viewBox="0 0 24 24"
					fill="#F1F5F9"    
					stroke="#334155"  
					strokeWidth="1.4"   
					strokeLinejoin="round" 
					strokeLinecap="round"
					style={{ transform: 'translateY(0px)' }} 
				>
					{/* Твои точки */}
					<polygon points="5.2 2.5 23.8 12.1 5.2 22.2" />
				</svg>
			</div>

			{(leftLabel != null || rightLabel != null) ? (
				<>
					{leftLabel != null && leftLabel !== '' ? renderLabel(leftLabel, 'left') : null}
					{rightLabel != null && rightLabel !== '' ? renderLabel(rightLabel, 'right') : null}
				</>
			) : label ? (
				renderLabel(label, 'center') 
			) : null}
		</div>
	)
}