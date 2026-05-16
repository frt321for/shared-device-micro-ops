import { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { fetchDevices, fetchDeviceTypes, createDevice, updateDevice, deleteDevice, fetchSites } from '../api/endpoints'
import { useAuth } from '../hooks/useAuth'
import type { IDevice, IDeviceType, ISite } from '../api/endpoints'

const s = {
  page: { padding: '32px', maxWidth: '1400px', margin: '0 auto', fontFamily: 'Inter, system-ui, sans-serif' },
  header: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' },
  title: { fontSize: '24px', fontWeight: 600, color: '#08060d', margin: 0 },
  sub: { fontSize: '14px', color: '#6b7280', margin: '4px 0 0 0' },
  btn: (bg: string, color: string) => ({ display: 'inline-flex', alignItems: 'center', gap: '6px', padding: '8px 16px', borderRadius: '8px', fontSize: '14px', fontWeight: 500, border: 'none', cursor: 'pointer', background: bg, color }),
  card: { background: '#fff', borderRadius: '12px', boxShadow: '0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.04)', overflow: 'hidden' },
  table: { width: '100%', borderCollapse: 'collapse' as const },
  th: { textAlign: 'left' as const, padding: '12px 16px', fontSize: '12px', fontWeight: 600, color: '#6b7280', borderBottom: '2px solid #e5e7eb', textTransform: 'uppercase' as const, letterSpacing: '0.05em', background: '#f9fafb' },
  td: { padding: '12px 16px', fontSize: '14px', color: '#111827', borderBottom: '1px solid #f3f4f6' },
  badge: (color: string, bg: string) => ({ display: 'inline-block', padding: '2px 10px', borderRadius: '9999px', fontSize: '12px', fontWeight: 500, color, background: bg }),
  actionBtn: (c: string, disabled = false) => ({ padding: '4px 10px', borderRadius: '6px', fontSize: '13px', fontWeight: 500, border: 'none', cursor: disabled ? 'not-allowed' : 'pointer', background: disabled ? '#f3f4f6' : `${c}15`, color: disabled ? '#d1d5db' : c, marginRight: '6px' }),
  overlay: { position: 'fixed' as const, inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, fontFamily: 'Inter, system-ui, sans-serif' },
  modal: { background: '#fff', borderRadius: '16px', padding: '32px', width: '520px', maxWidth: '90vw', boxShadow: '0 25px 50px rgba(0,0,0,0.25)' },
  modalTitle: { fontSize: '18px', fontWeight: 600, color: '#08060d', margin: '0 0 20px 0' },
  field: { marginBottom: '16px' },
  label: { display: 'block', fontSize: '14px', fontWeight: 500, color: '#374151', marginBottom: '6px' },
  input: { width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid #d1d5db', fontSize: '14px', outline: 'none', boxSizing: 'border-box' as const },
  select: { width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid #d1d5db', fontSize: '14px', outline: 'none', boxSizing: 'border-box' as const, background: '#fff' },
  modalActions: { display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '24px' },
  confirmText: { fontSize: '14px', color: '#6b7280', margin: '8px 0 0 0' },
  empty: { textAlign: 'center' as const, padding: '48px', color: '#9ca3af', fontSize: '14px' },
  loading: { textAlign: 'center' as const, padding: '48px', color: '#6b7280', fontSize: '14px' },
  error: { textAlign: 'center' as const, padding: '48px', color: '#dc2626', fontSize: '14px' },
  row: { display: 'flex', gap: '16px' },
  half: { flex: 1 },
  toolbar: { display: 'flex', gap: '12px', marginBottom: '16px', alignItems: 'center' as const },
  searchInput: { padding: '8px 12px', borderRadius: '8px', border: '1px solid #d1d5db', fontSize: '14px', outline: 'none', width: '200px' },
  pagination: { display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '4px', padding: '16px' },
  pageBtn: (active: boolean) => ({ padding: '6px 12px', borderRadius: '6px', fontSize: '14px', fontWeight: active ? 600 : 400, border: active ? '1px solid #533afd' : 'none', cursor: 'pointer', background: active ? '#533afd' : 'transparent', color: active ? '#fff' : '#374151' }),
  pageArrow: (disabled: boolean) => ({ padding: '6px 12px', borderRadius: '6px', fontSize: '14px', fontWeight: 500, border: 'none', cursor: disabled ? 'default' : 'pointer', background: 'transparent', color: disabled ? '#d1d5db' : '#374151' }),
}

const statusStyle: Record<string, [string, string]> = { online: ['#059669', '#ecfdf5'], offline: ['#9ca3af', '#f3f4f6'], faulty: ['#dc2626', '#fee2e2'], retired: ['#6b7280', '#f3f4f6'] }
const statusLabel: Record<string, string> = { online: '在线', offline: '离线', faulty: '故障', retired: '停用' }

export default function DevicesPage() {
  const navigate = useNavigate()
  const { token } = useAuth()
  const [devices, setDevices] = useState<IDevice[]>([])
  const [deviceTypes, setDeviceTypes] = useState<IDeviceType[]>([])
  const [siteList, setSiteList] = useState<ISite[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<IDevice | null>(null)
  const [form, setForm] = useState({ deviceCode: '', name: '', deviceTypeId: 0, status: 'online', siteId: 0 })
  const [deleteTarget, setDeleteTarget] = useState<IDevice | null>(null)

  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const size = 10
  const [searchInput, setSearchInput] = useState('')
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const debounceRef = useRef<ReturnType<typeof setTimeout> | undefined>(undefined)
  const [siteFilter, setSiteFilter] = useState(0)

  function loadDevices() {
    setLoading(true)
    setError('')
    const params: Record<string, unknown> = { page, size }
    if (search) params.name = search
    if (statusFilter) params.status = statusFilter
    if (siteFilter) params.siteId = siteFilter
    Promise.all([
      fetchDevices(params),
      fetchDeviceTypes(),
      fetchSites(),
    ]).then(([devRes, dtRes, siteRes]) => {
      setDevices(devRes.data.content)
      setDeviceTypes(dtRes.data)
      setSiteList(siteRes.data.content)
      setTotalPages(Math.ceil(devRes.data.totalElements / size) || 1)
    }).catch(err => {
      setError(err.message || '加载设备列表失败')
    }).finally(() => setLoading(false))
  }

  useEffect(() => {
    loadDevices()
  }, [token, page, search, statusFilter, siteFilter])

  function openCreate() {
    setEditing(null)
    setForm({ deviceCode: '', name: '', deviceTypeId: deviceTypes[0]?.id || 0, status: 'online', siteId: siteList[0]?.id || 0 })
    setModalOpen(true)
  }

  function openEdit(dev: IDevice) {
    setEditing(dev)
    setForm({ deviceCode: dev.deviceCode, name: dev.name, deviceTypeId: dev.deviceTypeId, status: dev.status, siteId: dev.siteId || 0 })
    setModalOpen(true)
  }

  function handleSave() {
    const data = { ...form }
    if (editing) {
      updateDevice(editing.id, data).then(res => {
        setDevices(prev => prev.map(d => d.id === editing.id ? res.data : d))
        setModalOpen(false)
      }).catch(err => {
        setError(err.message || '更新设备失败')
        setModalOpen(false)
      })
    } else {
      createDevice(data).then(res => {
        setDevices(prev => [...prev, res.data])
        setModalOpen(false)
      }).catch(err => {
        setError(err.message || '创建设备失败')
        setModalOpen(false)
      })
    }
  }

  function handleDelete() {
    if (!deleteTarget) return
    deleteDevice(deleteTarget.id).then(() => {
      setDevices(prev => prev.filter(d => d.id !== deleteTarget.id))
      setDeleteTarget(null)
    }).catch(err => {
      setError(err.message || '删除设备失败')
      setDeleteTarget(null)
    })
  }

  const typeMap = new Map(deviceTypes.map(dt => [dt.id, dt.name]))
  const siteMap = new Map(siteList.map(site => [site.id, site.name]))

  function renderPagination() {
    const pages: React.ReactNode[] = []
    const maxVisible = 5
    let start = Math.max(0, page - Math.floor(maxVisible / 2))
    const end = Math.min(totalPages, start + maxVisible)
    if (end - start < maxVisible) start = Math.max(0, end - maxVisible)

    pages.push(
      <button key="prev" style={s.pageArrow(page === 0)} disabled={page === 0} onClick={() => setPage(p => Math.max(0, p - 1))}>« Prev</button>
    )
    for (let i = start; i < end; i++) {
      pages.push(
        <button key={i} style={s.pageBtn(page === i)} onClick={() => setPage(i)}>{i + 1}</button>
      )
    }
    pages.push(
      <button key="next" style={s.pageArrow(page >= totalPages - 1)} disabled={page >= totalPages - 1} onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}>Next »</button>
    )
    return pages
  }

  if (loading && devices.length === 0) {
    return (
      <div style={s.page}>
        <div style={s.header}><h1 style={s.title}>设备管理</h1></div>
        <div style={s.loading}>加载中...</div>
      </div>
    )
  }

  if (error && devices.length === 0) {
    return (
      <div style={s.page}>
        <div style={s.header}><h1 style={s.title}>设备管理</h1></div>
        <div style={s.error}>{error}</div>
      </div>
    )
  }

  return (
    <div style={s.page}>
      <div style={s.header}>
        <div>
          <h1 style={s.title}>设备管理</h1>
          <p style={s.sub}>管理所有IoT设备信息与运行状态</p>
        </div>
        <button style={s.btn('#533afd', '#fff')} onClick={openCreate}>+ 新增设备</button>
      </div>

      <div style={s.toolbar}>
        <input style={s.searchInput} placeholder="搜索设备名称..." value={searchInput} onChange={e => { setSearchInput(e.target.value); setPage(0); if (debounceRef.current) clearTimeout(debounceRef.current); debounceRef.current = setTimeout(() => setSearch(e.target.value), 300) }} />
        <select style={{ ...s.select, width: '120px' }} value={statusFilter} onChange={e => { setStatusFilter(e.target.value); setPage(0) }}>
          <option value="">全部状态</option>
          <option value="online">在线</option>
          <option value="offline">离线</option>
          <option value="faulty">故障</option>
          <option value="retired">停用</option>
        </select>
        <select style={{ ...s.select, width: '160px' }} value={siteFilter} onChange={e => { setSiteFilter(parseInt(e.target.value) || 0); setPage(0) }}>
          <option value={0}>全部站点</option>
          {siteList.map(site => (
            <option key={site.id} value={site.id}>{site.name}</option>
          ))}
        </select>
      </div>

      <div style={s.card}>
        <table style={s.table}>
          <thead>
            <tr>
              <th style={s.th}>设备编号</th>
              <th style={s.th}>设备名称</th>
              <th style={s.th}>类型</th>
              <th style={s.th}>状态</th>
              <th style={s.th}>所属站点</th>
              <th style={s.th}>创建时间</th>
              <th style={s.th}>操作</th>
            </tr>
          </thead>
          <tbody>
            {devices.map(d => (
              <tr key={d.id} style={{ cursor: 'pointer' }} onClick={() => navigate(`/devices/${d.id}`)}>
                <td style={{ ...s.td, fontFamily: 'ui-monospace, monospace', fontSize: '13px' }}>{d.deviceCode}</td>
                <td style={{ ...s.td, fontWeight: 500 }}>{d.name}</td>
                <td style={s.td}>{typeMap.get(d.deviceTypeId) || '-'}</td>
                <td style={s.td}>
                  <span style={s.badge(...statusStyle[d.status] || ['#6b7280', '#f3f4f6'])}>
                    {statusLabel[d.status] || d.status}
                  </span>
                </td>
                <td style={s.td}>{d.siteId ? siteMap.get(d.siteId) || '-' : '-'}</td>
                <td style={{ ...s.td, color: '#6b7280', fontSize: '13px' }}>{new Date(d.createdAt).toLocaleString()}</td>
                <td style={s.td} onClick={e => e.stopPropagation()}>
                  <button style={s.actionBtn('#533afd', d.status === 'retired')} disabled={d.status === 'retired'} onClick={() => openEdit(d)}>编辑</button>
                  <button style={s.actionBtn('#dc2626', d.status === 'retired')} disabled={d.status === 'retired'} onClick={() => setDeleteTarget(d)}>删除</button>
                </td>
              </tr>
            ))}
            {devices.length === 0 && (
              <tr><td colSpan={7} style={s.empty}>暂无设备数据</td></tr>
            )}
          </tbody>
        </table>
        {totalPages > 1 && (
          <div style={s.pagination}>{renderPagination()}</div>
        )}
      </div>

      {modalOpen && (
        <div style={s.overlay} onClick={() => setModalOpen(false)}>
          <div style={s.modal} onClick={e => e.stopPropagation()}>
            <h2 style={s.modalTitle}>{editing ? '编辑设备' : '新增设备'}</h2>
            <div style={s.row}>
              <div style={s.half}>
                <div style={s.field}>
                  <label style={s.label}>设备编号</label>
                  <input style={s.input} value={form.deviceCode} onChange={e => setForm(f => ({ ...f, deviceCode: e.target.value }))} placeholder="DEV-XXX" />
                </div>
              </div>
              <div style={s.half}>
                <div style={s.field}>
                  <label style={s.label}>设备名称</label>
                  <input style={s.input} value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} placeholder="请输入名称" />
                </div>
              </div>
            </div>
            <div style={s.row}>
              <div style={s.half}>
                <div style={s.field}>
                  <label style={s.label}>设备类型</label>
                  <select style={s.select} value={form.deviceTypeId} onChange={e => setForm(f => ({ ...f, deviceTypeId: parseInt(e.target.value) || 0 }))}>
                    {deviceTypes.map(dt => (
                      <option key={dt.id} value={dt.id}>{dt.name}</option>
                    ))}
                  </select>
                </div>
              </div>
              <div style={s.half}>
                <div style={s.field}>
                  <label style={s.label}>状态</label>
                  <select style={s.select} value={form.status} onChange={e => setForm(f => ({ ...f, status: e.target.value }))}>
                    <option value="online">在线</option>
                    <option value="offline">离线</option>
                    <option value="faulty">故障</option>
                    <option value="retired">停用</option>
                  </select>
                </div>
              </div>
            </div>
            <div style={s.field}>
              <label style={s.label}>所属站点</label>
              <select style={s.select} value={form.siteId} onChange={e => setForm(f => ({ ...f, siteId: parseInt(e.target.value) || 0 }))}>
                <option value={0}>请选择站点</option>
                {siteList.map(site => (
                  <option key={site.id} value={site.id}>{site.name}</option>
                ))}
              </select>
            </div>
            <div style={s.modalActions}>
              <button style={s.btn('#f3f4f6', '#374151')} onClick={() => setModalOpen(false)}>取消</button>
              <button style={s.btn('#533afd', '#fff')} onClick={handleSave}>保存</button>
            </div>
          </div>
        </div>
      )}

      {deleteTarget && (
        <div style={s.overlay} onClick={() => setDeleteTarget(null)}>
          <div style={{ ...s.modal, width: '400px' }} onClick={e => e.stopPropagation()}>
            <h2 style={s.modalTitle}>确认删除</h2>
            <p style={s.confirmText}>确定要删除设备「{deleteTarget.name}」吗？此操作不可撤销。</p>
            <div style={s.modalActions}>
              <button style={s.btn('#f3f4f6', '#374151')} onClick={() => setDeleteTarget(null)}>取消</button>
              <button style={s.btn('#dc2626', '#fff')} onClick={handleDelete}>确认删除</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
