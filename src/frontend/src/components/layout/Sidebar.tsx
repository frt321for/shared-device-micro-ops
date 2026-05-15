import { NavLink } from 'react-router-dom';

interface SidebarProps {
  collapsed: boolean;
  onToggle: () => void;
}

interface NavItemData {
  label: string;
  path: string;
  icon: React.ReactNode;
}

interface NavSectionData {
  label: string;
  items: NavItemData[];
}

const IconDashboard = (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
    <rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" />
    <rect x="3" y="14" width="7" height="7" rx="1" /><rect x="14" y="14" width="7" height="7" rx="1" />
  </svg>
);
const IconSites = (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
    <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z" /><circle cx="12" cy="10" r="3" />
  </svg>
);
const IconDevices = (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
    <rect x="2" y="3" width="20" height="14" rx="2" /><line x1="8" y1="21" x2="16" y2="21" /><line x1="12" y1="17" x2="12" y2="21" />
  </svg>
);
const IconWorkOrders = (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
    <path d="M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2" />
    <rect x="8" y="2" width="8" height="4" rx="1" /><line x1="9" y1="12" x2="15" y2="12" /><line x1="9" y1="16" x2="13" y2="16" />
  </svg>
);
const IconInventory = (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
    <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" />
    <polyline points="3.27 6.96 12 12.01 20.73 6.96" /><line x1="12" y1="22.08" x2="12" y2="12" />
  </svg>
);
const IconRoutes = (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
    <circle cx="6" cy="18" r="2" /><circle cx="18" cy="6" r="2" /><line x1="7.5" y1="16.5" x2="16.5" y2="7.5" />
  </svg>
);
const IconRevenue = (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
    <line x1="4" y1="21" x2="20" y2="21" /><polyline points="4 17 8 13 12 17 20 7" /><polyline points="15 7 20 7 20 12" />
  </svg>
);
const IconAiReport = (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
    <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
  </svg>
);

const ChevronLeft = (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="15 18 9 12 15 6" /></svg>
);
const ChevronRight = (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="9 18 15 12 9 6" /></svg>
);

const NAV_SECTIONS: NavSectionData[] = [
  { label: 'Overview', items: [
    { label: '仪表盘', path: '/dashboard', icon: IconDashboard },
    { label: '站点管理', path: '/sites', icon: IconSites },
    { label: '设备管理', path: '/devices', icon: IconDevices },
  ]},
  { label: 'Operations', items: [
    { label: '工单工作台', path: '/work-orders', icon: IconWorkOrders },
    { label: '库存管理', path: '/inventory', icon: IconInventory },
    { label: '路线视图', path: '/routes', icon: IconRoutes },
  ]},
  { label: 'Analytics', items: [
    { label: '收益分析', path: '/revenue', icon: IconRevenue },
    { label: 'AI周报', path: '/ai-report', icon: IconAiReport },
  ]},
];

export default function Sidebar({ collapsed, onToggle }: SidebarProps) {
  return (
    <nav style={{
      position: 'fixed', top: 0, left: 0, height: '100vh', zIndex: 100,
      width: collapsed ? 60 : 240,
      backgroundColor: '#0b1120', color: '#fff',
      display: 'flex', flexDirection: 'column',
      transition: 'width 0.2s cubic-bezier(0.4,0,0.2,1)',
      borderRight: '1px solid rgba(255,255,255,0.06)',
      fontFamily: "'Inter',-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif",
    }}>
      <div style={{
        display: 'flex', alignItems: 'center', justifyContent: collapsed ? 'center' : 'flex-start', gap: 10,
        height: 56, padding: collapsed ? 0 : '0 16px', flexShrink: 0,
        borderBottom: '1px solid rgba(255,255,255,0.06)',
      }}>
        <div style={{ width: 28, height: 28, backgroundColor: '#533afd', borderRadius: 6, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"><polyline points="20 6 9 17 4 12" /></svg>
        </div>
        {!collapsed && <span style={{ fontSize: 16, fontWeight: 600, color: '#fff', whiteSpace: 'nowrap' }}>IoT Ops</span>}
      </div>

      <div style={{ flex: 1, overflowY: 'auto', padding: collapsed ? '8px 0' : '8px 0' }}>
        {NAV_SECTIONS.map((section) => (
          <div key={section.label}>
            {!collapsed && (
              <div style={{ padding: '10px 16px 4px', fontSize: 11, fontWeight: 600, color: '#5a6276', textTransform: 'uppercase', letterSpacing: '0.08em', whiteSpace: 'nowrap' }}>
                {section.label}
              </div>
            )}
            {section.items.map((item) => (
              <NavLink
                key={item.path}
                to={item.path}
                style={({ isActive }) => ({
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: collapsed ? 'center' : 'flex-start',
                  gap: collapsed ? 0 : 12,
                  height: 40,
                  margin: collapsed ? '2px 10px' : '0 8px',
                  padding: collapsed ? 0 : '0 12px',
                  borderRadius: collapsed ? 8 : 6,
                  textDecoration: 'none',
                  fontSize: 14,
                  fontWeight: isActive ? 500 : 400,
                  color: isActive ? '#fff' : '#8b93a5',
                  backgroundColor: isActive ? 'rgba(83,58,253,0.15)' : 'transparent',
                  transition: 'all 0.12s',
                  cursor: 'pointer',
                })}
                onMouseEnter={(e) => { if (!e.currentTarget.classList.contains('active')) e.currentTarget.style.backgroundColor = 'rgba(255,255,255,0.04)'; }}
                onMouseLeave={(e) => { if (!e.currentTarget.classList.contains('active')) e.currentTarget.style.backgroundColor = 'transparent'; }}
              >
                <span style={{ width: 20, height: 20, flexShrink: 0, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  {item.icon}
                </span>
                {!collapsed && <span style={{ whiteSpace: 'nowrap' }}>{item.label}</span>}
              </NavLink>
            ))}
          </div>
        ))}
      </div>

      <div style={{
        display: 'flex', alignItems: 'center', justifyContent: collapsed ? 'center' : 'flex-start', gap: collapsed ? 0 : 10,
        padding: collapsed ? '12px 0' : '12px 16px', flexShrink: 0,
        borderTop: '1px solid rgba(255,255,255,0.06)',
      }}>
        <div style={{ width: 32, height: 32, borderRadius: 8, backgroundColor: '#533afd', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#fff', fontSize: 13, fontWeight: 600, flexShrink: 0 }}>
          张
        </div>
        {!collapsed && (
          <div style={{ overflow: 'hidden' }}>
            <div style={{ fontSize: 13, fontWeight: 500, color: '#e2e6ed', whiteSpace: 'nowrap' }}>张三</div>
            <div style={{ fontSize: 11, color: '#5a6276', whiteSpace: 'nowrap' }}>运维工程师</div>
          </div>
        )}
      </div>

      <button onClick={onToggle} style={{
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        height: 40, cursor: 'pointer', color: '#5a6276',
        borderTop: '1px solid rgba(255,255,255,0.06)',
        backgroundColor: 'transparent', border: 'none', width: '100%', flexShrink: 0,
      }}
      title={collapsed ? '展开' : '收起'}
      onMouseEnter={(e) => { e.currentTarget.style.color = '#e2e6ed'; }}
      onMouseLeave={(e) => { e.currentTarget.style.color = '#5a6276'; }}>
        {collapsed ? ChevronRight : ChevronLeft}
      </button>
    </nav>
  );
}
