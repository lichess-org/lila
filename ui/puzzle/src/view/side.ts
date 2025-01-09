import { MaybeVNode, dataIcon, onInsert } from 'common/snabbdom';
import { engineNameFromCode } from 'shogi/engine-name';
import { VNode, h } from 'snabbdom';
import {
  Controller,
  Puzzle,
  PuzzleDifficulty,
  PuzzleGame,
  PuzzlePlayer,
  ThemeKey,
} from '../interfaces';
import { i18n, i18nVdom, i18nVdomPlural } from 'i18n';
import { i18nThemes } from './theme';
import { numberFormat } from 'common/number';

export function puzzleBox(ctrl: Controller): VNode {
  const data = ctrl.getData();
  return h('div.puzzle__side__metas', [
    puzzleInfos(ctrl, data.puzzle),
    data.game.id ? gameInfos(ctrl, data.game, data.puzzle) : sourceInfos(ctrl, data.game),
  ]);
}

function puzzleInfos(ctrl: Controller, puzzle: Puzzle): VNode {
  return h(
    'div.infos.puzzle',
    {
      attrs: dataIcon('-'),
    },
    [
      h('div', [
        h('p', [
          ...i18nVdom(
            'puzzle:puzzleId',
            h(
              'a',
              {
                attrs: { href: `/training/${puzzle.id}` },
              },
              '#' + puzzle.id,
            ),
          ),
        ]),
        h(
          'p',
          i18nVdom(
            'puzzle:ratingX',
            ctrl.vm.mode === 'play'
              ? h('span.hidden', i18n('puzzle:hidden'))
              : h('strong', puzzle.rating),
          ),
        ),
        h(
          'p',
          i18nVdomPlural('playedXTimes', puzzle.plays, h('strong', numberFormat(puzzle.plays))),
        ),
      ]),
    ],
  );
}

function sourceInfos(ctrl: Controller, game: PuzzleGame): VNode {
  const authorName =
    ctrl.vm.mode === 'play'
      ? h('span.hidden', i18n('puzzle:hidden'))
      : game.author && game.author.startsWith('http')
        ? h(
            'a',
            {
              attrs: {
                href: `${game.author}`,
                target: '_blank',
              },
            },
            game.author.replace(/(^\w+:|^)\/\//, ''),
          )
        : game.author
          ? h('span', game.author)
          : h('span', 'Unknown author');
  return h(
    'div.infos',
    {
      attrs: dataIcon('f'),
    },
    [
      h('div', [
        h('p', i18nVdom('puzzle:puzzleSource', authorName)),
        h('div.source-description', game.description ?? ''),
      ]),
    ],
  );
}

function gameInfos(ctrl: Controller, game: PuzzleGame, puzzle: Puzzle): VNode {
  const perfName = game.perf?.name || game.id,
    gameName = game.clock ? `${game.clock} - ${perfName}` : `${perfName}`;
  return h(
    'div.infos',
    {
      attrs: game.perf ? dataIcon(game.perf.icon) : undefined,
    },
    [
      h('div', [
        h(
          'p',
          i18nVdom(
            'puzzle:fromGameLink',
            ctrl.vm.mode == 'play'
              ? h('span', gameName)
              : h(
                  'a',
                  {
                    attrs: {
                      href: `/${game.id}/${ctrl.vm.pov}#${puzzle.initialPly}`,
                    },
                  },
                  gameName,
                ),
          ),
        ),
        h(
          'div.players',
          game.players?.map(p =>
            h(
              'div.player.color-icon.is.text.' + p.color,
              p.ai
                ? engineNameFromCode(p.aiCode, p.ai)
                : p.userId === 'anon'
                  ? 'Anonymous'
                  : p.userId
                    ? h(
                        'a.user-link.ulpt',
                        {
                          attrs: { href: '/@/' + p.userId },
                        },
                        playerName(p),
                      )
                    : p.name,
            ),
          ),
        ),
      ]),
    ],
  );
}

function playerName(p: PuzzlePlayer) {
  return p.title && p.title != 'BOT' ? [h('span.title', p.title), ' ' + p.name] : p.name;
}

export function userBox(ctrl: Controller): VNode {
  const data = ctrl.getData();
  if (!data.user)
    return h('div.puzzle__side__user', [
      h('p', i18n('puzzle:toGetPersonalizedPuzzles')),
      h('button.button', { attrs: { href: '/signup' } }, i18n('signUp')),
    ]);
  const diff = ctrl.vm.round?.ratingDiff;
  return h('div.puzzle__side__user', [
    h(
      'div.puzzle__side__user__rating',
      i18nVdom(
        'puzzle:yourPuzzleRatingX',
        h('strong', [
          data.user.rating - (diff || 0),
          ...(diff && diff > 0 ? [' ', h('good.rp', '+' + diff)] : []),
          ...(diff && diff < 0 ? [' ', h('bad.rp', '−' + -diff)] : []),
        ]),
      ),
    ),
  ]);
}

const difficulties: PuzzleDifficulty[] = ['easiest', 'easier', 'normal', 'harder', 'hardest'];

export function replay(ctrl: Controller): MaybeVNode {
  const replay = ctrl.getData().replay;
  if (!replay) return;
  const i = replay.i + (ctrl.vm.mode == 'play' ? 0 : 1);
  return h('div.puzzle__side__replay', [
    h(
      'a',
      {
        attrs: {
          href: `/training/dashboard/${replay.days}`,
        },
      },
      ['« ', `Replaying ${i18nThemes[ctrl.getData().theme.key as ThemeKey]} puzzles`],
    ),
    h('div.puzzle__side__replay__bar', {
      attrs: {
        style: `--p:${replay.of ? Math.round((100 * i) / replay.of) : 1}%`,
        'data-text': `${i} / ${replay.of}`,
      },
    }),
  ]);
}

export function config(ctrl: Controller): MaybeVNode {
  const id = 'puzzle-toggle-autonext';
  return h('div.puzzle__side__config', [
    h('div.puzzle__side__config__jump', [
      h('div.switch', [
        h(`input#${id}.cmn-toggle.cmn-toggle--subtle`, {
          attrs: {
            type: 'checkbox',
            checked: ctrl.autoNext(),
          },
          hook: {
            insert: vnode =>
              (vnode.elm as HTMLElement).addEventListener('change', () =>
                ctrl.autoNext(!ctrl.autoNext()),
              ),
          },
        }),
        h('label', { attrs: { for: id } }),
      ]),
      h('label', { attrs: { for: id } }, i18n('puzzle:jumpToNextPuzzleImmediately')),
    ]),
    !ctrl.getData().replay && ctrl.difficulty
      ? h(
          'form.puzzle__side__config__difficulty',
          {
            attrs: {
              action: `/training/difficulty/${ctrl.getData().theme.key}`,
              method: 'post',
            },
          },
          [
            h(
              'label',
              {
                attrs: { for: 'puzzle-difficulty' },
              },
              i18n('puzzle:difficultyLevel'),
            ),
            h(
              'select#puzzle-difficulty.puzzle__difficulty__selector',
              {
                attrs: { name: 'difficulty' },
                hook: onInsert(elm =>
                  elm.addEventListener('change', () =>
                    (elm.parentNode as HTMLFormElement).submit(),
                  ),
                ),
              },
              difficulties.map(diff =>
                h(
                  'option',
                  {
                    attrs: {
                      value: diff,
                      selected: diff == ctrl.difficulty,
                    },
                  },
                  i18nDifficulty[diff],
                ),
              ),
            ),
          ],
        )
      : null,
  ]);
}

const i18nDifficulty: Record<string, string> = {
  easiest: i18n('puzzle:easiest'),
  easier: i18n('puzzle:easier'),
  normal: i18n('puzzle:normal'),
  harder: i18n('puzzle:harder'),
  hardest: i18n('puzzle:hardest'),
};
