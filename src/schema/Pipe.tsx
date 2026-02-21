import { Group, Line } from 'react-konva'

interface PipeProps {
	points: number[]
	type: 'hot' | 'cold' | 'return'
	opacity?: number
}

export function Pipe({ points, type, opacity = 1 }: PipeProps) {
	// Цвета: Используем насыщенные профессиональные оттенки (Tailwind palette)
	const colorMap = {
		hot: '#E11D48',   // Rose-600: Благородный красный, не вырвиглазный
		cold: '#2563EB',  // Blue-600: Классический инженерный синий
		return: '#64748B', // Slate-500: Нейтральный серый для обратки
	}

	return (
		<Group opacity={opacity}>
			<Line
				points={points}
				stroke={colorMap[type]}
				strokeWidth={4} // Оптимальная толщина для схемы
				lineCap="round"
				lineJoin="round" // Скругленные углы - обязательно
				// Мягкая тень для объема (без жесткой обводки)
				shadowColor="black"
				shadowBlur={6}
				shadowOpacity={0.15}
				shadowOffset={{ x: 1, y: 2 }}
			/>
		</Group>
	)
}