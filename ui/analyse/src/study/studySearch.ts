import { h } from 'snabbdom';

import { escapeHtml } from 'lib';
import { licon } from 'lib/licon';
import { bind, dataIcon, enter, onInsert, snabDialog } from 'lib/view';

import type { StudyChapters } from './studyChapters';

export class SearchCtrl {
  constructor(
    readonly studyName: string,
    readonly chapters: StudyChapters,
    readonly rootSetChapter: (id: string) => void,
    readonly isBroadcast: boolean,
  ) {}

  open = () => {
    let query = '';
    return snabDialog({
      class: 'study-search study-modal',
      show: true,
      noScrollable: true,
      easyClose: 'clickOutside',
      modal: true,
      render: (redraw, dlg) => {
        const cleanQuery = query.toLowerCase();
        const results = this.chapters.all().filter(c => c.name.toLowerCase().includes(cleanQuery));
        const highlightRegex =
          cleanQuery && new RegExp(cleanQuery.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'gi');
        const setChapter = (id: string) => {
          this.rootSetChapter(id);
          dlg.close();
        };
        return [
          h('h2', this.isBroadcast ? i18n.study.searchGames : i18n.study.searchChapters),
          h(
            'div.search-wrapper',
            h('input', {
              attrs: { autofocus: 1, placeholder: i18n.site.searchInX(this.studyName) },
              hook: onInsert((el: HTMLInputElement) => {
                el.addEventListener('input', () => {
                  query = el.value.trim();
                  redraw();
                });
                el.addEventListener(
                  'keydown',
                  enter(() => {
                    const first = this.chapters
                      .all()
                      .find(c => c.name.toLowerCase().includes(query.toLowerCase()));
                    if (first) setChapter(first.id);
                  }),
                );
              }),
            }),
          ),
          h(
            // dynamic extra class necessary to fully redraw the results and produce innerHTML
            `div.study-search__results.search-query-${encodeURIComponent(cleanQuery)}`,
            { attrs: { tabindex: -1 } },
            results.length > 0
              ? results.map(c =>
                  h('button', { hook: bind('click', () => setChapter(c.id)) }, [
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
        ];
      },
    });
  };
}
