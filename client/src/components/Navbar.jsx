import navItems from './navItems'

export default function Navbar({ activeView, onNavigate, connection, softwareCount, apiOrigin, onOpenSettings }) {
  return (
    <aside className="sidebar">
      <a className="brand" href="#" onClick={(event) => { event.preventDefault(); onNavigate('discover') }}>
        <span className="brand-mark">R</span>
        <span>release<span className="brand-light">hub</span></span>
      </a>
      <div className="sidebar-label">WORKSPACE</div>
      <nav className="side-nav" aria-label="Main navigation">
        {navItems.map((item) => (
          <button
            className={`nav-item ${activeView === item.id ? 'is-active' : ''}`}
            key={item.id}
            onClick={() => onNavigate(item.id)}
          >
            <span className="nav-icon">{item.icon}</span>
            <span>{item.label}</span>
            {item.id === 'catalog' && <span className="nav-count">{softwareCount}</span>}
          </button>
        ))}
      </nav>
      <div className="sidebar-spacer" />
      <button className={`connection-card ${connection}`} onClick={onOpenSettings}>
        <span className="connection-dot" />
        <span>
          <strong>{connection === 'connected' ? 'API connected' : connection === 'checking' ? 'Connecting…' : 'API offline'}</strong>
          <small>{apiOrigin || 'Local service proxy'}</small>
        </span>
        <span className="settings-cog">⚙</span>
      </button>
      <div className="sidebar-foot">A release platform demo<br />built for teams that ship.</div>
    </aside>
  )
}
