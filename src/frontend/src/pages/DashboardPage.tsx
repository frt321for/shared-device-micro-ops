import { useState, useEffect } from 'react'
import {
  fetchDashboardOverview,
  fetchDashboardAlerts,
} from '../api/endpoints'
import { useAuth } from '../hooks/useAuth'
import type { IDashboardOverview } from '../api/endpoints'
import ReactECharts from 'echarts-for-react'

const s = {
  page: { padding: '32px', maxWidth: '1400px', margin: '0 auto', fontFamily: 'Inter, system-ui, sans-serif' },
  header: { marginBottom: '32px' },
  title: { fontSize: '24px', fontWeight: 600, color: '#08060d', margin: 0 },
  subtitle: { fontSize: '14px', color: '#6b7280', marginTop: '4px' },
  grid: { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '16px', marginBottom: '24px' },
  card: { background: '#fff', borderRadius: '12px', boxShadow: '0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.04)', padding: '24px' },
  twoCol: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '24px', marginBottom: '24px' },
  statIcon: (bg: string) => ({ width: '40px', height: '40px', borderRadius: '10px', background: bg, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '18px', marginBottom: '12px' }),
  statLabel: { fontSize: '13px', color: '#6b7280', marginBottom: '4px' },
  statValue: { fontSize: '28px', fontWeight: 700, color: '#08060d' },
  statChange: (up: boolean) => ({ fontSize: '12px', color: up ? '#059669' : '#dc2626', marginTop: '4px' }),
  sectionTitle: { fontSize: '16px', fontWeight: 600, color: '#08060d', margin: '0 0 16px 0' },
  alertItem: { display: 'flex', alignItems: 'center', gap: '12px', padding: '12px 0', borderBottom: '1px solid #f3f4f6' },
  alertDot: (level: string) => ({
    width: '8px', height: '8px', borderRadius: '50%', flexShrink: 0,
    background: level === 'error' ? '#dc2626' : level === 'warning' ? '#f59e0b' : '#3b82f6',
  }),
  alertText: { flex: 1, fontSize: '14px', color: '#374151' },
  alertTime: { fontSize: '12px', color: '#9ca3af' },
  table: { width: '100%', borderCollapse: 'collapse' as const },
  th: { textAlign: 'left' as const, padding: '10px 16px', fontSize: '12px', fontWeight: 600, color: '#6b7280', borderBottom: '2px solid #e5e7eb', textTransform: 'uppercase' as const, letterSpacing: '0.05em' },
  td: { padding: '10px 16px', fontSize: '14px', color: '#111827', borderBottom: '1px solid #f3f4f6' },
  badge: (color: string, bg: string) => ({ display: 'inline-block', padding: '2px 10px', borderRadius: '9999px', fontSize: '12px', fontWeight: 500, color, background: bg }),
  deviceBar: { display: 'flex', alignItems: 'center', gap: '12px', padding: '8px 0', borderBottom: '1px solid #f3f4f6' },
  barTrack: { flex: 1, height: '8px', borderRadius: '4px', background: '#f3f4f6', overflow: 'hidden' },
  barFill: (pct: number) => ({ height: '100%', width: `${pct}%`, borderRadius: '4px', background: '#533afd' }),
  siteName: { fontSize: '13px', color: '#374151', width: '100px', flexShrink: 0 },
}

function formatDate(dateStr: string) {
  const d = new Date(dateStr)
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mi = String(d.getMinutes()).padStart(2, '0')
  return `${mm}-${dd} ${hh}:${mi}`
}

const statusLabel: Record<string, string> = { pending: '待处理', assigned: '已派单', en_route: '途中', in_progress: '处理中', completed: '已完成', reviewed: '已审核' }
const typeLabel: Record<string, string> = { replenishment: '补货', repair: '维修' }
const priLabel: Record<string, string> = { high: '高', medium: '中', low: '低' }
const priColor: Record<string, string> = { high: '#dc2626', medium: '#f59e0b', low: '#6b7280' }

export default function DashboardPage() {
  const { token } = useAuth()
  const [overview, setOverview] = useState<IDashboardOverview | null>(null)
  const [alerts, setAlerts] = useState<Record<string, unknown>[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!token) return
    setLoading(true)
    setError('')
    Promise.all([
      fetchDashboardOverview(),
      fetchDashboardAlerts(),
    ]).then(([o, a]) => {
      setOverview(o.data)
      setAlerts(a.data)
    }).catch(() => {
      setError('获取数据失败，请稍后重试')
    }).finally(() => setLoading(false))
  }, [token])

  return (
    <div style={s.page}>
      <div style={s.header}>
        <h1 style={s.title}>运营概览</h1>
        <p style={s.subtitle}>实时监控设备状态、库存预警与工单情况</p>
      </div>

      {loading && (
        <div style={{ textAlign: 'center', padding: '64px 0', fontSize: '16px', color: '#6b7280' }}>
          加载中...
        </div>
      )}

      {error && (
        <div style={{ textAlign: 'center', padding: '64px 0', fontSize: '16px', color: '#dc2626' }}>
          {error}
        </div>
      )}

      {!loading && !error && (
        <>
          <div style={s.grid}>
            <StatCard icon="🖥️" bg="#ede9fe" label="在线设备" value={(overview?.onlineDevices ?? 0).toLocaleString()} change="" up />
            <StatCard icon="📦" bg="#fef3c7" label="缺货预警" value={(overview?.lowStockCount ?? 0).toString()} change="" up={false} />
            <StatCard icon="⚠️" bg="#fee2e2" label="故障设备" value={(overview?.faultDevices ?? 0).toString()} change="" up={false} />
            <StatCard icon="📋" bg="#dbeafe" label="待派工单" value={(overview?.pendingWorkOrders ?? 0).toString()} change="" up={false} />
          </div>

          <div style={s.twoCol}>
            <div style={s.card}>
              <h2 style={s.sectionTitle}>实时告警</h2>
              {alerts.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '32px 0', fontSize: '14px', color: '#9ca3af' }}>暂无告警</div>
              ) : (
                alerts.map((a, i) => (
                  <div key={String(a.id ?? i)} style={s.alertItem}>
                    <div style={s.alertDot(String(a.level ?? 'info'))} />
                    <span style={s.alertText}>{String(a.message ?? '')}</span>
                    <span style={s.alertTime}>{String(a.time ?? '')}</span>
                  </div>
                ))
              )}
            </div>

            <div style={s.card}>
              <h2 style={s.sectionTitle}>设备在线率</h2>
              <ReactECharts option={{
                tooltip: { trigger: 'item' },
                series: [{
                  type: 'pie',
                  radius: ['55%', '75%'],
                  avoidLabelOverlap: false,
                  itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
                  label: { show: false },
                  labelLine: { show: false },
                  data: [
                    { value: 68, name: '在线', itemStyle: { color: '#10b981' } },
                    { value: 15, name: '离线', itemStyle: { color: '#94a3b8' } },
                    { value: 10, name: '故障', itemStyle: { color: '#ef4444' } },
                    { value: 7, name: '维护中', itemStyle: { color: '#f59e0b' } },
                  ],
                }],
                graphic: {
                  type: 'text',
                  left: 'center',
                  top: 'center',
                  style: { text: '68%', textAlign: 'center', fill: '#08060d', fontSize: 28, fontWeight: 700 },
                },
              }} style={{ height: 240 }} />
            </div>
          </div>

          <div style={{ ...s.card, marginBottom: 0 }}>
            <h2 style={s.sectionTitle}>最近工单</h2>
            <table style={s.table}>
              <thead>
                <tr>
                  <th style={s.th}>工单号</th>
                  <th style={s.th}>类型</th>
                  <th style={s.th}>标题</th>
                  <th style={s.th}>站点</th>
                  <th style={s.th}>优先级</th>
                  <th style={s.th}>状态</th>
                  <th style={s.th}>创建时间</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td style={{ ...s.td, textAlign: 'center', color: '#9ca3af' }} colSpan={7}>暂无工单</td>
                </tr>
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  )
}

function StatCard({ icon, bg, label, value, change, up }: { icon: string; bg: string; label: string; value: string; change: string; up: boolean }) {
  return (
    <div style={{ background: '#fff', borderRadius: '12px', boxShadow: '0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.04)', padding: '24px' }}>
      <div style={s.statIcon(bg)}>{icon}</div>
      <div style={s.statLabel}>{label}</div>
      <div style={s.statValue}>{value}</div>
      {change && <div style={s.statChange(up)}>{change}</div>}
    </div>
  )
}
