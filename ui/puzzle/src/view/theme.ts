import { licon } from 'lib/licon';
import { type VNode, type MaybeVNode, bind, hl, type VNodeData, iconTag } from 'lib/view';

import type PuzzleCtrl from '@/ctrl';
import type { ThemeKey, RoundThemes } from '@/interfaces';

import { renderColorForm } from './side';

const STUDY_URL = 'https://lichess.org/study/viiWlKjv';

export default function theme(ctrl: PuzzleCtrl): MaybeVNode {
  const { angle, replay } = ctrl.data;
  const showEditor = ctrl.mode === 'view' && !ctrl.autoNexting();

  if (replay) return showEditor ? hl('div.puzzle__side__theme', editor(ctrl)) : null;
  if (ctrl.streak) return null;

  const backHref = ctrl.routerWithLang(`/training/${angle.opening ? 'openings' : 'themes'}`);

  if (ctrl.isDaily) {
    return hl(
      'div.puzzle__side__theme.puzzle__side__theme--daily',
      backToTheme(backHref, ['« ', i18n.puzzle.dailyPuzzle]),
    );
  }

  return hl('div.puzzle__side__theme', [
    backToTheme(backHref, ['« ', angle.name], { class: { long: angle.name.length > 20 } }),
    angle.opening
      ? hl('a', { attrs: { href: `/opening/${angle.opening.key}` } }, [
          'Learn more about ',
          angle.opening.name,
        ])
      : hl('p', [
          angle.desc,
          angle.chapter &&
            hl(
              'a.puzzle__side__theme__chapter.text',
              { attrs: { href: `${STUDY_URL}/${angle.chapter}`, target: '_blank' } },
              [' ', i18n.puzzle.example],
            ),
        ]),
    showEditor
      ? hl('div.puzzle__themes', editor(ctrl))
      : !replay && !ctrl.streak && (angle.opening || angle.openingAbstract) && renderColorForm(ctrl),
  ]);
}

const invisibleThemes = new Set(['master', 'masterVsMaster', 'superGM']);

function backToTheme(href: string, content: string[], data: VNodeData = {}): VNode {
  return hl('a.puzzle__side__theme__back', { attrs: { href }, ...data }, content);
}

function themeTrans(key: string) {
  return key in i18n.puzzleTheme ? i18n.puzzleTheme[key as keyof typeof i18n.puzzleTheme].toString() : key;
}

const editor = (ctrl: PuzzleCtrl): VNode[] => {
  const { puzzle } = ctrl.data;
  const votedThemes = ctrl.round?.themes ?? ({} as RoundThemes);

  const visibleThemes: ThemeKey[] = [
    ...puzzle.themes.filter(t => !invisibleThemes.has(t)),
    ...Object.keys(votedThemes).filter(
      (t: ThemeKey): t is ThemeKey => !!votedThemes[t] && !puzzle.themes.includes(t),
    ),
  ].sort();
  const allThemes = ctrl.isDaily ? null : ctrl.allThemes;
  const availableThemes = allThemes ? allThemes.dynamic.filter((t: ThemeKey) => !votedThemes[t]) : null;

  if (availableThemes) availableThemes.sort((a, b) => (themeTrans(a) < themeTrans(b) ? -1 : 1));

  return [
    hl(
      'div.puzzle__themes_list',
      {
        hook: bind('click', e => {
          const target = e.target as HTMLElement;
          const theme = target.getAttribute('data-theme') as ThemeKey;
          if (theme) ctrl.voteTheme(theme, target.classList.contains('vote-up'));
        }),
      },
      visibleThemes.map(key =>
        hl('div.puzzle__themes__list__entry', { class: { strike: votedThemes[key] === false } }, [
          hl(
            'a',
            { attrs: { href: `/training/${key}`, title: themeTrans(`${key}Description`) } },
            themeTrans(key),
          ),
          allThemes &&
            hl(
              'div.puzzle__themes__votes',
              allThemes.static.has(key)
                ? [hl('div.puzzle__themes__lock', iconTag(licon.Padlock))]
                : [
                    hl('button.puzzle__themes__vote.vote-up', {
                      class: { active: !!votedThemes[key] },
                      attrs: { 'data-theme': key },
                    }),
                    hl('button.puzzle__themes__vote.vote-down', {
                      class: { active: votedThemes[key] === false },
                      attrs: { 'data-theme': key },
                    }),
                  ],
            ),
        ]),
      ),
    ),
    ...(availableThemes
      ? [
          hl(
            `select.puzzle__themes__selector.cache-bust-${availableThemes.length}`,
            {
              hook: {
                ...bind('change', e => {
                  const theme = (e.target as HTMLInputElement).value as ThemeKey;
                  if (theme) ctrl.voteTheme(theme, true);
                }),
                postpatch(_, vnode) {
                  (vnode.elm as HTMLSelectElement).value = '';
                },
              },
            },
            [
              hl('option', { attrs: { value: '', selected: true } }, i18n.puzzle.addAnotherTheme),
              availableThemes.map(theme =>
                hl(
                  'option',
                  { attrs: { value: theme, title: themeTrans(`${theme}Description`) } },
                  themeTrans(theme),
                ),
              ),
            ],
          ),
          hl(
            'a.puzzle__themes__study.text',
            { attrs: { 'data-icon': licon.InfoCircle, href: STUDY_URL, target: '_blank' } },
            'About puzzle themes',
          ),
        ]
      : []),
  ];
};
