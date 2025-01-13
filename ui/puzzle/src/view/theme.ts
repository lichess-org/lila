import { type MaybeVNode, bind, dataIcon } from 'common/snabbdom';
import { i18n } from 'i18n';
import { type VNode, h } from 'snabbdom';
import type { Controller, RoundThemes, ThemeKey } from '../interfaces';

// const studyUrl = 'https://lishogi.org/study/viiWlKjv'; // change

export default function theme(ctrl: Controller): MaybeVNode {
  const t = ctrl.getData().theme;
  return ctrl.getData().replay
    ? null
    : h('div.puzzle__side__theme', [
        h('a', { attrs: { href: '/training/themes' } }, h('h2', ['« ', t.name])),
        h('h3', [
          t.desc,
          //t.chapter &&
          //  h(
          //    'a.puzzle__side__theme__chapter.text',
          //    {
          //      attrs: {
          //        href: `${studyUrl}/${t.chapter}`,
          //        target: '_blank',
          //      },
          //    },
          //    [' ', _i18n('example')]
          //  ),
        ]),
        ctrl.vm.mode != 'view' || ctrl.autoNexting() ? null : editor(ctrl),
      ]);
}

const invisibleThemes = new Set<ThemeKey>(['lishogiGames', 'otherSources']);

const editor = (ctrl: Controller): VNode => {
  const data = ctrl.getData();
  const votedThemes = ctrl.vm.round?.themes || ({} as RoundThemes);
  const visibleThemes: ThemeKey[] = data.puzzle.themes
    .filter(t => !invisibleThemes.has(t))
    .concat(
      Object.keys(votedThemes).filter(
        (t: ThemeKey) => votedThemes[t] && !data.puzzle.themes.includes(t),
      ) as ThemeKey[],
    )
    .sort();
  const allThemes = location.pathname == '/training/daily' ? null : ctrl.allThemes;
  const availableThemes = allThemes ? allThemes.dynamic.filter(t => !votedThemes[t]) : null;
  if (availableThemes) availableThemes.sort((a, b) => (i18nThemes[a] < i18nThemes[b] ? -1 : 1));
  return h('div.puzzle__themes', [
    h(
      'div.puzzle__themes_list',
      {
        hook: bind('click', e => {
          const target = e.target as HTMLElement;
          const theme = target.getAttribute('data-theme') as ThemeKey;
          if (theme) ctrl.voteTheme(theme, target.classList.contains('vote-up'));
        }),
      },
      visibleThemes.map(key =>
        h(
          'div.puzzle__themes__list__entry',
          {
            class: {
              strike: votedThemes[key] === false,
            },
          },
          [
            h(
              'a',
              {
                attrs: {
                  href: `/training/${key}`,
                  title: i18nThemes[`${key}Description`],
                },
              },
              i18nThemes[key],
            ),
            !allThemes
              ? null
              : h(
                  'div.puzzle__themes__votes',
                  allThemes.static.has(key)
                    ? [
                        h(
                          'div.puzzle__themes__lock',
                          h('i', {
                            attrs: dataIcon('a'),
                          }),
                        ),
                      ]
                    : [
                        h('span.puzzle__themes__vote.vote-up', {
                          class: { active: votedThemes[key] },
                          attrs: { 'data-theme': key },
                        }),
                        h('span.puzzle__themes__vote.vote-down', {
                          class: { active: votedThemes[key] === false },
                          attrs: { 'data-theme': key },
                        }),
                      ],
                ),
          ],
        ),
      ),
    ),
    ...(availableThemes
      ? [
          h(
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
              h(
                'option',
                {
                  attrs: { value: '', selected: true },
                },
                i18n('puzzle:addAnotherTheme'),
              ),
              ...availableThemes.map(theme =>
                h(
                  'option',
                  {
                    attrs: {
                      value: theme,
                      title: i18nThemes[`${theme}Description`],
                    },
                  },
                  i18nThemes[theme],
                ),
              ),
            ],
          ),
          //h(
          //  'a.puzzle__themes__study.text',
          //  {
          //    attrs: {
          //      'data-icon': '',
          //      href: studyUrl,
          //      target: '_blank',
          //    },
          //  },
          //  'About puzzle themes'
          //),
        ]
      : []),
  ]);
};

export const i18nThemes: Record<ThemeKey | `${ThemeKey}Description`, string> = {
  mix: i18n('puzzleTheme:healthyMix'),
  mixDescription: i18n('puzzleTheme:healthyMixDescription'),
  advantage: i18n('puzzleTheme:advantage'),
  advantageDescription: i18n('puzzleTheme:advantageDescription'),
  equality: i18n('puzzleTheme:equality'),
  equalityDescription: i18n('puzzleTheme:equalityDescription'),
  crushing: i18n('puzzleTheme:crushing'),
  crushingDescription: i18n('puzzleTheme:crushingDescription'),
  opening: i18n('puzzleTheme:opening'),
  openingDescription: i18n('puzzleTheme:openingDescription'),
  middlegame: i18n('puzzleTheme:middlegame'),
  middlegameDescription: i18n('puzzleTheme:middlegameDescription'),
  endgame: i18n('puzzleTheme:endgame'),
  endgameDescription: i18n('puzzleTheme:endgameDescription'),
  oneMove: i18n('puzzleTheme:oneMove'),
  oneMoveDescription: i18n('puzzleTheme:oneMoveDescription'),
  short: i18n('puzzleTheme:short'),
  shortDescription: i18n('puzzleTheme:shortDescription'),
  long: i18n('puzzleTheme:long'),
  longDescription: i18n('puzzleTheme:longDescription'),
  veryLong: i18n('puzzleTheme:veryLong'),
  veryLongDescription: i18n('puzzleTheme:veryLongDescription'),
  fork: i18n('puzzleTheme:fork'),
  forkDescription: i18n('puzzleTheme:forkDescription'),
  pin: i18n('puzzleTheme:pin'),
  pinDescription: i18n('puzzleTheme:pinDescription'),
  sacrifice: i18n('puzzleTheme:sacrifice'),
  sacrificeDescription: i18n('puzzleTheme:sacrificeDescription'),
  strikingPawn: i18n('puzzleTheme:strikingPawn'),
  strikingPawnDescription: i18n('puzzleTheme:strikingPawnDescription'),
  joiningPawn: i18n('puzzleTheme:joiningPawn'),
  joiningPawnDescription: i18n('puzzleTheme:joiningPawnDescription'),
  edgeAttack: i18n('puzzleTheme:edgeAttack'),
  edgeAttackDescription: i18n('puzzleTheme:edgeAttackDescription'),
  mate: i18n('puzzleTheme:mate'),
  mateDescription: i18n('puzzleTheme:mateDescription'),
  mateIn1: i18n('puzzleTheme:mateIn1'),
  mateIn1Description: i18n('puzzleTheme:mateIn1Description'),
  mateIn3: i18n('puzzleTheme:mateIn3'),
  mateIn3Description: i18n('puzzleTheme:mateIn3Description'),
  mateIn5: i18n('puzzleTheme:mateIn5'),
  mateIn5Description: i18n('puzzleTheme:mateIn5Description'),
  mateIn7: i18n('puzzleTheme:mateIn7'),
  mateIn7Description: i18n('puzzleTheme:mateIn7Description'),
  mateIn9: i18n('puzzleTheme:mateIn9'),
  mateIn9Description: i18n('puzzleTheme:mateIn9Description'),
  tsume: i18n('puzzleTheme:tsume'),
  tsumeDescription: i18n('puzzleTheme:tsumeDescription'),
  lishogiGames: i18n('puzzleTheme:lishogiGames'),
  lishogiGamesDescription: i18n('puzzleTheme:lishogiGamesDescription'),
  otherSources: i18n('puzzleTheme:otherSources'),
  otherSourcesDescription: i18n('puzzleTheme:otherSourcesDescription'),
};
