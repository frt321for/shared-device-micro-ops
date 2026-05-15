import { useState, useEffect } from 'react'
import { fetchRevenueOverview, fetchRevenueSites, fetchRevenueDevices } from '../api/endpoints'
import { useAuth } from '../hooks/useAuth'
import type { ISiteRevenue } from '../api/endpoints'

const s = {
  page: { padding: '32px', maxWidth: '1400px', margin: '0 auto', fontFamily: 'Inter, system-ui, sans-serif' },
  header: { marginBottom: '32px' },
  title: { fontSize: '24px', fontWeight: 600, color: '#08060d', margin: 0 },
  sub: { fontSize: '14px', color: '#6b7280', marginTop: '4px' },
  grid: { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '16px', marginBottom: '32px' },
  card: { background: '#fff', borderRadius: '12px', boxShadow: '0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.04)', padding: '24px' },
  twoCol: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '24px', marginBottom: '24px' },
  statLabel: { fontSize: '13px', color: '#6b7280', marginBottom: '4px' },
  statValue: { fontSize: '28px', fontWeight: 700, color: '#08060d' },
  statChange: (up: boolean) => ({ fontSize: '12px', color: up ? '#059669' : '#dc2626', marginTop: '4px' }),
  sectionTitle: { fontSize: '16px', fontWeight: 600, color: '#08060d', margin: '0 0 16px 0' },
  table: { width: '100%', borderCollapse: 'collapse' as const },
  th: { textAlign: 'left' as const, padding: '12px 16px', fontSize: '12px', fontWeight: 600, color: '#6b7280', borderBottom: '2px solid #e5e7eb', textTransform: 'uppercase' as const, letterSpacing: '0.05em', background: '#f9fafb' },
  td: { padding: '12px 16px', fontSize: '14px', color: '#111827', borderBottom: '1px solid #f3f4f6' },
  badge: (color: string, bg: string) => ({ display: 'inline-block', padding: '2px 10px', borderRadius: '9999px', fontSize: '12px', fontWeight: 500, color, background: bg }),
  deviceItem: { display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '12px 0', borderBottom: '1px solid #f3f4f6' },
  deviceInfo: { flex: 1 },
  deviceName: { fontSize: '14px', fontWeight: 500, color: '#111827' },
  deviceCode: { fontSize: '12px', color: '#9ca3af', marginTop: '2px' },
  deviceRev: { fontSize: '14px', fontWeight: 600, color: '#08060d' },
  barTrack: { height: '6px', borderRadius: '3px', background: '#f3f4f6', overflow: 'hidden', marginTop: '4px' },
  barFill: (pct: number) => ({ height: '100%', width: `${Math.min(pct, 100)}%`, borderRadius: '3px', background: '#533afd' }),
  rankNum: (i: number) => ({ width: '24px', height: '24px', borderRadius: '6px', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '12px', fontWeight: 600, color: i < 3 ? '#fff' : '#6b7280', background: i < 3 ? '#533afd' : '#f3f4f6', marginRight: '12px', flexShrink: 0 }),
  loading: { textAlign: 'center' as const, padding: '48px', color: '#6b7280', fontSize: '14px' },
  error: { textAlign: 'center' as const, padding: '48px', color: '#dc2626', fontSize: '14px' },
}

function formatMoney(n: number) {
  if (n >= 10000) return `¥${(n / 10000).toFixed(1)}万`
  return `¥${n.toLocaleString()}`
}

export default function RevenuePage() {
  const { token } = useAuth()
  const [overview, setOverview] = useState<Record<string, unknown> | null>(null)
  const [sites, setSites] = useState<ISiteRevenue[]>([])
  const [devices, setDevices] = useState<Record<string, unknown>[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!token) return
    setLoading(true)
    setError('')
    Promise.all([
      fetchRevenueOverview(),
      fetchRevenueSites(),
      fetchRevenueDevices(),
    ]).then(([oRes, sRes, dRes]) => {
      setOverview(oRes.data as Record<string, unknown>)
      setSites(sRes.data)
      setDevices(dRes.data)
    }).catch(err => {
      setError(err.message || '加载营收数据失败')
    }).finally(() => setLoading(false))
  }, [token])

  if (loading) {
    return (
      <div style={s.page}>
        <div style={s.header}><h1 style={s.title}>营收分析</h1></div>
        <div style={s.loading}>加载中...</div>
      </div>
    )
  }

  if (error) {
    return (
      <div style={s.page}>
        <div style={s.header}><h1 style={s.title}>营收分析</h1></div>
        <div style={s.error}>{error}</div>
      </div>
    )
  }

  return (
    <div style={s.page}>
      <div style={s.header}>
        <h1 style={s.title}>营收分析</h1>
        <p style={s.sub}>查看各站点与设备的营收表现</p>
      </div>

      <div style={s.grid}>
        <div style={s.card}>
          <div style={s.statLabel}>总营收</div>
          <div style={s.statValue}>{overview ? formatMoney(Number(overview.totalRevenue || 0)) : '-'}</div>
          {overview?.revenueChange != null && (
            <div style={s.statChange(Number(overview.revenueChange) >= 0)}>
              {Number(overview.revenueChange) >= 0 ? '+' : ''}{overview.revenueChange}% 较上月
            </div>
          )}
        </div>
        <div style={s.card}>
          <div style={s.statLabel}>月营收</div>
          <div style={s.statValue}>          {overview ? formatMoney(Number(overview.monthlyRevenue || overview.totalRevenue || 0)) : '-'}</div>
        </div>
        <div style={s.card}>
          <div style={s.statLabel}>运营站点</div>
          <div style={s.statValue}>{overview?.totalSites ?? overview?.activeSites ?? '-'}</div>
        </div>
        <div style={s.card}>
          <div style={s.statLabel}>活跃设备</div>
          <div style={s.statValue}>{overview?.totalDevices != null ? Number(overview.totalDevices).toLocaleString() : overview?.activeDevices != null ? Number(overview.activeDevices).toLocaleString() : '-'}</div>
          {overview?.deviceChange != null && (
            <div style={s.statChange(Number(overview.deviceChange) >= 0)}>
              {Number(overview.deviceChange) >= 0 ? '+' : ''}{overview.deviceChange}% 较上月
            </div>
          )}
        </div>
      </div>

      <div style={s.twoCol}>
        <div style={s.card}>
          <h2 style={s.sectionTitle}>站点营收排名</h2>
          <table style={s.table}>
            <thead>
              <tr>
                <th style={s.th}>排名</th>
                <th style={s.th}>站点</th>
                <th style={s.th}>营收</th>
                <th style={s.th}>订单数</th>
                <th style={s.th}>毛利</th>
              </tr>
            </thead>
            <tbody>
              {sites.map((site, i) => (
                <tr key={site.siteId}>
                  <td style={s.td}>
                    <div style={s.rankNum(i)}>{i + 1}</div>
                  </td>
                  <td style={{ ...s.td, fontWeight: 500 }}>{site.siteName}</td>
                  <td style={{ ...s.td, fontWeight: 600, color: '#533afd' }}>{formatMoney(site.totalRevenue)}</td>
                  <td style={s.td}>{site.totalOrders.toLocaleString()}</td>
                  <td style={s.td}>{formatMoney(site.grossProfit)}</td>
                </tr>
              ))}
              {sites.length === 0 && (
                <tr><td colSpan={5} style={{ textAlign: 'center', padding: '48px', color: '#9ca3af', fontSize: '14px' }}>暂无站点营收数据</td></tr>
              )}
            </tbody>
          </table>
        </div>

        <div style={s.card}>
          <h2 style={s.sectionTitle}>设备效率排行</h2>
          {devices.map((d: any, i: number) => (
            <div key={d.deviceCode || i} style={s.deviceItem}>
              <div style={s.deviceInfo}>
                <div style={s.deviceName}>{d.deviceName || '-'}</div>
                <div style={s.deviceCode}>{d.deviceCode} · {d.orderCount || 0}单</div>
                <div style={s.barTrack}>
                  <div style={s.barFill(d.efficiency || 0)} />
                </div>
              </div>
              <div style={{ textAlign: 'right' as const }}>
                <div style={{ fontSize: '14px', fontWeight: 600, color: '#08060d' }}>{formatMoney(d.revenue || 0)}</div>
                <div style={{ fontSize: '12px', color: (d.efficiency || 0) >= 80 ? '#059669' : (d.efficiency || 0) >= 60 ? '#f59e0b' : '#dc2626' }}>
                  {d.efficiency || 0}%
                </div>
              </div>
            </div>
          ))}
          {devices.length === 0 && (
            <div style={{ textAlign: 'center', padding: '48px', color: '#9ca3af', fontSize: '14px' }}>暂无设备效率数据</div>
          )}
        </div>
      </div>
    </div>
  )
}
