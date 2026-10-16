import { afterEach, beforeEach, expect, it, vi } from 'vitest';
const reply = (data: unknown, status = 200, headers = {}) => new Response(JSON.stringify(data), { status, headers });
beforeEach(() => { vi.resetModules(); vi.stubGlobal('window', { dispatchEvent: vi.fn() }); });
afterEach(() => vi.unstubAllGlobals());

it('detects a missed deployment even on the first check of an old tab', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(reply({ buildId: 'other-build', updating: false })));
  const api = await import('./platformUpdate');
  await api.checkPlatform();
  expect(api.platformIsBlocked()).toBe(true);
});
it('blocks mutations while installing but permits status reads', async () => {
  const fetch = vi.fn().mockResolvedValue(reply({ buildId: 'new', updating: true }));
  vi.stubGlobal('fetch', fetch);
  const api = await import('./platformUpdate');
  await api.checkPlatform();
  await expect(api.platformFetch('/api/v1/devices/start', { method: 'POST' })).rejects.toThrow('не отправлена');
  expect(fetch).toHaveBeenCalledTimes(1);
  await api.platformFetch('/api/v1/devices/dashboard', {});
  expect(fetch).toHaveBeenCalledTimes(2);
});
it('does not retry a command rejected by the server during a deployment race', async () => {
  const fetch = vi.fn().mockResolvedValue(reply({}, 503, { 'X-Panel-Update': 'required' }));
  vi.stubGlobal('fetch', fetch);
  const api = await import('./platformUpdate');
  await expect(api.platformFetch('/api/v1/devices/start', { method: 'POST' })).rejects.toThrow('не выполнена');
  expect(fetch).toHaveBeenCalledTimes(1);
  expect(api.platformIsBlocked()).toBe(true);
});
it('does not interpret an ordinary network failure or 503 as a deployment', async () => {
  vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('offline')));
  const api = await import('./platformUpdate');
  await expect(api.checkPlatform()).rejects.toThrow();
  expect(api.platformIsBlocked()).toBe(false);
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(reply({}, 503)));
  expect((await api.platformFetch('/api/v1/devices/start', { method: 'POST' })).status).toBe(503);
  expect(api.platformIsBlocked()).toBe(false);
});
it('leaves the current build usable and attaches its identity to commands', async () => {
  const api = await import('./platformUpdate');
  const fetch = vi.fn().mockResolvedValue(reply({ buildId: api.platformBuildId, updating: false }));
  vi.stubGlobal('fetch', fetch);
  await api.checkPlatform();
  expect(api.platformIsBlocked()).toBe(false);
  await api.platformFetch('/api/v1/devices/start', { method: 'POST' });
  expect(new Headers(fetch.mock.calls[1][1].headers).get('X-Panel-Build')).toBe(api.platformBuildId);
});
