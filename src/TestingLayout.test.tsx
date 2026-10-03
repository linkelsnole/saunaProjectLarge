import { expect, it, vi } from 'vitest';
import { renderToStaticMarkup } from 'react-dom/server';
import { MemoryRouter } from 'react-router-dom';
import App from './App';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

vi.stubGlobal('localStorage', { getItem: () => null });

const auth = vi.hoisted(() => ({ role: 'ROLE_TESTER' }));
vi.mock('@/contexts/AuthContext', () => ({ useAuth: () => ({ role: auth.role, isAuthenticated: true, isLoading: false }) }));
vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }));
vi.mock('@/header/AppHeader', () => ({ default: () => <div>header</div> }));
vi.mock('@/schema/Schema', () => ({ default: () => <div data-testid="installation-diagram" /> }));
vi.mock('@/components/settings/ManualSettingsWidget', () => ({ ManualSettingsWidget: () => <div data-testid="volume-widget" /> }));
vi.mock('@/components/settings/TempSettingsWidget', () => ({ TempSettingsWidget: () => null }));
vi.mock('@/components/settings/RecuperatorWidget', () => ({ RecuperatorWidget: () => null }));
vi.mock('@/components/settings/ScheduleWorkspace', () => ({ ScheduleWorkspace: () => null }));
vi.mock('@/components/settings/ConfiguratorWidget', () => ({ ConfiguratorWidget: () => null }));
vi.mock('@/components/logs/SystemEventsJournal', () => ({ default: () => null }));
vi.mock('@/components/logs/DashboardLogs', () => ({ default: () => null }));
vi.mock('@/components/settings/UpdateSettings', () => ({ default: () => null }));
vi.mock('@/components/updates/PanelUpdateNotice', () => ({ PanelUpdateNotice: () => null }));
vi.mock('@/components/testing/TestingWorkspace', () => ({ TestingWorkspace: () => <div data-testid="testing-widget" /> }));

function markup(role: string) {
  auth.role = role;
  return renderToStaticMarkup(<QueryClientProvider client={new QueryClient()}><MemoryRouter initialEntries={['/']}><App /></MemoryRouter></QueryClientProvider>);
}
it('keeps the diagram and regular tabs for testers and adds a testing tab', () => {
  const html = markup('ROLE_TESTER');
  expect(html).toContain('data-testid="installation-diagram"');
  expect(html).toContain('data-testid="volume-widget"');
  for (const name of ['volume', 'temperature', 'recuperator', 'schedule', 'testing']) {
    expect(html).toContain(`trigger-${name}`);
  }
  expect(html).not.toContain('trigger-errors');
  expect(html).not.toContain('trigger-update');
});
it.each(['ROLE_ADMIN', 'ROLE_USER', 'ROLE_READONLY'])('keeps testing hidden from %s', role => {
  const html = markup(role);
  expect(html).toContain('data-testid="installation-diagram"');
  expect(html).not.toContain('trigger-testing');
});
