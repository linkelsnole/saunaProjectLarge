import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import i18n from '@/shared/config/i18n/i18n';
import { clearAccessToken, loginRequest, logoutRequest, refreshRequest, isSessionExpired } from '@/api/authApi';
import type { AuthRequest, AuthUser } from '@/api/authTypes';

interface AuthContextType {
  user: AuthUser | null;
  username: string | null;
  role: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (credentials: AuthRequest) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const login = useCallback(async (credentials: AuthRequest) => {
    try {
      const response = await loginRequest(credentials);
      setUser(response.user);
    } catch (error) {
      // TypeError у fetch — сеть недоступна, сервер не ответил
      if (error instanceof TypeError) {
        throw new Error(i18n.t('login.serverUnavailable'));
      }
      const message = error instanceof Error ? error.message : '';
      throw new Error(message || i18n.t('login.error'));
    }
  }, []);

  const logout = useCallback(async () => {
    // Запрос уходит с ещё живым access-токеном, состояние чистим сразу
    const request = logoutRequest().catch(() => undefined);
    clearAccessToken();
    setUser(null);
    await request;
  }, []);

  // A temporary outage must not turn a valid cookie into a forced logout.
  useEffect(() => {
    let cancelled = false;
    let retryTimer: ReturnType<typeof setTimeout> | undefined;
    const bootstrap = async () => {
      try {
        const refreshed = await refreshRequest();
        if (!cancelled) {
          setUser(refreshed.user);
          setIsLoading(false);
        }
      } catch (error) {
        if (cancelled) return;
        if (isSessionExpired(error)) {
          clearAccessToken();
          setUser(null);
          setIsLoading(false);
        } else {
          retryTimer = setTimeout(() => { void bootstrap(); }, 5000);
        }
      }
    };
    void bootstrap();
    return () => {
      cancelled = true;
      clearTimeout(retryTimer);
    };
  }, []);

  const value = useMemo<AuthContextType>(() => ({
    user,
    username: user?.username ?? null,
    role: user?.role ?? null,
    isAuthenticated: Boolean(user),
    isLoading,
    login,
    logout,
  }), [user, isLoading, login, logout]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
