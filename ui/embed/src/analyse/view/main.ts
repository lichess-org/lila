import { h, VNode } from 'snabbdom';
import { AnalyseCtrl } from '../ctrl';
import { renderMoves } from './moves';
import { initOneWithState } from 'common/mini-board';
import { renderJumps } from './jumps';
import { renderFooter } from './footer';

export function view(ctrl: AnalyseCtrl): VNode {
  return h('main.analyse', [
    h(`div.analyse__board.main-board.mini-board.v-${ctrl.data.game.variant.key}`, {
      hook: {
        insert: vnode => {
          initOneWithState(vnode.elm as HTMLElement, {
            variant: ctrl.data.game.variant.key,
            sfen: ctrl.node.sfen,
            orientation: ctrl.data.orientation,
          });
          ctrl.groundElement = vnode.elm as HTMLElement;
        },
      },
    }),
    renderMoves(ctrl),
    renderJumps(ctrl),
    renderFooter(ctrl),
  ]);
}
