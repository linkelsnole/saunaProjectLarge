interface TemperatureBlockIconProps {
 tone?: string
	width: number
	height: number
	x?: number
	y?: number
	rotation?: number
}

export function TemperatureBlockIcon({
 tone,
	width,
	height,
	x = 0,
	y = 0,
	rotation = 0,
}: TemperatureBlockIconProps) {
	const triangleSize = height * 0.8
	const lineLength = width * 0.3

	return (
		<div
			style={{
				position: 'absolute',
				left: x,
				top: y,
				width: triangleSize + lineLength,
				height: triangleSize,
				display: 'flex',
				alignItems: 'center',
				justifyContent: 'center',
				transform: `translate(-50%, -50%) rotate(${rotation}deg)`,
				transformOrigin: 'center',
				zIndex: 10,
				pointerEvents: 'none',
			}}
		>
			<svg
				width={triangleSize + lineLength}
				height={triangleSize}
				viewBox={`0 0 ${triangleSize + lineLength} ${triangleSize}`}
			>
				<polygon
					points={`5,0 ${triangleSize},${triangleSize / 2} 5,${triangleSize}`}
					fill={tone ?? "#000"}
					stroke={tone ?? "#000"}
					strokeWidth="1"
				/>
				<line
					x1={triangleSize}
					y1={triangleSize / 2}
					x2={triangleSize + lineLength}
					y2={triangleSize / 2}
					stroke={tone ?? "#000"}
					strokeWidth="2"
				/>
			</svg>
		</div>
	)
}
