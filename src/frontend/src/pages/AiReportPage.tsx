import { useState, useEffect } from 'react'
import { fetchSites, fetchAiReportsBySite, generateAiReport, fetchAiReportDetail } from '../api/endpoints'
import { useAuth } from '../hooks/useAuth'
import type { ISite, IWeeklyReport } from '../api/endpoints'

const s = {
  page: { padding: '32px', maxWidth: '1400px', margin: '0 auto', fontFamily: 'Inter, system-ui, sans-serif' },
  header: { marginBottom: '24px' },
  title: { fontSize: '24px', fontWeight: 600, color: '#08060d', margin: 0 },
  sub: { fontSize: '14px', color: '#6b7280', margin: '4px 0 0 0' },
  controls: { display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '24px', flexWrap: 'wrap' as const },
  select: { padding: '10px 16px', borderRadius: '8px', border: '1px solid #d1d5db', fontSize: '14px', outline: 'none', background: '#fff', minWidth: '200px' },
  btn: (bg: string, color: string, disabled?: boolean) => ({ display: 'inline-flex', alignItems: 'center', gap: '6px', padding: '10px 20px', borderRadius: '8px', fontSize: '14px', fontWeight: 500, border: 'none', cursor: disabled ? 'not-allowed' : 'pointer', background: disabled ? '#d1d5db' : bg, color: disabled ? '#fff' : color, opacity: disabled ? 0.7 : 1 }),
  twoCol: { display: 'grid', gridTemplateColumns: '360px 1fr', gap: '24px', alignItems: 'start' },
  card: { background: '#fff', borderRadius: '12px', boxShadow: '0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.04)', padding: '24px' },
  sectionTitle: { fontSize: '16px', fontWeight: 600, color: '#08060d', margin: '0 0 16px 0' },
  reportItem: { padding: '12px 16px', borderRadius: '8px', cursor: 'pointer', border: 'none', background: 'transparent', width: '100%', textAlign: 'left' as const, display: 'block', fontSize: '14px', transition: 'all 0.15s' },
  reportItemActive: { background: '#ede9fe' },
  reportTitle: { fontWeight: 500, color: '#08060d', marginBottom: '2px' },
  reportMeta: { fontSize: '12px', color: '#9ca3af' },
  contentCard: { background: '#fff', borderRadius: '12px', boxShadow: '0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.04)', padding: '32px', minHeight: '400px' },
  contentTitle: { fontSize: '20px', fontWeight: 600, color: '#08060d', marginBottom: '8px' },
  contentPeriod: { fontSize: '14px', color: '#6b7280', marginBottom: '24px' },
  contentBody: { fontSize: '15px', lineHeight: '1.8', color: '#374151', whiteSpace: 'pre-wrap' as const },
  summaryBox: { background: '#f9fafb', borderRadius: '8px', padding: '16px', marginBottom: '24px', borderLeft: '4px solid #533afd' },
  summaryText: { fontSize: '14px', color: '#374151', lineHeight: '1.6' },
  empty: { textAlign: 'center' as const, padding: '48px', color: '#9ca3af', fontSize: '14px' },
  loading: { textAlign: 'center' as const, padding: '48px', color: '#6b7280', fontSize: '14px' },
  error: { textAlign: 'center' as const, padding: '48px', color: '#dc2626', fontSize: '14px' },
  placeholder: { textAlign: 'center' as const, padding: '80px 0', color: '#9ca3af' },
  placeholderIcon: { fontSize: '48px', marginBottom: '16px' },
  placeholderText: { fontSize: '16px', color: '#9ca3af' },
}

function formatDate(dateStr: string) {
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function formatPeriod(start: string, end: string) {
  const s = start.slice(0, 10)
  const e = end.slice(0, 10)
  return `${s} ~ ${e}`
}

function getContentPreview(content: string | undefined): string {
  if (!content) return ''
  const lines = content.split('\n').filter(l => l.trim() && !l.startsWith('#'))
  return lines.slice(0, 3).join(' ').slice(0, 200)
}

export default function AiReportPage() {
  const { isAuthenticated, token } = useAuth()
  const [sites, setSites] = useState<ISite[]>([])
  const [selectedSite, setSelectedSite] = useState('')
  const [reports, setReports] = useState<IWeeklyReport[]>([])
  const [selectedReport, setSelectedReport] = useState<IWeeklyReport | null>(null)
  const [generating, setGenerating] = useState(false)
  const [loadingReports, setLoadingReports] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    fetchSites()
      .then(res => setSites(res.data.content))
      .catch(err => setError(err.message || '加载站点列表失败'))
  }, [token])

  useEffect(() => {
    if (!selectedSite) return
    setSelectedReport(null)
    setLoadingReports(true)
    setError('')
    fetchAiReportsBySite(Number(selectedSite))
      .then(res => setReports(res.data))
      .catch(err => setError(err.message || '加载报告列表失败'))
      .finally(() => setLoadingReports(false))
  }, [selectedSite])

  function handleSelectReport(report: IWeeklyReport) {
    if (report.id === selectedReport?.id) {
      setSelectedReport(null)
      return
    }
    fetchAiReportDetail(report.id).then(res => {
      setSelectedReport(res.data)
    }).catch(() => {
      setSelectedReport(report)
    })
  }

  async function handleGenerate() {
    if (!selectedSite || generating) return
    setGenerating(true)
    setError('')
    const now = new Date()
    const periodStart = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10)
    const periodEnd = now.toISOString().slice(0, 10)
    try {
      const newReport = await generateAiReport({ siteId: Number(selectedSite), periodStart, periodEnd })
      setReports(prev => [newReport.data, ...prev])
      setSelectedReport(newReport.data)
    } catch (err: any) {
      setError(err.message || '生成报告失败')
    } finally {
      setGenerating(false)
    }
  }

  return (
    <div style={s.page}>
      <div style={s.header}>
        <h1 style={s.title}>AI运营报告</h1>
        <p style={s.sub}>基于AI的站点运营分析与建议报告</p>
      </div>

      <div style={s.controls}>
        <select style={s.select} value={selectedSite} onChange={e => setSelectedSite(e.target.value)}>
          <option value="">选择站点...</option>
          {sites.map(site => (
            <option key={site.id} value={site.id}>{site.name}</option>
          ))}
        </select>
        <button
          style={s.btn('#533afd', '#fff', !selectedSite || generating)}
          disabled={!selectedSite || generating}
          onClick={handleGenerate}
        >
          {generating ? '生成中...' : '生成报告'}
        </button>
      </div>

      {error && (
        <div style={s.error}>{error}</div>
      )}

      {selectedSite ? (
        <div style={s.twoCol}>
          <div style={s.card}>
            <h2 style={s.sectionTitle}>历史报告</h2>
            {loadingReports ? (
              <div style={s.loading}>加载中...</div>
            ) : reports.length === 0 ? (
              <div style={s.empty}>暂无报告，点击"生成报告"创建</div>
            ) : (
              reports.map(report => (
                <button
                  key={report.id}
                  style={{
                    ...s.reportItem,
                    ...(selectedReport?.id === report.id ? s.reportItemActive : {}),
                  }}
                  onClick={() => handleSelectReport(report)}
                >
                  <div style={s.reportTitle}>{report.title}</div>
                  <div style={s.reportMeta}>{formatPeriod(report.periodStart, report.periodEnd)}</div>
                </button>
              ))
            )}
          </div>

          <div>
            {selectedReport ? (
              <div style={s.contentCard}>
                <div style={s.contentTitle}>{selectedReport.title}</div>
                <div style={s.contentPeriod}>
                  {formatPeriod(selectedReport.periodStart, selectedReport.periodEnd)} · 生成于 {formatDate(selectedReport.periodEnd)}
                </div>
                {selectedReport.content && (
                  <div style={s.summaryBox}>
                    <div style={{ fontSize: '13px', fontWeight: 600, color: '#533afd', marginBottom: '8px' }}>摘要</div>
                    <div style={s.summaryText}>{getContentPreview(selectedReport.content)}</div>
                  </div>
                )}
                <div style={s.contentBody}>{selectedReport.content || '暂无报告内容'}</div>
              </div>
            ) : (
              <div style={s.contentCard}>
                <div style={s.placeholder}>
                  <div style={s.placeholderIcon}>📄</div>
                  <div style={s.placeholderText}>选择左侧报告查看详情</div>
                </div>
              </div>
            )}
          </div>
        </div>
      ) : (
        <div style={s.card}>
          <div style={s.placeholder}>
            <div style={s.placeholderIcon}>🤖</div>
            <div style={s.placeholderText}>请先选择一个站点查看AI运营报告</div>
          </div>
        </div>
      )}
    </div>
  )
}
