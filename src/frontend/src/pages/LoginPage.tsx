import { useState, type FormEvent } from 'react';
import { useAuth } from '../hooks/useAuth';

export default function LoginPage() {
  const { login } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!username.trim() || !password.trim()) {
      setError('请输入用户名和密码');
      return;
    }
    setError('');
    setLoading(true);
    try {
      await login(username, password);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : '登录失败';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: '#0f1117',
    }}>
      <form
        onSubmit={handleSubmit}
        style={{
          width: 380,
          padding: '40px 36px',
          background: '#1a1d27',
          borderRadius: 12,
          boxShadow: '0 4px 24px rgba(0,0,0,.4)',
        }}
      >
        <h1 style={{
          fontSize: 24,
          fontWeight: 700,
          color: '#fff',
          marginBottom: 4,
          textAlign: 'center',
        }}>
          运维管理平台
        </h1>
        <p style={{
          fontSize: 13,
          color: '#888',
          textAlign: 'center',
          marginBottom: 28,
        }}>
          请登录以继续
        </p>

        {error && (
          <div style={{
            padding: '10px 14px',
            background: 'rgba(239,68,68,.15)',
            border: '1px solid rgba(239,68,68,.3)',
            borderRadius: 8,
            color: '#ef4444',
            fontSize: 13,
            marginBottom: 18,
          }}>
            {error}
          </div>
        )}

        <div style={{ marginBottom: 16 }}>
          <label style={{
            display: 'block',
            fontSize: 13,
            color: '#aaa',
            marginBottom: 6,
          }}>
            用户名
          </label>
          <input
            value={username}
            onChange={e => setUsername(e.target.value)}
            placeholder="请输入用户名"
            style={{
              width: '100%',
              padding: '10px 14px',
              background: '#262a36',
              border: '1px solid #333',
              borderRadius: 8,
              color: '#fff',
              fontSize: 14,
              outline: 'none',
            }}
            onFocus={e => { e.target.style.borderColor = '#7c5cfc'; }}
            onBlur={e => { e.target.style.borderColor = '#333'; }}
          />
        </div>

        <div style={{ marginBottom: 24 }}>
          <label style={{
            display: 'block',
            fontSize: 13,
            color: '#aaa',
            marginBottom: 6,
          }}>
            密码
          </label>
          <input
            type="password"
            value={password}
            onChange={e => setPassword(e.target.value)}
            placeholder="请输入密码"
            style={{
              width: '100%',
              padding: '10px 14px',
              background: '#262a36',
              border: '1px solid #333',
              borderRadius: 8,
              color: '#fff',
              fontSize: 14,
              outline: 'none',
            }}
            onFocus={e => { e.target.style.borderColor = '#7c5cfc'; }}
            onBlur={e => { e.target.style.borderColor = '#333'; }}
          />
        </div>

        <button
          type="submit"
          disabled={loading}
          style={{
            width: '100%',
            padding: '11px 0',
            background: loading ? '#5a4aa0' : '#7c5cfc',
            border: 'none',
            borderRadius: 8,
            color: '#fff',
            fontSize: 15,
            fontWeight: 600,
            cursor: loading ? 'not-allowed' : 'pointer',
            transition: 'background .2s',
          }}
          onMouseEnter={e => { if (!loading) e.currentTarget.style.background = '#6b4de0'; }}
          onMouseLeave={e => { if (!loading) e.currentTarget.style.background = '#7c5cfc'; }}
        >
          {loading ? '登录中...' : '登 录'}
        </button>
      </form>
    </div>
  );
}
