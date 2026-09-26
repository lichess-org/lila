import { Chessground as makeChessground } from '@lichess-org/chessground';
import { INITIAL_BOARD_FEN } from 'chessops/fen';

import { pubsub } from 'lib/pubsub';
import { makeCgOpts } from 'lib/puz/run';
import { makeConfig as makeCgConfig } from 'lib/puz/view/chessground';
import { div, makeExoticTag, onInsert, type VNode } from 'lib/view';

import type RacerCtrl from '@/ctrl';

export const renderBoard = (ctrl: RacerCtrl) => {
  return div('.puz-board.main-board', [
    renderGround(ctrl),
    ctrl.promotion.view(),
    renderCountdown(ctrl.countdownSeconds()),
  ]);
};

const renderGround = (ctrl: RacerCtrl): VNode =>
  div('.cg-wrap', {
    hook: onInsert(el => {
      ctrl.ground(
        makeChessground(
          el,
          makeCgConfig(
            ctrl.isRacing() && ctrl.isPlayer()
              ? makeCgOpts(ctrl.run, true, ctrl.flipped)
              : { fen: INITIAL_BOARD_FEN, orientation: ctrl.run.pov, movable: { color: ctrl.run.pov } },
            ctrl.pref,
            ctrl.userMove,
          ),
        ),
      );
      pubsub.on('board.change', (is3d: boolean) =>
        ctrl.withGround(g => {
          g.state.addPieceZIndex = is3d;
          g.redrawAll();
        }),
      );
    }),
  });

const light = makeExoticTag('light');

const renderCountdown = (seconds?: number) => {
  if (!seconds) return undefined;
  return div('.racer__countdown', [
    div('.racer__countdown__lights', [
      light('.red', { class: { active: seconds > 4 } }),
      light('.orange', { class: { active: seconds === 3 || seconds === 4 } }),
      light('.green', { class: { active: seconds <= 2 } }),
    ]),
    div('.racer__countdown__seconds', seconds),
  ]);
};
