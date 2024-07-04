import { Puzzle, PuzzleGame, PuzzleDifficulty } from '../interfaces';
import * as licon from 'common/licon';
import { dataIcon, onInsert, MaybeVNode, looseH as h } from 'common/snabbdom';
import { VNode } from 'snabbdom';
import { numberFormat } from 'common/number';
import perfIcons from 'common/perfIcons';
import * as router from 'common/router';
import { userLink } from 'common/userLink';
import PuzzleStreak from '../streak';
import PuzzleCtrl from '../ctrl';

export function puzzleBox(ctrl: PuzzleCtrl): VNode {
  const data = ctrl.data;
  return h('div.puzzle__side__metas', [
    puzzleInfos(ctrl, data.puzzle),
    gameInfos(ctrl, data.game, data.puzzle),
  ]);
}

const angleImg = (ctrl: PuzzleCtrl): string => {
  const angle = ctrl.data.angle;
  const name =
    angle.opening || angle.openingAbstract ? 'opening' : angle.key.startsWith('mateIn') ? 'mate' : angle.key;
  return site.asset.url(`images/puzzle-themes/${name}.svg`);
};

const puzzleInfos = (ctrl: PuzzleCtrl, puzzle: Puzzle): VNode =>
  h('div.infos.puzzle', [
    h('img.infos__angle-img', { attrs: { src: angleImg(ctrl), alt: ctrl.data.angle.name } }),
    h('div', [
      h(
        'p',
        ctrl.trans.vdom(
          'puzzleId',
          ctrl.streak && ctrl.mode === 'play'
            ? h('span.hidden', ctrl.trans.noarg('hidden'))
            : h(
                'a',
                {
                  attrs: {
                    href: router.withLang(`/training/${puzzle.id}`),
                    ...(ctrl.streak ? { target: '_blank', rel: 'noopener' } : {}),
                  },
                },
                '#' + puzzle.id,
              ),
        ),
      ),
      ctrl.opts.showRatings &&
        h(
          'p',
          ctrl.trans.vdom(
            'ratingX',
            !ctrl.streak && ctrl.mode === 'play'
              ? h('span.hidden', ctrl.trans.noarg('hidden'))
              : h('strong', `${puzzle.rating}`),
          ),
        ),
      h('p', ctrl.trans.vdomPlural('playedXTimes', puzzle.plays, h('strong', numberFormat(puzzle.plays)))),
    ]),
  ]);

function gameInfos(ctrl: PuzzleCtrl, game: PuzzleGame, puzzle: Puzzle): VNode {
  const gameName = `${game.clock} • ${game.perf.name}`;
  return h('div.infos', { attrs: dataIcon(perfIcons[game.perf.key]) }, [
    h('div', [
      h(
        'p',
        ctrl.trans.vdom(
          'fromGameLink',
          ctrl.mode == 'play'
            ? h('span', gameName)
            : h('a', { attrs: { href: `/${game.id}/${ctrl.pov}#${puzzle.initialPly}` } }, gameName),
        ),
      ),
      h(
        'div.players',
        game.players.map(p => {
          const user = { ...p, rating: ctrl.opts.showRatings ? p.rating : undefined, line: false };
          return h('div.player.color-icon.is.text.' + p.color, userLink(user));
        }),
      ),
    ]),
  ]);
}

const renderStreak = (streak: PuzzleStreak, noarg: TransNoArg) =>
  h(
    'div.puzzle__side__streak',
    streak.data.index == 0
      ? h('div.puzzle__side__streak__info', [
          h('h1.text', { attrs: dataIcon(licon.ArrowThruApple) }, 'Puzzle Streak'),
          h('p', noarg('streakDescription')),
        ])
      : h(
          'div.puzzle__side__streak__score.text',
          { attrs: dataIcon(licon.ArrowThruApple) },
          `${streak.data.index}`,
        ),
  );

export const userBox = (ctrl: PuzzleCtrl): VNode => {
  const data = ctrl.data,
    noarg = ctrl.trans.noarg;
  if (!data.user)
    return h('div.puzzle__side__user', [
      h('p', noarg('toGetPersonalizedPuzzles')),
      h('a.button', { attrs: { href: router.withLang('/signup') } }, noarg('signUp')),
    ]);
  const diff = ctrl.round?.ratingDiff,
    ratedId = 'puzzle-toggle-rated';
  return h('div.puzzle__side__user', [
    !data.replay &&
      !ctrl.streak &&
      data.user &&
      h('div.puzzle__side__config__toggle', [
        h('div.switch', [
          h(`input#${ratedId}.cmn-toggle.cmn-toggle--subtle`, {
            attrs: { type: 'checkbox', checked: ctrl.rated(), disabled: ctrl.lastFeedback != 'init' },
            hook: {
              insert: vnode => (vnode.elm as HTMLElement).addEventListener('change', ctrl.toggleRated),
            },
          }),
          h('label', { attrs: { for: ratedId } }),
        ]),
        h('label', { attrs: { for: ratedId } }, noarg('rated')),
      ]),
    h(
      'div.puzzle__side__user__rating',
      ctrl.rated()
        ? ctrl.opts.showRatings &&
            h('strong', [
              data.user.rating - (diff || 0),
              ...(diff && diff > 0 ? [' ', h('good.rp', '+' + diff)] : []),
              ...(diff && diff < 0 ? [' ', h('bad.rp', '−' + -diff)] : []),
            ])
        : h('p.puzzle__side__user__rating__casual', noarg('yourPuzzleRatingWillNotChange')),
    ),
  ]);
};

export const streakBox = (ctrl: PuzzleCtrl) =>
  h('div.puzzle__side__user', renderStreak(ctrl.streak!, ctrl.trans.noarg));

const difficulties: [PuzzleDifficulty, number][] = [
  ['easiest', -600],
  ['easier', -300],
  ['normal', 0],
  ['harder', 300],
  ['hardest', 600],
];
const colors = [
  ['black', 'asBlack'],
  ['random', 'randomColor'],
  ['white', 'asWhite'],
];

export function replay(ctrl: PuzzleCtrl): MaybeVNode {
  const replay = ctrl.data.replay;
  if (!replay) return;
  const i = replay.i + (ctrl.mode == 'play' ? 0 : 1);
  return h('div.puzzle__side__replay', [
    h('a', { attrs: { href: `/training/dashboard/${replay.days}` } }, [
      '« ',
      `Replaying ${ctrl.trans.noarg(ctrl.data.angle.key)} puzzles`,
    ]),
    h('div.puzzle__side__replay__bar', {
      attrs: {
        style: `---p:${replay.of ? Math.round((100 * i) / replay.of) : 1}%`,
        'data-text': `${i} / ${replay.of}`,
      },
    }),
  ]);
}

export function config(ctrl: PuzzleCtrl): MaybeVNode {
  const autoNextId = 'puzzle-toggle-autonext',
    noarg = ctrl.trans.noarg,
    data = ctrl.data;
  return h('div.puzzle__side__config', [
    h('div.puzzle__side__config__toggle', [
      h('div.switch', [
        h(`input#${autoNextId}.cmn-toggle.cmn-toggle--subtle`, {
          attrs: { type: 'checkbox', checked: ctrl.autoNext() },
          hook: {
            insert: vnode =>
              (vnode.elm as HTMLElement).addEventListener('change', () => {
                ctrl.autoNext(!ctrl.autoNext());
                if (ctrl.autoNext() && ctrl.resultSent && !ctrl.streak) {
                  ctrl.nextPuzzle();
                }
              }),
          },
        }),
        h('label', { attrs: { for: autoNextId } }),
      ]),
      h('label', { attrs: { for: autoNextId } }, noarg('jumpToNextPuzzleImmediately')),
    ]),
    !data.user || data.replay || ctrl.streak ? null : renderDifficultyForm(ctrl),
  ]);
}

export const renderDifficultyForm = (ctrl: PuzzleCtrl): VNode =>
  h(
    'form.puzzle__side__config__difficulty',
    { attrs: { action: `/training/difficulty/${ctrl.data.angle.key}`, method: 'post' } },
    [
      h('label', { attrs: { for: 'puzzle-difficulty' } }, ctrl.trans.noarg('difficultyLevel')),
      h(
        'select#puzzle-difficulty.puzzle__difficulty__selector',
        {
          attrs: { name: 'difficulty' },
          hook: onInsert(elm =>
            elm.addEventListener('change', () => (elm.parentNode as HTMLFormElement).submit()),
          ),
        },
        difficulties.map(([key, delta]) =>
          h(
            'option',
            {
              attrs: {
                value: key,
                selected: key == ctrl.opts.settings.difficulty,
                title:
                  !!delta &&
                  ctrl.trans.pluralSame(
                    delta < 0 ? 'nbPointsBelowYourPuzzleRating' : 'nbPointsAboveYourPuzzleRating',
                    Math.abs(delta),
                  ),
              },
            },
            [ctrl.trans.noarg(key), delta ? ` (${delta > 0 ? '+' : ''}${delta})` : ''],
          ),
        ),
      ),
    ],
  );

export const renderColorForm = (ctrl: PuzzleCtrl): VNode =>
  h(
    'div.puzzle__side__config__color',
    h(
      'group.radio',
      colors.map(([key, i18n]) =>
        h('div', [
          h(
            `a.label.color-${key}${key === (ctrl.opts.settings.color || 'random') ? '.active' : ''}`,
            {
              attrs: { href: `/training/${ctrl.data.angle.key}/${key}`, title: ctrl.trans.noarg(i18n) },
            },
            h('i'),
          ),
        ]),
      ),
    ),
  );
