import { licon } from 'lib/licon';
import { pubsub } from 'lib/pubsub';
import { jsx, spinnerVdom, type Hooks, type VNode, type VNodeData } from 'lib/view';
import { url } from 'lib/xhr';

import type { HistoryTabContent } from './historyTabContent';
import type { MemberTabContent } from './memberTabContent';
import { popupData } from './popupData';
import type { ResultTabContent } from './resultTabContent';
import type { Bookmark, StudyData, StudyPosition } from './types';

export function studyListView(list: MemberTabContent | ResultTabContent, controls?: VNode): VNode {
  const results = list.results;
  const isResult = list.kind === 'resultTab';
  const body = (
    <div class="body" key={list.revision}>
      {results.length > 0 && (
        <ul class="studies" attrs={{ role: 'listbox', 'aria-multiselectable': 'true' }}>
          {results.map(study => studyRow(list, study))}
        </ul>
      )}
      {list.error && (
        <div class="error" attrs={{ role: 'alert' }}>
          {list.error}
          <button attrs={{ type: 'button' }} on={{ click: () => list.retry() }}>
            Retry
          </button>
        </div>
      )}
      {list.loading && isResult && (
        <div class="status" attrs={{ role: 'status' }}>
          {spinnerVdom()}
        </div>
      )}
      {!list.loading && !list.error && !results.length && (list.exhausted || !list.hasMore) && (
        <div class="status">{i18n.site.none}</div>
      )}
      {list.hasMore && (!list.loading || list.exhausted) && (!list.error || list.exhausted) && (
        <div
          class="scroll-boundary"
          attrs={{ 'aria-hidden': 'true' }}
          hook={infiniteScroll(() => list.loadMore())}
        />
      )}
    </div>
  );
  return (
    <div class="study-list" {...listData(list.id)}>
      {!isResult && studyFilterInput(list)}
      {isResult && <div class="submitted-query">{list.submitted}</div>}
      {controls}
      {body}
    </div>
  );
}

export function studyRow(
  list: HistoryTabContent | MemberTabContent | ResultTabContent,
  study: StudyData,
): VNode {
  const position = list.kind === 'historyTab' ? study.visit : undefined;
  const chapterName = study.chapters?.find(chapter => chapter.id === position?.chapterId)?.name;
  const selected = list.selected.has(study.studyId);
  const entry = 'visibility' in study ? study : undefined;
  const termsFor =
    list.kind === 'resultTab' ? (field: string) => list.highlights[field] ?? [] : () => [list.query];
  const matchingFields =
    list.kind === 'resultTab' ? list.matchingChapterFields.get(study.studyId) : undefined;
  const name = highlightMatch(study.studyName, termsFor('name'));
  return (
    <li
      class={['study-row', selected && 'selected']}
      key={study.studyId}
      attrs={{ role: 'option', tabindex: 0, 'aria-selected': String(selected), draggable: 'true' }}
      on={{
        click: (event: MouseEvent) => {
          if (!(event.target as HTMLElement).closest('a, button')) list.select(study.studyId, event);
        },
        keydown: (event: KeyboardEvent) => {
          if ((event.target as HTMLElement).closest('a, button')) return;
          if (event.key === ' ') {
            event.preventDefault();
            list.select(study.studyId, event);
          } else if (event.key === 'Enter') studyLink(study, position).navigate();
        },
        dragstart: (event: DragEvent) => list.drag(study.studyId, event),
      }}>
      {studyIcon(study)}
      <div class="study-content">
        {position ? <strong class="study-name">{name}</strong> : <span class="study-name">{name}</span>}
        {entry && studyPublicIcon(entry)}
        {chapterName && <span class="study-chapter">{chapterName}</span>}
      </div>
      {studyNavigator(study, position, matchingFields)}
      {studyMatchView(study, termsFor, matchingFields)}
    </li>
  );
}

export function studyPublicIcon(entry: StudyData): VNode | undefined {
  return entry.visibility === 'public' ? (
    <span
      class="study-public"
      attrs={{
        role: 'img',
        title: 'Public study',
        'aria-label': 'Public study',
        'data-icon': licon.Meridians,
      }}></span>
  ) : undefined;
}

export function studyNavigator(
  study: StudyData | Bookmark,
  position?: StudyPosition,
  matchingFields?: StudyData['matchingChapterFields'],
): VNode {
  const entry = 'visibility' in study ? study : undefined;
  const link = studyLink(study, position);
  const chapterName = entry?.chapters?.find(chapter => chapter.id === position?.chapterId)?.name;
  const chapters =
    entry?.chapters?.map(chapter => {
      const fields = matchingFields?.[chapter.id];
      return {
        name: fields?.length ? `${chapter.name} - ${fields.join(', ')}` : chapter.name,
        ...studyLink(study, { chapterId: chapter.id }),
      };
    }) ?? [];
  const count = entry?.chapterCount;
  const preview = popupData(chapters);
  return (
    <span class="study-navigation">
      <button class="button-empty study-chapter-count" {...preview}>
        {count}
      </button>
      <a
        class="study-navigate"
        attrs={{
          href: link.href,
          'aria-label': `Navigate to ${study.studyName}${chapterName ? ' - ' + chapterName : ''}`,
          'data-icon': licon.InternalArrow,
        }}
        on={{
          click: (event: MouseEvent) => {
            if (event.button || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
            event.preventDefault();
            link.navigate();
          },
        }}></a>
    </span>
  );
}

export function studyFilterInput(content: { query: string; filterChanged(): void }): VNode {
  return (
    <input
      class="study-filter"
      attrs={{ type: 'search', placeholder: 'Filter', 'aria-label': 'Filter', maxlength: 100 }}
      props={{ value: content.query }}
      on={{
        input: (event: Event) => {
          content.query = (event.target as HTMLInputElement).value;
          content.filterChanged();
        },
      }}
    />
  );
}

export function studyMatchView(
  entry: StudyData,
  termsFor: (field: string) => readonly string[],
  matchingFields?: StudyData['matchingChapterFields'],
): VNode | undefined {
  const matches = (text: string | undefined, field: string) =>
    !!text &&
    termsFor(field).some(term => term.trim() && text.toLowerCase().includes(term.trim().toLowerCase()));
  const lines: VNode[] = [];
  const descriptionTerms = termsFor('description');
  const topicTerms = termsFor('topics');
  const chapterNameTerms = termsFor('chapters.name');
  const chapterDescriptionTerms = termsFor('chapters.description');
  if (matches(entry.description, 'description'))
    lines.push(
      <div class="study-match" key="description">
        {highlightMatch(matchSnippet(entry.description!, descriptionTerms), descriptionTerms)}
      </div>,
    );
  if (entry.topics?.some(topic => matches(topic, 'topics')))
    lines.push(
      <div class="study-match" key="topics">
        {highlightMatch(entry.topics.join(', '), topicTerms)}
      </div>,
    );
  for (const chapter of entry.chapters ?? []) {
    const fields = matchingFields?.[chapter.id] ?? [];
    const tags =
      chapter.tags?.filter(
        ([name, value]) =>
          matches(value, `tags.${name.toLowerCase()}`) ||
          fields.some(field => field.toLowerCase() === `tags.${name.toLowerCase()}`),
      ) ?? [];
    const description =
      matches(chapter.description, 'chapters.description') || fields.includes('description')
        ? chapter.description
        : undefined;
    if (
      !matches(chapter.name, 'chapters.name') &&
      !tags.length &&
      description === undefined &&
      !fields.length
    )
      continue;
    const link = studyLink(entry, { chapterId: chapter.id });
    lines.push(
      <div class="study-match" key={chapter.id}>
        <span class="study-match-chapter">
          <a
            attrs={{ href: link.href }}
            on={{
              click: (event: MouseEvent) => {
                if (event.button || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
                event.preventDefault();
                link.navigate();
              },
            }}>
            {highlightMatch(chapter.name, chapterNameTerms)}
          </a>
          {tags.length || description !== undefined ? ': ' : ''}
        </span>
        {tags.map(([name, value], index) => (
          <span>
            {index > 0 ? '; ' : ''}
            {name}:{' '}
            {highlightMatch(
              matchSnippet(value, termsFor(`tags.${name.toLowerCase()}`)),
              termsFor(`tags.${name.toLowerCase()}`),
            )}
          </span>
        ))}
        {description !== undefined && (
          <span>
            {tags.length ? '; ' : ''}
            {highlightMatch(matchSnippet(description, chapterDescriptionTerms), chapterDescriptionTerms)}
          </span>
        )}
      </div>,
    );
  }
  return lines.length ? <div class="study-matches">{lines}</div> : undefined;
}

function matchRanges(text: string, terms: readonly string[], context = 0): [number, number][] {
  const lower = text.toLowerCase();
  const ranges: [number, number][] = [];
  for (const term of terms) {
    const query = term.trim().toLowerCase();
    if (!query) continue;
    let index = lower.indexOf(query);
    while (index >= 0) {
      ranges.push([Math.max(0, index - context), Math.min(text.length, index + query.length + context)]);
      index = lower.indexOf(query, index + 1);
    }
  }
  const merged: [number, number][] = [];
  for (const range of ranges.sort((left, right) => left[0] - right[0])) {
    const previous = merged[merged.length - 1];
    if (previous && range[0] <= previous[1]) previous[1] = Math.max(previous[1], range[1]);
    else merged.push(range);
  }
  return merged;
}

function matchSnippet(text: string, terms: readonly string[]): string {
  const ranges = matchRanges(text, terms, 40);
  if (!ranges.length) return text;
  return `${ranges[0][0] ? '...' : ''}${ranges.map(([start, end]) => text.slice(start, end)).join(' ... ')}${
    ranges[ranges.length - 1][1] < text.length ? '...' : ''
  }`;
}

export function highlightMatch(text: string, terms: readonly string[]): (string | VNode)[] {
  const parts: (string | VNode)[] = [];
  let offset = 0;
  for (const [start, end] of matchRanges(text, terms)) {
    parts.push(text.slice(offset, start), <mark>{text.slice(start, end)}</mark>);
    offset = end;
  }
  parts.push(text.slice(offset));
  return parts;
}

export function studyLink(study: Pick<StudyData, 'studyId'>, position?: StudyPosition) {
  return {
    href: url(['/study', study.studyId, position?.chapterId].filter(Boolean).join('/'), {
      path: position?.path,
      orientation: position?.orientation,
    }),
    navigate: () => pubsub.emit('study.navigate', { studyId: study.studyId, ...position }),
  };
}

export function studyIcon(study?: StudyData): VNode {
  const sorted = study?.members?.slice().sort((a, b) => {
    if (a.user.id === study.ownerId) return -1;
    if (b.user.id === study.ownerId) return 1;
    if (a.role === 'w' && b.role !== 'w') return -1;
    if (b.role === 'w' && a.role !== 'w') return 1;
    return a.user.name.localeCompare(b.user.name);
  });
  const members =
    sorted?.map(({ user, role }) => {
      const permission = user.id === study?.ownerId ? 'Owner' : role === 'w' ? 'Contributor' : 'Member';
      return { name: `${user.title ? user.title + ' ' : ''}${user.name} - ${permission}` };
    }) ?? (study?.owner ? [{ name: study.owner.name + ' - Owner' }] : []);
  const preview = popupData(members, study?.studyName);
  const attrs = { ...preview.attrs, 'aria-label': study ? 'Study members' : undefined };
  return study?.flair ? (
    <img
      class="study-icon"
      {...preview}
      attrs={{ ...attrs, src: site.asset.flairSrc(study.flair), alt: '', draggable: 'false' }}
    />
  ) : (
    <span
      class="study-icon"
      {...preview}
      attrs={{ ...attrs, 'data-icon': licon.StudyBoard, 'aria-hidden': !study }}
    />
  );
}

export function listData(tab: string): VNodeData {
  return {
    key: tab,
    attrs: {
      role: 'tabpanel',
      'aria-labelledby': tab,
    },
  };
}

export function infiniteScroll(loadMore: () => Promise<void>): Hooks {
  let observer: IntersectionObserver | undefined;
  const observe = (vnode: VNode) => {
    if (!window.IntersectionObserver) return;
    const boundary = vnode.elm as HTMLElement;
    observer = new window.IntersectionObserver(
      entries => {
        if (entries.some(entry => entry.isIntersecting)) {
          observer!.disconnect();
          loadMore();
        }
      },
      { root: boundary.parentElement, rootMargin: '0px 0px 100px 0px' },
    );
    observer.observe(boundary);
  };
  return {
    insert: observe,
    postpatch: (previous, vnode) => {
      previous.data?.hook?.destroy?.(previous);
      observe(vnode);
    },
    destroy: () => observer?.disconnect(),
  };
}
