import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { fetchDevices, fetchDeviceStock, fetchDeviceTypes, fetchSkus, fetchDeviceTelemetry, fetchDeviceEventsList } from '../api/endpoints'
import type { IDevice, IDeviceStock, IDeviceType, ISku } from '../api/endpoints'
import ReactECharts from 'echarts-for-react'

interface ITelemetryPoint { timestamp: string; value: number }
interface IDeviceEvent { id: number; type: string; message: string; timestamp: string }

const fmtEvent = (ts: string) => {
  try {
    return new Date(ts).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
  } catch { return ts }
}

const eventColors: Record<string, string> = {
  info: '#3b82f6',
  warning: '#f59e0b',
  error: '#ef4444',
  heartbeat: '#10b981',
}

const s = {
  page: { padding: '32px', maxWidth: '1200px', margin: '0 auto', fontFamily: 'Inter, system-ui, sans-serif' },
  backBtn: { display: 'inline-flex', alignItems: 'center', gap: '6px', padding: '6px 12px', borderRadius: '8px', fontSize: '14px', fontWeight: 500, border: 'none', cursor: 'pointer', background: '#f3f4f6', color: '#374151', marginBottom: '16px' },
  header: { marginBottom: '24px' },
  title: { fontSize: '24px', fontWeight: 600, color: '#08060d', margin: 0, display: 'flex', alignItems: 'center', gap: '12px' },
  sub: { fontSize: '14px', color: '#6b7280', margin: '4px 0 0 0', display: 'flex', alignItems: 'center', gap: '8px' },
  badge: (color: string, bg: string) => ({ display: 'inline-block', padding: '2px 10px', borderRadius: '9999px', fontSize: '12px', fontWeight: 500, color, background: bg }),
  card: { background: '#fff', borderRadius: '12px', boxShadow: '0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.04)', padding: '20px' },
  cardTitle: { fontSize: '14px', fontWeight: 600, color: '#6b7280', margin: '0 0 16px 0', textTransform: 'uppercase' as const, letterSpacing: '0.05em' },
  grid2: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '24px', marginTop: '24px' },
  infoRow: { display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid #f3f4f6', fontSize: '14px' },
  infoLabel: { color: '#6b7280', fontWeight: 500 },
  infoValue: { color: '#111827', fontWeight: 400 },
  table: { width: '100%', borderCollapse: 'collapse' as const },
  th: { textAlign: 'left' as const, padding: '10px 12px', fontSize: '12px', fontWeight: 600, color: '#6b7280', borderBottom: '2px solid #e5e7eb', textTransform: 'uppercase' as const, letterSpacing: '0.05em', background: '#f9fafb' },
  td: { padding: '10px 12px', fontSize: '14px', color: '#111827', borderBottom: '1px solid #f3f4f6' },
  loading: { textAlign: 'center' as const, padding: '48px', color: '#6b7280', fontSize: '14px' },
  error: { textAlign: 'center' as const, padding: '48px', color: '#dc2626', fontSize: '14px' },
  empty: { textAlign: 'center' as const, padding: '32px', color: '#9ca3af', fontSize: '14px' },
  statusDot: (color: string) => ({ display: 'inline-block', width: '10px', height: '10px', borderRadius: '50%', background: color, marginRight: '8px' }),
  placeholder: { display: 'flex', flexDirection: 'column' as const, alignItems: 'center', justifyContent: 'center', padding: '48px', color: '#9ca3af', fontSize: '14px', gap: '12px' },
}

const statusConfig: Record<string, { color: string; bg: string; dot: string; label: string }> = {
  online: { color: '#059669', bg: '#ecfdf5', dot: '#10b981', label: '在线' },
  offline: { color: '#9ca3af', bg: '#f3f4f6', dot: '#9ca3af', label: '离线' },
  faulty: { color: '#dc2626', bg: '#fee2e2', dot: '#ef4444', label: '故障' },
}

function BackIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M19 12H5" /><polyline points="12 19 5 12 12 5" />
    </svg>
  )
}

function ChipIcon() {
  return (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#533afd" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
      <rect x="4" y="4" width="16" height="16" rx="2" /><line x1="9" y1="9" x2="15" y2="9" /><line x1="9" y1="13" x2="15" y2="13" /><line x1="9" y1="17" x2="13" y2="17" />
    </svg>
  )
}

function InfoIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="10" /><line x1="12" y1="16" x2="12" y2="12" /><line x1="12" y1="8" x2="12.01" y2="8" />
    </svg>
  )
}

function ActivityIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
    </svg>
  )
}

function PackageIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M16.5 9.4 7.55 4.24" /><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" /><polyline points="3.29 7 12 12 20.71 7" /><line x1="12" y1="22" x2="12" y2="12" />
    </svg>
  )
}

function ClockIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="10" /><polyline points="12 6 12 12 16 14" />
    </svg>
  )
}

export default function DeviceDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()

  const [device, setDevice] = useState<IDevice | null>(null)
  const [stockList, setStockList] = useState<IDeviceStock[]>([])
  const [deviceTypes, setDeviceTypes] = useState<IDeviceType[]>([])
  const [skuMap, setSkuMap] = useState<Map<number, ISku>>(new Map())
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [telemetryTemp, setTelemetryTemp] = useState<ITelemetryPoint[]>([])
  const [telemetryInventory, setTelemetryInventory] = useState<ITelemetryPoint[]>([])
  const [events, setEvents] = useState<IDeviceEvent[]>([])

  useEffect(() => {
    if (!id) return
    setLoading(true)
    setError('')

    Promise.all([
      fetchDevices({ page: 0, size: 1, id: parseInt(id) }),
      fetchDeviceStock(parseInt(id)),
      fetchDeviceTypes(),
      fetchSkus(),
    ])
      .then(([devRes, stockRes, dtRes, skuRes]) => {
        const found = devRes.data.content[0]
        if (!found) {
          setError('设备不存在')
          return
        }
        setDevice(found)
        setStockList(stockRes.data || [])
        setDeviceTypes(dtRes.data)
        setSkuMap(new Map((skuRes.data.content || []).map((sku: ISku) => [sku.id, sku])))
      })
      .catch(err => setError(err.message || '加载设备详情失败'))
      .finally(() => setLoading(false))
  }, [id])

  useEffect(() => {
    if (!id) return
    Promise.all([
      fetchDeviceTelemetry(parseInt(id), 'temperature'),
      fetchDeviceTelemetry(parseInt(id), 'inventory_1'),
      fetchDeviceEventsList(parseInt(id)),
    ]).then(([tempRes, invRes, evtsRes]) => {
      setTelemetryTemp((tempRes.data || []).map((e: unknown) => {
        const r = e as Record<string, unknown>
        return { timestamp: (r.time || r.createdAt) as string, value: (r.value as number) || 0 }
      }))
      setTelemetryInventory((invRes.data || []).map((e: unknown) => {
        const r = e as Record<string, unknown>
        return { timestamp: (r.time || r.createdAt) as string, value: (r.value as number) || 0 }
      }))
      setEvents((evtsRes.data || []).map((e: unknown) => {
        const r = e as Record<string, unknown>
        return {
          id: r.id as number,
          type: (r.eventType as string) || 'info',
          message: (r.eventData as string) || (r.severity as string) || '事件',
          timestamp: (r.occurredAt || r.createdAt) as string,
        }
      }))
    }).catch(() => {})
  }, [id])

  if (loading) {
    return (
      <div style={s.page}>
        <div style={s.loading}>加载中...</div>
      </div>
    )
  }

  if (error || !device) {
    return (
      <div style={s.page}>
        <button style={s.backBtn} onClick={() => navigate('/devices')}><BackIcon /> 返回设备列表</button>
        <div style={s.error}>{error || '设备不存在'}</div>
      </div>
    )
  }

  const stCfg = statusConfig[device.status] || statusConfig.offline
  const typeName = deviceTypes.find(dt => dt.id === device.deviceTypeId)?.name || '-'
  const stockStatusMap: Record<string, [string, string]> = {
    adequate: ['#059669', '#ecfdf5'],
    low_stock: ['#d97706', '#fef3c7'],
    out_of_stock: ['#dc2626', '#fee2e2'],
    overstock: ['#533afd', '#eef2ff'],
  }
  const stockStatusLabel: Record<string, string> = {
    adequate: '正常',
    low_stock: '不足',
    out_of_stock: '缺货',
    overstock: '溢库',
  }

  const fmt = (ts: string) => new Date(ts).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  const fmtEvt = fmtEvent

  const tempChartOption = {
    title: { text: '温度趋势', textStyle: { fontSize: 14, fontWeight: 600, color: '#374151' } },
    xAxis: { type: 'category' as const, data: telemetryTemp.map(p => fmt(p.timestamp)) },
    yAxis: { type: 'value' as const, name: '温度 (°C)' },
    series: [{ type: 'line' as const, data: telemetryTemp.map(p => p.value), smooth: true, lineStyle: { color: '#ef4444', width: 2 }, itemStyle: { color: '#ef4444' }, areaStyle: { color: 'rgba(239,68,68,0.1)' } }],
    tooltip: { trigger: 'axis' as const },
    grid: { left: '12%', right: '5%', top: '20%', bottom: '12%' },
  }

  const inventoryChartOption = {
    title: { text: '库存趋势 (SKU-1)', textStyle: { fontSize: 14, fontWeight: 600, color: '#374151' } },
    xAxis: { type: 'category' as const, data: telemetryInventory.map(p => fmt(p.timestamp)) },
    yAxis: { type: 'value' as const, name: '数量' },
    series: [{ type: 'line' as const, data: telemetryInventory.map(p => p.value), smooth: true, lineStyle: { color: '#533afd', width: 2 }, itemStyle: { color: '#533afd' }, areaStyle: { color: 'rgba(83,58,253,0.1)' } }],
    tooltip: { trigger: 'axis' as const },
    grid: { left: '12%', right: '5%', top: '20%', bottom: '12%' },
  }

  return (
    <div style={s.page}>
      <button style={s.backBtn} onClick={() => navigate('/devices')}><BackIcon /> 返回设备列表</button>

      <div style={s.header}>
        <div style={s.title}>
          <ChipIcon />
          {device.name}
          <span style={s.badge(stCfg.color, stCfg.bg)}>{stCfg.label}</span>
        </div>
        <div style={s.sub}>
          <span>{device.deviceCode}</span>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '24px' }}>
        <div style={s.card}>
          <div style={s.cardTitle}><InfoIcon /> 设备信息</div>
          <div><div style={s.infoRow}><span style={s.infoLabel}>设备型号</span><span style={s.infoValue}>{typeName}</span></div>
          <div style={s.infoRow}><span style={s.infoLabel}>设备类型</span><span style={s.infoValue}>{typeName}</span></div>
          <div style={s.infoRow}><span style={s.infoLabel}>容量</span><span style={s.infoValue}>{device.capacity != null ? `${device.capacity}` : '-'}</span></div>
          <div style={s.infoRow}><span style={s.infoLabel}>安装日期</span><span style={s.infoValue}>{device.createdAt ? new Date(device.createdAt).toLocaleDateString() : '-'}</span></div>
          <div style={{ ...s.infoRow, borderBottom: 'none' }}><span style={s.infoLabel}>位置</span><span style={s.infoValue}>{device.siteId ? `站点 #${device.siteId}` : '-'}</span></div></div>
        </div>

        <div style={s.card}>
          <div style={s.cardTitle}><ActivityIcon /> 运行状态</div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '16px' }}>
            <span style={{ ...s.statusDot(stCfg.dot) }} />
            <span style={{ fontSize: '16px', fontWeight: 600, color: stCfg.color }}>{stCfg.label}</span>
          </div>
          <div style={s.infoRow}>
            <span style={s.infoLabel}><ClockIcon /> 最后心跳</span>
            <span style={s.infoValue}>
              {device.createdAt ? new Date(device.createdAt).toLocaleString() : '-'}
            </span>
          </div>
        </div>
      </div>

      <div style={s.grid2}>
        <div style={s.card}>
          {telemetryTemp.length === 0 ? <div style={s.empty}>暂无温度数据</div> : <ReactECharts option={tempChartOption} style={{ height: '300px' }} />}
        </div>
        <div style={s.card}>
          {telemetryInventory.length === 0 ? <div style={s.empty}>暂无库存趋势数据</div> : <ReactECharts option={inventoryChartOption} style={{ height: '300px' }} />}
        </div>
      </div>

      <div style={s.grid2}>
        <div style={s.card}>
          <div style={s.cardTitle}><PackageIcon /> 库存清单</div>
          {stockList.length === 0 ? (
            <div style={s.empty}>暂无库存数据</div>
          ) : (
            <table style={s.table}>
              <thead>
                <tr>
                  <th style={s.th}>SKU</th>
                  <th style={s.th}>数量</th>
                  <th style={s.th}>阈值</th>
                  <th style={s.th}>状态</th>
                </tr>
              </thead>
              <tbody>
                {stockList.map(item => {
                  const sku = skuMap.get(item.skuId)
                  const st = stockStatusMap[item.status] || stockStatusMap.normal
                  return (
                    <tr key={item.id}>
                      <td style={s.td}>{sku?.name || `SKU #${item.skuId}`}</td>
                      <td style={s.td}>{item.quantity}</td>
                      <td style={s.td}>最小 {item.minThreshold} / 最大 {item.maxCapacity}</td>
                      <td style={s.td}>
                        <span style={s.badge(st[0], st[1])}>{stockStatusLabel[item.status] || item.status}</span>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          )}
        </div>

        <div style={s.card}>
          <div style={s.cardTitle}><ActivityIcon /> 事件记录</div>
          {events.length === 0 ? (
            <div style={s.empty}>暂无事件数据</div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              {events.map(evt => (
                <div key={evt.id} style={{ display: 'flex', alignItems: 'flex-start', gap: '12px', padding: '8px 0', borderBottom: '1px solid #f3f4f6' }}>
                  <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: eventColors[evt.type] || '#6b7280', marginTop: '6px', flexShrink: 0 }} />
                  <div style={{ flex: 1, fontSize: '14px', color: '#111827' }}>{evt.message}</div>
                  <span style={{ fontSize: '12px', color: '#9ca3af', whiteSpace: 'nowrap' }}>{fmtEvt(evt.timestamp)}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
