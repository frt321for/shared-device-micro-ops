import { createContext, useContext, useState, useCallback, useEffect, type ReactNode } from 'react';
import { api, AUTH_EXPIRED } from '../api/client';

interface IUserInfo {
  username: string;
  displayName: string;
  role: string;
}

interface AuthContextValue {
  token: string | null;
  user: IUserInfo | null;
  isAuthenticated: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
}

const USER_INFO_KEY = 'auth_user';

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('auth_token'));
  const [user, setUser] = useState<IUserInfo | null>(() => {
    try {
      const saved = localStorage.getItem(USER_INFO_KEY);
      return saved ? JSON.parse(saved) : null;
    } catch { return null; }
  });

  useEffect(() => {
    const handler = () => {
      setToken(null);
      setUser(null);
      localStorage.removeItem('auth_token');
      localStorage.removeItem(USER_INFO_KEY);
    };
    window.addEventListener(AUTH_EXPIRED, handler);
    return () => window.removeEventListener(AUTH_EXPIRED, handler);
  }, []);

  const login = useCallback(async (username: string, password: string) => {
    const res = await api.post<{ token: string; user?: IUserInfo }>('/auth/login', {
      username,
      password,
    });

    const { token: newToken, user: userInfo } = res.data;
    localStorage.setItem('auth_token', newToken);
    setToken(newToken);
    if (userInfo) {
      localStorage.setItem(USER_INFO_KEY, JSON.stringify(userInfo));
      setUser(userInfo);
    }
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('auth_token');
    localStorage.removeItem(USER_INFO_KEY);
    setToken(null);
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider
      value={{
        token,
        user,
        isAuthenticated: token !== null,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return ctx;
}
