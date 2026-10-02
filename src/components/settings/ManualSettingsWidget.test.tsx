import { describe, expect, it, vi } from 'vitest';
import { renderToStaticMarkup } from 'react-dom/server';
import { ManualSettingsWidget } from './ManualSettingsWidget';
const fixture = vi.hoisted(() => ({ airflow: 15000 }));
vi.mock('@/api/useDashboard', () => ({ useDashboardData: () => ({data: {
  volumeSettings: { manualAirflow: fixture.airflow, ecoAirflow: 15000, normalAirflow: 21000, intensiveAirflow: 21000 },
  ventilationModeCode: 4, screenMode: true, recuperatorStatus: { enabled: true }
}}) }));
vi.mock('@/api/hooks', () => ({ useSetVentilationMode: () => ({mutate: vi.fn()}), useUpdateVolumeSettings: () => ({mutate: vi.fn()}) }));
vi.mock('@/contexts/AuthContext', () => ({useAuth: () => ({role: 'ROLE_ADMIN'})}));
vi.mock('react-i18next', () => ({useTranslation: () => ({t: (key: string) => key})}));
describe('airflow controls', () => {
  it.each([[0,0],[15000,15000/21000*100],[21000,100]])('renders %s with matching slider position', (value, percent) => {
    fixture.airflow=value;
    const html=renderToStaticMarkup(<ManualSettingsWidget/>);
    const range=html.match(/<input[^>]*type="range"[^>]*>/)?.[0] ?? '';
    expect(range).toContain('min="0"'); expect(range).toContain('max="21000"');
    expect(range).toContain('step="500"'); expect(range).toContain(`value="${value}"`);
    expect(html).toContain(`width:${percent}%`);
    expect(html).toContain(`left:calc(${percent}% + 4px)`);
    const presets=[...html.matchAll(/<input[^>]*type="number"[^>]*>/g)].slice(0,4);
    expect(presets).toHaveLength(4);
    for(const [input] of presets) {expect(input).toContain('min="0"');expect(input).toContain('max="21000"');}
  });
});

vi.mock('@tanstack/react-query', () => ({useIsMutating: () => 0}));
