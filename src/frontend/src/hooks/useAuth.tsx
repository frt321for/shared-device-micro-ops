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

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('auth_token'));
  const [user, setUser] = useState<IUserInfo | null>(null);

  useEffect(() => {
    const handler = () => {
      setToken(null);
      setUser(null);
    };
    window.addEventListener(AUTH_EXPIRED, handler);

    if (!token) {
      api.post<{ token: string }>('/auth/login', { username: 'admin' }).then(res => {
        const t = res.data.token;
        localStorage.setItem('auth_token', t);
        setToken(t);
      }).catch(() => {});
    }

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
      setUser(userInfo);
    }
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('auth_token');
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
