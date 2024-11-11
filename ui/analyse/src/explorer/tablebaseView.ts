import { h, type VNode } from 'snabbdom';
import type AnalyseCtrl from '../ctrl';
import { moveArrowAttributes, winnerOf } from './explorerUtil';
import type { TablebaseMoveStats } from './interfaces';

export function showTablebase(
  ctrl: AnalyseCtrl,
  fen: FEN,
  title: string,
  tooltip: string | undefined,
  moves: TablebaseMoveStats[],
): VNode[] {
  if (!moves.length) return [];
  return [
    h('div.title', tooltip ? { attrs: { title: tooltip } } : {}, title),
    h('table.tablebase', [
      h(
        'tbody',
        moveArrowAttributes(ctrl, { fen, onClick: (_, uci) => uci && ctrl.explorerMove(uci) }),
        moves.map(move =>
          h('tr', { key: move.uci, attrs: { 'data-uci': move.uci } }, [
            h('td', move.san),
            h('td', [showDtz(fen, move), showDtm(fen, move), showDtw(fen, move)]),
          ]),
        ),
      ),
    ]),
  ];
}

function showDtm(fen: FEN, move: TablebaseMoveStats) {
  if (move.dtm)
    return h(
      'result.' + winnerOf(fen, move),
      {
        attrs: { title: i18n.site.mateInXHalfMoves(Math.abs(move.dtm)) + ' (Depth To Mate)' },
      },
      'DTM ' + Math.abs(move.dtm),
    );
  return undefined;
}

function showDtw(fen: FEN, move: TablebaseMoveStats) {
  if (move.dtw)
    return h(
      'result.' + winnerOf(fen, move),
      { attrs: { title: 'Depth To Win' } },
      'DTW ' + Math.abs(move.dtw),
    );
  return undefined;
}

function showDtz(fen: FEN, move: TablebaseMoveStats): VNode | null {
  if (move.checkmate) return h('result.' + winnerOf(fen, move), i18n.site.checkmate);
  else if (move.variant_win) return h('result.' + winnerOf(fen, move), i18n.site.variantLoss);
  else if (move.variant_loss) return h('result.' + winnerOf(fen, move), i18n.site.variantWin);
  else if (move.stalemate) return h('result.draws', i18n.site.stalemate);
  else if (move.insufficient_material) return h('result.draws', i18n.site.insufficientMaterial);
  else if (move.dtz === null) return null;
  else if (move.dtz === 0) return h('result.draws', i18n.site.draw);
  else if (move.zeroing)
    return move.san.includes('x')
      ? h('result.' + winnerOf(fen, move), i18n.site.capture)
      : h('result.' + winnerOf(fen, move), i18n.site.pawnMove);
  return h(
    'result.' + winnerOf(fen, move),
    {
      attrs: {
        title: i18n.site.dtzWithRounding + ' (Distance To Zeroing)',
      },
    },
    'DTZ ' + Math.abs(move.dtz),
  );
}
