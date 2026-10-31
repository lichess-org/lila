import { jsx, type VNode } from 'lib/view';

import type { HistoryTabContent } from './historyTabContent';
import { infiniteScroll, listData, studyRow, studyFilterInput } from './studyListView';

export function historyView(content: HistoryTabContent): VNode {
  const results = content.results;
  return (
    <div class="study-list" {...listData(content.id)}>
      {studyFilterInput(content)}
      <div class="body" key={content.revision}>
        {results.length ? (
          <ul class="studies" attrs={{ role: 'listbox', 'aria-multiselectable': 'true' }}>
            {results.map(study => studyRow(content, study))}
          </ul>
        ) : (
          !content.error && <div class="status">{i18n.site.none}</div>
        )}
        {content.error && (
          <div class="error" attrs={{ role: 'alert' }}>
            {content.error}
            <button attrs={{ type: 'button' }} on={{ click: () => content.retry() }}>
              Retry
            </button>
          </div>
        )}
        {content.hasMore && !content.error && (
          <div
            class="scroll-boundary"
            attrs={{ 'aria-hidden': 'true' }}
            hook={infiniteScroll(() => content.loadMore())}
          />
        )}
      </div>
    </div>
  );
}
