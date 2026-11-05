export default function VersionList({ versions, onDownload, onDelete, onGet }) {
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
