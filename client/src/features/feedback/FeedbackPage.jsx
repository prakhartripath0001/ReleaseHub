export default function FeedbackPage({
  software,
  selectedSoftware,
  onSelectSoftware,
  versions,
  feedbackForm,
  setFeedbackForm,
  onSubmit,
  onLoadFeedback,
  feedback,
  busy,
  settings,
}) {
  return (
    <section className="view-section">
      <div className="view-header">
        <div><div className="eyebrow">COMMUNITY VOICE</div><h1>Feedback</h1><p className="page-lead">Ratings and notes help great software get even better.</p></div>
        <span className="large-stamp">✳</span>
      </div>
      <div className="feedback-layout">
        <form className="form-card" onSubmit={onSubmit}>
          <div className="form-card-heading"><span className="step-number step-number--orange">✳</span><div><h2>Leave a review</h2><p>One review per user for each release.</p></div></div>
          <label>Software <select required value={selectedSoftware?.id ?? ''} onChange={(event) => onSelectSoftware(event.target.value)}><option value="">Choose software…</option>{software.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>
          <label>Release <select required value={feedbackForm.versionId} onChange={(event) => setFeedbackForm({ ...feedbackForm, versionId: event.target.value })}><option value="">Choose a release…</option>{versions.map((version) => <option key={version.id} value={version.id}>{version.versionNumber} · {version.os}</option>)}</select></label>
          <label>Your user ID <input value={feedbackForm.userId} onChange={(event) => setFeedbackForm({ ...feedbackForm, userId: event.target.value })} placeholder={settings.userId || 'demo-user'} /></label>
          <div className="rating-field"><span>Your rating</span><div className="star-picker" role="radiogroup" aria-label="Rating">
            {[1, 2, 3, 4, 5].map((rating) => <button type="button" role="radio" aria-checked={Number(feedbackForm.rating) === rating} className={Number(feedbackForm.rating) >= rating ? 'star selected' : 'star'} key={rating} onClick={() => setFeedbackForm({ ...feedbackForm, rating })} aria-label={`${rating} stars`}>★</button>)}
          </div><strong>{feedbackForm.rating} / 5</strong></div>
          <label>Review <textarea rows="4" maxLength="4000" value={feedbackForm.comment} onChange={(event) => setFeedbackForm({ ...feedbackForm, comment: event.target.value })} placeholder="Share what worked well, or what could be improved…" /></label>
          <button className="primary-button" disabled={busy || !feedbackForm.versionId}>Submit feedback <span>→</span></button>
        </form>
        <div className="feedback-list-panel">
          <div className="section-heading"><div><span className="eyebrow">RECENT REVIEWS</span><h2>{selectedSoftware?.name || 'Choose a product'}</h2></div><button className="quiet-button" onClick={onLoadFeedback} disabled={!selectedSoftware}>Refresh <span>⟳</span></button></div>
          {!selectedSoftware ? <div className="empty-inline">Select a product in the review form to see its feedback.</div> : feedback.length ? <div className="review-list">{feedback.map((item) => <article className="review-card" key={item.id}><div className="review-card__top"><strong>{item.userId || 'Community member'}</strong><time>{item.createdAt ? new Date(item.createdAt).toLocaleDateString() : 'Just now'}</time></div><div className="review-stars">{'★'.repeat(item.rating)}<span>{'★'.repeat(5 - item.rating)}</span></div><p>{item.comment || 'No written comment.'}</p><small>Release {versions.find((version) => version.id === item.versionId)?.versionNumber ?? item.versionId}</small></article>)}</div> : <div className="empty-inline">No reviews yet. Be the first to leave feedback.</div>}
        </div>
      </div>
    </section>
  )
}
