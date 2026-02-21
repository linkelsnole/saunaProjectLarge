interface TriangleBlockIconProps {
	size: number
	x?: number
	y?: number
	rotation?: number
}

export function TriangleBlockIcon({
	size,
	x = 0,
	y = 0,
	rotation = 0,
}: TriangleBlockIconProps) {
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
					<polygon points="5.2 2.5 23.8 12.1 5.2 22.2" />
				</svg>
			</div>
		</div>
	)
}
