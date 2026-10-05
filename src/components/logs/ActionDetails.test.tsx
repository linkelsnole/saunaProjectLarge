import { describe, expect, it } from 'vitest'
import { renderToStaticMarkup } from 'react-dom/server'
import { ActionDetails } from './ActionDetails'
describe('action details', () => {
  it('colors before and after, retaining all rows and units', () => {
    const html = renderToStaticMarkup(<ActionDetails success details="ПВ, зона 1: 60 → 65 °C; ЭКО: неизвестно → 3500 м³/ч" />)
    expect(html).toContain('text-slate-500">60')
    expect(html).toContain('text-emerald-700">65 °C')
    expect(html).toContain('неизвестно')
    expect(html).toContain('3500 м³/ч')
  })
  it('keeps historical text readable and errors out of green', () => {
    expect(renderToStaticMarkup(<ActionDetails success details="ЭКО → 3500.0 м³/ч" />)).toContain('ЭКО → 3500.0 м³/ч')
    const html = renderToStaticMarkup(<ActionDetails success={false} details="ПВ: 60 → 65 °C" />)
    expect(html).toContain('text-rose-600')
    expect(html).not.toContain('text-emerald')
    expect(renderToStaticMarkup(<ActionDetails success details={null} />)).toBe('')
  })
})
