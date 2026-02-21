import { Group, Rect, Text } from 'react-konva'

interface StatusBarBlockProps {
  x?: number
  y?: number
  width?: number
  height?: number
  text?: string
  fontSize?: number
}

export function StatusBarBlock({
  x = 0,
  y = 0,
  width = 520,
  height = 44,
  text = '',
  fontSize = 13,
}: StatusBarBlockProps) {
  const paddingX = 12
  const paddingY = 8

  return (
    <Group x={x} y={y} listening={false}>
      <Rect
        width={width}
        height={height}
        fill="#FFFFFF"
        stroke="#E2E8F0"
        strokeWidth={1.6}
        cornerRadius={12}
        shadowColor="#000"
        shadowBlur={8}
        shadowOpacity={0.08}
        shadowOffsetY={2}
      />
      <Text
        text={text}
        x={paddingX}
        y={paddingY}
        width={Math.max(0, width - paddingX * 2)}
        height={Math.max(0, height - paddingY * 2)}
        fontSize={fontSize}
        fontFamily="Inter, Arial, sans-serif"
        fontStyle="normal"
        fill="#0F172A"
        align="left"
        verticalAlign="middle"
      />
    </Group>
  )
}
