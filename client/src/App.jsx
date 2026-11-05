import { useCallback, useEffect, useMemo, useState } from 'react'
import { apiRequest, queryString } from './api'
import './App.css'

const initialSettings = {
  origin: localStorage.getItem('releasehub.apiOrigin') ?? '',
  token: localStorage.getItem('releasehub.authToken') ?? '',
  role: localStorage.getItem('releasehub.role') ?? 'PUBLISHER',
  userId: localStorage.getItem('releasehub.userId') ?? '',
}

const navItems = [
  { id: 'discover', label: 'Discover', icon: '⌕' },
  { id: 'catalog', label: 'Catalog', icon: '▦' },
  { id: 'publish', label: 'Publisher studio', icon: '＋' },
  { id: 'feedback', label: 'Feedback', icon: '✳' },
  { id: 'reports', label: 'Analytics', icon: '↗' },
]

const emptySoftwareForm = {
  name: '',
  description: '',
  category: '',
  tags: '',
  publisherId: '',
}

const emptyVersionForm = {
  versionNumber: '',
  os: 'LINUX',
  releaseNotes: '',
  fileRef: '',
}

function getErrorMessage(error) {
  return error instanceof Error ? error.message : 'Something went wrong.'
}

function toSoftware(item) {
  return {
    ...item,
    category: item.category || 'Uncategorized',
    tags: Array.isArray(item.tags) ? item.tags : [],
  }
}

function App() {
  const [settings, setSettings] = useState(initialSettings)
  const [activeView, setActiveView] = useState('discover')
  const [settingsOpen, setSettingsOpen] = useState(false)
  const [software, setSoftware] = useState([])
  const [selectedSoftware, setSelectedSoftware] = useState(null)
  const [versions, setVersions] = useState([])
  const [feedback, setFeedback] = useState([])
  const [report, setReport] = useState(null)
  const [searchResult, setSearchResult] = useState(null)
  const [searchForm, setSearchForm] = useState({ q: '', category: '', os: '' })
  const [catalogFilters, setCatalogFilters] = useState({ name: '', category: '', os: '' })
  const [softwareForm, setSoftwareForm] = useState(emptySoftwareForm)
  const [editingSoftwareId, setEditingSoftwareId] = useState('')
  const [versionForm, setVersionForm] = useState(emptyVersionForm)
  const [feedbackForm, setFeedbackForm] = useState({ versionId: '', userId: '', rating: 5, comment: '' })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [connection, setConnection] = useState('checking')

  const request = useCallback((service, path, options = {}) => (
    apiRequest(service, path, options, settings)
  ), [settings])

  const loadCatalog = useCallback(async (filters) => {
    const result = await request('catalog', `/software${queryString({ ...filters, page: 0, size: 50 })}`)
    const items = (result.content ?? result ?? []).map(toSoftware)
    setSoftware(items)
    return items
  }, [request])

  const run = useCallback(async (action, successMessage = '') => {
    setBusy(true)
    setError('')
    setNotice('')
    try {
      const result = await action()
      if (successMessage) setNotice(successMessage)
      return result
    } catch (requestError) {
      setError(getErrorMessage(requestError))
      return null
    } finally {
      setBusy(false)
    }
  }, [])

  useEffect(() => {
    localStorage.setItem('releasehub.apiOrigin', settings.origin)
    localStorage.setItem('releasehub.authToken', settings.token)
    localStorage.setItem('releasehub.role', settings.role)
    localStorage.setItem('releasehub.userId', settings.userId)
  }, [settings])

  useEffect(() => {
    let active = true
    request('catalog', `/software${queryString({ page: 0, size: 50 })}`)
      .then((result) => {
        if (!active) return
        setSoftware((result.content ?? result ?? []).map(toSoftware))
        setConnection('connected')
      })
      .catch(() => { if (active) setConnection('offline') })
    return () => { active = false }
  }, [request])

  const loadVersions = useCallback(async (softwareId) => {
    if (!softwareId) return
    const result = await request('catalog', `/software/${encodeURIComponent(softwareId)}/versions`)
    setVersions(result ?? [])
    return result ?? []
  }, [request])

  const loadSearch = async (event) => {
    event?.preventDefault()
    const result = await run(
      () => request('search', queryString({ ...searchForm, page: 0, size: 20 })),
    )
    if (result) setSearchResult(result)
  }

  const openSoftware = async (item, navigate = true) => {
    const result = await run(async () => {
      const [fullSoftware, softwareVersions] = await Promise.all([
        request('catalog', `/software/${encodeURIComponent(item.id)}`),
        request('catalog', `/software/${encodeURIComponent(item.id)}/versions`),
      ])
      return { item: toSoftware(fullSoftware), versions: softwareVersions ?? [] }
    })
    if (result) {
      setSelectedSoftware(result.item)
      setVersions(result.versions)
      if (navigate) setActiveView('catalog')
    }
  }

  const beginEdit = (item) => {
    setEditingSoftwareId(item.id)
    setSoftwareForm({
      name: item.name ?? '',
      description: item.description ?? '',
      category: item.category === 'Uncategorized' ? '' : item.category,
      tags: (item.tags ?? []).join(', '),
      publisherId: item.publisherId ?? '',
    })
    setActiveView('publish')
  }

  const saveSoftware = async (event) => {
    event.preventDefault()
    const payload = {
      name: softwareForm.name.trim(),
      description: softwareForm.description.trim(),
      category: softwareForm.category.trim(),
      tags: softwareForm.tags.split(',').map((tag) => tag.trim()).filter(Boolean),
    }
    if (!editingSoftwareId && softwareForm.publisherId.trim()) {
      payload.publisherId = softwareForm.publisherId.trim()
    }

    const result = await run(() => request(
      'catalog',
      editingSoftwareId ? `/software/${encodeURIComponent(editingSoftwareId)}` : '/software',
      {
        method: editingSoftwareId ? 'PUT' : 'POST',
        body: JSON.stringify(payload),
      },
    ), editingSoftwareId ? 'Software details updated.' : 'Software added to the catalog.')

    if (result) {
      const item = toSoftware(result)
      setSoftware((current) => editingSoftwareId
        ? current.map((entry) => entry.id === item.id ? item : entry)
        : [item, ...current])
      setSelectedSoftware(item)
      setEditingSoftwareId('')
      setSoftwareForm(emptySoftwareForm)
      setActiveView('catalog')
      await loadVersions(item.id).catch(() => {})
    }
  }

  const deleteSoftware = async (item) => {
    if (!window.confirm(`Delete "${item.name}" and its catalog entry?`)) return
    const result = await run(
      () => request('catalog', `/software/${encodeURIComponent(item.id)}`, { method: 'DELETE' }),
      'Software entry deleted.',
    )
    if (result !== null) {
      setSoftware((current) => current.filter((entry) => entry.id !== item.id))
      if (selectedSoftware?.id === item.id) {
        setSelectedSoftware(null)
        setVersions([])
      }
    }
  }

  const saveVersion = async (event) => {
    event.preventDefault()
    if (!selectedSoftware) {
      setError('Choose a software entry before adding a release.')
      return
    }
    const result = await run(() => request(
      'catalog',
      `/software/${encodeURIComponent(selectedSoftware.id)}/versions`,
      { method: 'POST', body: JSON.stringify(versionForm) },
    ), 'Release published.')
    if (result) {
      setVersions((current) => [result, ...current])
      setVersionForm(emptyVersionForm)
    }
  }

  const deleteVersion = async (version) => {
    if (!window.confirm(`Delete release ${version.versionNumber}?`)) return
    const result = await run(
      () => request('catalog', `/versions/${encodeURIComponent(version.id)}`, { method: 'DELETE' }),
      'Release deleted.',
    )
    if (result !== null) setVersions((current) => current.filter((entry) => entry.id !== version.id))
  }

  const getVersion = async (version) => {
    const result = await run(() => request('catalog', `/versions/${encodeURIComponent(version.id)}`))
    if (result) setNotice(`Loaded release ${result.versionNumber} (${result.id}).`)
  }

  const recordDownload = async (version) => {
    const result = await run(
      () => request('catalog', `/download/${encodeURIComponent(version.id)}`),
      'Download event recorded.',
    )
    if (result?.downloadUrl) {
      setNotice(`Download recorded. File reference: ${result.downloadUrl}`)
      if (/^https?:\/\//i.test(result.downloadUrl)) window.open(result.downloadUrl, '_blank', 'noopener,noreferrer')
    }
  }

  const submitFeedback = async (event) => {
    event.preventDefault()
    const result = await run(
      () => request('feedback', '', {
        method: 'POST',
        body: JSON.stringify({
          ...feedbackForm,
          userId: feedbackForm.userId.trim() || settings.userId || 'demo-user',
          rating: Number(feedbackForm.rating),
        }),
      }),
      'Thanks — your feedback was submitted.',
    )
    if (result) {
      setFeedback((current) => [result, ...current])
      setFeedbackForm((current) => ({ ...current, comment: '' }))
    }
  }

  const loadFeedback = async (event) => {
    event?.preventDefault()
    if (!selectedSoftware) {
      setError('Select a software entry to view feedback.')
      return
    }
    const result = await run(() => request('feedback', `/${encodeURIComponent(selectedSoftware.id)}`))
    if (result) setFeedback(result)
  }

  const loadReport = async () => {
    if (!selectedSoftware) {
      setError('Select a software entry to view analytics.')
      return
    }
    const result = await run(() => request('reports', `/${encodeURIComponent(selectedSoftware.id)}`))
    if (result) setReport(result)
  }

  const reindex = async () => {
    const result = await run(
      () => request('search', '/admin/reindex', { method: 'POST' }),
      'Search index reindex completed.',
    )
    if (result) setNotice(`Index ${result.index ?? ''} is now live.`)
  }

  const shownSoftware = useMemo(() => (
    searchResult ? searchResult.hits.map((hit) => toSoftware(hit)) : software
  ), [searchResult, software])

  const renderSoftwareCard = (item) => (
    <article className="software-card" key={item.id}>
      <div className="software-card__top">
        <div className="app-mark">{(item.name || 'R').slice(0, 1).toUpperCase()}</div>
        <span className="category-chip">{item.category}</span>
      </div>
      <button className="text-button software-title" onClick={() => openSoftware(item)}>{item.name}</button>
      <p className="software-description">{item.description || 'No description provided yet.'}</p>
      <div className="tag-row">
        {(item.tags ?? []).slice(0, 3).map((tag) => <span className="tag" key={tag}>{tag}</span>)}
      </div>
      <div className="software-card__footer">
        <span className="muted small">{item.publisherId || 'Community publisher'}</span>
        <button className="quiet-button" onClick={() => openSoftware(item)}>View releases <span>→</span></button>
      </div>
      {activeView === 'catalog' && (
        <div className="card-actions">
          <button className="quiet-button" onClick={() => beginEdit(item)}>Edit</button>
          <button className="quiet-button danger-text" onClick={() => deleteSoftware(item)}>Delete</button>
        </div>
      )}
    </article>
  )

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a className="brand" href="#" onClick={(event) => { event.preventDefault(); setActiveView('discover') }}>
          <span className="brand-mark">R</span>
          <span>release<span className="brand-light">hub</span></span>
        </a>
        <div className="sidebar-label">WORKSPACE</div>
        <nav className="side-nav" aria-label="Main navigation">
          {navItems.map((item) => (
            <button
              className={`nav-item ${activeView === item.id ? 'is-active' : ''}`}
              key={item.id}
              onClick={() => { setActiveView(item.id); setError(''); setNotice('') }}
            >
              <span className="nav-icon">{item.icon}</span>
              <span>{item.label}</span>
              {item.id === 'catalog' && <span className="nav-count">{software.length}</span>}
            </button>
          ))}
        </nav>
        <div className="sidebar-spacer" />
        <button className={`connection-card ${connection}`} onClick={() => setSettingsOpen((open) => !open)}>
          <span className="connection-dot" />
          <span>
            <strong>{connection === 'connected' ? 'API connected' : connection === 'checking' ? 'Connecting…' : 'API offline'}</strong>
            <small>{settings.origin || 'Local service proxy'}</small>
          </span>
          <span className="settings-cog">⚙</span>
        </button>
        <div className="sidebar-foot">A release platform demo<br />built for teams that ship.</div>
      </aside>

      <main className="main-area">
        <header className="topbar">
          <div className="breadcrumbs"><span>Workspace</span><b>/</b><strong>{navItems.find((item) => item.id === activeView)?.label}</strong></div>
          <div className="topbar-right">
            <span className="environment-pill"><span /> DEMO ENVIRONMENT</span>
            <button className="avatar" title="Publisher profile">{(settings.userId || 'P').slice(0, 1).toUpperCase()}</button>
          </div>
        </header>

        {settingsOpen && (
          <section className="settings-panel">
            <div className="settings-panel__heading">
              <div><strong>API connection</strong><p>Use the local proxy by default, or point the app to Kong.</p></div>
              <button className="icon-button" onClick={() => setSettingsOpen(false)} aria-label="Close settings">×</button>
            </div>
            <div className="settings-grid">
              <label>API base URL <input value={settings.origin} placeholder="Same origin / local Vite proxy" onChange={(event) => setSettings({ ...settings, origin: event.target.value })} /></label>
              <label>JWT bearer token <input type="password" value={settings.token} placeholder="Optional for direct local services" onChange={(event) => setSettings({ ...settings, token: event.target.value })} /></label>
              <label>Catalog role <select value={settings.role} onChange={(event) => setSettings({ ...settings, role: event.target.value })}><option>PUBLISHER</option><option>ADMIN</option><option>USER</option></select></label>
              <label>Demo user ID <input value={settings.userId} placeholder="demo-user" onChange={(event) => setSettings({ ...settings, userId: event.target.value })} /></label>
            </div>
            <p className="settings-hint">Local mode forwards requests through Vite to ports 8083–8086. Gateway mode requires a valid JWT. Role headers are for the project’s local catalog filter.</p>
          </section>
        )}

        <div className="page-content">
          {(error || notice) && (
            <div className={`toast ${error ? 'toast-error' : 'toast-success'}`} role={error ? 'alert' : 'status'}>
              <span>{error ? '!' : '✓'}</span>
              <p>{error || notice}</p>
              <button onClick={() => { setError(''); setNotice('') }} aria-label="Dismiss notification">×</button>
            </div>
          )}

          {activeView === 'discover' && (
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
                <form className="search-form" onSubmit={loadSearch}>
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
                  <button className="quiet-button" onClick={() => { setActiveView('catalog'); setSearchResult(null) }}>Browse all software <span>→</span></button>
                </div>
                {shownSoftware.length ? <div className="software-grid">{shownSoftware.slice(0, 9).map(renderSoftwareCard)}</div> : (
                  <div className="empty-state"><div className="empty-icon">⌕</div><h3>{connection === 'offline' ? 'Connect your services' : 'Nothing here yet'}</h3><p>{connection === 'offline' ? 'Start the backend stack, then refresh the page.' : 'Try a different search or add your first software entry in Publisher studio.'}</p><button className="secondary-button" onClick={() => { setActiveView('publish'); setSearchResult(null) }}>Add software</button></div>
                )}
              </section>
              <div className="page-footer"><span>Made for people who build.</span><span>ReleaseHub <b>·</b> Software, in good company.</span></div>
            </>
          )}

          {activeView === 'catalog' && (
            <section className="view-section">
              <div className="view-header">
                <div><div className="eyebrow">YOUR SOFTWARE LIBRARY</div><h1>Catalog</h1><p className="page-lead">Browse, inspect, and maintain every product in the ReleaseHub catalog.</p></div>
                <button className="primary-button" onClick={() => { setEditingSoftwareId(''); setSoftwareForm(emptySoftwareForm); setActiveView('publish') }}>＋ Add software</button>
              </div>
              <form className="filter-toolbar" onSubmit={(event) => { event.preventDefault(); run(() => loadCatalog(catalogFilters)) }}>
                <input placeholder="Filter by name" value={catalogFilters.name} onChange={(event) => setCatalogFilters({ ...catalogFilters, name: event.target.value })} />
                <input placeholder="Category" value={catalogFilters.category} onChange={(event) => setCatalogFilters({ ...catalogFilters, category: event.target.value })} />
                <select value={catalogFilters.os} onChange={(event) => setCatalogFilters({ ...catalogFilters, os: event.target.value })}><option value="">Any OS</option><option value="LINUX">Linux</option><option value="WINDOWS">Windows</option><option value="MACOS">macOS</option></select>
                <button className="secondary-button">Apply filters</button>
                <button type="button" className="quiet-button" onClick={() => { const clean = { name: '', category: '', os: '' }; setCatalogFilters(clean); run(() => loadCatalog(clean)) }}>Clear</button>
              </form>
              <div className="catalog-summary"><span><strong>{software.length}</strong> software entries</span><span>Filtered catalog · MySQL</span></div>
              {software.length ? <div className="software-grid">{software.map(renderSoftwareCard)}</div> : <div className="empty-state"><div className="empty-icon">▦</div><h3>Your catalog starts here</h3><p>Add a software product to publish your first release.</p><button className="primary-button" onClick={() => setActiveView('publish')}>Create first product</button></div>}
              {selectedSoftware && (
                <div className="detail-panel">
                  <div className="section-heading">
                    <div><span className="eyebrow">PRODUCT DETAIL</span><h2>{selectedSoftware.name}</h2></div>
                    <button className="icon-button" onClick={() => { setSelectedSoftware(null); setVersions([]) }} aria-label="Close product detail">×</button>
                  </div>
                  <p className="detail-copy">{selectedSoftware.description || 'No description provided.'}</p>
                  <p className="small muted">ID · <code>{selectedSoftware.id}</code></p>
                  <div className="section-heading version-heading"><div><h3>Releases</h3><p>{versions.length} versions published</p></div><button className="secondary-button" onClick={() => setActiveView('publish')}>＋ Add release</button></div>
                  {versions.length ? <VersionList versions={versions} onDownload={recordDownload} onDelete={deleteVersion} onGet={getVersion} /> : <p className="empty-inline">No versions published for this product yet.</p>}
                </div>
              )}
            </section>
          )}

          {activeView === 'publish' && (
            <section className="view-section">
              <div className="view-header">
                <div><div className="eyebrow">PUBLISHER WORKSPACE</div><h1>{editingSoftwareId ? 'Edit software' : 'Ship something great.'}</h1><p className="page-lead">Add a product to the catalog, then publish its first release.</p></div>
                <div className="publisher-badge"><span>✦</span> PUBLISHER STUDIO</div>
              </div>
              <div className="publisher-layout">
                <form className="form-card" onSubmit={saveSoftware}>
                  <div className="form-card-heading"><span className="step-number">01</span><div><h2>{editingSoftwareId ? 'Update product details' : 'Create a software entry'}</h2><p>Tell users what this product is about.</p></div></div>
                  <label>Product name <input required maxLength="160" value={softwareForm.name} onChange={(event) => setSoftwareForm({ ...softwareForm, name: event.target.value })} placeholder="e.g. Studio North" /></label>
                  <label>Description <textarea rows="3" value={softwareForm.description} onChange={(event) => setSoftwareForm({ ...softwareForm, description: event.target.value })} placeholder="What does your software help people do?" /></label>
                  <div className="form-row">
                    <label>Category <input value={softwareForm.category} onChange={(event) => setSoftwareForm({ ...softwareForm, category: event.target.value })} placeholder="developer-tools" /></label>
                    {!editingSoftwareId && <label>Publisher ID <input value={softwareForm.publisherId} onChange={(event) => setSoftwareForm({ ...softwareForm, publisherId: event.target.value })} placeholder="Generated if blank" /></label>}
                  </div>
                  <label>Tags <input value={softwareForm.tags} onChange={(event) => setSoftwareForm({ ...softwareForm, tags: event.target.value })} placeholder="design, open-source, editor" /><small>Separate tags with commas.</small></label>
                  <div className="form-actions"><button className="primary-button" disabled={busy}>{editingSoftwareId ? 'Save changes' : 'Create software'} <span>→</span></button>{editingSoftwareId && <button type="button" className="quiet-button" onClick={() => { setEditingSoftwareId(''); setSoftwareForm(emptySoftwareForm) }}>Cancel edit</button>}</div>
                </form>

                <div className="release-column">
                  <form className="form-card release-card" onSubmit={saveVersion}>
                    <div className="form-card-heading"><span className="step-number step-number--green">02</span><div><h2>Publish a release</h2><p>Add a downloadable version for a product.</p></div></div>
                    <label>Software entry <select value={selectedSoftware?.id ?? ''} onChange={async (event) => {
                      const chosen = software.find((item) => item.id === event.target.value)
                      if (chosen) await openSoftware(chosen, false)
                      else { setSelectedSoftware(null); setVersions([]) }
                    }}><option value="">Select a product…</option>{software.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>
                    <div className="form-row">
                      <label>Version number <input required value={versionForm.versionNumber} onChange={(event) => setVersionForm({ ...versionForm, versionNumber: event.target.value })} placeholder="1.0.0" /></label>
                      <label>Operating system <select value={versionForm.os} onChange={(event) => setVersionForm({ ...versionForm, os: event.target.value })}><option>LINUX</option><option>WINDOWS</option><option>MACOS</option><option>ANDROID</option><option>IOS</option></select></label>
                    </div>
                    <label>Download URL / file reference <input value={versionForm.fileRef} onChange={(event) => setVersionForm({ ...versionForm, fileRef: event.target.value })} placeholder="https://downloads.example.com/app.zip" /></label>
                    <label>Release notes <textarea rows="2" value={versionForm.releaseNotes} onChange={(event) => setVersionForm({ ...versionForm, releaseNotes: event.target.value })} placeholder="What’s new in this version?" /></label>
                    <button className="secondary-button" disabled={busy || !selectedSoftware}>Publish release <span>→</span></button>
                  </form>
                  <div className="tip-card"><div className="tip-icon">✳</div><div><strong>One product. Every platform.</strong><p>Publish releases by operating system so people can find the right version for their setup.</p></div></div>
                  <button className="reindex-button" onClick={reindex} disabled={busy}><span>⟳</span> Rebuild search index <small>Admin action</small></button>
                </div>
              </div>
            </section>
          )}

          {activeView === 'feedback' && (
            <section className="view-section">
              <div className="view-header">
                <div><div className="eyebrow">COMMUNITY VOICE</div><h1>Feedback</h1><p className="page-lead">Ratings and notes help great software get even better.</p></div>
                <span className="large-stamp">✳</span>
              </div>
              <div className="feedback-layout">
                <form className="form-card" onSubmit={submitFeedback}>
                  <div className="form-card-heading"><span className="step-number step-number--orange">✳</span><div><h2>Leave a review</h2><p>One review per user for each release.</p></div></div>
                  <label>Software <select required value={selectedSoftware?.id ?? ''} onChange={async (event) => {
                    const item = software.find((entry) => entry.id === event.target.value)
                    if (item) {
                      setFeedback([])
                      setFeedbackForm((current) => ({ ...current, versionId: '' }))
                      await openSoftware(item, false)
                    }
                    else { setSelectedSoftware(null); setVersions([]) }
                  }}><option value="">Choose software…</option>{software.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>
                  <label>Release <select required value={feedbackForm.versionId} onChange={(event) => setFeedbackForm({ ...feedbackForm, versionId: event.target.value })}><option value="">Choose a release…</option>{versions.map((version) => <option key={version.id} value={version.id}>{version.versionNumber} · {version.os}</option>)}</select></label>
                  <label>Your user ID <input value={feedbackForm.userId} onChange={(event) => setFeedbackForm({ ...feedbackForm, userId: event.target.value })} placeholder={settings.userId || 'demo-user'} /></label>
                  <div className="rating-field"><span>Your rating</span><div className="star-picker" role="radiogroup" aria-label="Rating">
                    {[1, 2, 3, 4, 5].map((rating) => <button type="button" role="radio" aria-checked={Number(feedbackForm.rating) === rating} className={Number(feedbackForm.rating) >= rating ? 'star selected' : 'star'} key={rating} onClick={() => setFeedbackForm({ ...feedbackForm, rating })} aria-label={`${rating} stars`}>★</button>)}
                  </div><strong>{feedbackForm.rating} / 5</strong></div>
                  <label>Review <textarea rows="4" maxLength="4000" value={feedbackForm.comment} onChange={(event) => setFeedbackForm({ ...feedbackForm, comment: event.target.value })} placeholder="Share what worked well, or what could be improved…" /></label>
                  <button className="primary-button" disabled={busy || !feedbackForm.versionId}>Submit feedback <span>→</span></button>
                </form>
                <div className="feedback-list-panel">
                  <div className="section-heading"><div><span className="eyebrow">RECENT REVIEWS</span><h2>{selectedSoftware?.name || 'Choose a product'}</h2></div><button className="quiet-button" onClick={loadFeedback} disabled={!selectedSoftware}>Refresh <span>⟳</span></button></div>
                  {!selectedSoftware ? <div className="empty-inline">Select a product in the review form to see its feedback.</div> : feedback.length ? <div className="review-list">{feedback.map((item) => <article className="review-card" key={item.id}><div className="review-card__top"><strong>{item.userId || 'Community member'}</strong><time>{item.createdAt ? new Date(item.createdAt).toLocaleDateString() : 'Just now'}</time></div><div className="review-stars">{'★'.repeat(item.rating)}<span>{'★'.repeat(5 - item.rating)}</span></div><p>{item.comment || 'No written comment.'}</p><small>Release {versions.find((version) => version.id === item.versionId)?.versionNumber ?? item.versionId}</small></article>)}</div> : <div className="empty-inline">No reviews yet. Be the first to leave feedback.</div>}
                </div>
              </div>
            </section>
          )}

          {activeView === 'reports' && (
            <section className="view-section">
              <div className="view-header">
                <div><div className="eyebrow">PUBLISHER INSIGHTS</div><h1>Analytics</h1><p className="page-lead">A real-time pulse on downloads and community feedback.</p></div>
                <span className="analytics-art">↗</span>
              </div>
              <div className="report-toolbar">
                <label>Software product <select value={selectedSoftware?.id ?? ''} onChange={async (event) => {
                  const item = software.find((entry) => entry.id === event.target.value)
                  if (item) {
                    setReport(null)
                    await openSoftware(item, false)
                  } else { setSelectedSoftware(null); setReport(null) }
                }}><option value="">Choose a product…</option>{software.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>
                <button className="primary-button" onClick={loadReport} disabled={busy || !selectedSoftware}>Load report <span>→</span></button>
              </div>
              {report ? (
                <>
                  <div className="report-context"><div className="app-mark">{(selectedSoftware?.name || 'R').slice(0, 1).toUpperCase()}</div><div><strong>{selectedSoftware?.name}</strong><span>{selectedSoftware?.category}</span></div><span className="live-pill"><i /> LIVE REPORT</span></div>
                  <div className="metrics-grid">
                    <MetricCard label="Total downloads" value={report.downloads ?? 0} note="All recorded releases" icon="↓" tone="blue" />
                    <MetricCard label="Average rating" value={Number(report.avgRating ?? 0).toFixed(1)} note="Out of 5 stars" icon="★" tone="yellow" />
                    <MetricCard label="Feedback received" value={report.feedbackCount ?? 0} note="Community reviews" icon="✳" tone="violet" />
                  </div>
                  <div className="report-note"><span>✳</span><p>Metrics are updated asynchronously as download and feedback events are consumed from Kafka.</p><button className="quiet-button" onClick={loadReport}>Refresh metrics <span>⟳</span></button></div>
                </>
              ) : <div className="report-empty"><div className="chart-placeholder"><span /><span /><span /><span /><span /><span /><span /></div><h2>Every release tells a story.</h2><p>Select a product to see its download and feedback totals.</p></div>}
            </section>
          )}
        </div>
      </main>
      {busy && <div className="busy-indicator"><span /> Syncing with ReleaseHub APIs…</div>}
    </div>
  )
}

function VersionList({ versions, onDownload, onDelete, onGet }) {
  return (
    <div className="version-list">
      {versions.map((version) => (
        <div className="version-row" key={version.id}>
          <div className="version-icon">↓</div>
          <div className="version-info"><strong>{version.versionNumber}</strong><span>{version.os} <i /> {version.releasedAt ? new Date(version.releasedAt).toLocaleDateString() : 'Release date unavailable'}</span></div>
          <button className="version-action" title="Fetch release detail" onClick={() => onGet(version)}>Details</button>
          <button className="version-action version-download" onClick={() => onDownload(version)}>Download</button>
          <button className="version-action danger-text" onClick={() => onDelete(version)}>Delete</button>
        </div>
      ))}
    </div>
  )
}

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

export default App
