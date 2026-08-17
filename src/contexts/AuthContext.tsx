import { createContext, useContext, useState, type ReactNode } from 'react';
import i18n from '@/shared/config/i18n/i18n';
import type { AuthState, AuthRequest, AuthResponse } from '@/api/authTypes';

interface AuthContextType extends AuthState {
  login: (credentials: AuthRequest) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const TOKEN_KEY = 'auth_token';
const USERNAME_KEY = 'auth_username';
const ROLE_KEY = 'auth_role';

function isTokenExpired(token: string): boolean {
  try {
    const payloadBase64 = token.split('.')[1];
    const decodedJson = atob(payloadBase64);
    const decoded = JSON.parse(decodedJson) as { exp: number };
    const exp = decoded.exp * 1000;
    return Date.now() >= exp;
  } catch (e) {
    return true;
  }
}

function getInitialState(): AuthState {
  const token = localStorage.getItem(TOKEN_KEY);
  const username = localStorage.getItem(USERNAME_KEY);
  const role = localStorage.getItem(ROLE_KEY);
  
  if (token && !isTokenExpired(token)) {
    return {
      token,
      username,
      role,
      isAuthenticated: true,
    };
  } else {
    if (token) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USERNAME_KEY);
      localStorage.removeItem(ROLE_KEY);
    }
    return {
      token: null,
      username: null,
      role: null,
      isAuthenticated: false,
    };
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [authState, setAuthState] = useState<AuthState>(getInitialState);

  const login = async (credentials: AuthRequest) => {
    let response: Response;
    
    try {
      response = await fetch('/api/v1/auth/login', {
        method: 'POST',
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
        message: i18n.t('login.error')
      }));
      throw new Error(errorData.message || i18n.t('login.error'));
    }

    const data: AuthResponse = await response.json();
    
    localStorage.setItem(TOKEN_KEY, data.token);
    localStorage.setItem(USERNAME_KEY, data.username);
    localStorage.setItem(ROLE_KEY, data.role);

    setAuthState({
      token: data.token,
      username: data.username,
      role: data.role,
      isAuthenticated: true,
    });
  };

  const logout = () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USERNAME_KEY);
    localStorage.removeItem(ROLE_KEY);
    
    setAuthState({
      token: null,
      username: null,
      role: null,
      isAuthenticated: false,
    });
  };

  return (
    <AuthContext.Provider
      value={{
        ...authState,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

