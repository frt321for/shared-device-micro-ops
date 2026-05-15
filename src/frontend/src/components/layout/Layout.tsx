import { useState, useCallback } from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import Topbar from './Topbar';

const STORAGE_KEY = 'sidebar-collapsed';
const SIDEBAR_WIDTH = { expanded: 240, collapsed: 60 };

export default function Layout() {
  const [collapsed, setCollapsed] = useState(() => {
    const saved = localStorage.getItem(STORAGE_KEY);
    return saved === 'true';
  });

  const handleToggle = useCallback(() => {
    setCollapsed((prev) => {
      const next = !prev;
      localStorage.setItem(STORAGE_KEY, String(next));
      return next;
    });
  }, []);

  const sidebarWidth = collapsed ? SIDEBAR_WIDTH.collapsed : SIDEBAR_WIDTH.expanded;

  return (
    <div style={{
      minHeight: '100vh',
      backgroundColor: '#f6f8fa',
      fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
    }}>
      <Sidebar collapsed={collapsed} onToggle={handleToggle} />

      <div style={{
        marginLeft: sidebarWidth,
        transition: 'margin-left 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column',
      }}>
        <Topbar />

        <main style={{
          flex: 1,
          padding: 32,
        }}>
          <Outlet />
        </main>
      </div>
    </div>
  );
}
