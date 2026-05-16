import { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { fetchSites, fetchDevices, fetchWorkOrders } from '../api/endpoints';
import type { ISite, IDevice, IWorkOrder } from '../api/endpoints';

const tabStyle = (active: boolean) => ({
  padding: '8px 16px',
  fontSize: 13,
  fontWeight: active ? 600 : 400,
  color: active ? '#533afd' : '#6b7280',
  borderBottom: active ? '2px solid #533afd' : '2px solid transparent',
  cursor: 'pointer',
  background: 'none',
  border: 'none',
  transition: 'all 0.15s',
});

const statusColor: Record<string, string> = {
  online: '#059669', offline: '#6b7280', fault: '#dc2626', low_stock: '#d97706',
  out_of_stock: '#dc2626', maintenance: '#2563eb', retired: '#9ca3af',
};

export default function SearchPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const query = searchParams.get('q') || '';

  const [sites, setSites] = useState<ISite[]>([]);
  const [devices, setDevices] = useState<IDevice[]>([]);
  const [orders, setOrders] = useState<IWorkOrder[]>([]);
  const [loading, setLoading] = useState(false);
  const [activeTab, setActiveTab] = useState<'sites' | 'devices' | 'orders'>('sites');

  useEffect(() => {
    if (!query) return;
    setLoading(true);
    Promise.all([
      fetchSites({ search: query, size: 20 }).then(r => setSites(r.data.content)).catch(() => setSites([])),
      fetchDevices({ size: 50 }).then(r => {
        const all = r.data.content;
        const q = query.toLowerCase();
        setDevices(all.filter(d =>
          d.name.toLowerCase().includes(q) || d.deviceCode.toLowerCase().includes(q)
        ));
      }).catch(() => setDevices([])),
      fetchWorkOrders({ size: 50 }).then(r => {
        const all = r.data.content;
        const q = query.toLowerCase();
        setOrders(all.filter(o =>
          o.title.toLowerCase().includes(q) || o.orderNo.toLowerCase().includes(q)
        ));
      }).catch(() => setOrders([])),
    ]).finally(() => setLoading(false));
  }, [query]);

  const totalResults = sites.length + devices.length + orders.length;

  const statusLabel: Record<string, string> = {
    pending_assign: '待派单', assigned: '已派单', arrived: '已到场',
    processing: '处理中', pending_review: '待复核', closed: '已关闭',
  };

  return (
    <div style={{ maxWidth: 960, margin: '0 auto', fontFamily: 'Inter, system-ui, sans-serif' }}>
      <h1 style={{ fontSize: 22, fontWeight: 600, color: '#08060d', marginBottom: 4 }}>
        搜索结果
      </h1>
      <p style={{ fontSize: 13, color: '#6b7280', marginBottom: 24 }}>
        关键词: <strong>"{query}"</strong>
        {!loading && ` — 找到 ${totalResults} 条结果`}
      </p>

      {/* Tabs */}
      <div style={{ display: 'flex', gap: 0, borderBottom: '1px solid #e5e7eb', marginBottom: 24 }}>
        <button style={tabStyle(activeTab === 'sites')} onClick={() => setActiveTab('sites')}>
          站点 ({sites.length})
        </button>
        <button style={tabStyle(activeTab === 'devices')} onClick={() => setActiveTab('devices')}>
          设备 ({devices.length})
        </button>
        <button style={tabStyle(activeTab === 'orders')} onClick={() => setActiveTab('orders')}>
          工单 ({orders.length})
        </button>
      </div>

      {loading && (
        <div style={{ textAlign: 'center', padding: '48px 0', color: '#6b7280', fontSize: 14 }}>搜索中...</div>
      )}

      {!loading && totalResults === 0 && (
        <div style={{ textAlign: 'center', padding: '48px 0', color: '#9ca3af', fontSize: 14 }}>
          未找到与 "{query}" 相关的结果
        </div>
      )}

      {/* Sites */}
      {!loading && activeTab === 'sites' && sites.length > 0 && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {sites.map(site => (
            <div
              key={site.id}
              onClick={() => navigate(`/sites/${site.id}`)}
              style={{
                display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                padding: '14px 18px', background: '#fff', borderRadius: 10, border: '1px solid #e5e7eb',
                cursor: 'pointer', transition: 'border-color 0.15s',
              }}
              onMouseEnter={(e) => { (e.currentTarget as HTMLDivElement).style.borderColor = '#533afd'; }}
              onMouseLeave={(e) => { (e.currentTarget as HTMLDivElement).style.borderColor = '#e5e7eb'; }}
            >
              <div>
                <div style={{ fontSize: 14, fontWeight: 500, color: '#08060d' }}>{site.name}</div>
                <div style={{ fontSize: 12, color: '#6b7280', marginTop: 2 }}>{site.address || '暂无地址'}</div>
              </div>
              <span style={{
                fontSize: 12, fontWeight: 500, padding: '2px 10px', borderRadius: 9999,
                color: site.status === 'active' ? '#059669' : '#6b7280',
                background: site.status === 'active' ? '#ecfdf5' : '#f3f4f6',
              }}>
                {site.status === 'active' ? '运营中' : site.status}
              </span>
            </div>
          ))}
        </div>
      )}

      {/* Devices */}
      {!loading && activeTab === 'devices' && devices.length > 0 && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {devices.map(device => (
            <div
              key={device.id}
              onClick={() => navigate(`/devices/${device.id}`)}
              style={{
                display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                padding: '14px 18px', background: '#fff', borderRadius: 10, border: '1px solid #e5e7eb',
                cursor: 'pointer', transition: 'border-color 0.15s',
              }}
              onMouseEnter={(e) => { (e.currentTarget as HTMLDivElement).style.borderColor = '#533afd'; }}
              onMouseLeave={(e) => { (e.currentTarget as HTMLDivElement).style.borderColor = '#e5e7eb'; }}
            >
              <div>
                <div style={{ fontSize: 14, fontWeight: 500, color: '#08060d' }}>{device.name}</div>
                <div style={{ fontSize: 12, color: '#6b7280', marginTop: 2 }}>{device.deviceCode}</div>
              </div>
              <span style={{
                fontSize: 12, fontWeight: 500, padding: '2px 10px', borderRadius: 9999,
                color: statusColor[device.status] || '#6b7280',
                background: `${statusColor[device.status] || '#6b7280'}15`,
              }}>
                {device.status}
              </span>
            </div>
          ))}
        </div>
      )}

      {/* Work Orders */}
      {!loading && activeTab === 'orders' && orders.length > 0 && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {orders.map(order => (
            <div
              key={order.id}
              style={{
                display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                padding: '14px 18px', background: '#fff', borderRadius: 10, border: '1px solid #e5e7eb',
              }}
            >
              <div>
                <div style={{ fontSize: 14, fontWeight: 500, color: '#08060d' }}>{order.title}</div>
                <div style={{ fontSize: 12, color: '#6b7280', marginTop: 2 }}>{order.orderNo}</div>
              </div>
              <span style={{
                fontSize: 12, fontWeight: 500, padding: '2px 10px', borderRadius: 9999,
                color: '#2563eb', background: '#eff6ff',
              }}>
                {statusLabel[order.status] || order.status}
              </span>
            </div>
          ))}
        </div>
      )}

      {/* Empty tabs */}
      {!loading && activeTab === 'sites' && sites.length === 0 && totalResults > 0 && (
        <div style={{ textAlign: 'center', padding: '32px 0', color: '#9ca3af', fontSize: 13 }}>站点中未找到匹配结果</div>
      )}
      {!loading && activeTab === 'devices' && devices.length === 0 && totalResults > 0 && (
        <div style={{ textAlign: 'center', padding: '32px 0', color: '#9ca3af', fontSize: 13 }}>设备中未找到匹配结果</div>
      )}
      {!loading && activeTab === 'orders' && orders.length === 0 && totalResults > 0 && (
        <div style={{ textAlign: 'center', padding: '32px 0', color: '#9ca3af', fontSize: 13 }}>工单中未找到匹配结果</div>
      )}
    </div>
  );
}
