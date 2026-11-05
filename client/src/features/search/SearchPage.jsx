import SoftwareCard from '../catalog/components/SoftwareCard'

export default function SearchPage({
  searchForm,
  setSearchForm,
  onSearch,
  busy,
  searchResult,
  software,
  connection,
  onOpen,
  onBrowseCatalog,
  onAddSoftware,
}) {
  const items = searchResult ? searchResult.hits : software
  return (
    <>
      <section className="welcome-row">
        <div>
          <div className="eyebrow"><span className="eyebrow-line" /> RELEASEHUB SOFTWARE DIRECTORY</div>
          <h1>Find your next<br /><em>favorite tool.</em></h1>
          <p className="page-lead">Explore trusted software releases, compare platforms, and hear from the people who use them.</p>
        </div>
        <div className="welcome-art" aria-hidden="true">
          <div className="orb orb-one" /><div className="orb orb-two" /><div className="orb orb-three" />
          <div className="art-window"><span /><span /><span /><div className="art-window-icon">R</div><div className="art-window-line line-a" /><div className="art-window-line line-b" /><div className="art-window-line line-c" /></div>
          <div className="art-spark">✳</div>
        </div>
      </section>
      <section className="search-panel">
        <div className="section-heading">
          <div><span className="eyebrow">DISCOVER</span><h2>Search the catalog</h2></div>
          <span className="search-hint">Fuzzy search · OS and category facets</span>
        </div>
        <form className="search-form" onSubmit={onSearch}>
          <label className="search-input"><span>⌕</span><input aria-label="Search software" placeholder="Try “design”, “editor”, or a product name…" value={searchForm.q} onChange={(event) => setSearchForm({ ...searchForm, q: event.target.value })} /></label>
          <select aria-label="Filter by category" value={searchForm.category} onChange={(event) => setSearchForm({ ...searchForm, category: event.target.value })}><option value="">All categories</option><option value="developer-tools">Developer tools</option><option value="tools">Tools</option><option value="productivity">Productivity</option><option value="design">Design</option></select>
          <select aria-label="Filter by operating system" value={searchForm.os} onChange={(event) => setSearchForm({ ...searchForm, os: event.target.value })}><option value="">All operating systems</option><option value="LINUX">Linux</option><option value="WINDOWS">Windows</option><option value="MACOS">macOS</option></select>
          <button className="primary-button" disabled={busy}>{busy ? 'Searching…' : 'Search'} <span>→</span></button>
        </form>
        {searchResult?.facets && (
          <div className="facet-row">
            {Object.entries(searchResult.facets).map(([facet, values]) => (
              <div className="facet-group" key={facet}><strong>{facet === 'os' ? 'OS' : facet}</strong>{values.slice(0, 4).map((value) => <span key={value.key}>{value.key} <small>{value.count}</small></span>)}</div>
            ))}
            <span className="result-count">{searchResult.total} results</span>
          </div>
        )}
      </section>
      <section className="catalog-section">
        <div className="section-heading">
          <div><span className="eyebrow">{searchResult ? 'SEARCH RESULTS' : 'CURATED DIRECTORY'}</span><h2>{searchResult ? `${searchResult.total} matches` : 'Popular software'}</h2></div>
          <button className="quiet-button" onClick={onBrowseCatalog}>Browse all software <span>→</span></button>
        </div>
        {items.length ? <div className="software-grid">{items.slice(0, 9).map((item) => <SoftwareCard key={item.id} item={item} onOpen={onOpen} />)}</div> : (
          <div className="empty-state"><div className="empty-icon">⌕</div><h3>{connection === 'offline' ? 'Connect your services' : 'Nothing here yet'}</h3><p>{connection === 'offline' ? 'Start the backend stack, then refresh the page.' : 'Try a different search or add your first software entry in Publisher studio.'}</p><button className="secondary-button" onClick={onAddSoftware}>Add software</button></div>
        )}
      </section>
      <div className="page-footer"><span>Made for people who build.</span><span>ReleaseHub <b>·</b> Software, in good company.</span></div>
    </>
  )
}
