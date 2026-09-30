import { afterEach, expect, it, vi } from 'vitest';
import { fetchWithAuth } from './authApi';
import { requestPanelUpdate, updateOutcome } from './panelUpdates';
vi.mock('./authApi', () => ({ fetchWithAuth: vi.fn() }));
afterEach(() => vi.resetAllMocks());
it('requests only the exact version the administrator approved', async () => {
  vi.mocked(fetchWithAuth).mockResolvedValue(new Response('{"status":"queued"}'));
  await requestPanelUpdate('small-production-17-1');
  expect(fetchWithAuth).toHaveBeenCalledWith('/api/v1/panel-updates', expect.objectContaining({
    method: 'POST', body: '{"version":"small-production-17-1"}',
  }));
});
it('does not report a rejected request as successful', async () => {
  vi.mocked(fetchWithAuth).mockResolvedValue(new Response('', { status: 409 }));
  await expect(requestPanelUpdate('small-production-17-1')).rejects.toThrow('409');
});
it('reloads only after this exact installation succeeds, not when the old server responds', () => {
  const version = 'small-production-17-1';
  for (const status of ['available', 'queued', 'installing'] as const)
    expect(updateOutcome({ enabled: true, version, status }, version)).toBe('waiting');
  expect(updateOutcome({ enabled: true, version: 'old', status: 'success' }, version)).toBe('waiting');
  expect(updateOutcome({ enabled: true, version, status: 'success' }, version)).toBe('reload');
  expect(updateOutcome({ enabled: true, version, status: 'failed' }, version)).toBe('failed');
});
