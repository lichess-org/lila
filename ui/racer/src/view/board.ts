import type RacerCtrl from '../ctrl';
import { makeCgOpts } from 'lib/puz/run';
import { makeConfig as makeCgConfig } from 'lib/puz/view/chessground';
import { h, type VNode } from 'snabbdom';
import { INITIAL_BOARD_FEN } from 'chessops/fen';
import { Chessground as makeChessground } from '@lichess-org/chessground';
import { pubsub } from 'lib/pubsub';
import { setupSafariDragHover } from 'lib/safariDragHover';

export const renderBoard = (ctrl: RacerCtrl) => {
  const secs = ctrl.countdownSeconds();
  return h('div.puz-board.main-board', [
    renderGround(ctrl),
    ctrl.promotion.view(),
    secs ? renderCountdown(secs) : undefined,
  ]);
};

const renderGround = (ctrl: RacerCtrl): VNode =>
  h('div.cg-wrap', {
    hook: {
      insert: vnode => {
        const cg = makeChessground(
          vnode.elm as HTMLElement,
          makeCgConfig(
            ctrl.isRacing() && ctrl.isPlayer()
              ? makeCgOpts(ctrl.run, true, ctrl.flipped)
              : { fen: INITIAL_BOARD_FEN, orientation: ctrl.run.pov, movable: { color: ctrl.run.pov } },
            ctrl.pref,
            ctrl.userMove,
          ),
        );
        
        ctrl.ground(cg);
        
        // Setup Safari-specific drag hover fix
        setupSafariDragHover(cg);
        
        pubsub.on('board.change', (is3d: boolean) =>
          ctrl.withGround(g => {
            g.state.addPieceZIndex = is3d;
            g.redrawAll();
          }),
        );
      },
    },
  });

const renderCountdown = (seconds: number) =>
  h('div.racer__countdown', [
    h('div.racer__countdown__lights', [
      h('light.red', { class: { active: seconds > 4 } }),
      h('light.orange', { class: { active: seconds === 3 || seconds === 4 } }),
      h('light.green', { class: { active: seconds <= 2 } }),
    ]),
    h('div.racer__countdown__seconds', seconds),
  ]);
