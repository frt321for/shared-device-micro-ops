import { useLocation } from 'react-router-dom';

/* ---------- Path Labels ---------- */

const PATH_LABELS: Record<string, string> = {
  dashboard: '仪表盘',
  sites: '站点管理',
  devices: '设备管理',
  'work-orders': '工单工作台',
  inventory: '库存管理',
  routes: '路线视图',
  revenue: '收益分析',
  'ai-report': 'AI周报',
};

/* ---------- SVG Icons ---------- */

const SearchIcon = (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#8b93a5" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <circle cx="11" cy="11" r="8" />
    <line x1="21" y1="21" x2="16.65" y2="16.65" />
  </svg>
);

const BellIcon = (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
    <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
    <path d="M13.73 21a2 2 0 0 1-3.46 0" />
  </svg>
);

/* ---------- Component ---------- */

export default function Topbar() {
  const location = useLocation();
  const segments = location.pathname.split('/').filter(Boolean);

  return (
    <header style={{
      position: 'sticky',
      top: 0,
      zIndex: 50,
      height: 56,
      backgroundColor: '#ffffff',
      borderBottom: '1px solid #e2e8f0',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '0 24px',
      fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
    }}>
      {/* Left: Breadcrumb */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 14 }}>
        {segments.length === 0 ? (
          <span style={{ color: '#0f172a', fontWeight: 500 }}>仪表盘</span>
        ) : (
          segments.map((segment, index) => {
            const label = PATH_LABELS[segment] ?? segment;
            const isLast = index === segments.length - 1;
            return (
              <div key={segment} style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                {index > 0 && (
                  <span style={{ color: '#94a3b8', fontSize: 12 }}>/</span>
                )}
                <span style={{
                  color: isLast ? '#0f172a' : '#64748b',
                  fontWeight: isLast ? 500 : 400,
                }}>
                  {label}
                </span>
              </div>
            );
          })
        )}
      </div>

      {/* Right: Search, Notifications, Avatar */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
        {/* Search */}
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: 8,
          backgroundColor: '#f0f2f5',
          borderRadius: 8,
          padding: '0 12px',
          height: 36,
          width: 220,
        }}>
          {SearchIcon}
          <input
            type="text"
            placeholder="搜索..."
            style={{
              border: 'none',
              background: 'transparent',
              outline: 'none',
              fontSize: 14,
              color: '#0f172a',
              width: '100%',
              fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
            }}
          />
        </div>

        {/* Notifications */}
        <button style={{
          position: 'relative',
          width: 36,
          height: 36,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          border: 'none',
          backgroundColor: 'transparent',
          borderRadius: 8,
          cursor: 'pointer',
          color: '#64748b',
          transition: 'all 0.15s ease',
        }}
          onMouseEnter={(e) => { e.currentTarget.style.backgroundColor = '#f0f2f5'; e.currentTarget.style.color = '#0f172a'; }}
          onMouseLeave={(e) => { e.currentTarget.style.backgroundColor = 'transparent'; e.currentTarget.style.color = '#64748b'; }}
        >
          {BellIcon}
          <span style={{
            position: 'absolute',
            top: 6,
            right: 6,
            width: 8,
            height: 8,
            borderRadius: '50%',
            backgroundColor: '#ef4444',
          }} />
        </button>

        {/* Avatar */}
        <div style={{
          width: 32,
          height: 32,
          borderRadius: 8,
          backgroundColor: '#533afd',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: '#ffffff',
          fontSize: 13,
          fontWeight: 600,
          cursor: 'pointer',
        }}>
          张
        </div>
      </div>
    </header>
  );
}
