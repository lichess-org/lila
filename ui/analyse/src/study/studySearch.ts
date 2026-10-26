import { h } from 'snabbdom';

import { type Prop, type Toggle, propWithEffect, toggle, escapeHtml } from 'lib';
import { licon } from 'lib/licon';
import { bind, dataIcon, enter, onInsert, snabDialog } from 'lib/view';

import type { ChapterPreview } from './interfaces';
import type { StudyChapters } from './studyChapters';

export class SearchCtrl {
  open: Toggle;
  query: Prop<string> = propWithEffect('', this.redraw);

  constructor(
    readonly studyName: string,
    readonly chapters: StudyChapters,
    readonly rootSetChapter: (id: string) => void,
    readonly redraw: Redraw,
  ) {
    this.open = toggle(false, () => this.query(''));
  }

  cleanQuery = () => this.query().toLowerCase().trim();

  results = () => {
    const q = this.cleanQuery();
    if (!q) return this.chapters.all();
    return this.chapters.all().filter((c: ChapterPreview) => c.name.toLowerCase().includes(q));
  };

  setChapter = (id: string) => {
    this.rootSetChapter(id);
    this.open(false);
    this.query('');
  };

  setFirstChapter = () => {
    const c = this.results()[0];
    if (c) this.setChapter(c.id);
  };
}

const escapeRegExp = (s: string) => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'); // $& means the whole matched string

export function view(ctrl: SearchCtrl, type: 'study' | 'broadcast') {
  const cleanQuery = ctrl.cleanQuery();
  const results = ctrl.results();
  const highlightRegex = cleanQuery && new RegExp(escapeRegExp(cleanQuery), 'gi');
  const isBroadcast = type === 'broadcast';

  return snabDialog({
    class: 'study-search',
    onClose() {
      ctrl.open(false);
    },
    noScrollable: true,
    easyClose: 'clickOutside',
    modal: true,
    vnodes: [
      h('h2', isBroadcast ? i18n.study.searchGames : i18n.study.searchChapters),
      h(
        'div.search-wrapper',
        h('input', {
          attrs: { autofocus: 1, placeholder: i18n.site.searchInX(ctrl.studyName), value: ctrl.query() },
          hook: onInsert((el: HTMLInputElement) => {
            el.addEventListener('input', (e: KeyboardEvent) =>
              ctrl.query((e.target as HTMLInputElement).value.trim()),
            );
            el.addEventListener('keydown', enter(ctrl.setFirstChapter));
          }),
        }),
      ),
      h(
        // dynamic extra class necessary to fully redraw the results and produce innerHTML
        `div.study-search__results.search-query-${encodeURIComponent(cleanQuery)}`,
        { attrs: { tabindex: -1 } },
        results.length > 0
          ? results.map(c =>
              h('button', { hook: bind('click', () => ctrl.setChapter(c.id)) }, [
                h(
                  'h3',
                  highlightRegex
                    ? {
                        hook: onInsert((el: HTMLElement) => {
                          el.innerHTML = escapeHtml(c.name).replace(highlightRegex, '<high>$&</high>');
                        }),
                      }
                    : {},
                  c.name,
                ),
                c.playing
                  ? h('ongoing', { attrs: { ...dataIcon(licon.DiscBig), title: 'Ongoing' } })
                  : c.status && h('res', c.status),
              ]),
            )
          : h('p.no-results', i18n.site.thereAreNoResultsForX(cleanQuery)),
      ),
    ],
  });
}
