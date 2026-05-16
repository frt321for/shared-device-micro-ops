import { useState, useEffect } from 'react'
import {
  fetchWorkOrders, createWorkOrder, assignWorkOrder, arriveWorkOrder,
  processWorkOrder, completeWorkOrder, reviewWorkOrder,
  type IWorkOrder,
} from '../api/endpoints'
import { useAuth } from '../hooks/useAuth'
import { api } from '../api/client'

interface IUser {
  id: number; username: string; displayName: string; role: string;
}

const s = {
  page: { padding: '32px', maxWidth: '1400px', margin: '0 auto', fontFamily: 'Inter, system-ui, sans-serif' },
  header: { marginBottom: '24px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' },
  title: { fontSize: '24px', fontWeight: 600, color: '#08060d', margin: 0 },
  sub: { fontSize: '14px', color: '#6b7280', marginTop: '4px' },
  headerLeft: {},
  addBtn: { padding: '8px 20px', borderRadius: '8px', fontSize: '14px', fontWeight: 500, border: 'none', cursor: 'pointer', background: '#533afd', color: '#fff', whiteSpace: 'nowrap' as const },
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
  pagination: { display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', padding: '16px', borderTop: '1px solid #f3f4f6' },
  pageBtn: (active: boolean) => ({ padding: '6px 12px', borderRadius: '6px', fontSize: '13px', fontWeight: active ? 600 : 400, border: '1px solid', cursor: 'pointer', background: active ? '#533afd' : '#fff', color: active ? '#fff' : '#374151', borderColor: active ? '#533afd' : '#e5e7eb', minWidth: '32px' }),
  pageInfo: { fontSize: '13px', color: '#6b7280', margin: '0 12px' },
  overlay: { position: 'fixed' as const, inset: 0, background: 'rgba(0,0,0,0.4)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 },
  modal: { background: '#fff', borderRadius: '16px', padding: '32px', width: '520px', maxWidth: '90vw', maxHeight: '80vh', overflowY: 'auto' as const, boxShadow: '0 20px 60px rgba(0,0,0,0.15)' },
  modalTitle: { fontSize: '18px', fontWeight: 600, color: '#08060d', marginBottom: '24px' },
  fieldRow: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '16px' },
  fieldFull: { marginBottom: '16px' },
  label: { display: 'block', fontSize: '13px', fontWeight: 500, color: '#374151', marginBottom: '6px' },
  input: { width: '100%', padding: '8px 12px', borderRadius: '8px', border: '1px solid #e5e7eb', fontSize: '14px', color: '#111827', outline: 'none', boxSizing: 'border-box' as const },
  select: { width: '100%', padding: '8px 12px', borderRadius: '8px', border: '1px solid #e5e7eb', fontSize: '14px', color: '#111827', outline: 'none', background: '#fff', boxSizing: 'border-box' as const },
  modalActions: { display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '24px' },
  btnPrimary: { padding: '8px 24px', borderRadius: '8px', fontSize: '14px', fontWeight: 500, border: 'none', cursor: 'pointer', background: '#533afd', color: '#fff' },
  btnSecondary: { padding: '8px 24px', borderRadius: '8px', fontSize: '14px', fontWeight: 500, border: '1px solid #e5e7eb', cursor: 'pointer', background: '#fff', color: '#374151' },
  formError: { fontSize: '13px', color: '#dc2626', marginBottom: '12px' },
  detailGrid: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', marginBottom: '20px' },
  detailItem: {},
  detailLabel: { fontSize: '12px', color: '#6b7280', marginBottom: '2px' },
  detailValue: { fontSize: '14px', color: '#111827', fontWeight: 500 },
  detailTabs: { display: 'flex', gap: '4px', marginBottom: '16px', background: '#f3f4f6', borderRadius: '8px', padding: '3px', width: 'fit-content' },
  detailTab: (active: boolean) => ({ padding: '6px 16px', borderRadius: '6px', fontSize: '13px', fontWeight: 500, border: 'none', cursor: 'pointer', background: active ? '#fff' : 'transparent', color: active ? '#08060d' : '#6b7280', boxShadow: active ? '0 1px 2px rgba(0,0,0,0.08)' : 'none' }),
  timeline: { position: 'relative' as const, paddingLeft: '24px' },
  timelineLine: { position: 'absolute' as const, left: '7px', top: '8px', bottom: '8px', width: '2px', background: '#e5e7eb' },
  timelineDot: { position: 'absolute' as const, left: '-16px', top: '4px', width: '10px', height: '10px', borderRadius: '50%', background: '#533afd', border: '2px solid #fff', boxShadow: '0 0 0 2px #533afd' },
  timelineItem: { position: 'relative' as const, paddingBottom: '20px' },
  timelineTime: { fontSize: '12px', color: '#9ca3af', marginBottom: '2px' },
  timelineAction: { fontSize: '13px', color: '#374151', fontWeight: 500 },
  timelineDetail: { fontSize: '13px', color: '#6b7280', marginTop: '2px' },
}

const typeLabel: Record<string, string> = { replenishment: '补货', repair: '维修' }
const typeColor: Record<string, string> = { replenishment: '#533afd', repair: '#dc2626' }
const priLabel: Record<number, string> = { 1: '低', 2: '中', 3: '高' }
const priColor: Record<number, string> = { 1: '#6b7280', 2: '#f59e0b', 3: '#dc2626' }
const statusLabel: Record<string, string> = { pending_assign: '待派单', assigned: '已派单', arrived: '已到场', processing: '处理中', pending_review: '待复核', closed: '已关闭', rejected: '已驳回', cancelled: '已取消' }

const statusActions: Record<string, { label: string; action: string }[]> = {
  pending_assign: [{ label: '派单', action: 'assign' }],
  assigned: [{ label: '到达', action: 'arrive' }],
  arrived: [{ label: '处理', action: 'process' }],
  processing: [{ label: '完成', action: 'complete' }],
  pending_review: [{ label: '复核', action: 'review' }],
}

function formatDate(dateStr: string) {
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function formatDateTime(dateStr: string) {
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

const actionApi: Record<string, (id: number, ...args: unknown[]) => Promise<unknown>> = {
  assign: (id, assigneeId) => assignWorkOrder(id, { assigneeId: assigneeId as number }),
  arrive: (id) => arriveWorkOrder(id),
  process: (id) => processWorkOrder(id),
  complete: (id) => completeWorkOrder(id, { actualQty: 0 }),
  review: (id) => reviewWorkOrder(id, { reviewResult: 'approved' }),
}

const PAGE_SIZE = 10

export default function WorkOrdersPage() {
  const { token } = useAuth()
  const [orders, setOrders] = useState<IWorkOrder[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [tab, setTab] = useState('all')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [detailOrder, setDetailOrder] = useState<IWorkOrder | null>(null)
  const [auditLog, setAuditLog] = useState<Record<string, unknown>[]>([])
  const [auditTab, setAuditTab] = useState<'info' | 'audit'>('info')
  const [users, setUsers] = useState<IUser[]>([])
  const [actionInputs, setActionInputs] = useState<Record<number, { action: string; assigneeId?: number; actualQty?: number; reviewResult?: string }>>({})
  const [form, setForm] = useState({ type: 'replenishment', title: '', deviceId: '', siteId: '', expectedQty: '', priority: '2' })
  const [formError, setFormError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function loadOrders() {
    if (!token) return
    setLoading(true)
    setError('')
    try {
      const params: Record<string, unknown> = { page, size: PAGE_SIZE }
      if (tab !== 'all') params.type = tab
      const res = await fetchWorkOrders(params)
      setOrders(res.data.content)
      setTotalPages(res.data.totalPages)
      setTotalElements(res.data.totalElements)
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '加载失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadOrders()
  }, [token, page, tab])

  useEffect(() => {
    if (!token) return
    api.get<IUser[]>('/auth/users').then(res => setUsers(res.data || [])).catch(() => {})
  }, [token])

  function handleTabChange(newTab: string) {
    setTab(newTab)
    setPage(0)
  }

  async function handleAction(order: IWorkOrder, action: string) {
    if (action === 'complete' || action === 'review' || action === 'assign') return
    const fn = actionApi[action]
    if (!fn) return
    try {
      await fn(order.id)
      loadOrders()
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '操作失败')
    }
  }

  async function handleAssign(order: IWorkOrder, assigneeId: number) {
    try {
      await assignWorkOrder(order.id, { assigneeId })
      setActionInputs(prev => { const next = { ...prev }; delete next[order.id]; return next })
      loadOrders()
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '操作失败')
    }
  }

  async function handleComplete(order: IWorkOrder, actualQty: number) {
    try {
      await completeWorkOrder(order.id, { actualQty })
      setActionInputs(prev => { const next = { ...prev }; delete next[order.id]; return next })
      loadOrders()
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '操作失败')
    }
  }

  async function handleReview(order: IWorkOrder, result: string) {
    try {
      await reviewWorkOrder(order.id, { reviewResult: result })
      setActionInputs(prev => { const next = { ...prev }; delete next[order.id]; return next })
      loadOrders()
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : '操作失败')
    }
  }

  async function handleCreate() {
    setFormError('')
    if (!form.title.trim()) { setFormError('请输入标题'); return }
    setSubmitting(true)
    try {
      await createWorkOrder({
        type: form.type,
        title: form.title,
        deviceId: form.deviceId ? Number(form.deviceId) : undefined,
        siteId: form.siteId ? Number(form.siteId) : undefined,
        expectedQty: form.expectedQty ? Number(form.expectedQty) : undefined,
        priority: Number(form.priority),
      })
      setShowCreateModal(false)
      setForm({ type: 'replenishment', title: '', deviceId: '', siteId: '', expectedQty: '', priority: '2' })
      setPage(0)
      loadOrders()
    } catch (e: unknown) {
      setFormError(e instanceof Error ? e.message : '创建失败')
    } finally {
      setSubmitting(false)
    }
  }

  async function openDetail(order: IWorkOrder) {
    setDetailOrder(order)
    setAuditTab('info')
    setAuditLog([])
    try {
      const res = await api.get<Record<string, unknown>[]>(`/work-orders/${order.id}/audit`)
      setAuditLog(res.data || [])
    } catch {
      /* ignore */
    }
  }

  function closeDetail() {
    setDetailOrder(null)
    setAuditLog([])
  }

  const pageNumbers: number[] = []
  const maxVisible = 5
  let start = Math.max(0, page - Math.floor(maxVisible / 2))
  const end = Math.min(totalPages, start + maxVisible)
  if (end - start < maxVisible) start = Math.max(0, end - maxVisible)
  for (let i = start; i < end; i++) pageNumbers.push(i)

  const FieldLabel: Record<string, string> = {
    orderNo: '工单号', type: '类型', status: '状态', priority: '优先级',
    title: '标题', deviceId: '设备 ID', siteId: '站点 ID', skuId: '商品 ID',
    expectedQty: '期望数量', actualQty: '实际数量', assigneeId: '负责人 ID',
    description: '描述', createdAt: '创建时间', updatedAt: '更新时间',
  }

  function renderFieldValue(key: string, order: IWorkOrder): string {
    const v = (order as unknown as Record<string, unknown>)[key]
    if (v === undefined || v === null) return '-'
    const vs = String(v)
    if (key === 'type') return typeLabel[vs] || vs
    if (key === 'status') return statusLabel[vs] || vs
    if (key === 'priority') return priLabel[v as number] || vs
    if (key === 'createdAt' || key === 'updatedAt') return formatDateTime(vs)
    return vs
  }

  const detailFields: (keyof IWorkOrder)[] = [
    'orderNo', 'type', 'status', 'priority', 'title',
    'deviceId', 'siteId', 'skuId', 'expectedQty', 'actualQty',
    'assigneeId', 'description', 'createdAt', 'updatedAt',
  ]

  return (
    <div style={s.page}>
      <div style={s.header}>
        <div style={s.headerLeft}>
          <h1 style={s.title}>工单管理</h1>
          <p style={s.sub}>追踪维修与补货工单的执行状态</p>
        </div>
        <button style={s.addBtn} onClick={() => setShowCreateModal(true)}>+ 新建工单</button>
      </div>

      <div style={s.tabs}>
        {[
          { key: 'all', label: '全部工单' },
          { key: 'replenishment', label: '补货工单' },
          { key: 'repair', label: '维修工单' },
        ].map(t => (
          <button key={t.key} style={s.tab(tab === t.key)} onClick={() => handleTabChange(t.key)}>{t.label}</button>
        ))}
      </div>

      <div style={s.card}>
        {loading ? (
          <div style={s.loading}>加载中...</div>
        ) : error ? (
          <div style={s.error}>{error}</div>
        ) : (
          <>
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
                {orders.map(o => (
                  <tr key={o.id} onClick={() => openDetail(o)} style={{ cursor: 'pointer' }}>
                    <td style={{ ...s.td, fontFamily: 'ui-monospace, monospace', fontSize: '13px' }}>{o.orderNo}</td>
                    <td style={s.td}><span style={s.badge(typeColor[o.type], `${typeColor[o.type]}15`)}>{typeLabel[o.type]}</span></td>
                    <td style={{ ...s.td, fontWeight: 500 }}>{o.title}</td>
                    <td style={{ ...s.td, color: '#6b7280' }}>{String((o as unknown as Record<string, unknown>).siteName || (o as unknown as Record<string, unknown>).site || '-')}</td>
                    <td style={s.td}><span style={s.badge('#fff', priColor[o.priority])}>{priLabel[o.priority] || o.priority}</span></td>
                    <td style={s.td}>{statusLabel[o.status] || o.status}</td>
                    <td style={{ ...s.td, color: '#6b7280', fontSize: '13px' }}>{formatDate(o.createdAt)}</td>
                    <td style={s.td} onClick={e => e.stopPropagation()}>
                      {(statusActions[o.status] || []).map(a => {
                        if (a.action === 'assign') {
                          const input = actionInputs[o.id]
                          if (input?.action === 'assign') {
                            return (
                              <span key={a.action} style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                                <select style={{ padding: '4px 6px', borderRadius: '6px', border: '1px solid #e5e7eb', fontSize: '12px', outline: 'none', background: '#fff' }}
                                  value={input.assigneeId || ''}
                                  onChange={e => setActionInputs(prev => ({ ...prev, [o.id]: { action: 'assign', assigneeId: Number(e.target.value) } }))}>
                                  <option value="">选择负责人</option>
                                  {users.filter(u => u.status !== 'inactive').map(u => (
                                    <option key={u.id} value={u.id}>{u.displayName} ({u.role})</option>
                                  ))}
                                </select>
                                <button style={s.statusBtn('#059669')} onClick={() => handleAssign(o, input.assigneeId || users[0]?.id || 1)}>确认</button>
                                <button style={s.statusBtn('#6b7280')} onClick={() => setActionInputs(prev => { const next = { ...prev }; delete next[o.id]; return next })}>取消</button>
                              </span>
                            )
                          }
                          return <button key={a.action} style={s.statusBtn('#533afd')} onClick={() => setActionInputs(prev => ({ ...prev, [o.id]: { action: 'assign', assigneeId: users[0]?.id || 1 } }))}>{a.label}</button>
                        }
                        if (a.action === 'complete') {
                          const input = actionInputs[o.id]
                          if (input?.action === 'complete') {
                            return (
                              <span key={a.action} style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                                <input type="number" min={0} placeholder="实际数量"
                                  style={{ width: '72px', padding: '4px 6px', borderRadius: '6px', border: '1px solid #e5e7eb', fontSize: '12px', outline: 'none' }}
                                  value={input.actualQty ?? ''}
                                  onChange={e => setActionInputs(prev => ({ ...prev, [o.id]: { action: 'complete', actualQty: Number(e.target.value) } }))} />
                                <button style={s.statusBtn('#059669')} onClick={() => handleComplete(o, input.actualQty || 0)}>确认</button>
                                <button style={s.statusBtn('#6b7280')} onClick={() => setActionInputs(prev => { const next = { ...prev }; delete next[o.id]; return next })}>取消</button>
                              </span>
                            )
                          }
                          return <button key={a.action} style={s.statusBtn('#533afd')} onClick={() => setActionInputs(prev => ({ ...prev, [o.id]: { action: 'complete', actualQty: 0 } }))}>{a.label}</button>
                        }
                        if (a.action === 'review') {
                          const input = actionInputs[o.id]
                          if (input?.action === 'review') {
                            return (
                              <span key={a.action} style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                                <select style={{ padding: '4px 6px', borderRadius: '6px', border: '1px solid #e5e7eb', fontSize: '12px', outline: 'none', background: '#fff' }}
                                  value={input.reviewResult || 'approved'}
                                  onChange={e => setActionInputs(prev => ({ ...prev, [o.id]: { action: 'review', reviewResult: e.target.value } }))}>
                                  <option value="approved">通过</option>
                                  <option value="rejected">驳回</option>
                                </select>
                                <button style={s.statusBtn('#059669')} onClick={() => handleReview(o, input.reviewResult || 'approved')}>确认</button>
                                <button style={s.statusBtn('#6b7280')} onClick={() => setActionInputs(prev => { const next = { ...prev }; delete next[o.id]; return next })}>取消</button>
                              </span>
                            )
                          }
                          return <button key={a.action} style={s.statusBtn('#533afd')} onClick={() => setActionInputs(prev => ({ ...prev, [o.id]: { action: 'review', reviewResult: 'approved' } }))}>{a.label}</button>
                        }
                        return <button key={a.action} style={s.statusBtn('#533afd')} onClick={() => handleAction(o, a.action)}>{a.label}</button>
                      })}
                    </td>
                  </tr>
                ))}
                {orders.length === 0 && (
                  <tr><td colSpan={8} style={s.empty}>暂无工单数据</td></tr>
                )}
              </tbody>
            </table>
            {totalPages > 1 && (
              <div style={s.pagination}>
                <button
                  style={s.pageBtn(false)}
                  disabled={page === 0}
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                >上一页</button>
                {pageNumbers.map(n => (
                  <button
                    key={n}
                    style={s.pageBtn(page === n)}
                    onClick={() => setPage(n)}
                  >{n + 1}</button>
                ))}
                <span style={s.pageInfo}>第 {page + 1} 页 / 共 {totalPages} 页 (共 {totalElements} 条)</span>
                <button
                  style={s.pageBtn(false)}
                  disabled={page >= totalPages - 1}
                  onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                >下一页</button>
              </div>
            )}
          </>
        )}
      </div>

      {showCreateModal && (
        <div style={s.overlay} onClick={() => setShowCreateModal(false)}>
          <div style={s.modal} onClick={e => e.stopPropagation()}>
            <div style={s.modalTitle}>新建工单</div>
            {formError && <div style={s.formError}>{formError}</div>}
            <div style={s.fieldFull}>
              <label style={s.label}>类型</label>
              <select style={s.select} value={form.type} onChange={e => setForm(f => ({ ...f, type: e.target.value }))}>
                <option value="replenishment">补货</option>
                <option value="repair">维修</option>
              </select>
            </div>
            <div style={s.fieldFull}>
              <label style={s.label}>标题</label>
              <input style={s.input} value={form.title} onChange={e => setForm(f => ({ ...f, title: e.target.value }))} placeholder="请输入工单标题" />
            </div>
            <div style={s.fieldRow}>
              <div>
                <label style={s.label}>设备 ID</label>
                <input style={s.input} type="number" value={form.deviceId} onChange={e => setForm(f => ({ ...f, deviceId: e.target.value }))} placeholder="可选" />
              </div>
              <div>
                <label style={s.label}>站点 ID</label>
                <input style={s.input} type="number" value={form.siteId} onChange={e => setForm(f => ({ ...f, siteId: e.target.value }))} placeholder="可选" />
              </div>
            </div>
            <div style={s.fieldRow}>
              <div>
                <label style={s.label}>期望数量</label>
                <input style={s.input} type="number" value={form.expectedQty} onChange={e => setForm(f => ({ ...f, expectedQty: e.target.value }))} placeholder="可选" />
              </div>
              <div>
                <label style={s.label}>优先级</label>
                <input style={s.input} type="number" min={1} max={3} value={form.priority} onChange={e => setForm(f => ({ ...f, priority: e.target.value }))} />
              </div>
            </div>
            <div style={s.modalActions}>
              <button style={s.btnSecondary} onClick={() => setShowCreateModal(false)}>取消</button>
              <button style={s.btnPrimary} disabled={submitting} onClick={handleCreate}>
                {submitting ? '提交中...' : '创建'}
              </button>
            </div>
          </div>
        </div>
      )}

      {detailOrder && (
        <div style={s.overlay} onClick={closeDetail}>
          <div style={s.modal} onClick={e => e.stopPropagation()}>
            <div style={s.modalTitle}>工单详情 - {detailOrder.orderNo}</div>
            <div style={s.detailTabs}>
              <button style={s.detailTab(auditTab === 'info')} onClick={() => setAuditTab('info')}>基本信息</button>
              <button style={s.detailTab(auditTab === 'audit')} onClick={() => setAuditTab('audit')}>审计日志</button>
            </div>
            {auditTab === 'info' ? (
              <div style={s.detailGrid}>
                {detailFields.map(key => (
                  <div key={key} style={s.detailItem}>
                    <div style={s.detailLabel}>{FieldLabel[key] || key}</div>
                    <div style={s.detailValue}>{renderFieldValue(key, detailOrder)}</div>
                  </div>
                ))}
              </div>
            ) : (
              <div>
                {auditLog.length === 0 ? (
                  <div style={{ textAlign: 'center', padding: '32px', fontSize: '14px', color: '#9ca3af' }}>暂无审计日志</div>
                ) : (
                  <div style={s.timeline}>
                    <div style={s.timelineLine} />
                    {auditLog.map((entry, i) => (
                      <div key={i} style={s.timelineItem}>
                        <div style={s.timelineDot} />
                        <div style={s.timelineTime}>{formatDateTime(String(entry.timestamp || entry.time || entry.createdAt || ''))}</div>
                        <div style={s.timelineAction}>{String(entry.action || entry.operation || entry.event || '')}</div>
                        {((entry.operator as string) || (entry.operatedBy as string)) && (
                          <div style={s.timelineDetail}>操作人: {String(entry.operator || entry.operatedBy || '')}</div>
                        )}
                        {(entry.detail as string) && (
                          <div style={s.timelineDetail}>{String(entry.detail)}</div>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}
            <div style={s.modalActions}>
              <button style={s.btnSecondary} onClick={closeDetail}>关闭</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
