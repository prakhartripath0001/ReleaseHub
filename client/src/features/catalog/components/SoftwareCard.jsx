export default function SoftwareCard({ item, onOpen, showManagement = false, onEdit, onDelete }) {
  return (
    <article className="software-card">
      <div className="software-card__top">
        <div className="app-mark">{(item.name || 'R').slice(0, 1).toUpperCase()}</div>
        <span className="category-chip">{item.category}</span>
      </div>
      <button className="text-button software-title" onClick={() => onOpen(item)}>{item.name}</button>
      <p className="software-description">{item.description || 'No description provided yet.'}</p>
      <div className="tag-row">
        {(item.tags ?? []).slice(0, 3).map((tag) => <span className="tag" key={tag}>{tag}</span>)}
      </div>
      <div className="software-card__footer">
        <span className="muted small">{item.publisherId || 'Community publisher'}</span>
        <button className="quiet-button" onClick={() => onOpen(item)}>View releases <span>→</span></button>
      </div>
      {showManagement && (
        <div className="card-actions">
          <button className="quiet-button" onClick={() => onEdit(item)}>Edit</button>
          <button className="quiet-button danger-text" onClick={() => onDelete(item)}>Delete</button>
        </div>
      )}
    </article>
  )
}
