import { onInsert } from 'lib/view';
import { throttle } from 'lib/async';
import { type Attrs, h, thunk, type VNode } from 'snabbdom';
import { option } from '../view/util';
import { looksLikeLichessGame } from './studyChapters';
import { prop } from 'lib';
import type StudyCtrl from './studyCtrl';

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

  submit = (name: string, value: string) => this.editable() && this.makeChange(name, value);
}

export function view(root: StudyCtrl): VNode {
  const chapter = root.tags.getChapter(),
    tagKey = chapter.tags.map(t => t[1]).join(','),
    key = chapter.id + root.data.name + chapter.name + root.data.likes + tagKey + root.vm.mode.write;
  return thunk('div.' + chapter.id, doRender, [root, key]);
}

const doRender = (root: StudyCtrl): VNode => h('div', renderPgnTags(root.tags, root.data.showRatings));

const editable = (
  name: string,
  value: string,
  submit: (n: string, v: string, el: HTMLInputElement) => void,
): VNode =>
  h('input', {
    key: value, // force to redraw on change, to visibly update the input value
    attrs: { spellcheck: 'false', ...(inputAttrs[name] ?? {}), maxlength: 140, value },
    hook: onInsert<HTMLInputElement>(el => {
      el.onblur = () => submit(name, el.value, el);
      el.onkeydown = e => {
        if (e.key === 'Enter') el.blur();
      };
    }),
  });

// Set of titles derived from scalachess' PlayerTitle.scala.
const titles = 'GM|WGM|IM|WIM|FM|WFM|CM|WCM|NM|WNM|LM|BOT';
const acceptableTitlePattern = `${titles}|${titles.toLowerCase()}`;

const inputAttrs: { [name: string]: Attrs } = (() => {
  const elo = { pattern: '\\d{3,4}' };
  const fideId = { pattern: '\\d{2,9}' };
  const title = { pattern: acceptableTitlePattern };
  return {
    Date: {
      pattern:
        // Match 1700-2099. or ????.
        '(?:(?:(?:17|18|19|20)[0-9]{2}\\.)|\\?\\?\\?\\?\\.)' +
        '(?:' +
        // Match mm.dd 01-12.01-29
        '(?:(?:0[1-9]|1[0-2])\\.(?:0[1-9]|1[0-9]|2[0-9])|' +
        // Match all months except February with 30 days
        '(?:(?!02)(?:0[1-9]|1[0-2])\\.(?:30))|' +
        // Match months with 31 days
        '(?:(?:0[13578]|1[02])\\.31))|' +
        // Unknown month ??.01-31
        '(?:\\?\\?\\.(?:0[1-9]|1[0-9]|2[0-9]|30|31))|' +
        // Unknown day 01-12.??
        '(?:(?:0[1-9]|1[0-2])\\.\\?\\?)|' +
        // Unknown month and day ??.??
        '(?:\\?\\?\\.\\?\\?)' +
        ')',
      // PGN specification allows for substition of any numeric value with '?'
      title: 'yyyy.mm.dd or ????.??.??',
    },
    WhiteElo: elo,
    BlackElo: elo,
    WhiteFideId: fideId,
    BlackFideId: fideId,
    WhiteTitle: title,
    BlackTitle: title,
  };
})();

type TagRow = (string | VNode)[];

const fixed = ([key, value]: [string, string]) =>
  key.endsWith('FideId') ? h('a', { attrs: { href: `/fide/${value}/redirect` } }, value) : fixedValue(value);

const fixedValue = (value: string) => h('span', value);

function renderPgnTags(tags: TagsForm, showRatings: boolean): VNode {
  let rows: TagRow[] = [];
  const chapter = tags.getChapter();
  if (chapter.setup.variant.key !== 'standard')
    rows.push(['Variant', fixedValue(chapter.setup.variant.name)]);
  rows = rows.concat(
    chapter.tags
      .filter(
        tag =>
          tag[0] !== 'Variant' &&
          (showRatings || !['WhiteElo', 'BlackElo'].includes(tag[0]) || !looksLikeLichessGame(chapter.tags)),
      )
      .map(tag => [tag[0], tags.editable() ? editable(tag[0], tag[1], tags.submit) : fixed(tag)]),
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
          h('option', i18n.study.newTag),
          ...tags.types.map(t => (!existingTypes.includes(t) ? option(t, '', t) : undefined)),
        ],
      ),
      editable('', '', (_, value, el) => {
        const tpe = tags.selectedType();
        if (tpe) {
          tags.submit(tpe, value);
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
