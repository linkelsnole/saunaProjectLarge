import { expect, it, vi } from 'vitest';
import { renderToStaticMarkup } from 'react-dom/server';
import { TempSettingsWidget } from './TempSettingsWidget';
vi.mock('@/api/useDashboard', () => ({useDashboardData: () => ({data: {
  temperatureControlModeZone1Code: 0, temperatureControlModeZone2Code: 0,
  tempSettings: {
    orpZone1: {currentT1: 15, targetT1: 20, currentT2: 20, targetT2: 18, currentT3: 22, targetT3: 14},
    orpZone2: {currentT1: 0, targetT1: -5, currentT2: 11, targetT2: 12, currentT3: 13, targetT3: 14},
  },
}})}));
vi.mock('@/api/hooks', () => ({useUpdateTempSettings: () => ({mutate: vi.fn()}), useSetTemperatureControl: () => ({mutate: vi.fn()}), useUpdateTemperatureSetpoints: () => ({mutate: vi.fn()})}));
vi.mock('@/contexts/AuthContext', () => ({useAuth: () => ({role: 'ROLE_ADMIN'})}));
vi.mock('react-i18next', () => ({useTranslation: () => ({t: (key: string) => key})}));
it('renders all ORP temperatures including zero and negative values in compact fields', () => {
  const html = renderToStaticMarkup(<TempSettingsWidget />);
  const fields = [...html.matchAll(/<input[^>]*>/g)].map(m => m[0]).filter(s => s.includes('pl-1 pr-5'));
  expect(fields).toHaveLength(12);
  expect(fields.map(s => s.match(/value="([^"]*)"/)?.[1])).toEqual(['15','20','20','18','22','14','0','-5','11','12','13','14']);
});
