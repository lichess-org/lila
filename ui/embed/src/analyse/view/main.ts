import { initOneWithState } from 'common/mini-board';
import { type VNode, h } from 'snabbdom';
import type { AnalyseCtrl } from '../ctrl';
import { renderFooter } from './footer';
import { renderJumps } from './jumps';
import { renderMoves } from './moves';

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
