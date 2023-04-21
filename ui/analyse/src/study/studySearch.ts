import { Prop, propWithEffect } from 'common';
import { snabModal } from 'common/modal';
import { bind, dataIcon } from 'common/snabbdom';
import { h, VNode } from 'snabbdom';
import { Redraw } from '../interfaces';
import { StudyChapterMeta } from './interfaces';

export class SearchCtrl {
  open: Prop<boolean>;
  query: Prop<string> = propWithEffect('', this.redraw);

  constructor(
    readonly studyName: string,
    readonly chapters: Prop<StudyChapterMeta[]>,
    readonly rootSetChapter: (id: String) => void,
    readonly redraw: Redraw
  ) {
    this.open = propWithEffect(true, redraw);
    lichess.pubsub.on('study.search.open', () => this.open(true));
  }

  cleanQuery = () => this.query().toLowerCase().trim();

  results = () => {
    const q = this.cleanQuery();
    return q ? this.chapters().filter(this.match(q)) : this.chapters();
  };

  setChapter = (id: string) => {
    this.rootSetChapter(id);
    this.open(false);
    this.query('');
  };

  private tokenize = (c: StudyChapterMeta) => c.name.toLowerCase().split(' ');

  private match = (q: string) => (c: StudyChapterMeta) =>
    q.includes(' ') ? c.name.toLowerCase().includes(q) : this.tokenize(c).some(t => t.startsWith(q));
}

export function view(ctrl: SearchCtrl) {
  if (!ctrl.open()) return;
  const cleanQuery = ctrl.cleanQuery();
  return snabModal({
    class: 'study-search',
    onClose() {
      ctrl.open(false);
    },
    content: [
      h('input', {
        attrs: { autofocus: 1, placeholder: `Search in ${ctrl.studyName}`, value: ctrl.query() },
        hook: bind('input', (e: KeyboardEvent) => {
          ctrl.query((e.target as HTMLInputElement).value.trim());
        }),
      }),
      h(
        // dynamic extra class necessary to fully redraw the results and produce innerHTML
        `div.study-search__results.search-query-${cleanQuery}`,
        ctrl.results().map(c =>
          h(
            'div',
            {
              hook: bind('click', () => ctrl.setChapter(c.id)),
            },
            [
              h(
                'h3',
                {
                  hook: cleanQuery
                    ? {
                        insert(vnode: VNode) {
                          const el = vnode.elm as HTMLElement;
                          el.innerHTML = c.name.replace(new RegExp(cleanQuery, 'gi'), '<high>$&</high>');
                        },
                      }
                    : {},
                },
                c.name
              ),
              c.ongoing ? h('ongoing', { attrs: { ...dataIcon(''), title: 'Ongoing' } }) : null,
              !c.ongoing && c.res ? h('res', c.res) : null,
            ]
          )
        )
      ),
    ],
  });
}
