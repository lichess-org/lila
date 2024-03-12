import { TablebaseMoveStats } from './interfaces';
import { opposite } from 'chessops/util';
import { fenColor } from 'common/miniBoard';
import { VNode } from 'snabbdom';
import AnalyseCtrl from '../ctrl';
import { FEN } from 'chessground/types';

export function winnerOf(fen: FEN, move: TablebaseMoveStats): Color | undefined {
  const stm = fenColor(fen);
  if (move.checkmate || move.variant_loss || (move.dtz && move.dtz < 0)) return stm;
  if (move.variant_win || (move.dtz && move.dtz > 0)) return opposite(stm);
  return undefined;
}

export const ucfirst = (str: string) => `${str[0].toUpperCase()}${str.slice(1)}`;

export interface MoveArrowOpts {
  fen: FEN;
  onClick: (e: MouseEvent, uci: string | null) => void;
}

export const moveArrowAttributes = (ctrl: AnalyseCtrl, opts: MoveArrowOpts) => ({
  attrs: { 'data-fen': opts.fen },
  hook: {
    insert(vnode: VNode) {
      const el = vnode.elm as HTMLElement;
      el.addEventListener('mouseover', e => {
        ctrl.explorer.setHovering(
          $(el).attr('data-fen')!,
          $(e.target as HTMLElement)
            .parents('tr')
            .attr('data-uci'),
        );
      });
      el.addEventListener('mouseout', _ => {
        ctrl.explorer.setHovering($(el).attr('data-fen')!, null);
      });
      el.addEventListener('click', e => {
        const uci = $(e.target as HTMLElement)
          .parents('tr')
          .attr('data-uci');
        opts.onClick(e, uci);
      });
    },
  },
});
