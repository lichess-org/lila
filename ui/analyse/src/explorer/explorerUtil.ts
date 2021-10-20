import { TablebaseMoveStats } from './interfaces';
import { opposite } from 'chessops/util';
import { VNode } from 'snabbdom';
import AnalyseCtrl from '../ctrl';

export function colorOf(fen: Fen): Color {
  return fen.split(' ')[1] === 'w' ? 'white' : 'black';
}

export function winnerOf(fen: Fen, move: TablebaseMoveStats): Color | undefined {
  const stm = colorOf(fen);
  if (move.checkmate || move.variant_loss || (move.dtz && move.dtz < 0)) return stm;
  if (move.variant_win || (move.dtz && move.dtz > 0)) return opposite(stm);
  return undefined;
}

export const ucfirst = (str: string) => `${str[0].toUpperCase()}${str.slice(1)}`;

export const moveTableAttributes = (ctrl: AnalyseCtrl, fen: Fen) => ({
  attrs: { 'data-fen': fen },
  hook: {
    insert(vnode: VNode) {
      const el = vnode.elm as HTMLElement;
      el.addEventListener('mouseover', e => {
        ctrl.explorer.setHovering(
          $(el).attr('data-fen')!,
          $(e.target as HTMLElement)
            .parents('tr')
            .attr('data-uci')
        );
      });
      el.addEventListener('mouseout', _ => {
        ctrl.explorer.setHovering($(el).attr('data-fen')!, null);
      });
      el.addEventListener('mousedown', e => {
        const uci = $(e.target as HTMLElement)
          .parents('tr')
          .attr('data-uci');
        if (uci) ctrl.explorerMove(uci);
      });
    },
  },
});
