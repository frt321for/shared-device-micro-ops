import { useState, useRef, useEffect, useCallback } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';

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
  search: '搜索',
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

const ChevronDownIcon = (
  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <polyline points="6 9 12 15 18 9" />
  </svg>
);

const LogoutIcon = (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
    <polyline points="16 17 21 12 16 7" />
    <line x1="21" y1="12" x2="9" y2="12" />
  </svg>
);

const UserIcon = (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
    <circle cx="12" cy="7" r="4" />
  </svg>
);

const CloseIcon = (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <line x1="18" y1="6" x2="6" y2="18" /><line x1="6" y1="6" x2="18" y2="18" />
  </svg>
);

/* ---------- Component ---------- */

export default function Topbar() {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const segments = location.pathname.split('/').filter(Boolean);

  const [searchQuery, setSearchQuery] = useState('');
  const [showUserMenu, setShowUserMenu] = useState(false);
  const [showNotifications, setShowNotifications] = useState(false);

  const userMenuRef = useRef<HTMLDivElement>(null);
  const notifRef = useRef<HTMLDivElement>(null);

  const displayName = user?.displayName || user?.username || '用户';
  const avatarInitial = displayName.charAt(0);
  const roleMap: Record<string, string> = { admin: '管理员', operator: '运维工程师', engineer: '工程师', viewer: '观察员' };
  const roleLabel = user?.role ? (roleMap[user.role] || user.role) : '用户';

  // Close dropdowns on outside click
  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (userMenuRef.current && !userMenuRef.current.contains(e.target as Node)) {
        setShowUserMenu(false);
      }
      if (notifRef.current && !notifRef.current.contains(e.target as Node)) {
        setShowNotifications(false);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const handleSearch = useCallback((e: React.FormEvent) => {
    e.preventDefault();
    const q = searchQuery.trim();
    if (q) {
      navigate(`/search?q=${encodeURIComponent(q)}`);
      setSearchQuery('');
    }
  }, [searchQuery, navigate]);

  const handleLogout = useCallback(() => {
    logout();
    navigate('/login');
  }, [logout, navigate]);

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

      {/* Right: Search, Notifications, User Menu */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
        {/* Search */}
        <form onSubmit={handleSearch} style={{
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
            placeholder="搜索站点、设备、工单..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
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
        </form>

        {/* Notifications */}
        <div ref={notifRef} style={{ position: 'relative' }}>
          <button
            onClick={() => { setShowNotifications((p) => !p); setShowUserMenu(false); }}
            style={{
              position: 'relative',
              width: 36,
              height: 36,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              border: 'none',
              backgroundColor: showNotifications ? '#f0f2f5' : 'transparent',
              borderRadius: 8,
              cursor: 'pointer',
              color: showNotifications ? '#0f172a' : '#64748b',
              transition: 'all 0.15s ease',
            }}
            onMouseEnter={(e) => { e.currentTarget.style.backgroundColor = '#f0f2f5'; e.currentTarget.style.color = '#0f172a'; }}
            onMouseLeave={(e) => { if (!showNotifications) { e.currentTarget.style.backgroundColor = 'transparent'; e.currentTarget.style.color = '#64748b'; } }}
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

          {/* Notification Dropdown */}
          {showNotifications && (
            <div style={{
              position: 'absolute',
              top: 'calc(100% + 8px)',
              right: 0,
              width: 320,
              backgroundColor: '#fff',
              borderRadius: 12,
              boxShadow: '0 4px 24px rgba(0,0,0,0.12), 0 0 0 1px rgba(0,0,0,0.04)',
              overflow: 'hidden',
              zIndex: 100,
            }}>
              <div style={{ padding: '14px 16px', borderBottom: '1px solid #f3f4f6', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <span style={{ fontSize: 14, fontWeight: 600, color: '#0f172a' }}>通知</span>
                <span style={{ fontSize: 12, color: '#6b7280' }}>0 条未读</span>
              </div>
              <div style={{ padding: '32px 16px', textAlign: 'center', color: '#9ca3af', fontSize: 13 }}>
                暂无新通知
              </div>
            </div>
          )}
        </div>

        {/* User Menu */}
        <div ref={userMenuRef} style={{ position: 'relative' }}>
          <div
            onClick={() => { setShowUserMenu((p) => !p); setShowNotifications(false); }}
            style={{
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
              transition: 'opacity 0.15s',
            }}
            onMouseEnter={(e) => { e.currentTarget.style.opacity = '0.85'; }}
            onMouseLeave={(e) => { e.currentTarget.style.opacity = '1'; }}
          >
            {avatarInitial}
          </div>

          {/* User Dropdown */}
          {showUserMenu && (
            <div style={{
              position: 'absolute',
              top: 'calc(100% + 8px)',
              right: 0,
              width: 200,
              backgroundColor: '#fff',
              borderRadius: 12,
              boxShadow: '0 4px 24px rgba(0,0,0,0.12), 0 0 0 1px rgba(0,0,0,0.04)',
              overflow: 'hidden',
              zIndex: 100,
            }}>
              {/* User Info */}
              <div style={{ padding: '14px 16px', borderBottom: '1px solid #f3f4f6' }}>
                <div style={{ fontSize: 14, fontWeight: 600, color: '#0f172a' }}>{displayName}</div>
                <div style={{ fontSize: 12, color: '#6b7280', marginTop: 2 }}>{roleLabel}</div>
              </div>

              {/* Menu Items */}
              <div style={{ padding: '4px' }}>
                <button
                  onClick={() => { setShowUserMenu(false); navigate('/dashboard'); }}
                  style={{
                    display: 'flex', alignItems: 'center', gap: 10, width: '100%',
                    padding: '8px 12px', border: 'none', backgroundColor: 'transparent',
                    borderRadius: 6, cursor: 'pointer', fontSize: 13, color: '#374151',
                    textAlign: 'left',
                  }}
                  onMouseEnter={(e) => { e.currentTarget.style.backgroundColor = '#f9fafb'; }}
                  onMouseLeave={(e) => { e.currentTarget.style.backgroundColor = 'transparent'; }}
                >
                  {UserIcon} 个人信息
                </button>
                <button
                  onClick={handleLogout}
                  style={{
                    display: 'flex', alignItems: 'center', gap: 10, width: '100%',
                    padding: '8px 12px', border: 'none', backgroundColor: 'transparent',
                    borderRadius: 6, cursor: 'pointer', fontSize: 13, color: '#dc2626',
                    textAlign: 'left',
                  }}
                  onMouseEnter={(e) => { e.currentTarget.style.backgroundColor = '#fef2f2'; }}
                  onMouseLeave={(e) => { e.currentTarget.style.backgroundColor = 'transparent'; }}
                >
                  {LogoutIcon} 退出登录
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
