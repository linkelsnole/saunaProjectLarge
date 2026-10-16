import type { AuthRequest, AuthResponse, AuthUser } from './authTypes';
import { platformFetch } from './platformUpdate';

// The access token stays in memory; each app has its own HttpOnly session cookie.
let accessToken: string | null = null;
let authVersion = 0;
let refreshPromise: Promise<AuthResponse> | null = null;

export const getAccessToken = (): string | null => accessToken;
export const setAccessToken = (token: string | null): void => {
  authVersion += 1;
  accessToken = token;
};
export const clearAccessToken = (): void => setAccessToken(null);

export class SessionExpiredError extends Error {
  constructor() {
    super('Session expired');
    this.name = 'SessionExpiredError';
  }
}

export const isSessionExpired = (error: unknown): boolean => error instanceof SessionExpiredError;

const isAuthEndpoint = (url: string) =>
  url.includes('/auth/login') || url.includes('/auth/refresh') || url.includes('/auth/logout');

function withAuthHeader(options: RequestInit, token: string | null): RequestInit {
  const headers = new Headers(options.headers);
  if (!headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
  if (token) headers.set('Authorization', `Bearer ${token}`);
  return { ...options, credentials: 'include', headers };
}

export async function loginRequest(payload: AuthRequest): Promise<AuthResponse> {
  const response = await fetch('/api/v1/auth/login', withAuthHeader({
    method: 'POST', body: JSON.stringify(payload),
  }, null));
  if (!response.ok) {
    const data = await response.json().catch(() => null);
    throw new Error(data?.message ?? data?.error ?? '');
  }
  const data: AuthResponse = await response.json();
  setAccessToken(data.accessToken);
  return data;
}

async function requestRefresh(): Promise<AuthResponse> {
  const version = authVersion;
  const response = await fetch('/api/v1/auth/refresh', withAuthHeader({ method: 'POST' }, null));
  if (version !== authVersion) throw new Error('Authentication changed during refresh');
  if (response.status === 401) throw new SessionExpiredError();
  if (!response.ok) throw new Error(`Session refresh unavailable (${response.status})`);
  const data: AuthResponse = await response.json();
  if (version !== authVersion) throw new Error('Authentication changed during refresh');
  if (typeof data?.accessToken !== 'string' || !data.accessToken) {
    throw new Error('Invalid session refresh response');
  }
  accessToken = data.accessToken;
  return data;
}

// Bootstrap and API requests share the same refresh within this tab.
export function refreshRequest(): Promise<AuthResponse> {
  if (!refreshPromise) {
    refreshPromise = requestRefresh().finally(() => { refreshPromise = null; });
  }
  return refreshPromise;
}

export async function fetchWithAuth(url: string, options: RequestInit = {}): Promise<Response> {
  const originalToken = getAccessToken();
  const version = authVersion;
  const response = await platformFetch(url, withAuthHeader(options, originalToken));
  // A 403 means insufficient permissions, not an expired login.
  if (response.status !== 401 || isAuthEndpoint(url)) return response;
  if (version !== authVersion) throw new Error('Authentication changed during request');

  // A slower request may have used the previous token while another refreshed it.
  if (accessToken && accessToken !== originalToken) {
    return platformFetch(url, withAuthHeader(options, accessToken));
  }
  try {
    const refreshed = await refreshRequest();
    return platformFetch(url, withAuthHeader(options, refreshed.accessToken));
  } catch (error) {
    if (isSessionExpired(error) && version === authVersion) {
      clearAccessToken();
      if (window.location.pathname !== '/login') window.location.href = '/login';
      return response;
    }
    // Keep the session on network/5xx failures; polling can retry after recovery.
    throw error;
  }
}

export async function getCurrentUser(): Promise<AuthUser> {
  const response = await fetchWithAuth('/api/v1/auth/me');
  if (!response.ok) throw new Error('Failed to get current user');
  return response.json();
}

export async function logoutRequest(): Promise<void> {
  await fetch('/api/v1/auth/logout', withAuthHeader({ method: 'POST' }, getAccessToken()));
}
