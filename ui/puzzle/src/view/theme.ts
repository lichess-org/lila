import * as licon from 'lib/licon';
import { type VNode, type MaybeVNode, bind, dataIcon, hl } from 'lib/snabbdom';
import type { ThemeKey, RoundThemes } from '../interfaces';
import { renderColorForm } from './side';
import type PuzzleCtrl from '../ctrl';

const studyUrl = 'https://lichess.org/study/viiWlKjv';

export default function theme(ctrl: PuzzleCtrl): MaybeVNode {
  const data = ctrl.data,
    angle = data.angle;
  const showEditor = ctrl.mode === 'view' && !ctrl.autoNexting();
  if (data.replay) return showEditor ? hl('div.puzzle__side__theme', editor(ctrl)) : null;
  const puzzleMenu = (v: VNode): VNode =>
    hl(
      'a',
      { attrs: { href: ctrl.routerWithLang(`/training/${angle.opening ? 'openings' : 'themes'}`) } },
      v,
    );
  return ctrl.streak
    ? null
    : ctrl.isDaily
      ? hl(
          'div.puzzle__side__theme.puzzle__side__theme--daily',
          puzzleMenu(hl('h2', i18n.puzzle.dailyPuzzle)),
        )
      : hl('div.puzzle__side__theme', [
          puzzleMenu(hl('h2', { class: { long: angle.name.length > 20 } }, ['« ', angle.name])),
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
                    { attrs: { href: `${studyUrl}/${angle.chapter}`, target: '_blank' } },
                    [' ', i18n.puzzle.example],
                  ),
              ]),
          showEditor
            ? hl('div.puzzle__themes', editor(ctrl))
            : !data.replay &&
              !ctrl.streak &&
              (angle.opening || angle.openingAbstract) &&
              renderColorForm(ctrl),
        ]);
}

const invisibleThemes = new Set(['master', 'masterVsMaster', 'superGM']);

const editor = (ctrl: PuzzleCtrl): VNode[] => {
  const data = ctrl.data,
    votedThemes = ctrl.round?.themes || ({} as RoundThemes);
  const themeTrans = (key: string) => (i18n.puzzleTheme as any)[key] || key;
  const visibleThemes: ThemeKey[] = [
    ...data.puzzle.themes.filter(t => !invisibleThemes.has(t)),
    ...Object.keys(votedThemes).filter(
      (t: ThemeKey): t is ThemeKey => votedThemes[t] && !data.puzzle.themes.includes(t),
    ),
  ].sort();
  const allThemes = location.pathname === '/training/daily' ? null : ctrl.allThemes;
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
                ? [hl('div.puzzle__themes__lock', hl('i', { attrs: dataIcon(licon.Padlock) }))]
                : [
                    hl('span.puzzle__themes__vote.vote-up', {
                      class: { active: votedThemes[key] },
                      attrs: { 'data-theme': key },
                    }),
                    hl('span.puzzle__themes__vote.vote-down', {
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
            { attrs: { 'data-icon': licon.InfoCircle, href: studyUrl, target: '_blank' } },
            'About puzzle themes',
          ),
        ]
      : []),
  ];
};
