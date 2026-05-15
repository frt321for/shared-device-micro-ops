import { useState, useEffect, useRef } from 'react'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { fetchRoutes, createRoute } from '../api/endpoints'
import { useAuth } from '../hooks/useAuth'
import type { IRoute } from '../api/endpoints'

delete (L.Icon.Default.prototype as any)._getIconUrl
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon-2x.png',
  iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png',
  shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
})

interface RouteStop {
  latitude: number
  longitude: number
  name?: string
}

type RouteWithStops = IRoute & { stops?: RouteStop[] }

const s = {
  page: { padding: '32px', maxWidth: '1400px', margin: '0 auto', fontFamily: 'Inter, system-ui, sans-serif' },
  header: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' },
  title: { fontSize: '24px', fontWeight: 600, color: '#08060d', margin: 0 },
  sub: { fontSize: '14px', color: '#6b7280', margin: '4px 0 0 0' },
  btn: (bg: string, color: string) => ({ display: 'inline-flex', alignItems: 'center', gap: '6px', padding: '8px 16px', borderRadius: '8px', fontSize: '14px', fontWeight: 500, border: 'none', cursor: 'pointer', background: bg, color }),
  grid: { display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(380px, 1fr))', gap: '20px' },
  routeCard: { background: '#fff', borderRadius: '12px', boxShadow: '0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.04)', padding: '20px' },
  routeName: { fontSize: '16px', fontWeight: 600, color: '#08060d', marginBottom: '8px' },
  badge: (color: string, bg: string) => ({ display: 'inline-block', padding: '2px 10px', borderRadius: '9999px', fontSize: '12px', fontWeight: 500, color, background: bg }),
  overlay: { position: 'fixed' as const, inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000, fontFamily: 'Inter, system-ui, sans-serif' },
  modal: { background: '#fff', borderRadius: '16px', padding: '32px', width: '480px', maxWidth: '90vw', boxShadow: '0 25px 50px rgba(0,0,0,0.25)' },
  modalTitle: { fontSize: '18px', fontWeight: 600, color: '#08060d', margin: '0 0 20px 0' },
  field: { marginBottom: '16px' },
  label: { display: 'block', fontSize: '14px', fontWeight: 500, color: '#374151', marginBottom: '6px' },
  input: { width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid #d1d5db', fontSize: '14px', outline: 'none', boxSizing: 'border-box' as const },
  modalActions: { display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '24px' },
  empty: { textAlign: 'center' as const, padding: '48px', color: '#9ca3af', fontSize: '14px' },
  loading: { textAlign: 'center' as const, padding: '48px', color: '#6b7280', fontSize: '14px' },
  error: { textAlign: 'center' as const, padding: '48px', color: '#dc2626', fontSize: '14px' },
  metaRow: { display: 'flex', gap: '16px', fontSize: '13px', color: '#6b7280', marginBottom: '8px' },
  metaLabel: { color: '#9ca3af' },
}

const statusLabel: Record<string, string> = { planned: '待执行', in_progress: '进行中', completed: '已完成' }
const statusBadge: Record<string, [string, string]> = { planned: ['#f59e0b', '#fef3c7'], in_progress: ['#533afd', '#ede9fe'], completed: ['#059669', '#ecfdf5'] }

function RouteMap({ routes }: { routes: RouteWithStops[] }) {
  const mapRef = useRef<HTMLDivElement>(null)
  const mapInstance = useRef<L.Map | null>(null)

  useEffect(() => {
    if (!mapRef.current || mapInstance.current) return
    const map = L.map(mapRef.current).setView([39.9042, 116.4074], 11)
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors',
    }).addTo(map)
    mapInstance.current = map

    return () => {
      map.remove()
      mapInstance.current = null
    }
  }, [])

  useEffect(() => {
    const map = mapInstance.current
    if (!map) return

    const markers: L.Marker[] = []
    const lines: L.Polyline[] = []
    const bounds: L.LatLngBoundsExpression[] = []

    for (const route of routes) {
      if (!route.stops || route.stops.length < 2) continue
      const latlngs = route.stops.map(s => [s.latitude, s.longitude] as [number, number])
      const line = L.polyline(latlngs, { color: '#533afd', weight: 3, opacity: 0.7 }).addTo(map)
      lines.push(line)
      bounds.push(...latlngs.map(ll => ll as unknown as L.LatLngBoundsExpression))

      route.stops.forEach((stop, i) => {
        const marker = L.marker([stop.latitude, stop.longitude])
          .bindPopup(stop.name || `停靠点 ${i + 1}`)
          .addTo(map)
        markers.push(marker)
      })
    }

    if (bounds.length > 0) {
      map.fitBounds(bounds as any, { padding: [50, 50] })
    }

    return () => {
      markers.forEach(m => m.remove())
      lines.forEach(l => l.remove())
    }
  }, [routes])

  return (
    <div style={{ marginBottom: '24px' }}>
      <div ref={mapRef} style={{ height: '400px', borderRadius: '12px', overflow: 'hidden' }} />
    </div>
  )
}

export default function RoutePage() {
  const { isAuthenticated, token } = useAuth()
  const [routes, setRoutes] = useState<IRoute[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [modalOpen, setModalOpen] = useState(false)
  const [workOrderIds, setWorkOrderIds] = useState('')
  const [assigneeId, setAssigneeId] = useState(0)

  useEffect(() => {
    setLoading(true)
    setError('')
    fetchRoutes()
      .then(res => setRoutes(res.data.content))
      .catch(err => setError(err.message || '加载路线列表失败'))
      .finally(() => setLoading(false))
  }, [token])

  function handleCreate() {
    const ids = workOrderIds.split(',').map(s => parseInt(s.trim())).filter(n => !isNaN(n))
    createRoute({ workOrderIds: ids, assigneeId }).then(res => {
      setRoutes(prev => [...prev, res.data])
      setModalOpen(false)
      setWorkOrderIds('')
      setAssigneeId(0)
    }).catch(err => {
      setError(err.message || '创建路线失败')
      setModalOpen(false)
    })
  }

  if (loading) {
    return (
      <div style={s.page}>
        <div style={s.header}><h1 style={s.title}>配送路线</h1></div>
        <div style={s.loading}>加载中...</div>
      </div>
    )
  }

  if (error) {
    return (
      <div style={s.page}>
        <div style={s.header}><h1 style={s.title}>配送路线</h1></div>
        <div style={s.error}>{error}</div>
      </div>
    )
  }

  return (
    <div style={s.page}>
      <div style={s.header}>
        <div>
          <h1 style={s.title}>配送路线</h1>
          <p style={s.sub}>管理配送路线与调度信息</p>
        </div>
        <button style={s.btn('#533afd', '#fff')} onClick={() => setModalOpen(true)}>+ 创建路线</button>
      </div>

      <RouteMap routes={routes as RouteWithStops[]} />

      <div style={s.grid}>
        {routes.map(route => (
          <div key={route.id} style={s.routeCard}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '12px' }}>
              <div style={s.routeName}>{route.name}</div>
              <span style={s.badge(...statusBadge[route.status] || ['#6b7280', '#f3f4f6'])}>
                {statusLabel[route.status] || route.status}
              </span>
            </div>
            <div style={s.metaRow}>
              <span><span style={s.metaLabel}>指派人ID:</span> {route.assigneeId ?? '-'}</span>
            </div>
            {route.totalDistance != null && (
              <div style={s.metaRow}>
                <span><span style={s.metaLabel}>距离:</span> {(route.totalDistance / 1000).toFixed(1)} km</span>
              </div>
            )}
            {route.estimatedMinutes != null && (
              <div style={s.metaRow}>
                <span><span style={s.metaLabel}>预计耗时:</span> {route.estimatedMinutes} 分钟</span>
              </div>
            )}
          </div>
        ))}
        {routes.length === 0 && (
          <div style={s.empty}>暂无路线数据</div>
        )}
      </div>

      {modalOpen && (
        <div style={s.overlay} onClick={() => setModalOpen(false)}>
          <div style={s.modal} onClick={e => e.stopPropagation()}>
            <h2 style={s.modalTitle}>创建路线</h2>
            <div style={s.field}>
              <label style={s.label}>工单ID（逗号分隔）</label>
              <input style={s.input} value={workOrderIds} onChange={e => setWorkOrderIds(e.target.value)} placeholder="例如: 1,2,3" />
            </div>
            <div style={s.field}>
              <label style={s.label}>指派人ID</label>
              <input style={s.input} type="number" value={assigneeId} onChange={e => setAssigneeId(parseInt(e.target.value) || 0)} placeholder="指派人ID" />
            </div>
            <div style={s.modalActions}>
              <button style={s.btn('#f3f4f6', '#374151')} onClick={() => setModalOpen(false)}>取消</button>
              <button style={s.btn('#533afd', '#fff')} onClick={handleCreate}>创建</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
