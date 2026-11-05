import { useCallback, useEffect, useRef, useState } from 'react'
import ApiSettings from './components/ApiSettings'
import Navbar from './components/Navbar'
import navItems from './components/navItems'
import { apiRequest, queryString } from './config/api'
import AppRoutes from './routes/AppRoutes'
import { useAuth } from './hooks/useAuth'
import './App.css'

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

function toSoftware(item) {
  return {
    ...item,
    category: item.category || 'Uncategorized',
    tags: Array.isArray(item.tags) ? item.tags : [],
  }
}

function getErrorMessage(error) {
  return error instanceof Error ? error.message : 'Something went wrong.'
}

function App() {
  const { settings, setSettings } = useAuth()
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
  const settingsRef = useRef(settings)

  const request = useCallback((service, path, options = {}) => (
    apiRequest(service, path, options, settings)
  ), [settings])

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

  const loadCatalog = useCallback(async (filters = { name: '', category: '', os: '' }) => {
    const result = await request('catalog', `/software${queryString({ ...filters, page: 0, size: 50 })}`)
    const items = (result.content ?? result ?? []).map(toSoftware)
    setSoftware(items)
    return items
  }, [request])

  useEffect(() => {
    settingsRef.current = settings
  }, [settings])

  useEffect(() => {
    const controller = new AbortController()
    apiRequest(
      'catalog',
      `/software${queryString({ page: 0, size: 50 })}`,
      { signal: controller.signal },
      settingsRef.current,
    )
      .then((result) => {
        if (controller.signal.aborted) return
        setSoftware((result.content ?? result ?? []).map(toSoftware))
        setConnection('connected')
      })
      .catch(() => { if (!controller.signal.aborted) setConnection('offline') })
    return () => controller.abort()
  }, [settings.origin])

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

  const selectSoftware = async (softwareId) => {
    if (!softwareId) {
      setSelectedSoftware(null)
      setVersions([])
      setFeedback([])
      setReport(null)
      return
    }
    if (activeView === 'feedback') {
      setFeedback([])
      setFeedbackForm((current) => ({ ...current, versionId: '' }))
    }
    if (activeView === 'reports') setReport(null)
    const item = software.find((entry) => entry.id === softwareId)
    if (item) await openSoftware(item, false)
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

  const loadFeedback = async () => {
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

  const clearAlerts = () => {
    setError('')
    setNotice('')
  }

  const startCreateSoftware = () => {
    setEditingSoftwareId('')
    setSoftwareForm(emptySoftwareForm)
    setActiveView('publish')
  }

  const viewProps = {
    software,
    catalogFilters,
    setCatalogFilters,
    onFilter: () => run(() => loadCatalog(catalogFilters)),
    onClearFilters: () => {
      const clean = { name: '', category: '', os: '' }
      setCatalogFilters(clean)
      run(() => loadCatalog(clean))
    },
    onAddSoftware: startCreateSoftware,
    onOpenSoftware: openSoftware,
    onEditSoftware: beginEdit,
    onDeleteSoftware: deleteSoftware,
    selectedSoftware,
    versions,
    onCloseDetail: () => { setSelectedSoftware(null); setVersions([]) },
    onAddRelease: () => setActiveView('publish'),
    onDownload: recordDownload,
    onDeleteVersion: deleteVersion,
    onGetVersion: getVersion,
    editingSoftwareId,
    softwareForm,
    setSoftwareForm,
    onSaveSoftware: saveSoftware,
    onCancelEdit: () => { setEditingSoftwareId(''); setSoftwareForm(emptySoftwareForm) },
    onSelectSoftware: selectSoftware,
    versionForm,
    setVersionForm,
    onSaveVersion: saveVersion,
    busy,
    onReindex: reindex,
    feedbackForm,
    setFeedbackForm,
    onSubmitFeedback: submitFeedback,
    onLoadFeedback: loadFeedback,
    feedback,
    settings,
    report,
    onLoadReport: loadReport,
    searchForm,
    setSearchForm,
    onSearch: loadSearch,
    searchResult,
    connection,
    onBrowseCatalog: () => { setActiveView('catalog'); setSearchResult(null) },
  }

  return (
    <div className="app-shell">
      <Navbar
        activeView={activeView}
        onNavigate={(view) => { setActiveView(view); clearAlerts() }}
        connection={connection}
        softwareCount={software.length}
        apiOrigin={settings.origin}
        onOpenSettings={() => setSettingsOpen((open) => !open)}
      />
      <main className="main-area">
        <header className="topbar">
          <div className="breadcrumbs"><span>Workspace</span><b>/</b><strong>{navItems.find((item) => item.id === activeView)?.label}</strong></div>
          <div className="topbar-right">
            <span className="environment-pill"><span /> DEMO ENVIRONMENT</span>
            <button className="avatar" title="Publisher profile">{(settings.userId || 'P').slice(0, 1).toUpperCase()}</button>
          </div>
        </header>
        {settingsOpen && <ApiSettings settings={settings} setSettings={setSettings} onClose={() => setSettingsOpen(false)} />}
        <div className="page-content">
          {(error || notice) && (
            <div className={`toast ${error ? 'toast-error' : 'toast-success'}`} role={error ? 'alert' : 'status'}>
              <span>{error ? '!' : '✓'}</span>
              <p>{error || notice}</p>
              <button onClick={clearAlerts} aria-label="Dismiss notification">×</button>
            </div>
          )}
          <AppRoutes activeView={activeView} {...viewProps} />
        </div>
      </main>
      {busy && <div className="busy-indicator"><span /> Syncing with ReleaseHub APIs…</div>}
    </div>
  )
}

export default App
