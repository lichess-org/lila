import { type Prop, type Toggle, escapeHtml, propWithEffect, toggle } from 'lib';
import * as licon from 'lib/licon';
import { bind, dataIcon, onInsert, snabDialog } from 'lib/view';
import { h, type VNode } from 'snabbdom';
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
    return q
      ? this.chapters.all().filter((c: ChapterPreview) => c.name.toLowerCase().includes(q))
      : this.chapters.all();
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

export function view(ctrl: SearchCtrl) {
  const cleanQuery = ctrl.cleanQuery();
  const highlightRegex = cleanQuery && new RegExp(escapeRegExp(cleanQuery), 'gi');
  return snabDialog({
    class: 'study-search',
    onClose() {
      ctrl.open(false);
    },
    modal: true,
    vnodes: [
      h('input', {
        attrs: { autofocus: 1, placeholder: `Search in ${ctrl.studyName}`, value: ctrl.query() },
        hook: onInsert((el: HTMLInputElement) => {
          el.addEventListener('input', (e: KeyboardEvent) =>
            ctrl.query((e.target as HTMLInputElement).value.trim()),
          );
          el.addEventListener('keydown', (e: KeyboardEvent) => e.key === 'Enter' && ctrl.setFirstChapter());
        }),
      }),
      h(
        // dynamic extra class necessary to fully redraw the results and produce innerHTML
        `div.study-search__results.search-query-${cleanQuery}`,
        { attrs: { tabindex: -1 } },
        ctrl.results().map(c =>
          h('button', { hook: bind('click', () => ctrl.setChapter(c.id)) }, [
            h(
              'h3',
              {
                hook: highlightRegex
                  ? {
                      insert(vnode: VNode) {
                        if (c.name !== escapeHtml(c.name)) return;
                        const el = vnode.elm as HTMLElement;
                        el.innerHTML = c.name.replace(highlightRegex, `<high>$&</high>`);
                      },
                    }
                  : {},
              },
              c.name,
            ),
            c.playing
              ? h('ongoing', { attrs: { ...dataIcon(licon.DiscBig), title: 'Ongoing' } })
              : c.status && h('res', c.status),
          ]),
        ),
      ),
    ],
  });
}
