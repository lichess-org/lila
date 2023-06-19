import { colorName, transWithColorName } from 'common/colorName';
import { onInsert } from 'common/snabbdom';
import throttle from 'common/throttle';
import { isHandicap } from 'shogiops/handicaps';
import { VNode, h, thunk } from 'snabbdom';
import AnalyseCtrl from '../ctrl';
import { option } from '../util';
import { StudyChapter, StudyCtrl } from './interfaces';
import { tagToKif } from '../notationExport';

const unwantedTags = ['Result', 'SenteElo', 'SenteTitle', 'GoteElo', 'GoteTitle'];

function editable(value: string, submit: (v: string, el: HTMLInputElement) => void): VNode {
  return h('input', {
    key: value, // force to redraw on change, to visibly update the input value
    attrs: {
      spellcheck: false,
      value,
    },
    hook: onInsert<HTMLInputElement>(el => {
      el.onblur = function () {
        submit(el.value, el);
      };
      el.onkeypress = function (e) {
        if ((e.keyCode || e.which) == 13) el.blur();
      };
    }),
  });
}

function fixed(text: string) {
  return h('span', text);
}

let selectedType: string;

type TagRow = (string | VNode)[];

function renderTags(chapter: StudyChapter, submit, types: string[], trans: Trans): VNode {
  let rows: TagRow[] = [];
  const wantedTags = chapter.tags.filter(t => !unwantedTags.includes(t[0])),
    handicap = isHandicap({ rules: chapter.setup.variant.key, sfen: chapter.initialSfen });
  rows = rows.concat(wantedTags.map(tag => [tag[0], submit ? editable(tag[1], submit(tag[0])) : fixed(tag[1])]));
  if (submit) {
    const existingTypes = wantedTags.map(t => t[0]);
    rows.push([
      h(
        'select',
        {
          hook: {
            insert: vnode => {
              const el = vnode.elm as HTMLInputElement;
              selectedType = el.value;
              el.addEventListener('change', _ => {
                selectedType = el.value;
                $(el).parents('tr').find('input').focus();
              });
            },
            postpatch: (_, vnode) => {
              selectedType = (vnode.elm as HTMLInputElement).value;
            },
          },
        },
        [
          h('option', trans.noarg('newTag')),
          ...types
            .filter(t => !unwantedTags.includes(t))
            .map(t => {
              if (!existingTypes.includes(t)) return option(t, '', translateTag(trans, t, handicap));
              return undefined;
            }),
        ]
      ),
      editable('', (value, el) => {
        if (selectedType) {
          submit(selectedType)(value);
          el.value = '';
        }
      }),
    ]);
  }

  return h(
    'table.study__tags.slist',
    h(
      'tbody',
      rows.map(function (r) {
        const tag = typeof r[0] === 'string' ? translateTag(trans, r[0], handicap) : r[0];

        return h(
          'tr',
          {
            key: '' + r[0],
          },
          [
            h('th', { attrs: { title: (typeof r[0] === 'string' ? tagToKif(r[0], handicap) : undefined) || '' } }, tag),
            h('td', r[1]),
          ]
        );
      })
    )
  );
}

export function ctrl(root: AnalyseCtrl, getChapter: () => StudyChapter, types) {
  const submit = throttle(500, function (name, value) {
    root.study!.makeChange('setTag', {
      chapterId: getChapter().id,
      name,
      value: value.substr(0, 140),
    });
  });

  return {
    submit(name) {
      return value => submit(name, value);
    },
    getChapter,
    types,
  };
}
function doRender(root: StudyCtrl): VNode {
  return h(
    'div',
    renderTags(root.tags.getChapter(), root.vm.mode.write && root.tags.submit, root.tags.types, root.trans)
  );
}

function translateTag(trans: Trans, tag: string, handicap: boolean): string {
  const transformString = str => `${str[0].toLowerCase()}${str.slice(1)}`;
  if (tag === 'Sente' || tag === 'Gote') {
    return colorName(trans.noarg, tag.toLowerCase() as Color, handicap);
  } else if (tag.startsWith('Sente') || tag.startsWith('Gote')) {
    return transWithColorName(
      trans,
      (tag.replace(/^(Sente|Gote)/, 'x') + 'Tag') as I18nKey,
      tag.startsWith('Sente') ? 'sente' : 'gote',
      handicap
    );
  } else return trans.noarg((transformString(tag) + 'Tag') as I18nKey);
}

export function view(root: StudyCtrl): VNode {
  const chapter = root.tags.getChapter(),
    tagKey = chapter.tags
      .filter(t => !unwantedTags.includes(t[0]))
      .map(t => t[1])
      .join(','),
    key = chapter.id + root.data.name + chapter.name + root.data.likes + tagKey + root.vm.mode.write;
  return thunk('div.' + chapter.id, doRender, [root, key]);
}
