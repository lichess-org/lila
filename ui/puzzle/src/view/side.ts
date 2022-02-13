import { Controller, Puzzle, PuzzleGame, MaybeVNode, PuzzleDifficulty } from '../interfaces';
import { dataIcon, onInsert, bind } from 'common/snabbdom';
import { h, VNode } from 'snabbdom';
import { numberFormat } from 'common/number';
import PuzzleStreak from '../streak';

export function puzzleBox(ctrl: Controller): VNode {
  const data = ctrl.getData();
  return h('div.puzzle__side__metas', [puzzleInfos(ctrl, data.puzzle), gameInfos(ctrl, data.game, data.puzzle)]);
}

function puzzleInfos(ctrl: Controller, puzzle: Puzzle): VNode {
  return h(
    'div.infos.puzzle',
    {
      attrs: dataIcon(''),
    },
    [
      h('div', [
        ctrl.streak
          ? null
          : h(
              'p',
              ctrl.trans.vdom(
                'puzzleId',
                h(
                  'a',
                  {
                    attrs: {
                      href: `/training/${puzzle.id}`,
                      ...(ctrl.streak ? { target: '_blank', rel: 'noopener' } : {}),
                    },
                  },
                  '#' + puzzle.id
                )
              )
            ),
        ctrl.showRatings
          ? h(
              'p',
              ctrl.trans.vdom(
                'ratingX',
                !ctrl.streak && ctrl.vm.mode === 'play'
                  ? h('span.hidden', ctrl.trans.noarg('hidden'))
                  : h('strong', puzzle.rating)
              )
            )
          : null,
        h('p', ctrl.trans.vdom('playedXTimes', h('strong', numberFormat(puzzle.plays)))),
      ]),
    ]
  );
}

function gameInfos(ctrl: Controller, game: PuzzleGame, puzzle: Puzzle): VNode {
  const gameName = `${game.clock} • ${game.perf.name}`;
  return h('div.infos', { attrs: dataIcon(game.perf.icon) }, [
    h('div', [
      h(
        'p',
        ctrl.trans.vdom(
          'fromGameLink',
          ctrl.vm.mode == 'play'
            ? h('span', gameName)
            : h(
                'a',
                {
                  attrs: { href: `/${game.id}/${ctrl.vm.pov}#${puzzle.initialPly}` },
                },
                gameName
              )
        )
      ),
      h(
        'div.players',
        game.players.map(p => {
          const name = ctrl.showRatings ? p.name : p.name.split(' ')[0];
          return h(
            'div.player.color-icon.is.text.' + p.color,
            p.userId != 'anon'
              ? h(
                  'a.user-link.ulpt',
                  {
                    attrs: { href: '/@/' + p.userId },
                  },
                  p.title && p.title != 'BOT' ? [h('span.utitle', p.title), ' ' + name] : name
                )
              : name
          );
        })
      ),
    ]),
  ]);
}

const renderStreak = (streak: PuzzleStreak, noarg: TransNoArg) =>
  h(
    'div.puzzle__side__streak',
    streak.data.index == 0
      ? h('div.puzzle__side__streak__info', [
          h(
            'h1.text',
            {
              attrs: dataIcon(''),
            },
            'Puzzle Streak'
          ),
          h('p', noarg('streakDescription')),
        ])
      : h(
          'div.puzzle__side__streak__score.text',
          {
            attrs: dataIcon(''),
          },
          streak.data.index
        )
  );

export const userBox = (ctrl: Controller): VNode => {
  const data = ctrl.getData(),
    noarg = ctrl.trans.noarg;
  if (!data.user)
    return h('div.puzzle__side__user', [
      h('p', noarg('toGetPersonalizedPuzzles')),
      h('a.button', { attrs: { href: '/signup' } }, noarg('signUp')),
    ]);
  const diff = ctrl.vm.round?.ratingDiff,
    ratedId = 'puzzle-toggle-rated';
  return h('div.puzzle__side__user', [
    !data.replay && !ctrl.streak && data.user
      ? h('div.puzzle__side__config__toggle', [
          h('div.switch', [
            h(`input#${ratedId}.cmn-toggle.cmn-toggle--subtle`, {
              attrs: {
                type: 'checkbox',
                checked: ctrl.rated(),
                disabled: ctrl.vm.lastFeedback != 'init',
              },
              hook: {
                insert: vnode => (vnode.elm as HTMLElement).addEventListener('change', ctrl.toggleRated),
              },
            }),
            h('label', { attrs: { for: ratedId } }),
          ]),
          h('label', { attrs: { for: ratedId } }, noarg('rated')),
        ])
      : undefined,
    h(
      'div.puzzle__side__user__rating',
      ctrl.rated()
        ? ctrl.showRatings
          ? h('strong', [
              data.user.rating - (diff || 0),
              ...(diff && diff > 0 ? [' ', h('good.rp', '+' + diff)] : []),
              ...(diff && diff < 0 ? [' ', h('bad.rp', '−' + -diff)] : []),
            ])
          : null
        : h('p.puzzle__side__user__rating__casual', noarg('yourPuzzleRatingWillNotChange'))
    ),
  ]);
};

export const streakBox = (ctrl: Controller) =>
  h('div.puzzle__side__user', renderStreak(ctrl.streak!, ctrl.trans.noarg));

const difficulties: [PuzzleDifficulty, number][] = [
  ['easiest', -600],
  ['easier', -300],
  ['normal', 0],
  ['harder', 300],
  ['hardest', 600],
];

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
      ['« ', `Replaying ${ctrl.trans.noarg(ctrl.getData().theme.key)} puzzles`]
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
  const autoNextId = 'puzzle-toggle-autonext',
    noarg = ctrl.trans.noarg,
    data = ctrl.getData();
  return h('div.puzzle__side__config', [
    h('div.puzzle__side__config__toggle', [
      h('div.switch', [
        h(`input#${autoNextId}.cmn-toggle.cmn-toggle--subtle`, {
          attrs: {
            type: 'checkbox',
            checked: ctrl.autoNext(),
          },
          hook: {
            insert: vnode =>
              (vnode.elm as HTMLElement).addEventListener('change', () => {
                ctrl.autoNext(!ctrl.autoNext());
                if (ctrl.autoNext() && ctrl.vm.resultSent && !ctrl.streak) {
                  ctrl.nextPuzzle();
                }
              }),
          },
        }),
        h('label', { attrs: { for: autoNextId } }),
      ]),
      h('label', { attrs: { for: autoNextId } }, noarg('jumpToNextPuzzleImmediately')),
    ]),
    !data.replay && !ctrl.streak && ctrl.difficulty ? renderDifficultyForm(ctrl) : null,
    h('div.puzzle__side__config__toggles', [
      h(
        'a.puzzle__side__config__zen.button.button-empty',
        {
          hook: bind('click', () => lichess.pubsub.emit('zen')),
          attrs: {
            title: 'Keyboard: z',
          },
        },
        noarg('zenMode')
      ),
      h(
        'a.puzzle__side__config__flip.button',
        {
          class: { active: ctrl.flipped(), 'button-empty': !ctrl.flipped() },
          hook: bind('click', ctrl.flip),
          attrs: {
            title: 'Keyboard: f',
          },
        },
        noarg('flipBoard')
      ),
    ]),
  ]);
}

export function renderDifficultyForm(ctrl: Controller): VNode {
  return h(
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
        ctrl.trans.noarg('difficultyLevel')
      ),
      h(
        'select#puzzle-difficulty.puzzle__difficulty__selector',
        {
          attrs: { name: 'difficulty' },
          hook: onInsert(elm => elm.addEventListener('change', () => (elm.parentNode as HTMLFormElement).submit())),
        },
        difficulties.map(([key, delta]) =>
          h(
            'option',
            {
              attrs: {
                value: key,
                selected: key == ctrl.difficulty,
                title:
                  !!delta &&
                  ctrl.trans.plural(
                    delta < 0 ? 'nbPointsBelowYourPuzzleRating' : 'nbPointsAboveYourPuzzleRating',
                    Math.abs(delta)
                  ),
              },
            },
            [ctrl.trans.noarg(key), delta ? ` (${delta > 0 ? '+' : ''}${delta})` : '']
          )
        )
      ),
    ]
  );
}
