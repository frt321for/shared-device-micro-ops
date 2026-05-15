import { useNavigate } from 'react-router-dom';

export default function NotFoundPage() {
  const navigate = useNavigate();

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      backgroundColor: '#0b1120',
      fontFamily: "'Inter',-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif",
    }}>
      <div style={{
        textAlign: 'center',
        padding: 48,
        backgroundColor: 'rgba(255,255,255,0.03)',
        borderRadius: 16,
        border: '1px solid rgba(255,255,255,0.06)',
      }}>
        <div style={{
          fontSize: 72,
          fontWeight: 700,
          color: '#533afd',
          lineHeight: 1,
          marginBottom: 16,
        }}>
          404
        </div>
        <h1 style={{
          fontSize: 20,
          fontWeight: 600,
          color: '#e2e6ed',
          margin: '0 0 8px',
        }}>
          页面不存在
        </h1>
        <p style={{
          fontSize: 14,
          color: '#5a6276',
          margin: '0 0 32px',
        }}>
          你访问的页面不存在或已被移除
        </p>
        <button
          onClick={() => navigate('/dashboard')}
          style={{
            padding: '10px 24px',
            fontSize: 14,
            fontWeight: 500,
            color: '#fff',
            backgroundColor: '#533afd',
            border: 'none',
            borderRadius: 8,
            cursor: 'pointer',
            transition: 'background-color 0.15s',
          }}
          onMouseEnter={(e) => { e.currentTarget.style.backgroundColor = '#6b52ff'; }}
          onMouseLeave={(e) => { e.currentTarget.style.backgroundColor = '#533afd'; }}
        >
          返回仪表盘
        </button>
      </div>
    </div>
  );
}
