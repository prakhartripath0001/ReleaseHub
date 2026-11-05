import SoftwareCard from './components/SoftwareCard'
import VersionList from './components/VersionList'

export default function CatalogPage({
  software,
  catalogFilters,
  setCatalogFilters,
  onFilter,
  onClear,
  onAdd,
  onOpen,
  onEdit,
  onDelete,
  selectedSoftware,
  versions,
  onCloseDetail,
  onAddRelease,
  onDownload,
  onDeleteVersion,
  onGetVersion,
}) {
  return (
    <section className="view-section">
      <div className="view-header">
        <div><div className="eyebrow">YOUR SOFTWARE LIBRARY</div><h1>Catalog</h1><p className="page-lead">Browse, inspect, and maintain every product in the ReleaseHub catalog.</p></div>
        <button className="primary-button" onClick={onAdd}>＋ Add software</button>
      </div>
      <form className="filter-toolbar" onSubmit={(event) => { event.preventDefault(); onFilter() }}>
        <input placeholder="Filter by name" value={catalogFilters.name} onChange={(event) => setCatalogFilters({ ...catalogFilters, name: event.target.value })} />
        <input placeholder="Category" value={catalogFilters.category} onChange={(event) => setCatalogFilters({ ...catalogFilters, category: event.target.value })} />
        <select value={catalogFilters.os} onChange={(event) => setCatalogFilters({ ...catalogFilters, os: event.target.value })}><option value="">Any OS</option><option value="LINUX">Linux</option><option value="WINDOWS">Windows</option><option value="MACOS">macOS</option></select>
        <button className="secondary-button">Apply filters</button>
        <button type="button" className="quiet-button" onClick={onClear}>Clear</button>
      </form>
      <div className="catalog-summary"><span><strong>{software.length}</strong> software entries</span><span>Filtered catalog · MySQL</span></div>
      {software.length ? <div className="software-grid">{software.map((item) => <SoftwareCard key={item.id} item={item} onOpen={onOpen} showManagement onEdit={onEdit} onDelete={onDelete} />)}</div> : <div className="empty-state"><div className="empty-icon">▦</div><h3>Your catalog starts here</h3><p>Add a software product to publish your first release.</p><button className="primary-button" onClick={onAdd}>Create first product</button></div>}
      {selectedSoftware && (
        <div className="detail-panel">
          <div className="section-heading">
            <div><span className="eyebrow">PRODUCT DETAIL</span><h2>{selectedSoftware.name}</h2></div>
            <button className="icon-button" onClick={onCloseDetail} aria-label="Close product detail">×</button>
          </div>
          <p className="detail-copy">{selectedSoftware.description || 'No description provided.'}</p>
          <p className="small muted">ID · <code>{selectedSoftware.id}</code></p>
          <div className="section-heading version-heading"><div><h3>Releases</h3><p>{versions.length} versions published</p></div><button className="secondary-button" onClick={onAddRelease}>＋ Add release</button></div>
          {versions.length ? <VersionList versions={versions} onDownload={onDownload} onDelete={onDeleteVersion} onGet={onGetVersion} /> : <p className="empty-inline">No versions published for this product yet.</p>}
        </div>
      )}
    </section>
  )
}
