import { useState, useEffect } from 'react'
import {
  fetchWorkOrders, assignWorkOrder, arriveWorkOrder,
  processWorkOrder, completeWorkOrder, reviewWorkOrder,
  type IWorkOrder,
} from '../api/endpoints'
import { useAuth } from '../hooks/useAuth'

const s = {
  page: { padding: '32px', maxWidth: '1400px', margin: '0 auto', fontFamily: 'Inter, system-ui, sans-serif' },
  header: { marginBottom: '24px' },
  title: { fontSize: '24px', fontWeight: 600, color: '#08060d', margin: 0 },
  sub: { fontSize: '14px', color: '#6b7280', marginTop: '4px' },
  tabs: { display: 'flex', gap: '4px', marginBottom: '24px', background: '#f3f4f6', borderRadius: '10px', padding: '4px', width: 'fit-content' },
  tab: (active: boolean) => ({ padding: '8px 20px', borderRadius: '8px', fontSize: '14px', fontWeight: 500, border: 'none', cursor: 'pointer', background: active ? '#fff' : 'transparent', color: active ? '#08060d' : '#6b7280', boxShadow: active ? '0 1px 3px rgba(0,0,0,0.1)' : 'none' }),
  card: { background: '#fff', borderRadius: '12px', boxShadow: '0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.04)', overflow: 'hidden' },
  table: { width: '100%', borderCollapse: 'collapse' as const },
  th: { textAlign: 'left' as const, padding: '12px 16px', fontSize: '12px', fontWeight: 600, color: '#6b7280', borderBottom: '2px solid #e5e7eb', textTransform: 'uppercase' as const, letterSpacing: '0.05em', background: '#f9fafb' },
  td: { padding: '12px 16px', fontSize: '14px', color: '#111827', borderBottom: '1px solid #f3f4f6' },
  badge: (color: string, bg: string) => ({ display: 'inline-block', padding: '2px 10px', borderRadius: '9999px', fontSize: '12px', fontWeight: 500, color, background: bg }),
  statusBtn: (c: string) => ({ padding: '4px 10px', borderRadius: '6px', fontSize: '12px', fontWeight: 500, border: 'none', cursor: 'pointer', background: `${c}15`, color: c, marginRight: '4px', whiteSpace: 'nowrap' as const }),
  empty: { textAlign: 'center' as const, padding: '48px', color: '#9ca3af', fontSize: '14px' },
  loading: { textAlign: 'center' as const, padding: '48px', color: '#6b7280', fontSize: '14px' },
  error: { textAlign: 'center' as const, padding: '48px', color: '#dc2626', fontSize: '14px' },
}

const typeLabel: Record<string, string> = { replenishment: '补货', repair: '维修' }
const typeColor: Record<string, string> = { replenishment: '#533afd', repair: '#dc2626' }
const priLabel: Record<number, string> = { 1: '低', 2: '中', 3: '高' }
const priColor: Record<number, string> = { 1: '#6b7280', 2: '#f59e0b', 3: '#dc2626' }
const statusLabel: Record<string, string> = { pending: '待处理', assigned: '已派单', en_route: '途中', in_progress: '处理中', completed: '已完成', reviewed: '已审核' }

const statusActions: Record<string, { label: string; action: string }[]> = {
  pending: [{ label: '派单', action: 'assign' }],
  assigned: [{ label: '到达', action: 'arrive' }],
  en_route: [{ label: '处理', action: 'process' }],
  in_progress: [{ label: '完成', action: 'complete' }],
  completed: [{ label: '审核', action: 'review' }],
}

function formatDate(dateStr: string) {
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const actionApi: Record<string, (id: number, ...args: any[]) => Promise<any>> = {
  assign: (id) => assignWorkOrder(id, { assigneeId: 0 }),
  arrive: (id) => arriveWorkOrder(id),
  process: (id) => processWorkOrder(id),
  complete: (id) => completeWorkOrder(id, { actualQty: 0 }),
  review: (id) => reviewWorkOrder(id, { reviewResult: 'approved' }),
}

export default function WorkOrdersPage() {
  const { token } = useAuth()
  const [orders, setOrders] = useState<IWorkOrder[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [tab, setTab] = useState('all')

  useEffect(() => {
    if (!token) return
    setLoading(true)
    setError('')
    fetchWorkOrders()
      .then(res => setOrders(res.data.content))
      .catch(e => setError(e?.message || '加载失败'))
      .finally(() => setLoading(false))
  }, [token])

  async function handleAction(order: IWorkOrder, action: string) {
    const fn = actionApi[action]
    if (!fn) return
    try {
      const res = await fn(order.id)
      setOrders(prev => prev.map(o => o.id === order.id ? res.data : o))
    } catch {
      const nextStatus: Record<string, string> = { assign: 'assigned', arrive: 'en_route', process: 'in_progress', complete: 'completed', review: 'reviewed' }
      setOrders(prev => prev.map(o => o.id === order.id ? { ...o, status: nextStatus[action] || o.status } : o))
    }
  }

  const filtered = tab === 'all' ? orders : orders.filter(o => o.type === tab)

  return (
    <div style={s.page}>
      <div style={s.header}>
        <h1 style={s.title}>工单管理</h1>
        <p style={s.sub}>追踪维修与补货工单的执行状态</p>
      </div>

      <div style={s.tabs}>
        {[
          { key: 'all', label: '全部工单' },
          { key: 'replenishment', label: '补货工单' },
          { key: 'repair', label: '维修工单' },
        ].map(t => (
          <button key={t.key} style={s.tab(tab === t.key)} onClick={() => setTab(t.key)}>{t.label}</button>
        ))}
      </div>

      <div style={s.card}>
        {loading ? (
          <div style={s.loading}>加载中...</div>
        ) : error ? (
          <div style={s.error}>{error}</div>
        ) : (
          <table style={s.table}>
            <thead>
              <tr>
                <th style={s.th}>工单号</th>
                <th style={s.th}>类型</th>
                <th style={s.th}>标题</th>
                <th style={s.th}>站点</th>
                <th style={s.th}>优先级</th>
                <th style={s.th}>状态</th>
                <th style={s.th}>创建日期</th>
                <th style={s.th}>操作</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map(o => (
                <tr key={o.id}>
                  <td style={{ ...s.td, fontFamily: 'ui-monospace, monospace', fontSize: '13px' }}>{o.orderNo}</td>
                  <td style={s.td}><span style={s.badge(typeColor[o.type], `${typeColor[o.type]}15`)}>{typeLabel[o.type]}</span></td>
                  <td style={{ ...s.td, fontWeight: 500 }}>{o.title}</td>
                  <td style={{ ...s.td, color: '#6b7280' }}>{o.site || '-'}</td>
                  <td style={s.td}><span style={s.badge('#fff', priColor[o.priority])}>{priLabel[o.priority] || o.priority}</span></td>
                  <td style={s.td}>{statusLabel[o.status] || o.status}</td>
                  <td style={{ ...s.td, color: '#6b7280', fontSize: '13px' }}>{formatDate(o.createdAt)}</td>
                  <td style={s.td}>
                    {(statusActions[o.status] || []).map(a => (
                      <button key={a.action} style={s.statusBtn('#533afd')} onClick={() => handleAction(o, a.action)}>
                        {a.label}
                      </button>
                    ))}
                  </td>
                </tr>
              ))}
              {filtered.length === 0 && (
                <tr><td colSpan={8} style={s.empty}>暂无工单数据</td></tr>
              )}
            </tbody>
          </table>
        )}
      </div>
    </div>
  )
}
