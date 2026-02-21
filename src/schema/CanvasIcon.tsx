interface CanvasIconProps {
	src: string
	x?: number
	y?: number
	width?: number
	height?: number
	size?: number
}

export function CanvasIcon({
	src,
	x = 0,
	y = 0,
	width: w,
	height: h,
	size,
}: CanvasIconProps) {
	const width = w ?? size ?? 24
	const height = h ?? size ?? 24

	return (
		<div
			style={{
				position: 'absolute',
				left: x,
				top: y,
				width,
				height,
				transform: 'translate(-50%, -50%)',
				display: 'flex',
				alignItems: 'center',
				justifyContent: 'center',
				zIndex: 10,
			}}
		>
			<img
				src={src}
				alt=""
				style={{
					width: '100%',
					height: '100%',
					objectFit: 'contain',
				}}
			/>
		</div>
	)
}
