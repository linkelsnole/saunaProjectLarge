interface StatusLineProps {
  text: string
  x?: number
  y?: number
  width?: number
  fontSize?: number
}

export function StatusLine({
  text,
  x = 0,
  y = 0,
  width = 225,
  fontSize = 12,
}: StatusLineProps) {
  return (
    <div
      style={{
        position: 'absolute',
        left: x,
        top: y,
        minWidth: width,
        backgroundColor: '#FFFFFF',
        border: '1.6px solid #E2E8F0',
        borderRadius: '12px',
        boxShadow:
          '0 4px 12px -2px rgba(0, 0, 0, 0.08), 0 2px 6px -1px rgba(0, 0, 0, 0.04)',
        padding: '8px 12px',
        fontSize: fontSize,
        fontFamily: 'Inter, Arial, sans-serif',
        fontWeight: 400,
        color: '#0F172A',
        lineHeight: '1.2',
        whiteSpace: 'nowrap',
        userSelect: 'none',
        pointerEvents: 'none',
        zIndex: 20,
      }}
    >
      {text}
    </div>
  )
}
