export default function ApiSettings({ settings, setSettings, onClose }) {
  return (
    <section className="settings-panel">
      <div className="settings-panel__heading">
        <div><strong>API connection</strong><p>Use the local proxy by default, or point the app to Kong.</p></div>
        <button className="icon-button" onClick={onClose} aria-label="Close settings">×</button>
      </div>
      <div className="settings-grid">
        <label>API base URL <input value={settings.origin} placeholder="Same origin / local Vite proxy" onChange={(event) => setSettings({ ...settings, origin: event.target.value })} /></label>
        <label>JWT bearer token <input type="password" value={settings.token} placeholder="Optional for direct local services" onChange={(event) => setSettings({ ...settings, token: event.target.value })} /></label>
        <label>Catalog role <select value={settings.role} onChange={(event) => setSettings({ ...settings, role: event.target.value })}><option>PUBLISHER</option><option>ADMIN</option><option>USER</option></select></label>
        <label>Demo user ID <input value={settings.userId} placeholder="demo-user" onChange={(event) => setSettings({ ...settings, userId: event.target.value })} /></label>
      </div>
      <p className="settings-hint">Local mode forwards requests through Vite to ports 8083–8086. Gateway mode requires a valid JWT. Role headers are for the project’s local catalog filter.</p>
    </section>
  )
}
