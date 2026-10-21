interface TemperatureBlockProps {
 tone?: string
  width: number
  height: number
  target?: string
  text: string
  subText?: string
  x?: number
  y?: number
  rotation?: number
  labelOffsetX?: number
  labelOffsetY?: number
  labelWidth?: number
  keepLabelUpright?: boolean
  subTextFontSize?: number
  subTextColor?: string
  subTextOffsetY?: number
}

export function TemperatureBlock({
 tone,
  width,
  height,
  text,
  target,
  subText,
  x = 0,
  y = 0,
  rotation = 0,
  labelOffsetX = 0,
  labelOffsetY = 0,
  labelWidth = 40,
  keepLabelUpright = true,
  subTextColor = '#4b5563',
  subTextOffsetY = 0,
}: TemperatureBlockProps) {
  const triangleSize = height * 0.8
  const lineLength = width * 0.3

  const labelPaddingX = 4
  const labelPaddingY = 4
  const badgeGap = 6
  const mainFontSize = 13
  const subFontSize = 10
  const labelRotation = keepLabelUpright ? 0 : rotation

  return (
    <div
      style={{
        position: 'absolute',
        left: x,
        top: y,
        height: height,
        display: 'flex',
        alignItems: 'center',
        gap: 6,
        zIndex: 10,
        pointerEvents: 'none',
      }}
    >
      <div
        style={{
          minWidth: Math.max(labelWidth, width),
          display: 'flex',
          alignItems: 'stretch',
          textAlign: 'right',
          transform: `translate(${labelOffsetX}px, ${labelOffsetY}px) rotate(${labelRotation}deg)`,
          transformOrigin: 'center',
        }}
      >
        <div
          style={{
            width: target ? 70 : undefined,
            boxSizing: 'border-box',
            backgroundColor: tone ? (tone === '#dc2626' ? '#fff1f2' : tone === '#d97706' ? '#fffbeb' : '#ecfdf5') : '#FFFFFF',
            border: '1.4px solid #E2E8F0',
            borderRadius: '12px',
            boxShadow:
              '0 4px 10px -4px rgba(0, 0, 0, 0.12), 0 2px 6px -3px rgba(0, 0, 0, 0.08)',
            padding: `${labelPaddingY + 2}px ${labelPaddingX + 4}px`,
            display: 'flex',
            flexDirection: 'column',
            gap: badgeGap,
            userSelect: 'none',
          }}
        >
          {target && <div aria-label="Цель температуры" style={{display:'flex',alignItems:'baseline',justifyContent:'space-between',gap:3,color:'#059669',fontFamily:'Inter, Arial, sans-serif',whiteSpace:'nowrap',paddingBottom:6,borderBottom:'1px solid #e2e8f0'}}><span style={{fontSize:8}}>Цель</span><span style={{fontSize:11,fontWeight:500}}>{target}</span></div>}
          <div
            style={{
              fontSize: mainFontSize,
              fontFamily: 'Inter, Arial, sans-serif',
              fontWeight: 400,
              color: tone ?? '#0F172A',
              lineHeight: '1',
              whiteSpace: 'nowrap',
              textAlign: 'right',
            }}
          >
            {text}
          </div>
          {subText ? (
            <>
              <div
                style={{
                  height: 1,
                  background:
                    'linear-gradient(90deg, rgba(0,0,0,0) 0%, rgba(148,163,184,0.7) 50%, rgba(0,0,0,0) 100%)',
                }}
              />
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  gap: 3,
                  marginTop: subTextOffsetY,
                }}
              >
                <svg
                  width={15}
                  height={15}
                  viewBox="0 0 24 24"
                  fill="none"
                  style={{ flex: '0 0 auto' }}
                >
                  <defs>
                    <linearGradient id="dropGradient" x1="0%" y1="0%" x2="0%" y2="100%">
                      <stop offset="0%" style={{ stopColor: '#60a5fa', stopOpacity: 1 }} />
                      <stop offset="100%" style={{ stopColor: '#3b82f6', stopOpacity: 1 }} />
                    </linearGradient>
                  </defs>
                  <path
                    d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"
                    fill="url(#dropGradient)"
                    style={{ filter: 'drop-shadow(0 2px 4px rgba(59, 130, 246, 0.3))' }}
                  />
                  <circle cx="9" cy="12" r="1.5" fill="rgba(255, 255, 255, 0.5)" />
                </svg>
                <div
                  style={{
                    fontSize: subFontSize,
                    fontFamily: 'Inter, Arial, sans-serif',
                    fontWeight: 400,
                    color: subTextColor,
                    lineHeight: '1',
                    whiteSpace: 'nowrap',
                    textAlign: 'right',
                  }}
                >
                  {subText}
                </div>
              </div>
            </>
          ) : null}
        </div>
      </div>

      <div
        style={{
          width: triangleSize + lineLength,
          height: triangleSize,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          transform: `rotate(${rotation}deg)`,
          transformOrigin: 'center',
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
    </div>
  )
}