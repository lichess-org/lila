import { onInsert } from 'common/snabbdom';
import throttle from 'common/throttle';
import { i18n, i18nFormatCapitalized } from 'i18n';
import { colorName } from 'shogi/color-name';
import { isHandicap } from 'shogiops/handicaps';
import { type VNode, h, thunk } from 'snabbdom';
import type AnalyseCtrl from '../ctrl';
import { tagToKif } from '../notation-export';
import { option } from '../util';
import type { StudyChapter, StudyCtrl, StudyTagsCtrl, TagTypes } from './interfaces';

const unwantedTags = ['Result', 'SenteElo', 'SenteTitle', 'GoteElo', 'GoteTitle'];

function editable(value: string, submit: (v: string, el: HTMLInputElement) => void): VNode {
  return h('input', {
    key: value, // force to redraw on change, to visibly update the input value
    attrs: {
      spellcheck: false,
      value,
    },
    hook: onInsert<HTMLInputElement>(el => {
      el.onblur = () => {
        submit(el.value, el);
      };
      el.onkeypress = e => {
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

function renderTags(chapter: StudyChapter, submit, types: string[]): VNode {
  let rows: TagRow[] = [];
  const wantedTags = chapter.tags.filter(t => !unwantedTags.includes(t[0])),
    handicap = isHandicap({ rules: chapter.setup.variant.key, sfen: chapter.initialSfen });
  rows = rows.concat(
    wantedTags.map(tag => [tag[0], submit ? editable(tag[1], submit(tag[0])) : fixed(tag[1])]),
  );
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
                $(el).parents('tr').find('input').trigger('focus');
              });
            },
            postpatch: (_, vnode) => {
              selectedType = (vnode.elm as HTMLInputElement).value;
            },
          },
        },
        [
          h('option', i18n('study:newTag')),
          ...types
            .filter(t => !unwantedTags.includes(t))
            .map(t => {
              if (!existingTypes.includes(t)) return option(t, '', translateTag(t, handicap));
              return undefined;
            }),
        ],
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
      rows.map(r => {
        const tag = typeof r[0] === 'string' ? translateTag(r[0], handicap) : r[0];

        return h(
          'tr',
          {
            key: `${r[0]}`,
          },
          [
            h(
              'th',
              {
                attrs: {
                  title: (typeof r[0] === 'string' ? tagToKif(r[0], handicap) : undefined) || '',
                },
              },
              tag,
            ),
            h('td', r[1]),
          ],
        );
      }),
    ),
  );
}

export function ctrl(
  root: AnalyseCtrl,
  getChapter: () => StudyChapter,
  types: TagTypes,
): StudyTagsCtrl {
  const submit = throttle(500, (name, value) => {
    root.study!.makeChange('setTag', {
      chapterId: getChapter().id,
      name,
      value: value.slice(0, 140),
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
    renderTags(root.tags.getChapter(), root.vm.mode.write && root.tags.submit, root.tags.types),
  );
}

function translateTag(tag: string, handicap: boolean): string {
  const transformString = str => `${str[0].toLowerCase()}${str.slice(1)}`;
  if (tag === 'Sente' || tag === 'Gote') {
    return colorName(tag.toLowerCase() as Color, handicap);
  } else if (tag.startsWith('Sente') || tag.startsWith('Gote')) {
    return i18nColorTag(tag, handicap);
  } else return tagI18n[`${transformString(tag)}Tag`] || tag;
}

export function view(root: StudyCtrl): VNode {
  const chapter = root.tags.getChapter(),
    tagKey = chapter.tags
      .filter(t => !unwantedTags.includes(t[0]))
      .map(t => t[1])
      .join(','),
    key =
      chapter.id + root.data.name + chapter.name + root.data.likes + tagKey + root.vm.mode.write;
  return thunk(`div.${chapter.id}`, doRender, [root, key]);
}

function i18nColorTag(tag: string, handicap: boolean): string {
  const xTag = `${tag.replace(/^(Sente|Gote)/, 'x')}Tag`,
    color = tag.startsWith('Sente') ? 'sente' : 'gote';
  switch (xTag) {
    case 'xEloTag':
      return i18nFormatCapitalized('study:xEloTag', colorName(color, handicap));
    case 'xTitleTag':
      return i18nFormatCapitalized('study:xTitleTag', colorName(color, handicap));
    case 'xTeamTag':
      return i18nFormatCapitalized('study:xTeamTag', colorName(color, handicap));
    default:
      return tagI18n[tag] || tag;
  }
}

const tagI18n = {
  startTag: i18n('study:startTag'),
  endTag: i18n('study:endTag'),
  siteTag: i18n('study:siteTag'),
  eventTag: i18n('study:eventTag'),
  timeControlTag: i18n('study:timeControlTag'),
  openingTag: i18n('study:openingTag'),
  resultTag: i18n('study:resultTag'),
  problemNameTag: i18n('study:problemNameTag'),
  problemIdTag: i18n('study:problemIdTag'),
  dateOfPublicationTag: i18n('study:dateOfPublicationTag'),
  composerTag: i18n('study:composerTag'),
  publicationTag: i18n('study:publicationTag'),
  collectionTag: i18n('study:collectionTag'),
  lengthTag: i18n('study:lengthTag'),
  prizeTag: i18n('study:prizeTag'),
};
