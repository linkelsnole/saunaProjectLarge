import { expect, it, vi } from 'vitest';
import { renderToStaticMarkup } from 'react-dom/server';
import { DeviceStatePanel } from './DeviceStatePanel';
const mock = vi.hoisted(() => ({ error: null as Error | null }));
vi.mock('@/api/useDashboard', () => ({ useDashboardData: () => ({
  error: mock.error, isLoading: false,
  data: { deviceStates: [0, 1, 2, 3, null].map((alarmMode, i) => ({ id: `T${i}`, baseAddress: 4096 + i * 16, alarmMode, alarmSeconds: alarmMode ? 120 : null })) },
}) }));
it('shows device names and four distinct states including duration', () => {
  mock.error = null;
  const html = renderToStaticMarkup(<DeviceStatePanel />);
  for (const label of ['Норма', 'Предупреждение', 'Неисправность', 'Авария', 'Нет данных', '120', 'T0', 'T4']) expect(html).toContain(label);
  expect(html).toContain('bg-orange-50');
  expect(html).toContain('bg-red-50');
});
it('does not show stale values as confirmed normal or active alarms after a connection error', () => {
  mock.error = new Error('offline');
  const html = renderToStaticMarkup(<DeviceStatePanel />);
  expect(html).toContain('Нет связи');
  expect(html).not.toContain('bg-emerald-50');
  expect(html).not.toContain('bg-red-50');
  expect(html).not.toContain('120');
  mock.error = null;
});
