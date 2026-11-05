export default function PublisherPage({
  editingSoftwareId,
  softwareForm,
  setSoftwareForm,
  onSaveSoftware,
  onCancelEdit,
  software,
  selectedSoftware,
  versions,
  onSelectSoftware,
  versionForm,
  setVersionForm,
  onSaveVersion,
  busy,
  onReindex,
}) {
  return (
    <section className="view-section">
      <div className="view-header">
        <div><div className="eyebrow">PUBLISHER WORKSPACE</div><h1>{editingSoftwareId ? 'Edit software' : 'Ship something great.'}</h1><p className="page-lead">Add a product to the catalog, then publish its first release.</p></div>
        <div className="publisher-badge"><span>✦</span> PUBLISHER STUDIO</div>
      </div>
      <div className="publisher-layout">
        <form className="form-card" onSubmit={onSaveSoftware}>
          <div className="form-card-heading"><span className="step-number">01</span><div><h2>{editingSoftwareId ? 'Update product details' : 'Create a software entry'}</h2><p>Tell users what this product is about.</p></div></div>
          <label>Product name <input required maxLength="160" value={softwareForm.name} onChange={(event) => setSoftwareForm({ ...softwareForm, name: event.target.value })} placeholder="e.g. Studio North" /></label>
          <label>Description <textarea rows="3" value={softwareForm.description} onChange={(event) => setSoftwareForm({ ...softwareForm, description: event.target.value })} placeholder="What does your software help people do?" /></label>
          <div className="form-row">
            <label>Category <input value={softwareForm.category} onChange={(event) => setSoftwareForm({ ...softwareForm, category: event.target.value })} placeholder="developer-tools" /></label>
            {!editingSoftwareId && <label>Publisher ID <input value={softwareForm.publisherId} onChange={(event) => setSoftwareForm({ ...softwareForm, publisherId: event.target.value })} placeholder="Generated if blank" /></label>}
          </div>
          <label>Tags <input value={softwareForm.tags} onChange={(event) => setSoftwareForm({ ...softwareForm, tags: event.target.value })} placeholder="design, open-source, editor" /><small>Separate tags with commas.</small></label>
          <div className="form-actions"><button className="primary-button" disabled={busy}>{editingSoftwareId ? 'Save changes' : 'Create software'} <span>→</span></button>{editingSoftwareId && <button type="button" className="quiet-button" onClick={onCancelEdit}>Cancel edit</button>}</div>
        </form>

        <div className="release-column">
          <form className="form-card release-card" onSubmit={onSaveVersion}>
            <div className="form-card-heading"><span className="step-number step-number--green">02</span><div><h2>Publish a release</h2><p>Add a downloadable version for a product.</p></div></div>
            <label>Software entry <select value={selectedSoftware?.id ?? ''} onChange={(event) => onSelectSoftware(event.target.value)}><option value="">Select a product…</option>{software.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>
            <div className="form-row">
              <label>Version number <input required value={versionForm.versionNumber} onChange={(event) => setVersionForm({ ...versionForm, versionNumber: event.target.value })} placeholder="1.0.0" /></label>
              <label>Operating system <select value={versionForm.os} onChange={(event) => setVersionForm({ ...versionForm, os: event.target.value })}><option>LINUX</option><option>WINDOWS</option><option>MACOS</option><option>ANDROID</option><option>IOS</option></select></label>
            </div>
            <label>Download URL / file reference <input value={versionForm.fileRef} onChange={(event) => setVersionForm({ ...versionForm, fileRef: event.target.value })} placeholder="https://downloads.example.com/app.zip" /></label>
            <label>Release notes <textarea rows="2" value={versionForm.releaseNotes} onChange={(event) => setVersionForm({ ...versionForm, releaseNotes: event.target.value })} placeholder="What’s new in this version?" /></label>
            <button className="secondary-button" disabled={busy || !selectedSoftware}>Publish release <span>→</span></button>
          </form>
          {selectedSoftware && (
            <div className="publisher-releases">
              <div><strong>{selectedSoftware.name}</strong><span>{versions.length} published releases</span></div>
              {versions.map((version) => <small key={version.id}>{version.versionNumber} · {version.os}</small>)}
            </div>
          )}
          <div className="tip-card"><div className="tip-icon">✳</div><div><strong>One product. Every platform.</strong><p>Publish releases by operating system so people can find the right version for their setup.</p></div></div>
          <button className="reindex-button" onClick={onReindex} disabled={busy}><span>⟳</span> Rebuild search index <small>Admin action</small></button>
        </div>
      </div>
    </section>
  )
}
