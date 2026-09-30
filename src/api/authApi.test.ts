import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest';

const reply = (status: number, data: unknown = {}) => new Response(JSON.stringify(data), {
  status, headers: { 'Content-Type': 'application/json' },
});
const authData = { accessToken: 'renewed', user: { username: 'test', role: 'ROLE_USER' } };

beforeEach(() => {
  vi.resetModules();
  vi.stubGlobal('window', { location: { pathname: '/', href: '' } });
});
afterEach(() => { vi.unstubAllGlobals(); });

describe('session recovery', () => {
  it('renews an expired access token and repeats the request', async () => {
    const fetch = vi.fn().mockResolvedValueOnce(reply(401))
      .mockResolvedValueOnce(reply(200, authData)).mockResolvedValueOnce(reply(200));
    vi.stubGlobal('fetch', fetch);
    const api = await import('./authApi');
    api.setAccessToken('expired');
    expect((await api.fetchWithAuth('/api/v1/devices/dashboard')).status).toBe(200);
    expect(new Headers(fetch.mock.calls[2][1].headers).get('Authorization')).toBe('Bearer renewed');
    expect(window.location.href).toBe('');
  });

  it.each([500, 502, 503, 429])('keeps the session when refresh returns %s and recovers on the next poll', async (status) => {
    const fetch = vi.fn().mockResolvedValueOnce(reply(401)).mockResolvedValueOnce(reply(status))
      .mockResolvedValueOnce(reply(401)).mockResolvedValueOnce(reply(200, authData))
      .mockResolvedValueOnce(reply(200));
    vi.stubGlobal('fetch', fetch);
    const api = await import('./authApi');
    api.setAccessToken('expired');
    await expect(api.fetchWithAuth('/api/v1/devices/dashboard')).rejects.toThrow();
    expect(api.getAccessToken()).toBe('expired');
    expect(window.location.href).toBe('');
    expect((await api.fetchWithAuth('/api/v1/devices/dashboard')).status).toBe(200);
  });

  it('keeps the session when the refresh response is lost', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValueOnce(reply(401))
      .mockRejectedValueOnce(new TypeError('Network unavailable')));
    const api = await import('./authApi');
    api.setAccessToken('expired');
    await expect(api.fetchWithAuth('/api/v1/devices/dashboard')).rejects.toThrow();
    expect(api.getAccessToken()).toBe('expired');
    expect(window.location.href).toBe('');
  });

  it('redirects only when refresh confirms the session is invalid', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValueOnce(reply(401)).mockResolvedValueOnce(reply(401)));
    const api = await import('./authApi');
    api.setAccessToken('expired');
    await api.fetchWithAuth('/api/v1/devices/dashboard');
    expect(api.getAccessToken()).toBeNull();
    expect(window.location.href).toBe('/login');
  });

  it('does not refresh or log out on a permissions denial', async () => {
    const fetch = vi.fn().mockResolvedValue(reply(403));
    vi.stubGlobal('fetch', fetch);
    const api = await import('./authApi');
    api.setAccessToken('valid');
    expect((await api.fetchWithAuth('/api/v1/devices/settings')).status).toBe(403);
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(api.getAccessToken()).toBe('valid');
  });

  it('shares one refresh across concurrent requests and bootstrap', async () => {
    let finish!: (response: Response) => void;
    const pending = new Promise<Response>(resolve => { finish = resolve; });
    const fetch = vi.fn(async (url: string, options: RequestInit) => {
      if (url.endsWith('/refresh')) return pending;
      return reply(new Headers(options.headers).get('Authorization') === 'Bearer renewed' ? 200 : 401);
    });
    vi.stubGlobal('fetch', fetch);
    const api = await import('./authApi');
    api.setAccessToken('expired');
    const bootstrap = api.refreshRequest();
    const requests = Array.from({ length: 5 }, () => api.fetchWithAuth('/api/v1/devices/dashboard'));
    await Promise.resolve();
    finish(reply(200, authData));
    expect((await Promise.all(requests)).map(r => r.status)).toEqual([200, 200, 200, 200, 200]);
    await bootstrap;
    expect(fetch.mock.calls.filter(([url]) => url.endsWith('/refresh'))).toHaveLength(1);
  });

  it('does not restore an in-flight session after logout', async () => {
    let finish!: (response: Response) => void;
    vi.stubGlobal('fetch', vi.fn(() => new Promise<Response>(resolve => { finish = resolve; })));
    const api = await import('./authApi');
    api.setAccessToken('old');
    const pending = api.refreshRequest();
    api.clearAccessToken();
    finish(reply(200, authData));
    await expect(pending).rejects.toThrow('Authentication changed');
    expect(api.getAccessToken()).toBeNull();
  });

  it('allows bootstrap to retry after an outage instead of expiring the session', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValueOnce(reply(503)).mockResolvedValueOnce(reply(200, authData)));
    const api = await import('./authApi');
    const error = await api.refreshRequest().catch(error => error);
    expect(api.isSessionExpired(error)).toBe(false);
    expect((await api.refreshRequest()).accessToken).toBe('renewed');
  });
});
