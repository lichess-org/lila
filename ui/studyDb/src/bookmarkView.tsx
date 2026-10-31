import { licon } from 'lib/licon';
import { jsx, type VNode } from 'lib/view';

import type { BookmarkTabContent } from './bookmarkTabContent';
import {
  highlightMatch,
  infiniteScroll,
  listData,
  studyIcon,
  studyLink,
  studyNavigator,
  studyFilterInput,
  studyMatchView,
  studyPublicIcon,
} from './studyListView';
import { studyDragType, type BookmarkFolder, type StudyDrag } from './types';

type Row = BookmarkTabContent['rows'][number];

export function bookmarkView(content: BookmarkTabContent): VNode {
  const selected = [...content.selected];
  const clearDrop = () => {
    for (const target of document.querySelectorAll('.study-db [data-bookmark-drop]'))
      target.removeAttribute('data-bookmark-drop');
  };
  const insertion = (event: DragEvent, row?: Row) => {
    if (!row) return { parent: undefined, before: undefined, position: 'before' };
    const { entry, path } = row;
    const parent = path.length > 1 ? (content.at(path.slice(0, -1)) as BookmarkFolder) : undefined;
    const rect = (event.currentTarget as HTMLElement).getBoundingClientRect();
    if (event.clientY < rect.top + rect.height / 2) return { parent, before: entry, position: 'before' };
    if ('children' in entry) return { parent: entry, before: entry.children[0], position: 'inside' };
    const siblings = parent?.children ?? content.entries;
    return { parent, before: siblings[siblings.indexOf(entry) + 1], position: 'after' };
  };
  const dragOver = (event: DragEvent, row?: Row) => {
    if (!event.dataTransfer!.types.includes(studyDragType)) return;
    event.preventDefault();
    event.stopPropagation();
    clearDrop();
    (event.currentTarget as HTMLElement).dataset.bookmarkDrop = insertion(event, row).position;
    event.dataTransfer!.dropEffect = event.dataTransfer!.effectAllowed === 'copy' ? 'copy' : 'move';
  };
  const drop = (event: DragEvent, row?: Row) => {
    if (!event.dataTransfer!.types.includes(studyDragType)) return;
    event.preventDefault();
    event.stopPropagation();
    const target = insertion(event, row);
    clearDrop();
    content.drop(
      JSON.parse(event.dataTransfer!.getData(studyDragType)) as StudyDrag,
      target.parent,
      target.before,
    );
  };
  return (
    <div
      class="study-list"
      {...listData(content.id)}
      on={{
        dragend: clearDrop,
        keydown: (event: KeyboardEvent) => {
          if ((event.target as HTMLElement).closest('input')) return;
          if (event.key === 'Delete') {
            event.preventDefault();
            content.removeSelected();
          } else if (event.key === 'F2' && selected.length === 1) {
            event.preventDefault();
            content.rename(selected[0]);
          } else if ((event.ctrlKey || event.metaKey) && event.key === 'a') {
            event.preventDefault();
            for (const row of content.results) content.selected.add(row.entry);
            content.onChange();
          }
        },
      }}>
      <div class="bookmark-controls">
        {studyFilterInput(content)}
        <button
          key="folder-create"
          class="button-empty bookmark-new-folder"
          attrs={{
            type: 'button',
            title: 'New folder',
            'aria-label': 'New folder',
            'data-icon': licon.NewFolder,
          }}
          on={{ click: () => content.createFolder() }}
        />
        <button
          key="bookmark-delete"
          class="button-empty bookmark-delete"
          attrs={{
            type: 'button',
            title: 'Remove selected',
            'aria-label': 'Remove selected',
            'data-icon': licon.Trash,
            disabled: !content.selected.size,
          }}
          on={{ click: () => content.removeSelected() }}
        />
      </div>
      {content.error && (
        <div class="error" attrs={{ role: 'alert' }}>
          {content.error.message}
          <button attrs={{ type: 'button' }} on={{ click: () => content.error!.retry() }}>
            Retry
          </button>
        </div>
      )}
      <div class="body bookmarks-body" key={content.revision}>
        <ul class="studies" attrs={{ role: 'tree', 'aria-multiselectable': 'true' }}>
          {content.results.map(row => {
            const { entry, path } = row;
            const folder = 'children' in entry;
            const study = folder ? undefined : content.studies.get(entry.studyId);
            return (
              <li
                key={path.join('/')}
                class={['study-row', 'bookmark-row', content.selected.has(entry) && 'selected']}
                attrs={{
                  role: 'treeitem',
                  tabindex: 0,
                  draggable: String(content.editing?.entry !== entry),
                  'aria-level': path.length,
                  'aria-selected': String(content.selected.has(entry)),
                  ...(folder ? { 'aria-expanded': String(content.expanded.has(entry)) } : {}),
                  style: `---bookmark-depth:${path.length - 1}`,
                }}
                on={{
                  click: (event: MouseEvent) => {
                    if (!(event.target as HTMLElement).closest('a, button, input, form'))
                      content.select(entry, event);
                  },
                  keydown: (event: KeyboardEvent) => {
                    if ((event.target as HTMLElement).closest('a, button, input')) return;
                    if (event.key === ' ') {
                      event.preventDefault();
                      content.select(entry, event);
                    } else if (event.key === 'Enter') {
                      if (folder) content.toggle(entry);
                      else studyLink(entry).navigate();
                    }
                  },
                  dragstart: (event: DragEvent) => {
                    event.dataTransfer!.effectAllowed = 'move';
                    event.dataTransfer!.setData(studyDragType, JSON.stringify(content.drag(entry)));
                  },
                  dragover: (event: DragEvent) => dragOver(event, row),
                  dragleave: clearDrop,
                  drop: (event: DragEvent) => drop(event, row),
                }}>
                {folder ? (
                  <button
                    class="bookmark-toggle"
                    attrs={{
                      type: 'button',
                      title: content.expanded.has(entry) ? 'Collapse folder' : 'Expand folder',
                      'aria-label': `${content.expanded.has(entry) ? 'Collapse' : 'Expand'} ${entry.name}`,
                    }}
                    on={{ click: () => content.toggle(entry) }}>
                    <svg attrs={{ viewBox: '0 0 10 10', 'aria-hidden': 'true', focusable: 'false' }}>
                      <path attrs={{ d: 'M2 1L8 5L2 9Z', fill: 'currentColor' }} />
                    </svg>
                  </button>
                ) : (
                  <span />
                )}
                {folder ? (
                  <span class="study-icon" attrs={{ 'data-icon': licon.Folder, 'aria-hidden': 'true' }} />
                ) : (
                  studyIcon(study)
                )}
                {content.editing?.entry === entry ? (
                  <form
                    class="bookmark-rename-form"
                    on={{
                      submit: (event: Event) => {
                        event.preventDefault();
                        content.finishRename();
                      },
                    }}>
                    <input
                      attrs={{ type: 'text', 'aria-label': 'Name', maxlength: 200 }}
                      props={{ value: content.editing.name }}
                      hook={{
                        insert: (vnode: VNode) => {
                          const input = vnode.elm as HTMLInputElement;
                          input.focus();
                          input.select();
                        },
                      }}
                      on={{
                        input: (event: Event) => {
                          content.editing!.name = (event.target as HTMLInputElement).value;
                        },
                        keydown: (event: KeyboardEvent) => {
                          if (event.key === 'Escape') {
                            event.preventDefault();
                            event.stopPropagation();
                            content.editing = undefined;
                            content.onChange();
                          }
                        },
                      }}
                    />
                    <button
                      attrs={{
                        type: 'submit',
                        title: 'Save name',
                        'aria-label': 'Save name',
                        'data-icon': licon.Checkmark,
                      }}
                    />
                  </form>
                ) : (
                  <div class="study-content">
                    <span class="bookmark-name">
                      {highlightMatch(entry.name, [content.query])}
                      {study && 'visibility' in study && studyPublicIcon(study)}
                      <button
                        class="button-empty bookmark-rename"
                        attrs={{
                          type: 'button',
                          title: 'Rename',
                          'aria-label': `Rename ${entry.name}`,
                          'data-icon': licon.Pencil,
                        }}
                        on={{ click: () => content.rename(entry) }}
                      />
                    </span>
                  </div>
                )}
                {!folder && studyNavigator(study ?? entry)}
                {study && content.editing?.entry !== entry && studyMatchView(study, () => [content.query])}
              </li>
            );
          })}
        </ul>
        {content.hasMore && (
          <div
            class="scroll-boundary"
            attrs={{ 'aria-hidden': 'true' }}
            hook={infiniteScroll(() => content.loadMore())}
          />
        )}
        <div
          class="bookmark-end"
          attrs={{ 'aria-label': 'Bookmarks root' }}
          on={{
            dragover: (event: DragEvent) => dragOver(event),
            dragleave: clearDrop,
            drop: (event: DragEvent) => drop(event),
          }}>
          {!content.entries.length && <span class="status">{i18n.site.none}</span>}
        </div>
      </div>
    </div>
  );
}
