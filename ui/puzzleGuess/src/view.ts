import { Chessground as makeChessground } from '@lichess-org/chessground';
import type { VNode } from 'snabbdom';

import { licon } from 'lib/licon';
import { pubsub } from 'lib/pubsub';
import { hl, onInsert } from 'lib/view';

import type PuzzleGuessCtrl from './ctrl';

export default function view(ctrl: PuzzleGuessCtrl): VNode {
  return hl('div.puzzle-guess.puzzle-guess-app', [
    hl('div.puzzle-guess__board.main-board', [chessground(ctrl), ctrl.promotion.view()]),
    hl('div.puzzle-guess__side', renderSide(ctrl)),
  ]);
}

const chessground = (ctrl: PuzzleGuessCtrl): VNode =>
  hl('div.cg-wrap', {
    hook: {
      insert: vnode => {
        ctrl.ground(makeChessground(vnode.elm as HTMLElement, ctrl.cgConfig()));
        pubsub.on('board.change', (is3d: boolean) =>
          ctrl.withGround(g => {
            g.state.addPieceZIndex = is3d;
            g.redrawAll();
          }),
        );
      },
    },
  });

const renderSide = (ctrl: PuzzleGuessCtrl): (VNode | false | undefined)[] => {
  if (ctrl.noMorePositions)
    return [
      hl('div.puzzle-guess__no-more', [
        hl('strong', 'No more positions available'),
        hl('p', 'Come back later for more!'),
      ]),
    ];
  return [
    renderPlayer(ctrl),
    ctrl.phase === 'guess' && renderGuess(ctrl),
    ctrl.phase === 'solve' && renderSolve(),
    ctrl.phase === 'done' && renderDone(ctrl),
  ];
};

const renderPlayer = (ctrl: PuzzleGuessCtrl): VNode | undefined =>
  ctrl.player &&
  hl('div.puzzle-guess__player', [
    hl('span.puzzle-guess__player__rating', [
      'Rating: ',
      hl('strong', `${ctrl.player.rating}${ctrl.player.provisional ? '?' : ''}`),
    ]),
    hl('span.puzzle-guess__player__score', `${ctrl.player.wins} / ${ctrl.player.runs}`),
  ]);

const renderGuess = (ctrl: PuzzleGuessCtrl): VNode =>
  hl('div.puzzle-guess__prompt', [
    hl('p.puzzle-guess__question', [
      hl('strong', ctrl.position?.color === 'white' ? 'White to move.' : 'Black to move.'),
      ' Is this a puzzle?',
    ]),
    hl('div.puzzle-guess__buttons', [
      hl(
        'button.button.puzzle-guess__yes',
        {
          attrs: { disabled: ctrl.loading },
          hook: onInsert(el => el.addEventListener('click', () => ctrl.guess(true))),
        },
        'Puzzle!',
      ),
      hl(
        'button.button.button-metal.puzzle-guess__no',
        {
          attrs: { disabled: ctrl.loading },
          hook: onInsert(el => el.addEventListener('click', () => ctrl.guess(false))),
        },
        'Just a normal position',
      ),
    ]),
  ]);

const renderSolve = (): VNode =>
  hl('div.puzzle-guess__prompt', [
    hl('p.puzzle-guess__feedback.good', [hl('strong', 'Correct, it is a puzzle!')]),
    hl('p', 'Now find the winning line.'),
  ]);

const renderDone = (ctrl: PuzzleGuessCtrl): VNode => {
  const r = ctrl.result;
  return hl('div.puzzle-guess__prompt', [
    r &&
      hl(`p.puzzle-guess__feedback.${r.win ? 'good' : 'bad'}`, [
        hl(
          'strong',
          ctrl.solveFailed
            ? 'Wrong move. This was the solution.'
            : r.correct
              ? 'Correct!'
              : r.isPuzzle
                ? 'Wrong: there was a winning tactic here.'
                : 'Wrong: just a normal position.',
        ),
      ]),
    r?.positionRating &&
      hl('p.puzzle-guess__position-rating', ['Position difficulty: ', hl('strong', `${r.positionRating}`)]),
    r?.ratingDiff &&
      hl('p.puzzle-guess__rating-diff', [
        'Your rating: ',
        hl('strong', `${r.ratingDiff.after}`),
        ratingDiffTag(r.ratingDiff.after - r.ratingDiff.before),
      ]),
    hl(
      'button.button.puzzle-guess__next',
      {
        attrs: { disabled: ctrl.loading || ctrl.replaying, 'data-icon': licon.PlayTriangle },
        hook: onInsert(el => el.addEventListener('click', ctrl.next)),
      },
      ' Next position',
    ),
  ]);
};

const ratingDiffTag = (diff: number): VNode =>
  diff >= 0 ? hl('good.rp', ` +${diff}`) : hl('bad.rp', ` −${-diff}`);
