import type { AuthResponse, AuthUser } from './authTypes';

// Access-токен живёт только в памяти вкладки, в storage ничего не пишем
let accessToken: string | null = null;

export function getAccessToken(): string | null {
  return accessToken;
}

export function setAccessToken(token: string | null): void {
  accessToken = token;
}

export function clearAccessToken(): void {
  accessToken = null;
}

async function requestRefresh(): Promise<AuthResponse | null> {
  const response = await fetch('/api/v1/auth/refresh', {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
  });

  if (!response.ok) {
    return null;
  }

  const data: AuthResponse = await response.json();
  if (typeof data?.accessToken === 'string' && data.accessToken.length > 0) {
    setAccessToken(data.accessToken);
    return data;
  }
  return null;
}

let refreshPromise: Promise<AuthResponse | null> | null = null;

// Один параллельный refresh на все одновременные 401
export function refreshSession(): Promise<AuthResponse | null> {
  if (!refreshPromise) {
    refreshPromise = requestRefresh()
      .catch(() => null)
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

function buildHeaders(options: RequestInit, token: string | null): HeadersInit {
  return {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> | undefined),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
}

// Бэкенд отдаёт 401 на защищённых эндпоинтах и 403 на /api/v1/auth/** — оба означают протухший токен
const isAuthFailureStatus = (status: number) => status === 401 || status === 403;

const isAuthEndpoint = (url: string) =>
  url.includes('/auth/login') || url.includes('/auth/refresh') || url.includes('/auth/logout');

export async function fetchWithAuth(url: string, options: RequestInit = {}): Promise<Response> {
  const response = await fetch(url, {
    credentials: 'include',
    ...options,
    headers: buildHeaders(options, getAccessToken()),
  });

  if (!isAuthFailureStatus(response.status) || isAuthEndpoint(url)) {
    return response;
  }

  // Пробуем обновить access-токен по refresh-cookie и повторить запрос один раз
  const refreshed = await refreshSession();
  if (refreshed) {
    return fetch(url, {
      credentials: 'include',
      ...options,
      headers: buildHeaders(options, refreshed.accessToken),
    });
  }

  clearAccessToken();
  if (window.location.pathname !== '/login') {
    window.location.href = '/login';
  }
  return response;
}

export async function getCurrentUser(): Promise<AuthUser> {
  const response = await fetchWithAuth('/api/v1/auth/me');
  if (!response.ok) {
    throw new Error('Failed to get current user');
  }
  return response.json();
}

export async function logoutRequest(): Promise<void> {
  await fetch('/api/v1/auth/logout', {
    method: 'POST',
    credentials: 'include',
    headers: buildHeaders({}, getAccessToken()),
  });
}
