import { onInsert } from 'common/snabbdom';
import throttle from 'common/throttle';
import { h, thunk, VNode } from 'snabbdom';
import { option } from '../view/util';
import { looksLikeLichessGame } from './studyChapters';
import { prop } from 'common';
import StudyCtrl from './studyCtrl';

export class TagsForm {
  selectedType = prop<string | undefined>(undefined);
  constructor(
    private readonly root: StudyCtrl,
    readonly types: string[],
  ) {}

  getChapter = () => this.root.data.chapter;

  private makeChange = throttle(500, (name: string, value: string) => {
    this.root.makeChange('setTag', {
      chapterId: this.getChapter().id,
      name,
      value: value.slice(0, 140),
    });
  });

  editable = () => this.root.vm.mode.write;

  submit = (name: string) => (value: string) => this.editable() && this.makeChange(name, value);
}

export function view(root: StudyCtrl): VNode {
  const chapter = root.tags.getChapter(),
    tagKey = chapter.tags.map(t => t[1]).join(','),
    key = chapter.id + root.data.name + chapter.name + root.data.likes + tagKey + root.vm.mode.write;
  return thunk('div.' + chapter.id, doRender, [root, key]);
}

const doRender = (root: StudyCtrl): VNode =>
  h('div', renderPgnTags(root.tags, root.trans, root.data.showRatings));

const editable = (value: string, submit: (v: string, el: HTMLInputElement) => void): VNode =>
  h('input', {
    key: value, // force to redraw on change, to visibly update the input value
    attrs: { spellcheck: 'false', value },
    hook: onInsert<HTMLInputElement>(el => {
      el.onblur = () => submit(el.value, el);
      el.onkeydown = e => {
        if (e.key === 'Enter') el.blur();
      };
    }),
  });

type TagRow = (string | VNode)[];

const fixed = ([key, value]: [string, string]) =>
  key.endsWith('FideId') ? h('a', { attrs: { href: `/fide/${value}/redirect` } }, value) : fixedValue(value);

const fixedValue = (value: string) => h('span', value);

function renderPgnTags(tags: TagsForm, trans: Trans, showRatings: boolean): VNode {
  let rows: TagRow[] = [];
  const chapter = tags.getChapter();
  if (chapter.setup.variant.key !== 'standard')
    rows.push(['Variant', fixedValue(chapter.setup.variant.name)]);
  rows = rows.concat(
    chapter.tags
      .filter(
        tag =>
          showRatings || !['WhiteElo', 'BlackElo'].includes(tag[0]) || !looksLikeLichessGame(chapter.tags),
      )
      .map(tag => [tag[0], tags.editable() ? editable(tag[1], tags.submit(tag[0])) : fixed(tag)]),
  );
  if (tags.editable()) {
    const existingTypes = chapter.tags.map(t => t[0]);
    rows.push([
      h(
        'select.button.button-metal',
        {
          hook: {
            insert: vnode => {
              const el = vnode.elm as HTMLInputElement;
              tags.selectedType(el.value);
              el.addEventListener('change', _ => {
                tags.selectedType(el.value);
                $(el)
                  .parents('tr')
                  .find('input')
                  .each(function (this: HTMLInputElement) {
                    this.focus();
                  });
              });
            },
            postpatch: (_, vnode) => tags.selectedType((vnode.elm as HTMLInputElement).value),
          },
        },
        [
          h('option', trans.noarg('newTag')),
          ...tags.types.map(t => (!existingTypes.includes(t) ? option(t, '', t) : undefined)),
        ],
      ),
      editable('', (value, el) => {
        const tpe = tags.selectedType();
        if (tpe) {
          tags.submit(tpe)(value);
          el.value = '';
        }
      }),
    ]);
  }

  return h(
    'table.study__tags.slist',
    h(
      'tbody',
      rows.map(r => h('tr', { key: r[0].toString() }, [h('th', r[0]), h('td', r[1])])),
    ),
  );
}
