interface TriangleBlockIconProps {
	size: number
	x?: number
	y?: number
	rotation?: number
	isActive?: boolean
	isStopped?: boolean
}

export function TriangleBlockIcon({
	size,
	x = 0,
	y = 0,
	rotation = 0,
	isActive = false,
	isStopped = false,
}: TriangleBlockIconProps) {
	// Остановка перебивает всё (красный), иначе активность — зелёный, по умолчанию — серый.
	let fillColor = '#F1F5F9'
	let strokeColor = '#334155'
	let borderColor = '#E2E8F0'
	if (isStopped) {
		fillColor = '#fee2e2'
		strokeColor = '#dc2626'
		borderColor = '#fca5a5'
	} else if (isActive) {
		fillColor = '#dcfce7'
		strokeColor = '#16a34a'
		borderColor = '#86efac'
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
					border: `1.5px solid ${borderColor}`,
					boxShadow: isStopped
						? '0 0 8px rgba(220, 38, 38, 0.5), inset 0 2px 4px rgba(0,0,0,0.05)'
						: isActive
							? '0 0 8px rgba(22, 163, 74, 0.4), inset 0 2px 4px rgba(0,0,0,0.05)'
							: '0 4px 12px -2px rgba(0, 0, 0, 0.08), 0 2px 6px -1px rgba(0, 0, 0, 0.04)',
					display: 'flex',
					alignItems: 'center',
					justifyContent: 'center',
					transform: `rotate(${rotation}deg)`,
					transition: 'all 0.3s ease',
				}}
			>
				<svg
					width={size * 0.88}
					height={size * 0.88}
					viewBox="0 0 24 24"
					fill={fillColor}
					stroke={strokeColor}
					strokeWidth="1.4"
					strokeLinejoin="round"
					strokeLinecap="round"
					style={{ transform: 'translateY(0px)', transition: 'all 0.3s ease' }}
				>
					<polygon points="5.2 2.5 23.8 12.1 5.2 22.2" />
				</svg>
			</div>
		</div>
	)
}
