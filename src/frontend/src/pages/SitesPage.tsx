import { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { fetchSites, createSite, updateSite, deleteSite } from '../api/endpoints'
import { useAuth } from '../hooks/useAuth'
import type { ISite } from '../api/endpoints'

const s = {
  page: { padding: '32px', maxWidth: '1400px', margin: '0 auto', fontFamily: 'Inter, system-ui, sans-serif' },
  header: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' },
  title: { fontSize: '24px', fontWeight: 600, color: '#08060d', margin: 0 },
  btn: (bg: string, color: string) => ({ display: 'inline-flex', alignItems: 'center', gap: '6px', padding: '8px 16px', borderRadius: '8px', fontSize: '14px', fontWeight: 500, border: 'none', cursor: 'pointer', background: bg, color }),
  card: { background: '#fff', borderRadius: '12px', boxShadow: '0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.04)', overflow: 'hidden' },
  table: { width: '100%', borderCollapse: 'collapse' as const },
  th: { textAlign: 'left' as const, padding: '12px 16px', fontSize: '12px', fontWeight: 600, color: '#6b7280', borderBottom: '2px solid #e5e7eb', textTransform: 'uppercase' as const, letterSpacing: '0.05em', background: '#f9fafb' },
  td: { padding: '12px 16px', fontSize: '14px', color: '#111827', borderBottom: '1px solid #f3f4f6' },
  badge: (color: string, bg: string) => ({ display: 'inline-block', padding: '2px 10px', borderRadius: '9999px', fontSize: '12px', fontWeight: 500, color, background: bg }),
  actionBtn: (c: string) => ({ padding: '4px 10px', borderRadius: '6px', fontSize: '13px', fontWeight: 500, border: 'none', cursor: 'pointer', background: `${c}15`, color: c, marginRight: '6px' }),
  overlay: { position: 'fixed' as const, inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, fontFamily: 'Inter, system-ui, sans-serif' },
  modal: { background: '#fff', borderRadius: '16px', padding: '32px', width: '480px', maxWidth: '90vw', boxShadow: '0 25px 50px rgba(0,0,0,0.25)' },
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
  toolbar: { display: 'flex', gap: '12px', marginBottom: '16px', alignItems: 'center' as const },
  searchInput: { padding: '8px 12px', borderRadius: '8px', border: '1px solid #d1d5db', fontSize: '14px', outline: 'none', width: '240px' },
  pagination: { display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '4px', padding: '16px' },
  pageBtn: (active: boolean) => ({ padding: '6px 12px', borderRadius: '6px', fontSize: '14px', fontWeight: active ? 600 : 400, border: active ? '1px solid #533afd' : 'none', cursor: 'pointer', background: active ? '#533afd' : 'transparent', color: active ? '#fff' : '#374151' }),
  pageArrow: (disabled: boolean) => ({ padding: '6px 12px', borderRadius: '6px', fontSize: '14px', fontWeight: 500, border: 'none', cursor: disabled ? 'default' : 'pointer', background: 'transparent', color: disabled ? '#d1d5db' : '#374151' }),
}

export default function SitesPage() {
  const navigate = useNavigate()
  const { isAuthenticated, token } = useAuth()
  const [sites, setSites] = useState<ISite[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<any>(null)
  const [form, setForm] = useState({ name: '', address: '', contactName: '', contactPhone: '', status: 'active' })
  const [deleteTarget, setDeleteTarget] = useState<any>(null)

  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const size = 10
  const [searchInput, setSearchInput] = useState('')
  const [search, setSearch] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const debounceRef = useRef<ReturnType<typeof setTimeout>>()

  function loadSites() {
    setLoading(true)
    setError('')
    const params: Record<string, unknown> = { page, size }
    if (search) params.name = search
    if (statusFilter) params.status = statusFilter
    fetchSites(params)
      .then(res => {
        setSites(res.data.content)
        setTotalPages(Math.ceil(res.data.totalElements / size) || 1)
      })
      .catch(err => setError(err.message || '加载站点列表失败'))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    loadSites()
  }, [token, page, search, statusFilter])

  function openCreate() {
    setEditing(null)
    setForm({ name: '', address: '', contactName: '', contactPhone: '', status: 'active' })
    setModalOpen(true)
  }

  function openEdit(site: ISite) {
    setEditing(site)
    setForm({
      name: site.name,
      address: site.address || '',
      contactName: site.contactName || '',
      contactPhone: site.contactPhone || '',
      status: site.status,
    })
    setModalOpen(true)
  }

  function handleSave() {
    const data = { ...form }
    if (editing) {
      updateSite(editing.id, data).then(res => {
        setSites(prev => prev.map(s => s.id === editing.id ? res.data : s))
        setModalOpen(false)
      }).catch(err => {
        setError(err.message || '更新站点失败')
        setModalOpen(false)
      })
    } else {
      createSite(data).then(res => {
        setSites(prev => [...prev, res.data])
        setModalOpen(false)
      }).catch(err => {
        setError(err.message || '创建站点失败')
        setModalOpen(false)
      })
    }
  }

  function handleDelete() {
    if (!deleteTarget) return
    deleteSite(deleteTarget.id).then(() => {
      setSites(prev => prev.filter(s => s.id !== deleteTarget.id))
      setDeleteTarget(null)
    }).catch(err => {
      setError(err.message || '删除站点失败')
      setDeleteTarget(null)
    })
  }

  function renderPagination() {
    const pages: React.ReactNode[] = []
    const maxVisible = 5
    let start = Math.max(0, page - Math.floor(maxVisible / 2))
    let end = Math.min(totalPages, start + maxVisible)
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

  if (loading && sites.length === 0) {
    return (
      <div style={s.page}>
        <div style={s.header}><h1 style={s.title}>站点管理</h1></div>
        <div style={s.loading}>加载中...</div>
      </div>
    )
  }

  if (error && sites.length === 0) {
    return (
      <div style={s.page}>
        <div style={s.header}><h1 style={s.title}>站点管理</h1></div>
        <div style={s.error}>{error}</div>
      </div>
    )
  }

  return (
    <div style={s.page}>
      <div style={s.header}>
        <div>
          <h1 style={s.title}>站点管理</h1>
          <p style={{ fontSize: '14px', color: '#6b7280', margin: '4px 0 0 0' }}>管理所有运营站点信息</p>
        </div>
        <button style={s.btn('#533afd', '#fff')} onClick={openCreate}>+ 新增站点</button>
      </div>

      <div style={s.toolbar}>
        <input style={s.searchInput} placeholder="搜索站点名称..." value={searchInput} onChange={e => { setSearchInput(e.target.value); setPage(0); if (debounceRef.current) clearTimeout(debounceRef.current); debounceRef.current = setTimeout(() => setSearch(e.target.value), 300) }} />
        <select style={{ ...s.select, width: '140px' }} value={statusFilter} onChange={e => { setStatusFilter(e.target.value); setPage(0) }}>
          <option value="">全部状态</option>
          <option value="active">运营中</option>
          <option value="inactive">已停用</option>
        </select>
      </div>

      <div style={s.card}>
        <table style={s.table}>
          <thead>
            <tr>
              <th style={s.th}>站点名称</th>
              <th style={s.th}>地址</th>
              <th style={s.th}>联系人</th>
              <th style={s.th}>状态</th>
              <th style={s.th}>操作</th>
            </tr>
          </thead>
          <tbody>
            {sites.map(site => (
              <tr key={site.id} onClick={() => navigate(`/sites/${site.id}`)} style={{ cursor: 'pointer' }}>
                <td style={{ ...s.td, fontWeight: 500 }}>{site.name}</td>
                <td style={{ ...s.td, color: '#6b7280' }}>{site.address || '-'}</td>
                <td style={s.td}>{site.contactName || '-'}</td>
                <td style={s.td}>
                  <span style={site.status === 'active' ? s.badge('#059669', '#ecfdf5') : s.badge('#9ca3af', '#f3f4f6')}>
                    {site.status === 'active' ? '运营中' : '已停用'}
                  </span>
                </td>
                <td style={s.td} onClick={e => e.stopPropagation()}>
                  <button style={s.actionBtn('#533afd')} onClick={() => openEdit(site)}>编辑</button>
                  <button style={s.actionBtn('#dc2626')} onClick={() => setDeleteTarget(site)}>删除</button>
                </td>
              </tr>
            ))}
            {sites.length === 0 && (
              <tr><td colSpan={5} style={s.empty}>暂无站点数据</td></tr>
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
            <h2 style={s.modalTitle}>{editing ? '编辑站点' : '新增站点'}</h2>
            <div style={s.field}>
              <label style={s.label}>站点名称</label>
              <input style={s.input} value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} placeholder="请输入站点名称" />
            </div>
            <div style={s.field}>
              <label style={s.label}>地址</label>
              <input style={s.input} value={form.address} onChange={e => setForm(f => ({ ...f, address: e.target.value }))} placeholder="请输入地址" />
            </div>
            <div style={s.field}>
              <label style={s.label}>联系人</label>
              <input style={s.input} value={form.contactName} onChange={e => setForm(f => ({ ...f, contactName: e.target.value }))} placeholder="请输入联系人" />
            </div>
            <div style={s.field}>
              <label style={s.label}>联系电话</label>
              <input style={s.input} value={form.contactPhone} onChange={e => setForm(f => ({ ...f, contactPhone: e.target.value }))} placeholder="请输入联系电话" />
            </div>
            <div style={s.field}>
              <label style={s.label}>状态</label>
              <select style={s.select} value={form.status} onChange={e => setForm(f => ({ ...f, status: e.target.value }))}>
                <option value="active">运营中</option>
                <option value="inactive">已停用</option>
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
            <p style={s.confirmText}>确定要删除站点「{deleteTarget.name}」吗？此操作不可撤销。</p>
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
