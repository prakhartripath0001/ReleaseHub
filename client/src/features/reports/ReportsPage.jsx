function MetricCard({ label, value, note, icon, tone }) {
  return (
    <article className="metric-card">
      <div className={`metric-icon ${tone}`}>{icon}</div>
      <span className="metric-label">{label}</span>
      <strong className="metric-value">{value}</strong>
      <span className="metric-note">{note}</span>
      <div className="metric-spark"><span /><span /><span /><span /><span /><span /><span /></div>
    </article>
  )
}

export default function ReportsPage({ software, selectedSoftware, onSelectSoftware, report, onLoadReport, busy }) {
  return (
    <section className="view-section">
      <div className="view-header">
        <div><div className="eyebrow">PUBLISHER INSIGHTS</div><h1>Analytics</h1><p className="page-lead">A real-time pulse on downloads and community feedback.</p></div>
        <span className="analytics-art">↗</span>
      </div>
      <div className="report-toolbar">
        <label>Software product <select value={selectedSoftware?.id ?? ''} onChange={(event) => onSelectSoftware(event.target.value)}><option value="">Choose a product…</option>{software.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>
        <button className="primary-button" onClick={onLoadReport} disabled={busy || !selectedSoftware}>Load report <span>→</span></button>
      </div>
      {report ? (
        <>
          <div className="report-context"><div className="app-mark">{(selectedSoftware?.name || 'R').slice(0, 1).toUpperCase()}</div><div><strong>{selectedSoftware?.name}</strong><span>{selectedSoftware?.category}</span></div><span className="live-pill"><i /> LIVE REPORT</span></div>
          <div className="metrics-grid">
            <MetricCard label="Total downloads" value={report.downloads ?? 0} note="All recorded releases" icon="↓" tone="blue" />
            <MetricCard label="Average rating" value={Number(report.avgRating ?? 0).toFixed(1)} note="Out of 5 stars" icon="★" tone="yellow" />
            <MetricCard label="Feedback received" value={report.feedbackCount ?? 0} note="Community reviews" icon="✳" tone="violet" />
          </div>
          <div className="report-note"><span>✳</span><p>Metrics are updated asynchronously as download and feedback events are consumed from Kafka.</p><button className="quiet-button" onClick={onLoadReport}>Refresh metrics <span>⟳</span></button></div>
        </>
      ) : <div className="report-empty"><div className="chart-placeholder"><span /><span /><span /><span /><span /><span /><span /></div><h2>Every release tells a story.</h2><p>Select a product to see its download and feedback totals.</p></div>}
    </section>
  )
}
