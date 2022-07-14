import ReplayCtrl from './ctrl';
import { Chessground } from 'chessground';
import { Config as CgConfig } from 'chessground/config';
import { uciToChessgroundLastMove } from 'chess';
import { h, VNode } from 'snabbdom';

export default function view(ctrl: ReplayCtrl) {
  return h('div.replay', [h('div.replay__board', renderGround(ctrl))]);
}

const renderGround = (ctrl: ReplayCtrl): VNode =>
  h('div.cg-wrap', {
    hook: {
      insert: vnode => ctrl.ground(Chessground(vnode.elm as HTMLElement, makeConfig(ctrl))),
    },
  });

export function makeConfig(ctrl: ReplayCtrl): CgConfig {
  const node = ctrl.node();
  return {
    viewOnly: true,
    fen: node.fen,
    orientation: ctrl.orientation,
    check: node.check,
    lastMove: uciToChessgroundLastMove(node.uci),
    coordinates: true,
    addDimensionsCssVars: true,
    drawable: {
      enabled: false,
      visible: false,
    },
  };
}
