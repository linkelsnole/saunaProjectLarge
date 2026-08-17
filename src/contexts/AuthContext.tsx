import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import i18n from '@/shared/config/i18n/i18n';
import type { AuthRequest, AuthResponse, AuthUser } from '@/api/authTypes';
import { clearAccessToken, logoutRequest, refreshSession, setAccessToken } from '@/api/authApi';

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
  const bootstrapStartedRef = useRef(false);

  const clearAuthState = useCallback(() => {
    clearAccessToken();
    setUser(null);
  }, []);

  const login = useCallback(async (credentials: AuthRequest) => {
    let response: Response;

    try {
      response = await fetch('/api/v1/auth/login', {
        method: 'POST',
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(credentials),
      });
    } catch (error) {
      // Ошибка сети - сервер недоступен
      throw new Error(i18n.t('login.serverUnavailable'));
    }

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({
        error: 'UNKNOWN_ERROR',
        message: i18n.t('login.error'),
      }));
      throw new Error(errorData.message || i18n.t('login.error'));
    }

    const data: AuthResponse = await response.json();
    setAccessToken(data.accessToken);
    setUser(data.user);
  }, []);

  const logout = useCallback(async () => {
    try {
      await logoutRequest();
    } finally {
      clearAuthState();
    }
  }, [clearAuthState]);

  // Bootstrap: пробуем восстановить сессию по refresh-cookie до показа формы логина
  useEffect(() => {
    if (bootstrapStartedRef.current) {
      return;
    }
    bootstrapStartedRef.current = true;

    const bootstrap = async () => {
      try {
        const refreshed = await refreshSession();
        if (refreshed) {
          setUser(refreshed.user);
        } else {
          clearAuthState();
        }
      } catch (_error) {
        clearAuthState();
      } finally {
        setIsLoading(false);
      }
    };

    void bootstrap();
  }, [clearAuthState]);

  const value = useMemo<AuthContextType>(
    () => ({
      user,
      username: user?.username ?? null,
      role: user?.role ?? null,
      isAuthenticated: Boolean(user),
      isLoading,
      login,
      logout,
    }),
    [isLoading, login, logout, user]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
