import { useState, useEffect } from 'react'
import { fetchSkus, createSku, updateSku, fetchDeviceStock, fetchWarehouseStock } from '../api/endpoints'
import { useAuth } from '../hooks/useAuth'
import type { ISku, IDeviceStock, IWarehouseStock } from '../api/endpoints'

const s = {
  page: { padding: '32px', maxWidth: '1400px', margin: '0 auto', fontFamily: 'Inter, system-ui, sans-serif' },
  header: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' },
  title: { fontSize: '24px', fontWeight: 600, color: '#08060d', margin: 0 },
  sub: { fontSize: '14px', color: '#6b7280', margin: '4px 0 0 0' },
  btn: (bg: string, color: string) => ({ display: 'inline-flex', alignItems: 'center', gap: '6px', padding: '8px 16px', borderRadius: '8px', fontSize: '14px', fontWeight: 500, border: 'none', cursor: 'pointer', background: bg, color }),
  tabs: { display: 'flex', gap: '4px', marginBottom: '24px', background: '#f3f4f6', borderRadius: '10px', padding: '4px', width: 'fit-content' },
  tab: (active: boolean) => ({ padding: '8px 20px', borderRadius: '8px', fontSize: '14px', fontWeight: 500, border: 'none', cursor: 'pointer', background: active ? '#fff' : 'transparent', color: active ? '#08060d' : '#6b7280', boxShadow: active ? '0 1px 3px rgba(0,0,0,0.1)' : 'none' }),
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
  empty: { textAlign: 'center' as const, padding: '48px', color: '#9ca3af', fontSize: '14px' },
  loading: { textAlign: 'center' as const, padding: '48px', color: '#6b7280', fontSize: '14px' },
  error: { textAlign: 'center' as const, padding: '48px', color: '#dc2626', fontSize: '14px' },
  stockGrid: { display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '16px' },
  stockCard: { background: '#fff', borderRadius: '12px', boxShadow: '0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.04)', padding: '20px' },
  stockName: { fontSize: '15px', fontWeight: 600, color: '#08060d', marginBottom: '12px' },
  barRow: { marginBottom: '8px' },
  barLabel: { display: 'flex', justifyContent: 'space-between', fontSize: '13px', color: '#6b7280', marginBottom: '4px' },
  barTrack: { height: '8px', borderRadius: '4px', background: '#f3f4f6', overflow: 'hidden' },
  barFill: (pct: number, color: string) => ({ height: '100%', width: `${Math.min(pct, 100)}%`, borderRadius: '4px', background: color }),
  row: { display: 'flex', gap: '16px' },
  half: { flex: 1 },
}

const tabs = [
  { key: 'skus', label: 'SKU列表' },
  { key: 'device', label: '设备库存' },
  { key: 'warehouse', label: '仓库库存' },
]

export default function InventoryPage() {
  const { isAuthenticated, token } = useAuth()
  const [tab, setTab] = useState('skus')
  const [skus, setSkus] = useState<ISku[]>([])
  const [deviceStock, setDeviceStock] = useState<IDeviceStock[]>([])
  const [warehouse, setWarehouse] = useState<IWarehouseStock[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState<any>(null)
  const [form, setForm] = useState({ code: '', name: '', category: '饮品', unit: '瓶', sellingPrice: 0 })

  useEffect(() => {
    setLoading(true)
    setError('')
    Promise.all([
      fetchSkus(),
      fetchDeviceStock(),
      fetchWarehouseStock(),
    ]).then(([sRes, dRes, wRes]) => {
      setSkus(sRes.data.content)
      setDeviceStock(dRes.data)
      setWarehouse(wRes.data)
    }).catch(err => {
      setError(err.message || '加载库存数据失败')
    }).finally(() => setLoading(false))
  }, [token])

  function openCreate() {
    setEditing(null)
    setForm({ code: '', name: '', category: '饮品', unit: '瓶', sellingPrice: 0 })
    setModalOpen(true)
  }

  function openEdit(sku: ISku) {
    setEditing(sku)
    setForm({ code: sku.code, name: sku.name, category: sku.category || '饮品', unit: sku.unit || '瓶', sellingPrice: sku.sellingPrice || 0 })
    setModalOpen(true)
  }

  function handleSave() {
    const data = { ...form }
    if (editing) {
      updateSku(editing.id, data).then(res => {
        setSkus(prev => prev.map(s => s.id === editing.id ? res.data : s))
        setModalOpen(false)
      }).catch(err => {
        setError(err.message || '更新SKU失败')
        setModalOpen(false)
      })
    } else {
      createSku(data).then(res => {
        setSkus(prev => [...prev, res.data])
        setModalOpen(false)
      }).catch(err => {
        setError(err.message || '创建SKU失败')
        setModalOpen(false)
      })
    }
  }

  function currentStock(skuId: number): number {
    return deviceStock
      .filter(ds => ds.skuId === skuId)
      .reduce((sum, ds) => sum + ds.quantity, 0)
  }

  if (loading) {
    return (
      <div style={s.page}>
        <div style={s.header}><h1 style={s.title}>库存管理</h1></div>
        <div style={s.loading}>加载中...</div>
      </div>
    )
  }

  if (error) {
    return (
      <div style={s.page}>
        <div style={s.header}><h1 style={s.title}>库存管理</h1></div>
        <div style={s.error}>{error}</div>
      </div>
    )
  }

  return (
    <div style={s.page}>
      <div style={s.header}>
        <div>
          <h1 style={s.title}>库存管理</h1>
          <p style={s.sub}>管理商品SKU、设备库存与仓库库存</p>
        </div>
        {tab === 'skus' && (
          <button style={s.btn('#533afd', '#fff')} onClick={openCreate}>+ 新增SKU</button>
        )}
      </div>

      <div style={s.tabs}>
        {tabs.map(t => (
          <button key={t.key} style={s.tab(tab === t.key)} onClick={() => setTab(t.key)}>{t.label}</button>
        ))}
      </div>

      {tab === 'skus' && (
        <div style={s.card}>
          <table style={s.table}>
            <thead>
              <tr>
                <th style={s.th}>SKU编码</th>
                <th style={s.th}>名称</th>
                <th style={s.th}>分类</th>
                <th style={s.th}>单位</th>
                <th style={s.th}>售价</th>
                <th style={s.th}>设备库存</th>
                <th style={s.th}>操作</th>
              </tr>
            </thead>
            <tbody>
              {skus.map(sku => (
                <tr key={sku.id}>
                  <td style={{ ...s.td, fontFamily: 'ui-monospace, monospace', fontSize: '13px' }}>{sku.code}</td>
                  <td style={{ ...s.td, fontWeight: 500 }}>{sku.name}</td>
                  <td style={s.td}>{sku.category || '-'}</td>
                  <td style={s.td}>{sku.unit || '-'}</td>
                  <td style={s.td}>{sku.sellingPrice != null ? `¥${sku.sellingPrice.toFixed(1)}` : '-'}</td>
                  <td style={s.td}>
                    <span style={{ color: currentStock(sku.id) === 0 ? '#dc2626' : currentStock(sku.id) < 50 ? '#f59e0b' : '#059669', fontWeight: 500 }}>
                      {currentStock(sku.id)}
                    </span>
                  </td>
                  <td style={s.td}>
                    <button style={s.actionBtn('#533afd')} onClick={() => openEdit(sku)}>编辑</button>
                  </td>
                </tr>
              ))}
              {skus.length === 0 && (
                <tr><td colSpan={7} style={s.empty}>暂无SKU数据</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {tab === 'device' && (
        <div style={s.stockGrid}>
          {deviceStock.map(d => {
            const maxCap = d.maxCapacity || d.quantity
            const allocPct = maxCap > 0 ? (d.quantity / maxCap) * 100 : 0
            const availPct = maxCap > 0 ? ((maxCap - d.quantity) / maxCap) * 100 : 0
            return (
              <div key={d.id} style={s.stockCard}>
                <div style={s.stockName}>设备 #{d.deviceId} · SKU #{d.skuId}</div>
                <div style={{ fontSize: '13px', color: '#6b7280', marginBottom: '16px' }}>
                  {d.status === 'normal' ? '正常' : d.status === 'low' ? '低库存' : d.status === 'overstock' ? '超量' : d.status}
                </div>
                <div style={s.barRow}>
                  <div style={s.barLabel}><span>当前库存</span><span>{d.quantity}</span></div>
                  <div style={s.barTrack}><div style={s.barFill(allocPct, '#533afd')} /></div>
                </div>
                <div style={s.barRow}>
                  <div style={s.barLabel}><span>可用空间</span><span>{maxCap - d.quantity}</span></div>
                  <div style={s.barTrack}><div style={s.barFill(availPct, '#10b981')} /></div>
                </div>
                <div style={{ textAlign: 'right', fontSize: '13px', color: '#9ca3af', marginTop: '8px' }}>
                  阈值: {d.minThreshold} / 容量: {maxCap}
                </div>
              </div>
            )
          })}
          {deviceStock.length === 0 && (
            <div style={s.empty}>暂无设备库存数据</div>
          )}
        </div>
      )}

      {tab === 'warehouse' && (
        <div style={s.card}>
          <table style={s.table}>
            <thead>
              <tr>
                <th style={s.th}>SKU ID</th>
                <th style={s.th}>数量</th>
                <th style={s.th}>批次号</th>
              </tr>
            </thead>
            <tbody>
              {warehouse.map(w => (
                <tr key={w.id}>
                  <td style={{ ...s.td, fontWeight: 500 }}>SKU #{w.skuId}</td>
                  <td style={s.td}>{w.quantity.toLocaleString()}</td>
                  <td style={{ ...s.td, fontFamily: 'ui-monospace, monospace', fontSize: '13px' }}>{w.batchNo || '-'}</td>
                </tr>
              ))}
              {warehouse.length === 0 && (
                <tr><td colSpan={3} style={s.empty}>暂无仓库数据</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {modalOpen && (
        <div style={s.overlay} onClick={() => setModalOpen(false)}>
          <div style={s.modal} onClick={e => e.stopPropagation()}>
            <h2 style={s.modalTitle}>{editing ? '编辑SKU' : '新增SKU'}</h2>
            <div style={s.row}>
              <div style={s.half}>
                <div style={s.field}>
                  <label style={s.label}>SKU编码</label>
                  <input style={s.input} value={form.code} onChange={e => setForm(f => ({ ...f, code: e.target.value }))} placeholder="SKU-XXX" />
                </div>
              </div>
              <div style={s.half}>
                <div style={s.field}>
                  <label style={s.label}>名称</label>
                  <input style={s.input} value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} placeholder="商品名称" />
                </div>
              </div>
            </div>
            <div style={s.row}>
              <div style={s.half}>
                <div style={s.field}>
                  <label style={s.label}>分类</label>
                  <select style={s.select} value={form.category} onChange={e => setForm(f => ({ ...f, category: e.target.value }))}>
                    <option value="饮品">饮品</option>
                    <option value="零食">零食</option>
                    <option value="日用品">日用品</option>
                  </select>
                </div>
              </div>
              <div style={s.half}>
                <div style={s.field}>
                  <label style={s.label}>单位</label>
                  <select style={s.select} value={form.unit} onChange={e => setForm(f => ({ ...f, unit: e.target.value }))}>
                    <option value="瓶">瓶</option>
                    <option value="罐">罐</option>
                    <option value="袋">袋</option>
                    <option value="盒">盒</option>
                    <option value="块">块</option>
                    <option value="包">包</option>
                  </select>
                </div>
              </div>
            </div>
            <div style={s.field}>
              <label style={s.label}>售价 (¥)</label>
              <input style={s.input} type="number" value={form.sellingPrice} onChange={e => setForm(f => ({ ...f, sellingPrice: parseFloat(e.target.value) || 0 }))} />
            </div>
            <div style={s.modalActions}>
              <button style={s.btn('#f3f4f6', '#374151')} onClick={() => setModalOpen(false)}>取消</button>
              <button style={s.btn('#533afd', '#fff')} onClick={handleSave}>保存</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
