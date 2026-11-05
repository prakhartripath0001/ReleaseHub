import CatalogPage from '../features/catalog/CatalogPage'
import FeedbackPage from '../features/feedback/FeedbackPage'
import PublisherPage from '../features/publisher/PublisherPage'
import ReportsPage from '../features/reports/ReportsPage'
import SearchPage from '../features/search/SearchPage'

export default function AppRoutes({ activeView, ...props }) {
  if (activeView === 'catalog') {
    return (
      <CatalogPage
        software={props.software}
        catalogFilters={props.catalogFilters}
        setCatalogFilters={props.setCatalogFilters}
        onFilter={props.onFilter}
        onClear={props.onClearFilters}
        onAdd={props.onAddSoftware}
        onOpen={props.onOpenSoftware}
        onEdit={props.onEditSoftware}
        onDelete={props.onDeleteSoftware}
        selectedSoftware={props.selectedSoftware}
        versions={props.versions}
        onCloseDetail={props.onCloseDetail}
        onAddRelease={props.onAddRelease}
        onDownload={props.onDownload}
        onDeleteVersion={props.onDeleteVersion}
        onGetVersion={props.onGetVersion}
      />
    )
  }
  if (activeView === 'publish') {
    return (
      <PublisherPage
        editingSoftwareId={props.editingSoftwareId}
        softwareForm={props.softwareForm}
        setSoftwareForm={props.setSoftwareForm}
        onSaveSoftware={props.onSaveSoftware}
        onCancelEdit={props.onCancelEdit}
        software={props.software}
        selectedSoftware={props.selectedSoftware}
        versions={props.versions}
        onSelectSoftware={props.onSelectSoftware}
        versionForm={props.versionForm}
        setVersionForm={props.setVersionForm}
        onSaveVersion={props.onSaveVersion}
        busy={props.busy}
        onReindex={props.onReindex}
      />
    )
  }
  if (activeView === 'feedback') {
    return (
      <FeedbackPage
        software={props.software}
        selectedSoftware={props.selectedSoftware}
        onSelectSoftware={props.onSelectSoftware}
        versions={props.versions}
        feedbackForm={props.feedbackForm}
        setFeedbackForm={props.setFeedbackForm}
        onSubmit={props.onSubmitFeedback}
        onLoadFeedback={props.onLoadFeedback}
        feedback={props.feedback}
        busy={props.busy}
        settings={props.settings}
      />
    )
  }
  if (activeView === 'reports') {
    return (
      <ReportsPage
        software={props.software}
        selectedSoftware={props.selectedSoftware}
        onSelectSoftware={props.onSelectSoftware}
        report={props.report}
        onLoadReport={props.onLoadReport}
        busy={props.busy}
      />
    )
  }
  return (
    <SearchPage
      searchForm={props.searchForm}
      setSearchForm={props.setSearchForm}
      onSearch={props.onSearch}
      busy={props.busy}
      searchResult={props.searchResult}
      software={props.software}
      connection={props.connection}
      onOpen={props.onOpenSoftware}
      onBrowseCatalog={props.onBrowseCatalog}
      onAddSoftware={props.onAddSoftware}
    />
  )
}
