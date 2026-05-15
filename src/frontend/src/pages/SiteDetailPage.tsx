import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { fetchDevices, fetchWorkOrders, fetchDeviceTypes } from '../api/endpoints'
import { useAuth } from '../hooks/useAuth'
import type { ISite, IDevice, IWorkOrder, IDeviceType } from '../api/endpoints'

const s = {
  page: { padding: '32px', maxWidth: '1400px', margin: '0 auto', fontFamily: 'Inter, system-ui, sans-serif' },
  back: {
    display: 'inline-flex', alignItems: 'center', gap: '6px', padding: '6px 12px', borderRadius: '8px',
    fontSize: '14px', fontWeight: 500, border: 'none', cursor: 'pointer', background: 'transparent',
    color: '#6b7280', marginBottom: '16px',
  },
  header: { marginBottom: '32px' },
  title: { fontSize: '28px', fontWeight: 700, color: '#08060d', margin: 0 },
  subtitle: { fontSize: '14px', color: '#6b7280', marginTop: '6px', lineHeight: 1.6 },
  grid: { display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '16px', marginBottom: '32px' },
  statCard: { background: '#fff', borderRadius: '12px', boxShadow: '0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.04)', padding: '20px 24px' },
  statIcon: (bg: string) => ({ width: '36px', height: '36px', borderRadius: '10px', background: bg, display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '12px' }),
  statLabel: { fontSize: '13px', color: '#6b7280', marginBottom: '4px' },
  statValue: { fontSize: '24px', fontWeight: 700, color: '#08060d' },
  twoCol: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '24px', marginBottom: '24px' },
  card: { background: '#fff', borderRadius: '12px', boxShadow: '0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.04)', overflow: 'hidden' },
  cardTitle: { fontSize: '16px', fontWeight: 600, color: '#08060d', padding: '16px 20px', borderBottom: '1px solid #f3f4f6', margin: 0 },
  table: { width: '100%', borderCollapse: 'collapse' as const },
  th: { textAlign: 'left' as const, padding: '12px 16px', fontSize: '12px', fontWeight: 600, color: '#6b7280', borderBottom: '2px solid #e5e7eb', textTransform: 'uppercase' as const, letterSpacing: '0.05em', background: '#f9fafb' },
  td: { padding: '12px 16px', fontSize: '14px', color: '#111827', borderBottom: '1px solid #f3f4f6' },
  badge: (color: string, bg: string) => ({ display: 'inline-block', padding: '2px 10px', borderRadius: '9999px', fontSize: '12px', fontWeight: 500, color, background: bg }),
  bottomGrid: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '24px' },
  bottomCard: { background: '#fff', borderRadius: '12px', boxShadow: '0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.04)', padding: '24px' },
  sectionTitle: { fontSize: '16px', fontWeight: 600, color: '#08060d', margin: '0 0 16px 0' },
  revRow: { display: 'flex', justifyContent: 'space-between', padding: '10px 0', borderBottom: '1px solid #f3f4f6', fontSize: '14px' },
  revLabel: { color: '#6b7280' },
  revValue: { fontWeight: 600, color: '#08060d' },
  statRow: { display: 'flex', justifyContent: 'space-between', padding: '10px 0', borderBottom: '1px solid #f3f4f6', fontSize: '14px' },
  statLabel2: { color: '#6b7280' },
  statValue2: { fontWeight: 500, color: '#111827' },
  empty: { textAlign: 'center' as const, padding: '48px', color: '#9ca3af', fontSize: '14px' },
  loading: { textAlign: 'center' as const, padding: '48px', color: '#6b7280', fontSize: '14px' },
  error: { textAlign: 'center' as const, padding: '48px', color: '#dc2626', fontSize: '14px' },
}

const devStatusLabel: Record<string, string> = { online: '在线', offline: '离线', faulty: '故障' }
const devStatusStyle: Record<string, [string, string]> = {
  online: ['#059669', '#ecfdf5'], offline: ['#9ca3af', '#f3f4f6'], faulty: ['#dc2626', '#fee2e2'],
}
const woTypeLabel: Record<string, string> = { replenishment: '补货', repair: '维修' }
const woStatusLabel: Record<string, string> = {
  pending: '待处理', assigned: '已派单', en_route: '途中', in_progress: '处理中', completed: '已完成', reviewed: '已审核',
}

function formatDate(dateStr: string) {
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function formatMoney(n: number) {
  if (n >= 10000) return `¥${(n / 10000).toFixed(1)}万`
  return `¥${n.toLocaleString()}`
}

const BackIcon = (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <polyline points="15 18 9 12 15 6" />
  </svg>
)

const BoxIcon = (bg: string) => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
    <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" />
  </svg>
)

const CpuIcon = (bg: string) => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
    <rect x="4" y="4" width="16" height="16" rx="2" /><rect x="9" y="9" width="6" height="6" /><line x1="9" y1="1" x2="9" y2="4" /><line x1="15" y1="1" x2="15" y2="4" /><line x1="9" y1="20" x2="9" y2="23" /><line x1="15" y1="20" x2="15" y2="23" /><line x1="20" y1="9" x2="23" y2="9" /><line x1="20" y1="14" x2="23" y2="14" /><line x1="1" y1="9" x2="4" y2="9" /><line x1="1" y1="14" x2="4" y2="14" />
  </svg>
)

const TrendingUpIcon = (bg: string) => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
    <polyline points="23 6 13.5 15.5 8.5 10.5 1 18" /><polyline points="17 6 23 6 23 12" />
  </svg>
)

const ClockIcon = (bg: string) => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
    <circle cx="12" cy="12" r="10" /><polyline points="12 6 12 12 16 14" />
  </svg>
)

export default function SiteDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { token } = useAuth()

  const [site, setSite] = useState<ISite | null>(null)
  const [devices, setDevices] = useState<IDevice[]>([])
  const [deviceTypes, setDeviceTypes] = useState<IDeviceType[]>([])
  const [workOrders, setWorkOrders] = useState<IWorkOrder[]>([])
  const [revenue, setRevenue] = useState<Record<string, unknown> | null>(null)
  const [statistics, setStatistics] = useState<Record<string, unknown> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!token || !id) return
    setLoading(true)
    setError('')
    const siteId = parseInt(id)
    Promise.all([
      api.get<ISite>(`/sites/${siteId}`),
      fetchDevices({ siteId, page: 0, size: 100 }),
      fetchWorkOrders({ siteId, page: 0, size: 10 }),
      api.get<Record<string, unknown>>(`/revenue/sites/${siteId}`),
      api.get<Record<string, unknown>>(`/sites/${siteId}/statistics`),
      fetchDeviceTypes(),
    ]).then(([siteRes, devRes, woRes, revRes, statRes, dtRes]) => {
      setSite(siteRes.data)
      setDevices(devRes.data.content)
      setWorkOrders(woRes.data.content)
      setRevenue(revRes.data)
      setStatistics(statRes.data)
      setDeviceTypes(dtRes.data)
    }).catch(err => {
      setError(err.message || '加载站点详情失败')
    }).finally(() => setLoading(false))
  }, [token, id])

  if (loading) {
    return (
      <div style={s.page}>
        <div style={s.loading}>加载中...</div>
      </div>
    )
  }

  if (error) {
    return (
      <div style={s.page}>
        <button style={s.back} onClick={() => navigate('/sites')}>{BackIcon} 返回站点列表</button>
        <div style={s.error}>{error}</div>
      </div>
    )
  }

  if (!site) {
    return (
      <div style={s.page}>
        <button style={s.back} onClick={() => navigate('/sites')}>{BackIcon} 返回站点列表</button>
        <div style={s.empty}>站点不存在</div>
      </div>
    )
  }

  const activeDevices = devices.filter(d => d.status === 'online').length
  const pendingOrders = workOrders.filter(o => o.status === 'pending').length
  const rev = revenue || {}
  const totalRevenue = Number(rev.totalRevenue || rev.revenue || 0)
  const stats = statistics || {}
  const typeMap = new Map(deviceTypes.map(dt => [dt.id, dt.name]))

  return (
    <div style={s.page}>
      <button style={s.back} onClick={() => navigate('/sites')}>{BackIcon} 返回站点列表</button>

      <div style={s.header}>
        <h1 style={s.title}>{site.name}</h1>
        <div style={s.subtitle}>
          {site.address && <span>{site.address}{site.contactName ? ' · ' : ''}</span>}
          {site.contactName && <span>联系人: {site.contactName}</span>}
          {site.contactPhone && <span> · {site.contactPhone}</span>}
          <span style={{ marginLeft: '12px', display: 'inline-block' }}>
            <span style={site.status === 'active' ? s.badge('#059669', '#ecfdf5') : s.badge('#9ca3af', '#f3f4f6')}>
              {site.status === 'active' ? '运营中' : '已停用'}
            </span>
          </span>
        </div>
      </div>

      <div style={s.grid}>
        <div style={s.statCard}>
          <div style={s.statIcon('#533afd')}>{BoxIcon('#533afd')}</div>
          <div style={s.statLabel}>设备总数</div>
          <div style={s.statValue}>{devices.length}</div>
        </div>
        <div style={s.statCard}>
          <div style={s.statIcon('#059669')}>{CpuIcon('#059669')}</div>
          <div style={s.statLabel}>在线设备</div>
          <div style={s.statValue}>{activeDevices}</div>
        </div>
        <div style={s.statCard}>
          <div style={s.statIcon('#f59e0b')}>{TrendingUpIcon('#f59e0b')}</div>
          <div style={s.statLabel}>总营收</div>
          <div style={s.statValue}>{totalRevenue ? formatMoney(totalRevenue) : '¥0'}</div>
        </div>
        <div style={s.statCard}>
          <div style={s.statIcon('#dc2626')}>{ClockIcon('#dc2626')}</div>
          <div style={s.statLabel}>待处理工单</div>
          <div style={s.statValue}>{pendingOrders}</div>
        </div>
      </div>

      <div style={s.twoCol}>
        <div style={s.card}>
          <h3 style={s.cardTitle}>站点设备 ({devices.length})</h3>
          <table style={s.table}>
            <thead>
              <tr>
                <th style={s.th}>设备编号</th>
                <th style={s.th}>设备名称</th>
                <th style={s.th}>状态</th>
                <th style={s.th}>类型</th>
              </tr>
            </thead>
            <tbody>
              {devices.map(d => (
                <tr key={d.id}>
                  <td style={{ ...s.td, fontFamily: 'ui-monospace, monospace', fontSize: '13px' }}>{d.deviceCode}</td>
                  <td style={{ ...s.td, fontWeight: 500 }}>{d.name}</td>
                  <td style={s.td}>
                    <span style={s.badge(...devStatusStyle[d.status] || ['#6b7280', '#f3f4f6'])}>
                      {devStatusLabel[d.status] || d.status}
                    </span>
                  </td>
                  <td style={s.td}>{typeMap.get(d.deviceTypeId) || '-'}</td>
                </tr>
              ))}
              {devices.length === 0 && (
                <tr><td colSpan={4} style={s.empty}>暂无设备数据</td></tr>
              )}
            </tbody>
          </table>
        </div>

        <div style={s.card}>
          <h3 style={s.cardTitle}>最近工单</h3>
          <table style={s.table}>
            <thead>
              <tr>
                <th style={s.th}>工单号</th>
                <th style={s.th}>类型</th>
                <th style={s.th}>标题</th>
                <th style={s.th}>状态</th>
                <th style={s.th}>创建日期</th>
              </tr>
            </thead>
            <tbody>
              {workOrders.map(o => (
                <tr key={o.id}>
                  <td style={{ ...s.td, fontFamily: 'ui-monospace, monospace', fontSize: '13px' }}>{o.orderNo}</td>
                  <td style={s.td}>{woTypeLabel[o.type] || o.type}</td>
                  <td style={{ ...s.td, fontWeight: 500 }}>{o.title}</td>
                  <td style={s.td}>{woStatusLabel[o.status] || o.status}</td>
                  <td style={{ ...s.td, color: '#6b7280', fontSize: '13px' }}>{formatDate(o.createdAt)}</td>
                </tr>
              ))}
              {workOrders.length === 0 && (
                <tr><td colSpan={5} style={s.empty}>暂无工单数据</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      <div style={s.bottomGrid}>
        <div style={s.bottomCard}>
          <h3 style={s.sectionTitle}>营收信息</h3>
          <div style={s.revRow}>
            <span style={s.revLabel}>总营收</span>
            <span style={s.revValue}>{formatMoney(totalRevenue)}</span>
          </div>
          <div style={s.revRow}>
            <span style={s.revLabel}>总成本</span>
            <span style={s.revValue}>{formatMoney(Number(rev.totalCost || 0))}</span>
          </div>
          <div style={s.revRow}>
            <span style={s.revLabel}>毛利</span>
            <span style={{ ...s.revValue, color: '#059669' }}>{formatMoney(Number(rev.grossProfit || rev.profit || 0))}</span>
          </div>
          <div style={s.revRow}>
            <span style={s.revLabel}>总订单数</span>
            <span style={s.revValue}>{Number(rev.totalOrders || 0).toLocaleString()}</span>
          </div>
        </div>

        <div style={s.bottomCard}>
          <h3 style={s.sectionTitle}>站点统计</h3>
          {Object.keys(stats).length > 0 ? (
            Object.entries(stats).map(([key, value]) => (
              <div key={key} style={s.statRow}>
                <span style={s.statLabel2}>{key}</span>
                <span style={s.statValue2}>{String(value)}</span>
              </div>
            ))
          ) : (
            <div style={s.empty}>暂无统计数据</div>
          )}
        </div>
      </div>
    </div>
  )
}
